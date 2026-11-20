package inf513.datos;

import inf513.negocio.Campo;
import inf513.negocio.Entidad;
import inf513.negocio.TipoDato;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos genérico. TODAS las sentencias usan PreparedStatement con
 * parámetros "?" (los valores del correo nunca se concatenan al SQL). Los nombres
 * de tabla/columna salen exclusivamente del Catálogo (lista blanca), no del correo.
 */
public final class Repositorio {

    private static final int MAX_TEXTO_LISTADO = 120;

    private Repositorio() {
    }

    /** INSERT ... RETURNING id (o sin RETURNING para tablas de llave compuesta). */
    public static Long insertar(Connection cn, Entidad e, Map<String, Object> valores) throws SQLException {
        List<String> cols = new ArrayList<>(valores.keySet());
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(e.getTabla()).append(" (");
        StringBuilder marcas = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            sql.append(i > 0 ? ", " : "").append(cols.get(i));
            marcas.append(i > 0 ? ", " : "").append('?');
        }
        if (e.tieneTimestamps()) {
            sql.append(", created_at, updated_at");
            marcas.append(", now(), now()");
        }
        sql.append(") VALUES (").append(marcas).append(')');
        if (e.isAutoId()) {
            sql.append(" RETURNING id");
        }
        try (PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            for (int i = 0; i < cols.size(); i++) {
                asignar(ps, i + 1, valores.get(cols.get(i)), tipoDe(e, cols.get(i)));
            }
            if (e.isAutoId()) {
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
            ps.executeUpdate();
            return null;
        }
    }

    /** UPDATE tabla SET c1 = ?, ... WHERE id = ? -> filas afectadas. */
    public static int actualizar(Connection cn, Entidad e, long id, Map<String, Object> valores) throws SQLException {
        List<String> cols = new ArrayList<>(valores.keySet());
        StringBuilder sql = new StringBuilder("UPDATE ").append(e.getTabla()).append(" SET ");
        for (int i = 0; i < cols.size(); i++) {
            sql.append(i > 0 ? ", " : "").append(cols.get(i)).append(" = ?");
        }
        if (e.tieneTimestamps()) {
            sql.append(cols.isEmpty() ? "" : ", ").append("updated_at = now()");
        }
        sql.append(" WHERE id = ?");
        try (PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            int i = 1;
            for (String c : cols) {
                asignar(ps, i++, valores.get(c), tipoDe(e, c));
            }
            ps.setLong(i, id);
            return ps.executeUpdate();
        }
    }

    /** DELETE por llave (simple o compuesta) -> filas afectadas. */
    public static int eliminar(Connection cn, Entidad e, long[] llaves) throws SQLException {
        String sql = "DELETE FROM " + e.getTabla() + " WHERE " + condicionLlaves(e);
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < llaves.length; i++) {
                ps.setLong(i + 1, llaves[i]);
            }
            return ps.executeUpdate();
        }
    }

    /**
     * SELECT de listado. Filtro opcional: si 'parcial' es true se usa ILIKE '%valor%'
     * (texto), si no, igualdad exacta con el valor ya tipado.
     */
    public static Tabla listar(Connection cn, Entidad e, String[] columnas, String colFiltro, Object valorFiltro,
            boolean parcial, TipoDato tipoFiltro, int limite) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT ").append(String.join(", ", columnas))
                .append(" FROM ").append(e.getTabla());
        if (colFiltro != null) {
            sql.append(" WHERE ").append(parcial ? colFiltro + "::text ILIKE ? ESCAPE '\\'" : colFiltro + " = ?");
        }
        sql.append(" ORDER BY ").append(e.getOrden()).append(" LIMIT ?");
        try (PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            int i = 1;
            if (colFiltro != null) {
                if (parcial) {
                    ps.setString(i++, "%" + escaparLike(String.valueOf(valorFiltro)) + "%");
                } else {
                    asignar(ps, i++, valorFiltro, tipoFiltro);
                }
            }
            ps.setInt(i, limite + 1); // uno de más para saber si hay más filas
            return volcar(ps, e.getNombre(), limite, true);
        }
    }

    /** Un registro por su llave, en formato vertical (campo | valor). */
    public static Tabla detalle(Connection cn, Entidad e, long[] llaves) throws SQLException {
        String[] cols = e.getColumnasDetalle();
        String sql = "SELECT " + String.join(", ", cols) + " FROM " + e.getTabla() + " WHERE " + condicionLlaves(e);
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < llaves.length; i++) {
                ps.setLong(i + 1, llaves[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Tabla t = new Tabla(e.getNombre() + " - detalle", "Campo", "Valor");
                for (int i = 0; i < cols.length; i++) {
                    t.fila(cols[i], formatear(rs.getObject(i + 1), false));
                }
                return t;
            }
        }
    }

    /** Consulta libre (solo SQL fijo escrito en el código, con parámetros "?"). */
    public static Tabla consultar(Connection cn, String titulo, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return volcar(ps, titulo, Integer.MAX_VALUE, true);
        }
    }

    public static long contar(Connection cn, String tabla) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT count(*) FROM " + tabla);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    public static boolean existe(Connection cn, String tabla, String columna, Object valor, Long excluirId) throws SQLException {
        String sql = "SELECT 1 FROM " + tabla + " WHERE " + columna + " = ?" + (excluirId != null ? " AND id <> ?" : "");
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, valor);
            if (excluirId != null) {
                ps.setLong(2, excluirId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static Long idUsuarioPorCorreo(Connection cn, String correo, boolean soloActivos) throws SQLException {
        String sql = "SELECT id FROM users WHERE lower(email) = lower(?)" + (soloActivos ? " AND activo" : "");
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    public static void registrarBitacora(Connection cn, Long userId, String accion, String modulo,
            String descripcion, Long registroId, String agente) throws SQLException {
        String sql = "INSERT INTO bitacora (user_id, accion, modulo, descripcion, registro_id, ip, agente, created_at) "
                + "VALUES (?, ?, ?, ?, ?, NULL, ?, now())";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            asignar(ps, 1, userId, TipoDato.ENTERO);
            ps.setString(2, accion);
            ps.setString(3, recortar(modulo, 60));
            ps.setString(4, recortar(descripcion, 500));
            asignar(ps, 5, registroId, TipoDato.ENTERO);
            ps.setString(6, recortar(agente, 255));
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static Tabla volcar(PreparedStatement ps, String titulo, int limite, boolean recortarTextos) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            List<String> cols = new ArrayList<>();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                cols.add(md.getColumnLabel(i));
            }
            Tabla t = new Tabla(titulo, cols);
            int n = 0;
            while (rs.next()) {
                if (n == limite) {
                    t.nota("Se muestran los primeros " + limite + " registros. Use un filtro, ej: LIS…[\"campo\",\"valor\"]");
                    break;
                }
                String[] fila = new String[cols.size()];
                for (int i = 0; i < fila.length; i++) {
                    fila[i] = formatear(rs.getObject(i + 1), recortarTextos);
                }
                t.fila(fila);
                n++;
            }
            return t;
        }
    }

    private static String condicionLlaves(Entidad e) {
        StringBuilder sb = new StringBuilder();
        for (String l : e.getLlaves()) {
            sb.append(sb.length() > 0 ? " AND " : "").append(l).append(" = ?");
        }
        return sb.toString();
    }

    private static TipoDato tipoDe(Entidad e, String columna) {
        Campo c = e.campo(columna);
        return c == null ? TipoDato.TEXTO : c.getTipo();
    }

    static void asignar(PreparedStatement ps, int i, Object v, TipoDato tipo) throws SQLException {
        if (v == null) {
            ps.setNull(i, tipo == null ? Types.VARCHAR : tipo.tipoSql());
        } else if (v instanceof Long) {
            ps.setLong(i, (Long) v);
        } else if (v instanceof Boolean) {
            ps.setBoolean(i, (Boolean) v);
        } else if (v instanceof BigDecimal) {
            ps.setBigDecimal(i, (BigDecimal) v);
        } else if (v instanceof java.sql.Date) {
            ps.setDate(i, (java.sql.Date) v);
        } else if (v instanceof java.sql.Time) {
            ps.setTime(i, (java.sql.Time) v);
        } else {
            ps.setString(i, v.toString());
        }
    }

    static String formatear(Object v, boolean recortar) {
        if (v == null) {
            return "";
        }
        if (v instanceof Boolean) {
            return (Boolean) v ? "Sí" : "No";
        }
        if (v instanceof Timestamp) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm").format((Timestamp) v);
        }
        if (v instanceof java.sql.Time) {
            return v.toString().substring(0, 5);
        }
        if (v instanceof BigDecimal) {
            return ((BigDecimal) v).stripTrailingZeros().toPlainString();
        }
        String s = v.toString();
        return recortar && s.length() > MAX_TEXTO_LISTADO ? s.substring(0, MAX_TEXTO_LISTADO) + "…" : s;
    }

    static String escaparLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String recortar(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
