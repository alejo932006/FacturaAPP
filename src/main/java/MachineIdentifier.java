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
            
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface network = networkInterfaces.nextElement();
                byte[] mac = network.getHardwareAddress();
                
                // 1. Filtramos tarjetas virtuales exclusivas de Mac/Windows (AirDrop, Docker, VPNs, Túneles)
                String name = network.getName().toLowerCase();
                boolean isIgnored = name.contains("awdl") || name.contains("utun") || 
                                    name.contains("bridge") || name.contains("vmnet") || 
                                    name.contains("vbox") || name.contains("llw");

                if (mac != null && !network.isLoopback() && !network.isVirtual() && !isIgnored) {
                    
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    macs.add(sb.toString());
                }
            }
            
            if (!macs.isEmpty()) {
                // 2. ORDENAMOS la lista alfabéticamente. 
                // Así garantizamos que SIEMPRE se seleccione la misma MAC física, sin importar el orden del sistema.
                Collections.sort(macs);
                return macs.get(0);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "ID_NO_DISPONIBLE";
    }
}