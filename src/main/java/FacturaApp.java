// En el archivo: FacturaApp.java

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class FacturaApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // --- INICIO DEL CÓDIGO AÑADIDO ---
                // Establece el Look and Feel "Nimbus" para un aspecto moderno y estable.
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
                // --- FIN DEL CÓDIGO AÑADIDO ---
            } catch (Exception e) {
                // Si Nimbus no está disponible, no es un error crítico,
                // la aplicación simplemente usará el L&F por defecto.
                e.printStackTrace();
            }

            UserStorage.actualizarContrasena("admin", "1234");

            // El resto de tu código original no cambia.
            if (LicenseManager.checkLicense()) {
                LoginGUI ventanaLogin = new LoginGUI();
                ventanaLogin.setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}