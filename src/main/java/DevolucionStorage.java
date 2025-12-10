import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import javax.swing.JOptionPane;

public class DevolucionStorage {

    public static void guardarDevolucion(String numFactura, String codigoProducto, String nombreProducto, int cantidad, double valor) {
        String fechaHoy = LocalDate.now().toString();
        // El formato ahora es: fecha;factura;código;nombre;cantidad;valor
        String linea = String.join(";",
            fechaHoy,
            numFactura,
            codigoProducto,
            nombreProducto,
            String.valueOf(cantidad),
            String.valueOf(valor)
        );
    
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PathsHelper.getDevolucionesCSV(), true))) {
            writer.write(linea);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al guardar la devolución.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}