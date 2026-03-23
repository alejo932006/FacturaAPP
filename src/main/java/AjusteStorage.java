import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AjusteStorage {

    // --- GUARDAR AJUSTE EN POSTGRESQL ---
    public static void guardarAjuste(Ajuste ajuste) {
        String sql = "INSERT INTO ajustes_saldo (fecha, tipo, cuenta, monto, motivo) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDate(1, java.sql.Date.valueOf(ajuste.getFecha()));
            pstmt.setString(2, ajuste.getTipo());
            pstmt.setString(3, ajuste.getCuenta());
            pstmt.setDouble(4, ajuste.getMonto());
            pstmt.setString(5, ajuste.getMotivo());
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error guardando el ajuste en la base de datos.");
        }
    }
    
    // --- CARGAR HISTORIAL DE AJUSTES ---
    public static List<Ajuste> cargarAjustes() {
        List<Ajuste> lista = new ArrayList<>();
        String sql = "SELECT * FROM ajustes_saldo ORDER BY fecha DESC, id DESC";
        
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
             
            while (rs.next()) {
                LocalDate fecha = rs.getDate("fecha").toLocalDate();
                String tipo = rs.getString("tipo");
                String cuenta = rs.getString("cuenta");
                double monto = rs.getDouble("monto");
                String motivo = rs.getString("motivo");
                
                lista.add(new Ajuste(fecha, tipo, cuenta, monto, motivo));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}