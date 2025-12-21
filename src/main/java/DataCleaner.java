import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JOptionPane;

public class DataCleaner {

    // --- LIMPIAR BASE DE DATOS ---
    public static void vaciarTablasSeleccionadas(List<String> tablas) {
        if (tablas.isEmpty()) return;

        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement()) {
            
            conn.setAutoCommit(false); // Transacción para seguridad

            for (String tabla : tablas) {
                // TRUNCATE elimina rápido y reinicia contadores (IDs). 
                // CASCADE borra también los datos hijos (ej. borrar Clientes borra sus Facturas si hay relación).
                String sql = "TRUNCATE TABLE " + tabla + " RESTART IDENTITY CASCADE";
                stmt.addBatch(sql);
                System.out.println("Cola de borrado: " + tabla);
            }

            stmt.executeBatch();
            conn.commit();
            JOptionPane.showMessageDialog(null, "Tablas vaciadas exitosamente en la Base de Datos.");

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al vaciar BD: " + e.getMessage(), "Error Crítico", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- LIMPIAR ARCHIVOS CSV ---
    public static void vaciarArchivosSeleccionados(boolean clientes, boolean productos, boolean arqueos, boolean devoluciones, boolean facturasTxt) {
        try {
            if (clientes) vaciarArchivo(new File("clientes.csv"));
            if (productos) vaciarArchivo(new File("productos.csv"));
            if (arqueos) vaciarArchivo(PathsHelper.getArqueosCSV());
            if (devoluciones) vaciarArchivo(PathsHelper.getDevolucionesCSV());
            
            if (facturasTxt) {
                File carpetaFacturas = PathsHelper.getFacturasFolder();
                if (carpetaFacturas.exists()) {
                    for (File archivo : carpetaFacturas.listFiles()) {
                        if (archivo.getName().endsWith(".txt")) {
                            archivo.delete();
                        }
                    }
                }
            }
            JOptionPane.showMessageDialog(null, "Archivos seleccionados han sido vaciados/eliminados.");
            
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al limpiar archivos: " + e.getMessage());
        }
    }

    private static void vaciarArchivo(File archivo) throws IOException {
        if (archivo != null && archivo.exists()) {
            // Sobrescribir con cadena vacía borra el contenido
            try (FileWriter fw = new FileWriter(archivo, false)) {
                fw.write("");
            }
        }
    }
}