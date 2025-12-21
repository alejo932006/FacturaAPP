import javax.swing.text.AbstractDocument;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CrearProductoGUI extends JDialog {
    // --- Campos del Formulario ---
    private JTextField txtCodigo, txtNombre, txtPrecio, txtCosto;
    private JSpinner spinnerCantidad;
    private JComboBox<String> comboUnidad;
    private JButton btnGuardar, btnNuevo, btnEditarStock, btnCambiarEstado;
    private JComboBox<String> comboAreaEncargada;
    private JButton btnGestionarLineas;
    private JLabel lblAreaEncargada;
    
    // --- NUEVOS CAMPOS PARA PROVEEDOR ---
    private JTextField txtNombreProveedor, txtNitProveedor;

    // --- Componentes de la Lista ---
    private JTable tablaInventario;
    private DefaultTableModel modeloTabla;
    private TableRowSorter<DefaultTableModel> sorter;

    // --- Referencias y otros ---
    private final FacturaGUI facturaGUI;
    private final Color COLOR_ERROR = new Color(255, 224, 224);
    private final Color COLOR_NORMAL = UIManager.getColor("TextField.background");
    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));

    public CrearProductoGUI(FacturaGUI facturaGUI) {
        super(facturaGUI, "Gestión de Productos (Base de Datos)", true);
        this.facturaGUI = facturaGUI;
        formatoMoneda.setMaximumFractionDigits(0);
        setSize(1050, 650); 
        setLocationRelativeTo(facturaGUI);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarProductosEnTabla();
        validarCampos();
    }

    private void initComponents() {
        JPanel panelContenedor = new JPanel(new BorderLayout());
        panelContenedor.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // === PANEL IZQUIERDO (Inventario) ===
        JPanel panelIzquierdo = new JPanel(new BorderLayout(5, 10));
        panelIzquierdo.setBorder(BorderFactory.createTitledBorder("Inventario en Base de Datos"));

        JTextField txtBuscar = new JTextField();
        JPanel panelBusqueda = new JPanel(new BorderLayout(5,0));
        panelBusqueda.add(new JLabel("Buscar: "), BorderLayout.WEST);
        panelBusqueda.add(txtBuscar, BorderLayout.CENTER);
        panelIzquierdo.add(panelBusqueda, BorderLayout.NORTH);

        String[] columnas = {"Código", "Nombre", "Stock", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaInventario = new JTable(modeloTabla);
        tablaInventario.setDefaultRenderer(Object.class, new EstadoProductoRenderer());
        sorter = new TableRowSorter<>(modeloTabla);
        
        // --- CORRECCIÓN DE ORDENAMIENTO ---
        // Le enseñamos a la columna 0 (Código) a ordenar números correctamente
        sorter.setComparator(0, (String s1, String s2) -> {
            try {
                // Intentamos convertir a número para comparar
                long n1 = Long.parseLong(s1);
                long n2 = Long.parseLong(s2);
                return Long.compare(n1, n2);
            } catch (NumberFormatException e) {
                // Si no son números (ej: "A1"), comparamos como texto normal
                return s1.compareToIgnoreCase(s2);
            }
        });
        // -----------------------------------

        tablaInventario.setRowSorter(sorter);
        panelIzquierdo.add(new JScrollPane(tablaInventario), BorderLayout.CENTER);
        
        JPanel panelAccionesTabla = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnEditarStock = new JButton("Editar Stock");
        btnCambiarEstado = new JButton("Activar/Desactivar");
        configurarBoton(btnEditarStock, "Editar la cantidad total de stock.", "stock.png");
        configurarBoton(btnCambiarEstado, "Cambia el estado Activo/Inactivo.", "toggle.png");
        panelAccionesTabla.add(btnEditarStock);
        panelAccionesTabla.add(btnCambiarEstado);
        panelIzquierdo.add(panelAccionesTabla, BorderLayout.SOUTH);

        // === PANEL DERECHO (Formulario) ===
        JPanel panelDerecho = new JPanel(new BorderLayout(10, 10));
        panelDerecho.setBorder(BorderFactory.createTitledBorder("Datos del Producto"));
        
        JPanel panelCampos = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        txtCodigo = new JTextField(15);
        txtNombre = new JTextField(15);
        txtPrecio = new JTextField(15);
        txtCosto = new JTextField(15);
        
        txtNombreProveedor = new JTextField(15);
        txtNitProveedor = new JTextField(15);

        // Filtro básico para evitar comillas simples que rompan SQL (aunque PreparedStatement lo maneja, es buena práctica en GUI)
        SymbolFilter filterSQL = new SymbolFilter("'"); 
        ((AbstractDocument) txtCodigo.getDocument()).setDocumentFilter(filterSQL);
        
        spinnerCantidad = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 999999.0, 1.0));
        spinnerCantidad.setToolTipText("Si es nuevo: Stock Inicial. Si edita: Cantidad a SUMAR al stock actual.");
        
        String[] unidades = {"Unidad(es)", "Kg", "Lb", "Litro", "Metro", "Caja", "Servicio"};
        comboUnidad = new JComboBox<>(unidades);
        
        // --- FILA 1: Código ---
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST; 
        panelCampos.add(new JLabel("Código:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.WEST; 
        panelCampos.add(txtCodigo, gbc);

        // --- FILA 2: Nombre ---
        gbc.gridy++; gbc.gridx = 0; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        panelCampos.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panelCampos.add(txtNombre, gbc);

        // --- FILA 3: Precios ---
        gbc.gridy++; gbc.gridx = 0; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        panelCampos.add(new JLabel("Precio Venta:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panelCampos.add(txtPrecio, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        panelCampos.add(new JLabel("Costo Compra:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panelCampos.add(txtCosto, gbc);

        // --- FILA 4: Cantidad y Unidad ---
        gbc.gridy++; gbc.gridx = 0; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        panelCampos.add(new JLabel("Cant. a Agregar:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panelCampos.add(spinnerCantidad, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        panelCampos.add(new JLabel("Unidad de Medida:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panelCampos.add(comboUnidad, gbc);

        // --- SEPARADOR VISUAL PARA PROVEEDOR ---
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        panelCampos.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // --- FILA 5: Proveedor ---
        gbc.gridy++; gbc.gridx = 0; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        panelCampos.add(new JLabel("Proveedor:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panelCampos.add(txtNombreProveedor, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        panelCampos.add(new JLabel("NIT Proveedor:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panelCampos.add(txtNitProveedor, gbc);


        // --- FILA 6: Área Encargada / Línea (NUEVO DISEÑO) ---
        gbc.gridy++; gbc.gridx = 0; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        lblAreaEncargada = new JLabel("Línea / Área:");
        panelCampos.add(lblAreaEncargada, gbc);

        // Panel pequeño para ComboBox + Botón [+]
        JPanel panelLinea = new JPanel(new BorderLayout(5, 0));
        comboAreaEncargada = new JComboBox<>();
        btnGestionarLineas = new JButton("+");
        btnGestionarLineas.setToolTipText("Crear o Eliminar Líneas");
        btnGestionarLineas.setPreferredSize(new Dimension(45, 25));

        panelLinea.add(comboAreaEncargada, BorderLayout.CENTER);
        panelLinea.add(btnGestionarLineas, BorderLayout.EAST);

        gbc.gridx = 1; gbc.weightx = 1.0;
        panelCampos.add(panelLinea, gbc);

        // Cargar las líneas al iniciar
        cargarLineasEnCombo();

        boolean gestionAreasHabilitada = ConfiguracionManager.isGestionPorAreasHabilitada();
        lblAreaEncargada.setVisible(gestionAreasHabilitada);
        panelLinea.setVisible(gestionAreasHabilitada);

        addCurrencyFormatting(txtPrecio);
        addCurrencyFormatting(txtCosto);
        
        JPanel panelBotonesFormulario = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnNuevo = new JButton("Limpiar / Nuevo");
        btnGuardar = new JButton("Guardar en BD");
        configurarBoton(btnNuevo, "Limpia el formulario para crear un producto nuevo.", "new.png");
        configurarBoton(btnGuardar, "Guarda el producto en la base de datos.", "save.png");
        btnGuardar.setBackground(new Color(223, 240, 216));

        panelBotonesFormulario.add(btnNuevo);
        panelBotonesFormulario.add(btnGuardar);
        
        panelDerecho.add(panelCampos, BorderLayout.CENTER);
        panelDerecho.add(panelBotonesFormulario, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, panelDerecho);
        splitPane.setDividerLocation(500);
        panelContenedor.add(splitPane, BorderLayout.CENTER);
        add(panelContenedor);

        // --- Lógica de Componentes ---
        DocumentListener validador = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { validarCampos(); }
            public void removeUpdate(DocumentEvent e) { validarCampos(); }
            public void changedUpdate(DocumentEvent e) { validarCampos(); }
        };
        txtCodigo.getDocument().addDocumentListener(validador);
        txtNombre.getDocument().addDocumentListener(validador);
        txtPrecio.getDocument().addDocumentListener(validador);
        txtCosto.getDocument().addDocumentListener(validador);
        
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { sorter.setRowFilter(RowFilter.regexFilter("(?i)" + txtBuscar.getText())); }
            public void removeUpdate(DocumentEvent e) { sorter.setRowFilter(RowFilter.regexFilter("(?i)" + txtBuscar.getText())); }
            public void changedUpdate(DocumentEvent e) { sorter.setRowFilter(RowFilter.regexFilter("(?i)" + txtBuscar.getText())); }
        });

        tablaInventario.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tablaInventario.getSelectedRow() != -1) {
                cargarProductoSeleccionadoEnFormulario();
            }
        });

        btnGuardar.addActionListener(e -> guardarProducto());
        btnNuevo.addActionListener(e -> limpiarFormulario());
        btnEditarStock.addActionListener(e -> editarStockSeleccionado());
        btnCambiarEstado.addActionListener(e -> cambiarEstadoSeleccionado());
        btnGestionarLineas.addActionListener(e -> abrirGestionLineas());
    }

    private void configurarBoton(JButton boton, String tooltip, String iconName) {
        boton.setToolTipText(tooltip);
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/img/" + iconName));
            Image scaledImg = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            boton.setIcon(new ImageIcon(scaledImg));
        } catch (Exception e) {}
    }
    
    // --- LÓGICA SQL PRINCIPAL ---
    private void guardarProducto() {
        try {
            // 1. Recoger datos del formulario
            String codigo = txtCodigo.getText().trim();
            String nombre = txtNombre.getText().trim();
            double precio = formatoMoneda.parse(txtPrecio.getText()).doubleValue();
            double costo = formatoMoneda.parse(txtCosto.getText()).doubleValue();
            double cantidadIngresada = ((Number) spinnerCantidad.getValue()).doubleValue();
            String unidad = (String) comboUnidad.getSelectedItem();
            String area = (String) comboAreaEncargada.getSelectedItem();
            if (area == null) area = "";
            String proveedor = txtNombreProveedor.getText().trim();
            String nitProv = txtNitProveedor.getText().trim();

            if (txtCodigo.isEditable()) {
                // --- MODO CREAR NUEVO (INSERT) ---
                Producto nuevoProducto = new Producto(codigo, nombre, precio, costo, cantidadIngresada, "Activo", unidad, area, proveedor, nitProv);
                
                boolean exito = ProductoStorage.guardarProducto(nuevoProducto);
                if (exito) {
                    JOptionPane.showMessageDialog(this, "Producto guardado en Base de Datos correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    cargarProductosEnTabla();
                    limpiarFormulario();
                }
                
            } else {
                // --- MODO EDITAR (UPDATE) ---
                
                // Primero buscamos el producto actual en la BD para saber su stock actual
                List<Producto> listaActual = ProductoStorage.cargarProductos();
                Optional<Producto> prodActualOpt = listaActual.stream().filter(p -> p.getCodigo().equals(codigo)).findFirst();
                
                double stockFinal = cantidadIngresada; // Si no se encuentra, asumimos lo ingresado
                String estadoActual = "Activo";

                if (prodActualOpt.isPresent()) {
                    Producto prodDB = prodActualOpt.get();
                    // Lógica: Stock Nuevo = Stock que ya tenía + Lo que escribí en el spinner
                    stockFinal = prodDB.getCantidad() + cantidadIngresada;
                    estadoActual = prodDB.getEstado();
                }

                Producto productoActualizado = new Producto(codigo, nombre, precio, costo, stockFinal, estadoActual, unidad, area, proveedor, nitProv);
                
                boolean exito = ProductoStorage.actualizarProducto(productoActualizado);
                if (exito) {
                    String mensaje = "Producto actualizado.";
                    if (cantidadIngresada > 0) {
                        mensaje += "\nSe sumaron " + cantidadIngresada + " unidades al stock.";
                    }
                    JOptionPane.showMessageDialog(this, mensaje, "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    cargarProductosEnTabla();
                    limpiarFormulario();
                }
            }
            
            // Actualizar la lista en la ventana de Facturación si está abierta
            if(facturaGUI != null) facturaGUI.refrescarListaProductos();

        } catch (ParseException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Verifica los datos numéricos.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormulario() {
        txtCodigo.setText("");
        txtNombre.setText("");
        txtPrecio.setText("");
        txtCosto.setText("");
        spinnerCantidad.setValue(0.0); 
        comboUnidad.setSelectedIndex(0);
        if (comboAreaEncargada.getItemCount() > 0) comboAreaEncargada.setSelectedIndex(0);
        txtNombreProveedor.setText("");
        txtNitProveedor.setText("");
        
        txtCodigo.setEditable(true); // Habilitar código para nuevo producto
        txtCodigo.setBackground(Color.WHITE);
        tablaInventario.clearSelection();
        txtCodigo.requestFocus();
    }
    
    private void validarCampos() {
        boolean esCodigoValido = !txtCodigo.getText().trim().isEmpty();
        boolean esNombreValido = !txtNombre.getText().trim().isEmpty();
        boolean esPrecioValido = esNumeroValido(txtPrecio, false);
        boolean esCostoValido = esNumeroValido(txtCosto, true);
        
        btnGuardar.setEnabled(esCodigoValido && esNombreValido && esPrecioValido && esCostoValido);
    }
    
    private void cargarProductoSeleccionadoEnFormulario() {
        int filaVista = tablaInventario.getSelectedRow();
        if (filaVista < 0) return;
        int filaModelo = tablaInventario.convertRowIndexToModel(filaVista);
        String codigo = modeloTabla.getValueAt(filaModelo, 0).toString();

        // Buscamos en la BD (o en la lista cargada en memoria si preferimos rapidez)
        // Para asegurar datos frescos, usamos la lista que ya cargó la tabla.
        // Pero ProductoStorage.cargarProductos() hace SELECT, así que es seguro.
        Producto productoACargar = ProductoStorage.cargarProductos().stream()
                .filter(p -> p.getCodigo().equals(codigo))
                .findFirst().orElse(null);

        if (productoACargar != null) {
            txtCodigo.setText(productoACargar.getCodigo());
            txtNombre.setText(productoACargar.getNombre());
            txtPrecio.setText(formatoMoneda.format(productoACargar.getPrecioVenta()));
            txtCosto.setText(formatoMoneda.format(productoACargar.getCosto()));
            
            // En modo edición, el spinner se pone en 0 para "Agregar Stock", no para reemplazarlo
            spinnerCantidad.setValue(0.0); 
            
            comboUnidad.setSelectedItem(productoACargar.getUnidadDeMedida());
            comboAreaEncargada.setSelectedItem(productoACargar.getAreaEncargada());
            txtNombreProveedor.setText(productoACargar.getNombreProveedor());
            txtNitProveedor.setText(productoACargar.getNitProveedor());

            txtCodigo.setEditable(false); // No se puede editar el código (es Primary Key)
            txtCodigo.setBackground(new Color(240, 240, 240));
        }
    }

    private void addCurrencyFormatting(JTextField textField) {
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                try {
                    Number valor = formatoMoneda.parse(textField.getText());
                    textField.setText(String.valueOf(valor.longValue()));
                } catch (ParseException ex) {}
            }
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    long valor = Long.parseLong(textField.getText());
                    textField.setText(formatoMoneda.format(valor));
                } catch (NumberFormatException ex) {}
            }
        });
    }

    private boolean esNumeroValido(JTextField campo, boolean puedeSerCero) {
        String texto = campo.getText().trim();
        if (texto.isEmpty()) {
            campo.setBackground(COLOR_NORMAL);
            return false;
        }
        try {
            Number valorNum = formatoMoneda.parse(texto);
            double valor = valorNum.doubleValue();
            if ((!puedeSerCero && valor <= 0) || (puedeSerCero && valor < 0)) {
                campo.setBackground(COLOR_ERROR);
                return false;
            }
            campo.setBackground(COLOR_NORMAL);
            return true;
        } catch (ParseException e) {
            try {
                double valor = Double.parseDouble(texto);
                if ((!puedeSerCero && valor <= 0) || (puedeSerCero && valor < 0)) {
                    campo.setBackground(COLOR_ERROR);
                    return false;
                }
                campo.setBackground(COLOR_NORMAL);
                return true;
            } catch (NumberFormatException e2) {
                campo.setBackground(COLOR_ERROR);
                return false;
            }
        }
    }
    
    private void cargarProductosEnTabla() {
        modeloTabla.setRowCount(0);
        // Ahora esto hace un SELECT * FROM productos
        List<Producto> productos = ProductoStorage.cargarProductos(); 
        for (Producto p : productos) {
            modeloTabla.addRow(new Object[]{p.getCodigo(), p.getNombre(), p.getCantidad(), p.getEstado()});
        }
    }

    private void editarStockSeleccionado() {
        // Validación de admin
        JPasswordField passwordField = new JPasswordField();
        int option = JOptionPane.showConfirmDialog(this, passwordField, "Ingrese la Clave Administrativa", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (option != JOptionPane.OK_OPTION) return;
        
        String enteredPassword = new String(passwordField.getPassword());
        if (!enteredPassword.equals(ConfiguracionManager.getAdminPassword())) {
            JOptionPane.showMessageDialog(this, "Clave incorrecta.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int filaSeleccionada = tablaInventario.getSelectedRow();
        if (filaSeleccionada < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int filaModelo = tablaInventario.convertRowIndexToModel(filaSeleccionada);
        String codigo = modeloTabla.getValueAt(filaModelo, 0).toString();
        String nombre = modeloTabla.getValueAt(filaModelo, 1).toString();
        Object cantidadActual = modeloTabla.getValueAt(filaModelo, 2);
        
        String nuevaCantidadStr = JOptionPane.showInputDialog(this, "Producto: " + nombre + "\nStock Actual: " + cantidadActual + "\n\nNueva cantidad TOTAL (Reemplazar):", "Editar Stock", JOptionPane.QUESTION_MESSAGE);
        if (nuevaCantidadStr == null || nuevaCantidadStr.trim().isEmpty()) return;
        
        try {
            double nuevaCantidad = Double.parseDouble(nuevaCantidadStr);
            if (nuevaCantidad < 0) throw new NumberFormatException();
            
            // Llamamos al método SQL auxiliar
            ProductoStorage.actualizarStock(codigo, nuevaCantidad);
            
            cargarProductosEnTabla();
            if(facturaGUI != null) facturaGUI.refrescarListaProductos();
            JOptionPane.showMessageDialog(this, "Stock actualizado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Número inválido.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cambiarEstadoSeleccionado() {
        int filaSeleccionada = tablaInventario.getSelectedRow();
        if (filaSeleccionada < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int filaModelo = tablaInventario.convertRowIndexToModel(filaSeleccionada);
        String codigo = modeloTabla.getValueAt(filaModelo, 0).toString();
        String estadoActual = modeloTabla.getValueAt(filaModelo, 3).toString();
        String nuevoEstado = estadoActual.equals("Activo") ? "Inactivo" : "Activo";
        
        if (JOptionPane.showConfirmDialog(this, "¿Cambiar estado a '" + nuevoEstado + "'?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            // Buscamos el producto completo para actualizarlo
            Optional<Producto> prodOpt = ProductoStorage.cargarProductos().stream().filter(p -> p.getCodigo().equals(codigo)).findFirst();
            if (prodOpt.isPresent()) {
                Producto p = prodOpt.get();
                p.setEstado(nuevoEstado);
                ProductoStorage.actualizarProducto(p);
                cargarProductosEnTabla();
                if(facturaGUI != null) facturaGUI.refrescarListaProductos();
            }
        }
   }

   // --- NUEVO MÉTODO: Cargar líneas en el ComboBox ---
   private void cargarLineasEnCombo() {
    String seleccionActual = (String) comboAreaEncargada.getSelectedItem();
    comboAreaEncargada.removeAllItems();
    comboAreaEncargada.addItem(""); // Opción vacía por defecto
    
    java.util.List<String> lineas = LineaStorage.cargarLineas();
    for (String linea : lineas) {
        comboAreaEncargada.addItem(linea);
    }
    
    if (seleccionActual != null) {
        comboAreaEncargada.setSelectedItem(seleccionActual);
    }
}

// --- NUEVO MÉTODO: Ventanita para agregar/borrar líneas ---
private void abrirGestionLineas() {
    JPanel panel = new JPanel(new BorderLayout(5, 5));
    DefaultListModel<String> modeloLista = new DefaultListModel<>();
    JList<String> listaLineas = new JList<>(modeloLista);
    
    // Cargar datos
    LineaStorage.cargarLineas().forEach(modeloLista::addElement);
    
    panel.add(new JScrollPane(listaLineas), BorderLayout.CENTER);
    
    JPanel panelBotones = new JPanel(new GridLayout(1, 2, 5, 5));
    JButton btnAdd = new JButton("Agregar");
    JButton btnDel = new JButton("Eliminar");
    panelBotones.add(btnAdd);
    panelBotones.add(btnDel);
    panel.add(panelBotones, BorderLayout.SOUTH);
    
    // Acción Agregar
    btnAdd.addActionListener(ev -> {
        String nuevaLinea = JOptionPane.showInputDialog(panel, "Nombre de la nueva línea:");
        if (nuevaLinea != null && !nuevaLinea.trim().isEmpty()) {
            if (LineaStorage.agregarLinea(nuevaLinea.trim())) {
                modeloLista.addElement(nuevaLinea.trim());
                cargarLineasEnCombo(); // Actualizar el combo de la ventana principal
            }
        }
    });
    
    // Acción Eliminar
    btnDel.addActionListener(ev -> {
        String seleccion = listaLineas.getSelectedValue();
        if (seleccion != null) {
            if (JOptionPane.showConfirmDialog(panel, "¿Eliminar la línea '" + seleccion + "'?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                if (LineaStorage.eliminarLinea(seleccion)) {
                    modeloLista.removeElement(seleccion);
                    cargarLineasEnCombo(); // Actualizar el combo de la ventana principal
                }
            }
        }
    });

    JOptionPane.showMessageDialog(this, panel, "Gestionar Líneas / Áreas", JOptionPane.PLAIN_MESSAGE);
}

    class EstadoProductoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                String estado = (String) table.getModel().getValueAt(table.convertRowIndexToModel(row), 3);
                if ("Inactivo".equals(estado)) {
                    c.setForeground(Color.GRAY);
                    c.setFont(new Font(c.getFont().getName(), Font.ITALIC, c.getFont().getSize()));
                } else {
                    c.setForeground(table.getForeground());
                    c.setFont(new Font(c.getFont().getName(), Font.PLAIN, c.getFont().getSize()));
                }
            }
            return c;
        }
    }
}