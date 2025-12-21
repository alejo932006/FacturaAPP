import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
// Se eliminó el import java.util.stream.Collectors que no se usaba

public class ProveedoresGUI extends JDialog {
    private JTextField txtNit, txtNombre, txtTelefono, txtDireccion, txtEmail, txtBuscar;
    private JTable tablaProveedores;
    private DefaultTableModel modelo;
    private JButton btnGuardar, btnEliminar, btnVerCompras, btnLimpiar;
    
    // Colores modernos
    private final Color PRIMARY_COLOR = new Color(50, 100, 200);   // Azul corporativo
    private final Color SUCCESS_COLOR = new Color(40, 167, 69);    // Verde éxito
    private final Color DANGER_COLOR = new Color(220, 53, 69);     // Rojo peligro
    private final Color BACKGROUND_COLOR = new Color(245, 245, 250); // Fondo gris claro

    public ProveedoresGUI(Frame parent) {
        super(parent, "Gestión de Proveedores", true);
        setSize(1000, 650);
        setLocationRelativeTo(parent);
        initComponents();
        cargarTabla(""); // Carga inicial
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        
        // --- PANEL SUPERIOR (TÍTULO) ---
        JPanel panelHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        panelHeader.setBackground(PRIMARY_COLOR);
        JLabel lblTitulo = new JLabel("Directorio de Proveedores");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(Color.WHITE);
        panelHeader.add(lblTitulo);
        add(panelHeader, BorderLayout.NORTH);

        // --- PANEL CENTRAL (FORMULARIO Y TABLA) ---
        JPanel panelCentral = new JPanel(new BorderLayout(15, 15));
        panelCentral.setBorder(new EmptyBorder(15, 15, 15, 15));
        panelCentral.setBackground(BACKGROUND_COLOR);

        // 1. Formulario
        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBackground(Color.WHITE);
        panelForm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(15, 15, 15, 15)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Campos del formulario
        txtNit = crearTextField();
        txtNombre = crearTextField();
        txtTelefono = crearTextField();
        txtDireccion = crearTextField();
        txtEmail = crearTextField();

        agregarCampo(panelForm, "NIT / RUC:", txtNit, 0, 0, gbc);
        agregarCampo(panelForm, "Razón Social:", txtNombre, 0, 1, gbc);
        agregarCampo(panelForm, "Teléfono:", txtTelefono, 0, 2, gbc);
        agregarCampo(panelForm, "Dirección:", txtDireccion, 0, 3, gbc);
        agregarCampo(panelForm, "Email:", txtEmail, 0, 4, gbc);

        // Botones del formulario
        JPanel panelBotonesForm = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotonesForm.setBackground(Color.WHITE);
        
        btnLimpiar = new JButton("Limpiar");
        estilarBoton(btnLimpiar, new Color(108, 117, 125));
        
        btnGuardar = new JButton("Guardar Proveedor");
        estilarBoton(btnGuardar, SUCCESS_COLOR);
        btnGuardar.setFont(new Font("Segoe UI", Font.BOLD, 14));

        panelBotonesForm.add(btnLimpiar);
        panelBotonesForm.add(btnGuardar);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        panelForm.add(panelBotonesForm, gbc);

        // 2. Tabla de Proveedores
        JPanel panelTabla = new JPanel(new BorderLayout(5, 5));
        panelTabla.setBackground(BACKGROUND_COLOR);
        
        // Barra de búsqueda sobre la tabla
        JPanel panelBuscar = new JPanel(new BorderLayout(10, 0));
        panelBuscar.setBackground(BACKGROUND_COLOR);
        panelBuscar.add(new JLabel("Buscar Proveedor:"), BorderLayout.WEST);
        txtBuscar = new JTextField();
        txtBuscar.putClientProperty("JTextField.placeholderText", "Escribe nombre o NIT...");
        panelBuscar.add(txtBuscar, BorderLayout.CENTER);
        
        panelTabla.add(panelBuscar, BorderLayout.NORTH);

        String[] cols = {"NIT", "Razón Social", "Teléfono", "Dirección", "Email"};
        modelo = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaProveedores = new JTable(modelo);
        estilarTabla(tablaProveedores);
        JScrollPane scroll = new JScrollPane(tablaProveedores);
        scroll.getViewport().setBackground(Color.WHITE);
        panelTabla.add(scroll, BorderLayout.CENTER);

        // Split para dividir Formulario y Tabla
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelForm, panelTabla);
        split.setDividerLocation(350); 
        split.setResizeWeight(0);
        split.setBorder(null);
        
        panelCentral.add(split, BorderLayout.CENTER);
        add(panelCentral, BorderLayout.CENTER);

        // --- PANEL INFERIOR (ACCIONES) ---
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        panelAcciones.setBackground(Color.WHITE);
        panelAcciones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200,200,200)));

        btnEliminar = new JButton("Eliminar Seleccionado");
        estilarBoton(btnEliminar, DANGER_COLOR);
        
        btnVerCompras = new JButton("Ver Historial y Productos");
        estilarBoton(btnVerCompras, PRIMARY_COLOR);
        btnVerCompras.setIcon(UIManager.getIcon("FileView.directoryIcon"));

        panelAcciones.add(btnEliminar);
        panelAcciones.add(btnVerCompras);
        add(panelAcciones, BorderLayout.SOUTH);

        // --- EVENTOS ---
        btnGuardar.addActionListener(e -> guardar());
        btnLimpiar.addActionListener(e -> limpiar());
        btnEliminar.addActionListener(e -> eliminar());
        btnVerCompras.addActionListener(e -> verCompras());
        
        tablaProveedores.getSelectionModel().addListSelectionListener(e -> cargarDatosFormulario());
        
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                cargarTabla(txtBuscar.getText());
            }
        });
    }
    
    // --- METODOS DE AYUDA PARA UI ---
    private JTextField crearTextField() {
        JTextField tf = new JTextField(15);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return tf;
    }
    
    private void agregarCampo(JPanel p, String label, JComponent cmp, int x, int y, GridBagConstraints gbc) {
        gbc.gridx = x; gbc.gridy = y; gbc.gridwidth = 1; gbc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        p.add(lbl, gbc);
        
        gbc.gridx = x + 1; gbc.weightx = 1.0;
        p.add(cmp, gbc);
    }

    private void estilarBoton(JButton btn, Color bgColor) {
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void estilarTabla(JTable tabla) {
        tabla.setRowHeight(28);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setSelectionBackground(new Color(232, 242, 254));
        tabla.setSelectionForeground(Color.BLACK);
        
        JTableHeader header = tabla.getTableHeader();
        header.setBackground(new Color(240, 240, 240));
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setForeground(Color.DARK_GRAY);
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);
    }

    // --- LÓGICA DE DATOS ---

    private void cargarTabla(String filtro) {
        modelo.setRowCount(0);
        List<Proveedor> lista = ProveedorStorage.listarProveedores();
        for (Proveedor p : lista) {
            if (filtro.isEmpty() || 
                p.getNombre().toLowerCase().contains(filtro.toLowerCase()) || 
                p.getNit().contains(filtro)) {
                
                modelo.addRow(new Object[]{p.getNit(), p.getNombre(), p.getTelefono(), p.getDireccion(), p.getEmail()});
            }
        }
    }

    private void guardar() {
        if (txtNit.getText().isEmpty() || txtNombre.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "NIT y Razón Social son obligatorios", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Proveedor p = new Proveedor(
            txtNit.getText(), txtNombre.getText(), txtTelefono.getText(), txtDireccion.getText(), txtEmail.getText()
        );
        
        boolean existe = false;
        for(int i=0; i<modelo.getRowCount(); i++) {
            if(modelo.getValueAt(i, 0).toString().equals(p.getNit())) {
                existe = true; 
                break;
            }
        }

        boolean exito;
        if (existe) {
            exito = ProveedorStorage.actualizarProveedor(p);
        } else {
            exito = ProveedorStorage.guardarProveedor(p);
        }
        
        if(exito) {
            limpiar();
            cargarTabla("");
            JOptionPane.showMessageDialog(this, "Proveedor guardado correctamente.");
        }
    }

    private void eliminar() {
        int row = tablaProveedores.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un proveedor para eliminar.");
            return;
        }
        String nit = (String) modelo.getValueAt(row, 0);
        
        if (JOptionPane.showConfirmDialog(this, "¿Seguro que deseas eliminar este proveedor?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if(ProveedorStorage.eliminarProveedor(nit)) {
                cargarTabla("");
                limpiar();
            }
        }
    }
    
    private void cargarDatosFormulario() {
        int row = tablaProveedores.getSelectedRow();
        if (row != -1) {
            txtNit.setText((String) modelo.getValueAt(row, 0));
            txtNit.setEditable(false);
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

    // ==========================================
    //       NUEVA LÓGICA DE VER COMPRAS
    // ==========================================
    
    private void verCompras() {
        int row = tablaProveedores.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un proveedor de la lista.");
            return;
        }
        String nit = (String) modelo.getValueAt(row, 0);
        String nombreProveedor = (String) modelo.getValueAt(row, 1);
        
        new DialogoDetalleProveedor(this, nit, nombreProveedor).setVisible(true);
    }

    // Clase interna para la ventana de detalles con filtro
    class DialogoDetalleProveedor extends JDialog {
        private String nit, nombre;
        private JTable tablaInventario, tablaHistorial;
        private DefaultTableModel modeloInv, modeloHist;
        private JDateChooser fechaInicio, fechaFin;
        private JLabel lblTotalHistorial, lblTotalInventario; // Añadido label para total inventario
        private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));

        public DialogoDetalleProveedor(Dialog owner, String nit, String nombre) {
            super(owner, "Detalle de Proveedor: " + nombre, true);
            this.nit = nit;
            this.nombre = nombre;
            setSize(950, 600);
            setLocationRelativeTo(owner);
            formatoMoneda.setMaximumFractionDigits(0);
            initDialog();
        }

        private void initDialog() {
            setLayout(new BorderLayout());
            
            JTabbedPane tabs = new JTabbedPane();
            tabs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            
            // --- PESTAÑA 1: INVENTARIO ACTUAL (Stock) ---
            JPanel panelInv = new JPanel(new BorderLayout());
            String[] colsInv = {"Código", "Producto", "Stock Actual", "Costo Unit.", "Total Invertido"};
            modeloInv = new DefaultTableModel(colsInv, 0) {
                 public boolean isCellEditable(int row, int column) { return false; }
            };
            tablaInventario = new JTable(modeloInv);
            estilarTabla(tablaInventario);
            
            // Footer del inventario (Nota + Total)
            JPanel panelFooterInv = new JPanel(new BorderLayout());
            panelFooterInv.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            panelFooterInv.add(new JLabel(" * Productos activos asociados a este proveedor."), BorderLayout.WEST);
            
            lblTotalInventario = new JLabel("Total en Inventario: $0");
            lblTotalInventario.setFont(new Font("Segoe UI", Font.BOLD, 14));
            panelFooterInv.add(lblTotalInventario, BorderLayout.EAST);

            cargarInventario(); // Carga datos y actualiza el total
            
            panelInv.add(new JScrollPane(tablaInventario), BorderLayout.CENTER);
            panelInv.add(panelFooterInv, BorderLayout.SOUTH);
            
            // --- PESTAÑA 2: HISTORIAL DE COMPRAS (Gastos filtrables) ---
            JPanel panelHist = new JPanel(new BorderLayout());
            
            // Filtros
            JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT));
            panelFiltros.setBorder(BorderFactory.createTitledBorder("Filtrar Compras por Fecha"));
            fechaInicio = new JDateChooser();
            fechaFin = new JDateChooser();
            JButton btnFiltrar = new JButton("Filtrar Historial");
            estilarBoton(btnFiltrar, PRIMARY_COLOR);
            
            panelFiltros.add(new JLabel("Desde:")); panelFiltros.add(fechaInicio);
            panelFiltros.add(new JLabel("Hasta:")); panelFiltros.add(fechaFin);
            panelFiltros.add(btnFiltrar);
            
            // Tabla Historial
            String[] colsHist = {"Fecha", "Descripción / Concepto", "Método Pago", "Monto"};
            modeloHist = new DefaultTableModel(colsHist, 0);
            tablaHistorial = new JTable(modeloHist);
            estilarTabla(tablaHistorial);
            
            JPanel panelFooterHist = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            lblTotalHistorial = new JLabel("Total Compras en Periodo: $0");
            lblTotalHistorial.setFont(new Font("Segoe UI", Font.BOLD, 15));
            panelFooterHist.add(lblTotalHistorial);

            panelHist.add(panelFiltros, BorderLayout.NORTH);
            panelHist.add(new JScrollPane(tablaHistorial), BorderLayout.CENTER);
            panelHist.add(panelFooterHist, BorderLayout.SOUTH);

            tabs.addTab("Inventario Actual", null, panelInv, "Productos activos en bodega");
            tabs.addTab("Historial de Compras (Gastos)", null, panelHist, "Registro de gastos asociados a este proveedor");
            
            add(tabs, BorderLayout.CENTER);
            
            // Lógica del filtro
            btnFiltrar.addActionListener(e -> cargarHistorialCompras());
            
            // Cargar historial inicial
            cargarHistorialCompras();
        }

        private void cargarInventario() {
            modeloInv.setRowCount(0); // Limpiar tabla antes de cargar
            List<Object[]> productos = ProveedorStorage.obtenerProductosPorProveedor(nit);
            double total = 0;
            for(Object[] p : productos) {
                double subtotal = (double) p[4]; // El 5to elemento es el total invertido
                modeloInv.addRow(new Object[]{
                    p[0], p[1], p[2], 
                    formatoMoneda.format(p[3]), 
                    formatoMoneda.format(subtotal)
                });
                total += subtotal;
            }
            // AHORA SÍ USAMOS LA VARIABLE TOTAL
            lblTotalInventario.setText("Valor del Inventario Actual: " + formatoMoneda.format(total));
        }
        
        private void cargarHistorialCompras() {
            modeloHist.setRowCount(0);
            List<Gasto> todosGastos = GastoStorage.cargarGastos();
            double total = 0;
            
            LocalDate inicio = fechaInicio.getDate() != null ? 
                fechaInicio.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : LocalDate.MIN;
            LocalDate fin = fechaFin.getDate() != null ? 
                fechaFin.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : LocalDate.MAX;

            for (Gasto g : todosGastos) {
                boolean coincideProveedor = g.getDescripcion().toLowerCase().contains(nombre.toLowerCase());
                boolean enRango = !g.getFecha().isBefore(inicio) && !g.getFecha().isAfter(fin);
                
                if (coincideProveedor && enRango) {
                    modeloHist.addRow(new Object[]{
                        g.getFecha(),
                        g.getDescripcion(),
                        g.getMetodoPago(),
                        formatoMoneda.format(g.getMonto())
                    });
                    total += g.getMonto();
                }
            }
            lblTotalHistorial.setText("Total Compras en Periodo: " + formatoMoneda.format(total));
        }
    }
}