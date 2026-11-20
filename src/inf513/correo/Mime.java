package inf513.correo;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidades MIME mínimas (RFC 5322 / RFC 2047) para leer y escribir cabeceras:
 * desdoblado de líneas, decodificación de asuntos "=?UTF-8?B?...?=" y extracción
 * de la dirección del remitente.
 */
public final class Mime {

    private static final Pattern PALABRA_CODIFICADA =
            Pattern.compile("=\\?([^?\\s]+)\\?([bBqQ])\\?([^?\\s]*)\\?=");
    private static final Pattern DIRECCION_ANGULAR = Pattern.compile("<\\s*([^<>\\s]+@[^<>\\s]+)\\s*>");
    private static final Pattern DIRECCION_SUELTA = Pattern.compile("([^\\s<>\"(),;:]+@[^\\s<>\"(),;:]+)");

    private Mime() {
    }

    /** Convierte el bloque de cabeceras (texto leído como ISO-8859-1) en un correo. */
    public static Correo parsearCorreo(int numero, String bloque) {
        Map<String, String> cab = parsearCabeceras(bloque);
        String from = decodificar(cab.getOrDefault("from", ""));
        String asunto = decodificar(cab.getOrDefault("subject", ""));
        return new Correo(numero, extraerDireccion(from), extraerNombre(from), asunto,
                cab.getOrDefault("message-id", "").trim(), cab);
    }

    /** Cabeceras en minúsculas -> valor (primera aparición), con líneas plegadas unidas. */
    public static Map<String, String> parsearCabeceras(String bloque) {
        Map<String, String> mapa = new LinkedHashMap<>();
        String nombre = null;
        StringBuilder valor = new StringBuilder();
        for (String linea : bloque.split("\r?\n")) {
            if (linea.isEmpty()) {
                break; // fin de cabeceras
            }
            if ((linea.charAt(0) == ' ' || linea.charAt(0) == '\t') && nombre != null) {
                valor.append(' ').append(linea.trim()); // línea plegada
                continue;
            }
            if (nombre != null) {
                mapa.putIfAbsent(nombre, valor.toString());
            }
            int dosPuntos = linea.indexOf(':');
            if (dosPuntos <= 0) {
                nombre = null;
                continue;
            }
            nombre = linea.substring(0, dosPuntos).trim().toLowerCase();
            valor = new StringBuilder(linea.substring(dosPuntos + 1).trim());
        }
        if (nombre != null) {
            mapa.putIfAbsent(nombre, valor.toString());
        }
        return mapa;
    }

    /**
     * Decodifica un valor de cabecera: primero repara UTF-8 "crudo" (8 bits) y luego
     * las palabras codificadas RFC 2047 (Base64 o Quoted-Printable, cualquier charset).
     * Palabras consecutivas del mismo charset se unen antes de decodificar para no
     * romper caracteres multibyte partidos entre dos palabras.
     */
    public static String decodificar(String valor) {
        if (valor == null) {
            return "";
        }
        String texto = repararUtf8(valor);
        Matcher m = PALABRA_CODIFICADA.matcher(texto);
        StringBuilder sb = new StringBuilder();
        int ultimo = 0;
        String charsetPendiente = null;
        ByteArrayOutputStream bytesPendientes = new ByteArrayOutputStream();
        while (m.find()) {
            String entre = texto.substring(ultimo, m.start());
            String charset = m.group(1);
            int asterisco = charset.indexOf('*'); // RFC 2231: charset*idioma
            if (asterisco > 0) {
                charset = charset.substring(0, asterisco);
            }
            boolean soloEspacios = entre.trim().isEmpty();
            if (charsetPendiente != null && !(soloEspacios && charset.equalsIgnoreCase(charsetPendiente))) {
                sb.append(decodificarBytes(bytesPendientes.toByteArray(), charsetPendiente));
                bytesPendientes.reset();
                charsetPendiente = null;
            }
            boolean hayPalabraAnterior = ultimo > 0;
            if (!(soloEspacios && hayPalabraAnterior)) {
                sb.append(entre); // el espacio entre dos palabras codificadas se descarta (RFC 2047)
            }
            byte[] b = m.group(2).equalsIgnoreCase("B")
                    ? Base64.getMimeDecoder().decode(m.group(3))
                    : decodificarQ(m.group(3));
            bytesPendientes.write(b, 0, b.length);
            charsetPendiente = charset;
            ultimo = m.end();
        }
        if (charsetPendiente != null) {
            sb.append(decodificarBytes(bytesPendientes.toByteArray(), charsetPendiente));
        }
        sb.append(texto.substring(ultimo));
        return sb.toString().trim();
    }

    private static byte[] decodificarQ(String s) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_') {
                out.write(' ');
            } else if (c == '=' && i + 2 < s.length()) {
                try {
                    out.write(Integer.parseInt(s.substring(i + 1, i + 3), 16));
                    i += 2;
                } catch (NumberFormatException e) {
                    out.write(c);
                }
            } else {
                out.write(c);
            }
        }
        return out.toByteArray();
    }

    private static String decodificarBytes(byte[] b, String charset) {
        try {
            return new String(b, Charset.forName(charset));
        } catch (RuntimeException e) {
            return new String(b, StandardCharsets.UTF_8);
        }
    }

    /** Si la cabecera trae bytes UTF-8 sin codificar (leídos como ISO-8859-1), los recompone. */
    static String repararUtf8(String latin1) {
        boolean ascii = true;
        for (int i = 0; i < latin1.length() && ascii; i++) {
            ascii = latin1.charAt(i) < 0x80;
        }
        if (ascii) {
            return latin1;
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(latin1.getBytes(StandardCharsets.ISO_8859_1)))
                    .toString();
        } catch (CharacterCodingException e) {
            return latin1; // era ISO-8859-1 de verdad
        }
    }

    public static String extraerDireccion(String from) {
        Matcher m = DIRECCION_ANGULAR.matcher(from);
        if (m.find()) {
            return m.group(1).trim().toLowerCase();
        }
        m = DIRECCION_SUELTA.matcher(from);
        return m.find() ? m.group(1).trim().toLowerCase() : "";
    }

    public static String extraerNombre(String from) {
        int i = from.indexOf('<');
        String nombre = i > 0 ? from.substring(0, i) : "";
        return nombre.replace("\"", "").trim();
    }

    /**
     * Codifica un texto para una cabecera (Subject/From). Si es ASCII lo deja igual;
     * si no, lo parte en palabras "=?UTF-8?B?...?=" de máximo ~45 bytes sin cortar
     * caracteres, plegadas en varias líneas (RFC 2047: máx. 75 caracteres por palabra).
     */
    public static String codificarCabecera(String texto) {
        boolean ascii = true;
        for (int i = 0; i < texto.length() && ascii; i++) {
            char c = texto.charAt(i);
            ascii = c >= 0x20 && c < 0x7F;
        }
        if (ascii && texto.length() <= 900) {
            return texto;
        }
        StringBuilder sb = new StringBuilder();
        StringBuilder trozo = new StringBuilder();
        int bytesTrozo = 0;
        for (int i = 0; i < texto.length(); ) {
            int cp = texto.codePointAt(i);
            String c = new String(Character.toChars(cp));
            int n = c.getBytes(StandardCharsets.UTF_8).length;
            if (bytesTrozo + n > 45) {
                agregarPalabra(sb, trozo.toString());
                trozo.setLength(0);
                bytesTrozo = 0;
            }
            trozo.append(c);
            bytesTrozo += n;
            i += Character.charCount(cp);
        }
        if (trozo.length() > 0) {
            agregarPalabra(sb, trozo.toString());
        }
        return sb.toString();
    }

    private static void agregarPalabra(StringBuilder sb, String trozo) {
        if (sb.length() > 0) {
            sb.append("\r\n ");
        }
        sb.append("=?UTF-8?B?")
                .append(Base64.getEncoder().encodeToString(trozo.getBytes(StandardCharsets.UTF_8)))
                .append("?=");
    }
}
