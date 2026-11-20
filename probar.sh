#!/usr/bin/env bash
# Ejecuta las pruebas automáticas (todo en local, no toca tecnoweb):
#   1) unitarias: parser, MIME y validación
#   2) extremo a extremo: POP3/SMTP simulados + base de datos local (./local.sh)
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v javac >/dev/null 2>&1; then
  for j in "${JAVA_HOME:-}" "$HOME"/.local/jdk/*; do
    if [ -n "$j" ] && [ -x "$j/bin/javac" ]; then export PATH="$j/bin:$PATH"; break; fi
  done
fi
./compilar.sh >/dev/null
mkdir -p build/test
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -cp "build/classes:lib/*" -d build/test $(find test -name '*.java')
CP="build/test:build/classes:lib/*"

echo "== Pruebas unitarias"
java -Dfile.encoding=UTF-8 -cp "$CP" inf513.PruebasUnitarias

echo; echo "== Prueba de correo simulado (base local reiniciada)"
./local.sh reiniciar >/dev/null
java -Dfile.encoding=UTF-8 -Dbd.host=127.0.0.1 -Dbd.puerto=5434 -Dbd.nombre=db_grupo23sa_local \
  -Dbd.usuario="$(id -un)" -Dbd.password= -Dlogs.directorio=logs -cp "$CP" inf513.PruebaCorreoSimulado
