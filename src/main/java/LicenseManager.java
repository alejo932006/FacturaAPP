// Crea este nuevo archivo: LicenseManager.java

import javax.swing.JOptionPane;
import java.io.File;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class LicenseManager {

    // IMPORTANTE: Esta es tu clave pública. La generaremos en el Paso 3.
    // DEBES REEMPLAZAR ESTE TEXTO por la clave que generes.
    private static final String PUBLIC_KEY_STRING = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4LhYVMbfl5i6m3PEdY8uDCqcRWXEw6pxANLY6huYl3YOR8SnHCyuh2r/kRbNBVXmlM4QQkoI/uzwYJAE1AFYvXUqCbKYIoccPZhjqnJeDcC/U7A7QGBDODjWA3a8l32U8zUh9J3K/D5kxPoOkKoOceIOYn6+JogC60WJEpMXE5KXiJJoJIxbn0USssEfSE8eUfcemDnFy2EbMARvaVZGIcZjqoCfVRxBzrGOH6gbaLbSqlDUWwBErxk2g6bGoT43kvlCtPIqbsIRwA6PNyGB/bP026nwNEFseJa68GBXvcuAAvIUu33CWTLgMB0brc4fctQpjxxXGFwhNncagM/CcQIDAQAB";

    public static boolean checkLicense() {
        File licenseFile = new File(PathsHelper.getDatosFolder(), "license.key");

        if (!licenseFile.exists()) {
            showError("Archivo de licencia no encontrado.\n\n" +
                      "Por favor, contacte al soporte y proporcione el ID de su máquina:\n" +
                      MachineIdentifier.getMachineId());
            return false;
        }

        try {
            String content = new String(Files.readAllBytes(licenseFile.toPath()));
            String[] parts = content.split("\\.");

            if (parts.length != 2) {
                showError("Formato de licencia inválido.");
                return false;
            }

            String dataBase64 = parts[0];
            String signatureBase64 = parts[1];
            
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            byte[] dataBytes = Base64.getDecoder().decode(dataBase64);
            
            // Verificación con la Clave Pública
            byte[] publicKeyBytes = Base64.getDecoder().decode(PUBLIC_KEY_STRING);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(dataBytes);

            if (!sig.verify(signatureBytes)) {
                showError("Firma de licencia inválida. La licencia puede estar corrupta.");
                return false;
            }
            
            // Si la firma es válida, verificamos los datos internos
            String licenseData = new String(dataBytes);
            String[] dataParts = licenseData.split(";");
            
            String licensedToMachineId = dataParts[1];
            long expiryTimestamp = Long.parseLong(dataParts[2]);

            if (!licensedToMachineId.equals(MachineIdentifier.getMachineId())) {
                showError("La licencia no pertenece a esta máquina.\n\n" +
                          "ID de esta máquina: " + MachineIdentifier.getMachineId());
                return false;
            }

            if (System.currentTimeMillis() > expiryTimestamp) {
                showError("La licencia ha expirado.");
                return false;
            }

            // Si todas las verificaciones pasan...
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            showError("Ocurrió un error al verificar la licencia: " + e.getMessage());
            return false;
        }
    }

    private static void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Error de Licencia", JOptionPane.ERROR_MESSAGE);
    }
}