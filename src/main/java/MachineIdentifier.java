import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MachineIdentifier {

    public static String getMachineId() {
        String os = System.getProperty("os.name").toLowerCase();
        String uuid = "";

        try {
            if (os.contains("win")) {
                // Estrategia 1: Leer el MachineGuid desde el Registro de Windows (Estándar moderno y muy seguro)
                uuid = getWindowsRegistryUUID();
                
                // Estrategia 2: Si el registro falla, usar PowerShell (Nativo en Windows 10 y 11)
                if (uuid.isEmpty()) {
                    uuid = getWindowsPowerShellUUID();
                }
                
                // Estrategia 3: Si lo anterior falla, usar WMIC como último recurso para Windows antiguos
                if (uuid.isEmpty()) {
                    uuid = getWindowsWmicUUID();
                }
            } else if (os.contains("mac")) {
                // Comando nativo e infalible para macOS (IOreg)
                Process process = Runtime.getRuntime().exec(new String[]{"/usr/sbin/ioreg", "-rd1", "-c", "IOPlatformExpertDevice"});
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("IOPlatformUUID")) {
                        uuid = line.split("=")[1].replace("\"", "").trim();
                        break;
                    }
                }
                reader.close();
            }
            
            if (uuid != null && !uuid.trim().isEmpty()) {
                return uuid.trim().toUpperCase();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Estrategia de Respaldo Absoluto: Si el sistema operativo bloquea todo, 
        // genera un ID único basado en el nombre del usuario para que la app nunca se quede colgada.
        return "FALLBACK-" + System.getProperty("user.name").toUpperCase().hashCode();
    }

    private static String getWindowsRegistryUUID() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"reg", "query", "HKLM\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("MachineGuid")) {
                    String[] parts = line.split("REG_SZ");
                    if (parts.length > 1) {
                        return parts[1].trim();
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            // Falla silenciosamente para pasar al siguiente método
        }
        return "";
    }

    private static String getWindowsPowerShellUUID() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"powershell", "-Command", "(Get-CimInstance -ClassName Win32_ComputerSystemProduct).UUID"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            if (line != null && !line.trim().isEmpty()) {
                return line.trim();
            }
        } catch (Exception e) {
            // Falla silenciosamente
        }
        return "";
    }

    private static String getWindowsWmicUUID() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"wmic", "csproduct", "get", "UUID"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.equalsIgnoreCase("UUID") && !line.contains("error")) {
                    return line;
                }
            }
            reader.close();
        } catch (Exception e) {
            // Falla silenciosamente
        }
        return "";
    }
}