#!/bin/bash
set -euo pipefail

# Git doesn't keep track of the executable bit on files in the repo
# for security reasons - we need to restore them here

# --legacy-peer-deps is mandatory (see CLAUDE.md / .circleci/config.yml): react-inspector
# pins React 16-18 against the React 19 tree, so a strict `npm ci` computes a different ideal
# tree than the lockfile and wrongly reports it as out of sync. Must match CircleCI's install.
npm ci --legacy-peer-deps

find node_modules/ -type d -name 'bin' -exec chmod -R +x {} \;
find node_modules/ -type d -name '.bin' -exec chmod -R +x {} \;

npm run lint && npm run build
