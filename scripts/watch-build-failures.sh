#!/bin/bash
# Watch for build failure notifications from GitHub Actions

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
REQUESTS_DIR="$REPO_DIR/.claude-code-requests"

echo "🔍 Monitoring for build failures..."
echo "Press Ctrl+C to stop"
echo ""

while true; do
    # Pull latest changes
    git fetch origin main:main 2>/dev/null
    git pull --rebase 2>/dev/null

    # Check for new fix requests
    if [ -d "$REQUESTS_DIR" ]; then
        REQUEST_COUNT=$(ls -1 "$REQUESTS_DIR"/*.md 2>/dev/null | wc -l)

        if [ "$REQUEST_COUNT" -gt 0 ]; then
            echo ""
            echo "⚠️  BUILD FAILURE DETECTED! ($REQUEST_COUNT fix request(s))"
            echo ""

            # Show each request
            for request in "$REQUESTS_DIR"/*.md; do
                echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                cat "$request"
                echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                echo ""
                echo "📝 Fix request saved to: $request"
                echo ""
            done

            echo "💡 TELL CLAUDE:"
            echo "   \"There's a build failure. Check .claude-code-requests/ and fix it.\""
            echo ""

            # Archive the requests after showing them
            mkdir -p "$REQUESTS_DIR/processed"
            mv "$REQUESTS_DIR"/*.md "$REQUESTS_DIR/processed/" 2>/dev/null

            break
        fi
    fi

    sleep 10
done
