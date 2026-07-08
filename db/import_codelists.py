#!/usr/bin/env python3
"""One-time import of official MetaIS codelists into the civil-registry schema.

Downloads codelist items from the public MetaIS codelist-repo API
(https://metais.slovensko.sk/api/codelist-repo, no authentication required)
and generates a portable SQL seed file (plain INSERT statements, no vendor-
specific upsert) for the codelist.* tables. The output is the Flyway migration
V6__seed_codelists.sql consumed by the application (embedded H2, portable to
PostgreSQL); a versioned migration runs exactly once, so ON CONFLICT / upsert
semantics are unnecessary and deliberately omitted.

Codelists imported (identity tables — PK = official MetaIS item code):
  CL000086 Krajina OSN                    -> codelist.country
  CL000023 Kraj                           -> codelist.region
  CL000024 Okres                          -> codelist.district
  CL000025 Obec                           -> codelist.municipality
  CL010141 Cast obce                      -> codelist.municipality_part
  CL000015 Pracovisko matricneho uradu    -> codelist.registry_office
  CL004002 Rodinny stav                   -> codelist.marital_status
  CL004003 Existencny stav                -> codelist.existence_status
  CL000062 Titul pred menom               -> codelist.academic_title (before)
  CL000063 Titul za menom                 -> codelist.academic_title (after)
  CL000014 Sud                            -> codelist.court
  CL000010 Typ rodinneho vztahu           -> codelist.family_relation_type

Semantic tables (internal PKs seeded by schema.sql; only metais_code +
validity get updated):
  CL003003 Pohlavie                       -> codelist.sex      (M/F/X <- 1/2/0)
  CL010139 Druh adresy                    -> codelist.address_type
                                             (permanent/temporary/foreign;
                                              'official' has no MetaIS item)

Prerequisites: Python 3.11+, requests (pip install -r requirements.txt).

Usage:
  ./import_codelists.py                # fetch all, write the V6 migration
  ./import_codelists.py --dry-run      # fetch + report counts, write nothing
  ./import_codelists.py --only CL000086,CL003003
  ./import_codelists.py --output path/to/file.sql
  ./import_codelists.py --debug

The result is applied automatically by Flyway on application start (it is a
versioned migration under api/src/main/resources/db/migration/).
"""

import argparse
import logging
import sys
from datetime import datetime
from pathlib import Path

import requests

BASE_URL = "https://metais.slovensko.sk/api/codelist-repo"
PER_PAGE = 100
TIMEOUT = 30
# The application's Flyway migration; portable SQL applied on start.
DEFAULT_OUTPUT_FILE = (
    Path(__file__).parent.parent / "api/src/main/resources/db/migration/V6__seed_codelists.sql"
)

# CL code -> (table, pk column, extra constant columns).
# academic_title carries an extra title_position column: title codes overlap
# between CL000062 (before) and CL000063 (after) — composite PK in the DDL.
IDENTITY_TARGETS: dict[str, tuple[str, str, dict[str, str]]] = {
    "CL000086": ("codelist.country", "country_code", {}),
    "CL000023": ("codelist.region", "region_code", {}),
    "CL000024": ("codelist.district", "district_code", {}),
    "CL000025": ("codelist.municipality", "municipality_code", {}),
    "CL010141": ("codelist.municipality_part", "part_code", {}),
    "CL000015": ("codelist.registry_office", "office_code", {}),
    "CL004002": ("codelist.marital_status", "marital_status_code", {}),
    "CL004003": ("codelist.existence_status", "existence_status_code", {}),
    "CL000062": ("codelist.academic_title", "title_code", {"title_position": "before"}),
    "CL000063": ("codelist.academic_title", "title_code", {"title_position": "after"}),
    "CL000014": ("codelist.court", "court_code", {}),
    "CL000010": ("codelist.family_relation_type", "relation_code", {}),
}

# CL code -> (table, pk column, {internal_key: metais_item_code})
SEMANTIC_TARGETS: dict[str, tuple[str, str, dict[str, str]]] = {
    "CL003003": ("codelist.sex", "sex_code", {"M": "1", "F": "2", "X": "0"}),
    "CL010139": (
        "codelist.address_type",
        "address_type_code",
        {"permanent": "100001", "temporary": "100002", "foreign": "100003"},
    ),
}

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
log = logging.getLogger(__name__)


# SQL keywords are emitted lowercase to satisfy the migration Checkstyle rule
# (sqlKeywordsMustBeLowerCase) shared with the application's V1-V5 migrations.
def sql_quote(value: str | None) -> str:
    if value is None:
        return "null"
    return "'" + value.replace("'", "''") + "'"


def sql_date(value: str | None) -> str:
    """Convert MetaIS timestamp ('2000-01-01T01:00:00.000') to SQL date literal."""
    if not value:
        return "null"
    return "'" + datetime.fromisoformat(value).date().isoformat() + "'"


def fetch_items(session: requests.Session, cl_code: str) -> list[dict]:
    """Fetch all effective items of one codelist, paginated."""
    items: list[dict] = []
    page = 1
    while True:
        resp = session.get(
            f"{BASE_URL}/codelists/codelistheaders/{cl_code}/codelistitems",
            params={
                "language": "sk",
                "pageNumber": page,
                "perPage": PER_PAGE,
                "effective": "true",
            },
            timeout=TIMEOUT,
        )
        resp.raise_for_status()
        batch = resp.json().get("codelistsItems") or []
        if not batch:
            break
        items.extend(batch)
        log.debug("%s: page %d -> %d items", cl_code, page, len(batch))
        if len(batch) < PER_PAGE:
            break
        page += 1
    return items


def item_name_sk(item: dict) -> str | None:
    """Pick the currently effective Slovak name of an item."""
    names = [n for n in item.get("codelistItemNames", []) if n.get("language") == "sk"]
    if not names:
        return None
    open_ended = [n for n in names if not n.get("effectiveTo")]
    chosen = open_ended[-1] if open_ended else names[-1]
    return chosen.get("value")


def item_validity(item: dict) -> tuple[str | None, str | None]:
    validities = item.get("codelistItemValidities", [])
    if not validities:
        return None, None
    v = validities[0]
    return v.get("effectiveFrom"), v.get("effectiveTo")


def identity_inserts(cl_code: str, items: list[dict]) -> list[str]:
    table, pk_col, extra = IDENTITY_TARGETS[cl_code]
    extra_cols = "".join(f", {col}" for col in extra)
    # A few MetaIS codelists carry duplicate effective itemCodes (e.g.
    # municipality parts, marital status). Plain INSERTs (no upsert) would
    # violate the PK, so keep the last occurrence per code — the same result
    # the retired PostgreSQL ON CONFLICT DO UPDATE produced.
    deduped: dict[str, dict] = {}
    for item in items:
        code = item.get("itemCode")
        if code is not None:
            deduped[code] = item
    stmts: list[str] = []
    for item in deduped.values():
        code = item.get("itemCode")
        name = item_name_sk(item)
        if not code or not name:
            log.warning("%s: skipping item without code/name: id=%s", cl_code, item.get("id"))
            continue
        valid_from, valid_to = item_validity(item)
        extra_vals = "".join(f", {sql_quote(val)}" for val in extra.values())
        # Plain portable INSERT: a versioned Flyway migration runs once, so no
        # ON CONFLICT / upsert. Stays valid across H2 and PostgreSQL.
        stmts.append(
            f"insert into {table} ({pk_col}, name, source_codelist, metais_code,"
            f" item_valid_from, item_valid_to{extra_cols})\n"
            f"values ({sql_quote(code)}, {sql_quote(name)}, {sql_quote(cl_code)},"
            f" {sql_quote(code)}, {sql_date(valid_from)}, {sql_date(valid_to)}{extra_vals});"
        )
    return stmts


def semantic_updates(cl_code: str, items: list[dict]) -> list[str]:
    table, pk_col, key_map = SEMANTIC_TARGETS[cl_code]
    by_code = {item.get("itemCode"): item for item in items}
    stmts: list[str] = []
    for internal_key, metais_code in key_map.items():
        item = by_code.get(metais_code)
        if item is None:
            log.warning(
                "%s: item %s not found for internal key '%s'", cl_code, metais_code, internal_key
            )
            continue
        valid_from, valid_to = item_validity(item)
        stmts.append(
            f"update {table} set source_codelist = {sql_quote(cl_code)},"
            f" metais_code = {sql_quote(metais_code)},"
            f" item_valid_from = {sql_date(valid_from)},"
            f" item_valid_to = {sql_date(valid_to)}\n"
            f"where {pk_col} = {sql_quote(internal_key)};"
        )
    return stmts


def main() -> int:
    parser = argparse.ArgumentParser(description="Import MetaIS codelists to seed SQL")
    parser.add_argument(
        "--dry-run", action="store_true", help="fetch and report counts, do not write the seed file"
    )
    parser.add_argument(
        "--only", metavar="CL1,CL2", help="comma-separated subset of codelist codes to import"
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT_FILE,
        help="output SQL file (default: the V6 Flyway migration)",
    )
    parser.add_argument("--debug", action="store_true", help="verbose logging")
    args = parser.parse_args()
    if args.debug:
        logging.getLogger().setLevel(logging.DEBUG)

    log.info("=== MetaIS codelist import started ===")
    selected = set(args.only.split(",")) if args.only else None
    codes = [
        c
        for c in list(IDENTITY_TARGETS) + list(SEMANTIC_TARGETS)
        if selected is None or c in selected
    ]
    if not codes:
        log.error("No codelists selected (check --only values)")
        return 1

    statements: list[str] = []
    session = requests.Session()
    failures = 0
    for cl_code in codes:
        try:
            items = fetch_items(session, cl_code)
        except requests.RequestException as exc:
            log.error("%s: fetch failed: %s", cl_code, exc)
            failures += 1
            continue
        if cl_code in IDENTITY_TARGETS:
            stmts = identity_inserts(cl_code, items)
        else:
            stmts = semantic_updates(cl_code, items)
        log.info("%s: %d items -> %d SQL statements", cl_code, len(items), len(stmts))
        statements.append(f"-- {cl_code}: {len(stmts)} statements")
        statements.extend(stmts)

    if failures:
        log.error("%d codelist(s) failed to download — seed file NOT written", failures)
        return 1
    if args.dry_run:
        log.info("Dry run: %d statements total, nothing written", len(statements))
        log.info("=== Done ===")
        return 0

    header = (
        "-- ============================================================================\n"
        f"-- V6 - Official MetaIS codelist seed ({len(codes)} codelists).\n"
        "--\n"
        "-- Generated by import_codelists.py on "
        f"{datetime.now().isoformat(timespec='seconds')}.\n"
        "-- Source: MetaIS codelist-repo (public API), language=sk, effective items.\n"
        "-- Portable INSERTs (no ON CONFLICT): a versioned Flyway migration runs once.\n"
        "-- ============================================================================\n\n"
    )
    output_file: Path = args.output
    output_file.parent.mkdir(parents=True, exist_ok=True)
    output_file.write_text(header + "\n\n".join(statements) + "\n", encoding="utf-8")
    log.info("Wrote %s (%d statements)", output_file, len(statements))
    log.info("Flyway applies it automatically on application start.")
    log.info("=== Done ===")
    return 0


if __name__ == "__main__":
    sys.exit(main())
