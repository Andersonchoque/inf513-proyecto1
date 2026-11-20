package inf513.correo;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Cliente POP3 (RFC 1939) sobre socket, igual que en el proyecto de ejemplo, pero:
 *  - lee solo las cabeceras con TOP n 0 (el comando está en el Asunto);
 *  - lee en ISO-8859-1 para no perder bytes y luego Mime repara el UTF-8;
 *  - DELE marca para borrar y QUIT confirma el borrado.
 */
public class Pop3Cliente implements Closeable {

    private final String host;
    private final int puerto;
    private final int timeoutMs;
    private Socket socket;
    private BufferedReader entrada;
    private OutputStream salida;

    public Pop3Cliente(String host, int puerto, int timeoutMs) {
        this.host = host;
        this.puerto = puerto;
        this.timeoutMs = timeoutMs;
    }

    public void conectar(String usuario, String password) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, puerto), timeoutMs);
        socket.setSoTimeout(timeoutMs);
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
        salida = socket.getOutputStream();
        esperarOk(entrada.readLine(), "saludo del servidor");
        esperarOk(enviar("USER " + usuario), "USER");
        String r = enviar("PASS " + password);
        if (r == null || !r.startsWith("+OK")) {
            throw new IOException("Autenticación POP3 rechazada para '" + usuario + "': " + r);
        }
    }

    /** STAT -> cantidad de correos en el buzón. */
    public int contar() throws IOException {
        String r = enviar("STAT");
        esperarOk(r, "STAT");
        String[] partes = r.split("\\s+");
        return Integer.parseInt(partes[1]);
    }

    /** Cabeceras del correo n (TOP n 0); si el servidor no soporta TOP usa RETR. */
    public String cabeceras(int n) throws IOException {
        String r = enviar("TOP " + n + " 0");
        if (r == null || !r.startsWith("+OK")) {
            esperarOk(enviar("RETR " + n), "RETR " + n);
        }
        return leerMultilinea();
    }

    public void eliminar(int n) throws IOException {
        esperarOk(enviar("DELE " + n), "DELE " + n);
    }

    /** QUIT confirma los DELE y cierra la conexión. */
    public void cerrar() throws IOException {
        if (socket == null) {
            return;
        }
        try {
            if (!socket.isClosed()) {
                enviar("QUIT");
            }
        } finally {
            socket.close();
            socket = null;
        }
    }

    @Override
    public void close() {
        try {
            cerrar();
        } catch (IOException ignorada) {
            // ya se está cerrando
        }
    }

    private String enviar(String comando) throws IOException {
        salida.write((comando + "\r\n").getBytes(StandardCharsets.UTF_8));
        salida.flush();
        return entrada.readLine();
    }

    private String leerMultilinea() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            String linea = entrada.readLine();
            if (linea == null) {
                throw new IOException("El servidor POP3 cerró la conexión al leer un correo");
            }
            if (linea.equals(".")) {
                break;
            }
            if (linea.startsWith("..")) {
                linea = linea.substring(1); // "byte-stuffing" de POP3
            }
            sb.append(linea).append("\r\n");
        }
        return sb.toString();
    }

    private static void esperarOk(String respuesta, String paso) throws IOException {
        if (respuesta == null || !respuesta.startsWith("+OK")) {
            throw new IOException("POP3 respondió error en " + paso + ": " + respuesta);
        }
    }
}
