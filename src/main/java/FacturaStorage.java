import java.sql.*;
import javax.swing.JOptionPane;

public class FacturaStorage {

    public static boolean guardarFacturaCompleta(Factura factura) {
        String sqlFactura = "INSERT INTO facturas (numero_factura, fecha, hora, cliente_cedula, metodo_pago, total_venta, usuario_responsable) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        
        String sqlDetalle = "INSERT INTO detalle_facturas (factura_id, producto_codigo, nombre_producto_snapshot, cantidad, precio_unitario, costo_unitario, subtotal, descuento, detalle_extra) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement psFactura = null;
        PreparedStatement psDetalle = null;
        ResultSet rs = null;

        try {
            conn = ConexionDB.conectar();
            // 1. Desactivar el autoguardado para manejar transacción manual
            conn.setAutoCommit(false); 

            // --- GUARDAR ENCABEZADO ---
            psFactura = conn.prepareStatement(sqlFactura);
            psFactura.setString(1, factura.getNumeroFactura());
            psFactura.setDate(2, java.sql.Date.valueOf(java.time.LocalDate.now()));
            psFactura.setTime(3, java.sql.Time.valueOf(java.time.LocalTime.now()));
            psFactura.setString(4, factura.getCliente().getCedula());
            psFactura.setString(5, factura.getMetodoPago());
            psFactura.setDouble(6, factura.calcularTotal());
            
            // Intentamos obtener el usuario logueado, si no hay, ponemos "Sistema"
            String usuario = UserSession.getLoggedInUsername();
            psFactura.setString(7, usuario != null ? usuario : "Sistema");

            psFactura.executeQuery(); // Usamos executeQuery porque tiene RETURNING id
            rs = psFactura.getResultSet();
            
            int facturaId = 0;
            if (rs.next()) {
                facturaId = rs.getInt(1); // Obtenemos el ID generado por PostgreSQL
            }

            // --- GUARDAR DETALLES ---
            psDetalle = conn.prepareStatement(sqlDetalle);
            for (DetalleFactura det : factura.getDetalles()) {
                psDetalle.setInt(1, facturaId);
                psDetalle.setString(2, det.getProducto().getCodigo());
                psDetalle.setString(3, det.getProducto().getNombre()); 
                psDetalle.setDouble(4, det.getCantidad());
                psDetalle.setDouble(5, det.getProducto().getPrecioVenta());
                psDetalle.setDouble(6, det.getProducto().getCosto()); 
                psDetalle.setDouble(7, det.getSubtotal());
                psDetalle.setDouble(8, det.getDescuento());
                psDetalle.setString(9, det.getDetalle() != null ? det.getDetalle() : ""); 

                psDetalle.addBatch(); // Añadir al lote
            }
            
            psDetalle.executeBatch(); // Ejecutar todo el lote de productos

            // 2. Si todo salió bien, confirmamos los cambios
            conn.commit();
            return true;

        } catch (SQLException e) {
            // 3. Si algo falló, deshacemos todo
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error crítico al guardar factura en BD: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (psFactura != null) psFactura.close();
                if (psDetalle != null) psDetalle.close();
                if (conn != null) {
                    conn.setAutoCommit(true); // Restaurar estado
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}