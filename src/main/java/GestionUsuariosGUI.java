import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GestionUsuariosGUI extends JDialog {

    private DefaultListModel<String> listModel;
    private JList<String> userList;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JComboBox<UserSession.Rol> comboRoles; // <-- NUEVO

    public GestionUsuariosGUI(Frame owner) {
        super(owner, "Gestión de Usuarios", true);
        setSize(550, 400); // Un poco más ancho
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        // --- Panel de Lista de Usuarios ---
        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        JScrollPane listScrollPane = new JScrollPane(userList);
        listScrollPane.setBorder(BorderFactory.createTitledBorder("Usuarios Existentes"));
        add(listScrollPane, BorderLayout.CENTER);

        // --- Panel para Crear y Eliminar ---
        JPanel panelAcciones = new JPanel(new BorderLayout(5, 5));
        
        // Subpanel para crear
        JPanel panelCrear = new JPanel(new GridBagLayout());
        panelCrear.setBorder(BorderFactory.createTitledBorder("Crear Nuevo Usuario"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtUsername = new JTextField(15);
        txtPassword = new JPasswordField(15);
        comboRoles = new JComboBox<>(UserSession.Rol.values()); // <-- NUEVO: Llenamos el combo con los roles
        JButton btnCrear = new JButton("Crear");

        gbc.gridx = 0; gbc.gridy = 0; panelCrear.add(new JLabel("Usuario:"), gbc);
        gbc.gridx = 1; panelCrear.add(txtUsername, gbc);
        gbc.gridy++; gbc.gridx = 0; panelCrear.add(new JLabel("Contraseña:"), gbc);
        gbc.gridx = 1; panelCrear.add(txtPassword, gbc);
        gbc.gridy++; gbc.gridx = 0; panelCrear.add(new JLabel("Rol:"), gbc); // <-- NUEVO
        gbc.gridx = 1; panelCrear.add(comboRoles, gbc); // <-- NUEVO
        gbc.gridy++; gbc.gridx = 1; gbc.fill = GridBagConstraints.NONE; panelCrear.add(btnCrear, gbc);
        
        // Subpanel para eliminar y cambiar pass
        JButton btnEliminar = new JButton("Eliminar Usuario Seleccionado");
        JButton btnCambiarPass = new JButton("Cambiar Contraseña");
        
        JPanel panelBotonesInferiores = new JPanel(new GridLayout(2, 1, 5, 5));
        panelBotonesInferiores.add(btnCambiarPass);
        panelBotonesInferiores.add(btnEliminar);

        panelAcciones.add(panelCrear, BorderLayout.CENTER);
        panelAcciones.add(panelBotonesInferiores, BorderLayout.SOUTH);
        
        add(panelAcciones, BorderLayout.EAST);
        
        // Lógica de botones
        btnCrear.addActionListener(e -> crearUsuario());
        btnEliminar.addActionListener(e -> eliminarUsuario());
        btnCambiarPass.addActionListener(e -> cambiarContrasena());

        cargarUsuarios();
    }
    
    private void cargarUsuarios() {
        listModel.clear();
        List<String> usuarios = UserStorage.cargarUsuariosParaVista(); // Usamos el nuevo método
        for (String usuario : usuarios) {
            listModel.addElement(usuario);
        }
    }

    private void crearUsuario() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        UserSession.Rol rolSeleccionado = (UserSession.Rol) comboRoles.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre de usuario y la contraseña no pueden estar vacíos.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (UserStorage.crearUsuario(username, password, rolSeleccionado)) {
            JOptionPane.showMessageDialog(this, "Usuario creado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarUsuarios();
            txtUsername.setText("");
            txtPassword.setText("");
        }
    }
    
    private String getSelectedUsername() {
        String selectedValue = userList.getSelectedValue();
        if (selectedValue == null) return null;
        // Extrae el nombre de usuario de "usuario (ROL)"
        return selectedValue.split(" \\(")[0];
    }

    private void eliminarUsuario() {
        String selectedUser = getSelectedUsername();
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un usuario de la lista para eliminar.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedUser.equals("admin")) {
            JOptionPane.showMessageDialog(this, "No se puede eliminar al usuario 'admin'.", "Acción no permitida", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int respuesta = JOptionPane.showConfirmDialog(this, "¿Está seguro de que desea eliminar al usuario '" + selectedUser + "'?", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);
        if (respuesta == JOptionPane.YES_OPTION) {
            UserStorage.eliminarUsuario(selectedUser);
            cargarUsuarios();
        }
    }

    private void cambiarContrasena() {
        String selectedUser = getSelectedUsername();
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un usuario de la lista.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
    
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JPasswordField passField1 = new JPasswordField(15);
        JPasswordField passField2 = new JPasswordField(15);
        panel.add(new JLabel("Nueva Contraseña:"));
        panel.add(passField1);
        panel.add(new JLabel("Confirmar Contraseña:"));
        panel.add(passField2);
    
        int option = JOptionPane.showConfirmDialog(this, panel, "Cambiar Contraseña para " + selectedUser, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    
        if (option == JOptionPane.OK_OPTION) {
            char[] pass1 = passField1.getPassword();
            char[] pass2 = passField2.getPassword();
            String passStr1 = new String(pass1);
            String passStr2 = new String(pass2);
    
            if (passStr1.isEmpty() || !passStr1.equals(passStr2)) {
                JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden o están vacías. Inténtelo de nuevo.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
    
            UserStorage.actualizarContrasena(selectedUser, passStr1);
            JOptionPane.showMessageDialog(this, "La contraseña para '" + selectedUser + "' ha sido cambiada exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}