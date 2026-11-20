package inf513.negocio;

import static inf513.negocio.Campo.booleano;
import static inf513.negocio.Campo.clave;
import static inf513.negocio.Campo.email;
import static inf513.negocio.Campo.entero;
import static inf513.negocio.Campo.fecha;
import static inf513.negocio.Campo.hora;
import static inf513.negocio.Campo.id;
import static inf513.negocio.Campo.opcion;
import static inf513.negocio.Campo.slug;
import static inf513.negocio.Campo.texto;
import static inf513.negocio.Campo.textoLargo;
import static inf513.negocio.Campo.url;

import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Catálogo de tablas gestionables por correo, agrupadas por caso de uso.
 * Cada entidad genera 4 comandos: LIS<cod>, INS<cod>, MOD<cod>, ELI<cod>.
 * Los valores de las opciones son los mismos de app/Support/Catalogos.php del sistema web.
 */
public final class Catalogo {

    public static final String[][] CASOS_DE_USO = {
        {"CU1", "Gestión de usuarios (roles y permisos)"},
        {"CU2", "Portal web y bitácora de auditoría"},
        {"CU3", "Información académica, malla curricular y plantel docente"},
        {"CU4", "Investigación, Revista INNOVAR y repositorio documental"},
        {"CU5", "Vinculación, Empresa Junior INNOVA y graduados"},
        {"CU6", "Noticias, eventos y galería multimedia"},
        {"CU7", "Consultas del formulario de contacto y búsqueda"},
        {"CU8", "Reportes y estadísticas"},
    };

    private static final Map<String, Entidad> ENTIDADES = new LinkedHashMap<>();

    static {
        // ------------------------------------------------------------------ CU1
        agregar(new Entidad("ROL", "roles", "Roles", "CU1").campos(
                texto("nombre", 60).obligatorio().ejemplo("Editor"),
                texto("descripcion", 255).ejemplo("Edita contenido del sitio"),
                booleano("acceso_panel", true)));
        agregar(new Entidad("PRM", "permisos", "Permisos", "CU1").campos(
                texto("nombre", 80).obligatorio().ejemplo("noticias.publicar"),
                texto("descripcion", 255).ejemplo("Publicar noticias"),
                texto("modulo", 60).obligatorio().ejemplo("Difusión")));
        agregar(new Entidad("RPE", "rol_permiso", "Permisos asignados a roles", "CU1")
                .llaveCompuesta("rol_id", "permiso_id")
                .campos(id("rol_id", "roles").obligatorio().ejemplo("2"), id("permiso_id", "permisos").obligatorio().ejemplo("1"))
                .ordenarPor("rol_id, permiso_id"));
        agregar(new Entidad("USU", "users", "Usuarios", "CU1").campos(
                texto("name", 255).obligatorio().ejemplo("Juan Carlos Pérez"),
                email("email", 255).obligatorio().ejemplo("juanperez@uagrm.edu.bo"),
                texto("telefono", 20).ejemplo("71055123"),
                id("rol_id", "roles"),
                booleano("activo", true),
                clave("password").obligatorio())
                .listado("id", "name", "email", "telefono", "rol_id", "activo", "created_at"));

        // ------------------------------------------------------------------ CU2
        agregar(new Entidad("BIT", "bitacora", "Bitácora de auditoría", "CU2").soloLectura().sinTimestamps()
                .campos(id("user_id", "users"), texto("accion", 30), texto("modulo", 60),
                        texto("descripcion", 500), entero("registro_id"), texto("ip", 45), texto("agente", 255))
                .listado("id", "created_at", "user_id", "accion", "modulo", "descripcion", "registro_id", "agente")
                .ordenarPor("id DESC"));
        agregar(new Entidad("MEN", "menu_items", "Menú del panel", "CU2").campos(
                texto("etiqueta", 80).obligatorio().ejemplo("Reportes"),
                texto("ruta", 120).ejemplo("admin.reportes.index"),
                texto("icono", 40).ejemplo("bar-chart"),
                texto("permiso", 80).ejemplo("reportes.ver"),
                id("padre_id", "menu_items").ejemplo(""),
                entero("orden").porDefecto("0").rango(0, 10000),
                booleano("activo", true)).ordenarPor("orden, id"));
        agregar(new Entidad("BAN", "banners", "Banners del carrusel", "CU2").campos(
                texto("titulo", 150).obligatorio().ejemplo("Inscripciones abiertas 2027"),
                texto("subtitulo", 255).ejemplo("Carrera de Administración de Empresas"),
                texto("imagen", 255).ejemplo("banners/inscripciones.jpg"),
                url("enlace", 255),
                texto("texto_boton", 40).ejemplo("Más información"),
                entero("orden").porDefecto("0").rango(0, 10000),
                booleano("activo", true)).ordenarPor("orden, id"));
        agregar(new Entidad("DES", "destacados", "Indicadores destacados", "CU2").campos(
                texto("titulo", 120).obligatorio().ejemplo("Estudiantes"),
                texto("descripcion", 255).ejemplo("Matriculados en la gestión"),
                texto("valor", 40).ejemplo("3500+"),
                url("enlace", 255).ejemplo(""),
                entero("orden").porDefecto("0").rango(0, 10000),
                booleano("activo", true)).ordenarPor("orden, id"));
        agregar(new Entidad("ENL", "enlaces_plataforma", "Enlaces a plataformas", "CU2").campos(
                texto("nombre", 100).obligatorio().ejemplo("Moodle FCEE"),
                url("url", 255).obligatorio().ejemplo("https://moodle.uagrm.edu.bo"),
                texto("descripcion", 255).ejemplo("Aula virtual"),
                entero("orden").porDefecto("0").rango(0, 10000),
                booleano("activo", true)).ordenarPor("orden, id"));
        agregar(new Entidad("CNF", "configuraciones", "Configuraciones del portal", "CU2").campos(
                texto("clave", 80).obligatorio().ejemplo("telefono_contacto"),
                textoLargo("valor").ejemplo("3-3355000"),
                texto("grupo", 40).porDefecto("general").ejemplo("contacto")));
        agregar(new Entidad("VIS", "contador_visitas", "Contador de visitas por página", "CU2").soloLectura()
                .campos(texto("ruta", 191), entero("total"))
                .listado("id", "ruta", "total", "ultima_visita").ordenarPor("total DESC"));

        // ------------------------------------------------------------------ CU3
        agregar(new Entidad("PAG", "paginas", "Páginas de contenido", "CU3").campos(
                opcion("seccion", "institucional", "academico", "revista", "innova", "graduados").obligatorio(),
                texto("titulo", 150).obligatorio().ejemplo("Misión y visión"),
                slug("slug", 120, "titulo"),
                texto("resumen", 255).ejemplo("Propósito de la Carrera"),
                textoLargo("contenido").ejemplo("Formar profesionales líderes..."),
                texto("imagen", 255),
                entero("orden").porDefecto("0").rango(0, 10000),
                booleano("publicado", true))
                .listado("id", "seccion", "titulo", "slug", "orden", "publicado").ordenarPor("seccion, orden, id"));
        agregar(new Entidad("DOC", "docentes", "Autoridades y docentes", "CU3").campos(
                texto("nombre", 150).obligatorio().ejemplo("Lic. María Fernández"),
                opcion("tipo", "autoridad", "docente", "administrativo").porDefecto("docente"),
                texto("cargo", 120).ejemplo("Docente titular"),
                texto("formacion", 255).ejemplo("MBA en Finanzas"),
                texto("especialidad", 150).ejemplo("Finanzas corporativas"),
                texto("materias", 255).ejemplo("Finanzas I, Finanzas II"),
                email("correo", 150).ejemplo("mfernandez@uagrm.edu.bo"),
                texto("foto", 255),
                booleano("mostrar_correo", false),
                booleano("mostrar_formacion", true),
                booleano("visible", true),
                entero("orden").porDefecto("0").rango(0, 10000))
                .listado("id", "nombre", "tipo", "cargo", "especialidad", "correo", "visible").ordenarPor("orden, nombre"));
        agregar(new Entidad("MAT", "materias", "Materias de la malla curricular", "CU3").campos(
                texto("sigla", 20).obligatorio().ejemplo("ADM-300"),
                texto("nombre", 150).obligatorio().ejemplo("Administración II"),
                entero("semestre").obligatorio().rango(1, 12).ejemplo("3"),
                entero("creditos").porDefecto("0").rango(0, 30).ejemplo("5"),
                entero("horas_teoricas").porDefecto("0").rango(0, 40).ejemplo("4"),
                entero("horas_practicas").porDefecto("0").rango(0, 40).ejemplo("2"),
                opcion("tipo", "obligatoria", "electiva", "modalidad").porDefecto("obligatoria"),
                textoLargo("descripcion"))
                .listado("id", "sigla", "nombre", "semestre", "creditos", "tipo").ordenarPor("semestre, sigla"));
        agregar(new Entidad("PRE", "materia_prerequisito", "Prerrequisitos de materias", "CU3")
                .llaveCompuesta("materia_id", "prerequisito_id")
                .campos(id("materia_id", "materias").obligatorio().ejemplo("11"),
                        id("prerequisito_id", "materias").obligatorio().ejemplo("1"))
                .regla(v -> {
                    if (Objects.equals(v.get("materia_id"), v.get("prerequisito_id"))) {
                        throw new ErrorValidacion("una materia no puede ser prerrequisito de sí misma");
                    }
                })
                .ordenarPor("materia_id, prerequisito_id"));
        agregar(new Entidad("CAL", "actividades_calendario", "Calendario académico", "CU3").campos(
                texto("titulo", 150).obligatorio().ejemplo("Inscripción de materias"),
                texto("descripcion", 255).ejemplo("Inscripción por sistema"),
                fecha("fecha_inicio").obligatorio().ejemplo("2027-02-01"),
                fecha("fecha_fin").ejemplo("2027-02-15"),
                opcion("tipo", "inscripciones", "clases", "examenes", "feriado", "academico", "titulacion").porDefecto("academico"),
                texto("gestion", 20).obligatorio().ejemplo("1-2027"))
                .regla(fechasOrdenadas("fecha_inicio", "fecha_fin")).ordenarPor("fecha_inicio"));
        agregar(new Entidad("FAQ", "preguntas_frecuentes", "Preguntas frecuentes", "CU3").campos(
                texto("pregunta", 255).obligatorio().ejemplo("¿Cuándo son las inscripciones?"),
                textoLargo("respuesta").obligatorio().ejemplo("Según el calendario académico publicado."),
                texto("categoria", 60).ejemplo("Inscripciones"),
                entero("orden").porDefecto("0").rango(0, 10000),
                booleano("activo", true)).ordenarPor("orden, id"));

        // ------------------------------------------------------------------ CU4
        agregar(new Entidad("CDO", "categorias_documento", "Categorías de documentos", "CU4").campos(
                texto("nombre", 80).obligatorio().ejemplo("Resoluciones"),
                slug("slug", 100, "nombre"),
                texto("descripcion", 255).ejemplo("Resoluciones de la Dirección de Carrera")));
        agregar(new Entidad("BIB", "documentos", "Biblioteca digital (documentos)", "CU4").campos(
                id("categoria_documento_id", "categorias_documento").obligatorio(),
                texto("titulo", 200).obligatorio().ejemplo("Reglamento de Graduación"),
                texto("descripcion", 500).ejemplo("Versión aprobada 2026"),
                texto("archivo", 255).ejemplo("documentos/reglamento-graduacion.pdf"),
                fecha("fecha_publicacion").ejemplo("2026-03-15"),
                entero("descargas").porDefecto("0").rango(0, Integer.MAX_VALUE).ejemplo("0"),
                booleano("publicado", true))
                .listado("id", "categoria_documento_id", "titulo", "fecha_publicacion", "descargas", "publicado"));
        agregar(new Entidad("LIN", "lineas_investigacion", "Líneas de investigación", "CU4").campos(
                texto("nombre", 150).obligatorio().ejemplo("Marketing digital"),
                textoLargo("descripcion").ejemplo("Estrategias de mercadeo en entornos digitales"),
                booleano("activo", true)));
        agregar(new Entidad("PRY", "proyectos_investigacion", "Proyectos de investigación", "CU4").campos(
                id("linea_investigacion_id", "lineas_investigacion"),
                opcion("tipo", "proyecto", "pia", "semillero", "produccion").porDefecto("proyecto"),
                texto("titulo", 200).obligatorio().ejemplo("Competitividad de las PyMEs cruceñas"),
                slug("slug", 220, "titulo"),
                textoLargo("descripcion").ejemplo("Estudio de factores de competitividad"),
                texto("responsables", 255).ejemplo("Lic. Ana Rojas; Est. Luis Vaca"),
                fecha("fecha_inicio").ejemplo("2026-03-01"),
                fecha("fecha_fin").ejemplo("2026-12-15"),
                opcion("estado", "planificado", "en_curso", "finalizado").porDefecto("en_curso"),
                textoLargo("resultados"),
                url("enlace_publicacion", 255),
                url("enlace_repositorio", 255).ejemplo(""),
                texto("imagen", 255),
                booleano("publicado", true))
                .regla(fechasOrdenadas("fecha_inicio", "fecha_fin"))
                .listado("id", "tipo", "titulo", "responsables", "estado", "fecha_inicio", "publicado"));
        agregar(new Entidad("EDI", "revista_ediciones", "Ediciones de la Revista INNOVAR", "CU4").campos(
                entero("numero").obligatorio().rango(1, 100000).ejemplo("5"),
                texto("titulo", 200).obligatorio().ejemplo("INNOVAR N° 5"),
                fecha("fecha_publicacion").ejemplo("2026-12-01"),
                textoLargo("descripcion").ejemplo("Edición dedicada al emprendimiento"),
                texto("portada", 255),
                texto("archivo_pdf", 255),
                booleano("publicado", true))
                .listado("id", "numero", "titulo", "fecha_publicacion", "publicado").ordenarPor("numero DESC"));
        agregar(new Entidad("ART", "revista_articulos", "Artículos de la Revista INNOVAR", "CU4").campos(
                id("revista_edicion_id", "revista_ediciones").obligatorio(),
                texto("titulo", 255).obligatorio().ejemplo("El emprendimiento femenino en Santa Cruz"),
                texto("autores", 255).obligatorio().ejemplo("Ana Rojas, Luis Vaca"),
                textoLargo("resumen").ejemplo("Se analiza..."),
                texto("palabras_clave", 255).ejemplo("emprendimiento, género, PyME"),
                texto("archivo_pdf", 255),
                entero("orden").porDefecto("0").rango(0, 10000))
                .listado("id", "revista_edicion_id", "titulo", "autores", "orden").ordenarPor("revista_edicion_id, orden"));

        // ------------------------------------------------------------------ CU5
        agregar(new Entidad("CNV", "convenios", "Convenios institucionales", "CU5").campos(
                texto("institucion", 200).obligatorio().ejemplo("Cámara de Industria y Comercio (CAINCO)"),
                opcion("tipo", "convenio", "practica", "proyecto_conjunto", "consultoria", "interaccion_social").porDefecto("convenio"),
                textoLargo("finalidad").ejemplo("Prácticas empresariales para estudiantes"),
                fecha("fecha_inicio").ejemplo("2026-01-10"),
                fecha("fecha_fin").ejemplo("2028-01-10"),
                texto("logo", 255),
                opcion("estado", "vigente", "en_tramite", "finalizado").porDefecto("vigente"),
                booleano("publicado", true))
                .regla(fechasOrdenadas("fecha_inicio", "fecha_fin"))
                .listado("id", "institucion", "tipo", "fecha_inicio", "fecha_fin", "estado"));
        agregar(new Entidad("SER", "innova_servicios", "Servicios de INNOVA", "CU5").campos(
                texto("nombre", 150).obligatorio().ejemplo("Plan de negocios"),
                textoLargo("descripcion").ejemplo("Elaboración de planes de negocio para PyMEs"),
                entero("orden").porDefecto("0").rango(0, 10000),
                booleano("activo", true)).ordenarPor("orden, id"));
        agregar(new Entidad("INT", "innova_integrantes", "Integrantes de INNOVA", "CU5").campos(
                texto("nombre", 150).obligatorio().ejemplo("Carla Justiniano"),
                texto("cargo", 100).ejemplo("Gerente general"),
                texto("semestre", 30).ejemplo("8vo semestre"),
                texto("foto", 255),
                entero("orden").porDefecto("0").rango(0, 10000),
                booleano("activo", true)).ordenarPor("orden, id"));
        agregar(new Entidad("IPR", "innova_proyectos", "Proyectos de INNOVA", "CU5").campos(
                texto("titulo", 200).obligatorio().ejemplo("Estudio de mercado para cafetería"),
                texto("cliente", 150).ejemplo("Café Patujú"),
                textoLargo("descripcion"),
                fecha("fecha").ejemplo("2026-09-20"),
                texto("imagen", 255),
                booleano("publicado", true))
                .listado("id", "titulo", "cliente", "fecha", "publicado"));
        agregar(new Entidad("SOL", "innova_solicitudes", "Solicitudes de servicio a INNOVA", "CU5").campos(
                id("innova_servicio_id", "innova_servicios"),
                texto("empresa", 150).obligatorio().ejemplo("Distribuidora El Sol"),
                texto("contacto", 150).obligatorio().ejemplo("Pedro Suárez"),
                email("correo", 150).obligatorio().ejemplo("pedro@elsol.com"),
                texto("telefono", 30).ejemplo("70011223"),
                textoLargo("mensaje").obligatorio().ejemplo("Necesitamos un plan de marketing"),
                opcion("estado", "pendiente", "en_proceso", "atendida", "rechazada").porDefecto("pendiente"),
                textoLargo("observacion"))
                .listado("id", "empresa", "contacto", "correo", "innova_servicio_id", "estado", "created_at"));
        agregar(new Entidad("POS", "innova_postulaciones", "Postulaciones a INNOVA", "CU5").campos(
                texto("nombre", 150).obligatorio().ejemplo("Luis Vaca"),
                texto("registro", 20).obligatorio().ejemplo("219012345"),
                email("correo", 150).obligatorio().ejemplo("luisvaca@uagrm.edu.bo"),
                texto("telefono", 30).ejemplo("76543210"),
                texto("semestre", 30).ejemplo("6to"),
                textoLargo("motivacion").obligatorio().ejemplo("Quiero aplicar lo aprendido en consultoría"),
                opcion("estado", "pendiente", "entrevista", "aceptada", "rechazada").porDefecto("pendiente"),
                textoLargo("observacion"))
                .listado("id", "nombre", "registro", "correo", "semestre", "estado", "created_at"));
        agregar(new Entidad("GRA", "graduados", "Graduados (Alumni)", "CU5").campos(
                id("user_id", "users").ejemplo(""),
                texto("nombre", 150).obligatorio().ejemplo("Sofía Paz"),
                texto("ci", 20).obligatorio().ejemplo("4715292"),
                texto("registro", 20).ejemplo("209876543"),
                entero("anio_graduacion").obligatorio().rango(1950, Year.now().getValue() + 1).ejemplo("2024"),
                opcion("modalidad_titulacion", "tesis", "proyecto_grado", "trabajo_dirigido", "examen_grado", "excelencia", "diplomado"),
                opcion("situacion_laboral", "dependiente", "independiente", "busca_empleo", "estudiando", "otro").obligatorio(),
                texto("empresa", 150).ejemplo("Banco Ganadero"),
                texto("cargo", 120).ejemplo("Analista de créditos"),
                texto("ciudad", 80).ejemplo("Santa Cruz"),
                email("correo", 150).obligatorio().ejemplo("sofiapaz@gmail.com"),
                texto("telefono", 30).ejemplo("77001122"),
                url("linkedin", 255).ejemplo(""))
                .listado("id", "nombre", "ci", "anio_graduacion", "situacion_laboral", "empresa", "correo"));

        // ------------------------------------------------------------------ CU6
        agregar(new Entidad("CNO", "categorias_noticia", "Categorías de noticias", "CU6").campos(
                texto("nombre", 80).obligatorio().ejemplo("Deportes"),
                slug("slug", 100, "nombre")));
        agregar(new Entidad("NOT", "noticias", "Noticias y comunicados", "CU6").campos(
                id("categoria_noticia_id", "categorias_noticia"),
                id("user_id", "users").ejemplo(""),
                texto("titulo", 200).obligatorio().ejemplo("Feria de emprendimiento 2026"),
                slug("slug", 220, "titulo"),
                texto("resumen", 300).ejemplo("Más de 40 emprendimientos estudiantiles"),
                textoLargo("contenido").ejemplo("La Carrera realizó su feria anual..."),
                texto("imagen", 255),
                fecha("fecha_publicacion").obligatorio().ejemplo("2026-11-20"),
                opcion("estado", "borrador", "publicado", "archivado").porDefecto("publicado"),
                booleano("destacado", false))
                .listado("id", "titulo", "categoria_noticia_id", "fecha_publicacion", "estado", "destacado")
                .ordenarPor("fecha_publicacion DESC, id DESC"));
        agregar(new Entidad("EVE", "eventos", "Eventos académicos", "CU6").campos(
                texto("titulo", 200).obligatorio().ejemplo("Congreso de Administración 2026"),
                slug("slug", 220, "titulo"),
                opcion("tipo", "conferencia", "seminario", "taller", "feria", "congreso", "otro").porDefecto("conferencia"),
                textoLargo("descripcion").ejemplo("Tres días de conferencias"),
                fecha("fecha").obligatorio().ejemplo("2026-12-05"),
                hora("hora_inicio").ejemplo("09:00"),
                hora("hora_fin").ejemplo("12:30"),
                texto("lugar", 200).ejemplo("Auditorio FCEE"),
                texto("afiche", 255),
                url("enlace_inscripcion", 255).ejemplo(""),
                booleano("publicado", true))
                .regla(v -> {
                    Object ini = v.get("hora_inicio");
                    Object fin = v.get("hora_fin");
                    if (ini != null && fin != null && ((java.sql.Time) fin).before((java.sql.Time) ini)) {
                        throw new ErrorValidacion("hora_fin no puede ser anterior a hora_inicio");
                    }
                })
                .listado("id", "titulo", "tipo", "fecha", "hora_inicio", "lugar", "publicado").ordenarPor("fecha DESC"));
        agregar(new Entidad("ALB", "albumes", "Álbumes de la galería", "CU6").campos(
                texto("titulo", 200).obligatorio().ejemplo("Graduación 2026"),
                slug("slug", 220, "titulo"),
                texto("descripcion", 500).ejemplo("Acto de graduación de la promoción 2026"),
                fecha("fecha").ejemplo("2026-10-15"),
                texto("portada", 255),
                booleano("publicado", true))
                .listado("id", "titulo", "slug", "fecha", "publicado"));
        agregar(new Entidad("FOT", "fotos", "Fotos de la galería", "CU6").campos(
                id("album_id", "albumes").obligatorio(),
                texto("imagen", 255).obligatorio().ejemplo("galeria/graduacion-01.jpg"),
                texto("descripcion", 255).ejemplo("Entrega de títulos"),
                entero("orden").porDefecto("0").rango(0, 10000)).ordenarPor("album_id, orden"));
        agregar(new Entidad("VID", "videos", "Videos de la galería", "CU6").campos(
                id("album_id", "albumes").ejemplo(""),
                texto("titulo", 200).obligatorio().ejemplo("Resumen del congreso"),
                url("url", 255).obligatorio().ejemplo("https://www.youtube.com/watch?v=abc123"),
                texto("descripcion", 500),
                booleano("publicado", true)));

        // ------------------------------------------------------------------ CU7
        agregar(new Entidad("CCO", "categorias_consulta", "Categorías de consulta", "CU7").campos(
                texto("nombre", 80).obligatorio().ejemplo("Becas"),
                email("correo_destino", 150).ejemplo("becas@uagrm.edu.bo")));
        agregar(new Entidad("CON", "consultas", "Consultas del formulario de contacto", "CU7").campos(
                id("categoria_consulta_id", "categorias_consulta"),
                texto("nombre", 150).obligatorio().ejemplo("Mariana López"),
                email("correo", 150).obligatorio().ejemplo("mariana@gmail.com"),
                texto("telefono", 30).ejemplo("70123456"),
                textoLargo("mensaje").obligatorio().ejemplo("¿Cuáles son los requisitos de inscripción?"),
                opcion("estado", "nueva", "en_revision", "respondida", "cerrada").porDefecto("nueva"),
                textoLargo("respuesta"))
                .listado("id", "nombre", "correo", "categoria_consulta_id", "estado", "created_at")
                .ordenarPor("id DESC"));
    }

    private Catalogo() {
    }

    private static void agregar(Entidad e) {
        ENTIDADES.put(e.getCodigo(), e);
    }

    public static Entidad buscar(String codigo) {
        return codigo == null ? null : ENTIDADES.get(codigo.toUpperCase());
    }

    public static Collection<Entidad> todas() {
        return ENTIDADES.values();
    }

    public static List<Entidad> delCasoDeUso(String cu) {
        List<Entidad> lista = new ArrayList<>();
        for (Entidad e : ENTIDADES.values()) {
            if (e.getCasoDeUso().equals(cu)) {
                lista.add(e);
            }
        }
        return lista;
    }

    private static Entidad.Regla fechasOrdenadas(String inicio, String fin) {
        return v -> {
            Object a = v.get(inicio);
            Object b = v.get(fin);
            if (a != null && b != null && ((java.sql.Date) b).before((java.sql.Date) a)) {
                throw new ErrorValidacion(fin + " no puede ser anterior a " + inicio);
            }
        };
    }
}
