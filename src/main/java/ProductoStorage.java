import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class ProductoStorage {

    // --- CARGAR TODOS LOS PRODUCTOS (SELECT) ---
    public static List<Producto> cargarProductos() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT * FROM productos ORDER BY nombre ASC";

        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(new Producto(
                    rs.getString("codigo"),
                    rs.getString("nombre"),
                    rs.getDouble("precio_venta"),
                    rs.getDouble("costo_compra"), // Ojo: en DB lo llamamos costo_compra
                    rs.getDouble("cantidad"),
                    rs.getString("estado"),
                    rs.getString("unidad_medida"),
                    rs.getString("area_encargada"),
                    rs.getString("nombre_proveedor"),
                    rs.getString("nit_proveedor")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al cargar productos de la BD: " + e.getMessage());
        }
        return lista;
    }

    // --- GUARDAR NUEVO PRODUCTO (INSERT) ---
    public static boolean guardarProducto(Producto p) {
        String sql = "INSERT INTO productos (codigo, nombre, precio_venta, costo_compra, cantidad, unidad_medida, estado, area_encargada, nombre_proveedor, nit_proveedor) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, p.getCodigo());
            pstmt.setString(2, p.getNombre());
            pstmt.setDouble(3, p.getPrecioVenta());
            pstmt.setDouble(4, p.getCosto());
            pstmt.setDouble(5, p.getCantidad());
            pstmt.setString(6, p.getUnidadDeMedida());
            pstmt.setString(7, p.getEstado());
            pstmt.setString(8, p.getAreaEncargada());
            pstmt.setString(9, p.getNombreProveedor());
            pstmt.setString(10, p.getNitProveedor());

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) { // Código de error para duplicados en Postgres
                JOptionPane.showMessageDialog(null, "El código del producto ya existe en la base de datos.", "Error de Duplicado", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Error al guardar producto: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            }
            e.printStackTrace();
            return false;
        }
    }

    // --- ACTUALIZAR PRODUCTO EXISTENTE (UPDATE) ---
    public static boolean actualizarProducto(Producto p) {
        String sql = "UPDATE productos SET nombre=?, precio_venta=?, costo_compra=?, cantidad=?, " +
                     "unidad_medida=?, estado=?, area_encargada=?, nombre_proveedor=?, nit_proveedor=? " +
                     "WHERE codigo=?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, p.getNombre());
            pstmt.setDouble(2, p.getPrecioVenta());
            pstmt.setDouble(3, p.getCosto());
            pstmt.setDouble(4, p.getCantidad());
            pstmt.setString(5, p.getUnidadDeMedida());
            pstmt.setString(6, p.getEstado());
            pstmt.setString(7, p.getAreaEncargada());
            pstmt.setString(8, p.getNombreProveedor());
            pstmt.setString(9, p.getNitProveedor());
            pstmt.setString(10, p.getCodigo()); // El WHERE va al final

            int filasAfectadas = pstmt.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar producto: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    // --- ELIMINAR PRODUCTO (DELETE) ---
    public static boolean eliminarProducto(String codigo) {
        // Primero verificamos si tiene historial
        if (productoTieneMovimiento(codigo)) {
            JOptionPane.showMessageDialog(null, "No se puede eliminar: El producto tiene facturas o movimientos asociados.\nInactívelo en su lugar.", "Integridad Referencial", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        String sql = "DELETE FROM productos WHERE codigo=?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, codigo);
            int filas = pstmt.executeUpdate();
            return filas > 0;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al eliminar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // --- VERIFICAR SI TIENE MOVIMIENTO ---
    public static boolean productoTieneMovimiento(String codigoProducto) {
        String sql = "SELECT 1 FROM detalle_facturas WHERE producto_codigo = ? LIMIT 1";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, codigoProducto);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); 

        } catch (SQLException e) {
            e.printStackTrace();
            return true; // Ante la duda, bloqueamos la eliminación
        }
    }
    
    // Método auxiliar para actualizar stock rápidamente
    public static void actualizarStock(String codigo, double nuevaCantidad) {
        String sql = "UPDATE productos SET cantidad = ? WHERE codigo = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, nuevaCantidad);
            pstmt.setString(2, codigo);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Método de compatibilidad
    public static void sobrescribirInventario(List<Producto> productos) {
        for (Producto p : productos) {
            actualizarProducto(p);
        }
    }

    // --- NUEVO: BORRADO FORZADO (ELIMINA HISTORIAL Y PRODUCTO) ---
    public static boolean eliminarProductoForzado(String codigo) {
        Connection conn = null;
        try {
            conn = ConexionDB.conectar();
            conn.setAutoCommit(false); // Iniciamos transacción manual

            // 1. Eliminar referencias en detalle_facturas
            String sqlDetalles = "DELETE FROM detalle_facturas WHERE producto_codigo = ?";
            try (PreparedStatement pstmtDet = conn.prepareStatement(sqlDetalles)) {
                pstmtDet.setString(1, codigo);
                pstmtDet.executeUpdate();
            }

            // 2. Eliminar el producto
            String sqlProducto = "DELETE FROM productos WHERE codigo = ?";
            try (PreparedStatement pstmtProd = conn.prepareStatement(sqlProducto)) {
                pstmtProd.setString(1, codigo);
                int filas = pstmtProd.executeUpdate();
                
                if (filas > 0) {
                    conn.commit(); // Confirmar cambios
                    return true;
                } else {
                    conn.rollback(); // Si falló borrar producto, deshacer borrado de detalles
                    return false;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            JOptionPane.showMessageDialog(null, "Error crítico al forzar eliminación: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}