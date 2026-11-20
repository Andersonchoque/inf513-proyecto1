package inf513.presentacion;

import inf513.datos.Tabla;
import inf513.negocio.Resultado;
import inf513.util.Config;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Arma el correo HTML de respuesta. Usa tablas y estilos en línea porque es lo único
 * que respetan todos los clientes de correo (Gmail, Outlook, móviles).
 */
public final class HtmlRespuesta {

    private static final String AZUL = "#1f3a5f";
    private static final String VERDE = "#1e7b45";
    private static final String ROJO = "#b3261e";

    private HtmlRespuesta() {
    }

    public static String asunto(Resultado r, String comando) {
        String estado = r.isExito() ? "[Éxito] " : "[Error] ";
        String s = estado + comando + " - " + r.getTitulo().replaceFirst("^(Éxito|Error): ", "");
        return s.length() > 150 ? s.substring(0, 147) + "..." : s;
    }

    public static String generar(Resultado r, String asuntoRecibido, String remitente, long ms) {
        String color = r.isExito() ? VERDE : ROJO;
        String fondo = r.isExito() ? "#e9f5ee" : "#fdecea";
        StringBuilder h = new StringBuilder(4096);
        h.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"></head>")
                .append("<body style=\"margin:0;padding:0;background:#f2f4f7;font-family:Arial,Helvetica,sans-serif;color:#1d2433;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f2f4f7;padding:16px 0;\"><tr><td align=\"center\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:760px;background:#ffffff;border:1px solid #d5dae1;\">");

        // Encabezado institucional
        h.append("<tr><td style=\"background:").append(AZUL).append(";padding:16px 20px;color:#ffffff;\">")
                .append("<div style=\"font-size:12px;letter-spacing:1px;text-transform:uppercase;opacity:.85;\">UAGRM · FCEE · INF513 Grupo 23SA</div>")
                .append("<div style=\"font-size:18px;font-weight:bold;margin-top:4px;\">Carrera de Administración de Empresas</div>")
                .append("<div style=\"font-size:13px;margin-top:2px;\">Sistema de gestión por correo electrónico</div>")
                .append("</td></tr>");

        // Estado
        h.append("<tr><td style=\"padding:18px 20px 6px 20px;\">")
                .append("<div style=\"border-left:6px solid ").append(color).append(";background:").append(fondo)
                .append(";padding:12px 14px;\"><div style=\"font-size:17px;font-weight:bold;color:").append(color).append(";\">")
                .append(esc(r.getTitulo())).append("</div>");
        if (r.getMensaje() != null && !r.getMensaje().isEmpty()) {
            h.append("<div style=\"font-size:14px;margin-top:6px;line-height:1.45;\">").append(esc(r.getMensaje())).append("</div>");
        }
        h.append("</div></td></tr>");

        // Tablas
        for (Tabla t : r.getTablas()) {
            h.append("<tr><td style=\"padding:14px 20px 4px 20px;\">")
                    .append("<div style=\"font-size:15px;font-weight:bold;color:").append(AZUL).append(";margin-bottom:6px;\">")
                    .append(esc(t.getTitulo())).append("</div>")
                    .append("<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"border-collapse:collapse;font-size:13px;\"><tr>");
            for (String c : t.getColumnas()) {
                h.append("<th align=\"left\" style=\"background:#e8edf4;border:1px solid #c9d1dc;padding:6px 8px;\">")
                        .append(esc(c)).append("</th>");
            }
            h.append("</tr>");
            int n = 0;
            for (String[] fila : t.getFilas()) {
                String bg = n++ % 2 == 0 ? "#ffffff" : "#f7f9fb";
                h.append("<tr>");
                for (String v : fila) {
                    h.append("<td style=\"border:1px solid #d5dae1;padding:6px 8px;vertical-align:top;background:").append(bg).append(";\">")
                            .append(esc(v)).append("</td>");
                }
                h.append("</tr>");
            }
            if (t.getFilas().isEmpty()) {
                h.append("<tr><td colspan=\"").append(t.getColumnas().size())
                        .append("\" style=\"border:1px solid #d5dae1;padding:8px;color:#6b7383;\">Sin registros</td></tr>");
            }
            h.append("</table>");
            if (t.getNota() != null) {
                h.append("<div style=\"font-size:12px;color:#6b7383;margin-top:4px;\">").append(esc(t.getNota())).append("</div>");
            }
            h.append("</td></tr>");
        }

        // Pie con la traza de la solicitud
        String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        h.append("<tr><td style=\"padding:18px 20px;\"><table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" ")
                .append("style=\"font-size:12px;color:#4a5262;border-top:1px solid #d5dae1;\">")
                .append(filaPie("Comando recibido", asuntoRecibido))
                .append(filaPie("Remitente", remitente))
                .append(filaPie("Procesado", fecha + " (" + ms + " ms)"))
                .append("</table><div style=\"font-size:12px;color:#6b7383;margin-top:10px;\">")
                .append("Envíe un correo a ").append(esc(Config.get("correo.buzon", "grupo23sa@tecnoweb.org.bo")))
                .append(" con el asunto <b>AYUDA[\"*\"]</b> para ver todos los comandos.")
                .append("</div></td></tr>");

        h.append("</table></td></tr></table></body></html>");
        return h.toString();
    }

    private static String filaPie(String etiqueta, String valor) {
        return "<tr><td style=\"padding:6px 8px 0 0;white-space:nowrap;vertical-align:top;\"><b>" + esc(etiqueta)
                + "</b></td><td style=\"padding:6px 0 0 0;word-break:break-all;\"><code>" + esc(valor) + "</code></td></tr>";
    }

    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\n':
                    sb.append("<br>");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
