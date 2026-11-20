package inf513.negocio;

/**
 * Columna de una tabla que el usuario puede enviar como parámetro.
 * Se construye con estilo fluido en el Catálogo, por ejemplo:
 *   Campo.texto("titulo", 150).obligatorio()
 *   Campo.opcion("estado", "nueva", "respondida").porDefecto("nueva")
 */
public final class Campo {

    private final String columna;
    private final TipoDato tipo;
    private boolean obligatorio;
    private int maximo;               // longitud máxima (textos)
    private Long minimoNum;           // rango (enteros)
    private Long maximoNum;
    private String[] opciones = new String[0];
    private String porDefecto;        // valor si llega "" (refleja el DEFAULT de la tabla)
    private String origenSlug;        // columna desde la que se genera el slug
    private String referencia;        // tabla referenciada (clave foránea), solo informativo
    private String ejemplo;

    private Campo(String columna, TipoDato tipo) {
        this.columna = columna;
        this.tipo = tipo;
    }

    // ---- fábricas ----
    public static Campo texto(String columna, int max) {
        Campo c = new Campo(columna, TipoDato.TEXTO);
        c.maximo = max;
        return c;
    }

    public static Campo textoLargo(String columna) {
        Campo c = new Campo(columna, TipoDato.TEXTO_LARGO);
        c.maximo = 20000;
        return c;
    }

    public static Campo email(String columna, int max) {
        Campo c = new Campo(columna, TipoDato.EMAIL);
        c.maximo = max;
        return c;
    }

    public static Campo url(String columna, int max) {
        Campo c = new Campo(columna, TipoDato.URL);
        c.maximo = max;
        return c;
    }

    public static Campo entero(String columna) {
        return new Campo(columna, TipoDato.ENTERO);
    }

    public static Campo id(String columna, String tablaReferenciada) {
        Campo c = new Campo(columna, TipoDato.ENTERO);
        c.minimoNum = 1L;
        c.referencia = tablaReferenciada;
        c.ejemplo = "1";
        return c;
    }

    public static Campo booleano(String columna, boolean porDefecto) {
        Campo c = new Campo(columna, TipoDato.BOOLEANO);
        c.porDefecto = String.valueOf(porDefecto);
        return c;
    }

    public static Campo fecha(String columna) {
        return new Campo(columna, TipoDato.FECHA);
    }

    public static Campo hora(String columna) {
        return new Campo(columna, TipoDato.HORA);
    }

    public static Campo opcion(String columna, String... opciones) {
        Campo c = new Campo(columna, TipoDato.OPCION);
        c.opciones = opciones;
        c.maximo = 60;
        return c;
    }

    public static Campo clave(String columna) {
        Campo c = new Campo(columna, TipoDato.CLAVE);
        c.maximo = 72; // límite de BCrypt
        return c;
    }

    public static Campo slug(String columna, int max, String origen) {
        Campo c = new Campo(columna, TipoDato.SLUG);
        c.maximo = max;
        c.origenSlug = origen;
        return c;
    }

    // ---- modificadores ----
    public Campo obligatorio() {
        this.obligatorio = true;
        return this;
    }

    public Campo porDefecto(String valor) {
        this.porDefecto = valor;
        return this;
    }

    public Campo rango(long min, long max) {
        this.minimoNum = min;
        this.maximoNum = max;
        return this;
    }

    public Campo ejemplo(String ejemplo) {
        this.ejemplo = ejemplo;
        return this;
    }

    // ---- consultas ----
    public String getColumna() {
        return columna;
    }

    public TipoDato getTipo() {
        return tipo;
    }

    public boolean isObligatorio() {
        return obligatorio;
    }

    public int getMaximo() {
        return maximo;
    }

    public Long getMinimoNum() {
        return minimoNum;
    }

    public Long getMaximoNum() {
        return maximoNum;
    }

    public String[] getOpciones() {
        return opciones;
    }

    public String getPorDefecto() {
        return porDefecto;
    }

    public String getOrigenSlug() {
        return origenSlug;
    }

    public String getReferencia() {
        return referencia;
    }

    /** Valor de ejemplo para la ayuda (si no se definió uno, se deduce del tipo). */
    public String getEjemplo() {
        if (ejemplo != null) {
            return ejemplo;
        }
        switch (tipo) {
            case ENTERO:
                return minimoNum != null ? String.valueOf(Math.max(minimoNum, 1)) : "1";
            case DECIMAL:
                return "10.50";
            case BOOLEANO:
                return porDefecto != null ? porDefecto : "true";
            case FECHA:
                return "2026-11-20";
            case HORA:
                return "18:30";
            case EMAIL:
                return "correo@ejemplo.com";
            case URL:
                return "https://ejemplo.com";
            case OPCION:
                return porDefecto != null ? porDefecto : opciones[0];
            case CLAVE:
                return "secreto123";
            case SLUG:
                return "";
            default:
                return obligatorio ? "Texto de " + columna.replace('_', ' ') : "";
        }
    }

    /** Descripción corta del tipo para la ayuda. */
    public String describirTipo() {
        StringBuilder sb = new StringBuilder(tipo.descripcion());
        if (tipo == TipoDato.OPCION) {
            sb.append(": ").append(String.join(" | ", opciones));
        } else if ((tipo == TipoDato.TEXTO || tipo == TipoDato.EMAIL || tipo == TipoDato.URL || tipo == TipoDato.SLUG) && maximo > 0) {
            sb.append(" (máx. ").append(maximo).append(')');
        } else if (minimoNum != null && maximoNum != null) {
            sb.append(" (").append(minimoNum).append(" a ").append(maximoNum).append(')');
        }
        if (referencia != null) {
            sb.append(" → id de ").append(referencia);
        }
        return sb.toString();
    }
}
