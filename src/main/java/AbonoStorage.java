import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class AbonoStorage {

    // --- GUARDAR ABONO (INSERT) ---
    public static void guardarAbono(Abono abono) {
        String sql = "INSERT INTO abonos_compromisos (compromiso_id, fecha, monto_capital, monto_interes, metodo_pago) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // El ID del compromiso debe ser numérico ahora
            int compromisoId = Integer.parseInt(abono.getCompromisoId());
            
            pstmt.setInt(1, compromisoId);
            pstmt.setDate(2, java.sql.Date.valueOf(abono.getFecha()));
            pstmt.setDouble(3, abono.getMontoCapital());
            pstmt.setDouble(4, abono.getMontoInteres());
            pstmt.setString(5, abono.getMetodoPago());

            pstmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al guardar abono: " + e.getMessage());
        }
    }

    // --- CARGAR TODOS LOS ABONOS (SELECT) ---
    public static List<Abono> cargarTodosLosAbonos() {
        List<Abono> abonos = new ArrayList<>();
        String sql = "SELECT * FROM abonos_compromisos ORDER BY fecha ASC";

        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String id = String.valueOf(rs.getInt("id"));
                String compromisoId = String.valueOf(rs.getInt("compromiso_id"));
                LocalDate fecha = rs.getDate("fecha").toLocalDate();
                double capital = rs.getDouble("monto_capital");
                double interes = rs.getDouble("monto_interes");
                String metodo = rs.getString("metodo_pago");

                abonos.add(new Abono(id, compromisoId, fecha, capital, interes, metodo));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return abonos;
    }

    public static void eliminarAbonosPorCompromiso(String compromisoId) {
        String sql = "DELETE FROM abonos_compromisos WHERE compromiso_id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(compromisoId));
            pstmt.executeUpdate();
            // No mostramos mensaje aquí para que sea transparente al usuario

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error al eliminar abonos del compromiso " + compromisoId + ": " + e.getMessage());
        }
    }
}