-- =====================================================================
--  Datos iniciales (tomados de los seeders del sistema web Laravel)
-- =====================================================================

INSERT INTO permisos (nombre, modulo, descripcion, created_at, updated_at) VALUES
('usuarios.gestionar',      'Usuarios',      'Gestionar usuarios, roles y menú', now(), now()),
('portal.gestionar',        'Portal web',    'Administrar página principal, contacto y redes', now(), now()),
('bitacora.ver',            'Portal web',    'Consultar la bitácora de auditoría', now(), now()),
('academico.gestionar',     'Académico',     'Contenido institucional, académico, malla y calendario', now(), now()),
('docentes.gestionar',      'Académico',     'Autoridades y docentes', now(), now()),
('documentos.gestionar',    'Investigación', 'Biblioteca digital de documentos', now(), now()),
('investigacion.gestionar', 'Investigación', 'Líneas y proyectos de investigación', now(), now()),
('revista.gestionar',       'Investigación', 'Revista INNOVAR', now(), now()),
('vinculacion.gestionar',   'Vinculación',   'Convenios y relaciones externas', now(), now()),
('innova.gestionar',        'Vinculación',   'Empresa Junior INNOVA', now(), now()),
('graduados.gestionar',     'Vinculación',   'Seguimiento a graduados', now(), now()),
('noticias.gestionar',      'Difusión',      'Noticias y comunicados', now(), now()),
('eventos.gestionar',       'Difusión',      'Eventos académicos', now(), now()),
('galeria.gestionar',       'Difusión',      'Galería multimedia', now(), now()),
('consultas.gestionar',     'Consultas',     'Consultas del formulario de contacto', now(), now()),
('reportes.ver',            'Reportes',      'Reportes y estadísticas', now(), now());

INSERT INTO roles (nombre, descripcion, acceso_panel, created_at, updated_at) VALUES
('Administrador',              'Acceso total al sistema', TRUE, now(), now()),
('Responsable de Noticias',    'Publica noticias, eventos y galería', TRUE, now(), now()),
('Encargado Revista INNOVAR',  'Administra la revista, investigación y biblioteca', TRUE, now(), now()),
('Administrador de Graduados', 'Seguimiento a graduados, INNOVA y convenios', TRUE, now(), now()),
('Secretaría Académica',       'Contenido académico, plantel y consultas', TRUE, now(), now()),
('Graduado',                   'Titulado que actualiza sus datos desde el sitio', FALSE, now(), now());

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p WHERE r.nombre = 'Administrador';

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r JOIN permisos p ON (
       (r.nombre = 'Responsable de Noticias'    AND p.nombre IN ('noticias.gestionar','eventos.gestionar','galeria.gestionar','reportes.ver'))
    OR (r.nombre = 'Encargado Revista INNOVAR'  AND p.nombre IN ('revista.gestionar','investigacion.gestionar','documentos.gestionar'))
    OR (r.nombre = 'Administrador de Graduados' AND p.nombre IN ('graduados.gestionar','innova.gestionar','vinculacion.gestionar','reportes.ver'))
    OR (r.nombre = 'Secretaría Académica'       AND p.nombre IN ('academico.gestionar','docentes.gestionar','documentos.gestionar','consultas.gestionar')));

-- Usuario administrador (contraseña: password, cifrada con BCrypt igual que Laravel)
INSERT INTO users (name, email, rol_id, activo, email_verified_at, password, created_at, updated_at)
SELECT 'Administrador del Sistema', 'admin@carrera.test', id, TRUE, now(),
       '$2y$12$KBya21lMYPwxdqZgoTai/uLOK5lIs5Yd0CT9DxJvWgpsAfM5GDsz2', now(), now()
FROM roles WHERE nombre = 'Administrador';

INSERT INTO categorias_documento (nombre, slug, descripcion, created_at, updated_at) VALUES
('Reglamentos',   'reglamentos',   'Normativas que rigen la vida académica', now(), now()),
('Instructivos',  'instructivos',  'Guías para trámites y procesos', now(), now()),
('Formularios',   'formularios',   'Formularios descargables', now(), now()),
('Convocatorias', 'convocatorias', 'Convocatorias vigentes y pasadas', now(), now());

INSERT INTO lineas_investigacion (nombre, descripcion, created_at, updated_at) VALUES
('Gestión empresarial y estrategia', 'Estudios sobre dirección, planificación estratégica y competitividad de las organizaciones de la región.', now(), now()),
('Emprendimiento e innovación', 'Creación de empresas, ecosistemas emprendedores e innovación en PyMEs.', now(), now()),
('Finanzas y mercados', 'Finanzas corporativas, inclusión financiera y mercados de capitales.', now(), now()),
('Gestión pública y responsabilidad social', 'Administración de entidades públicas, transparencia y responsabilidad social empresarial.', now(), now());

INSERT INTO categorias_noticia (nombre, slug, created_at, updated_at) VALUES
('Institucional', 'institucional', now(), now()),
('Académico', 'academico', now(), now()),
('Investigación', 'investigacion', now(), now()),
('Convocatorias', 'convocatorias', now(), now()),
('Comunicados', 'comunicados', now(), now()),
('Reconocimientos', 'reconocimientos', now(), now());

INSERT INTO categorias_consulta (nombre, created_at, updated_at) VALUES
('Admisión e inscripciones', now(), now()),
('Trámites académicos', now(), now()),
('Titulación', now(), now()),
('Revista INNOVAR', now(), now()),
('Empresa Junior INNOVA', now(), now()),
('Otros', now(), now());

INSERT INTO materias (sigla, nombre, semestre, creditos, horas_teoricas, horas_practicas, tipo, created_at, updated_at) VALUES
('ADM-100', 'Introducción a la Administración', 1, 5, 4, 2, 'obligatoria', now(), now()),
('MAT-101', 'Matemática I',                     1, 5, 4, 2, 'obligatoria', now(), now()),
('CON-101', 'Contabilidad I',                   1, 5, 4, 2, 'obligatoria', now(), now()),
('ECO-101', 'Introducción a la Economía',       1, 4, 3, 2, 'obligatoria', now(), now()),
('MET-101', 'Metodología de la Investigación',  1, 4, 3, 2, 'obligatoria', now(), now()),
('LEN-101', 'Comunicación Oral y Escrita',      1, 3, 2, 2, 'obligatoria', now(), now()),
('ADM-200', 'Administración I',                 2, 5, 4, 2, 'obligatoria', now(), now()),
('MAT-201', 'Matemática II',                    2, 5, 4, 2, 'obligatoria', now(), now()),
('CON-201', 'Contabilidad II',                  2, 5, 4, 2, 'obligatoria', now(), now()),
('ECO-201', 'Microeconomía',                    2, 4, 3, 2, 'obligatoria', now(), now()),
('DER-201', 'Derecho Empresarial',              2, 4, 3, 2, 'obligatoria', now(), now()),
('INF-201', 'Informática Aplicada',             2, 3, 2, 2, 'obligatoria', now(), now());

INSERT INTO materia_prerequisito (materia_id, prerequisito_id)
SELECT m.id, p.id FROM materias m JOIN materias p ON (
       (m.sigla = 'ADM-200' AND p.sigla = 'ADM-100')
    OR (m.sigla = 'MAT-201' AND p.sigla = 'MAT-101')
    OR (m.sigla = 'CON-201' AND p.sigla = 'CON-101')
    OR (m.sigla = 'ECO-201' AND p.sigla = 'ECO-101'));

INSERT INTO configuraciones (clave, valor, grupo, created_at, updated_at) VALUES
('nombre_carrera', 'Administración de Empresas', 'general', now(), now()),
('facultad', 'Facultad de Ciencias Económicas y Empresariales - UAGRM', 'general', now(), now()),
('correo_contacto', 'grupo23sa@tecnoweb.org.bo', 'contacto', now(), now());
