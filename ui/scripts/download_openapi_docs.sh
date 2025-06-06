#!/bin/bash
default_url="http://localhost:8000/v3/api-docs"
curl ${1:-$default_url} > api-docs.json
