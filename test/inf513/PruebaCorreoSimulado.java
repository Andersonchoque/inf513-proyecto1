package inf513;

import inf513.datos.ConexionBD;
import inf513.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Prueba de extremo a extremo SIN tocar el servidor real: levanta un POP3 y un SMTP
 * falsos en localhost, ejecuta un ciclo del procesador contra la base LOCAL y verifica
 * respuestas, borrados, protección anti-bucles, decodificación UTF-8 y AUTH CRAM-MD5.
 */
public final class PruebaCorreoSimulado {

    static final String CLAVE = "clave-smtp";
    static final List<String> BORRADOS = Collections.synchronizedList(new ArrayList<>());
    static final List<String[]> ENVIADOS = Collections.synchronizedList(new ArrayList<>()); // {rcpt, mensaje}
    static volatile boolean authOk;
    private static int ok;
    private static int fallos;

    public static void main(String[] args) throws Exception {
        String[][] buzon = {
            {"\"Juan Pérez\" <juan@gmail.com>", "LISROL[\"*\"]", ""},
            {"=?UTF-8?B?TWFyw61h?= <maria@gmail.com>",
                "=?UTF-8?B?" + b64("INSMAT[\"ADM-900\",\"Administración Estratégica\",\"8\",\"5\",\"4\",\"2\",\"obligatoria\",\"\"]") + "?=", ""},
            {"<grupo23sa@tecnoweb.org.bo>", "LISUSU[\"*\"]", ""},
            {"MAILER-DAEMON@tecnoweb.org.bo", "Undelivered Mail Returned to Sender", ""},
            {"robot@empresa.com", "LISROL[\"*\"]", "Auto-Submitted: auto-replied"},
            {"ana@gmail.com", "=?UTF-8?Q?BUSCAR=5B=22Administraci=C3?= =?UTF-8?Q?=B3n=22=5D?=", ""},
            {"pedro@gmail.com", "LISUSU[*]", ""},
            {"luis@gmail.com", "Re: [Éxito] LISROL - Consulta realizada correctamente", ""},
            {"Sistema <sistema.grupo23@gmail.com>", "LISROL[\"*\"]", ""},
        };
        try (ServerSocket pop = new ServerSocket(0); ServerSocket smtp = new ServerSocket(0)) {
            hilo(() -> servidorPop3(pop, buzon));
            hilo(() -> servidorSmtp(smtp));
            System.setProperty("pop3.host", "127.0.0.1");
            System.setProperty("pop3.puerto", String.valueOf(pop.getLocalPort()));
            System.setProperty("pop3.usuario", "grupo23sa");
            System.setProperty("pop3.password", "x");
            System.setProperty("smtp.host", "127.0.0.1");
            System.setProperty("smtp.puerto", String.valueOf(smtp.getLocalPort()));
            System.setProperty("smtp.auth", "cram-md5");
            System.setProperty("smtp.usuario", "grupo23sa");
            System.setProperty("smtp.password", CLAVE);
            System.setProperty("smtp.remitente", "sistema.grupo23@gmail.com"); // envía con Gmail
            System.setProperty("correo.buzon", "grupo23sa@tecnoweb.org.bo");  // recibe en tecnoweb
            System.setProperty("correo.timeout_segundos", "10");
            Log.iniciar(System.getProperty("logs.directorio", "logs"));

            int procesados = new ProcesadorCorreos().procesarBuzon();

            verificar("se procesaron 4 comandos (5 ignorados)", procesados == 4, "procesados=" + procesados);
            verificar("se borraron los 9 correos del buzón", BORRADOS.size() == 9, BORRADOS.toString());
            verificar("SMTP autenticó con CRAM-MD5", authOk, "");
            verificar("se enviaron 4 respuestas", ENVIADOS.size() == 4, String.valueOf(ENVIADOS.size()));
            verificar("respuesta a juan es Éxito", respuesta("juan@gmail.com").contains("Consulta realizada correctamente"), "");
            verificar("respuesta a maria confirma inserción", respuesta("maria@gmail.com").contains("Registro insertado correctamente"), "");
            verificar("la búsqueda de ana encontró la materia con tilde", respuesta("ana@gmail.com").contains("Administración Estratégica"), "");
            verificar("pedro recibe error de comillas", respuesta("pedro@gmail.com").contains("debe ir entre comillas"), "");
            verificar("no se respondió a sí mismo, a MAILER-DAEMON, a auto-respuestas ni a respuestas del sistema",
                    respuesta("grupo23sa@tecnoweb.org.bo").isEmpty() && respuesta("robot@empresa.com").isEmpty()
                            && respuesta("luis@gmail.com").isEmpty() && respuesta("sistema.grupo23@gmail.com").isEmpty(), "");
            verificar("las respuestas llevan Reply-To al buzón de tecnoweb",
                    ENVIADOS.get(0)[1].contains("Reply-To: <grupo23sa@tecnoweb.org.bo>"), "");
            verificar("las respuestas llevan Auto-Submitted", ENVIADOS.get(0)[1].contains("Auto-Submitted: auto-replied"), "");
            verificar("el asunto de la respuesta va codificado en UTF-8", ENVIADOS.get(0)[1].contains("Subject: =?UTF-8?B?"), "");
            try (Connection cn = ConexionBD.abrir();
                 PreparedStatement ps = cn.prepareStatement("SELECT nombre FROM materias WHERE sigla = 'ADM-900'");
                 ResultSet rs = ps.executeQuery()) {
                verificar("la materia quedó en la BD con tildes correctas", rs.next() && rs.getString(1).equals("Administración Estratégica"), "");
            }
        }
        System.out.println("\nPrueba de correo simulado: " + ok + " OK, " + fallos + " fallidas");
        System.exit(fallos == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ POP3 falso
    static void servidorPop3(ServerSocket ss, String[][] buzon) {
        try (Socket s = ss.accept()) {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.ISO_8859_1));
            OutputStream out = s.getOutputStream();
            escribir(out, "+OK POP3 simulado");
            String l;
            while ((l = in.readLine()) != null) {
                String[] p = l.split(" ");
                switch (p[0].toUpperCase()) {
                    case "STAT":
                        escribir(out, "+OK " + buzon.length + " 1000");
                        break;
                    case "TOP":
                    case "RETR": {
                        String[] m = buzon[Integer.parseInt(p[1]) - 1];
                        escribir(out, "+OK");
                        escribir(out, "From: " + m[0]);
                        escribir(out, "To: grupo23sa@tecnoweb.org.bo");
                        escribir(out, "Subject: " + m[1]);
                        escribir(out, "Message-ID: <" + p[1] + "@prueba>");
                        if (!m[2].isEmpty()) {
                            escribir(out, m[2]);
                        }
                        escribir(out, "");
                        escribir(out, ".");
                        break;
                    }
                    case "DELE":
                        BORRADOS.add(p[1]);
                        escribir(out, "+OK");
                        break;
                    case "QUIT":
                        escribir(out, "+OK adiós");
                        return;
                    default:
                        escribir(out, "+OK");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ------------------------------------------------------------------ SMTP falso
    static void servidorSmtp(ServerSocket ss) {
        while (true) {
            try (Socket s = ss.accept()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII));
                OutputStream out = s.getOutputStream();
                escribir(out, "220 smtp simulado");
                String rcpt = null;
                String l;
                while ((l = in.readLine()) != null) {
                    String u = l.toUpperCase();
                    if (u.startsWith("EHLO")) {
                        escribir(out, "250-simulado");
                        escribir(out, "250 AUTH CRAM-MD5");
                    } else if (u.startsWith("AUTH CRAM-MD5")) {
                        String reto = "<123.456@simulado>";
                        escribir(out, "334 " + b64(reto));
                        String resp = new String(Base64.getDecoder().decode(in.readLine()), StandardCharsets.UTF_8);
                        authOk = resp.equals("grupo23sa " + hmac(CLAVE, reto));
                        escribir(out, authOk ? "235 ok" : "535 mal");
                    } else if (u.startsWith("RCPT TO:")) {
                        rcpt = l.substring(l.indexOf('<') + 1, l.indexOf('>'));
                        escribir(out, "250 ok");
                    } else if (u.equals("DATA")) {
                        escribir(out, "354 envíe");
                        StringBuilder msg = new StringBuilder();
                        String d;
                        while (!(d = in.readLine()).equals(".")) {
                            msg.append(d).append('\n');
                        }
                        String texto = msg.toString();
                        String cuerpo = texto.substring(texto.indexOf("\n\n") + 2).replace("\n", "");
                        ENVIADOS.add(new String[]{rcpt, texto + new String(Base64.getDecoder().decode(cuerpo), StandardCharsets.UTF_8)});
                        escribir(out, "250 encolado");
                    } else if (u.equals("QUIT")) {
                        escribir(out, "221 adiós");
                        break;
                    } else {
                        escribir(out, "250 ok");
                    }
                }
            } catch (IOException e) {
                return;
            }
        }
    }

    // ------------------------------------------------------------------ utilidades
    static String respuesta(String para) {
        synchronized (ENVIADOS) {
            for (String[] e : ENVIADOS) {
                if (e[0].equals(para)) {
                    return e[1];
                }
            }
        }
        return "";
    }

    static void verificar(String nombre, boolean bien, String detalle) {
        if (bien) {
            ok++;
            System.out.println("  OK    " + nombre);
        } else {
            fallos++;
            System.out.println("  FALLA " + nombre + " " + detalle);
        }
    }

    static void escribir(OutputStream out, String linea) throws IOException {
        out.write((linea + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    static String hmac(String clave, String reto) {
        try {
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(new SecretKeySpec(clave.getBytes(StandardCharsets.UTF_8), "HmacMD5"));
            StringBuilder sb = new StringBuilder();
            for (byte b : mac.doFinal(reto.getBytes(StandardCharsets.ISO_8859_1))) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void hilo(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
    }
}
