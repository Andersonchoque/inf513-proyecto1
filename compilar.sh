#!/usr/bin/env bash
# Compila el proyecto y genera dist/inf513-proyecto1.jar (requiere JDK 8 o superior).
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v javac >/dev/null 2>&1; then
  for j in "${JAVA_HOME:-}" "$HOME"/.local/jdk/*; do
    if [ -n "$j" ] && [ -x "$j/bin/javac" ]; then export PATH="$j/bin:$PATH"; break; fi
  done
fi
command -v javac >/dev/null 2>&1 || { echo "No se encontró javac (instale un JDK 8+)"; exit 1; }

rm -rf build && mkdir -p build/classes dist
find src -name '*.java' > build/fuentes.txt
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -cp "lib/*" -d build/classes @build/fuentes.txt
printf 'Main-Class: inf513.App\nClass-Path: ../lib/postgresql-42.7.4.jar ../lib/jbcrypt-0.4.jar\n' > build/MANIFEST.MF
jar cfm dist/inf513-proyecto1.jar build/MANIFEST.MF -C build/classes .
echo "OK -> dist/inf513-proyecto1.jar"
