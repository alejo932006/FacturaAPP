import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.swing.JOptionPane;

public class ConexionDB {

    private static HikariDataSource dataSource;

    private ConexionDB() {
    }

    public static Connection conectar() {
        try {
            inicializarPoolSiEsNecesario();
            return dataSource.getConnection();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error al conectar con la Base de Datos:\n" + e.getMessage(),
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
    }

    private static synchronized void inicializarPoolSiEsNecesario() throws SQLException {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        Properties props = cargarConfiguracionDb();
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(construirJdbcUrl(props));
        config.setUsername(props.getProperty("db.user"));
        config.setPassword(props.getProperty("db.password"));
        config.setDriverClassName("org.postgresql.Driver");

        config.setMaximumPoolSize(parseInt(props, "db.pool.max", 5));
        config.setMinimumIdle(parseInt(props, "db.pool.min", 1));
        config.setConnectionTimeout(parseLong(props, "db.connect.timeout.ms", 30_000));
        config.setIdleTimeout(parseLong(props, "db.idle.timeout.ms", 600_000));
        config.setMaxLifetime(parseLong(props, "db.max.lifetime.ms", 1_800_000));
        config.setKeepaliveTime(parseLong(props, "db.keepalive.ms", 60_000));
        config.setPoolName("FacturaAppPool");

        config.addDataSourceProperty("tcpKeepAlive", "true");
        config.addDataSourceProperty("prepareThreshold", "1");
        config.addDataSourceProperty("ApplicationName", "FacturaAPP");
        config.addDataSourceProperty("connectTimeout", String.valueOf(parseInt(props, "db.pg.connect.timeout.seg", 10)));
        config.addDataSourceProperty("socketTimeout", String.valueOf(parseInt(props, "db.pg.socket.timeout.seg", 60)));

        dataSource = new HikariDataSource(config);
    }

    private static Properties cargarConfiguracionDb() throws SQLException {
        Properties props = new Properties();
        File configAppData = PathsHelper.getConfigProperties();
        File configLocal = new File("config.properties");

        try {
            if (configAppData.exists()) {
                try (InputStream input = new FileInputStream(configAppData)) {
                    props.load(input);
                }
            }

            if (configLocal.exists()) {
                Properties localProps = new Properties();
                try (InputStream input = new FileInputStream(configLocal)) {
                    localProps.load(input);
                }
                if (configAppData.exists()) {
                    // Completar claves db.* faltantes desde el config local del proyecto
                    for (String key : localProps.stringPropertyNames()) {
                        if (key.startsWith("db.")) {
                            String valor = props.getProperty(key);
                            if (valor == null || valor.isBlank()) {
                                props.setProperty(key, localProps.getProperty(key));
                            }
                        }
                    }
                } else {
                    props.putAll(localProps);
                }
            }

            if (props.isEmpty()) {
                throw new SQLException("No se encontró config.properties en %APPDATA%\\FacturaApp ni en la carpeta de la app.");
            }
        } catch (Exception ex) {
            throw new SQLException("Error leyendo config.properties: " + ex.getMessage(), ex);
        }

        validarPropiedad(props, "db.host");
        validarPropiedad(props, "db.port");
        validarPropiedad(props, "db.name");
        validarPropiedad(props, "db.user");
        validarPropiedad(props, "db.password");
        return props;
    }

    private static void validarPropiedad(Properties props, String clave) throws SQLException {
        if (props.getProperty(clave) == null || props.getProperty(clave).isBlank()) {
            throw new SQLException("Falta la propiedad '" + clave + "' en config.properties");
        }
    }

    private static String construirJdbcUrl(Properties props) {
        String host = props.getProperty("db.host").trim();
        String port = props.getProperty("db.port").trim();
        String name = props.getProperty("db.name").trim();
        String extra = props.getProperty("db.jdbc.params", "tcpKeepAlive=true&prepareThreshold=1");
        return "jdbc:postgresql://" + host + ":" + port + "/" + name + "?" + extra;
    }

    private static int parseInt(Properties props, String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static long parseLong(Properties props, String key, long defaultValue) {
        try {
            return Long.parseLong(props.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static void cerrar() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
