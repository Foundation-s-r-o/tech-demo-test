#!/usr/bin/env python3
"""One-time import of official MetaIS codelists into the civil-registry schema.

Downloads codelist items from the public MetaIS codelist-repo API
(https://metais.slovensko.sk/api/codelist-repo, no authentication required)
and generates an idempotent SQL seed file (INSERT ... ON CONFLICT DO UPDATE)
for the codelist.* tables defined in schema.sql.

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
  ./import_codelists.py                # fetch all, write seed_codelists.sql
  ./import_codelists.py --dry-run      # fetch + report counts, write nothing
  ./import_codelists.py --only CL000086,CL003003
  ./import_codelists.py --debug

Apply the result:
  psql -d registry -v ON_ERROR_STOP=1 -f seed_codelists.sql
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
OUTPUT_FILE = Path(__file__).parent / "seed_codelists.sql"

# CL code -> (table, pk column, extra constant columns, ON CONFLICT target).
# Conflict target includes the extra column for academic_title because title
# codes overlap between CL000062 and CL000063 (composite PK in the DDL).
IDENTITY_TARGETS: dict[str, tuple[str, str, dict[str, str], str]] = {
    "CL000086": ("codelist.country", "country_code", {}, "country_code"),
    "CL000023": ("codelist.region", "region_code", {}, "region_code"),
    "CL000024": ("codelist.district", "district_code", {}, "district_code"),
    "CL000025": ("codelist.municipality", "municipality_code", {}, "municipality_code"),
    "CL010141": ("codelist.municipality_part", "part_code", {}, "part_code"),
    "CL000015": ("codelist.registry_office", "office_code", {}, "office_code"),
    "CL004002": ("codelist.marital_status", "marital_status_code", {}, "marital_status_code"),
    "CL004003": ("codelist.existence_status", "existence_status_code", {}, "existence_status_code"),
    "CL000062": (
        "codelist.academic_title",
        "title_code",
        {"title_position": "before"},
        "title_code, title_position",
    ),
    "CL000063": (
        "codelist.academic_title",
        "title_code",
        {"title_position": "after"},
        "title_code, title_position",
    ),
    "CL000014": ("codelist.court", "court_code", {}, "court_code"),
    "CL000010": ("codelist.family_relation_type", "relation_code", {}, "relation_code"),
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


def sql_quote(value: str | None) -> str:
    if value is None:
        return "NULL"
    return "'" + value.replace("'", "''") + "'"


def sql_date(value: str | None) -> str:
    """Convert MetaIS timestamp ('2000-01-01T01:00:00.000') to SQL date literal."""
    if not value:
        return "NULL"
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
    table, pk_col, extra, conflict_target = IDENTITY_TARGETS[cl_code]
    extra_cols = "".join(f", {col}" for col in extra)
    stmts: list[str] = []
    for item in items:
        code = item.get("itemCode")
        name = item_name_sk(item)
        if not code or not name:
            log.warning("%s: skipping item without code/name: id=%s", cl_code, item.get("id"))
            continue
        valid_from, valid_to = item_validity(item)
        extra_vals = "".join(f", {sql_quote(val)}" for val in extra.values())
        stmts.append(
            f"INSERT INTO {table} ({pk_col}, name, source_codelist, metais_code,"
            f" item_valid_from, item_valid_to{extra_cols})\n"
            f"VALUES ({sql_quote(code)}, {sql_quote(name)}, {sql_quote(cl_code)},"
            f" {sql_quote(code)}, {sql_date(valid_from)}, {sql_date(valid_to)}{extra_vals})\n"
            f"ON CONFLICT ({conflict_target}) DO UPDATE SET name = EXCLUDED.name,"
            f" source_codelist = EXCLUDED.source_codelist,"
            f" metais_code = EXCLUDED.metais_code,"
            f" item_valid_from = EXCLUDED.item_valid_from,"
            f" item_valid_to = EXCLUDED.item_valid_to;"
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
            f"UPDATE {table} SET source_codelist = {sql_quote(cl_code)},"
            f" metais_code = {sql_quote(metais_code)},"
            f" item_valid_from = {sql_date(valid_from)},"
            f" item_valid_to = {sql_date(valid_to)}\n"
            f"WHERE {pk_col} = {sql_quote(internal_key)};"
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
        "-- Generated by import_codelists.py on "
        f"{datetime.now().isoformat(timespec='seconds')}\n"
        "-- Source: MetaIS codelist-repo (public API), language=sk, effective items\n"
        "-- Idempotent: safe to re-run.\n\nBEGIN;\n\n"
    )
    OUTPUT_FILE.write_text(header + "\n\n".join(statements) + "\n\nCOMMIT;\n", encoding="utf-8")
    log.info("Wrote %s (%d statements)", OUTPUT_FILE, len(statements))
    log.info("Apply with: psql -d registry -v ON_ERROR_STOP=1 -f %s", OUTPUT_FILE)
    log.info("=== Done ===")
    return 0


if __name__ == "__main__":
    sys.exit(main())
