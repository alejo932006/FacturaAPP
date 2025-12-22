import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;

public class CompromisoStorage {

    // --- CARGAR TODOS LOS COMPROMISOS (SELECT) ---
    public static List<Compromiso> cargarCompromisos() {
        List<Compromiso> lista = new ArrayList<>();
        String sql = "SELECT * FROM compromisos ORDER BY fecha_vencimiento ASC";

        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Recuperar datos básicos
                String id = String.valueOf(rs.getInt("id"));
                String descripcion = rs.getString("descripcion");
                double monto = rs.getDouble("monto_total");
                LocalDate fechaVenc = rs.getDate("fecha_vencimiento").toLocalDate();
                Compromiso.Estado estado = Compromiso.Estado.valueOf(rs.getString("estado"));
                String notas = rs.getString("notas");
                
                // Recuperar configuraciones avanzadas
                boolean esRecurrente = rs.getBoolean("es_recurrente");
                int diaVencimiento = rs.getInt("dia_vencimiento");
                boolean tieneIntereses = rs.getBoolean("tiene_intereses");
                String tipoStr = rs.getString("tipo");
                Compromiso.Tipo tipo = (tipoStr != null) ? Compromiso.Tipo.valueOf(tipoStr) : Compromiso.Tipo.DEUDA_TOTAL;

                // Recuperar historial de pagos (para recurrentes)
                String historialStr = rs.getString("historial_pagos");
                List<LocalDate> historial = new ArrayList<>();
                if (historialStr != null && !historialStr.isEmpty()) {
                    String[] fechas = historialStr.split("\\|");
                    for (String f : fechas) {
                        if (!f.isEmpty()) historial.add(LocalDate.parse(f));
                    }
                }

                // Usamos el constructor completo
                // Nota: 'fechaPago' no lo guardamos en columna aparte, lo asumimos null o del historial
                lista.add(new Compromiso(id, descripcion, monto, fechaVenc, estado, null, notas, 
                                         esRecurrente, diaVencimiento, tieneIntereses, tipo, historial));
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al cargar compromisos: " + e.getMessage());
        }
        return lista;
    }

    // --- GUARDAR O ACTUALIZAR (UPSERT LÓGICO) ---
    // Como tu GUI usa "guardarTodosLosCompromisos" y "agregarCompromiso", 
    // redirigimos todo a métodos de base de datos inteligentes.

    public static void agregarCompromiso(Compromiso c) {
        String sql = "INSERT INTO compromisos (descripcion, monto_total, fecha_vencimiento, estado, notas, " +
                     "es_recurrente, dia_vencimiento, tiene_intereses, tipo, historial_pagos) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, c.getDescripcion());
            pstmt.setDouble(2, c.getMonto());
            pstmt.setDate(3, java.sql.Date.valueOf(c.getFechaVencimiento()));
            pstmt.setString(4, c.getEstado().toString());
            pstmt.setString(5, c.getNotas());
            pstmt.setBoolean(6, c.esRecurrente());
            pstmt.setInt(7, c.getDiaDeVencimiento());
            pstmt.setBoolean(8, c.tieneIntereses());
            pstmt.setString(9, c.getTipo().toString());
            
            // Convertir lista de historial a String
            String historialStr = c.getHistorialDePagos().stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining("|"));
            pstmt.setString(10, historialStr);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al guardar compromiso: " + e.getMessage());
        }
    }

    public static void guardarTodosLosCompromisos(List<Compromiso> compromisos) {
        // En base de datos no se guardan "todos de golpe". 
        // Actualizamos uno por uno (UPDATE) si ya tienen ID numérico.
        for (Compromiso c : compromisos) {
            if (esIdNumerico(c.getId())) {
                actualizarCompromiso(c);
            } else {
                // Si el ID es un UUID temporal (de una sesión nueva), lo insertamos
                agregarCompromiso(c);
            }
        }
    }

    private static void actualizarCompromiso(Compromiso c) {
        String sql = "UPDATE compromisos SET descripcion=?, monto_total=?, fecha_vencimiento=?, estado=?, notas=?, " +
                     "es_recurrente=?, dia_vencimiento=?, tiene_intereses=?, tipo=?, historial_pagos=? " +
                     "WHERE id=?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, c.getDescripcion());
            pstmt.setDouble(2, c.getMonto());
            pstmt.setDate(3, java.sql.Date.valueOf(c.getFechaVencimiento()));
            pstmt.setString(4, c.getEstado().toString());
            pstmt.setString(5, c.getNotas());
            pstmt.setBoolean(6, c.esRecurrente());
            pstmt.setInt(7, c.getDiaDeVencimiento());
            pstmt.setBoolean(8, c.tieneIntereses());
            pstmt.setString(9, c.getTipo().toString());
            
            String historialStr = c.getHistorialDePagos().stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining("|"));
            pstmt.setString(10, historialStr);
            
            pstmt.setInt(11, Integer.parseInt(c.getId()));

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Método auxiliar para saber si un ID viene de la BD (es un número)
    private static boolean esIdNumerico(String id) {
        try {
            Integer.parseInt(id);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // En CompromisoStorage.java

    public static void eliminarCompromiso(String id) {
        // Validación de ID
        if (!esIdNumerico(id)) return;

        // 1. PRIMERO: Eliminamos los abonos hijos para evitar error de Foreign Key
        // (Esto no afectará a los pagos periódicos si no tienen registros en la tabla abonos)
        AbonoStorage.eliminarAbonosPorCompromiso(id);

        // 2. SEGUNDO: Eliminamos el compromiso padre
        String sql = "DELETE FROM compromisos WHERE id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(id));
            int filasAfectadas = pstmt.executeUpdate();

            if (filasAfectadas > 0) {
                JOptionPane.showMessageDialog(null, "Compromiso eliminado correctamente.");
            } else {
                JOptionPane.showMessageDialog(null, "No se pudo eliminar el compromiso.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al eliminar el compromiso: " + e.getMessage());
        }
    }
}