import java.sql.*;

public class CuentasStorage {

    // --- OBTENER SALDOS ---
    public static double getSaldoCaja() {
        return getSaldos()[0];
    }

    public static double getSaldoBanco() {
        return getSaldos()[1];
    }

    /** Devuelve [saldoCaja, saldoBanco] en una sola consulta. */
    public static double[] getSaldosActuales() {
        return getSaldos();
    }

    private static double[] getSaldos() {
        String sql = "SELECT saldo_caja, saldo_banco FROM saldos_cuentas WHERE id = 1";
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new double[]{rs.getDouble("saldo_caja"), rs.getDouble("saldo_banco")};
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new double[]{0.0, 0.0};
    }

    // --- ACTUALIZAR SALDOS EN BASE DE DATOS ---
    public static synchronized void actualizarSaldos(double nuevoSaldoCaja, double nuevoSaldoBanco) {
        // UPSERT: Si la fila 1 no existe, la crea. Si ya existe, solo le actualiza el saldo.
        String sql = "INSERT INTO saldos_cuentas (id, saldo_caja, saldo_banco) " +
                    "VALUES (1, ?, ?) " +
                    "ON CONFLICT (id) DO UPDATE " +
                    "SET saldo_caja = EXCLUDED.saldo_caja, saldo_banco = EXCLUDED.saldo_banco";
                    
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
        double[] saldos = getSaldos();
        actualizarSaldos(saldos[0] + monto, saldos[1]);
    }

    public static synchronized void restarDeCaja(double monto) {
        double[] saldos = getSaldos();
        actualizarSaldos(saldos[0] - monto, saldos[1]);
    }

    public static synchronized void agregarABanco(double monto) {
        double[] saldos = getSaldos();
        actualizarSaldos(saldos[0], saldos[1] + monto);
    }

    public static synchronized void restarDeBanco(double monto) {
        double[] saldos = getSaldos();
        actualizarSaldos(saldos[0], saldos[1] - monto);
    }
}