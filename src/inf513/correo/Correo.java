package inf513.correo;

import java.util.Map;

/** Correo recibido: solo interesan sus cabeceras (el comando viaja en el Asunto). */
public final class Correo {

    private final int numero;
    private final String remitente;
    private final String nombreRemitente;
    private final String asunto;
    private final String messageId;
    private final Map<String, String> cabeceras;

    public Correo(int numero, String remitente, String nombreRemitente, String asunto,
            String messageId, Map<String, String> cabeceras) {
        this.numero = numero;
        this.remitente = remitente;
        this.nombreRemitente = nombreRemitente;
        this.asunto = asunto;
        this.messageId = messageId;
        this.cabeceras = cabeceras;
    }

    public int getNumero() {
        return numero;
    }

    public String getRemitente() {
        return remitente;
    }

    public String getNombreRemitente() {
        return nombreRemitente;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getMessageId() {
        return messageId;
    }

    public String cabecera(String nombre) {
        return cabeceras.get(nombre.toLowerCase());
    }

    @Override
    public String toString() {
        return "#" + numero + " de " + remitente + " asunto=\"" + asunto + "\"";
    }
}
