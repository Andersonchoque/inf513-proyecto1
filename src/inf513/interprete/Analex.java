package inf513.interprete;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador léxico del asunto. Reconoce:
 *   IDENTIFICADOR  letra (letra | dígito | _)*
 *   [  ]  ,
 *   CADENA         " ... "  con escapes \" (comilla) y \\ (barra invertida)
 *   TEXTO_SUELTO   cualquier otra secuencia (sirve para avisar "faltan comillas")
 * Ignora espacios, tabuladores y espacios no separables (NBSP) entre tokens.
 */
public class Analex {

    private final String texto;
    private int i;

    public Analex(String texto) {
        this.texto = texto;
    }

    public List<Token> analizar() throws ErrorSintaxis {
        List<Token> tokens = new ArrayList<>();
        i = 0;
        while (true) {
            saltarEspacios();
            if (i >= texto.length()) {
                tokens.add(new Token(Token.Tipo.FIN, null, i + 1));
                return tokens;
            }
            char c = texto.charAt(i);
            int inicio = i + 1;
            if (c == '[') {
                tokens.add(new Token(Token.Tipo.CORCHETE_ABRE, "[", inicio));
                i++;
            } else if (c == ']') {
                tokens.add(new Token(Token.Tipo.CORCHETE_CIERRA, "]", inicio));
                i++;
            } else if (c == ',') {
                tokens.add(new Token(Token.Tipo.COMA, ",", inicio));
                i++;
            } else if (c == '"') {
                tokens.add(new Token(Token.Tipo.CADENA, leerCadena(), inicio));
            } else if (esLetra(c)) {
                tokens.add(new Token(Token.Tipo.IDENTIFICADOR, leerMientras(true), inicio));
            } else {
                tokens.add(new Token(Token.Tipo.TEXTO_SUELTO, leerMientras(false), inicio));
            }
        }
    }

    private String leerCadena() throws ErrorSintaxis {
        int inicio = i + 1;
        i++; // comilla de apertura
        StringBuilder sb = new StringBuilder();
        while (i < texto.length()) {
            char c = texto.charAt(i);
            if (c == '\\' && i + 1 < texto.length()) {
                char sig = texto.charAt(i + 1);
                if (sig == '"' || sig == '\\') {
                    sb.append(sig); // \" -> "   \\ -> \
                    i += 2;
                    continue;
                }
            }
            if (c == '"') {
                i++;
                return sb.toString();
            }
            sb.append(c);
            i++;
        }
        throw new ErrorSintaxis("Falta la comilla doble de cierre del parámetro que empieza en la posición "
                + inicio + ". Para escribir comillas dentro de un parámetro use \\\"", inicio);
    }

    private String leerMientras(boolean identificador) {
        int inicio = i;
        while (i < texto.length()) {
            char c = texto.charAt(i);
            boolean sigue = identificador
                    ? (esLetra(c) || Character.isDigit(c) || c == '_')
                    : !(esEspacio(c) || c == '[' || c == ']' || c == ',' || c == '"');
            if (!sigue) {
                break;
            }
            i++;
        }
        return texto.substring(inicio, i);
    }

    private void saltarEspacios() {
        while (i < texto.length() && esEspacio(texto.charAt(i))) {
            i++;
        }
    }

    private static boolean esEspacio(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '​' || c == '﻿';
    }

    private static boolean esLetra(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }
}
