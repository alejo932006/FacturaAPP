import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class LoginGUI extends JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private BackgroundPanel panelFondo;

    public LoginGUI() {
        setTitle("Inicio de Sesión - Sistema de Facturación");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
        initComponents();
    }

    private void initComponents() {
        panelFondo = new BackgroundPanel();
        panelFondo.setLayout(new GridBagLayout());
        setContentPane(panelFondo);

        JPanel panelContenido = new JPanel(new GridBagLayout());
        panelContenido.setOpaque(false);
        panelContenido.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        JLabel lblLogo = new JLabel();
        lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
        cargarLogo(lblLogo);
        gbc.gridx = 0; gbc.gridy = 0;
        panelContenido.add(lblLogo, gbc);

        Font fuenteCampos = new Font("Segoe UI", Font.PLAIN, 18);
        
        gbc.gridy++; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.fill = GridBagConstraints.NONE;
        JLabel lblUsuario = new JLabel("Usuario:");
        lblUsuario.setFont(fuenteCampos);
        lblUsuario.setForeground(Color.WHITE);
        panelContenido.add(lblUsuario, gbc);

        txtUsuario = new JTextField();
        txtUsuario.setFont(fuenteCampos);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.ipady = 7; gbc.ipadx = 200;
        panelContenido.add(txtUsuario, gbc);
        gbc.ipadx = 0; gbc.ipady = 0;
        
        gbc.gridy++; gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        JLabel lblPassword = new JLabel("Contraseña:");
        lblPassword.setFont(fuenteCampos);
        lblPassword.setForeground(Color.WHITE);
        panelContenido.add(lblPassword, gbc);

        txtPassword = new JPasswordField();
        txtPassword.setFont(fuenteCampos);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.ipady = 7; gbc.ipadx = 200;
        panelContenido.add(txtPassword, gbc);
        gbc.ipadx = 0; gbc.ipady = 0;

        JButton btnLogin = new JButton("Iniciar Sesión");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 20));
        btnLogin.setBackground(new Color(66, 133, 244));
        btnLogin.setForeground(Color.WHITE);
        gbc.gridy++; gbc.gridx = 0;
        gbc.gridwidth = 2; gbc.insets = new Insets(25, 5, 10, 5);
        panelContenido.add(btnLogin, gbc);
        
        JButton btnCambiarFondo = new JButton("Cambiar Fondo");
        btnCambiarFondo.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        gbc.gridy++; gbc.insets = new Insets(10, 5, 10, 5);
        panelContenido.add(btnCambiarFondo, gbc);

        add(panelContenido);

        btnLogin.addActionListener(e -> intentarLogin());
        txtPassword.addActionListener(e -> intentarLogin());
        btnCambiarFondo.addActionListener(e -> cambiarFondo());
    }

    private void intentarLogin() {
        String usuario = txtUsuario.getText();
        String password = new String(txtPassword.getPassword());

        UserSession.Rol rol = UserStorage.autenticarUsuario(usuario, password);
        
        if (rol != null) {
            UserSession.login(usuario, rol);
            SwingUtilities.invokeLater(() -> new DashboardGUI().setVisible(true));
            dispose(); // Cerramos la ventana de login
        } else {
            JOptionPane.showMessageDialog(this,
                "Usuario o contraseña incorrectos.",
                "Error de Autenticación",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // El resto de la clase (cargarLogo, cambiarFondo y la clase interna) no cambia
    private void cargarLogo(JLabel label) {
        ImageIcon icono = null;
        try {
            File logoExterno = PathsHelper.getLogoFile();
            java.net.URL imgUrl;

            if (logoExterno.exists() && !logoExterno.isDirectory()) {
                imgUrl = logoExterno.toURI().toURL();
            } else {
                imgUrl = getClass().getResource("/img/logo.jpeg");
            }

            if (imgUrl != null) {
                icono = new ImageIcon(imgUrl);
                Image imagen = icono.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                label.setIcon(new ImageIcon(imagen));
            } else {
                label.setText("Logo no encontrado");
            }
        } catch (Exception e) {
            e.printStackTrace();
            label.setText("Error al cargar logo");
        }
    }

    private void cambiarFondo() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Imágenes (JPG, PNG)", "jpg", "jpeg", "png");
        chooser.setFileFilter(filter);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivoSeleccionado = chooser.getSelectedFile();
            File destino = PathsHelper.getFondoLoginFile();
            try {
                Files.copy(archivoSeleccionado.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                panelFondo.cargarImagenDeFondo();
                panelFondo.repaint();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al guardar la imagen de fondo.", "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    class BackgroundPanel extends JPanel {
        private Image backgroundImage;
        public BackgroundPanel() { cargarImagenDeFondo(); }
        public void cargarImagenDeFondo() {
            File archivoFondo = PathsHelper.getFondoLoginFile();
            if (archivoFondo.exists()) {
                this.backgroundImage = new ImageIcon(archivoFondo.getAbsolutePath()).getImage();
            } else {
                setBackground(new Color(45, 48, 51));
                this.backgroundImage = null;
            }
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, this.getWidth(), this.getHeight(), this);
            }
        }
    }
}