package inf513.negocio;

import inf513.datos.ConexionBD;
import inf513.datos.Repositorio;
import inf513.datos.Tabla;
import inf513.interprete.Comando;
import inf513.util.Config;
import inf513.util.Log;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

/**
 * Capa de negocio: interpreta el comando ya analizado, valida los parámetros y
 * ejecuta la operación en la base de datos dentro de una transacción (la operación
 * y su registro en la bitácora se confirman juntos o se revierten juntos).
 */
public class ServicioComandos {

    private static final String[] ACCIONES = {"LIS", "INS", "MOD", "ELI"};

    private interface Operacion {
        Resultado ejecutar(Connection cn) throws SQLException, ErrorValidacion;
    }

    private final int maxFilas;
    private final boolean soloRegistrados;

    public ServicioComandos() {
        this.maxFilas = Config.entero("respuesta.max_filas", 200);
        this.soloRegistrados = Config.booleano("seguridad.solo_usuarios_registrados", false);
    }

    public Resultado ejecutar(Comando cmd, String remitente) {
        String n = cmd.getNombre();
        try {
            if (n.equals("AYUDA") || n.equals("HELP")) {
                return Ayuda.generar(cmd);
            }
            if (n.equals("BUSCAR")) {
                return enTransaccion(cn -> Reportes.buscar(cn, cmd, maxFilas));
            }
            if (n.startsWith("REP") && Reportes.existe(n)) {
                return enTransaccion(cn -> Reportes.ejecutar(cn, cmd));
            }
            if (n.length() == 6) {
                Entidad e = Catalogo.buscar(n.substring(3));
                String accion = n.substring(0, 3);
                if (e != null) {
                    switch (accion) {
                        case "LIS":
                            return enTransaccion(cn -> listar(cn, e, cmd));
                        case "INS":
                            return enTransaccion(cn -> insertar(cn, e, cmd, remitente));
                        case "MOD":
                            return enTransaccion(cn -> modificar(cn, e, cmd, remitente));
                        case "ELI":
                            return enTransaccion(cn -> eliminar(cn, e, cmd, remitente));
                        default:
                            break;
                    }
                }
            }
            return comandoDesconocido(n);
        } catch (ErrorValidacion ex) {
            return Resultado.error(ex.getMessage(), "Envíe AYUDA[\"" + codigoAyuda(n) + "\"] para ver el formato correcto.");
        } catch (SQLException ex) {
            Log.error("SQL al ejecutar " + textoSeguro(cmd), ex);
            return traducir(ex);
        } catch (RuntimeException ex) {
            Log.error("Error interno al ejecutar " + textoSeguro(cmd), ex);
            return Resultado.error("Error interno del sistema al procesar " + n, ex.toString());
        }
    }

    /** Texto del comando para los logs, ocultando contraseñas. */
    public static String textoSeguro(Comando cmd) {
        String n = cmd.getNombre();
        if (n.length() != 6) {
            return cmd.toString();
        }
        Entidad e = Catalogo.buscar(n.substring(3));
        String accion = n.substring(0, 3);
        if (e == null || !(accion.equals("INS") || accion.equals("MOD"))) {
            return cmd.toString();
        }
        int desplazamiento = accion.equals("MOD") ? 1 : 0;
        List<String> params = new ArrayList<>(cmd.getParametros());
        for (int i = 0; i < e.getCampos().size(); i++) {
            int pos = i + desplazamiento;
            if (e.getCampos().get(i).getTipo() == TipoDato.CLAVE && pos < params.size()) {
                params.set(pos, "******");
            }
        }
        return new Comando(n, params).toString();
    }

    // ------------------------------------------------------------------ LIS
    private Resultado listar(Connection cn, Entidad e, Comando cmd) throws SQLException, ErrorValidacion {
        List<String> p = cmd.getParametros();
        String nombre = cmd.getNombre();
        Tabla t;
        if (p.isEmpty() || (p.size() == 1 && p.get(0).trim().equals("*"))) {
            t = Repositorio.listar(cn, e, e.getColumnasListado(), null, null, false, null, maxFilas);
        } else if (p.size() == 1) {
            if (!e.isAutoId()) {
                throw new ErrorValidacion(nombre + " no busca por id porque la tabla tiene llave compuesta; use "
                        + nombre + "[\"*\"] o " + nombre + "[\"" + e.getLlaves()[0] + "\",\"valor\"]");
            }
            long id = Validador.entero(p.get(0), "Parámetro 1 (id)");
            t = Repositorio.detalle(cn, e, new long[]{id});
            if (t == null) {
                throw new ErrorValidacion("No existe un registro en " + e.getNombre() + " con id = " + id);
            }
            return Resultado.exito("Éxito: Consulta realizada correctamente", "Registro " + id + " de " + e.getNombre() + ".").tabla(t);
        } else if (p.size() == 2) {
            String col = p.get(0).trim().toLowerCase();
            Campo c = e.campo(col);
            boolean esLlave = col.equals("id") || java.util.Arrays.asList(e.getLlaves()).contains(col);
            if ((c == null && !esLlave) || (c != null && c.getTipo() == TipoDato.CLAVE)) {
                throw new ErrorValidacion("No se puede filtrar " + e.getNombre() + " por '" + p.get(0)
                        + "'. Campos válidos: " + String.join(", ", e.getColumnasListado()));
            }
            Object valor;
            boolean parcial;
            TipoDato tipo;
            if (c == null || (c.getTipo() == TipoDato.ENTERO && esLlave)) {
                valor = Validador.entero(p.get(1), "Parámetro 2 (" + col + ")");
                parcial = false;
                tipo = TipoDato.ENTERO;
            } else {
                tipo = c.getTipo();
                parcial = tipo.esTexto() && tipo != TipoDato.OPCION;
                valor = tipo == TipoDato.OPCION ? Validador.convertir(sinDefecto(c), p.get(1), 2) : Validador.convertirFiltro(c, p.get(1));
            }
            t = Repositorio.listar(cn, e, e.getColumnasListado(), col, valor, parcial, tipo, maxFilas);
        } else {
            throw new ErrorValidacion(nombre + " acepta 1 o 2 parámetros: " + nombre + "[\"*\"], "
                    + nombre + "[\"id\"] o " + nombre + "[\"campo\",\"valor\"] (se recibieron " + p.size() + ")");
        }
        return Resultado.exito("Éxito: Consulta realizada correctamente",
                "Se encontraron " + t.getFilas().size() + " registro(s) en " + e.getNombre() + ".").tabla(t);
    }

    // ------------------------------------------------------------------ INS
    private Resultado insertar(Connection cn, Entidad e, Comando cmd, String remitente) throws SQLException, ErrorValidacion {
        verificarEscritura(cn, e, cmd, remitente);
        List<Campo> campos = e.getCampos();
        exigirCantidad(cmd, campos.size(), e.firmaParametros(false));
        Map<String, Object> valores = convertirTodos(e, cmd.getParametros(), 0, false);
        for (Entidad.Regla r : e.getReglas()) {
            r.validar(valores);
        }
        generarSlugs(cn, e, valores, null);
        Long id = Repositorio.insertar(cn, e, valores);

        long[] llaves = e.isAutoId() ? new long[]{id} : valoresLlave(e, valores);
        String ref = e.isAutoId() ? "id = " + id : describirLlaves(e, llaves);
        bitacora(cn, remitente, "crear", e, cmd.getNombre() + ": registro creado (" + ref + ")", id);
        return Resultado.exito("Éxito: Registro insertado correctamente",
                "Se registró en " + e.getNombre() + " con " + ref + ".")
                .tabla(e.isAutoId() ? Repositorio.detalle(cn, e, llaves) : null);
    }

    // ------------------------------------------------------------------ MOD
    private Resultado modificar(Connection cn, Entidad e, Comando cmd, String remitente) throws SQLException, ErrorValidacion {
        if (!e.isSoloLectura() && !e.permiteModificar()) {
            throw new ErrorValidacion(cmd.getNombre() + " no existe: " + e.getNombre()
                    + " es una tabla de asignación; para cambiarla use ELI" + e.getCodigo() + " y luego INS" + e.getCodigo());
        }
        verificarEscritura(cn, e, cmd, remitente);
        exigirCantidad(cmd, 1 + e.getCampos().size(), e.firmaParametros(true));
        long id = Validador.entero(cmd.param(0), "Parámetro 1 (id)");
        Map<String, Object> valores = convertirTodos(e, cmd.getParametros(), 1, true);
        for (Entidad.Regla r : e.getReglas()) {
            r.validar(valores);
        }
        if (Repositorio.actualizar(cn, e, id, valores) == 0) {
            throw new ErrorValidacion("No existe un registro en " + e.getNombre() + " con id = " + id);
        }
        bitacora(cn, remitente, "editar", e, cmd.getNombre() + ": registro modificado (id = " + id + ")", id);
        return Resultado.exito("Éxito: Registro modificado correctamente",
                "Se actualizó el registro " + id + " de " + e.getNombre() + ".")
                .tabla(Repositorio.detalle(cn, e, new long[]{id}));
    }

    // ------------------------------------------------------------------ ELI
    private Resultado eliminar(Connection cn, Entidad e, Comando cmd, String remitente) throws SQLException, ErrorValidacion {
        verificarEscritura(cn, e, cmd, remitente);
        String[] nombresLlave = e.getLlaves();
        exigirCantidad(cmd, nombresLlave.length, firma(nombresLlave));
        long[] llaves = new long[nombresLlave.length];
        for (int i = 0; i < llaves.length; i++) {
            llaves[i] = Validador.entero(cmd.param(i), "Parámetro " + (i + 1) + " (" + nombresLlave[i] + ")");
        }
        Tabla antes = e.isAutoId() ? Repositorio.detalle(cn, e, llaves) : null;
        if (Repositorio.eliminar(cn, e, llaves) == 0) {
            throw new ErrorValidacion("No existe un registro en " + e.getNombre() + " con " + describirLlaves(e, llaves));
        }
        Long id = e.isAutoId() ? llaves[0] : null;
        bitacora(cn, remitente, "eliminar", e, cmd.getNombre() + ": registro eliminado (" + describirLlaves(e, llaves) + ")", id);
        Tabla eliminado = null;
        if (antes != null) {
            eliminado = new Tabla("Datos del registro eliminado", antes.getColumnas());
            for (String[] f : antes.getFilas()) {
                eliminado.fila(f);
            }
        }
        return Resultado.exito("Éxito: Registro eliminado correctamente",
                "Se eliminó de " + e.getNombre() + " el registro con " + describirLlaves(e, llaves) + ".")
                .tabla(eliminado);
    }

    // ------------------------------------------------------------------ apoyo
    private Resultado enTransaccion(Operacion op) throws SQLException, ErrorValidacion {
        try (Connection cn = ConexionBD.abrir()) {
            cn.setAutoCommit(false);
            try {
                Resultado r = op.ejecutar(cn);
                cn.commit();
                return r;
            } catch (SQLException | ErrorValidacion | RuntimeException ex) {
                cn.rollback();
                throw ex;
            }
        }
    }

    private Map<String, Object> convertirTodos(Entidad e, List<String> params, int desplazamiento, boolean modificar)
            throws ErrorValidacion {
        Map<String, Object> valores = new LinkedHashMap<>();
        List<Campo> campos = e.getCampos();
        for (int i = 0; i < campos.size(); i++) {
            Campo c = campos.get(i);
            String crudo = params.get(i + desplazamiento);
            boolean vacio = crudo == null || crudo.trim().isEmpty();
            if (modificar && vacio && (c.getTipo() == TipoDato.CLAVE || c.getTipo() == TipoDato.SLUG)) {
                continue; // en MOD, contraseña o slug vacíos = se conservan los actuales
            }
            valores.put(c.getColumna(), Validador.convertir(c, crudo, i + 1 + desplazamiento));
        }
        return valores;
    }

    private void generarSlugs(Connection cn, Entidad e, Map<String, Object> valores, Long excluirId) throws SQLException {
        for (Campo c : e.getCampos()) {
            if (c.getTipo() != TipoDato.SLUG || valores.get(c.getColumna()) != null) {
                continue;
            }
            String base = Validador.slugificar(String.valueOf(valores.get(c.getOrigenSlug())));
            if (base.length() > c.getMaximo() - 5) {
                base = base.substring(0, c.getMaximo() - 5).replaceAll("-+$", "");
            }
            String slug = base;
            int k = 2;
            while (Repositorio.existe(cn, e.getTabla(), c.getColumna(), slug, excluirId)) {
                slug = base + "-" + k++;
            }
            valores.put(c.getColumna(), slug);
        }
    }

    private void verificarEscritura(Connection cn, Entidad e, Comando cmd, String remitente)
            throws SQLException, ErrorValidacion {
        if (e.isSoloLectura()) {
            throw new ErrorValidacion(e.getNombre() + " es de solo lectura: solo admite LIS" + e.getCodigo()
                    + " (la llena el sistema automáticamente)");
        }
        if (soloRegistrados && Repositorio.idUsuarioPorCorreo(cn, remitente, true) == null) {
            throw new ErrorValidacion("El remitente " + remitente + " no es un usuario activo del sistema; "
                    + "solo los usuarios registrados pueden ejecutar " + cmd.getNombre().substring(0, 3));
        }
    }

    private void bitacora(Connection cn, String remitente, String accion, Entidad e, String descripcion, Long id)
            throws SQLException {
        Long userId = Repositorio.idUsuarioPorCorreo(cn, remitente, false);
        Repositorio.registrarBitacora(cn, userId, accion, e.getNombre(), descripcion, id, "correo:" + remitente);
    }

    private static void exigirCantidad(Comando cmd, int esperados, String firma) throws ErrorValidacion {
        if (cmd.cantidad() != esperados) {
            throw new ErrorValidacion(cmd.getNombre() + " requiere exactamente " + esperados + " parámetro(s) "
                    + firma + " y se recibieron " + cmd.cantidad());
        }
    }

    private static long[] valoresLlave(Entidad e, Map<String, Object> valores) {
        long[] r = new long[e.getLlaves().length];
        for (int i = 0; i < r.length; i++) {
            r[i] = (Long) valores.get(e.getLlaves()[i]);
        }
        return r;
    }

    private static String describirLlaves(Entidad e, long[] llaves) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < llaves.length; i++) {
            sb.append(i > 0 ? ", " : "").append(e.getLlaves()[i]).append(" = ").append(llaves[i]);
        }
        return sb.toString();
    }

    private static String firma(String[] nombres) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < nombres.length; i++) {
            sb.append(i > 0 ? "," : "").append('"').append(nombres[i]).append('"');
        }
        return sb.append(']').toString();
    }

    private static Campo sinDefecto(Campo c) {
        return Campo.opcion(c.getColumna(), c.getOpciones()).obligatorio();
    }

    private static String codigoAyuda(String nombre) {
        if (nombre.length() == 6 && Catalogo.buscar(nombre.substring(3)) != null) {
            return nombre.substring(3);
        }
        return nombre.startsWith("REP") || nombre.equals("BUSCAR") ? "REP" : "*";
    }

    private Resultado comandoDesconocido(String n) {
        StringBuilder detalle = new StringBuilder();
        if (n.length() == 6 && Catalogo.buscar(n.substring(3)) != null) {
            detalle.append("La acción '").append(n, 0, 3).append("' no existe. Acciones válidas: LIS (listar), INS (insertar), ")
                    .append("MOD (modificar) y ELI (eliminar), ej: LIS").append(n.substring(3)).append("[\"*\"].");
        } else {
            String sugerido = sugerir(n);
            if (sugerido != null) {
                detalle.append("¿Quiso decir ").append(sugerido).append("? ");
            }
            detalle.append("Envíe AYUDA[\"*\"] para ver la lista de comandos disponibles.");
        }
        return Resultado.error("Comando desconocido: '" + n + "'", detalle.toString());
    }

    /** Sugiere el comando válido más parecido (distancia de edición ≤ 2). */
    private static String sugerir(String n) {
        List<String> validos = new ArrayList<>();
        for (Entidad e : Catalogo.todas()) {
            for (String a : ACCIONES) {
                validos.add(a + e.getCodigo());
            }
        }
        validos.add("AYUDA");
        validos.add("BUSCAR");
        validos.addAll(Reportes.nombres());
        String mejor = null;
        int dist = 3;
        for (String v : validos) {
            int d = distancia(n, v);
            if (d < dist) {
                dist = d;
                mejor = v;
            }
        }
        return mejor;
    }

    private static int distancia(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + costo);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }

    /** Traduce errores de PostgreSQL (SQLSTATE) a mensajes entendibles para el usuario. */
    static Resultado traducir(SQLException ex) {
        String estado = ex.getSQLState() == null ? "" : ex.getSQLState();
        String mensaje = ex.getMessage();
        String detalle = null;
        if (ex instanceof PSQLException && ((PSQLException) ex).getServerErrorMessage() != null) {
            ServerErrorMessage sem = ((PSQLException) ex).getServerErrorMessage();
            mensaje = sem.getMessage();
            detalle = sem.getDetail();
        }
        String extra = (detalle != null ? detalle + " " : "") + "[SQLSTATE " + estado + ": " + mensaje + "]";
        switch (estado) {
            case "23505":
                return Resultado.error("Violación de unicidad: ya existe un registro con ese valor", extra);
            case "23503":
                return Resultado.error("Violación de integridad referencial: el registro relacionado no existe "
                        + "o el registro está siendo usado por otros datos", extra);
            case "23502":
                return Resultado.error("Falta un valor obligatorio", extra);
            case "23514":
                return Resultado.error("Un valor no cumple una restricción de la tabla", extra);
            case "22001":
                return Resultado.error("Un valor excede el tamaño permitido por la columna", extra);
            case "22003":
                return Resultado.error("Un número está fuera del rango permitido por la columna", extra);
            case "42P01":
                return Resultado.error("La tabla no existe en la base de datos (ejecute sql/01_esquema.sql)", extra);
            case "3D000":
                return Resultado.error("La base de datos configurada no existe", extra);
            default:
                if (estado.startsWith("08")) {
                    return Resultado.error("No se pudo conectar con la base de datos", extra);
                }
                if (estado.startsWith("28")) {
                    return Resultado.error("Credenciales de la base de datos inválidas", extra);
                }
                return Resultado.error("Fallo en la base de datos", extra);
        }
    }
}
