#!/bin/bash
set -e

# This script uses Claude API to automatically fix build failures

BUILD_ERROR="$1"
ANTHROPIC_API_KEY="${ANTHROPIC_API_KEY}"

if [ -z "$BUILD_ERROR" ]; then
    echo "ERROR: No build error provided"
    exit 1
fi

if [ -z "$ANTHROPIC_API_KEY" ]; then
    echo "ERROR: ANTHROPIC_API_KEY not set"
    exit 1
fi

echo "🤖 Analyzing build error with Claude..."

# Call Claude API
RESPONSE=$(curl -s https://api.anthropic.com/v1/messages \
  -H "content-type: application/json" \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "anthropic-beta: prompt-caching-2024-07-31" \
  -d @- << EOF
{
  "model": "claude-sonnet-4-20250514",
  "max_tokens": 8192,
  "messages": [{
    "role": "user",
    "content": "You are a Kotlin/Android build expert. A build just failed with this error:\n\n$BUILD_ERROR\n\nAnalyze the error and provide ONLY a JSON response with this exact structure:\n{\n  \"error_type\": \"brief description\",\n  \"file_path\": \"relative/path/to/file.kt\",\n  \"line_number\": 123,\n  \"fix_description\": \"what needs to change\",\n  \"search_text\": \"exact text to find and replace\",\n  \"replacement_text\": \"exact replacement text\"\n}\n\nIf you cannot determine a fix, return: {\"error\": \"Unable to auto-fix\"}"
  }]
}
EOF
)

echo "Claude response: $RESPONSE"

# Extract JSON from Claude's response (handle markdown code blocks)
FIX_JSON=$(echo "$RESPONSE" | grep -oP '(?<="text":").*(?=","type)' | sed 's/\\n/\n/g' | sed 's/\\"/"/g')

# Check if error
if echo "$FIX_JSON" | grep -q '"error"'; then
    echo "❌ Claude could not auto-fix this error"
    echo "$FIX_JSON"
    exit 1
fi

# Parse JSON and apply fix
ERROR_TYPE=$(echo "$FIX_JSON" | grep -oP '(?<="error_type": ")[^"]*')
FILE_PATH=$(echo "$FIX_JSON" | grep -oP '(?<="file_path": ")[^"]*')
FIX_DESC=$(echo "$FIX_JSON" | grep -oP '(?<="fix_description": ")[^"]*')
SEARCH=$(echo "$FIX_JSON" | grep -oP '(?<="search_text": ")[^"]*')
REPLACE=$(echo "$FIX_JSON" | grep -oP '(?<="replacement_text": ")[^"]*')

echo "📝 Fix details:"
echo "  Error: $ERROR_TYPE"
echo "  File: $FILE_PATH"
echo "  Fix: $FIX_DESC"

# Apply the fix
if [ -f "$FILE_PATH" ]; then
    echo "🔧 Applying fix to $FILE_PATH..."

    # Use sed to replace text
    sed -i "s|$SEARCH|$REPLACE|g" "$FILE_PATH"

    echo "✅ Fix applied!"
    exit 0
else
    echo "❌ File not found: $FILE_PATH"
    exit 1
fi
