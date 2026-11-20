# Referencia de comandos

> Generado automáticamente con `./ejecutar.sh doc` a partir del catálogo (`Catalogo.java`).

Formato del asunto: `COMANDO["param1","param2",...]`. Acciones: **LIS** listar · **INS** insertar · **MOD** modificar · **ELI** eliminar. Listar acepta `["*"]`, `["id"]` o `["campo","valor"]`.

## CU1 · Gestión de usuarios (roles y permisos)

### ROL · Roles (tabla `roles`)

Comandos: `LISROL`, `INSROL`, `MODROL`, `ELIROL`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `nombre` | texto (máx. 60) | Sí |  |
| 2 | `descripcion` | texto (máx. 255) | No |  |
| 3 | `acceso_panel` | true/false (o si/no) | No | true |

```
LISROL["*"]
LISROL["1"]
LISROL["nombre","texto"]
INSROL["Editor","Edita contenido del sitio","true"]
MODROL["1","Editor","Edita contenido del sitio","true"]
ELIROL["1"]
```

### PRM · Permisos (tabla `permisos`)

Comandos: `LISPRM`, `INSPRM`, `MODPRM`, `ELIPRM`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `nombre` | texto (máx. 80) | Sí |  |
| 2 | `descripcion` | texto (máx. 255) | No |  |
| 3 | `modulo` | texto (máx. 60) | Sí |  |

```
LISPRM["*"]
LISPRM["1"]
LISPRM["nombre","texto"]
INSPRM["noticias.publicar","Publicar noticias","Difusión"]
MODPRM["1","noticias.publicar","Publicar noticias","Difusión"]
ELIPRM["1"]
```

### RPE · Permisos asignados a roles (tabla `rol_permiso`)

Comandos: `LISRPE`, `INSRPE`, `ELIRPE`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `rol_id` | número entero → id de roles | Sí |  |
| 2 | `permiso_id` | número entero → id de permisos | Sí |  |

```
LISRPE["*"]
INSRPE["2","1"]
ELIRPE["1","2"]
```

### USU · Usuarios (tabla `users`)

Comandos: `LISUSU`, `INSUSU`, `MODUSU`, `ELIUSU`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `name` | texto (máx. 255) | Sí |  |
| 2 | `email` | correo electrónico (máx. 255) | Sí |  |
| 3 | `telefono` | texto (máx. 20) | No |  |
| 4 | `rol_id` | número entero → id de roles | No |  |
| 5 | `activo` | true/false (o si/no) | No | true |
| 6 | `password` | contraseña (mín. 6 caracteres) | Sí |  |

```
LISUSU["*"]
LISUSU["1"]
LISUSU["name","texto"]
INSUSU["Juan Carlos Pérez","juanperez@uagrm.edu.bo","71055123","1","true","secreto123"]
MODUSU["1","Juan Carlos Pérez","juanperez@uagrm.edu.bo","71055123","1","true",""]
ELIUSU["1"]
```

## CU2 · Portal web y bitácora de auditoría

### BIT · Bitácora de auditoría (tabla `bitacora`)

Comandos: `LISBIT`

### MEN · Menú del panel (tabla `menu_items`)

Comandos: `LISMEN`, `INSMEN`, `MODMEN`, `ELIMEN`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `etiqueta` | texto (máx. 80) | Sí |  |
| 2 | `ruta` | texto (máx. 120) | No |  |
| 3 | `icono` | texto (máx. 40) | No |  |
| 4 | `permiso` | texto (máx. 80) | No |  |
| 5 | `padre_id` | número entero → id de menu_items | No |  |
| 6 | `orden` | número entero (0 a 10000) | No | 0 |
| 7 | `activo` | true/false (o si/no) | No | true |

```
LISMEN["*"]
LISMEN["1"]
LISMEN["etiqueta","texto"]
INSMEN["Reportes","admin.reportes.index","bar-chart","reportes.ver","","1","true"]
MODMEN["1","Reportes","admin.reportes.index","bar-chart","reportes.ver","","1","true"]
ELIMEN["1"]
```

### BAN · Banners del carrusel (tabla `banners`)

Comandos: `LISBAN`, `INSBAN`, `MODBAN`, `ELIBAN`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `titulo` | texto (máx. 150) | Sí |  |
| 2 | `subtitulo` | texto (máx. 255) | No |  |
| 3 | `imagen` | texto (máx. 255) | No |  |
| 4 | `enlace` | enlace http(s):// (máx. 255) | No |  |
| 5 | `texto_boton` | texto (máx. 40) | No |  |
| 6 | `orden` | número entero (0 a 10000) | No | 0 |
| 7 | `activo` | true/false (o si/no) | No | true |

```
LISBAN["*"]
LISBAN["1"]
LISBAN["titulo","texto"]
INSBAN["Inscripciones abiertas 2027","Carrera de Administración de Empresas","banners/inscripciones.jpg","https://ejemplo.com","Más información","1","true"]
MODBAN["1","Inscripciones abiertas 2027","Carrera de Administración de Empresas","banners/inscripciones.jpg","https://ejemplo.com","Más información","1","true"]
ELIBAN["1"]
```

### DES · Indicadores destacados (tabla `destacados`)

Comandos: `LISDES`, `INSDES`, `MODDES`, `ELIDES`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `titulo` | texto (máx. 120) | Sí |  |
| 2 | `descripcion` | texto (máx. 255) | No |  |
| 3 | `valor` | texto (máx. 40) | No |  |
| 4 | `enlace` | enlace http(s):// (máx. 255) | No |  |
| 5 | `orden` | número entero (0 a 10000) | No | 0 |
| 6 | `activo` | true/false (o si/no) | No | true |

```
LISDES["*"]
LISDES["1"]
LISDES["titulo","texto"]
INSDES["Estudiantes","Matriculados en la gestión","3500+","","1","true"]
MODDES["1","Estudiantes","Matriculados en la gestión","3500+","","1","true"]
ELIDES["1"]
```

### ENL · Enlaces a plataformas (tabla `enlaces_plataforma`)

Comandos: `LISENL`, `INSENL`, `MODENL`, `ELIENL`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `nombre` | texto (máx. 100) | Sí |  |
| 2 | `url` | enlace http(s):// (máx. 255) | Sí |  |
| 3 | `descripcion` | texto (máx. 255) | No |  |
| 4 | `orden` | número entero (0 a 10000) | No | 0 |
| 5 | `activo` | true/false (o si/no) | No | true |

```
LISENL["*"]
LISENL["1"]
LISENL["nombre","texto"]
INSENL["Moodle FCEE","https://moodle.uagrm.edu.bo","Aula virtual","1","true"]
MODENL["1","Moodle FCEE","https://moodle.uagrm.edu.bo","Aula virtual","1","true"]
ELIENL["1"]
```

### CNF · Configuraciones del portal (tabla `configuraciones`)

Comandos: `LISCNF`, `INSCNF`, `MODCNF`, `ELICNF`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `clave` | texto (máx. 80) | Sí |  |
| 2 | `valor` | texto largo | No |  |
| 3 | `grupo` | texto (máx. 40) | No | general |

```
LISCNF["*"]
LISCNF["1"]
LISCNF["clave","texto"]
INSCNF["telefono_contacto","3-3355000","contacto"]
MODCNF["1","telefono_contacto","3-3355000","contacto"]
ELICNF["1"]
```

### VIS · Contador de visitas por página (tabla `contador_visitas`)

Comandos: `LISVIS`

## CU3 · Información académica, malla curricular y plantel docente

### PAG · Páginas de contenido (tabla `paginas`)

Comandos: `LISPAG`, `INSPAG`, `MODPAG`, `ELIPAG`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `seccion` | una opción de la lista: institucional \| academico \| revista \| innova \| graduados | Sí |  |
| 2 | `titulo` | texto (máx. 150) | Sí |  |
| 3 | `slug` | slug (vacío = se genera del título) (máx. 120) | No |  |
| 4 | `resumen` | texto (máx. 255) | No |  |
| 5 | `contenido` | texto largo | No |  |
| 6 | `imagen` | texto (máx. 255) | No |  |
| 7 | `orden` | número entero (0 a 10000) | No | 0 |
| 8 | `publicado` | true/false (o si/no) | No | true |

```
LISPAG["*"]
LISPAG["1"]
LISPAG["titulo","texto"]
INSPAG["institucional","Misión y visión","","Propósito de la Carrera","Formar profesionales líderes...","","1","true"]
MODPAG["1","institucional","Misión y visión","","Propósito de la Carrera","Formar profesionales líderes...","","1","true"]
ELIPAG["1"]
```

### DOC · Autoridades y docentes (tabla `docentes`)

Comandos: `LISDOC`, `INSDOC`, `MODDOC`, `ELIDOC`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `nombre` | texto (máx. 150) | Sí |  |
| 2 | `tipo` | una opción de la lista: autoridad \| docente \| administrativo | No | docente |
| 3 | `cargo` | texto (máx. 120) | No |  |
| 4 | `formacion` | texto (máx. 255) | No |  |
| 5 | `especialidad` | texto (máx. 150) | No |  |
| 6 | `materias` | texto (máx. 255) | No |  |
| 7 | `correo` | correo electrónico (máx. 150) | No |  |
| 8 | `foto` | texto (máx. 255) | No |  |
| 9 | `mostrar_correo` | true/false (o si/no) | No | false |
| 10 | `mostrar_formacion` | true/false (o si/no) | No | true |
| 11 | `visible` | true/false (o si/no) | No | true |
| 12 | `orden` | número entero (0 a 10000) | No | 0 |

```
LISDOC["*"]
LISDOC["1"]
LISDOC["nombre","texto"]
INSDOC["Lic. María Fernández","docente","Docente titular","MBA en Finanzas","Finanzas corporativas","Finanzas I, Finanzas II","mfernandez@uagrm.edu.bo","","false","true","true","1"]
MODDOC["1","Lic. María Fernández","docente","Docente titular","MBA en Finanzas","Finanzas corporativas","Finanzas I, Finanzas II","mfernandez@uagrm.edu.bo","","false","true","true","1"]
ELIDOC["1"]
```

### MAT · Materias de la malla curricular (tabla `materias`)

Comandos: `LISMAT`, `INSMAT`, `MODMAT`, `ELIMAT`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `sigla` | texto (máx. 20) | Sí |  |
| 2 | `nombre` | texto (máx. 150) | Sí |  |
| 3 | `semestre` | número entero (1 a 12) | Sí |  |
| 4 | `creditos` | número entero (0 a 30) | No | 0 |
| 5 | `horas_teoricas` | número entero (0 a 40) | No | 0 |
| 6 | `horas_practicas` | número entero (0 a 40) | No | 0 |
| 7 | `tipo` | una opción de la lista: obligatoria \| electiva \| modalidad | No | obligatoria |
| 8 | `descripcion` | texto largo | No |  |

```
LISMAT["*"]
LISMAT["1"]
LISMAT["sigla","texto"]
INSMAT["ADM-300","Administración II","3","5","4","2","obligatoria",""]
MODMAT["1","ADM-300","Administración II","3","5","4","2","obligatoria",""]
ELIMAT["1"]
```

### PRE · Prerrequisitos de materias (tabla `materia_prerequisito`)

Comandos: `LISPRE`, `INSPRE`, `ELIPRE`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `materia_id` | número entero → id de materias | Sí |  |
| 2 | `prerequisito_id` | número entero → id de materias | Sí |  |

```
LISPRE["*"]
INSPRE["11","1"]
ELIPRE["1","2"]
```

### CAL · Calendario académico (tabla `actividades_calendario`)

Comandos: `LISCAL`, `INSCAL`, `MODCAL`, `ELICAL`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `titulo` | texto (máx. 150) | Sí |  |
| 2 | `descripcion` | texto (máx. 255) | No |  |
| 3 | `fecha_inicio` | fecha AAAA-MM-DD | Sí |  |
| 4 | `fecha_fin` | fecha AAAA-MM-DD | No |  |
| 5 | `tipo` | una opción de la lista: inscripciones \| clases \| examenes \| feriado \| academico \| titulacion | No | academico |
| 6 | `gestion` | texto (máx. 20) | Sí |  |

```
LISCAL["*"]
LISCAL["1"]
LISCAL["titulo","texto"]
INSCAL["Inscripción de materias","Inscripción por sistema","2027-02-01","2027-02-15","academico","1-2027"]
MODCAL["1","Inscripción de materias","Inscripción por sistema","2027-02-01","2027-02-15","academico","1-2027"]
ELICAL["1"]
```

### FAQ · Preguntas frecuentes (tabla `preguntas_frecuentes`)

Comandos: `LISFAQ`, `INSFAQ`, `MODFAQ`, `ELIFAQ`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `pregunta` | texto (máx. 255) | Sí |  |
| 2 | `respuesta` | texto largo | Sí |  |
| 3 | `categoria` | texto (máx. 60) | No |  |
| 4 | `orden` | número entero (0 a 10000) | No | 0 |
| 5 | `activo` | true/false (o si/no) | No | true |

```
LISFAQ["*"]
LISFAQ["1"]
LISFAQ["pregunta","texto"]
INSFAQ["¿Cuándo son las inscripciones?","Según el calendario académico publicado.","Inscripciones","1","true"]
MODFAQ["1","¿Cuándo son las inscripciones?","Según el calendario académico publicado.","Inscripciones","1","true"]
ELIFAQ["1"]
```

## CU4 · Investigación, Revista INNOVAR y repositorio documental

### CDO · Categorías de documentos (tabla `categorias_documento`)

Comandos: `LISCDO`, `INSCDO`, `MODCDO`, `ELICDO`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `nombre` | texto (máx. 80) | Sí |  |
| 2 | `slug` | slug (vacío = se genera del título) (máx. 100) | No |  |
| 3 | `descripcion` | texto (máx. 255) | No |  |

```
LISCDO["*"]
LISCDO["1"]
LISCDO["nombre","texto"]
INSCDO["Resoluciones","","Resoluciones de la Dirección de Carrera"]
MODCDO["1","Resoluciones","","Resoluciones de la Dirección de Carrera"]
ELICDO["1"]
```

### BIB · Biblioteca digital (documentos) (tabla `documentos`)

Comandos: `LISBIB`, `INSBIB`, `MODBIB`, `ELIBIB`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `categoria_documento_id` | número entero → id de categorias_documento | Sí |  |
| 2 | `titulo` | texto (máx. 200) | Sí |  |
| 3 | `descripcion` | texto (máx. 500) | No |  |
| 4 | `archivo` | texto (máx. 255) | No |  |
| 5 | `fecha_publicacion` | fecha AAAA-MM-DD | No |  |
| 6 | `descargas` | número entero (0 a 2147483647) | No | 0 |
| 7 | `publicado` | true/false (o si/no) | No | true |

```
LISBIB["*"]
LISBIB["1"]
LISBIB["titulo","texto"]
INSBIB["1","Reglamento de Graduación","Versión aprobada 2026","documentos/reglamento-graduacion.pdf","2026-03-15","0","true"]
MODBIB["1","1","Reglamento de Graduación","Versión aprobada 2026","documentos/reglamento-graduacion.pdf","2026-03-15","0","true"]
ELIBIB["1"]
```

### LIN · Líneas de investigación (tabla `lineas_investigacion`)

Comandos: `LISLIN`, `INSLIN`, `MODLIN`, `ELILIN`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `nombre` | texto (máx. 150) | Sí |  |
| 2 | `descripcion` | texto largo | No |  |
| 3 | `activo` | true/false (o si/no) | No | true |

```
LISLIN["*"]
LISLIN["1"]
LISLIN["nombre","texto"]
INSLIN["Marketing digital","Estrategias de mercadeo en entornos digitales","true"]
MODLIN["1","Marketing digital","Estrategias de mercadeo en entornos digitales","true"]
ELILIN["1"]
```

### PRY · Proyectos de investigación (tabla `proyectos_investigacion`)

Comandos: `LISPRY`, `INSPRY`, `MODPRY`, `ELIPRY`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `linea_investigacion_id` | número entero → id de lineas_investigacion | No |  |
| 2 | `tipo` | una opción de la lista: proyecto \| pia \| semillero \| produccion | No | proyecto |
| 3 | `titulo` | texto (máx. 200) | Sí |  |
| 4 | `slug` | slug (vacío = se genera del título) (máx. 220) | No |  |
| 5 | `descripcion` | texto largo | No |  |
| 6 | `responsables` | texto (máx. 255) | No |  |
| 7 | `fecha_inicio` | fecha AAAA-MM-DD | No |  |
| 8 | `fecha_fin` | fecha AAAA-MM-DD | No |  |
| 9 | `estado` | una opción de la lista: planificado \| en_curso \| finalizado | No | en_curso |
| 10 | `resultados` | texto largo | No |  |
| 11 | `enlace_publicacion` | enlace http(s):// (máx. 255) | No |  |
| 12 | `enlace_repositorio` | enlace http(s):// (máx. 255) | No |  |
| 13 | `imagen` | texto (máx. 255) | No |  |
| 14 | `publicado` | true/false (o si/no) | No | true |

```
LISPRY["*"]
LISPRY["1"]
LISPRY["titulo","texto"]
INSPRY["1","proyecto","Competitividad de las PyMEs cruceñas","","Estudio de factores de competitividad","Lic. Ana Rojas; Est. Luis Vaca","2026-03-01","2026-12-15","en_curso","","https://ejemplo.com","","","true"]
MODPRY["1","1","proyecto","Competitividad de las PyMEs cruceñas","","Estudio de factores de competitividad","Lic. Ana Rojas; Est. Luis Vaca","2026-03-01","2026-12-15","en_curso","","https://ejemplo.com","","","true"]
ELIPRY["1"]
```

### EDI · Ediciones de la Revista INNOVAR (tabla `revista_ediciones`)

Comandos: `LISEDI`, `INSEDI`, `MODEDI`, `ELIEDI`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `numero` | número entero (1 a 100000) | Sí |  |
| 2 | `titulo` | texto (máx. 200) | Sí |  |
| 3 | `fecha_publicacion` | fecha AAAA-MM-DD | No |  |
| 4 | `descripcion` | texto largo | No |  |
| 5 | `portada` | texto (máx. 255) | No |  |
| 6 | `archivo_pdf` | texto (máx. 255) | No |  |
| 7 | `publicado` | true/false (o si/no) | No | true |

```
LISEDI["*"]
LISEDI["1"]
LISEDI["titulo","texto"]
INSEDI["5","INNOVAR N° 5","2026-12-01","Edición dedicada al emprendimiento","","","true"]
MODEDI["1","5","INNOVAR N° 5","2026-12-01","Edición dedicada al emprendimiento","","","true"]
ELIEDI["1"]
```

### ART · Artículos de la Revista INNOVAR (tabla `revista_articulos`)

Comandos: `LISART`, `INSART`, `MODART`, `ELIART`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `revista_edicion_id` | número entero → id de revista_ediciones | Sí |  |
| 2 | `titulo` | texto (máx. 255) | Sí |  |
| 3 | `autores` | texto (máx. 255) | Sí |  |
| 4 | `resumen` | texto largo | No |  |
| 5 | `palabras_clave` | texto (máx. 255) | No |  |
| 6 | `archivo_pdf` | texto (máx. 255) | No |  |
| 7 | `orden` | número entero (0 a 10000) | No | 0 |

```
LISART["*"]
LISART["1"]
LISART["titulo","texto"]
INSART["1","El emprendimiento femenino en Santa Cruz","Ana Rojas, Luis Vaca","Se analiza...","emprendimiento, género, PyME","","1"]
MODART["1","1","El emprendimiento femenino en Santa Cruz","Ana Rojas, Luis Vaca","Se analiza...","emprendimiento, género, PyME","","1"]
ELIART["1"]
```

## CU5 · Vinculación, Empresa Junior INNOVA y graduados

### CNV · Convenios institucionales (tabla `convenios`)

Comandos: `LISCNV`, `INSCNV`, `MODCNV`, `ELICNV`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `institucion` | texto (máx. 200) | Sí |  |
| 2 | `tipo` | una opción de la lista: convenio \| practica \| proyecto_conjunto \| consultoria \| interaccion_social | No | convenio |
| 3 | `finalidad` | texto largo | No |  |
| 4 | `fecha_inicio` | fecha AAAA-MM-DD | No |  |
| 5 | `fecha_fin` | fecha AAAA-MM-DD | No |  |
| 6 | `logo` | texto (máx. 255) | No |  |
| 7 | `estado` | una opción de la lista: vigente \| en_tramite \| finalizado | No | vigente |
| 8 | `publicado` | true/false (o si/no) | No | true |

```
LISCNV["*"]
LISCNV["1"]
LISCNV["institucion","texto"]
INSCNV["Cámara de Industria y Comercio (CAINCO)","convenio","Prácticas empresariales para estudiantes","2026-01-10","2028-01-10","","vigente","true"]
MODCNV["1","Cámara de Industria y Comercio (CAINCO)","convenio","Prácticas empresariales para estudiantes","2026-01-10","2028-01-10","","vigente","true"]
ELICNV["1"]
```

### SER · Servicios de INNOVA (tabla `innova_servicios`)

Comandos: `LISSER`, `INSSER`, `MODSER`, `ELISER`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `nombre` | texto (máx. 150) | Sí |  |
| 2 | `descripcion` | texto largo | No |  |
| 3 | `orden` | número entero (0 a 10000) | No | 0 |
| 4 | `activo` | true/false (o si/no) | No | true |

```
LISSER["*"]
LISSER["1"]
LISSER["nombre","texto"]
INSSER["Plan de negocios","Elaboración de planes de negocio para PyMEs","1","true"]
MODSER["1","Plan de negocios","Elaboración de planes de negocio para PyMEs","1","true"]
ELISER["1"]
```

### INT · Integrantes de INNOVA (tabla `innova_integrantes`)

Comandos: `LISINT`, `INSINT`, `MODINT`, `ELIINT`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `nombre` | texto (máx. 150) | Sí |  |
| 2 | `cargo` | texto (máx. 100) | No |  |
| 3 | `semestre` | texto (máx. 30) | No |  |
| 4 | `foto` | texto (máx. 255) | No |  |
| 5 | `orden` | número entero (0 a 10000) | No | 0 |
| 6 | `activo` | true/false (o si/no) | No | true |

```
LISINT["*"]
LISINT["1"]
LISINT["nombre","texto"]
INSINT["Carla Justiniano","Gerente general","8vo semestre","","1","true"]
MODINT["1","Carla Justiniano","Gerente general","8vo semestre","","1","true"]
ELIINT["1"]
```

### IPR · Proyectos de INNOVA (tabla `innova_proyectos`)

Comandos: `LISIPR`, `INSIPR`, `MODIPR`, `ELIIPR`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `titulo` | texto (máx. 200) | Sí |  |
| 2 | `cliente` | texto (máx. 150) | No |  |
| 3 | `descripcion` | texto largo | No |  |
| 4 | `fecha` | fecha AAAA-MM-DD | No |  |
| 5 | `imagen` | texto (máx. 255) | No |  |
| 6 | `publicado` | true/false (o si/no) | No | true |

```
LISIPR["*"]
LISIPR["1"]
LISIPR["titulo","texto"]
INSIPR["Estudio de mercado para cafetería","Café Patujú","","2026-09-20","","true"]
MODIPR["1","Estudio de mercado para cafetería","Café Patujú","","2026-09-20","","true"]
ELIIPR["1"]
```

### SOL · Solicitudes de servicio a INNOVA (tabla `innova_solicitudes`)

Comandos: `LISSOL`, `INSSOL`, `MODSOL`, `ELISOL`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `innova_servicio_id` | número entero → id de innova_servicios | No |  |
| 2 | `empresa` | texto (máx. 150) | Sí |  |
| 3 | `contacto` | texto (máx. 150) | Sí |  |
| 4 | `correo` | correo electrónico (máx. 150) | Sí |  |
| 5 | `telefono` | texto (máx. 30) | No |  |
| 6 | `mensaje` | texto largo | Sí |  |
| 7 | `estado` | una opción de la lista: pendiente \| en_proceso \| atendida \| rechazada | No | pendiente |
| 8 | `observacion` | texto largo | No |  |

```
LISSOL["*"]
LISSOL["1"]
LISSOL["empresa","texto"]
INSSOL["1","Distribuidora El Sol","Pedro Suárez","pedro@elsol.com","70011223","Necesitamos un plan de marketing","pendiente",""]
MODSOL["1","1","Distribuidora El Sol","Pedro Suárez","pedro@elsol.com","70011223","Necesitamos un plan de marketing","pendiente",""]
ELISOL["1"]
```

### POS · Postulaciones a INNOVA (tabla `innova_postulaciones`)

Comandos: `LISPOS`, `INSPOS`, `MODPOS`, `ELIPOS`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `nombre` | texto (máx. 150) | Sí |  |
| 2 | `registro` | texto (máx. 20) | Sí |  |
| 3 | `correo` | correo electrónico (máx. 150) | Sí |  |
| 4 | `telefono` | texto (máx. 30) | No |  |
| 5 | `semestre` | texto (máx. 30) | No |  |
| 6 | `motivacion` | texto largo | Sí |  |
| 7 | `estado` | una opción de la lista: pendiente \| entrevista \| aceptada \| rechazada | No | pendiente |
| 8 | `observacion` | texto largo | No |  |

```
LISPOS["*"]
LISPOS["1"]
LISPOS["nombre","texto"]
INSPOS["Luis Vaca","219012345","luisvaca@uagrm.edu.bo","76543210","6to","Quiero aplicar lo aprendido en consultoría","pendiente",""]
MODPOS["1","Luis Vaca","219012345","luisvaca@uagrm.edu.bo","76543210","6to","Quiero aplicar lo aprendido en consultoría","pendiente",""]
ELIPOS["1"]
```

### GRA · Graduados (Alumni) (tabla `graduados`)

Comandos: `LISGRA`, `INSGRA`, `MODGRA`, `ELIGRA`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `user_id` | número entero → id de users | No |  |
| 2 | `nombre` | texto (máx. 150) | Sí |  |
| 3 | `ci` | texto (máx. 20) | Sí |  |
| 4 | `registro` | texto (máx. 20) | No |  |
| 5 | `anio_graduacion` | número entero (1950 a 2027) | Sí |  |
| 6 | `modalidad_titulacion` | una opción de la lista: tesis \| proyecto_grado \| trabajo_dirigido \| examen_grado \| excelencia \| diplomado | No |  |
| 7 | `situacion_laboral` | una opción de la lista: dependiente \| independiente \| busca_empleo \| estudiando \| otro | Sí |  |
| 8 | `empresa` | texto (máx. 150) | No |  |
| 9 | `cargo` | texto (máx. 120) | No |  |
| 10 | `ciudad` | texto (máx. 80) | No |  |
| 11 | `correo` | correo electrónico (máx. 150) | Sí |  |
| 12 | `telefono` | texto (máx. 30) | No |  |
| 13 | `linkedin` | enlace http(s):// (máx. 255) | No |  |

```
LISGRA["*"]
LISGRA["1"]
LISGRA["nombre","texto"]
INSGRA["","Sofía Paz","4715292","209876543","2024","tesis","dependiente","Banco Ganadero","Analista de créditos","Santa Cruz","sofiapaz@gmail.com","77001122",""]
MODGRA["1","","Sofía Paz","4715292","209876543","2024","tesis","dependiente","Banco Ganadero","Analista de créditos","Santa Cruz","sofiapaz@gmail.com","77001122",""]
ELIGRA["1"]
```

## CU6 · Noticias, eventos y galería multimedia

### CNO · Categorías de noticias (tabla `categorias_noticia`)

Comandos: `LISCNO`, `INSCNO`, `MODCNO`, `ELICNO`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `nombre` | texto (máx. 80) | Sí |  |
| 2 | `slug` | slug (vacío = se genera del título) (máx. 100) | No |  |

```
LISCNO["*"]
LISCNO["1"]
LISCNO["nombre","texto"]
INSCNO["Deportes",""]
MODCNO["1","Deportes",""]
ELICNO["1"]
```

### NOT · Noticias y comunicados (tabla `noticias`)

Comandos: `LISNOT`, `INSNOT`, `MODNOT`, `ELINOT`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `categoria_noticia_id` | número entero → id de categorias_noticia | No |  |
| 2 | `user_id` | número entero → id de users | No |  |
| 3 | `titulo` | texto (máx. 200) | Sí |  |
| 4 | `slug` | slug (vacío = se genera del título) (máx. 220) | No |  |
| 5 | `resumen` | texto (máx. 300) | No |  |
| 6 | `contenido` | texto largo | No |  |
| 7 | `imagen` | texto (máx. 255) | No |  |
| 8 | `fecha_publicacion` | fecha AAAA-MM-DD | Sí |  |
| 9 | `estado` | una opción de la lista: borrador \| publicado \| archivado | No | publicado |
| 10 | `destacado` | true/false (o si/no) | No | false |

```
LISNOT["*"]
LISNOT["1"]
LISNOT["titulo","texto"]
INSNOT["1","","Feria de emprendimiento 2026","","Más de 40 emprendimientos estudiantiles","La Carrera realizó su feria anual...","","2026-11-20","publicado","false"]
MODNOT["1","1","","Feria de emprendimiento 2026","","Más de 40 emprendimientos estudiantiles","La Carrera realizó su feria anual...","","2026-11-20","publicado","false"]
ELINOT["1"]
```

### EVE · Eventos académicos (tabla `eventos`)

Comandos: `LISEVE`, `INSEVE`, `MODEVE`, `ELIEVE`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `titulo` | texto (máx. 200) | Sí |  |
| 2 | `slug` | slug (vacío = se genera del título) (máx. 220) | No |  |
| 3 | `tipo` | una opción de la lista: conferencia \| seminario \| taller \| feria \| congreso \| otro | No | conferencia |
| 4 | `descripcion` | texto largo | No |  |
| 5 | `fecha` | fecha AAAA-MM-DD | Sí |  |
| 6 | `hora_inicio` | hora HH:MM | No |  |
| 7 | `hora_fin` | hora HH:MM | No |  |
| 8 | `lugar` | texto (máx. 200) | No |  |
| 9 | `afiche` | texto (máx. 255) | No |  |
| 10 | `enlace_inscripcion` | enlace http(s):// (máx. 255) | No |  |
| 11 | `publicado` | true/false (o si/no) | No | true |

```
LISEVE["*"]
LISEVE["1"]
LISEVE["titulo","texto"]
INSEVE["Congreso de Administración 2026","","conferencia","Tres días de conferencias","2026-12-05","09:00","12:30","Auditorio FCEE","","","true"]
MODEVE["1","Congreso de Administración 2026","","conferencia","Tres días de conferencias","2026-12-05","09:00","12:30","Auditorio FCEE","","","true"]
ELIEVE["1"]
```

### ALB · Álbumes de la galería (tabla `albumes`)

Comandos: `LISALB`, `INSALB`, `MODALB`, `ELIALB`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `titulo` | texto (máx. 200) | Sí |  |
| 2 | `slug` | slug (vacío = se genera del título) (máx. 220) | No |  |
| 3 | `descripcion` | texto (máx. 500) | No |  |
| 4 | `fecha` | fecha AAAA-MM-DD | No |  |
| 5 | `portada` | texto (máx. 255) | No |  |
| 6 | `publicado` | true/false (o si/no) | No | true |

```
LISALB["*"]
LISALB["1"]
LISALB["titulo","texto"]
INSALB["Graduación 2026","","Acto de graduación de la promoción 2026","2026-10-15","","true"]
MODALB["1","Graduación 2026","","Acto de graduación de la promoción 2026","2026-10-15","","true"]
ELIALB["1"]
```

### FOT · Fotos de la galería (tabla `fotos`)

Comandos: `LISFOT`, `INSFOT`, `MODFOT`, `ELIFOT`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `album_id` | número entero → id de albumes | Sí |  |
| 2 | `imagen` | texto (máx. 255) | Sí |  |
| 3 | `descripcion` | texto (máx. 255) | No |  |
| 4 | `orden` | número entero (0 a 10000) | No | 0 |

```
LISFOT["*"]
LISFOT["1"]
LISFOT["imagen","texto"]
INSFOT["1","galeria/graduacion-01.jpg","Entrega de títulos","1"]
MODFOT["1","1","galeria/graduacion-01.jpg","Entrega de títulos","1"]
ELIFOT["1"]
```

### VID · Videos de la galería (tabla `videos`)

Comandos: `LISVID`, `INSVID`, `MODVID`, `ELIVID`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `album_id` | número entero → id de albumes | No |  |
| 2 | `titulo` | texto (máx. 200) | Sí |  |
| 3 | `url` | enlace http(s):// (máx. 255) | Sí |  |
| 4 | `descripcion` | texto (máx. 500) | No |  |
| 5 | `publicado` | true/false (o si/no) | No | true |

```
LISVID["*"]
LISVID["1"]
LISVID["titulo","texto"]
INSVID["","Resumen del congreso","https://www.youtube.com/watch?v=abc123","","true"]
MODVID["1","","Resumen del congreso","https://www.youtube.com/watch?v=abc123","","true"]
ELIVID["1"]
```

## CU7 · Consultas del formulario de contacto y búsqueda

### CCO · Categorías de consulta (tabla `categorias_consulta`)

Comandos: `LISCCO`, `INSCCO`, `MODCCO`, `ELICCO`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `nombre` | texto (máx. 80) | Sí |  |
| 2 | `correo_destino` | correo electrónico (máx. 150) | No |  |

```
LISCCO["*"]
LISCCO["1"]
LISCCO["nombre","texto"]
INSCCO["Becas","becas@uagrm.edu.bo"]
MODCCO["1","Becas","becas@uagrm.edu.bo"]
ELICCO["1"]
```

### CON · Consultas del formulario de contacto (tabla `consultas`)

Comandos: `LISCON`, `INSCON`, `MODCON`, `ELICON`

| N° | Campo | Tipo | Obligatorio | Por defecto |
|---|---|---|---|---|
| 1 | `categoria_consulta_id` | número entero → id de categorias_consulta | No |  |
| 2 | `nombre` | texto (máx. 150) | Sí |  |
| 3 | `correo` | correo electrónico (máx. 150) | Sí |  |
| 4 | `telefono` | texto (máx. 30) | No |  |
| 5 | `mensaje` | texto largo | Sí |  |
| 6 | `estado` | una opción de la lista: nueva \| en_revision \| respondida \| cerrada | No | nueva |
| 7 | `respuesta` | texto largo | No |  |

```
LISCON["*"]
LISCON["1"]
LISCON["nombre","texto"]
INSCON["1","Mariana López","mariana@gmail.com","70123456","¿Cuáles son los requisitos de inscripción?","nueva",""]
MODCON["1","1","Mariana López","mariana@gmail.com","70123456","¿Cuáles son los requisitos de inscripción?","nueva",""]
ELICON["1"]
```

## CU7 / CU8 · Búsqueda y reportes

| Comando | Descripción |
|---|---|
| `BUSCAR["texto"]` | Búsqueda global en los contenidos del sitio |
| `REPGEN["*"]` | Resumen general: cantidad de registros por tabla y caso de uso |
| `REPVIS["10"]` | Páginas más visitadas (top N) y visitas de los últimos 7 días |
| `REPCON["*"] o ["desde","hasta"]` | Consultas recibidas por estado y por categoría |
| `REPGRA["*"]` | Graduados por situación laboral, año y modalidad de titulación |
| `REPBIB["10"]` | Documentos más descargados de la biblioteca digital |
| `REPNOT["*"]` | Noticias por estado y categoría, y próximos eventos |
| `REPINV["*"]` | Proyectos de investigación por estado, tipo y línea |
| `REPBIT["*"] o ["desde","hasta"]` | Actividad de la bitácora por módulo y acción |
| `AYUDA["*"]` / `AYUDA["USU"]` | Ayuda general o de una tabla |
