import javax.swing.JOptionPane;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserStorage {

    // Carga todos los usuarios desde la Base de Datos
    private static Map<String, String[]> cargarTodosLosUsuarios() {
        Map<String, String[]> usuarios = new HashMap<>();
        String sql = "SELECT username, password_hash, rol FROM usuarios";

        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Key: username, Value: [password_hash, ROL]
                usuarios.put(rs.getString("username"), new String[]{
                    rs.getString("password_hash"), 
                    rs.getString("rol")
                });
            }
            
            // Si la base de datos está vacía, creamos el admin por defecto
            if (usuarios.isEmpty()) {
                crearAdminPorDefecto();
                // Recargamos recursivamente para asegurarnos que esté en el mapa
                return cargarTodosLosUsuarios(); 
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al cargar usuarios de BD: " + e.getMessage());
        }
        return usuarios;
    }

    private static void crearAdminPorDefecto() {
        String usuario = "admin";
        String pass = "1234";
        
        // Llamamos directamente a crearUsuario, que se encarga de hashear y guardar
        crearUsuario(usuario, pass, UserSession.Rol.ADMINISTRADOR);
        System.out.println("Usuario Admin por defecto creado en la BD.");
    }
    
    public static List<String> cargarUsuariosParaVista() {
        Map<String, String[]> todosLosUsuarios = cargarTodosLosUsuarios();
        List<String> vista = new ArrayList<>();
        todosLosUsuarios.forEach((usuario, data) -> {
            vista.add(String.format("%s (%s)", usuario, data[1]));
        });
        return vista;
    }

    public static UserSession.Rol autenticarUsuario(String username, String password) {
        // Consultamos directamente a la BD por seguridad
        String sql = "SELECT password_hash, rol FROM usuarios WHERE username = ?";
        
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hashAlmacenado = rs.getString("password_hash");
                String rolStr = rs.getString("rol");

                if (PasswordManager.verificarPassword(password, hashAlmacenado)) {
                    try {
                        return UserSession.Rol.valueOf(rolStr);
                    } catch (IllegalArgumentException e) {
                        return null; 
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Login fallido
    }

    public static boolean crearUsuario(String username, String password, UserSession.Rol rol) {
        String sql = "INSERT INTO usuarios (username, password_hash, rol) VALUES (?, ?, ?)";
        String hash = PasswordManager.hashPassword(password);

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hash);
            pstmt.setString(3, rol.toString());
            
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) { // Código de error PK duplicada
                JOptionPane.showMessageDialog(null, "El usuario ya existe.", "Error", JOptionPane.WARNING_MESSAGE);
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }

    public static void eliminarUsuario(String username) {
        String sql = "DELETE FROM usuarios WHERE username = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al eliminar usuario: " + e.getMessage());
        }
    }
    
    public static void actualizarContrasena(String username, String nuevaContrasena) {
        String sql = "UPDATE usuarios SET password_hash = ? WHERE username = ?";
        String nuevoHash = PasswordManager.hashPassword(nuevaContrasena);

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, nuevoHash);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al cambiar contraseña: " + e.getMessage());
        }
    }
}