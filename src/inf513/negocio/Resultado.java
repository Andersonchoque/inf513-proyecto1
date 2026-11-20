package inf513.negocio;

import inf513.datos.Tabla;
import java.util.ArrayList;
import java.util.List;

/** Resultado de un comando: éxito o error, un mensaje y tablas opcionales. */
public final class Resultado {

    private final boolean exito;
    private final String titulo;
    private final String mensaje;
    private final List<Tabla> tablas = new ArrayList<>();

    private Resultado(boolean exito, String titulo, String mensaje) {
        this.exito = exito;
        this.titulo = titulo;
        this.mensaje = mensaje;
    }

    /** titulo con el formato del enunciado, ej: "Éxito: Registro insertado correctamente". */
    public static Resultado exito(String titulo, String mensaje) {
        return new Resultado(true, titulo, mensaje);
    }

    /** Se responde como "Error: [descripción del fallo]". */
    public static Resultado error(String descripcion, String detalle) {
        return new Resultado(false, "Error: " + descripcion, detalle);
    }

    public Resultado tabla(Tabla t) {
        if (t != null) {
            tablas.add(t);
        }
        return this;
    }

    public boolean isExito() {
        return exito;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public List<Tabla> getTablas() {
        return tablas;
    }

    /** Versión en texto plano (modo consola). */
    public String comoTexto() {
        StringBuilder sb = new StringBuilder();
        sb.append(exito ? "[OK] " : "[ERROR] ").append(titulo).append('\n');
        if (mensaje != null && !mensaje.isEmpty()) {
            sb.append("     ").append(mensaje.replace("\n", "\n     ")).append('\n');
        }
        for (Tabla t : tablas) {
            sb.append('\n').append("  ").append(t.getTitulo()).append('\n');
            int[] ancho = new int[t.getColumnas().size()];
            for (int i = 0; i < ancho.length; i++) {
                ancho[i] = Math.min(40, t.getColumnas().get(i).length());
            }
            for (String[] f : t.getFilas()) {
                for (int i = 0; i < ancho.length && i < f.length; i++) {
                    ancho[i] = Math.min(40, Math.max(ancho[i], celda(f[i]).length()));
                }
            }
            sb.append("  ").append(linea(t.getColumnas().toArray(new String[0]), ancho)).append('\n');
            StringBuilder sep = new StringBuilder("  ");
            for (int a : ancho) {
                sep.append(repetir('-', a)).append("-+-");
            }
            sb.append(sep, 0, sep.length() - 2).append('\n');
            for (String[] f : t.getFilas()) {
                sb.append("  ").append(linea(f, ancho)).append('\n');
            }
            if (t.getFilas().isEmpty()) {
                sb.append("  (sin registros)\n");
            }
            if (t.getNota() != null) {
                sb.append("  * ").append(t.getNota()).append('\n');
            }
        }
        return sb.toString();
    }

    private static String linea(String[] valores, int[] ancho) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ancho.length; i++) {
            String v = i < valores.length ? celda(valores[i]) : "";
            if (v.length() > ancho[i]) {
                v = v.substring(0, ancho[i] - 1) + "…";
            }
            sb.append(v).append(repetir(' ', ancho[i] - v.length()));
            if (i < ancho.length - 1) {
                sb.append(" | ");
            }
        }
        return sb.toString();
    }

    private static String celda(String s) {
        return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ');
    }

    private static String repetir(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
