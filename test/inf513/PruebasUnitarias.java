package inf513;

import inf513.correo.Mime;
import inf513.interprete.Comando;
import inf513.interprete.ErrorSintaxis;
import inf513.interprete.Parser;
import inf513.negocio.Campo;
import inf513.negocio.ErrorValidacion;
import inf513.negocio.Validador;
import java.util.Arrays;

/** Pruebas sin red ni base de datos: parser, MIME y validación de tipos. */
public final class PruebasUnitarias {

    private static int ok;
    private static int fallos;

    public static void main(String[] args) {
        // ---------------- Parser
        comando("LISPER[\"*\"]", "LISPER", "*");
        comando("INSPER[\"4715292\",\"Juan Carlos\",\"Perez Seras\",\"Estudiante\",\"33554433\",\"71055123\",\"juanperez@uagrm.edu.bo\"]",
                "INSPER", "4715292", "Juan Carlos", "Perez Seras", "Estudiante", "33554433", "71055123", "juanperez@uagrm.edu.bo");
        comando("   lisusu  [  \"*\"   ]   ", "LISUSU", "*");
        comando("INSMAT[\"a\" ,\t\"b\"]", "INSMAT", "a", "b");
        comando("INSFAQ[\"Dijo \\\"hola\\\"\",\"C:\\\\ruta\"]", "INSFAQ", "Dijo \"hola\"", "C:\\ruta");
        comando("INSNOT[\u201CTítulo con ñ\u201D,\u201Cotro\u201D]", "INSNOT", "Título con ñ", "otro");
        comando("Re: RV: Fwd: LISROL[\"*\"]", "LISROL", "*");
        comando("AYUDA[]", "AYUDA");
        comando("INSCON[\"\",\"\"]", "INSCON", "", "");
        comando("INSX[\"a,b]c\"]", "INSX", "a,b]c");
        error("LISUSU[*]", "comillas");
        error("LISUSU[\"*\"", "]");
        error("LISUSU", "'['");
        error("LISUSU[\"*\"] LISROL[\"*\"]", "texto de más");
        error("INSUSU[\"a\" \"b\"]", "coma");
        error("INSUSU[\"abierta]", "comilla doble de cierre");
        error("[\"*\"]", "nombre del comando");
        error("", "vacío");
        error("INSUSU[\"a\",]", "parámetro 2");

        // ---------------- MIME
        igual("B64", Mime.decodificar("=?UTF-8?B?SU5TTUFUWyJBRE0tMzAwIiwiQWRtaW5pc3RyYWNpw7NuIl0=?="),
                "INSMAT[\"ADM-300\",\"Administración\"]");
        igual("Q con _", Mime.decodificar("=?UTF-8?Q?BUSCAR=5B=22gesti=C3=B3n_p=C3=BAblica=22=5D?="), "BUSCAR[\"gestión pública\"]");
        igual("multibyte partido", Mime.decodificar("=?UTF-8?Q?Administraci=C3?= =?UTF-8?Q?=B3n?="), "Administración");
        igual("ISO-8859-1", Mime.decodificar("=?ISO-8859-1?Q?Peque=F1o?="), "Pequeño");
        igual("mezcla", Mime.decodificar("Re: =?utf-8?b?w5FhbmR1?= final"), "Re: Ñandu final");
        igual("UTF-8 crudo", Mime.decodificar(new String("Gestión".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.charset.StandardCharsets.ISO_8859_1)), "Gestión");
        String largo = "[Éxito] INSNOT - Registro insertado correctamente con tildes áéíóú ñ y emojis 🎓";
        igual("ida y vuelta", Mime.decodificar(Mime.codificarCabecera(largo).replace("\r\n ", " ")), largo);
        igual("from angular", Mime.extraerDireccion("\"Juan Pérez\" <Juan.Perez@Gmail.com>"), "juan.perez@gmail.com");
        igual("from suelto", Mime.extraerDireccion("luirfer.camacho@gmail.com (Luis)"), "luirfer.camacho@gmail.com");
        igual("cabeceras plegadas", Mime.parsearCabeceras("Subject: LISUSU[\r\n \"*\"]\r\nFrom: a@b.co\r\n\r\ncuerpo").get("subject"),
                "LISUSU[ \"*\"]");

        // ---------------- Validador
        valida(Campo.fecha("f"), "2026-11-20", java.sql.Date.valueOf("2026-11-20"));
        valida(Campo.fecha("f"), "20/11/2026", java.sql.Date.valueOf("2026-11-20"));
        rechaza(Campo.fecha("f"), "2026-02-30");
        rechaza(Campo.entero("n").rango(1, 12), "13");
        rechaza(Campo.entero("n"), "12abc");
        valida(Campo.booleano("b", true), "Sí", Boolean.TRUE);
        valida(Campo.booleano("b", true), "", Boolean.TRUE);
        valida(Campo.opcion("e", "en_curso", "finalizado"), "En Curso", "en_curso");
        rechaza(Campo.opcion("e", "en_curso", "finalizado"), "cancelado");
        rechaza(Campo.email("c", 150), "sin-arroba.com");
        rechaza(Campo.texto("t", 5).obligatorio(), "");
        rechaza(Campo.texto("t", 5), "123456");
        valida(Campo.hora("h"), "9:30", java.sql.Time.valueOf("09:30:00"));
        igual("slug", Validador.slugificar("¿Congreso de Administración 2026?"), "congreso-de-administracion-2026");

        System.out.println("\nPruebas unitarias: " + ok + " OK, " + fallos + " fallidas");
        System.exit(fallos == 0 ? 0 : 1);
    }

    private static void comando(String asunto, String nombre, String... params) {
        try {
            Comando c = Parser.analizar(asunto);
            boolean bien = c.getNombre().equals(nombre) && c.getParametros().equals(Arrays.asList(params));
            registrar(bien, "parser " + asunto, c.toString());
        } catch (ErrorSintaxis e) {
            registrar(false, "parser " + asunto, "error inesperado: " + e.getMessage());
        }
    }

    private static void error(String asunto, String fragmento) {
        try {
            Parser.analizar(asunto);
            registrar(false, "error esperado en " + asunto, "se aceptó");
        } catch (ErrorSintaxis e) {
            registrar(e.getMessage().toLowerCase().contains(fragmento.toLowerCase()), "error en " + asunto, e.getMessage());
        }
    }

    private static void valida(Campo c, String v, Object esperado) {
        try {
            Object r = Validador.convertir(c, v, 1);
            registrar(esperado.equals(r), "valida " + c.getColumna() + "=" + v, String.valueOf(r));
        } catch (ErrorValidacion e) {
            registrar(false, "valida " + v, e.getMessage());
        }
    }

    private static void rechaza(Campo c, String v) {
        try {
            Validador.convertir(c, v, 1);
            registrar(false, "rechaza " + c.getColumna() + "=" + v, "se aceptó");
        } catch (ErrorValidacion e) {
            registrar(true, "rechaza " + c.getColumna() + "=" + v, e.getMessage());
        }
    }

    private static void igual(String nombre, String obtenido, String esperado) {
        registrar(esperado.equals(obtenido), nombre, obtenido);
    }

    private static void registrar(boolean bien, String nombre, String detalle) {
        if (bien) {
            ok++;
            System.out.println("  OK    " + nombre);
        } else {
            fallos++;
            System.out.println("  FALLA " + nombre + " -> " + detalle);
        }
    }
}
