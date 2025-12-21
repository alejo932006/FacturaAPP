import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.JOptionPane;

public class CajaStorage {

    // --- CARGAR ARQUEOS (SELECT) ---
    public static List<ArqueoCaja> cargarArqueos() {
        List<ArqueoCaja> lista = new ArrayList<>();
        String sql = "SELECT * FROM arqueos_caja ORDER BY fecha DESC"; // Más recientes primero

        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Reconstruimos el objeto ArqueoCaja desde la BD
                ArqueoCaja arqueo = new ArqueoCaja(
                    rs.getDate("fecha").toLocalDate(),
                    rs.getDouble("base_inicial"),
                    rs.getDouble("ventas_contado"),
                    rs.getDouble("ventas_transferencia"),
                    rs.getDouble("gastos_efectivo"),
                    rs.getDouble("ingresos_extra"),
                    rs.getDouble("retiros_efectivo"),
                    rs.getDouble("efectivo_esperado"),
                    rs.getDouble("efectivo_final_real"),
                    rs.getDouble("diferencia"),
                    ArqueoCaja.Estado.valueOf(rs.getString("estado"))
                );
                lista.add(arqueo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al cargar arqueos: " + e.getMessage());
        }
        return lista;
    }

    // --- GUARDAR O ACTUALIZAR ARQUEO (UPSERT) ---
    // Reemplaza al antiguo guardarArqueos(List)
    public static void guardarArqueo(ArqueoCaja arqueo) {
        // Intentamos actualizar primero (si ya existe caja hoy)
        if (existeArqueo(arqueo.getFecha())) {
            actualizarArqueo(arqueo);
        } else {
            insertarArqueo(arqueo);
        }
    }

    private static boolean existeArqueo(LocalDate fecha) {
        String sql = "SELECT 1 FROM arqueos_caja WHERE fecha = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(fecha));
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void insertarArqueo(ArqueoCaja a) {
        String sql = "INSERT INTO arqueos_caja (fecha, base_inicial, ventas_contado, ventas_transferencia, " +
                     "gastos_efectivo, ingresos_extra, retiros_efectivo, efectivo_esperado, " +
                     "efectivo_final_real, diferencia, estado) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        ejecutarUpdate(sql, a);
    }

    private static void actualizarArqueo(ArqueoCaja a) {
        String sql = "UPDATE arqueos_caja SET base_inicial=?, ventas_contado=?, ventas_transferencia=?, " +
                     "gastos_efectivo=?, ingresos_extra=?, retiros_efectivo=?, efectivo_esperado=?, " +
                     "efectivo_final_real=?, diferencia=?, estado=? WHERE fecha=?";
        // Nota: en el UPDATE el orden de parámetros cambia ligeramente, el WHERE va al final
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, a.getBaseInicial());
            pstmt.setDouble(2, a.getVentasContado());
            pstmt.setDouble(3, a.getVentasTransferencia());
            pstmt.setDouble(4, a.getGastosEfectivo());
            pstmt.setDouble(5, a.getIngresosExtra());
            pstmt.setDouble(6, a.getRetirosEfectivo());
            pstmt.setDouble(7, a.getEfectivoEsperado());
            pstmt.setDouble(8, a.getEfectivoFinalReal());
            pstmt.setDouble(9, a.getDiferencia());
            pstmt.setString(10, a.getEstado().toString());
            pstmt.setDate(11, java.sql.Date.valueOf(a.getFecha())); // WHERE fecha = ...

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al actualizar caja: " + e.getMessage());
        }
    }

    // Helper para el INSERT (ya que los parámetros están en orden)
    private static void ejecutarUpdate(String sql, ArqueoCaja a) {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDate(1, java.sql.Date.valueOf(a.getFecha()));
            pstmt.setDouble(2, a.getBaseInicial());
            pstmt.setDouble(3, a.getVentasContado());
            pstmt.setDouble(4, a.getVentasTransferencia());
            pstmt.setDouble(5, a.getGastosEfectivo());
            pstmt.setDouble(6, a.getIngresosExtra());
            pstmt.setDouble(7, a.getRetirosEfectivo());
            pstmt.setDouble(8, a.getEfectivoEsperado());
            pstmt.setDouble(9, a.getEfectivoFinalReal());
            pstmt.setDouble(10, a.getDiferencia());
            pstmt.setString(11, a.getEstado().toString());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error SQL Caja: " + e.getMessage());
        }
    }

    public static Optional<ArqueoCaja> getCajaAbiertaHoy() {
        return cargarArqueos().stream()
            .filter(a -> a.getFecha().equals(LocalDate.now()) && a.getEstado() == ArqueoCaja.Estado.ABIERTA)
            .findFirst();
    }

    public static void guardarArqueos(List<ArqueoCaja> lista) {
        for (ArqueoCaja a : lista) {
            guardarArqueo(a);
        }
    }

    public static Optional<ArqueoCaja> getUltimaCajaCerrada() {
        // Solicitamos a la BD solo la caja CERRADA con la fecha más reciente
        String sql = "SELECT * FROM arqueos_caja WHERE estado = 'CERRADA' ORDER BY fecha DESC LIMIT 1";

        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                ArqueoCaja arqueo = new ArqueoCaja(
                    rs.getDate("fecha").toLocalDate(),
                    rs.getDouble("base_inicial"),
                    rs.getDouble("ventas_contado"),
                    rs.getDouble("ventas_transferencia"),
                    rs.getDouble("gastos_efectivo"),
                    rs.getDouble("ingresos_extra"),
                    rs.getDouble("retiros_efectivo"),
                    rs.getDouble("efectivo_esperado"),
                    rs.getDouble("efectivo_final_real"),
                    rs.getDouble("diferencia"),
                    ArqueoCaja.Estado.valueOf(rs.getString("estado"))
                );
                return Optional.of(arqueo);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al obtener última caja cerrada: " + e.getMessage());
        }
        
        // Si no hay ninguna caja cerrada o hubo error, retornamos vacío
        return Optional.empty();
    }
    
}
