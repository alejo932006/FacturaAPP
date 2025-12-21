// En el archivo: MachineIdentifier.java
// Reemplaza todo el contenido con este código.

import java.net.NetworkInterface;
import java.util.Enumeration;

public class MachineIdentifier {

    public static String getMachineId() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface network = networkInterfaces.nextElement();
                byte[] mac = network.getHardwareAddress();

                if (mac != null && !network.isLoopback() && !network.isVirtual()) {
                    
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "ID_NO_DISPONIBLE";
    }
}