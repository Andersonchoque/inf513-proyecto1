package inf513.negocio;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Validación estricta de tipos en el servidor: cada parámetro recibido como texto
 * se convierte al tipo Java que corresponde a la columna (Long, BigDecimal, Boolean,
 * java.sql.Date, java.sql.Time o String) antes de llegar al PreparedStatement.
 */
public final class Validador {

    private static final Pattern ENTERO = Pattern.compile("[+-]?\\d{1,18}");
    private static final Pattern DECIMAL = Pattern.compile("[+-]?\\d{1,15}([.,]\\d{1,6})?");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+'-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern URL = Pattern.compile("https?://[^\\s\"<>]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\n\\t]]");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter LATINO = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private Validador() {
    }

    /**
     * Convierte el parámetro 'crudo' según el campo.
     * @param posicion número de parámetro (para el mensaje de error)
     * @return valor tipado, o null si quedó vacío y el campo lo permite
     */
    public static Object convertir(Campo c, String crudo, int posicion) throws ErrorValidacion {
        String v = crudo == null ? "" : crudo.trim();
        String donde = "Parámetro " + posicion + " (" + c.getColumna() + ")";

        if (v.isEmpty()) {
            if (c.getTipo() == TipoDato.SLUG) {
                return null; // se genera a partir del título
            }
            if (c.getPorDefecto() != null) {
                return convertir(clonarSinDefecto(c), c.getPorDefecto(), posicion);
            }
            if (c.isObligatorio()) {
                throw new ErrorValidacion(donde + " es obligatorio y llegó vacío");
            }
            return null;
        }
        if (CONTROL.matcher(v).find()) {
            throw new ErrorValidacion(donde + " contiene caracteres de control no permitidos");
        }

        switch (c.getTipo()) {
            case ENTERO: {
                if (!ENTERO.matcher(v).matches()) {
                    throw new ErrorValidacion(donde + ": '" + v + "' no es un número entero");
                }
                long n = Long.parseLong(v.startsWith("+") ? v.substring(1) : v);
                if (c.getMinimoNum() != null && n < c.getMinimoNum()) {
                    throw new ErrorValidacion(donde + ": debe ser mayor o igual a " + c.getMinimoNum() + " (llegó " + n + ")");
                }
                if (c.getMaximoNum() != null && n > c.getMaximoNum()) {
                    throw new ErrorValidacion(donde + ": debe ser menor o igual a " + c.getMaximoNum() + " (llegó " + n + ")");
                }
                return n;
            }
            case DECIMAL:
                if (!DECIMAL.matcher(v).matches()) {
                    throw new ErrorValidacion(donde + ": '" + v + "' no es un número decimal (ej: 150.50)");
                }
                return new BigDecimal(v.replace(',', '.'));
            case BOOLEANO: {
                String b = sinTildes(v).toLowerCase();
                if (Arrays.asList("true", "1", "si", "s", "verdadero", "v", "yes").contains(b)) {
                    return Boolean.TRUE;
                }
                if (Arrays.asList("false", "0", "no", "n", "falso", "f").contains(b)) {
                    return Boolean.FALSE;
                }
                throw new ErrorValidacion(donde + ": '" + v + "' no es un valor lógico (use true/false o si/no)");
            }
            case FECHA:
                return java.sql.Date.valueOf(fecha(v, donde));
            case HORA:
                try {
                    String h = v.indexOf(':') == 1 ? "0" + v : v; // 9:30 -> 09:30
                    return java.sql.Time.valueOf(LocalTime.parse(h.length() == 5 ? h + ":00" : h));
                } catch (DateTimeParseException e) {
                    throw new ErrorValidacion(donde + ": '" + v + "' no es una hora válida (use HH:MM, ej: 18:30)");
                }
            case EMAIL:
                longitud(v, c, donde);
                if (!EMAIL.matcher(v).matches()) {
                    throw new ErrorValidacion(donde + ": '" + v + "' no es un correo electrónico válido");
                }
                return v.toLowerCase();
            case URL:
                longitud(v, c, donde);
                if (!URL.matcher(v).matches()) {
                    throw new ErrorValidacion(donde + ": '" + v + "' no es un enlace válido (debe empezar con http:// o https://)");
                }
                return v;
            case OPCION: {
                String o = sinTildes(v).toLowerCase().replace(' ', '_');
                for (String permitida : c.getOpciones()) {
                    if (permitida.equals(o)) {
                        return permitida;
                    }
                }
                throw new ErrorValidacion(donde + ": '" + v + "' no es válido. Valores permitidos: "
                        + String.join(", ", c.getOpciones()));
            }
            case CLAVE:
                if (v.length() < 6) {
                    throw new ErrorValidacion(donde + ": la contraseña debe tener al menos 6 caracteres");
                }
                if (v.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                    throw new ErrorValidacion(donde + ": la contraseña no puede superar 72 bytes");
                }
                // mismo algoritmo que Laravel (Hash::make); PHP/Laravel reconocen el prefijo $2y$ (jBCrypt genera $2a$, idéntico)
                return BCrypt.hashpw(v, BCrypt.gensalt(12)).replaceFirst("^\\$2a\\$", "\\$2y\\$");
            case SLUG:
                longitud(v, c, donde);
                if (!SLUG.matcher(v).matches()) {
                    throw new ErrorValidacion(donde + ": '" + v + "' no es un slug válido (solo minúsculas, números y guiones, "
                            + "ej: mi-noticia-2026). Déjelo vacío \"\" para generarlo automáticamente");
                }
                return v;
            default: // TEXTO, TEXTO_LARGO
                longitud(v, c, donde);
                return v;
        }
    }

    /** Convierte un valor para usarlo como filtro (LISxxx["campo","valor"]); no cifra contraseñas. */
    public static Object convertirFiltro(Campo c, String crudo) throws ErrorValidacion {
        if (c.getTipo().esTexto()) {
            return crudo.trim();
        }
        return convertir(clonarSinDefecto(c), crudo, 2);
    }

    public static LocalDate fecha(String v, String donde) throws ErrorValidacion {
        try {
            return LocalDate.parse(v.trim(), v.contains("/") ? LATINO : ISO);
        } catch (DateTimeParseException e) {
            throw new ErrorValidacion(donde + ": '" + v + "' no es una fecha válida (use AAAA-MM-DD, ej: 2026-11-20)");
        }
    }

    public static long entero(String v, String donde) throws ErrorValidacion {
        String t = v == null ? "" : v.trim();
        if (!ENTERO.matcher(t).matches()) {
            throw new ErrorValidacion(donde + ": '" + t + "' no es un número entero");
        }
        return Long.parseLong(t.startsWith("+") ? t.substring(1) : t);
    }

    /** "Congreso de Administración 2026" -> "congreso-de-administracion-2026" */
    public static String slugificar(String texto) {
        String s = sinTildes(texto == null ? "" : texto).toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return s.isEmpty() ? "registro" : s;
    }

    public static String sinTildes(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static void longitud(String v, Campo c, String donde) throws ErrorValidacion {
        if (c.getMaximo() > 0 && v.length() > c.getMaximo()) {
            throw new ErrorValidacion(donde + ": excede el máximo de " + c.getMaximo()
                    + " caracteres (tiene " + v.length() + ")");
        }
    }

    private static Campo clonarSinDefecto(Campo c) {
        // Mismo campo pero sin valor por defecto, para validar el defecto o un filtro sin recursión.
        Campo copia;
        switch (c.getTipo()) {
            case ENTERO:
                copia = Campo.entero(c.getColumna());
                if (c.getMinimoNum() != null) {
                    copia.rango(c.getMinimoNum(), c.getMaximoNum() == null ? Long.MAX_VALUE : c.getMaximoNum());
                }
                break;
            case BOOLEANO:
                copia = Campo.booleano(c.getColumna(), false).porDefecto(null);
                break;
            case OPCION:
                copia = Campo.opcion(c.getColumna(), c.getOpciones());
                break;
            case FECHA:
                copia = Campo.fecha(c.getColumna());
                break;
            case HORA:
                copia = Campo.hora(c.getColumna());
                break;
            default:
                copia = Campo.texto(c.getColumna(), c.getMaximo());
        }
        return copia.obligatorio();
    }
}
