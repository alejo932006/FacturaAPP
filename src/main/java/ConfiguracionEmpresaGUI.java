import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ConfiguracionEmpresaGUI extends JDialog {

    private JTextField txtRazonSocial, txtNit, txtTelefono;
    private JComboBox<String> comboEtiquetaDetalle;
    private FacturaGUI owner; // Referencia a la ventana principal
    private JCheckBox chkHabilitarAreas;

    public ConfiguracionEmpresaGUI(FacturaGUI owner) {
        super(owner, "Configuración de la Empresa", true);
        this.owner = owner;
        setSize(850, 280); // Tamaño ajustado
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        cargarDatosActuales();
    }

    private void initComponents() {
        JPanel panelCampos = new JPanel(new GridBagLayout());
        panelCampos.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
    
        txtRazonSocial = new JTextField(20);
        txtNit = new JTextField(20);
        txtTelefono = new JTextField(20);
        comboEtiquetaDetalle = new JComboBox<>(new String[]{"IMEI", "Detalle", "Serial", "Código Único"});
    
        gbc.anchor = GridBagConstraints.EAST; // Alinea las etiquetas a la derecha
        gbc.weightx = 0; // Para que las etiquetas no se estiren

        gbc.gridx = 0; gbc.gridy = 0; panelCampos.add(new JLabel("Razón Social:"), gbc);
        gbc.gridy++; panelCampos.add(new JLabel("NIT:"), gbc);
        gbc.gridy++; panelCampos.add(new JLabel("Teléfono:"), gbc);
        gbc.gridy++; panelCampos.add(new JLabel("Etiqueta para Detalle:"), gbc);

        chkHabilitarAreas = new JCheckBox("Habilitar gestión y reportes por áreas");
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; // Ocupa ambas columnas
        panelCampos.add(chkHabilitarAreas, gbc);

        gbc.anchor = GridBagConstraints.WEST; // Alinea los campos a la izquierda
        gbc.weightx = 1.0; // Para que los campos se estiren

        gbc.gridx = 1; gbc.gridy = 0; panelCampos.add(txtRazonSocial, gbc);
        gbc.gridy++; panelCampos.add(txtNit, gbc);
        gbc.gridy++; panelCampos.add(txtTelefono, gbc);
        gbc.gridy++; panelCampos.add(comboEtiquetaDetalle, gbc);
    
        // --- PANEL DE BOTONES (VERSIÓN FINAL Y CORRECTA) ---
        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new BoxLayout(panelBotones, BoxLayout.X_AXIS));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    
        // --- Creación de todos los botones ---
        JButton btnLimpiarDatos = new JButton("Limpiar Base de Datos");
        btnLimpiarDatos.setBackground(new Color(220, 53, 69));
        btnLimpiarDatos.setForeground(Color.WHITE);
    
        JButton btnCambiarLogo = new JButton("Cambiar Logo...");
        JButton btnEditarGarantia = new JButton("Editar Garantía");
        JButton btnCambiarClaveAdmin = new JButton("Cambiar Pass Maestro");
        JButton btnGuardar = new JButton("Guardar Cambios");
    
        // --- Añadir componentes al panel en el orden correcto ---
        panelBotones.add(btnLimpiarDatos); // Botón de limpiar a la izquierda
        panelBotones.add(Box.createHorizontalGlue()); // Espacio flexible
        panelBotones.add(btnCambiarLogo);
        panelBotones.add(Box.createRigidArea(new Dimension(10, 0))); // Espacio fijo
        panelBotones.add(btnEditarGarantia);
        panelBotones.add(Box.createRigidArea(new Dimension(10, 0)));
        panelBotones.add(btnCambiarClaveAdmin); // <-- Añadimos el nuevo botón
        panelBotones.add(Box.createRigidArea(new Dimension(10, 0))); // Espacio fijo
        panelBotones.add(btnGuardar);
    
        add(panelCampos, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);
    
        // --- LÓGICA DE LOS BOTONES ---
        btnGuardar.addActionListener(e -> guardarCambios());
        btnCambiarLogo.addActionListener(e -> cambiarLogo());
        btnEditarGarantia.addActionListener(e -> new EditarGarantiaGUI(this).setVisible(true));
        btnCambiarClaveAdmin.addActionListener(e -> {
            // 1. Primero, pedimos la clave actual para verificar el permiso.
            JPasswordField passFieldActual = new JPasswordField();
            int optionActual = JOptionPane.showConfirmDialog(this, passFieldActual, "Ingrese la Clave Administrativa ACTUAL", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        
            if (optionActual != JOptionPane.OK_OPTION) {
                return; // El usuario canceló
            }
        
            String claveActualIngresada = new String(passFieldActual.getPassword());
            String claveCorrecta = ConfiguracionManager.getAdminPassword();
        
            // 2. Verificamos si la clave actual es correcta.
            if (!claveActualIngresada.equals(claveCorrecta)) {
                JOptionPane.showMessageDialog(this, "La clave actual es incorrecta. Acceso denegado.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        
            // 3. Si la clave es correcta, pedimos la nueva clave (dos veces).
            JPanel panelNuevaClave = new JPanel(new GridLayout(2, 2, 5, 5));
            JPasswordField passFieldNueva1 = new JPasswordField(15);
            JPasswordField passFieldNueva2 = new JPasswordField(15);
            panelNuevaClave.add(new JLabel("Nueva Clave:"));
            panelNuevaClave.add(passFieldNueva1);
            panelNuevaClave.add(new JLabel("Confirmar Nueva Clave:"));
            panelNuevaClave.add(passFieldNueva2);
        
            int optionNueva = JOptionPane.showConfirmDialog(this, panelNuevaClave, "Establecer Nueva Clave Administrativa", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
            if (optionNueva == JOptionPane.OK_OPTION) {
                String nuevaClave1 = new String(passFieldNueva1.getPassword());
                String nuevaClave2 = new String(passFieldNueva2.getPassword());
        
                if (nuevaClave1.isEmpty() || !nuevaClave1.equals(nuevaClave2)) {
                    JOptionPane.showMessageDialog(this, "Las nuevas claves no coinciden o están vacías. La clave no fue cambiada.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
        
                // 4. Si todo es correcto, guardamos la nueva clave.
                ConfiguracionManager.saveAdminPassword(nuevaClave1);
                JOptionPane.showMessageDialog(this, "La clave administrativa ha sido cambiada exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        btnLimpiarDatos.addActionListener(e -> {
            int respuesta = JOptionPane.showConfirmDialog(
                this,
                "Esta acción borrará TODOS los productos, clientes, facturas y configuraciones.\nLa aplicación se cerrará. Esta acción es IRREVERSIBLE.\n\n¿Está absolutamente seguro?",
                "🚨 ADVERTENCIA MÁXIMA 🚨",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE
            );

            if (respuesta == JOptionPane.YES_OPTION) {
                String confirmacion = JOptionPane.showInputDialog(
                    this,
                    "Para confirmar, por favor escriba la frase \"BORRAR TODO\" en mayúsculas.",
                    "Confirmación Final",
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (confirmacion != null && confirmacion.equals("BORRAR TODO")) {
                    ConfiguracionManager.limpiarTodaLaBaseDeDatos();
                    JOptionPane.showMessageDialog(this, "Todos los datos han sido eliminados. La aplicación se cerrará ahora.", "Proceso Completado", JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                } else {
                    JOptionPane.showMessageDialog(this, "La frase de confirmación no es correcta. No se ha borrado nada.", "Operación Cancelada", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
    }

    private void cargarDatosActuales() {
        Empresa empresa = ConfiguracionManager.cargarEmpresa();
        txtRazonSocial.setText(empresa.getRazonSocial());
        txtNit.setText(empresa.getNit());
        txtTelefono.setText(empresa.getTelefono());
        comboEtiquetaDetalle.setSelectedItem(ConfiguracionManager.cargarEtiquetaDetalle());
        chkHabilitarAreas.setSelected(ConfiguracionManager.isGestionPorAreasHabilitada());
    }

    private void cambiarLogo() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Imágenes (PNG, JPG)", "png", "jpg", "jpeg");
        chooser.setFileFilter(filter);
        
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File archivoSeleccionado = chooser.getSelectedFile();
            File destino = PathsHelper.getLogoFile();
            try {
                Files.copy(archivoSeleccionado.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(this, "Logo actualizado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                owner.actualizarDatosEmpresa();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al guardar el logo.", "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void guardarCambios() {
        Empresa nuevaEmpresa = new Empresa(
            txtRazonSocial.getText(),
            txtNit.getText(),
            txtTelefono.getText()
        );
        ConfiguracionManager.guardarEmpresa(nuevaEmpresa);
        ConfiguracionManager.guardarEtiquetaDetalle((String) comboEtiquetaDetalle.getSelectedItem());
        ConfiguracionManager.guardarGestionPorAreas(chkHabilitarAreas.isSelected());

        JOptionPane.showMessageDialog(this, "Datos guardados.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        owner.actualizarDatosEmpresa();
        dispose();
    }
}