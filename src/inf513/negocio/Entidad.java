package inf513.negocio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Describe una tabla gestionable por correo. Con el código de 3 letras se forman
 * los comandos: LIS + código (listar), INS (insertar), MOD (modificar), ELI (eliminar).
 */
public final class Entidad {

    /** Regla de negocio que involucra varios campos (ej: fecha_fin >= fecha_inicio). */
    public interface Regla {
        void validar(Map<String, Object> valores) throws ErrorValidacion;
    }

    private final String codigo;
    private final String tabla;
    private final String nombre;
    private final String casoDeUso;
    private final List<Campo> campos = new ArrayList<>();
    private final List<Regla> reglas = new ArrayList<>();
    private String[] llaves = {"id"};
    private boolean autoId = true;
    private boolean timestamps = true;
    private boolean soloLectura;
    private String[] columnasListado;
    private String orden = "id";

    public Entidad(String codigo, String tabla, String nombre, String casoDeUso) {
        this.codigo = codigo;
        this.tabla = tabla;
        this.nombre = nombre;
        this.casoDeUso = casoDeUso;
    }

    // ---- construcción fluida (usada solo por el Catálogo) ----
    public Entidad campos(Campo... c) {
        campos.addAll(Arrays.asList(c));
        return this;
    }

    public Entidad regla(Regla r) {
        reglas.add(r);
        return this;
    }

    /** Tabla intermedia con llave compuesta (sin id propio ni timestamps). */
    public Entidad llaveCompuesta(String... columnas) {
        this.llaves = columnas;
        this.autoId = false;
        this.timestamps = false;
        return this;
    }

    public Entidad sinTimestamps() {
        this.timestamps = false;
        return this;
    }

    public Entidad soloLectura() {
        this.soloLectura = true;
        return this;
    }

    public Entidad listado(String... columnas) {
        this.columnasListado = columnas;
        return this;
    }

    public Entidad ordenarPor(String orden) {
        this.orden = orden;
        return this;
    }

    // ---- consultas ----
    public String getCodigo() {
        return codigo;
    }

    public String getTabla() {
        return tabla;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCasoDeUso() {
        return casoDeUso;
    }

    public List<Campo> getCampos() {
        return Collections.unmodifiableList(campos);
    }

    public List<Regla> getReglas() {
        return reglas;
    }

    public String[] getLlaves() {
        return llaves;
    }

    public boolean isAutoId() {
        return autoId;
    }

    public boolean tieneTimestamps() {
        return timestamps;
    }

    public boolean isSoloLectura() {
        return soloLectura;
    }

    public boolean permiteModificar() {
        return !soloLectura && autoId;
    }

    public String getOrden() {
        return orden;
    }

    public Campo campo(String columna) {
        for (Campo c : campos) {
            if (c.getColumna().equalsIgnoreCase(columna)) {
                return c;
            }
        }
        return null;
    }

    /** Columnas que se muestran en LISxxx["*"] (las contraseñas nunca se listan). */
    public String[] getColumnasListado() {
        if (columnasListado != null) {
            return columnasListado;
        }
        List<String> cols = new ArrayList<>();
        if (autoId) {
            cols.add("id");
        }
        for (Campo c : campos) {
            if (c.getTipo() != TipoDato.CLAVE) {
                cols.add(c.getColumna());
            }
        }
        return cols.toArray(new String[0]);
    }

    /** Columnas del detalle de un registro: todas menos contraseñas. */
    public String[] getColumnasDetalle() {
        List<String> cols = new ArrayList<>();
        if (autoId) {
            cols.add("id");
        }
        for (Campo c : campos) {
            if (c.getTipo() != TipoDato.CLAVE) {
                cols.add(c.getColumna());
            }
        }
        if (timestamps) {
            cols.add("created_at");
            cols.add("updated_at");
        }
        return cols.toArray(new String[0]);
    }

    /** Lista de nombres de parámetros para la ayuda: ["nombre","email",...]. */
    public String firmaParametros(boolean conLlaves) {
        List<String> nombres = new ArrayList<>();
        if (conLlaves) {
            nombres.addAll(Arrays.asList(llaves));
        }
        for (Campo c : campos) {
            nombres.add(c.getColumna());
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < nombres.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(nombres.get(i)).append('"');
        }
        return sb.append(']').toString();
    }
}
