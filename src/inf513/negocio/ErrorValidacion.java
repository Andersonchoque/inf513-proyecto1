package inf513.negocio;

/** Error de negocio o de validación de parámetros (se responde como "Error: ..."). */
public class ErrorValidacion extends Exception {

    public ErrorValidacion(String mensaje) {
        super(mensaje);
    }
}
