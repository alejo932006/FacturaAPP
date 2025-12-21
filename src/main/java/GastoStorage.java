import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class GastoStorage {

    // --- CARGAR GASTOS (SELECT) ---
    public static List<Gasto> cargarGastos() {
        List<Gasto> gastos = new ArrayList<>();
        String sql = "SELECT * FROM gastos ORDER BY fecha DESC";

        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Convertimos el ID numérico de la BD a String para que sea compatible con tu clase Gasto
                String id = String.valueOf(rs.getInt("id"));
                java.sql.Date fechaSql = rs.getDate("fecha");
                String descripcion = rs.getString("descripcion");
                double monto = rs.getDouble("monto");
                String metodo = rs.getString("metodo_pago");

                gastos.add(new Gasto(id, fechaSql.toLocalDate(), descripcion, monto, metodo));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al cargar gastos: " + e.getMessage());
        }
        return gastos;
    }

    // --- AGREGAR GASTO (INSERT) ---
    public static boolean agregarGasto(Gasto gasto) {
        String sql = "INSERT INTO gastos (fecha, descripcion, monto, metodo_pago) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, java.sql.Date.valueOf(gasto.getFecha()));
            pstmt.setString(2, gasto.getDescripcion());
            pstmt.setDouble(3, gasto.getMonto());
            pstmt.setString(4, gasto.getMetodoPago());

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al guardar el gasto: " + e.getMessage());
            return false;
        }
    }

    // --- ACTUALIZAR GASTO (UPDATE) ---
    public static boolean actualizarGasto(Gasto gasto) {
        String sql = "UPDATE gastos SET fecha=?, descripcion=?, monto=?, metodo_pago=? WHERE id=?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, java.sql.Date.valueOf(gasto.getFecha()));
            pstmt.setString(2, gasto.getDescripcion());
            pstmt.setDouble(3, gasto.getMonto());
            pstmt.setString(4, gasto.getMetodoPago());
            
            // Convertimos el ID String de vuelta a Entero para la BD
            int idNumerico = Integer.parseInt(gasto.getId());
            pstmt.setInt(5, idNumerico);

            pstmt.executeUpdate();
            return true;

        } catch (Exception e) { // Catch Exception para capturar errores de parseo de ID también
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al actualizar gasto: " + e.getMessage());
            return false;
        }
    }

    // --- ELIMINAR GASTO (DELETE) ---
    public static boolean eliminarGasto(String id) {
        String sql = "DELETE FROM gastos WHERE id=?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int idNumerico = Integer.parseInt(id);
            pstmt.setInt(1, idNumerico);
            
            pstmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al eliminar gasto: " + e.getMessage());
            return false;
        }
    }

    // Método de compatibilidad (ya no se usa, pero se deja por si acaso alguna referencia queda suelta)
    public static void guardarTodosLosGastos(List<Gasto> gastos) {
        // No hace nada en modo BD, ya que guardamos uno por uno.
    }
}