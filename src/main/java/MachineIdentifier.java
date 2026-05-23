import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class MachineIdentifier {

    public static String getMachineId() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            List<String> macs = new ArrayList<>();
            String primaryMac = null;

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface network = networkInterfaces.nextElement();
                byte[] mac = network.getHardwareAddress();

                if (mac != null && !network.isLoopback() && !network.isVirtual()) {
                    String name = network.getName().toLowerCase();
                    
                    // 1. Ignorar basura virtual de raíz
                    if (name.contains("awdl") || name.contains("utun") || name.contains("bridge") || 
                        name.contains("vmnet") || name.contains("vbox") || name.contains("llw")) {
                        continue;
                    }

                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    String macStr = sb.toString();
                    
                    // 2. EL FRANCOTIRADOR: Si encuentra la tarjeta principal de Mac (en0) o Windows (eth0/wlan0), la asegura inmediatamente.
                    if (name.equals("en0") || name.equals("eth0") || name.equals("wlan0")) {
                        primaryMac = macStr;
                    }
                    
                    macs.add(macStr);
                }
            }

            // Prioridad Absoluta: La tarjeta física principal (NUNCA cambia al reiniciar)
            if (primaryMac != null) {
                return primaryMac;
            }

            // Respaldo: Si por algún motivo no existe en0, usa la primera de la lista ordenada
            if (!macs.isEmpty()) {
                Collections.sort(macs);
                return macs.get(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "ID_NO_DISPONIBLE";
    }
}