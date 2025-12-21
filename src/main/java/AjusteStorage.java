// Archivo nuevo: AjusteStorage.java
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AjusteStorage {

    private static File getAjustesCSV() {
        return new File(PathsHelper.getDatosFolder(), "ajustes_saldo.csv");
    }

    public static void guardarAjuste(Ajuste ajuste) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getAjustesCSV(), true))) {
            String linea = String.join(";",
                ajuste.getFecha().toString(),
                ajuste.getTipo(),
                ajuste.getCuenta(),
                String.valueOf(ajuste.getMonto()),
                ajuste.getMotivo()
            );
            writer.write(linea);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Opcional: un método para cargar los ajustes si se quisiera hacer un historial
    public static List<Ajuste> cargarAjustes() {
        List<Ajuste> lista = new ArrayList<>();
        // ... (lógica para leer el CSV si se necesita)
        return lista;
    }
}