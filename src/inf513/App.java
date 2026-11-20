package inf513;

import inf513.correo.Pop3Cliente;
import inf513.correo.SmtpCliente;
import inf513.datos.ConexionBD;
import inf513.negocio.Ayuda;
import inf513.negocio.Campo;
import inf513.negocio.Catalogo;
import inf513.negocio.Entidad;
import inf513.negocio.Reportes;
import inf513.negocio.Resultado;
import inf513.presentacion.HtmlRespuesta;
import inf513.util.Config;
import inf513.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Punto de entrada. Modos:
 *   demonio        bucle infinito: revisa el buzón cada N segundos (por defecto)
 *   una-vez        un solo ciclo (para ejecutarlo desde cron)
 *   consola        escribe asuntos por teclado/archivo sin usar correo (pruebas y defensa)
 *   diagnostico    prueba la conexión a PostgreSQL, POP3 y SMTP sin modificar nada
 *   enviar-prueba  envía un correo de prueba a la dirección indicada
 *   hash           genera el hash BCrypt de una contraseña
 *   doc            imprime la referencia de comandos en Markdown
 */
public final class App {

    private static volatile boolean activo = true;

    private App() {
    }

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, "UTF-8"));
        System.setErr(new PrintStream(System.err, true, "UTF-8"));
        String modo = args.length > 0 ? args[0].toLowerCase() : "demonio";

        if (modo.equals("hash")) {
            System.out.println(BCrypt.hashpw(args.length > 1 ? args[1] : "password", BCrypt.gensalt(12)).replaceFirst("^\\$2a\\$", "\\$2y\\$"));
            return;
        }
        if (modo.equals("doc")) {
            System.out.print(referenciaMarkdown());
            return;
        }

        String ruta = System.getProperty("config", "config.properties");
        if (new File(ruta).isFile()) {
            Config.cargar(ruta);
        } else if (!modo.equals("consola")) {
            Config.cargar(ruta); // lanza un error claro
        }
        Log.iniciar(Config.get("logs.directorio", "logs"));

        switch (modo) {
            case "demonio":
                demonio(false);
                break;
            case "una-vez":
                demonio(true);
                break;
            case "consola":
                consola();
                break;
            case "diagnostico":
                diagnostico();
                break;
            case "enviar-prueba":
                if (args.length < 2) {
                    System.err.println("Uso: enviar-prueba correo@destino.com");
                    System.exit(2);
                }
                enviarPrueba(args[1]);
                break;
            default:
                System.err.println("Modo desconocido: " + modo
                        + "\nModos: demonio | una-vez | consola | diagnostico | enviar-prueba <correo> | hash <clave> | doc");
                System.exit(2);
        }
    }

    // ------------------------------------------------------------------ demonio
    private static void demonio(boolean unaVez) throws Exception {
        File lock = new File(Log.directorio(), "demonio.lock");
        try (RandomAccessFile raf = new RandomAccessFile(lock, "rw"); FileLock fl = raf.getChannel().tryLock()) {
            if (fl == null) {
                Log.sistema("Ya hay otra instancia procesando el buzón; se omite esta ejecución");
                return;
            }
            ProcesadorCorreos p = new ProcesadorCorreos();
            int intervalo = Config.entero("demonio.intervalo_segundos", 10);
            Log.sistema("Iniciado en modo " + (unaVez ? "una-vez" : "demonio (cada " + intervalo + " s)")
                    + " · buzón " + Config.get("pop3.usuario", "?") + "@" + Config.get("pop3.host", "?")
                    + " · BD " + ConexionBD.url() + " · config " + Config.origen());
            Thread principal = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                activo = false;
                Log.sistema("Deteniendo el demonio...");
                try {
                    principal.join(15000);
                } catch (InterruptedException ignorada) {
                    Thread.currentThread().interrupt();
                }
            }));
            do {
                try {
                    int n = p.procesarBuzon();
                    if (n > 0) {
                        Log.sistema("Ciclo terminado: " + n + " comando(s) procesado(s)");
                    }
                } catch (Exception e) {
                    Log.error("Ciclo de lectura del buzón", e);
                }
                for (int s = 0; s < intervalo * 10 && activo && !unaVez; s++) {
                    Thread.sleep(100);
                }
            } while (activo && !unaVez);
        }
    }

    // ------------------------------------------------------------------ consola
    private static void consola() throws IOException {
        ProcesadorCorreos p = new ProcesadorCorreos();
        String remitente = Config.get("consola.remitente", "consola@local");
        boolean interactivo = System.console() != null;
        System.out.println("=== INF513 Grupo 23SA - modo consola (sin correo) · BD " + ConexionBD.url() + " ===");
        System.out.println("Escriba el ASUNTO como si fuera un correo. Ej: AYUDA[\"*\"]  ·  'salir' para terminar.");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (true) {
            if (interactivo) {
                System.out.print("\nasunto> ");
                System.out.flush();
            }
            String linea = in.readLine();
            if (linea == null || linea.trim().equalsIgnoreCase("salir")) {
                break;
            }
            if (linea.trim().isEmpty() || linea.trim().startsWith("#")) {
                continue;
            }
            if (!interactivo) {
                System.out.println("\nasunto> " + linea);
            }
            long t0 = System.currentTimeMillis();
            Resultado r = p.procesarAsunto(linea, remitente);
            System.out.println(r.comoTexto());
            try (Writer w = new OutputStreamWriter(new FileOutputStream(new File(Log.directorio(), "ultima-respuesta.html")),
                    StandardCharsets.UTF_8)) {
                w.write(HtmlRespuesta.generar(r, ProcesadorCorreos.asuntoSeguro(linea), remitente, System.currentTimeMillis() - t0));
            }
        }
    }

    // ------------------------------------------------------------------ diagnóstico
    private static void diagnostico() {
        System.out.println("Configuración: " + Config.origen());
        System.out.print("[BD]   " + ConexionBD.url() + " ... ");
        try (Connection cn = ConexionBD.abrir()) {
            List<String> faltan = new ArrayList<>();
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?")) {
                for (Entidad e : Catalogo.todas()) {
                    ps.setString(1, e.getTabla());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            faltan.add(e.getTabla());
                        }
                    }
                }
            }
            String version;
            try (PreparedStatement ps = cn.prepareStatement("SELECT current_setting('server_version')");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                version = rs.getString(1);
            }
            System.out.println("OK (PostgreSQL " + version + ")"
                    + (faltan.isEmpty() ? " · las " + Catalogo.todas().size() + " tablas existen"
                    : " · FALTAN " + faltan.size() + " tablas: " + faltan + " -> ejecute sql/01_esquema.sql"));
        } catch (Exception e) {
            System.out.println("FALLÓ: " + e.getMessage());
        }

        System.out.print("[POP3] " + Config.get("pop3.usuario", "?") + "@" + Config.get("pop3.host", "?") + ":"
                + Config.get("pop3.puerto", "110") + " ... ");
        try (Pop3Cliente pop = new Pop3Cliente(Config.requerido("pop3.host"), Config.entero("pop3.puerto", 110), 30000)) {
            pop.conectar(Config.requerido("pop3.usuario"), Config.requerido("pop3.password"));
            System.out.println("OK (" + pop.contar() + " correo(s) en el buzón; no se borró nada)");
        } catch (Exception e) {
            System.out.println("FALLÓ: " + e.getMessage());
        }

        System.out.print("[SMTP] " + Config.get("smtp.host", "?") + ":" + Config.get("smtp.puerto", "25")
                + " auth=" + Config.get("smtp.auth", "ninguna") + " ... ");
        try (SmtpCliente smtp = ProcesadorCorreos.nuevoSmtp()) {
            smtp.abrir();
            System.out.println("OK (conexión y saludo correctos; no se envió nada)");
        } catch (Exception e) {
            System.out.println("FALLÓ: " + e.getMessage());
        }
    }

    private static void enviarPrueba(String destino) throws IOException {
        Resultado r = Resultado.exito("Éxito: Correo de prueba",
                "Si recibe este mensaje, el envío SMTP del sistema del Grupo 23SA funciona. Pruebe enviando AYUDA[\"*\"].");
        try (SmtpCliente smtp = ProcesadorCorreos.nuevoSmtp()) {
            smtp.abrir();
            smtp.enviar(destino, "[Éxito] Prueba de envío - Grupo 23SA", HtmlRespuesta.generar(r, "enviar-prueba", destino, 0), null);
        }
        Log.sistema("Correo de prueba enviado a " + destino);
    }

    // ------------------------------------------------------------------ documentación
    static String referenciaMarkdown() {
        StringBuilder md = new StringBuilder();
        md.append("# Referencia de comandos\n\n")
                .append("> Generado automáticamente con `./ejecutar.sh doc` a partir del catálogo (`Catalogo.java`).\n\n")
                .append("Formato del asunto: `COMANDO[\"param1\",\"param2\",...]`. Acciones: **LIS** listar · **INS** insertar · ")
                .append("**MOD** modificar · **ELI** eliminar. Listar acepta `[\"*\"]`, `[\"id\"]` o `[\"campo\",\"valor\"]`.\n\n");
        for (String[] cu : Catalogo.CASOS_DE_USO) {
            List<Entidad> entidades = Catalogo.delCasoDeUso(cu[0]);
            if (entidades.isEmpty()) {
                continue;
            }
            md.append("## ").append(cu[0]).append(" · ").append(cu[1]).append("\n\n");
            for (Entidad e : entidades) {
                md.append("### ").append(e.getCodigo()).append(" · ").append(e.getNombre())
                        .append(" (tabla `").append(e.getTabla()).append("`)\n\n");
                md.append("Comandos: `").append(comandos(e)).append("`\n\n");
                if (!e.isSoloLectura()) {
                    md.append("| N° | Campo | Tipo | Obligatorio | Por defecto |\n|---|---|---|---|---|\n");
                    int i = 1;
                    for (Campo c : e.getCampos()) {
                        md.append("| ").append(i++).append(" | `").append(c.getColumna()).append("` | ")
                                .append(c.describirTipo().replace("|", "\\|")).append(" | ")
                                .append(c.isObligatorio() ? "Sí" : "No").append(" | ")
                                .append(c.getPorDefecto() == null ? "" : c.getPorDefecto()).append(" |\n");
                    }
                    md.append('\n');
                    Resultado ayuda;
                    try {
                        ayuda = Ayuda.generar(new inf513.interprete.Comando("AYUDA",
                                java.util.Collections.singletonList(e.getCodigo())));
                    } catch (Exception ex) {
                        continue;
                    }
                    md.append("```\n");
                    for (String[] fila : ayuda.getTablas().get(1).getFilas()) {
                        md.append(fila[1]).append('\n');
                    }
                    md.append("```\n\n");
                }
            }
        }
        md.append("## CU7 / CU8 · Búsqueda y reportes\n\n| Comando | Descripción |\n|---|---|\n")
                .append("| `BUSCAR[\"texto\"]` | Búsqueda global en los contenidos del sitio |\n");
        for (Map.Entry<String, String[]> r : Reportes.catalogo().entrySet()) {
            md.append("| `").append(r.getKey()).append(r.getValue()[0]).append("` | ").append(r.getValue()[1]).append(" |\n");
        }
        md.append("| `AYUDA[\"*\"]` / `AYUDA[\"USU\"]` | Ayuda general o de una tabla |\n");
        return md.toString();
    }

    private static String comandos(Entidad e) {
        String c = e.getCodigo();
        if (e.isSoloLectura()) {
            return "LIS" + c;
        }
        return "LIS" + c + "`, `INS" + c + (e.permiteModificar() ? "`, `MOD" + c : "") + "`, `ELI" + c;
    }
}
