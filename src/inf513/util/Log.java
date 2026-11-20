package inf513.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Registro de auditoría exigido por el proyecto:
 *  - logs/access.log : cada correo procesado, comando ejecutado y su resultado.
 *  - logs/error.log  : excepciones capturadas con su traza completa.
 * Todo se escribe en UTF-8 y también se muestra en consola.
 */
public final class Log {

    private static File directorio = new File("logs");
    private static final SimpleDateFormat FORMATO = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Log() {
    }

    public static synchronized void iniciar(String dir) {
        directorio = new File(dir);
        if (!directorio.isDirectory() && !directorio.mkdirs()) {
            System.err.println("No se pudo crear el directorio de logs: " + directorio.getAbsolutePath());
        }
    }

    public static File directorio() {
        return directorio;
    }

    /** Una línea por correo/comando procesado. */
    public static synchronized void acceso(String remitente, String asunto, String comando,
            String resultado, long ms, String detalle) {
        String linea = ahora()
                + " | " + limpiar(remitente)
                + " | " + limpiar(comando)
                + " | " + resultado
                + " | " + ms + " ms"
                + " | asunto=\"" + limpiar(asunto) + "\""
                + (detalle == null || detalle.isEmpty() ? "" : " | " + limpiar(detalle));
        escribir("access.log", linea);
        System.out.println("[ACCESO] " + linea);
    }

    /** Eventos del sistema (arranque, conexión al buzón, etc.). */
    public static synchronized void sistema(String mensaje) {
        String linea = ahora() + " | SISTEMA | " + limpiar(mensaje);
        escribir("access.log", linea);
        System.out.println("[SISTEMA] " + linea);
    }

    public static synchronized void error(String contexto, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String texto = ahora() + " | " + limpiar(contexto) + " | " + t + System.lineSeparator() + sw;
        escribir("error.log", texto.trim());
        System.err.println("[ERROR] " + ahora() + " | " + contexto + " | " + t);
    }

    public static synchronized void error(String contexto, String mensaje) {
        String linea = ahora() + " | " + limpiar(contexto) + " | " + limpiar(mensaje);
        escribir("error.log", linea);
        System.err.println("[ERROR] " + linea);
    }

    private static String ahora() {
        return FORMATO.format(new Date());
    }

    private static String limpiar(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
    }

    private static void escribir(String archivo, String linea) {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(new File(directorio, archivo), true), StandardCharsets.UTF_8)) {
            w.write(linea);
            w.write(System.lineSeparator());
        } catch (IOException e) {
            System.err.println("No se pudo escribir en " + archivo + ": " + e.getMessage());
        }
    }
}
