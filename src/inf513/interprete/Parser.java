package inf513.interprete;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Analizador sintáctico. Gramática del asunto:
 *
 *   comando    ::= IDENTIFICADOR "[" parametros? "]" FIN
 *   parametros ::= CADENA ( "," CADENA )*
 *
 * Antes de analizar se normaliza el asunto: se quitan prefijos de respuesta/reenvío
 * (Re:, RV:, Fwd:...) y se convierten comillas tipográficas “ ” (que agregan Outlook,
 * Gmail móvil o iOS) a comillas dobles normales.
 */
public final class Parser {

    private static final Pattern PREFIJOS = Pattern.compile(
            "^\\s*(re|rv|fw|fwd|reenviar|reenviado|respuesta|aw|wg)\\s*:\\s*", Pattern.CASE_INSENSITIVE);

    private List<Token> tokens;
    private int p;

    public static Comando analizar(String asunto) throws ErrorSintaxis {
        return new Parser().ejecutar(normalizar(asunto));
    }

    public static String normalizar(String asunto) {
        if (asunto == null) {
            return "";
        }
        String s = asunto.trim();
        String anterior;
        do {
            anterior = s;
            s = PREFIJOS.matcher(s).replaceFirst("");
        } while (!s.equals(anterior));
        return s.replace('“', '"').replace('”', '"')   // “ ”
                .replace('„', '"').replace('‟', '"')   // „ ‟
                .replace('″', '"').replace('＂', '"')   // ″ ＂
                .trim();
    }

    private Comando ejecutar(String asunto) throws ErrorSintaxis {
        if (asunto.isEmpty()) {
            throw new ErrorSintaxis("El asunto está vacío. Escriba un comando, por ejemplo: AYUDA[\"*\"]", 1);
        }
        tokens = new Analex(asunto).analizar();
        p = 0;

        Token nombre = actual();
        if (nombre.getTipo() != Token.Tipo.IDENTIFICADOR) {
            throw new ErrorSintaxis("El asunto debe empezar con el nombre del comando (ej: LISUSU[\"*\"]) "
                    + "y se encontró " + describir(nombre), nombre.getPosicion());
        }
        p++;
        String cmd = nombre.getValor().toUpperCase();

        Token abre = actual();
        if (abre.getTipo() != Token.Tipo.CORCHETE_ABRE) {
            throw new ErrorSintaxis("Se esperaba '[' después de " + cmd + " y se encontró " + describir(abre)
                    + ". Formato: " + cmd + "[\"param1\",\"param2\"]", abre.getPosicion());
        }
        p++;

        List<String> params = new ArrayList<>();
        if (actual().getTipo() != Token.Tipo.CORCHETE_CIERRA) {
            params.add(leerParametro(1));
            while (actual().getTipo() == Token.Tipo.COMA) {
                p++;
                params.add(leerParametro(params.size() + 1));
            }
        }

        Token cierra = actual();
        if (cierra.getTipo() != Token.Tipo.CORCHETE_CIERRA) {
            if (cierra.getTipo() == Token.Tipo.CADENA) {
                throw new ErrorSintaxis("Falta una coma entre el parámetro " + params.size() + " y el parámetro "
                        + (params.size() + 1), cierra.getPosicion());
            }
            throw new ErrorSintaxis("Se esperaba ',' o ']' después del parámetro " + params.size()
                    + " y se encontró " + describir(cierra), cierra.getPosicion());
        }
        p++;

        Token fin = actual();
        if (fin.getTipo() != Token.Tipo.FIN) {
            throw new ErrorSintaxis("Hay texto de más después de ']': " + describir(fin)
                    + ". Envíe un solo comando por correo", fin.getPosicion());
        }
        return new Comando(cmd, params);
    }

    private String leerParametro(int numero) throws ErrorSintaxis {
        Token t = actual();
        if (t.getTipo() == Token.Tipo.CADENA) {
            p++;
            return t.getValor();
        }
        if (t.getTipo() == Token.Tipo.IDENTIFICADOR || t.getTipo() == Token.Tipo.TEXTO_SUELTO) {
            throw new ErrorSintaxis("El parámetro " + numero + " (" + t.getValor()
                    + ") debe ir entre comillas dobles: \"" + t.getValor() + "\"", t.getPosicion());
        }
        throw new ErrorSintaxis("Se esperaba el parámetro " + numero + " entre comillas y se encontró "
                + describir(t), t.getPosicion());
    }

    private Token actual() {
        return tokens.get(p);
    }

    private static String describir(Token t) {
        switch (t.getTipo()) {
            case FIN:
                return "el final del asunto";
            case CADENA:
                return "el texto \"" + t.getValor() + "\"";
            default:
                return "'" + t.getValor() + "'";
        }
    }
}
