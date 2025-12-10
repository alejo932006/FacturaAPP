import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class LineaStorage {

    // Cargar todas las líneas para el ComboBox
    public static List<String> cargarLineas() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nombre FROM lineas ORDER BY nombre ASC";
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(rs.getString("nombre"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // Agregar nueva línea
    public static boolean agregarLinea(String nombre) {
        String sql = "INSERT INTO lineas (nombre) VALUES (?)";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error: La línea ya existe o hubo un problema.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // Eliminar línea
    public static boolean eliminarLinea(String nombre) {
        String sql = "DELETE FROM lineas WHERE nombre = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}