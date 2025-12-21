import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class FacturaApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Configuración visual (Look and Feel)
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Asegurar que el admin tenga contraseña (puedes quitar esto después si ya la cambiaste)
            UserStorage.actualizarContrasena("admin", "1234");

            // Verificar licencia e iniciar Login
            if (LicenseManager.checkLicense()) {
                LoginGUI ventanaLogin = new LoginGUI();
                ventanaLogin.setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}