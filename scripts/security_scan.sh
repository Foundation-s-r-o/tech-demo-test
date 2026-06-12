#!/usr/bin/env bash
set -euo pipefail

export SEMGREP_SEND_METRICS=off

scan_tmp="${TMPDIR:-/tmp}/tech-demo-security-scan"
mkdir -p "$scan_tmp"
export SEMGREP_LOG_FILE="$scan_tmp/semgrep.log"
export SEMGREP_SETTINGS_FILE="$scan_tmp/settings.yml"
export SEMGREP_VERSION_CACHE_PATH="$scan_tmp/semgrep-version"

if [ -z "${SSL_CERT_FILE:-}" ] && [ -f /etc/ssl/cert.pem ]; then
  export SSL_CERT_FILE=/etc/ssl/cert.pem
fi

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "### Security scan started"

if ! command -v semgrep >/dev/null; then
  echo "ERROR: semgrep is required"
  exit 1
fi

if ! command -v trivy >/dev/null; then
  echo "ERROR: trivy is required"
  exit 1
fi

semgrep scan \
  --metrics off \
  --no-trace \
  --config p/security-audit \
  --config p/secrets \
  --config p/owasp-top-ten \
  "$project_root"

trivy fs \
  --scanners vuln,secret,misconfig \
  --severity HIGH,CRITICAL \
  --exit-code 1 \
  "$project_root"

(
  cd "$project_root/ui"
  npm audit --audit-level=high
)

echo "### Security scan completed successfully"
