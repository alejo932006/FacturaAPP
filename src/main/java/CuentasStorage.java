import java.sql.*;

public class CuentasStorage {

    // --- OBTENER SALDOS ---
    public static double getSaldoCaja() {
        String sql = "SELECT saldo_caja FROM saldos_cuentas WHERE id = 1";
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble("saldo_caja");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public static double getSaldoBanco() {
        String sql = "SELECT saldo_banco FROM saldos_cuentas WHERE id = 1";
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble("saldo_banco");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // --- ACTUALIZAR SALDOS EN BASE DE DATOS ---
    public static synchronized void actualizarSaldos(double nuevoSaldoCaja, double nuevoSaldoBanco) {
        String sql = "UPDATE saldos_cuentas SET saldo_caja = ?, saldo_banco = ? WHERE id = 1";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, nuevoSaldoCaja);
            pstmt.setDouble(2, nuevoSaldoBanco);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- MÉTODOS PÚBLICOS (Intactos para no romper el resto del programa) ---
    public static synchronized void agregarACaja(double monto) {
        actualizarSaldos(getSaldoCaja() + monto, getSaldoBanco());
    }

    public static synchronized void restarDeCaja(double monto) {
        actualizarSaldos(getSaldoCaja() - monto, getSaldoBanco());
    }

    public static synchronized void agregarABanco(double monto) {
        actualizarSaldos(getSaldoCaja(), getSaldoBanco() + monto);
    }

    public static synchronized void restarDeBanco(double monto) {
        actualizarSaldos(getSaldoCaja(), getSaldoBanco() - monto);
    }
}