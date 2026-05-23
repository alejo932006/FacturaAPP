import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MachineIdentifier {

    public static String getMachineId() {
        String os = System.getProperty("os.name").toLowerCase();
        String uuid = "";

        try {
            if (os.contains("win")) {
                // Comando nativo para leer el UUID de la placa base en Windows
                Process process = Runtime.getRuntime().exec(new String[]{"wmic", "csproduct", "get", "UUID"});
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.equalsIgnoreCase("UUID")) {
                        uuid = line;
                        break;
                    }
                }
            } else if (os.contains("mac")) {
                // Comando nativo para leer el UUID del hardware en macOS
                Process process = Runtime.getRuntime().exec(new String[]{"/usr/sbin/ioreg", "-rd1", "-c", "IOPlatformExpertDevice"});
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("IOPlatformUUID")) {
                        // Extraemos solo el número entre las comillas
                        uuid = line.split("=")[1].replace("\"", "").trim();
                        break;
                    }
                }
            }
            
            if (!uuid.isEmpty()) {
                return uuid;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "ID_NO_DISPONIBLE";
    }
}