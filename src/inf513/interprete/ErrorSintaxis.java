package inf513.interprete;

/** Error léxico o sintáctico en el asunto del correo, con la posición del problema. */
public class ErrorSintaxis extends Exception {

    private final int posicion;

    public ErrorSintaxis(String mensaje, int posicion) {
        super(mensaje);
        this.posicion = posicion;
    }

    public int getPosicion() {
        return posicion;
    }
}
