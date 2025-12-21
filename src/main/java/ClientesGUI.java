import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.AbstractDocument;
import java.awt.*;

public class ClientesGUI extends JDialog { // Cambiado de JFrame a JDialog para mejor comportamiento
    private JTable tablaClientes;
    private DefaultTableModel modelo;
    private TableRowSorter<DefaultTableModel> sorter;
    
    // --- CAMBIO CLAVE: Usamos la interfaz en lugar de FacturaGUI ---
    private ClienteSeleccionListener listener;

    // --- CAMBIO CLAVE: El constructor ahora acepta la interfaz y un 'owner' ---
    public ClientesGUI(Window owner, ClienteSeleccionListener listener) {
        super(owner, "Consultar y Gestionar Clientes", ModalityType.APPLICATION_MODAL);
        this.listener = listener;
        setSize(900, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        // ... (El resto del método initComponents no necesita cambios)
        // El código que tenías para crear la interfaz gráfica va aquí sin modificaciones.
        // --- Panel Principal con BorderLayout ---
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    
        // --- Panel de Lista de Clientes (IZQUIERDA) ---
        JPanel panelLista = new JPanel(new BorderLayout(5, 5));
        
        // Panel de Búsqueda
        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblBuscar = new JLabel("Buscar:");
        JTextField txtBuscar = new JTextField(20);
        panelBusqueda.add(lblBuscar);
        panelBusqueda.add(txtBuscar);
    
        // Tabla de Clientes
        String[] columnas = {"Nombre", "Cédula", "Dirección", "Email"};
        modelo = new DefaultTableModel(columnas, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaClientes = new JTable(modelo);
        sorter = new TableRowSorter<>(modelo);
        tablaClientes.setRowSorter(sorter);
        cargarClientesEnTabla();
        
        panelLista.add(panelBusqueda, BorderLayout.NORTH);
        panelLista.add(new JScrollPane(tablaClientes), BorderLayout.CENTER);
    
        // --- Panel de Creación de Cliente (DERECHA) ---
        JPanel panelCreacion = new JPanel(new GridBagLayout());
        panelCreacion.setBorder(BorderFactory.createTitledBorder("Crear Nuevo Cliente"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
    
        JTextField txtNuevoNombre = new JTextField(20);
        JTextField txtNuevaCedula = new JTextField(20);
        JTextField txtNuevaDireccion = new JTextField(20);
        JTextField txtNuevoEmail = new JTextField(20);
        JButton btnGuardarCliente = new JButton("Guardar Nuevo Cliente");
        btnGuardarCliente.setBackground(new Color(40, 167, 69)); // Verde
        btnGuardarCliente.setForeground(Color.WHITE);
    
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST; panelCreacion.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; panelCreacion.add(txtNuevoNombre, gbc);
        gbc.gridy++; gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST; panelCreacion.add(new JLabel("Cédula:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; panelCreacion.add(txtNuevaCedula, gbc);
        gbc.gridy++; gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST; panelCreacion.add(new JLabel("Dirección/Teléfono:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; panelCreacion.add(txtNuevaDireccion, gbc);
        gbc.gridy++; gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST; panelCreacion.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; panelCreacion.add(txtNuevoEmail, gbc);
        gbc.gridy++; gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST; panelCreacion.add(btnGuardarCliente, gbc);
        ((AbstractDocument) txtNuevoNombre.getDocument()).setDocumentFilter(new SymbolFilter("#"));
        ((AbstractDocument) txtNuevaCedula.getDocument()).setDocumentFilter(new SymbolFilter("#"));
        ((AbstractDocument) txtNuevaDireccion.getDocument()).setDocumentFilter(new SymbolFilter("#"));
    
        btnGuardarCliente.addActionListener(e -> {
            String nombre = txtNuevoNombre.getText().trim();
            String cedula = txtNuevaCedula.getText().trim();
            String direccion = txtNuevaDireccion.getText().trim();
            String email = txtNuevoEmail.getText().trim();
    
            if (nombre.isEmpty() || cedula.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre y la cédula no pueden estar vacíos.", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (ClienteStorage.clienteExiste(cedula)) {
                JOptionPane.showMessageDialog(this, "Ya existe un cliente con esa cédula.", "Cliente Duplicado", JOptionPane.ERROR_MESSAGE);
                return;
            }
    
            Cliente nuevoCliente = new Cliente(nombre, cedula, direccion, email); 
            ClienteStorage.guardarCliente(nuevoCliente);
            
            cargarClientesEnTabla();
            txtNuevoNombre.setText("");
            txtNuevaCedula.setText("");
            txtNuevaDireccion.setText("");
            txtNuevoEmail.setText("");
            JOptionPane.showMessageDialog(this, "Cliente guardado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        });
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelLista, panelCreacion);
        splitPane.setDividerLocation(550);
        panelPrincipal.add(splitPane, BorderLayout.CENTER);
    
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCargarCliente = new JButton("Cargar Cliente Seleccionado");
        JButton btnEliminarCliente = new JButton("Eliminar Cliente Seleccionado");
        btnEliminarCliente.setBackground(new Color(220, 53, 69));
        btnEliminarCliente.setForeground(Color.WHITE);
        panelBoton.add(btnCargarCliente);
        panelBoton.add(btnEliminarCliente);
        
        panelPrincipal.add(panelBoton, BorderLayout.SOUTH);
    
        btnCargarCliente.addActionListener(e -> cargarClienteSeleccionado());
        btnEliminarCliente.addActionListener(e -> eliminarClienteSeleccionado());
    
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrarTabla(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrarTabla(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrarTabla(); }
            
            private void filtrarTabla() {
                String texto = txtBuscar.getText();
                if (texto.trim().length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto));
                }
            }
        });
        
        add(panelPrincipal);
        setSize(900, 500);
    }
    
    private void cargarClientesEnTabla() {
        modelo.setRowCount(0);
        ClienteStorage.cargarClientes().forEach(c -> {
            // Agregamos c.getEmail() al final
            modelo.addRow(new Object[]{c.getNombre(), c.getCedula(), c.getDireccion(), c.getEmail()});
        });
    }

    private void cargarClienteSeleccionado() {
        int filaVista = tablaClientes.getSelectedRow();
        if (filaVista >= 0) {
            int filaModelo = tablaClientes.convertRowIndexToModel(filaVista);
            String nombre = modelo.getValueAt(filaModelo, 0).toString();
            String cedula = modelo.getValueAt(filaModelo, 1).toString();
            String direccion = modelo.getValueAt(filaModelo, 2).toString();
    
            // LEER EL EMAIL (Manejar si es nulo para evitar errores)
            Object emailObj = modelo.getValueAt(filaModelo, 3);
            String email = (emailObj != null) ? emailObj.toString() : "";
    
            if (listener != null) {
                // AHORA PASAMOS EL EMAIL
                listener.setDatosCliente(nombre, cedula, direccion, email);
            }
            dispose(); 
        } else {
            JOptionPane.showMessageDialog(this, "Selecciona un cliente de la tabla.");
        }
    }

    private void eliminarClienteSeleccionado() {
        // ... (El método eliminarClienteSeleccionado no necesita cambios)
        int filaVista = tablaClientes.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un cliente para eliminar.", "Ningún cliente seleccionado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JPasswordField passwordField = new JPasswordField();
        int option = JOptionPane.showConfirmDialog(
            this,
            passwordField,
            "Ingrese la Clave Administrativa para Eliminar",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (option == JOptionPane.OK_OPTION) {
            String enteredPassword = new String(passwordField.getPassword());
            String correctPassword = ConfiguracionManager.getAdminPassword();

            if (!enteredPassword.equals(correctPassword)) {
                JOptionPane.showMessageDialog(this, "Clave incorrecta. Acción denegada.", "Error de Autenticación", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            return;
        }
        int filaModelo = tablaClientes.convertRowIndexToModel(filaVista);
        String nombre = modelo.getValueAt(filaModelo, 0).toString();
        String cedula = modelo.getValueAt(filaModelo, 1).toString();

        int respuesta = JOptionPane.showConfirmDialog(this,
            "¿Estás seguro de que deseas eliminar al cliente '" + nombre + "'?\nEsta acción es permanente.",
            "Confirmar Eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (respuesta == JOptionPane.YES_OPTION) {
            ClienteStorage.eliminarCliente(cedula);
            cargarClientesEnTabla();
            JOptionPane.showMessageDialog(this, "Cliente eliminado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}