package inf513.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Configuración del sistema leída desde config.properties (UTF-8).
 * Cualquier clave puede sobrescribirse al ejecutar con -Dclave=valor
 * (así el modo local cambia la base de datos sin tocar el archivo).
 */
public final class Config {

    private static final Properties PROPS = new Properties();
    private static String origen = "(sin archivo)";

    private Config() {
    }

    public static void cargar(String ruta) throws IOException {
        File f = new File(ruta);
        if (!f.isFile()) {
            throw new FileNotFoundException("No existe el archivo de configuración: " + f.getAbsolutePath()
                    + " (copie config.example.properties como config.properties y complete sus datos)");
        }
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            PROPS.load(r);
        }
        origen = f.getAbsolutePath();
    }

    public static String origen() {
        return origen;
    }

    public static String get(String clave, String defecto) {
        String v = System.getProperty(clave);
        if (v == null) {
            v = PROPS.getProperty(clave);
        }
        return v == null ? defecto : v.trim();
    }

    public static String requerido(String clave) {
        String v = get(clave, null);
        if (v == null || v.isEmpty()) {
            throw new IllegalStateException("Falta la clave '" + clave + "' en " + origen);
        }
        return v;
    }

    public static int entero(String clave, int defecto) {
        String v = get(clave, null);
        if (v == null || v.isEmpty()) {
            return defecto;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("La clave '" + clave + "' debe ser un número entero: " + v);
        }
    }

    public static boolean booleano(String clave, boolean defecto) {
        String v = get(clave, null);
        if (v == null || v.isEmpty()) {
            return defecto;
        }
        return v.equalsIgnoreCase("true") || v.equalsIgnoreCase("si") || v.equals("1");
    }
}
