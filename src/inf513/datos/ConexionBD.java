package inf513.datos;

import inf513.util.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Abre conexiones JDBC a PostgreSQL con los datos de config.properties (bd.*). */
public final class ConexionBD {

    private ConexionBD() {
    }

    public static String url() {
        return "jdbc:postgresql://" + Config.get("bd.host", "localhost") + ":" + Config.get("bd.puerto", "5432")
                + "/" + Config.get("bd.nombre", "db_grupo23sa");
    }

    public static Connection abrir() throws SQLException {
        Properties p = new Properties();
        p.setProperty("user", Config.get("bd.usuario", "grupo23sa"));
        p.setProperty("password", Config.get("bd.password", ""));
        p.setProperty("connectTimeout", "15");
        p.setProperty("socketTimeout", "60");
        p.setProperty("ApplicationName", "inf513-proyecto1");
        return DriverManager.getConnection(url(), p);
    }
}
