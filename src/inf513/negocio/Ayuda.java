package inf513.negocio;

import inf513.datos.Tabla;
import inf513.interprete.Comando;
import java.util.List;
import java.util.Map;

/** Comando AYUDA: se genera a partir del Catálogo, así nunca queda desactualizada. */
public final class Ayuda {

    private Ayuda() {
    }

    public static Resultado generar(Comando cmd) throws ErrorValidacion {
        if (cmd.cantidad() > 1) {
            throw new ErrorValidacion("AYUDA acepta 0 o 1 parámetro: AYUDA[\"*\"] o AYUDA[\"USU\"]");
        }
        String p = cmd.cantidad() == 0 ? "*" : cmd.param(0).trim().toUpperCase();
        if (p.equals("*") || p.isEmpty()) {
            return general();
        }
        if (p.equals("REP") || p.startsWith("REP") && Reportes.existe(p)) {
            return reportes();
        }
        String codigo = p.length() == 6 ? p.substring(3) : p;
        Entidad e = Catalogo.buscar(codigo);
        if (e == null) {
            throw new ErrorValidacion("No existe la tabla con código '" + p + "'. Envíe AYUDA[\"*\"] para ver los códigos");
        }
        return detalle(e);
    }

    private static Resultado general() {
        Resultado r = Resultado.exito("Éxito: Ayuda del sistema",
                "Escriba UN comando en el ASUNTO del correo con el formato COMANDO[\"param1\",\"param2\",...]. "
                        + "Cada parámetro va entre comillas dobles (use \\\" para una comilla dentro del texto). "
                        + "Los comandos se forman con una acción + el código de la tabla: LIS (listar), INS (insertar), "
                        + "MOD (modificar), ELI (eliminar). Ej: LISUSU[\"*\"], LISUSU[\"5\"], LISUSU[\"email\",\"gmail\"]. "
                        + "Para ver los parámetros de una tabla envíe AYUDA[\"código\"], ej: AYUDA[\"USU\"].");
        for (String[] cu : Catalogo.CASOS_DE_USO) {
            List<Entidad> entidades = Catalogo.delCasoDeUso(cu[0]);
            if (entidades.isEmpty()) {
                continue;
            }
            Tabla t = new Tabla(cu[0] + " - " + cu[1], "Código", "Tabla", "Comandos", "Parámetros de INS");
            for (Entidad e : entidades) {
                t.fila(e.getCodigo(), e.getNombre(), comandos(e), e.isSoloLectura() ? "(solo lectura)" : e.firmaParametros(false));
            }
            r.tabla(t);
        }
        Tabla esp = new Tabla("CU7 / CU8 - Búsqueda y reportes", "Comando", "Descripción");
        esp.fila("BUSCAR[\"texto\"]", "Busca en noticias, eventos, páginas, documentos, proyectos, docentes, materias, FAQ y convenios");
        for (Map.Entry<String, String[]> rep : Reportes.catalogo().entrySet()) {
            esp.fila(rep.getKey() + rep.getValue()[0], rep.getValue()[1]);
        }
        esp.fila("AYUDA[\"código\"]", "Parámetros detallados y ejemplos de una tabla, ej: AYUDA[\"MAT\"]");
        r.tabla(esp);
        return r;
    }

    private static Resultado detalle(Entidad e) {
        Resultado r = Resultado.exito("Éxito: Ayuda de " + e.getNombre() + " (" + e.getCodigo() + ")",
                e.getCasoDeUso() + " · tabla '" + e.getTabla() + "' · comandos: " + comandos(e)
                        + ". En INS/MOD envíe todos los parámetros en orden; un parámetro opcional puede ir vacío \"\""
                        + (tieneTipo(e, TipoDato.CLAVE) || tieneTipo(e, TipoDato.SLUG)
                        ? ". En MOD, contraseña o slug vacíos conservan el valor actual." : "."));
        Tabla campos = new Tabla("Parámetros", "N°", "Campo", "Tipo", "Obligatorio", "Por defecto");
        int i = 1;
        for (Campo c : e.getCampos()) {
            campos.fila(String.valueOf(i++), c.getColumna(), c.describirTipo(), c.isObligatorio() ? "Sí" : "No",
                    c.getPorDefecto() == null ? "" : c.getPorDefecto());
        }
        r.tabla(campos);
        Tabla ej = new Tabla("Ejemplos", "Acción", "Asunto del correo");
        String cod = e.getCodigo();
        ej.fila("Listar todo", "LIS" + cod + "[\"*\"]");
        if (e.isAutoId()) {
            ej.fila("Ver un registro", "LIS" + cod + "[\"1\"]");
        }
        Campo filtro = primerTexto(e);
        if (filtro != null) {
            ej.fila("Filtrar", "LIS" + cod + "[\"" + filtro.getColumna() + "\",\"texto\"]");
        }
        if (!e.isSoloLectura()) {
            ej.fila("Insertar", "INS" + cod + ejemplos(e, false));
            if (e.permiteModificar()) {
                ej.fila("Modificar", "MOD" + cod + ejemplos(e, true));
            }
            StringBuilder eli = new StringBuilder("ELI" + cod + "[");
            for (int k = 0; k < e.getLlaves().length; k++) {
                eli.append(k > 0 ? "," : "").append("\"").append(k + 1).append("\"");
            }
            ej.fila("Eliminar", eli.append(']').toString());
        }
        r.tabla(ej);
        return r;
    }

    private static Resultado reportes() {
        Resultado r = Resultado.exito("Éxito: Ayuda de reportes (CU8) y búsqueda (CU7)",
                "Los reportes no modifican datos. Las fechas van en formato AAAA-MM-DD.");
        Tabla t = new Tabla("Reportes", "Comando", "Descripción");
        for (Map.Entry<String, String[]> rep : Reportes.catalogo().entrySet()) {
            t.fila(rep.getKey() + rep.getValue()[0], rep.getValue()[1]);
        }
        t.fila("BUSCAR[\"texto\"]", "Búsqueda global en los contenidos del sitio (mínimo 3 caracteres)");
        return r.tabla(t);
    }

    static String comandos(Entidad e) {
        String c = e.getCodigo();
        if (e.isSoloLectura()) {
            return "LIS" + c;
        }
        return "LIS" + c + ", INS" + c + (e.permiteModificar() ? ", MOD" + c : "") + ", ELI" + c;
    }

    private static String ejemplos(Entidad e, boolean conId) {
        StringBuilder sb = new StringBuilder("[");
        if (conId) {
            sb.append("\"1\"");
        }
        for (Campo c : e.getCampos()) {
            if (sb.length() > 1) {
                sb.append(',');
            }
            String v = c.getEjemplo();
            if (conId && (c.getTipo() == TipoDato.CLAVE || c.getTipo() == TipoDato.SLUG)) {
                v = "";
            }
            sb.append('"').append(v.replace("\"", "\\\"")).append('"');
        }
        return sb.append(']').toString();
    }

    private static Campo primerTexto(Entidad e) {
        for (Campo c : e.getCampos()) {
            if (c.getTipo() == TipoDato.TEXTO) {
                return c;
            }
        }
        return null;
    }

    private static boolean tieneTipo(Entidad e, TipoDato t) {
        for (Campo c : e.getCampos()) {
            if (c.getTipo() == t) {
                return true;
            }
        }
        return false;
    }
}
