#!/bin/bash
set -e

OUTPUT="docs/repo-index.json"
MODULES_DIR="modules"
mkdir -p "$(dirname "$OUTPUT")"

echo "Generating module index from manifests..."

MODULES_JSON="[]"

for manifest in "$MODULES_DIR"/*/manifest.json; do
    if [ ! -f "$manifest" ]; then
        continue
    fi

    module_id=$(python3 -c "import json; print(json.load(open('$manifest'))['module_id'])" 2>/dev/null || \
                grep -o '"module_id"[[:space:]]*:[[:space:]]*"[^"]*"' "$manifest" | head -1 | grep -o '"[^"]*"$' | tr -d '"')

    version=$(python3 -c "import json; print(json.load(open('$manifest'))['version'])" 2>/dev/null || \
              grep -o '"version"[[:space:]]*:[[:space:]]*"[^"]*"' "$manifest" | head -1 | grep -o '"[^"]*"$' | tr -d '"')

    description=$(python3 -c "import json; print(json.load(open('$manifest'))['description'])" 2>/dev/null || echo "$module_id module")

    install_size=$(python3 -c "import json; print(json.load(open('$manifest')).get('install_size_mb', 10))" 2>/dev/null || echo "10")

    deps=$(python3 -c "
import json
d = json.load(open('$manifest'))
deps = d.get('dependencies', [])
print(deps)
" 2>/dev/null || echo "[]")

    name=$(echo "$module_id" | sed 's/env-//' | sed 's/.*/\u&/')

    case "$module_id" in
        env-base) name="Core Runtime" ;;
        env-cc) name="C/C++ Toolchain" ;;
        env-jdk) name="Java JDK" ;;
        env-python) name="Python 3" ;;
        env-node) name="Node.js" ;;
        env-git) name="Git" ;;
    esac

    MODULES_JSON=$(echo "$MODULES_JSON" | python3 -c "
import json, sys
modules = json.load(sys.stdin)
modules.append({
    'id': '$module_id',
    'name': '$name',
    'description': '''$description''',
    'latest': '$version',
    'size_mb': $install_size,
    'dependencies': $deps
})
print(json.dumps(modules))
" 2>/dev/null || echo "$MODULES_JSON")
done

if [ "$MODULES_JSON" = "[]" ]; then
    cat > "$OUTPUT" << INDEX
{
  "version": "1.0.0",
  "last_updated": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "base_url": "https://github.com/XION-HN/haisa-des/releases/download",
  "modules": [
    {
      "id": "env-base",
      "name": "Core Runtime",
      "description": "Base libraries and tools",
      "latest": "1.0.0",
      "size_mb": 5,
      "dependencies": []
    },
    {
      "id": "env-cc",
      "name": "C/C++ Toolchain",
      "description": "Clang/LLVM for C/C++ development",
      "latest": "17.0.1",
      "size_mb": 80,
      "dependencies": ["env-base"]
    },
    {
      "id": "env-jdk",
      "name": "Java JDK",
      "description": "OpenJDK for Java and Android development",
      "latest": "17.0.8",
      "size_mb": 180,
      "dependencies": ["env-base"]
    },
    {
      "id": "env-python",
      "name": "Python 3",
      "description": "Python 3.11 runtime",
      "latest": "3.11.8",
      "size_mb": 40,
      "dependencies": ["env-base"]
    },
    {
      "id": "env-node",
      "name": "Node.js",
      "description": "Node.js LTS runtime",
      "latest": "20.11.0",
      "size_mb": 35,
      "dependencies": ["env-base"]
    },
    {
      "id": "env-git",
      "name": "Git",
      "description": "Version control tool",
      "latest": "2.43.0",
      "size_mb": 10,
      "dependencies": ["env-base"]
    }
  ]
}
INDEX
else
    python3 -c "
import json
index = {
    'version': '1.0.0',
    'last_updated': '$(date -u +%Y-%m-%dT%H:%M:%SZ)',
    'base_url': 'https://github.com/XION-HN/haisa-des/releases/download',
    'modules': json.loads('''$MODULES_JSON''')
}
print(json.dumps(index, indent=2))
" > "$OUTPUT" 2>/dev/null
fi

echo "Module index generated at $OUTPUT"
