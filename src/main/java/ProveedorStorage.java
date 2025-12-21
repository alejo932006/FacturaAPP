import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class ProveedorStorage {

    // Crear tabla automáticamente si no existe
    public static void inicializarTabla() {
        String sql = "CREATE TABLE IF NOT EXISTS proveedores (" +
                     "nit VARCHAR(50) PRIMARY KEY, " +
                     "nombre VARCHAR(100) NOT NULL, " +
                     "telefono VARCHAR(50), " +
                     "direccion VARCHAR(150), " +
                     "email VARCHAR(100))";
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean guardarProveedor(Proveedor p) {
        inicializarTabla(); // Asegura que la tabla exista
        String sql = "INSERT INTO proveedores (nit, nombre, telefono, direccion, email) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getNit());
            pstmt.setString(2, p.getNombre());
            pstmt.setString(3, p.getTelefono());
            pstmt.setString(4, p.getDireccion());
            pstmt.setString(5, p.getEmail());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
            return false;
        }
    }
    
    public static boolean actualizarProveedor(Proveedor p) {
        String sql = "UPDATE proveedores SET nombre=?, telefono=?, direccion=?, email=? WHERE nit=?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getNombre());
            pstmt.setString(2, p.getTelefono());
            pstmt.setString(3, p.getDireccion());
            pstmt.setString(4, p.getEmail());
            pstmt.setString(5, p.getNit());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static boolean eliminarProveedor(String nit) {
         String sql = "DELETE FROM proveedores WHERE nit=?";
         try (Connection conn = ConexionDB.conectar();
              PreparedStatement pstmt = conn.prepareStatement(sql)) {
             pstmt.setString(1, nit);
             return pstmt.executeUpdate() > 0;
         } catch (SQLException e) {
             JOptionPane.showMessageDialog(null, "No se puede eliminar: " + e.getMessage());
             return false;
         }
    }

    public static List<Proveedor> listarProveedores() {
        inicializarTabla();
        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT * FROM proveedores ORDER BY nombre ASC";
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Proveedor(
                    rs.getString("nit"),
                    rs.getString("nombre"),
                    rs.getString("telefono"),
                    rs.getString("direccion"),
                    rs.getString("email")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    // --- CONSULTAR COMPRAS (Productos en inventario de este proveedor) ---
    public static List<Object[]> obtenerProductosPorProveedor(String nitProveedor) {
        List<Object[]> productos = new ArrayList<>();
        // Buscamos en la tabla 'productos' donde coincida el NIT del proveedor
        String sql = "SELECT codigo, nombre, cantidad, costo_compra FROM productos WHERE nit_proveedor = ?";
        
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nitProveedor);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                productos.add(new Object[]{
                    rs.getString("codigo"),
                    rs.getString("nombre"),
                    rs.getDouble("cantidad"),
                    rs.getDouble("costo_compra"),
                    rs.getDouble("cantidad") * rs.getDouble("costo_compra") // Total invertido
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return productos;
    }
}