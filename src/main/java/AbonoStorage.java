import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class AbonoStorage {

    // --- GUARDAR ABONO (INSERT) Y ACTUALIZAR ESTADO ---
    public static void guardarAbono(Abono abono) {
        String sqlInsert = "INSERT INTO abonos_compromisos (compromiso_id, fecha, monto_capital, monto_interes, metodo_pago) " +
                           "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConexionDB.conectar()) {
            
            // 1. Insertamos el abono
            int compromisoId = Integer.parseInt(abono.getCompromisoId());
            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                pstmt.setInt(1, compromisoId);
                pstmt.setDate(2, java.sql.Date.valueOf(abono.getFecha()));
                pstmt.setDouble(3, abono.getMontoCapital());
                pstmt.setDouble(4, abono.getMontoInteres());
                pstmt.setString(5, abono.getMetodoPago());
                pstmt.executeUpdate();
            }

            // 2. MAGIA: Actualizamos el estado del compromiso padre
            actualizarEstadoCompromiso(compromisoId, conn);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al guardar abono: " + e.getMessage());
        }
    }

    // --- FUNCIÓN INTELIGENTE PARA CALCULAR ESTADOS ---
    private static void actualizarEstadoCompromiso(int compromisoId, Connection conn) throws SQLException {
        // A. Obtener el total abonado a capital
        double totalAbonado = 0;
        String sqlSuma = "SELECT COALESCE(SUM(monto_capital), 0) AS total FROM abonos_compromisos WHERE compromiso_id = ?";
        try (PreparedStatement pstSuma = conn.prepareStatement(sqlSuma)) {
            pstSuma.setInt(1, compromisoId);
            ResultSet rs = pstSuma.executeQuery();
            if (rs.next()) totalAbonado = rs.getDouble("total");
        }

        // B. Obtener los datos del compromiso
        String sqlComp = "SELECT monto_total, tipo, historial_pagos FROM compromisos WHERE id = ?";
        double montoTotal = 0;
        String tipo = "DEUDA_TOTAL";
        String historial = "";
        
        try (PreparedStatement pstComp = conn.prepareStatement(sqlComp)) {
            pstComp.setInt(1, compromisoId);
            ResultSet rs = pstComp.executeQuery();
            if (rs.next()) {
                montoTotal = rs.getDouble("monto_total");
                tipo = rs.getString("tipo");
                historial = rs.getString("historial_pagos");
                if (historial == null) historial = "";
            }
        }

        // C. Lógica según el Tipo (Deuda vs Periódico)
        if ("DEUDA_TOTAL".equals(tipo)) {
            String nuevoEstado = (totalAbonado >= montoTotal) ? "PAGADO" : "PENDIENTE";
            String sqlUpdate = "UPDATE compromisos SET estado = ? WHERE id = ?";
            try (PreparedStatement pstUpd = conn.prepareStatement(sqlUpdate)) {
                pstUpd.setString(1, nuevoEstado);
                pstUpd.setInt(2, compromisoId);
                pstUpd.executeUpdate();
            }
        } 
        else if ("PAGO_PERIODICO".equals(tipo)) {
            // Guardamos la fecha actual en el historial separado por "|"
            LocalDate hoy = LocalDate.now();
            String mesActualStr = hoy.toString().substring(0, 7); // Da como resultado "YYYY-MM"
            
            // Si no le hemos pagado este mes, lo agregamos
            if (!historial.contains(mesActualStr)) {
                String nuevoHistorial = historial.isEmpty() ? hoy.toString() : historial + "|" + hoy.toString();
                String sqlUpdate = "UPDATE compromisos SET historial_pagos = ? WHERE id = ?";
                try (PreparedStatement pstUpd = conn.prepareStatement(sqlUpdate)) {
                    pstUpd.setString(1, nuevoHistorial);
                    pstUpd.setInt(2, compromisoId);
                    pstUpd.executeUpdate();
                }
            }
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
        } catch (SQLException e) { e.printStackTrace(); }
        return abonos;
    }

    public static void eliminarAbonosPorCompromiso(String compromisoId) {
        String sql = "DELETE FROM abonos_compromisos WHERE compromiso_id = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(compromisoId));
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}