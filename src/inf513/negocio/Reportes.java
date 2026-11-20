package inf513.negocio;

import inf513.datos.Repositorio;
import inf513.datos.Tabla;
import inf513.interprete.Comando;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** CU8 (reportes y estadísticas) y CU7 (búsqueda interna global). Solo SQL fijo con parámetros "?". */
public final class Reportes {

    /** nombre -> {parámetros, descripción} */
    private static final Map<String, String[]> REPORTES = new LinkedHashMap<>();

    static {
        REPORTES.put("REPGEN", new String[]{"[\"*\"]", "Resumen general: cantidad de registros por tabla y caso de uso"});
        REPORTES.put("REPVIS", new String[]{"[\"10\"]", "Páginas más visitadas (top N) y visitas de los últimos 7 días"});
        REPORTES.put("REPCON", new String[]{"[\"*\"] o [\"desde\",\"hasta\"]", "Consultas recibidas por estado y por categoría"});
        REPORTES.put("REPGRA", new String[]{"[\"*\"]", "Graduados por situación laboral, año y modalidad de titulación"});
        REPORTES.put("REPBIB", new String[]{"[\"10\"]", "Documentos más descargados de la biblioteca digital"});
        REPORTES.put("REPNOT", new String[]{"[\"*\"]", "Noticias por estado y categoría, y próximos eventos"});
        REPORTES.put("REPINV", new String[]{"[\"*\"]", "Proyectos de investigación por estado, tipo y línea"});
        REPORTES.put("REPBIT", new String[]{"[\"*\"] o [\"desde\",\"hasta\"]", "Actividad de la bitácora por módulo y acción"});
    }

    private Reportes() {
    }

    public static boolean existe(String nombre) {
        return REPORTES.containsKey(nombre);
    }

    public static List<String> nombres() {
        return new ArrayList<>(REPORTES.keySet());
    }

    public static Map<String, String[]> catalogo() {
        return REPORTES;
    }

    public static Resultado ejecutar(Connection cn, Comando cmd) throws SQLException, ErrorValidacion {
        String n = cmd.getNombre();
        Resultado r = Resultado.exito("Éxito: Reporte generado correctamente", REPORTES.get(n)[1] + ".");
        switch (n) {
            case "REPGEN":
                return generalPorTabla(cn, r);
            case "REPVIS": {
                int top = topN(cmd);
                r.tabla(Repositorio.consultar(cn, "Top " + top + " páginas más visitadas",
                        "SELECT ruta AS \"Página\", total AS \"Visitas\", ultima_visita AS \"Última visita\" "
                                + "FROM contador_visitas ORDER BY total DESC, ruta LIMIT ?", top));
                r.tabla(Repositorio.consultar(cn, "Visitas de los últimos 7 días",
                        "SELECT fecha AS \"Fecha\", sum(total) AS \"Visitas\" FROM visitas_diarias "
                                + "WHERE fecha >= current_date - 6 GROUP BY fecha ORDER BY fecha"));
                return r;
            }
            case "REPCON": {
                java.sql.Date[] rango = rango(cmd);
                r.tabla(Repositorio.consultar(cn, "Consultas por estado",
                        "SELECT estado AS \"Estado\", count(*) AS \"Cantidad\", "
                                + "round(100.0 * count(*) / sum(count(*)) OVER (), 1) AS \"%\" FROM consultas "
                                + "WHERE created_at >= ? AND created_at < ?::date + 1 GROUP BY estado ORDER BY 2 DESC",
                        rango[0], rango[1]));
                r.tabla(Repositorio.consultar(cn, "Consultas por categoría",
                        "SELECT coalesce(cc.nombre, '(sin categoría)') AS \"Categoría\", count(*) AS \"Cantidad\", "
                                + "sum(CASE WHEN c.estado IN ('nueva','en_revision') THEN 1 ELSE 0 END) AS \"Pendientes\" "
                                + "FROM consultas c LEFT JOIN categorias_consulta cc ON cc.id = c.categoria_consulta_id "
                                + "WHERE c.created_at >= ? AND c.created_at < ?::date + 1 GROUP BY 1 ORDER BY 2 DESC",
                        rango[0], rango[1]));
                return r;
            }
            case "REPGRA":
                r.tabla(Repositorio.consultar(cn, "Graduados por situación laboral",
                        "SELECT situacion_laboral AS \"Situación laboral\", count(*) AS \"Graduados\", "
                                + "round(100.0 * count(*) / sum(count(*)) OVER (), 1) AS \"%\" "
                                + "FROM graduados GROUP BY 1 ORDER BY 2 DESC"));
                r.tabla(Repositorio.consultar(cn, "Graduados por año de graduación",
                        "SELECT anio_graduacion AS \"Año\", count(*) AS \"Graduados\", "
                                + "sum(CASE WHEN situacion_laboral IN ('dependiente','independiente') THEN 1 ELSE 0 END) AS \"Trabajando\" "
                                + "FROM graduados GROUP BY 1 ORDER BY 1 DESC"));
                r.tabla(Repositorio.consultar(cn, "Graduados por modalidad de titulación",
                        "SELECT coalesce(modalidad_titulacion, '(no indicada)') AS \"Modalidad\", count(*) AS \"Graduados\" "
                                + "FROM graduados GROUP BY 1 ORDER BY 2 DESC"));
                return r;
            case "REPBIB": {
                int top = topN(cmd);
                r.tabla(Repositorio.consultar(cn, "Top " + top + " documentos más descargados",
                        "SELECT d.id AS \"Id\", d.titulo AS \"Documento\", c.nombre AS \"Categoría\", d.descargas AS \"Descargas\" "
                                + "FROM documentos d JOIN categorias_documento c ON c.id = d.categoria_documento_id "
                                + "ORDER BY d.descargas DESC, d.titulo LIMIT ?", top));
                r.tabla(Repositorio.consultar(cn, "Documentos por categoría",
                        "SELECT c.nombre AS \"Categoría\", count(d.id) AS \"Documentos\", coalesce(sum(d.descargas),0) AS \"Descargas\" "
                                + "FROM categorias_documento c LEFT JOIN documentos d ON d.categoria_documento_id = c.id "
                                + "GROUP BY c.nombre ORDER BY 3 DESC"));
                return r;
            }
            case "REPNOT":
                r.tabla(Repositorio.consultar(cn, "Noticias por estado",
                        "SELECT estado AS \"Estado\", count(*) AS \"Noticias\", sum(CASE WHEN destacado THEN 1 ELSE 0 END) AS \"Destacadas\" "
                                + "FROM noticias GROUP BY 1 ORDER BY 2 DESC"));
                r.tabla(Repositorio.consultar(cn, "Noticias por categoría",
                        "SELECT coalesce(c.nombre,'(sin categoría)') AS \"Categoría\", count(n.id) AS \"Noticias\" "
                                + "FROM noticias n LEFT JOIN categorias_noticia c ON c.id = n.categoria_noticia_id GROUP BY 1 ORDER BY 2 DESC"));
                r.tabla(Repositorio.consultar(cn, "Próximos eventos",
                        "SELECT fecha AS \"Fecha\", hora_inicio AS \"Hora\", titulo AS \"Evento\", tipo AS \"Tipo\", lugar AS \"Lugar\" "
                                + "FROM eventos WHERE fecha >= current_date ORDER BY fecha, hora_inicio LIMIT 10"));
                return r;
            case "REPINV":
                r.tabla(Repositorio.consultar(cn, "Proyectos por estado",
                        "SELECT estado AS \"Estado\", count(*) AS \"Proyectos\" FROM proyectos_investigacion GROUP BY 1 ORDER BY 2 DESC"));
                r.tabla(Repositorio.consultar(cn, "Proyectos por tipo",
                        "SELECT tipo AS \"Tipo\", count(*) AS \"Proyectos\" FROM proyectos_investigacion GROUP BY 1 ORDER BY 2 DESC"));
                r.tabla(Repositorio.consultar(cn, "Proyectos por línea de investigación",
                        "SELECT l.nombre AS \"Línea\", count(p.id) AS \"Proyectos\", "
                                + "sum(CASE WHEN p.estado = 'finalizado' THEN 1 ELSE 0 END) AS \"Finalizados\" "
                                + "FROM lineas_investigacion l LEFT JOIN proyectos_investigacion p ON p.linea_investigacion_id = l.id "
                                + "GROUP BY l.nombre ORDER BY 2 DESC"));
                return r;
            case "REPBIT": {
                java.sql.Date[] rango = rango(cmd);
                r.tabla(Repositorio.consultar(cn, "Operaciones por módulo y acción",
                        "SELECT modulo AS \"Módulo\", accion AS \"Acción\", count(*) AS \"Cantidad\" FROM bitacora "
                                + "WHERE created_at >= ? AND created_at < ?::date + 1 GROUP BY 1, 2 ORDER BY 3 DESC, 1",
                        rango[0], rango[1]));
                r.tabla(Repositorio.consultar(cn, "Últimas 15 operaciones",
                        "SELECT created_at AS \"Fecha\", accion AS \"Acción\", modulo AS \"Módulo\", descripcion AS \"Descripción\", "
                                + "agente AS \"Origen\" FROM bitacora WHERE created_at >= ? AND created_at < ?::date + 1 "
                                + "ORDER BY id DESC LIMIT 15", rango[0], rango[1]));
                return r;
            }
            default:
                throw new ErrorValidacion("Reporte desconocido: " + n);
        }
    }

    /** CU7: búsqueda en los contenidos públicos del sitio. */
    public static Resultado buscar(Connection cn, Comando cmd, int limite) throws SQLException, ErrorValidacion {
        if (cmd.cantidad() != 1) {
            throw new ErrorValidacion("BUSCAR requiere exactamente 1 parámetro: BUSCAR[\"texto\"] (se recibieron " + cmd.cantidad() + ")");
        }
        String texto = cmd.param(0).trim();
        if (texto.length() < 3) {
            throw new ErrorValidacion("El texto a buscar debe tener al menos 3 caracteres");
        }
        String patron = "%" + texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        String[][] fuentes = {
            {"Noticia", "NOT", "noticias", "titulo", "coalesce(resumen,'')"},
            {"Evento", "EVE", "eventos", "titulo", "coalesce(descripcion,'')"},
            {"Página", "PAG", "paginas", "titulo", "coalesce(resumen,'')"},
            {"Documento", "BIB", "documentos", "titulo", "coalesce(descripcion,'')"},
            {"Proyecto de investigación", "PRY", "proyectos_investigacion", "titulo", "coalesce(descripcion,'')"},
            {"Artículo INNOVAR", "ART", "revista_articulos", "titulo", "autores"},
            {"Docente", "DOC", "docentes", "nombre", "coalesce(especialidad,'')"},
            {"Materia", "MAT", "materias", "sigla || ' - ' || nombre", "sigla"},
            {"Pregunta frecuente", "FAQ", "preguntas_frecuentes", "pregunta", "respuesta"},
            {"Convenio", "CNV", "convenios", "institucion", "coalesce(finalidad,'')"},
        };
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        for (String[] f : fuentes) {
            sql.append(sql.length() > 0 ? " UNION ALL " : "")
                    .append("SELECT '").append(f[0]).append("' AS \"Tipo\", id AS \"Id\", ")
                    .append(f[3]).append(" AS \"Título\", 'LIS").append(f[1]).append("[\"' || id || '\"]' AS \"Ver detalle\" ")
                    .append("FROM ").append(f[2]).append(" WHERE ").append(f[3]).append(" ILIKE ? OR ").append(f[4]).append(" ILIKE ?");
            params.add(patron);
            params.add(patron);
        }
        sql.append(" LIMIT ").append(limite);
        Tabla t = Repositorio.consultar(cn, "Resultados para \"" + texto + "\"", sql.toString(), params.toArray());
        return Resultado.exito("Éxito: Búsqueda realizada correctamente",
                "Se encontraron " + t.getFilas().size() + " resultado(s) para \"" + texto + "\".").tabla(t);
    }

    private static Resultado generalPorTabla(Connection cn, Resultado r) throws SQLException {
        for (String[] cu : Catalogo.CASOS_DE_USO) {
            List<Entidad> entidades = Catalogo.delCasoDeUso(cu[0]);
            if (entidades.isEmpty()) {
                continue;
            }
            Tabla t = new Tabla(cu[0] + " - " + cu[1], "Código", "Tabla", "Descripción", "Registros");
            for (Entidad e : entidades) {
                t.fila(e.getCodigo(), e.getTabla(), e.getNombre(), String.valueOf(Repositorio.contar(cn, e.getTabla())));
            }
            r.tabla(t);
        }
        return r;
    }

    private static int topN(Comando cmd) throws ErrorValidacion {
        if (cmd.cantidad() == 0 || cmd.param(0).trim().equals("*")) {
            return 10;
        }
        long n = Validador.entero(cmd.param(0), "Parámetro 1 (cantidad)");
        if (n < 1 || n > 100) {
            throw new ErrorValidacion("Parámetro 1 (cantidad): debe estar entre 1 y 100");
        }
        return (int) n;
    }

    /** ["*"] = todo el historial; ["desde","hasta"] = rango de fechas inclusivo. */
    private static java.sql.Date[] rango(Comando cmd) throws ErrorValidacion {
        if (cmd.cantidad() == 0 || (cmd.cantidad() == 1 && cmd.param(0).trim().equals("*"))) {
            return new java.sql.Date[]{java.sql.Date.valueOf("1900-01-01"), java.sql.Date.valueOf("2999-12-31")};
        }
        if (cmd.cantidad() != 2) {
            throw new ErrorValidacion(cmd.getNombre() + " acepta [\"*\"] o [\"desde\",\"hasta\"] con fechas AAAA-MM-DD");
        }
        LocalDate desde = Validador.fecha(cmd.param(0), "Parámetro 1 (desde)");
        LocalDate hasta = Validador.fecha(cmd.param(1), "Parámetro 2 (hasta)");
        if (hasta.isBefore(desde)) {
            throw new ErrorValidacion("La fecha 'hasta' no puede ser anterior a 'desde'");
        }
        return new java.sql.Date[]{java.sql.Date.valueOf(desde), java.sql.Date.valueOf(hasta)};
    }
}
