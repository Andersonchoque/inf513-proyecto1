package inf513.correo;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLSocketFactory;

/**
 * Cliente SMTP (RFC 5321) sobre socket. Una sesión puede enviar varias respuestas
 * seguidas (el servidor de tecnoweb tarda varios segundos en saludar, así que se
 * reutiliza la conexión). Soporta AUTH CRAM-MD5, LOGIN y PLAIN, o sin autenticación.
 * El mensaje se envía como HTML UTF-8 en Base64 con cabeceras anti-bucle (RFC 3834).
 */
public class SmtpCliente implements Closeable {

    private final String host;
    private final int puerto;
    private final int timeoutMs;
    private final String auth;
    private final String usuario;
    private final String password;
    private final String remitente;
    private final String nombreRemitente;
    private final boolean ssl;
    private String responderA;
    private Socket socket;
    private BufferedReader entrada;
    private OutputStream salida;

    public SmtpCliente(String host, int puerto, int timeoutMs, String auth, String usuario,
            String password, String remitente, String nombreRemitente, boolean ssl) {
        this.host = host;
        this.puerto = puerto;
        this.timeoutMs = timeoutMs;
        this.auth = auth == null ? "ninguna" : auth.trim().toLowerCase();
        this.usuario = usuario;
        this.password = password;
        this.remitente = remitente;
        this.nombreRemitente = nombreRemitente;
        this.ssl = ssl;
    }

    /** Dirección para "Responder" (el buzón que recibe comandos), si es distinta del remitente. */
    public SmtpCliente responderA(String direccion) {
        this.responderA = direccion;
        return this;
    }

    public void abrir() throws IOException {
        Socket plano = new Socket();
        plano.connect(new InetSocketAddress(host, puerto), timeoutMs);
        // SSL implícito (ej: Gmail en el puerto 465); el certificado del servidor se valida con el almacén de Java
        socket = ssl ? ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(plano, host, puerto, true) : plano;
        socket.setSoTimeout(timeoutMs);
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
        salida = socket.getOutputStream();
        esperar(leerRespuesta(), 220, "saludo");
        esperar(comando("EHLO " + dominioLocal()), 250, "EHLO");
        autenticar();
    }

    public boolean abierto() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void enviar(String para, String asunto, String html, String enRespuestaA) throws IOException {
        try {
            esperar(comando("MAIL FROM:<" + remitente + ">"), 250, "MAIL FROM");
            String r = comando("RCPT TO:<" + para + ">");
            if (!r.startsWith("250") && !r.startsWith("251")) {
                throw new IOException("El servidor rechazó el destinatario " + para + ": " + r);
            }
            esperar(comando("DATA"), 354, "DATA");
            escribir(armarMensaje(para, asunto, html, enRespuestaA));
            esperar(comando("."), 250, "fin de DATA");
        } catch (IOException e) {
            if (abierto()) {
                try {
                    comando("RSET"); // deja la sesión lista para el siguiente mensaje
                } catch (IOException ignorada) {
                    // la conexión ya no sirve; el que llama reabrirá
                }
            }
            throw e;
        }
    }

    @Override
    public void close() {
        if (socket == null) {
            return;
        }
        try {
            if (!socket.isClosed()) {
                comando("QUIT");
            }
        } catch (IOException ignorada) {
            // cerrando
        } finally {
            try {
                socket.close();
            } catch (IOException ignorada) {
                // cerrando
            }
            socket = null;
        }
    }

    private void autenticar() throws IOException {
        switch (auth) {
            case "ninguna":
            case "none":
            case "":
                return;
            case "cram-md5": {
                String r = comando("AUTH CRAM-MD5");
                esperar(r, 334, "AUTH CRAM-MD5");
                String reto = new String(Base64.getDecoder().decode(r.substring(4).trim()), StandardCharsets.ISO_8859_1);
                String respuesta = usuario + " " + hmacMd5Hex(password, reto);
                esperar(comando(b64(respuesta)), 235, "credenciales CRAM-MD5");
                return;
            }
            case "login":
                esperar(comando("AUTH LOGIN"), 334, "AUTH LOGIN");
                esperar(comando(b64(usuario)), 334, "usuario LOGIN");
                esperar(comando(b64(password)), 235, "contraseña LOGIN");
                return;
            case "plain":
                esperar(comando("AUTH PLAIN " + b64("\0" + usuario + "\0" + password)), 235, "AUTH PLAIN");
                return;
            default:
                throw new IOException("smtp.auth desconocido: '" + auth + "' (use ninguna, cram-md5, login o plain)");
        }
    }

    private String armarMensaje(String para, String asunto, String html, String enRespuestaA) {
        SimpleDateFormat fecha = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        String dominio = remitente.substring(remitente.indexOf('@') + 1);
        StringBuilder m = new StringBuilder();
        m.append("From: ").append(Mime.codificarCabecera(nombreRemitente)).append(" <").append(remitente).append(">\r\n");
        m.append("To: <").append(para).append(">\r\n");
        m.append("Subject: ").append(Mime.codificarCabecera(asunto)).append("\r\n");
        m.append("Date: ").append(fecha.format(new Date())).append("\r\n");
        m.append("Message-ID: <").append(UUID.randomUUID()).append('@').append(dominio).append(">\r\n");
        if (enRespuestaA != null && !enRespuestaA.isEmpty()) {
            m.append("In-Reply-To: ").append(enRespuestaA).append("\r\n");
            m.append("References: ").append(enRespuestaA).append("\r\n");
        }
        if (responderA != null && !responderA.isEmpty() && !responderA.equalsIgnoreCase(remitente)) {
            m.append("Reply-To: <").append(responderA).append(">\r\n"); // "Responder" vuelve al buzón de comandos
        }
        m.append("Auto-Submitted: auto-replied\r\n");          // RFC 3834: evita bucles de respuestas
        m.append("X-Auto-Response-Suppress: All\r\n");
        m.append("MIME-Version: 1.0\r\n");
        m.append("Content-Type: text/html; charset=UTF-8\r\n");
        m.append("Content-Transfer-Encoding: base64\r\n\r\n");
        String cuerpo = Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(html.getBytes(StandardCharsets.UTF_8));
        m.append(cuerpo).append("\r\n");
        return m.toString();
    }

    private String comando(String linea) throws IOException {
        escribir(linea + "\r\n");
        return leerRespuesta();
    }

    private void escribir(String texto) throws IOException {
        salida.write(texto.getBytes(StandardCharsets.US_ASCII));
        salida.flush();
    }

    /** Lee una respuesta SMTP completa (las líneas "250-..." continúan, "250 ..." termina). */
    private String leerRespuesta() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            String linea = entrada.readLine();
            if (linea == null) {
                throw new IOException("El servidor SMTP cerró la conexión");
            }
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(linea);
            if (linea.length() < 4 || linea.charAt(3) != '-') {
                return sb.toString();
            }
        }
    }

    private static void esperar(String respuesta, int codigo, String paso) throws IOException {
        if (respuesta == null || !respuesta.startsWith(String.valueOf(codigo))) {
            throw new IOException("SMTP respondió error en " + paso + " (se esperaba " + codigo + "): " + respuesta);
        }
    }

    private String dominioLocal() {
        return remitente.contains("@") ? remitente.substring(remitente.indexOf('@') + 1) : "localhost";
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacMd5Hex(String clave, String reto) throws IOException {
        try {
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(new SecretKeySpec(clave.getBytes(StandardCharsets.UTF_8), "HmacMD5"));
            byte[] h = mac.doFinal(reto.getBytes(StandardCharsets.ISO_8859_1));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (GeneralSecurityException e) {
            throw new IOException("No se pudo calcular CRAM-MD5", e);
        }
    }
}
