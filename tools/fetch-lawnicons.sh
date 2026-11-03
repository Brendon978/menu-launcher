#!/usr/bin/env bash
# Baixa os ícones do Lawnicons (Apache-2.0) fixados num commit e copia só o necessário para os assets.
set -euo pipefail
COMMIT="${1:-238f782c6b3c29c3ada616ef48190fe7f86836d8}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/app/src/main/assets/lawnicons"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

curl -fsSL "https://codeload.github.com/LawnchairLauncher/lawnicons/tar.gz/$COMMIT" -o "$TMP/l.tgz"
tar -xzf "$TMP/l.tgz" -C "$TMP"
SRC="$(find "$TMP" -maxdepth 1 -type d -name 'lawnicons-*' | head -1)"

rm -rf "$DEST"
mkdir -p "$DEST"
cp -r "$SRC/svgs" "$DEST/svgs"
cp "$SRC/app/assets/appfilter.xml" "$DEST/appfilter.xml"
cp "$SRC/LICENSE" "$DEST/LICENSE"
echo "$COMMIT" > "$DEST/COMMIT"
echo "Lawnicons $COMMIT: $(ls "$DEST/svgs" | wc -l) ícones em $DEST"
