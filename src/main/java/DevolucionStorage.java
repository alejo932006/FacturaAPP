import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import javax.swing.JOptionPane;

public class DevolucionStorage {

    public static void guardarDevolucion(String numFactura, String codigoProducto, String nombreProducto, int cantidad, double valor) {
        String sql = "INSERT INTO devoluciones (fecha, numero_factura, codigo_producto, nombre_producto, cantidad, valor_total) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
            pstmt.setString(2, numFactura);
            pstmt.setString(3, codigoProducto);
            pstmt.setString(4, nombreProducto);
            pstmt.setInt(5, cantidad);
            pstmt.setDouble(6, valor);

            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al guardar la devolución en BD: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }
}