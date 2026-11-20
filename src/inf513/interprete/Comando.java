package inf513.interprete;

import java.util.Collections;
import java.util.List;

/** Comando ya analizado: nombre en mayúsculas + parámetros en orden. */
public final class Comando {

    private final String nombre;
    private final List<String> parametros;

    public Comando(String nombre, List<String> parametros) {
        this.nombre = nombre;
        this.parametros = Collections.unmodifiableList(parametros);
    }

    public String getNombre() {
        return nombre;
    }

    public List<String> getParametros() {
        return parametros;
    }

    public int cantidad() {
        return parametros.size();
    }

    public String param(int i) {
        return parametros.get(i);
    }

    /** Vuelve a escribir el comando en formato canónico NOMBRE["p1","p2"]. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(nombre).append('[');
        for (int i = 0; i < parametros.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(parametros.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        return sb.append(']').toString();
    }
}
