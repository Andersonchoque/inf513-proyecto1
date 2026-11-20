-- =====================================================================
--  INF513 - Proyecto #1 - Grupo 23SA
--  Esquema PostgreSQL de la "Página Web de la Carrera de Administración
--  de Empresas" (mismo modelo que las migraciones del sistema web Laravel).
--  Se omiten solo las tablas técnicas de Laravel (sessions, cache, jobs,
--  passkeys, password_reset_tokens) que no usa el sistema por correo.
-- =====================================================================

-- ---------------------------------------------------------------------
-- CU1: Gestión de usuarios (roles y permisos)
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id            BIGSERIAL PRIMARY KEY,
    nombre        VARCHAR(60)  NOT NULL UNIQUE,
    descripcion   VARCHAR(255),
    acceso_panel  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP(0),
    updated_at    TIMESTAMP(0)
);

CREATE TABLE permisos (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(80)  NOT NULL UNIQUE,
    descripcion  VARCHAR(255),
    modulo       VARCHAR(60)  NOT NULL,
    created_at   TIMESTAMP(0),
    updated_at   TIMESTAMP(0)
);

CREATE TABLE rol_permiso (
    rol_id      BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permiso_id  BIGINT NOT NULL REFERENCES permisos (id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE users (
    id                         BIGSERIAL PRIMARY KEY,
    name                       VARCHAR(255) NOT NULL,
    email                      VARCHAR(255) NOT NULL UNIQUE,
    telefono                   VARCHAR(20),
    rol_id                     BIGINT REFERENCES roles (id) ON DELETE SET NULL,
    activo                     BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified_at          TIMESTAMP(0),
    password                   VARCHAR(255) NOT NULL,
    two_factor_secret          TEXT,
    two_factor_recovery_codes  TEXT,
    two_factor_confirmed_at    TIMESTAMP(0),
    remember_token             VARCHAR(100),
    created_at                 TIMESTAMP(0),
    updated_at                 TIMESTAMP(0)
);

-- ---------------------------------------------------------------------
-- CU2: Portal web y bitácora de auditoría
-- ---------------------------------------------------------------------
CREATE TABLE bitacora (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT REFERENCES users (id) ON DELETE SET NULL,
    accion       VARCHAR(30)  NOT NULL,
    modulo       VARCHAR(60)  NOT NULL,
    descripcion  VARCHAR(500) NOT NULL,
    registro_id  BIGINT,
    ip           VARCHAR(45),
    agente       VARCHAR(255),
    created_at   TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX bitacora_created_at_index ON bitacora (created_at);

CREATE TABLE menu_items (
    id          BIGSERIAL PRIMARY KEY,
    etiqueta    VARCHAR(80)  NOT NULL,
    ruta        VARCHAR(120),
    icono       VARCHAR(40),
    permiso     VARCHAR(80),
    padre_id    BIGINT REFERENCES menu_items (id) ON DELETE SET NULL,
    orden       INTEGER      NOT NULL DEFAULT 0,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP(0),
    updated_at  TIMESTAMP(0)
);

CREATE TABLE banners (
    id           BIGSERIAL PRIMARY KEY,
    titulo       VARCHAR(150) NOT NULL,
    subtitulo    VARCHAR(255),
    imagen       VARCHAR(255),
    enlace       VARCHAR(255),
    texto_boton  VARCHAR(40),
    orden        INTEGER      NOT NULL DEFAULT 0,
    activo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP(0),
    updated_at   TIMESTAMP(0)
);

CREATE TABLE destacados (
    id           BIGSERIAL PRIMARY KEY,
    titulo       VARCHAR(120) NOT NULL,
    descripcion  VARCHAR(255),
    valor        VARCHAR(40),
    enlace       VARCHAR(255),
    orden        INTEGER      NOT NULL DEFAULT 0,
    activo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP(0),
    updated_at   TIMESTAMP(0)
);

CREATE TABLE enlaces_plataforma (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    url          VARCHAR(255) NOT NULL,
    descripcion  VARCHAR(255),
    orden        INTEGER      NOT NULL DEFAULT 0,
    activo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP(0),
    updated_at   TIMESTAMP(0)
);

CREATE TABLE configuraciones (
    id          BIGSERIAL PRIMARY KEY,
    clave       VARCHAR(80) NOT NULL UNIQUE,
    valor       TEXT,
    grupo       VARCHAR(40) NOT NULL DEFAULT 'general',
    created_at  TIMESTAMP(0),
    updated_at  TIMESTAMP(0)
);

CREATE TABLE contador_visitas (
    id             BIGSERIAL PRIMARY KEY,
    ruta           VARCHAR(191) NOT NULL UNIQUE,
    total          BIGINT       NOT NULL DEFAULT 0,
    ultima_visita  TIMESTAMP(0),
    created_at     TIMESTAMP(0),
    updated_at     TIMESTAMP(0)
);

CREATE TABLE visitas_diarias (
    id     BIGSERIAL PRIMARY KEY,
    fecha  DATE         NOT NULL,
    ruta   VARCHAR(191) NOT NULL,
    total  INTEGER      NOT NULL DEFAULT 0,
    UNIQUE (fecha, ruta)
);

-- ---------------------------------------------------------------------
-- CU3: Información académica, malla curricular y plantel docente
-- ---------------------------------------------------------------------
CREATE TABLE paginas (
    id          BIGSERIAL PRIMARY KEY,
    seccion     VARCHAR(30)  NOT NULL,
    slug        VARCHAR(120) NOT NULL UNIQUE,
    titulo      VARCHAR(150) NOT NULL,
    resumen     VARCHAR(255),
    contenido   TEXT,
    imagen      VARCHAR(255),
    orden       INTEGER      NOT NULL DEFAULT 0,
    publicado   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP(0),
    updated_at  TIMESTAMP(0)
);
CREATE INDEX paginas_seccion_index ON paginas (seccion);

CREATE TABLE docentes (
    id                 BIGSERIAL PRIMARY KEY,
    nombre             VARCHAR(150) NOT NULL,
    tipo               VARCHAR(20)  NOT NULL DEFAULT 'docente',
    cargo              VARCHAR(120),
    formacion          VARCHAR(255),
    especialidad       VARCHAR(150),
    materias           VARCHAR(255),
    correo             VARCHAR(150),
    foto               VARCHAR(255),
    mostrar_correo     BOOLEAN      NOT NULL DEFAULT FALSE,
    mostrar_formacion  BOOLEAN      NOT NULL DEFAULT TRUE,
    visible            BOOLEAN      NOT NULL DEFAULT TRUE,
    orden              INTEGER      NOT NULL DEFAULT 0,
    created_at         TIMESTAMP(0),
    updated_at         TIMESTAMP(0)
);

CREATE TABLE materias (
    id               BIGSERIAL PRIMARY KEY,
    sigla            VARCHAR(20)  NOT NULL UNIQUE,
    nombre           VARCHAR(150) NOT NULL,
    semestre         SMALLINT     NOT NULL,
    creditos         SMALLINT     NOT NULL DEFAULT 0,
    horas_teoricas   SMALLINT     NOT NULL DEFAULT 0,
    horas_practicas  SMALLINT     NOT NULL DEFAULT 0,
    tipo             VARCHAR(20)  NOT NULL DEFAULT 'obligatoria',
    descripcion      TEXT,
    created_at       TIMESTAMP(0),
    updated_at       TIMESTAMP(0)
);

CREATE TABLE materia_prerequisito (
    materia_id       BIGINT NOT NULL REFERENCES materias (id) ON DELETE CASCADE,
    prerequisito_id  BIGINT NOT NULL REFERENCES materias (id) ON DELETE CASCADE,
    PRIMARY KEY (materia_id, prerequisito_id)
);

CREATE TABLE actividades_calendario (
    id            BIGSERIAL PRIMARY KEY,
    titulo        VARCHAR(150) NOT NULL,
    descripcion   VARCHAR(255),
    fecha_inicio  DATE         NOT NULL,
    fecha_fin     DATE,
    tipo          VARCHAR(30)  NOT NULL DEFAULT 'academico',
    gestion       VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP(0),
    updated_at    TIMESTAMP(0)
);

CREATE TABLE preguntas_frecuentes (
    id          BIGSERIAL PRIMARY KEY,
    pregunta    VARCHAR(255) NOT NULL,
    respuesta   TEXT         NOT NULL,
    categoria   VARCHAR(60),
    orden       INTEGER      NOT NULL DEFAULT 0,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP(0),
    updated_at  TIMESTAMP(0)
);

-- ---------------------------------------------------------------------
-- CU4: Investigación, Revista INNOVAR y repositorio documental
-- ---------------------------------------------------------------------
CREATE TABLE categorias_documento (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(80)  NOT NULL,
    slug         VARCHAR(100) NOT NULL UNIQUE,
    descripcion  VARCHAR(255),
    created_at   TIMESTAMP(0),
    updated_at   TIMESTAMP(0)
);

CREATE TABLE documentos (
    id                      BIGSERIAL PRIMARY KEY,
    categoria_documento_id  BIGINT       NOT NULL REFERENCES categorias_documento (id) ON DELETE RESTRICT,
    titulo                  VARCHAR(200) NOT NULL,
    descripcion             VARCHAR(500),
    archivo                 VARCHAR(255),
    fecha_publicacion       DATE,
    descargas               INTEGER      NOT NULL DEFAULT 0,
    publicado               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP(0),
    updated_at              TIMESTAMP(0)
);

CREATE TABLE lineas_investigacion (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(150) NOT NULL,
    descripcion  TEXT,
    activo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP(0),
    updated_at   TIMESTAMP(0)
);

CREATE TABLE proyectos_investigacion (
    id                       BIGSERIAL PRIMARY KEY,
    linea_investigacion_id   BIGINT REFERENCES lineas_investigacion (id) ON DELETE SET NULL,
    tipo                     VARCHAR(20)  NOT NULL DEFAULT 'proyecto',
    titulo                   VARCHAR(200) NOT NULL,
    slug                     VARCHAR(220) NOT NULL UNIQUE,
    descripcion              TEXT,
    responsables             VARCHAR(255),
    fecha_inicio             DATE,
    fecha_fin                DATE,
    estado                   VARCHAR(20)  NOT NULL DEFAULT 'en_curso',
    resultados               TEXT,
    enlace_publicacion       VARCHAR(255),
    enlace_repositorio       VARCHAR(255),
    imagen                   VARCHAR(255),
    publicado                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP(0),
    updated_at               TIMESTAMP(0)
);
CREATE INDEX proyectos_investigacion_tipo_index ON proyectos_investigacion (tipo);

CREATE TABLE revista_ediciones (
    id                 BIGSERIAL PRIMARY KEY,
    numero             INTEGER      NOT NULL UNIQUE,
    titulo             VARCHAR(200) NOT NULL,
    fecha_publicacion  DATE,
    descripcion        TEXT,
    portada            VARCHAR(255),
    archivo_pdf        VARCHAR(255),
    publicado          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP(0),
    updated_at         TIMESTAMP(0)
);

CREATE TABLE revista_articulos (
    id                  BIGSERIAL PRIMARY KEY,
    revista_edicion_id  BIGINT       NOT NULL REFERENCES revista_ediciones (id) ON DELETE CASCADE,
    titulo              VARCHAR(255) NOT NULL,
    autores             VARCHAR(255) NOT NULL,
    resumen             TEXT,
    palabras_clave      VARCHAR(255),
    archivo_pdf         VARCHAR(255),
    orden               INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP(0),
    updated_at          TIMESTAMP(0)
);

-- ---------------------------------------------------------------------
-- CU5: Vinculación, Empresa Junior INNOVA y seguimiento a graduados
-- ---------------------------------------------------------------------
CREATE TABLE convenios (
    id            BIGSERIAL PRIMARY KEY,
    institucion   VARCHAR(200) NOT NULL,
    tipo          VARCHAR(30)  NOT NULL DEFAULT 'convenio',
    finalidad     TEXT,
    fecha_inicio  DATE,
    fecha_fin     DATE,
    logo          VARCHAR(255),
    estado        VARCHAR(20)  NOT NULL DEFAULT 'vigente',
    publicado     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP(0),
    updated_at    TIMESTAMP(0)
);

CREATE TABLE innova_servicios (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(150) NOT NULL,
    descripcion  TEXT,
    orden        INTEGER      NOT NULL DEFAULT 0,
    activo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP(0),
    updated_at   TIMESTAMP(0)
);

CREATE TABLE innova_integrantes (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(150) NOT NULL,
    cargo       VARCHAR(100),
    semestre    VARCHAR(30),
    foto        VARCHAR(255),
    orden       INTEGER      NOT NULL DEFAULT 0,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP(0),
    updated_at  TIMESTAMP(0)
);

CREATE TABLE innova_proyectos (
    id           BIGSERIAL PRIMARY KEY,
    titulo       VARCHAR(200) NOT NULL,
    cliente      VARCHAR(150),
    descripcion  TEXT,
    fecha        DATE,
    imagen       VARCHAR(255),
    publicado    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP(0),
    updated_at   TIMESTAMP(0)
);

CREATE TABLE innova_solicitudes (
    id                  BIGSERIAL PRIMARY KEY,
    innova_servicio_id  BIGINT REFERENCES innova_servicios (id) ON DELETE SET NULL,
    empresa             VARCHAR(150) NOT NULL,
    contacto            VARCHAR(150) NOT NULL,
    correo              VARCHAR(150) NOT NULL,
    telefono            VARCHAR(30),
    mensaje             TEXT         NOT NULL,
    estado              VARCHAR(20)  NOT NULL DEFAULT 'pendiente',
    observacion         TEXT,
    created_at          TIMESTAMP(0),
    updated_at          TIMESTAMP(0)
);

CREATE TABLE innova_postulaciones (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(150) NOT NULL,
    registro     VARCHAR(20)  NOT NULL,
    correo       VARCHAR(150) NOT NULL,
    telefono     VARCHAR(30),
    semestre     VARCHAR(30),
    motivacion   TEXT         NOT NULL,
    estado       VARCHAR(20)  NOT NULL DEFAULT 'pendiente',
    observacion  TEXT,
    created_at   TIMESTAMP(0),
    updated_at   TIMESTAMP(0)
);

CREATE TABLE graduados (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT UNIQUE REFERENCES users (id) ON DELETE SET NULL,
    nombre                VARCHAR(150) NOT NULL,
    ci                    VARCHAR(20)  NOT NULL,
    registro              VARCHAR(20),
    anio_graduacion       SMALLINT     NOT NULL,
    modalidad_titulacion  VARCHAR(60),
    situacion_laboral     VARCHAR(30)  NOT NULL,
    empresa               VARCHAR(150),
    cargo                 VARCHAR(120),
    ciudad                VARCHAR(80),
    correo                VARCHAR(150) NOT NULL,
    telefono              VARCHAR(30),
    linkedin              VARCHAR(255),
    created_at            TIMESTAMP(0),
    updated_at            TIMESTAMP(0)
);

-- ---------------------------------------------------------------------
-- CU6: Noticias, eventos y galería multimedia
-- ---------------------------------------------------------------------
CREATE TABLE categorias_noticia (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(80)  NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMP(0),
    updated_at  TIMESTAMP(0)
);

CREATE TABLE noticias (
    id                    BIGSERIAL PRIMARY KEY,
    categoria_noticia_id  BIGINT REFERENCES categorias_noticia (id) ON DELETE SET NULL,
    user_id               BIGINT REFERENCES users (id) ON DELETE SET NULL,
    titulo                VARCHAR(200) NOT NULL,
    slug                  VARCHAR(220) NOT NULL UNIQUE,
    resumen               VARCHAR(300),
    contenido             TEXT,
    imagen                VARCHAR(255),
    fecha_publicacion     DATE         NOT NULL,
    estado                VARCHAR(20)  NOT NULL DEFAULT 'publicado',
    destacado             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP(0),
    updated_at            TIMESTAMP(0)
);

CREATE TABLE eventos (
    id                  BIGSERIAL PRIMARY KEY,
    titulo              VARCHAR(200) NOT NULL,
    slug                VARCHAR(220) NOT NULL UNIQUE,
    tipo                VARCHAR(30)  NOT NULL DEFAULT 'conferencia',
    descripcion         TEXT,
    fecha               DATE         NOT NULL,
    hora_inicio         TIME(0),
    hora_fin            TIME(0),
    lugar               VARCHAR(200),
    afiche              VARCHAR(255),
    enlace_inscripcion  VARCHAR(255),
    publicado           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP(0),
    updated_at          TIMESTAMP(0)
);

CREATE TABLE albumes (
    id           BIGSERIAL PRIMARY KEY,
    titulo       VARCHAR(200) NOT NULL,
    slug         VARCHAR(220) NOT NULL UNIQUE,
    descripcion  VARCHAR(500),
    fecha        DATE,
    portada      VARCHAR(255),
    publicado    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP(0),
    updated_at   TIMESTAMP(0)
);

CREATE TABLE fotos (
    id           BIGSERIAL PRIMARY KEY,
    album_id     BIGINT       NOT NULL REFERENCES albumes (id) ON DELETE CASCADE,
    imagen       VARCHAR(255) NOT NULL,
    descripcion  VARCHAR(255),
    orden        INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMP(0),
    updated_at   TIMESTAMP(0)
);

CREATE TABLE videos (
    id           BIGSERIAL PRIMARY KEY,
    album_id     BIGINT REFERENCES albumes (id) ON DELETE SET NULL,
    titulo       VARCHAR(200) NOT NULL,
    url          VARCHAR(255) NOT NULL,
    descripcion  VARCHAR(500),
    publicado    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP(0),
    updated_at   TIMESTAMP(0)
);

-- ---------------------------------------------------------------------
-- CU7: Consultas mediante formulario de contacto
-- ---------------------------------------------------------------------
CREATE TABLE categorias_consulta (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(80) NOT NULL,
    correo_destino  VARCHAR(150),
    created_at      TIMESTAMP(0),
    updated_at      TIMESTAMP(0)
);

CREATE TABLE consultas (
    id                     BIGSERIAL PRIMARY KEY,
    categoria_consulta_id  BIGINT REFERENCES categorias_consulta (id) ON DELETE SET NULL,
    nombre                 VARCHAR(150) NOT NULL,
    correo                 VARCHAR(150) NOT NULL,
    telefono               VARCHAR(30),
    mensaje                TEXT         NOT NULL,
    estado                 VARCHAR(20)  NOT NULL DEFAULT 'nueva',
    respuesta              TEXT,
    ip                     VARCHAR(45),
    created_at             TIMESTAMP(0),
    updated_at             TIMESTAMP(0)
);
