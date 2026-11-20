#!/usr/bin/env bash
# Uso: ./ejecutar.sh [demonio | una-vez | consola | diagnostico | enviar-prueba <correo> | hash <clave> | doc]
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v java >/dev/null 2>&1; then
  for j in "${JAVA_HOME:-}" "$HOME"/.local/jdk/*; do
    if [ -n "$j" ] && [ -x "$j/bin/java" ]; then export PATH="$j/bin:$PATH"; break; fi
  done
fi
[ -f dist/inf513-proyecto1.jar ] || ./compilar.sh >/dev/null
exec java -Dfile.encoding=UTF-8 ${JAVA_OPTS:-} -jar dist/inf513-proyecto1.jar "$@"
