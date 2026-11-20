package inf513.negocio;

import java.sql.Types;

/** Tipos de dato que se validan antes de enviar un parámetro al motor SQL. */
public enum TipoDato {
    ENTERO("número entero", Types.BIGINT),
    DECIMAL("número decimal", Types.NUMERIC),
    BOOLEANO("true/false (o si/no)", Types.BOOLEAN),
    FECHA("fecha AAAA-MM-DD", Types.DATE),
    HORA("hora HH:MM", Types.TIME),
    TEXTO("texto", Types.VARCHAR),
    TEXTO_LARGO("texto largo", Types.VARCHAR),
    EMAIL("correo electrónico", Types.VARCHAR),
    URL("enlace http(s)://", Types.VARCHAR),
    OPCION("una opción de la lista", Types.VARCHAR),
    CLAVE("contraseña (mín. 6 caracteres)", Types.VARCHAR),
    SLUG("slug (vacío = se genera del título)", Types.VARCHAR);

    private final String descripcion;
    private final int tipoSql;

    TipoDato(String descripcion, int tipoSql) {
        this.descripcion = descripcion;
        this.tipoSql = tipoSql;
    }

    public String descripcion() {
        return descripcion;
    }

    public int tipoSql() {
        return tipoSql;
    }

    public boolean esTexto() {
        return tipoSql == Types.VARCHAR;
    }
}
