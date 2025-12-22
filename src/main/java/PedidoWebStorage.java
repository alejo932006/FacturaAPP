import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class PedidoWebStorage {

    public List<PedidoWeb> obtenerPedidos() {
        List<PedidoWeb> lista = new ArrayList<>();
        String sql = "SELECT * FROM pedidos ORDER BY fecha_pedido DESC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                PedidoWeb p = new PedidoWeb();
                p.setId(rs.getInt("id"));
                p.setClienteNombre(rs.getString("cliente_nombre"));
                p.setClienteCedula(rs.getString("cliente_cedula"));
                p.setClienteTelefono(rs.getString("cliente_telefono"));
                p.setClienteDireccion(rs.getString("cliente_direccion"));
                p.setMetodoPago(rs.getString("metodo_pago"));
                p.setTotalVenta(rs.getDouble("total_venta"));
                p.setDetalleProductos(rs.getString("detalle_productos"));
                p.setFechaPedido(rs.getTimestamp("fecha_pedido"));
                p.setEstado(rs.getString("estado"));
                p.setClienteEmail(rs.getString("cliente_email"));
                lista.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al cargar pedidos web: " + e.getMessage());
        }
        return lista;
    }

    public boolean actualizarEstado(int id, String nuevoEstado) {
        String sql = "UPDATE pedidos SET estado = ? WHERE id = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nuevoEstado);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean eliminarPedido(int id) {
        String sql = "DELETE FROM pedidos WHERE id = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int contarPedidosPendientes() {
        String sql = "SELECT COUNT(*) FROM pedidos WHERE estado = 'Pendiente'";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}