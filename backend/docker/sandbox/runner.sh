#!/bin/bash
set -e

LANG_CHOICE="$1"

# Read stdin content
PAYLOAD=$(cat)

# Extract code block
CODE=$(echo "$PAYLOAD" | awk '/---CODE_START---/{flag=1; next} /---CODE_END---/{flag=0} flag')

# Extract input block
INPUT_DATA=$(echo "$PAYLOAD" | awk '/---INPUT_START---/{flag=1; next} /---INPUT_END---/{flag=0} flag')

# Save input data to tmpfs
echo "$INPUT_DATA" > /tmp/input.txt

if [ "$LANG_CHOICE" = "JAVA" ]; then
    echo "$CODE" > /tmp/Solution.java
    # Compile Java
    if ! javac /tmp/Solution.java 2> /tmp/compile_err.txt; then
        cat /tmp/compile_err.txt >&2
        echo "CompileError" >&2
        exit 1
    fi
    # Execute Java
    java -cp /tmp Solution < /tmp/input.txt
elif [ "$LANG_CHOICE" = "PYTHON" ]; then
    echo "$CODE" > /tmp/script.py
    # Execute Python
    python3 /tmp/script.py < /tmp/input.txt
else
    echo "Unsupported language choice" >&2
    exit 1
fi
