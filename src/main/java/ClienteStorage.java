import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class ClienteStorage {
    
    // --- GUARDAR CLIENTE (INSERT) ---
    // Cambiamos 'void' por 'boolean'
    public static boolean guardarCliente(Cliente cliente) {
        String sql = "INSERT INTO clientes (cedula, nombre, direccion, email) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexionDB.conectar();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cliente.getCedula());
            pstmt.setString(2, cliente.getNombre());
            pstmt.setString(3, cliente.getDireccion());
            pstmt.setString(4, cliente.getEmail());

            pstmt.executeUpdate();
            return true; // <-- Retornamos true si todo salió perfecto

        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) { 
                // Mensaje actualizado para incluir el correo:
                JOptionPane.showMessageDialog(null, "Ya existe un cliente registrado con esta cédula o este correo electrónico.\nPor favor verifica los datos.", "Dato Duplicado", JOptionPane.WARNING_MESSAGE);
            } else {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error al guardar cliente: " + e.getMessage(), "Error BD", JOptionPane.ERROR_MESSAGE);
            }
            return false; // <-- Retornamos false si la base de datos lo rechazó
        }
    }

    // --- CARGAR TODOS LOS CLIENTES (SELECT) ---
    public static List<Cliente> cargarClientes() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM clientes ORDER BY nombre ASC";

        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // CORRECCIÓN: Leemos la columna 'email' y usamos el nuevo constructor
                lista.add(new Cliente(
                    rs.getString("nombre"),
                    rs.getString("cedula"),
                    rs.getString("direccion"),
                    rs.getString("email") // <--- NUEVO CAMPO RECUPERADO
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // --- ELIMINAR CLIENTE (DELETE) ---
    // (Este método no requiere cambios, ya que elimina por cédula)
    public static void eliminarCliente(String cedulaCliente) {
        String sql = "DELETE FROM clientes WHERE cedula = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cedulaCliente);
            int filas = pstmt.executeUpdate();
            
            if (filas == 0) {
                JOptionPane.showMessageDialog(null, "No se encontró el cliente para eliminar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            }

        } catch (SQLException e) {
            // Error común: Integridad referencial (El cliente tiene facturas)
            if (e.getSQLState().startsWith("23")) {
                JOptionPane.showMessageDialog(null, "No se puede eliminar el cliente porque tiene facturas asociadas.", "Error de Integridad", JOptionPane.ERROR_MESSAGE);
            } else {
                e.printStackTrace();
            }
        }
    }

    // --- VERIFICAR EXISTENCIA (SELECT COUNT) ---
    public static boolean clienteExiste(String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) { 
            return false;
        }

        String sql = "SELECT 1 FROM clientes WHERE cedula = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cedula);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Retorna true si encontró algo
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}