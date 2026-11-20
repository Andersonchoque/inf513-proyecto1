package inf513.interprete;

/** Unidad léxica reconocida en el asunto del correo. */
public final class Token {

    public enum Tipo {
        IDENTIFICADOR,   // nombre del comando: LISUSU, INSMAT, AYUDA...
        CORCHETE_ABRE,   // [
        CORCHETE_CIERRA, // ]
        COMA,            // ,
        CADENA,          // "texto entre comillas" (ya sin comillas y con escapes resueltos)
        TEXTO_SUELTO,    // texto sin comillas (error de sintaxis dentro de los parámetros)
        FIN
    }

    private final Tipo tipo;
    private final String valor;
    private final int posicion; // 1 = primer carácter del asunto

    public Token(Tipo tipo, String valor, int posicion) {
        this.tipo = tipo;
        this.valor = valor;
        this.posicion = posicion;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getValor() {
        return valor;
    }

    public int getPosicion() {
        return posicion;
    }

    @Override
    public String toString() {
        return tipo + (valor == null ? "" : "(" + valor + ")") + "@" + posicion;
    }
}
