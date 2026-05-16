import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;
import javax.swing.JOptionPane;

public class ConexionDB {

    private static Connection connection = null;

    public static Connection conectar() {
        try {
            if (connection == null || connection.isClosed()) {
                
                // 1. Cargar las propiedades
                Properties props = new Properties();
                try (InputStream input = new FileInputStream("config.properties")) {
                    props.load(input);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Error leyendo config.properties", "Error", JOptionPane.ERROR_MESSAGE);
                    return null;
                }

                // 2. Construir la URL con los datos del archivo
                String url = "jdbc:postgresql://" + props.getProperty("db.host") + ":" + 
                             props.getProperty("db.port") + "/" + props.getProperty("db.name");
                String user = props.getProperty("db.user");
                String pass = props.getProperty("db.password");

                try {
                    Class.forName("org.postgresql.Driver");
                } catch (ClassNotFoundException e) {
                    JOptionPane.showMessageDialog(null, "Error: No se encontró el Driver de PostgreSQL.", "Error de Driver", JOptionPane.ERROR_MESSAGE);
                    return null;
                }

                connection = DriverManager.getConnection(url, user, pass);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al conectar con la Base de Datos:\n" + e.getMessage(), "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return connection;
    }

    public static void cerrar() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}