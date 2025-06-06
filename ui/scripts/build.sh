#!/bin/bash

# Git doesn't keep track of the executable bit on files in the repo
# for security reasons - we need to restore them here

npm ci

find node_modules/ -type d -name 'bin' -exec chmod -R +x {} \;
find node_modules/ -type d -name '.bin' -exec chmod -R +x {} \;

npm run lint && npm run build
