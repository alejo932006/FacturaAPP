import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class ConexionDB {

    // Configuración de la base de datos
    private static final String URL = "jdbc:postgresql://localhost:5432/FacturaAPP";
    private static final String USER = "postgres"; // Usuario por defecto de Postgres
    private static final String PASSWORD = "0534";     // Sin contraseña como indicaste

    // Objeto único de conexión (Singleton básico)
    private static Connection connection = null;

    public static Connection conectar() {
        try {
            if (connection == null || connection.isClosed()) {
                // Carga el driver (necesario en versiones antiguas de Java, buena práctica)
                try {
                    Class.forName("org.postgresql.Driver");
                } catch (ClassNotFoundException e) {
                    JOptionPane.showMessageDialog(null, "Error: No se encontró el Driver de PostgreSQL.\nAsegúrate de incluir la librería .jar en el proyecto.", "Error de Driver", JOptionPane.ERROR_MESSAGE); // CORREGIDO
                    return null;
                }

                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                // System.out.println("Conexión a PostgreSQL exitosa.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al conectar con la Base de Datos:\n" + e.getMessage(), "Error de Conexión", JOptionPane.ERROR_MESSAGE); // CORREGIDO
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