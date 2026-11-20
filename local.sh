#!/usr/bin/env bash
# Entorno LOCAL de pruebas (sin correo y sin tocar la base de tecnoweb):
#   - PostgreSQL privado en 127.0.0.1:5434 con datos en ./.pgdata (solo esta máquina, sin contraseña)
#   - base db_grupo23sa_local creada con sql/01_esquema.sql + sql/02_datos.sql
#   - el sistema corre en modo consola: cada línea es el asunto de un correo
#
# Uso:
#   ./local.sh                    consola interactiva ("salir" para terminar)
#   ./local.sh < pruebas/comandos.txt   ejecuta una lista de asuntos
#   ./local.sh reiniciar          borra y recrea la base local
#   ./local.sh psql               abre psql sobre la base local
#   ./local.sh detener            apaga el PostgreSQL local
set -euo pipefail
cd "$(dirname "$0")"

PGBIN="$(ls -d /usr/lib/postgresql/*/bin 2>/dev/null | sort -V | tail -1)"
[ -n "$PGBIN" ] || { echo "No se encontraron los binarios de PostgreSQL (/usr/lib/postgresql/*/bin)"; exit 1; }
PGDATA="$PWD/.pgdata"; PUERTO=5434; BD=db_grupo23sa_local; USUARIO="$(id -un)"
pg() { "$PGBIN/psql" -h 127.0.0.1 -p "$PUERTO" -U "$USUARIO" -v ON_ERROR_STOP=1 -q "$@"; }

if [ "${1:-}" = "detener" ]; then "$PGBIN/pg_ctl" -D "$PGDATA" stop -m fast; exit 0; fi

if [ ! -f "$PGDATA/PG_VERSION" ]; then
  echo "[local] Creando PostgreSQL privado en $PGDATA ..."
  "$PGBIN/initdb" -D "$PGDATA" -U "$USUARIO" --auth=trust -E UTF8 --locale=C.UTF-8 >/dev/null
fi
if ! "$PGBIN/pg_ctl" -D "$PGDATA" status >/dev/null 2>&1; then
  "$PGBIN/pg_ctl" -D "$PGDATA" -l "$PGDATA/postgres.log" -w \
    -o "-p $PUERTO -c listen_addresses=127.0.0.1 -k '$PGDATA'" start >/dev/null
  echo "[local] PostgreSQL iniciado en 127.0.0.1:$PUERTO"
fi
if [ "${1:-}" = "reiniciar" ]; then pg -d postgres -c "DROP DATABASE IF EXISTS $BD"; fi
if ! pg -d postgres -Atc "SELECT 1 FROM pg_database WHERE datname='$BD'" | grep -q 1; then
  echo "[local] Creando $BD con sql/01_esquema.sql y sql/02_datos.sql ..."
  pg -d postgres -c "CREATE DATABASE $BD"
  pg -d "$BD" -f sql/01_esquema.sql
  pg -d "$BD" -f sql/02_datos.sql
fi
case "${1:-}" in
  reiniciar) echo "[local] Base reiniciada."; exit 0 ;;
  psql) exec "$PGBIN/psql" -h 127.0.0.1 -p "$PUERTO" -U "$USUARIO" "$BD" ;;
esac

[ -f dist/inf513-proyecto1.jar ] || ./compilar.sh >/dev/null
JAVA_OPTS="-Dbd.host=127.0.0.1 -Dbd.puerto=$PUERTO -Dbd.nombre=$BD -Dbd.usuario=$USUARIO -Dbd.password= \
-Dconsola.remitente=admin@carrera.test" exec ./ejecutar.sh consola
