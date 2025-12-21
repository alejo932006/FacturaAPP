import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

public class ProveedoresGUI extends JDialog {
    private JTextField txtNit, txtNombre, txtTelefono, txtDireccion, txtEmail;
    private JTable tablaProveedores;
    private DefaultTableModel modelo;
    private JButton btnGuardar, btnEliminar, btnVerCompras;

    public ProveedoresGUI(Frame parent) {
        super(parent, "Gestión de Proveedores", true);
        setSize(900, 500);
        setLocationRelativeTo(parent);
        initComponents();
        cargarTabla();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // --- PANEL FORMULARIO ---
        JPanel panelForm = new JPanel(new GridLayout(6, 2, 5, 5));
        panelForm.setBorder(BorderFactory.createTitledBorder("Datos del Proveedor"));
        
        txtNit = new JTextField();
        txtNombre = new JTextField();
        txtTelefono = new JTextField();
        txtDireccion = new JTextField();
        txtEmail = new JTextField();

        panelForm.add(new JLabel("NIT / Identificación:")); panelForm.add(txtNit);
        panelForm.add(new JLabel("Razón Social / Nombre:")); panelForm.add(txtNombre);
        panelForm.add(new JLabel("Teléfono:")); panelForm.add(txtTelefono);
        panelForm.add(new JLabel("Dirección:")); panelForm.add(txtDireccion);
        panelForm.add(new JLabel("Email:")); panelForm.add(txtEmail);

        btnGuardar = new JButton("Guardar / Actualizar");
        btnGuardar.setBackground(new Color(40, 167, 69));
        btnGuardar.setForeground(Color.WHITE);
        panelForm.add(new JLabel("")); panelForm.add(btnGuardar);

        // --- PANEL TABLA ---
        String[] cols = {"NIT", "Nombre", "Teléfono", "Dirección", "Email"};
        modelo = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaProveedores = new JTable(modelo);
        JScrollPane scroll = new JScrollPane(tablaProveedores);

        // --- BOTONES INFERIORES ---
        JPanel panelBotones = new JPanel();
        btnEliminar = new JButton("Eliminar Seleccionado");
        btnVerCompras = new JButton("Ver Compras / Productos");
        
        btnVerCompras.setBackground(new Color(0, 123, 255));
        btnVerCompras.setForeground(Color.WHITE);
        btnEliminar.setBackground(new Color(220, 53, 69));
        btnEliminar.setForeground(Color.WHITE);

        panelBotones.add(btnVerCompras);
        panelBotones.add(btnEliminar);

        add(panelForm, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);

        // --- EVENTOS ---
        btnGuardar.addActionListener(e -> guardar());
        btnEliminar.addActionListener(e -> eliminar());
        btnVerCompras.addActionListener(e -> verCompras());
        
        tablaProveedores.getSelectionModel().addListSelectionListener(e -> cargarDatosFormulario());
    }

    private void cargarTabla() {
        modelo.setRowCount(0);
        for (Proveedor p : ProveedorStorage.listarProveedores()) {
            modelo.addRow(new Object[]{p.getNit(), p.getNombre(), p.getTelefono(), p.getDireccion(), p.getEmail()});
        }
    }

    private void guardar() {
        if (txtNit.getText().isEmpty() || txtNombre.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "NIT y Nombre son obligatorios");
            return;
        }
        Proveedor p = new Proveedor(
            txtNit.getText(), txtNombre.getText(), txtTelefono.getText(), txtDireccion.getText(), txtEmail.getText()
        );
        
        // Verificamos si ya existe para saber si es update o insert (lógica simple por NIT)
        boolean existe = false;
        for(int i=0; i<modelo.getRowCount(); i++) {
            if(modelo.getValueAt(i, 0).toString().equals(p.getNit())) existe = true;
        }

        if (existe) {
            if(ProveedorStorage.actualizarProveedor(p)) limpiar();
        } else {
            if(ProveedorStorage.guardarProveedor(p)) limpiar();
        }
        cargarTabla();
    }

    private void eliminar() {
        int row = tablaProveedores.getSelectedRow();
        if (row == -1) return;
        String nit = (String) modelo.getValueAt(row, 0);
        
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar proveedor?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            ProveedorStorage.eliminarProveedor(nit);
            cargarTabla();
            limpiar();
        }
    }
    
    private void cargarDatosFormulario() {
        int row = tablaProveedores.getSelectedRow();
        if (row != -1) {
            txtNit.setText((String) modelo.getValueAt(row, 0));
            txtNit.setEditable(false); // No editar llave primaria
            txtNombre.setText((String) modelo.getValueAt(row, 1));
            txtTelefono.setText((String) modelo.getValueAt(row, 2));
            txtDireccion.setText((String) modelo.getValueAt(row, 3));
            txtEmail.setText((String) modelo.getValueAt(row, 4));
        }
    }

    private void limpiar() {
        txtNit.setText(""); txtNit.setEditable(true);
        txtNombre.setText(""); txtTelefono.setText("");
        txtDireccion.setText(""); txtEmail.setText("");
        tablaProveedores.clearSelection();
    }

    private void verCompras() {
        int row = tablaProveedores.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un proveedor para ver sus productos.");
            return;
        }
        String nit = (String) modelo.getValueAt(row, 0);
        String nombre = (String) modelo.getValueAt(row, 1);
        
        List<Object[]> productos = ProveedorStorage.obtenerProductosPorProveedor(nit);
        
        JDialog dialogCompras = new JDialog(this, "Inventario de: " + nombre, true);
        dialogCompras.setSize(600, 400);
        dialogCompras.setLocationRelativeTo(this);
        
        String[] cols = {"Código", "Producto", "Cant.", "Costo Unit.", "Total Invertido"};
        DefaultTableModel modelCompras = new DefaultTableModel(cols, 0);
        
        double totalGlobal = 0;
        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));

        for(Object[] p : productos) {
            modelCompras.addRow(new Object[]{
                p[0], p[1], p[2], 
                formatoMoneda.format(p[3]), 
                formatoMoneda.format(p[4])
            });
            totalGlobal += (double) p[4];
        }
        
        JTable tablaCompras = new JTable(modelCompras);
        dialogCompras.add(new JScrollPane(tablaCompras), BorderLayout.CENTER);
        
        JLabel lblTotal = new JLabel("  Total invertido en inventario activo: " + formatoMoneda.format(totalGlobal));
        lblTotal.setFont(new Font("Arial", Font.BOLD, 14));
        lblTotal.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        dialogCompras.add(lblTotal, BorderLayout.SOUTH);
        
        dialogCompras.setVisible(true);
    }
}