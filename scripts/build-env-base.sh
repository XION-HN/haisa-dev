#!/bin/bash
set -e

# Build script for env-base module
MODULE_ID="env-base"
ARCH="${2:-aarch64}"
VERSION="${4:-1.0.0}"
OUTPUT="${6:-./dist}"

echo "Building $MODULE_ID v$VERSION for $ARCH..."

mkdir -p "$OUTPUT/bin" "$OUTPUT/lib" "$OUTPUT/usr/bin"

# Copy essential binaries
# Note: In production, these would be built from source or extracted from Termux packages
cp /bin/sh "$OUTPUT/bin/" 2>/dev/null || true
cp /bin/busybox "$OUTPUT/bin/" 2>/dev/null || true
cp /lib/ld-linux*.so* "$OUTPUT/lib/" 2>/dev/null || true
cp /lib/libc.so* "$OUTPUT/lib/" 2>/dev/null || true

# Create manifest
cat > "$OUTPUT/manifest.json" << MANIFEST
{
  "module_id": "$MODULE_ID",
  "package_id": "${MODULE_ID}-${VERSION}-${ARCH}",
  "version": "$VERSION",
  "arch": "$ARCH",
  "description": "Core runtime libraries and base tools",
  "min_base_version": "1.0.0",
  "dependencies": [],
  "entry_binaries": {
    "sh": "bin/sh"
  },
  "env_vars": {
    "PATH": "{{install_dir}}/bin:{{old_path}}"
  },
  "install_size_mb": 5
}
MANIFEST

echo "✅ $MODULE_ID built successfully."
