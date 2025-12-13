#!/bin/bash
# Check GitHub Actions build status and save failure info

REPO="timmseth/survivalClicker"
API_URL="https://api.github.com/repos/$REPO/actions/runs"

echo "Checking latest build status for $REPO..."

# Get latest run
LATEST_RUN=$(curl -s "$API_URL?per_page=1" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')

if [ -z "$LATEST_RUN" ]; then
    echo "ERROR: Could not fetch build status"
    exit 1
fi

# Get run details
RUN_DATA=$(curl -s "$API_URL/$LATEST_RUN")

CONCLUSION=$(echo "$RUN_DATA" | grep -o '"conclusion":"[^"]*"' | cut -d'"' -f4)
STATUS=$(echo "$RUN_DATA" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
HTML_URL=$(echo "$RUN_DATA" | grep -o '"html_url":"[^"]*"' | head -1 | cut -d'"' -f4)

echo "Status: $STATUS"
echo "Conclusion: $CONCLUSION"
echo "URL: $HTML_URL"

if [ "$CONCLUSION" = "failure" ]; then
    echo ""
    echo "BUILD FAILED!"
    echo "Fetching error logs..."

    # Get job logs
    JOBS_URL="$API_URL/$LATEST_RUN/jobs"
    JOB_ID=$(curl -s "$JOBS_URL" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')

    echo "Latest job ID: $JOB_ID"
    echo ""
    echo "View full logs at: $HTML_URL"
    echo ""
    echo "TELL CLAUDE: Build failed at run $LATEST_RUN - check logs and fix"
    exit 1
elif [ "$CONCLUSION" = "success" ]; then
    echo ""
    echo "✓ BUILD PASSED!"
    exit 0
else
    echo ""
    echo "Build is $STATUS (conclusion: $CONCLUSION)"
    exit 0
fi
