package inf513;

import inf513.correo.Correo;
import inf513.correo.Mime;
import inf513.correo.Pop3Cliente;
import inf513.correo.SmtpCliente;
import inf513.interprete.Comando;
import inf513.interprete.ErrorSintaxis;
import inf513.interprete.Parser;
import inf513.negocio.Resultado;
import inf513.negocio.ServicioComandos;
import inf513.presentacion.HtmlRespuesta;
import inf513.util.Config;
import inf513.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta el ciclo de vida de cada mensaje (diagrama del enunciado):
 *   Buzón POP3 -> Parser y validador -> Negocio/PostgreSQL -> Correo de éxito o de error.
 */
public class ProcesadorCorreos {

    /** Respuesta pendiente de envío. */
    private static final class Pendiente {
        final String para;
        final String asunto;
        final String html;
        final String messageId;

        Pendiente(String para, String asunto, String html, String messageId) {
            this.para = para;
            this.asunto = asunto;
            this.html = html;
            this.messageId = messageId;
        }
    }

    private final ServicioComandos servicio = new ServicioComandos();
    /** Buzón donde llegan los comandos y cuenta que envía las respuestas (pueden ser distintas, ej. Gmail). */
    private final java.util.Set<String> direccionesPropias = new java.util.HashSet<>(java.util.Arrays.asList(
            buzon().toLowerCase(), Config.get("smtp.remitente", "").toLowerCase()));

    public static String buzon() {
        return Config.get("correo.buzon", "grupo23sa@tecnoweb.org.bo");
    }

    /** Analiza y ejecuta un asunto. Lo usan el modo correo y el modo consola. */
    public Resultado procesarAsunto(String asunto, String remitente) {
        long t0 = System.currentTimeMillis();
        Resultado r;
        String comandoLog;
        try {
            Comando cmd = Parser.analizar(asunto);
            comandoLog = ServicioComandos.textoSeguro(cmd);
            r = servicio.ejecutar(cmd, remitente);
        } catch (ErrorSintaxis e) {
            comandoLog = "(sintaxis inválida)";
            r = Resultado.error("Sintaxis inválida en la posición " + e.getPosicion() + ": " + e.getMessage(),
                    "Formato esperado: COMANDO[\"param1\",\"param2\",...]  ·  Envíe AYUDA[\"*\"] para ver los comandos.");
        }
        long ms = System.currentTimeMillis() - t0;
        Log.acceso(remitente, asuntoSeguro(asunto), comandoLog, r.isExito() ? "OK" : "ERROR", ms, r.getTitulo());
        return r;
    }

    /** Asunto apto para logs y respuestas: las contraseñas se reemplazan por ******. */
    public static String asuntoSeguro(String asunto) {
        try {
            return ServicioComandos.textoSeguro(Parser.analizar(asunto));
        } catch (ErrorSintaxis e) {
            String s = Parser.normalizar(asunto);
            String u = s.toUpperCase();
            if ((u.startsWith("INSUSU") || u.startsWith("MODUSU")) && s.indexOf('[') > 0) {
                return s.substring(0, s.indexOf('[')) + "[...parámetros ocultos por contener una contraseña...]";
            }
            return asunto;
        }
    }

    /** Un ciclo completo: lee el buzón, procesa cada correo y envía las respuestas. */
    public int procesarBuzon() throws IOException {
        List<Pendiente> respuestas = new ArrayList<>();
        boolean borrar = Config.booleano("demonio.borrar_procesados", true);
        int procesados = 0;

        try (Pop3Cliente pop = new Pop3Cliente(Config.requerido("pop3.host"), Config.entero("pop3.puerto", 110),
                Config.entero("correo.timeout_segundos", 90) * 1000)) {
            procesados = leerYEjecutar(pop, borrar, respuestas);
        } finally {
            enviar(respuestas); // los comandos ya ejecutados siempre reciben su respuesta
        }
        return procesados;
    }

    private int leerYEjecutar(Pop3Cliente pop, boolean borrar, List<Pendiente> respuestas) throws IOException {
        int procesados = 0;
        pop.conectar(Config.requerido("pop3.usuario"), Config.requerido("pop3.password"));
        int total = pop.contar();
        if (total == 0) {
            pop.cerrar();
            return 0;
        }
        Log.sistema("Buzón con " + total + " correo(s) nuevo(s)");
        for (int i = 1; i <= total; i++) {
            try {
                Correo c = Mime.parsearCorreo(i, pop.cabeceras(i));
                String motivo = motivoParaIgnorar(c);
                if (motivo != null) {
                    Log.acceso(c.getRemitente(), c.getAsunto(), "-", "IGNORADO", 0, motivo);
                } else {
                    long t0 = System.currentTimeMillis();
                    Resultado r = procesarAsunto(c.getAsunto(), c.getRemitente());
                    String cmd = nombreComando(c.getAsunto());
                    respuestas.add(new Pendiente(c.getRemitente(), HtmlRespuesta.asunto(r, cmd),
                            HtmlRespuesta.generar(r, asuntoSeguro(c.getAsunto()), c.getRemitente(), System.currentTimeMillis() - t0),
                            c.getMessageId()));
                    procesados++;
                }
            } catch (IOException | RuntimeException e) {
                Log.error("Procesando el correo #" + i + " del buzón", e);
            }
            if (borrar) {
                pop.eliminar(i); // se borra aunque falle, para no reprocesarlo en un bucle infinito
            }
        }
        pop.cerrar(); // QUIT: confirma los borrados antes de responder
        return procesados;
    }

    private void enviar(List<Pendiente> respuestas) {
        if (respuestas.isEmpty()) {
            return;
        }
        SmtpCliente smtp = null;
        try {
            for (Pendiente p : respuestas) {
                for (int intento = 1; intento <= 2; intento++) {
                    try {
                        if (smtp == null || !smtp.abierto()) {
                            smtp = nuevoSmtp();
                            smtp.abrir();
                        }
                        smtp.enviar(p.para, p.asunto, p.html, p.messageId);
                        Log.sistema("Respuesta enviada a " + p.para + ": " + p.asunto);
                        break;
                    } catch (IOException e) {
                        Log.error("Enviando respuesta a " + p.para + " (intento " + intento + ")", e);
                        if (smtp != null) {
                            smtp.close();
                            smtp = null;
                        }
                    }
                }
            }
        } finally {
            if (smtp != null) {
                smtp.close();
            }
        }
    }

    public static SmtpCliente nuevoSmtp() {
        return new SmtpCliente(Config.requerido("smtp.host"), Config.entero("smtp.puerto", 25),
                Config.entero("correo.timeout_segundos", 90) * 1000, Config.get("smtp.auth", "ninguna"),
                Config.get("smtp.usuario", ""), Config.get("smtp.password", ""),
                Config.requerido("smtp.remitente"), Config.get("smtp.nombre", "Sistema Grupo 23SA"),
                Config.booleano("smtp.ssl", false)).responderA(buzon());
    }

    /** Protección anti-bucles: no se responde a sí mismo, a rebotes ni a respuestas automáticas. */
    private String motivoParaIgnorar(Correo c) {
        String de = c.getRemitente();
        if (de.isEmpty() || !de.contains("@")) {
            return "remitente vacío o inválido";
        }
        if (direccionesPropias.contains(de)) {
            return "el correo proviene de la propia casilla del sistema";
        }
        String local = de.substring(0, de.indexOf('@'));
        if (local.equals("mailer-daemon") || local.equals("postmaster") || local.startsWith("no-reply") || local.startsWith("noreply")) {
            return "remitente automático (" + local + ")";
        }
        String auto = c.cabecera("auto-submitted");
        if (auto != null && !auto.trim().toLowerCase().startsWith("no")) {
            return "respuesta automática (Auto-Submitted: " + auto + ")";
        }
        String precedencia = c.cabecera("precedence");
        if (precedencia != null && precedencia.toLowerCase().matches(".*(bulk|junk|list|auto_reply).*")) {
            return "correo masivo o automático (Precedence: " + precedencia + ")";
        }
        String asunto = Parser.normalizar(c.getAsunto());
        if (asunto.startsWith("[Éxito]") || asunto.startsWith("[Error]")) {
            return "es una respuesta del propio sistema";
        }
        return null;
    }

    private static String nombreComando(String asunto) {
        String s = Parser.normalizar(asunto);
        int i = s.indexOf('[');
        String n = (i > 0 ? s.substring(0, i) : s).trim().toUpperCase();
        return n.length() > 30 ? n.substring(0, 30) : (n.isEmpty() ? "(vacío)" : n);
    }
}
