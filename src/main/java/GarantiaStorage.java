// Archivo nuevo: GarantiaStorage.java
import java.nio.file.Files;
import java.nio.file.Path;

public class GarantiaStorage {

    /**
     * Carga el texto de la política de garantía desde el archivo.
     * Si el archivo no existe, devuelve un texto por defecto.
     */
    public static String cargarPoliticaGarantia() {
        try {
            Path ruta = PathsHelper.getPoliticaGarantiaFile().toPath();
            return Files.readString(ruta);
        } catch (Exception e) {
            // Si hay un error o el archivo no existe, devuelve el texto por defecto.
            return "=== POLÍTICAS DE GARANTÍA ===\n" +
                   "* Celulares nuevos: 1 año de garantía.\n" +
                   "* Celulares usados: 3 meses de garantía.\n" +
                   "* Reparaciones: 15 días de garantía sobre el arreglo.\n" +
                   "* Accesorios: 15 días de garantía.\n\n" +
                   "NOTA: La garantía solo cubre daños de fábrica, no por mala manipulación (humedad, golpes, sobrevoltaje, etc.).";
        }
    }

    /**
     * Guarda el nuevo texto de la política de garantía en el archivo.
     */
    public static void guardarPoliticaGarantia(String texto) {
        try {
            Path ruta = PathsHelper.getPoliticaGarantiaFile().toPath();
            Files.writeString(ruta, texto);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}