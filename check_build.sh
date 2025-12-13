#!/bin/bash
# Fetch latest workflow run status from GitHub API
REPO="timmseth/survivalClicker"
WORKFLOW="build.yml"

# Get the latest workflow run
curl -s -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/$REPO/actions/workflows/$WORKFLOW/runs?per_page=1" \
  | grep -E '"conclusion"|"html_url"' \
  | head -2

