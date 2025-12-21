import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.swing.text.AbstractDocument;

public class FacturaGUI extends JFrame implements ClienteSeleccionListener {

    // --- Componentes de la clase ---
    private JTextField txtNombre, txtCedula, txtDireccion, txtEmail, txtCantidad, txtPrecio;
    private JTextField txtBuscarCodigo;
    private JComboBox<Producto> comboProductos;
    private JLabel lblTotal, lblFecha, lblHoraActual, lblNit, lblRazon, lblTelefono, lblNumeroFactura;
    private List<Producto> listaProductos;
    private Factura factura;
    private Empresa empresa; // Ahora se carga dinámicamente
    private JList<String> listaFacturaVisual;
    private DefaultListModel<String> modeloListaFactura;
    private double descuentoParaSiguienteProducto = 0.0;
    private JRadioButton radioContado, radioCredito;
    private JButton btnAplicarDescuento;
    private final NumberFormat formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    JMenuBar barraMenu = new JMenuBar();
        JMenu menuOpciones = new JMenu("Opciones");
            JMenuItem itemCrearProducto = new JMenuItem("Gestionar Productos");
            JMenuItem itemConsultarFacturas = new JMenuItem("Consultar Facturas");
            JMenuItem itemConsultarCliente = new JMenuItem("Gestionar Clientes");
            JMenuItem itemDevoluciones = new JMenuItem("Registrar Devolución");
            JMenuItem itemConsultarDevoluciones = new JMenuItem("Consultar Devoluciones");
            JMenuItem itemReporteVentas = new JMenuItem("Reporte de Ventas");
            JMenuItem itemGestionUsuarios = new JMenuItem("Gestionar Usuarios");
            JMenuItem itemConfiguracion = new JMenuItem("Configurar Empresa");
        JMenu menuInventario = new JMenu("Inventario"); 
            JMenuItem itemVerInventario = new JMenuItem("Ver Inventario");
            JMenuItem itemGenerarReporte = new JMenuItem("Generar Orden/Reporte");
            JMenuItem itemConsultarOrdenes = new JMenuItem("Consultar Órdenes/Reportes");
        JMenu menuArchivo = new JMenu("Archivo");
            JMenuItem itemCrearBackup = new JMenuItem("Crear Copia de Seguridad...");
            JMenuItem itemRestaurarBackup = new JMenuItem("Restaurar Copia de Seguridad...");
            JMenuItem itemMantenimientoDatos = new JMenuItem("Mantenimiento de Datos (Borrado)");

    // --- CAMBIOS PARA ETIQUETA PERSONALIZABLE ---
    private JTextField txtDetalle;
    private JLabel lblDetalle;
    
    // --- Variables para actualizar el logo dinámicamente ---
    private JPanel panelContenidoCliente;
    private JLabel lblImagen;
    private JLabel lblNombreClienteTarjeta, lblCedulaClienteTarjeta;

    public FacturaGUI() {
        setTitle("Sistema de Facturación");
        setSize(1400, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents();
        setVisible(true);
        aplicarPermisos();
        actualizarVistaFactura(); // Dibuja el encabezado por primera vez
        aplicarPermisos(); 
    }
    
    public FacturaGUI(Frame owner) {
        this(); // Llama al constructor principal
        setLocationRelativeTo(owner);
    }

    private void initComponents() {

        // --- Barra de Menú (con todas las opciones) ---
 
        barraMenu.setBackground(new Color(230, 240, 255)); // <-- CAMBIO DE COLOR

        // --- INICIO DEL CÓDIGO NUEVO ---
        itemCrearBackup = new JMenuItem("Crear Copia de Seguridad...");
        itemCrearBackup.addActionListener(e -> BackupManager.crearCopiaDeSeguridad(this));
        itemRestaurarBackup = new JMenuItem("Restaurar Copia de Seguridad...");
        itemRestaurarBackup.addActionListener(e -> BackupManager.restaurarCopiaDeSeguridad(this));
        itemMantenimientoDatos.setForeground(Color.RED);
        itemMantenimientoDatos.addActionListener(e -> {
            // Seguridad Extra: Solo permitir al ADMIN entrar aquí
            if (UserSession.esAdmin()) {
                new VaciarDatosGUI().setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Solo el administrador puede acceder a esta función.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
            }
        });
        menuArchivo.add(itemMantenimientoDatos);
        menuArchivo.add(itemCrearBackup);
        menuArchivo.add(itemRestaurarBackup);
        // --- FIN DEL CÓDIGO NUEVO ---

        itemCrearProducto = new JMenuItem("Gestionar Productos");
        itemCrearProducto.addActionListener(e -> new CrearProductoGUI(this).setVisible(true));
        menuOpciones.add(itemCrearProducto);
    
        itemConsultarFacturas = new JMenuItem("Consultar Facturas");
        itemConsultarFacturas.addActionListener(e -> new ConsultarFacturasGUI(this));
        menuOpciones.add(itemConsultarFacturas);
    
        itemConsultarCliente = new JMenuItem("Gestionar Clientes");
        itemConsultarCliente.addActionListener(e -> new ClientesGUI(this, this));
        menuOpciones.add(itemConsultarCliente);

        menuOpciones.addSeparator();
    
        itemDevoluciones = new JMenuItem("Registrar Devolución");
        itemDevoluciones.addActionListener(e -> new DevolucionesGUI(this).setVisible(true));
        menuOpciones.add(itemDevoluciones);

        itemConsultarDevoluciones = new JMenuItem("Consultar Devoluciones");
        itemConsultarDevoluciones.addActionListener(e -> new ConsultarDevolucionesGUI(this).setVisible(true));
        menuOpciones.add(itemConsultarDevoluciones);

        menuOpciones.addSeparator();
    
        itemReporteVentas = new JMenuItem("Reporte de Ventas");
        itemReporteVentas.addActionListener(e -> new ReporteVentasGUI(this).setVisible(true));
        menuOpciones.add(itemReporteVentas);

        menuOpciones.addSeparator();
        
        itemGestionUsuarios = new JMenuItem("Gestionar Usuarios");
        itemGestionUsuarios.addActionListener(e -> new GestionUsuariosGUI(this).setVisible(true));
        menuOpciones.add(itemGestionUsuarios);
        
        itemConfiguracion = new JMenuItem("Configurar Empresa");
        itemConfiguracion.addActionListener(e -> new ConfiguracionEmpresaGUI(this).setVisible(true));
        menuOpciones.add(itemConfiguracion);
        
        itemVerInventario = new JMenuItem("Ver Inventario");
        itemVerInventario.addActionListener(e -> new SeleccionarProductoGUI(this).setVisible(true));
        menuInventario.add(itemVerInventario);

        menuOpciones.addSeparator(); // Añade una línea visual de separación

        itemGenerarReporte = new JMenuItem("Generar Orden/Reporte");
        itemGenerarReporte.addActionListener(e -> new GeneradorReportesGUI(this, null).setVisible(true));
        menuOpciones.add(itemGenerarReporte);

        itemConsultarOrdenes = new JMenuItem("Consultar Órdenes/Reportes");
        itemConsultarOrdenes.addActionListener(e -> new ConsultarOrdenesGUI(this));
        menuOpciones.add(itemConsultarOrdenes);

        
        barraMenu.add(menuOpciones);
        barraMenu.add(menuInventario);
        barraMenu.add(menuArchivo);
        setJMenuBar(barraMenu);
        
        // --- Panel Empresa ---
        JPanel panelEmpresa = new JPanel(new GridLayout(2, 3, 10, 5));
        panelEmpresa.setBackground(new Color(240, 245, 250)); // Un azul muy claro
        TitledBorder bordeEmpresa = BorderFactory.createTitledBorder("Información de la Empresa y Factura");
        bordeEmpresa.setTitleColor(new Color(0, 71, 171)); // <-- CAMBIO DE COLOR (Azul oscuro)
        panelEmpresa.setBorder(bordeEmpresa);
    
        lblFecha = new JLabel("Fecha: " + java.time.LocalDate.now().toString());
        lblHoraActual = new JLabel("Hora: --:--:--");
        lblRazon = new JLabel("Razón Social:");
        lblNit = new JLabel("NIT:");
        lblTelefono = new JLabel("Teléfono:");
        lblNumeroFactura = new JLabel("Factura Nº: ---");
        lblNumeroFactura.setFont(new Font("Arial", Font.BOLD, 14));
        lblNumeroFactura.setForeground(Color.BLUE);
    
        panelEmpresa.add(lblRazon);
        panelEmpresa.add(lblNit);
        panelEmpresa.add(lblTelefono);
        panelEmpresa.add(lblFecha);
        panelEmpresa.add(lblHoraActual);
        panelEmpresa.add(lblNumeroFactura);
        Timer timer = new Timer(1000, e -> lblHoraActual.setText("Hora: " + java.time.LocalTime.now().withNano(0).toString()));
        timer.start();
    
        // --- Panel Cliente ---
        JPanel panelCliente = new JPanel();
        TitledBorder bordeCliente = BorderFactory.createTitledBorder("Datos del Cliente");
        panelCliente.setBackground(new Color(248, 249, 250));
        bordeCliente.setTitleColor(new Color(0, 71, 171)); // <-- CAMBIO DE COLOR (Azul oscuro)
        panelCliente.setLayout(new BoxLayout(panelCliente, BoxLayout.Y_AXIS));
        panelCliente.setBorder(bordeCliente);
    
        panelContenidoCliente = new JPanel(new BorderLayout(10, 0)); // Añadimos espacio horizontal

        // --- TARJETA DE CLIENTE (IZQUIERDA) ---
        JPanel panelTarjetaCliente = new JPanel();
        panelTarjetaCliente.setOpaque(false);
        panelTarjetaCliente.setLayout(new BoxLayout(panelTarjetaCliente, BoxLayout.Y_AXIS));
        panelTarjetaCliente.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        lblNombreClienteTarjeta = new JLabel("Cliente Ocasional");
        lblNombreClienteTarjeta.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblCedulaClienteTarjeta = new JLabel("C.C. 0");
        lblCedulaClienteTarjeta.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblCedulaClienteTarjeta.setForeground(Color.GRAY);

        panelTarjetaCliente.add(lblNombreClienteTarjeta);
        panelTarjetaCliente.add(lblCedulaClienteTarjeta);
        
        JPanel panelCampos = new JPanel();
        panelCampos.setOpaque(false);
        panelCampos.setLayout(new BoxLayout(panelCampos, BoxLayout.Y_AXIS));
    
        JPanel filaNombre = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JLabel lblNombre = new JLabel("Nombre:");
        lblNombre.setPreferredSize(new Dimension(150, 25));
        txtNombre = new JTextField();
        txtNombre.setPreferredSize(new Dimension(300, 25));
        SymbolFilter filter = new SymbolFilter("#");
        ((AbstractDocument) txtNombre.getDocument()).setDocumentFilter(filter); 
        filaNombre.add(lblNombre);
        filaNombre.add(txtNombre);

        // Boton buscar cliente 
        JButton btnBuscarCliente = new JButton("B");
        btnBuscarCliente.setToolTipText("Buscar y Cargar un Cliente Existente");
        btnBuscarCliente.setFont(new Font("Arial", Font.BOLD, 14));
        btnBuscarCliente.setMargin(new Insets(0, 0, 0, 0));
        btnBuscarCliente.setPreferredSize(new Dimension(25, 25));
        btnBuscarCliente.addActionListener(e -> new ClientesGUI(this, this));
        filaNombre.add(lblNombre);
        filaNombre.add(txtNombre);
        filaNombre.add(btnBuscarCliente);

        JButton btnLimpiarCliente = new JButton("Limpiar campos cliente");
        btnLimpiarCliente.setToolTipText("Limpiar Datos del Cliente");
        btnLimpiarCliente.setFont(new Font("Arial", Font.PLAIN, 14));
        btnLimpiarCliente.setMargin(new Insets(0, 0, 0, 0));
        btnLimpiarCliente.setPreferredSize(new Dimension(150, 25));
        btnLimpiarCliente.addActionListener(e -> limpiarDatosCliente()); // Conecta con el nuevo método
        filaNombre.add(btnLimpiarCliente); // Añade el botón al panel

        JPanel filaCedula = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JLabel lblCedula = new JLabel("Cédula:");
        lblCedula.setPreferredSize(new Dimension(150, 25));
        txtCedula = new JTextField();
        txtCedula.setPreferredSize(new Dimension(300, 25));
        filaCedula.add(lblCedula);
        filaCedula.add(txtCedula);
        ((AbstractDocument) txtCedula.getDocument()).setDocumentFilter(new SymbolFilter("#"));
        JPanel filaDireccion = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JLabel lblDireccion = new JLabel("Dirección y telefono:");
        lblDireccion.setPreferredSize(new Dimension(150, 25));
        txtDireccion = new JTextField();
        txtDireccion.setPreferredSize(new Dimension(300, 25));
        filaDireccion.add(lblDireccion);
        filaDireccion.add(txtDireccion);
        ((AbstractDocument) txtDireccion.getDocument()).setDocumentFilter(new SymbolFilter("#"));
        JPanel filaEmail = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JLabel lblEmail = new JLabel("Email:");
        lblEmail.setPreferredSize(new Dimension(150, 25));
        txtEmail = new JTextField();
        txtEmail.setPreferredSize(new Dimension(300, 25));
        filaEmail.add(lblEmail);
        filaEmail.add(txtEmail);

        panelCampos.add(filaEmail);
        panelCampos.add(filaNombre);
        panelCampos.add(filaCedula);
        panelCampos.add(filaDireccion);
        
        panelContenidoCliente.add(panelTarjetaCliente, BorderLayout.WEST); // Tarjeta a la izquierda
        panelContenidoCliente.add(panelCampos, BorderLayout.CENTER);      // Campos en el centro
        panelCliente.add(panelContenidoCliente);
        panelCliente.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelCliente.getPreferredSize().height));
    
        // --- Panel Producto (con etiqueta personalizable) ---
        JPanel panelProducto = new JPanel(new GridBagLayout());
        TitledBorder bordeProducto = BorderFactory.createTitledBorder("Agregar Producto");
        bordeProducto.setTitleColor(new Color(0, 71, 171)); // <-- CAMBIO DE COLOR (Azul oscuro)
        panelProducto.setBackground(new Color(240, 245, 250));
        panelProducto.setBorder(bordeProducto);
        GridBagConstraints gbcProducto = new GridBagConstraints();
        gbcProducto.insets = new Insets(5, 5, 5, 5);
        gbcProducto.anchor = GridBagConstraints.WEST;
    
        
        gbcProducto.gridx = 0; gbcProducto.gridy = 0; panelProducto.add(new JLabel("Buscar por Código (Enter):"), gbcProducto);
        gbcProducto.gridx = 1; gbcProducto.gridwidth = 3; txtBuscarCodigo = new JTextField(); txtBuscarCodigo.setPreferredSize(new Dimension(150, 25)); panelProducto.add(txtBuscarCodigo, gbcProducto);
        gbcProducto.gridwidth = 1; 

        gbcProducto.gridy = 1; gbcProducto.gridx = 0; panelProducto.add(new JLabel("Producto:"), gbcProducto);
        gbcProducto.gridx = 1; comboProductos = new JComboBox<>(); comboProductos.setPreferredSize(new Dimension(200, 25)); refrescarListaProductos(); panelProducto.add(comboProductos, gbcProducto);

      
        JButton btnBuscarProducto = new JButton("B");
        btnBuscarProducto.setToolTipText("Buscar y Cargar un Producto del Inventario");
        btnBuscarProducto.setFont(new Font("Arial", Font.BOLD, 14));
        btnBuscarProducto.setMargin(new Insets(0, 0, 0, 0));
        btnBuscarProducto.setPreferredSize(new Dimension(25, 25));
        btnBuscarProducto.addActionListener(e -> new SeleccionarProductoGUI(this).setVisible(true));

     
        gbcProducto.gridx = 2;
        panelProducto.add(btnBuscarProducto, gbcProducto);

       
        gbcProducto.gridx = 3; panelProducto.add(new JLabel("Cantidad:"), gbcProducto);
        gbcProducto.gridx = 4; txtCantidad = new JTextField("1", 5); panelProducto.add(txtCantidad, gbcProducto);
        gbcProducto.gridx = 5; panelProducto.add(new JLabel("Precio:"), gbcProducto);
        gbcProducto.gridx = 6; txtPrecio = new JTextField(8); txtPrecio.setEditable(false); panelProducto.add(txtPrecio, gbcProducto);
        gbcProducto.gridx = 7; lblDetalle = new JLabel("IMEI:"); panelProducto.add(lblDetalle, gbcProducto);
        gbcProducto.gridx = 8; txtDetalle = new JTextField(15); panelProducto.add(txtDetalle, gbcProducto);
        gbcProducto.gridx = 9; 
        ((AbstractDocument) txtDetalle.getDocument()).setDocumentFilter(new SymbolFilter("#"));
        ((AbstractDocument) txtCantidad.getDocument()).setDocumentFilter(new SymbolFilter("#"));
        ((AbstractDocument) txtPrecio.getDocument()).setDocumentFilter(new SymbolFilter("#"));
        

        btnAplicarDescuento = new JButton("Descuento");
        gbcProducto.gridx = 9;
        panelProducto.add(btnAplicarDescuento, gbcProducto);
        
        JButton btnAgregar = new JButton("Agregar a Factura");
        btnAgregar.setBackground(new Color(66, 133, 244));
        btnAgregar.setForeground(Color.WHITE);
        gbcProducto.gridx = 10; // La posición de este botón se corre
        panelProducto.add(btnAgregar, gbcProducto);
        panelProducto.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelProducto.getPreferredSize().height));
    
        // --- Lógica de los componentes de producto ---
        txtBuscarCodigo.addActionListener(e -> {
            String codigoBuscado = txtBuscarCodigo.getText().trim();
            if (codigoBuscado.isEmpty()) return;
            
            // Busca el producto en la lista cargada
            Optional<Producto> productoEncontrado = listaProductos.stream()
                .filter(p -> p.getCodigo().equalsIgnoreCase(codigoBuscado))
                .findFirst();
            
            if (productoEncontrado.isPresent()) {
                // Si lo encuentra, lo selecciona en el ComboBox
                comboProductos.setSelectedItem(productoEncontrado.get());
                txtBuscarCodigo.setText(""); // Limpia el campo de búsqueda
                txtCantidad.requestFocusInWindow(); // Pone el cursor en la cantidad
            } else {
                // Si no lo encuentra, muestra un aviso
                JOptionPane.showMessageDialog(this, "No se encontró ningún producto con ese código.", "Búsqueda Fallida", JOptionPane.WARNING_MESSAGE);
                txtBuscarCodigo.selectAll();
            }
        });

        comboProductos.addActionListener(e -> {
            Producto seleccionado = (Producto) comboProductos.getSelectedItem();
            if (seleccionado != null) {
                NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
                formatoMoneda.setMaximumFractionDigits(0);
                txtPrecio.setText(formatoMoneda.format(seleccionado.getPrecioVenta()));
            } else {
                txtPrecio.setText("");
            }
        });

        btnAgregar.addActionListener(e -> agregarProducto());

        btnAplicarDescuento.addActionListener(e -> {
            if (descuentoParaSiguienteProducto > 0) {
                // Si ya hay un descuento, la acción es cancelarlo
                int respuesta = JOptionPane.showConfirmDialog(this,
                    "¿Desea cancelar el descuento activo de " + formatoPesos.format(descuentoParaSiguienteProducto) + "?",
                    "Cancelar Descuento",
                    JOptionPane.YES_NO_OPTION);
                if (respuesta == JOptionPane.YES_OPTION) {
                    descuentoParaSiguienteProducto = 0.0;
                    actualizarEstadoBotonDescuento(); // Actualiza la apariencia del botón
                }
            } else {
                // Si no hay descuento, la acción es agregarlo
                String descuentoStr = JOptionPane.showInputDialog(this, "Ingrese el valor del descuento (ej: 5000):", "Aplicar Descuento", JOptionPane.QUESTION_MESSAGE);
                if (descuentoStr != null && !descuentoStr.trim().isEmpty()) {
                    try {
                        descuentoParaSiguienteProducto = Double.parseDouble(descuentoStr);
                        actualizarEstadoBotonDescuento(); // Actualiza la apariencia del botón
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Por favor, ingrese un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
                        descuentoParaSiguienteProducto = 0.0;
                    }
                }
            }
        });

    
        // --- Área de factura y panel inferior ---
        modeloListaFactura = new DefaultListModel<>();
        listaFacturaVisual = new JList<>(modeloListaFactura);
        listaFacturaVisual.setFont(new Font("Monospaced", Font.PLAIN, 14));
        listaFacturaVisual.setBackground(new Color(250, 252, 255)); // Fondo azul muy pálido
        listaFacturaVisual.setForeground(new Color(45, 45, 65));     // Texto gris oscuro/azulado
        listaFacturaVisual.setSelectionBackground(new Color(184, 207, 229)); // Fondo de selección azul claro
        listaFacturaVisual.setSelectionForeground(Color.BLACK); // Texto de la línea seleccionada
        JScrollPane scroll = new JScrollPane(listaFacturaVisual);
        TitledBorder bordeDetalle = BorderFactory.createTitledBorder("Detalle de la Factura");
        bordeDetalle.setTitleColor(new Color(0, 71, 171)); // <-- CAMBIO DE COLOR (Azul oscuro)
        scroll.setBorder(bordeDetalle);
        // --- Panel Inferior (AQUÍ ESTÁN LOS CAMBIOS IMPORTANTES) ---
        JPanel panelInferior = new JPanel(new GridBagLayout());
        GridBagConstraints gbcInferior = new GridBagConstraints();
        gbcInferior.insets = new Insets(5, 5, 5, 5);

        lblTotal = new JLabel("Total: $0.00");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 16));

        JButton btnEliminar = new JButton("Eliminar producto");
        JButton btnEditar = new JButton("Editar Cantidad");

        // --- NUEVO: Panel para los radio buttons ---
        JPanel panelTipoVenta = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelTipoVenta.setBorder(BorderFactory.createTitledBorder("Tipo de Venta"));
        radioContado = new JRadioButton("Contado", true); // Seleccionado por defecto
        radioCredito = new JRadioButton("Crédito");
        ButtonGroup grupoTipoVenta = new ButtonGroup();
        grupoTipoVenta.add(radioContado);
        grupoTipoVenta.add(radioCredito);
        panelTipoVenta.add(radioContado);
        panelTipoVenta.add(radioCredito);

        JButton btnRegistrarFactura = new JButton("Registrar Factura");
        btnRegistrarFactura.setBackground(new Color(46, 139, 87));
        btnRegistrarFactura.setForeground(Color.WHITE);

        // Posicionamiento con GridBagLayout
        gbcInferior.gridx = 0; gbcInferior.anchor = GridBagConstraints.WEST; panelInferior.add(lblTotal, gbcInferior);
        gbcInferior.gridx = 1; gbcInferior.weightx = 1.0; panelInferior.add(Box.createHorizontalGlue(), gbcInferior); // Espacio flexible
        gbcInferior.gridx = 2; gbcInferior.weightx = 0; gbcInferior.anchor = GridBagConstraints.EAST; panelInferior.add(btnEliminar, gbcInferior);
        gbcInferior.gridx = 3; panelInferior.add(btnEditar, gbcInferior);
        gbcInferior.gridx = 4; panelInferior.add(panelTipoVenta, gbcInferior); // Añadimos el panel de radios
        gbcInferior.gridx = 5; panelInferior.add(btnRegistrarFactura, gbcInferior);
        btnEliminar.addActionListener(e -> abrirDialogoEliminar());
        btnEditar.addActionListener(e -> editarProducto());
        btnRegistrarFactura.addActionListener(e -> iniciarProcesoDeRegistro());
    
        // --- Ensamblaje final ---
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelPrincipal.setBackground(new Color(245, 245, 245)); // <-- CAMBIO DE COLOR (Fondo general)
        
        JPanel panelCentro = new JPanel();
        panelCentro.setLayout(new BoxLayout(panelCentro, BoxLayout.Y_AXIS));
        panelCentro.setOpaque(false); // <-- Hace transparente el panel para ver el fondo general
        panelCentro.add(panelCliente);
        panelCentro.add(panelProducto);
        panelCentro.add(scroll);
        
        panelPrincipal.add(panelEmpresa, BorderLayout.NORTH);
        panelPrincipal.add(panelCentro, BorderLayout.CENTER);
        panelPrincipal.add(panelInferior, BorderLayout.SOUTH);
        
        setContentPane(panelPrincipal);
    
        actualizarDatosEmpresa();
    }
    
    public void refrescarListaProductos() {
        Object itemSeleccionado = null;
        if (comboProductos != null) {
            itemSeleccionado = comboProductos.getSelectedItem();
        }
        
        // CAMBIO: ProductoStorage.cargarProductos() ahora trae los datos desde PostgreSQL
        this.listaProductos = ProductoStorage.cargarProductos(); 
        
        if (comboProductos != null) {
            comboProductos.removeAllItems();
            comboProductos.addItem(null);
            
            // Filtramos para mostrar solo los activos
            this.listaProductos.stream()
                .filter(p -> p.getEstado().equalsIgnoreCase("Activo"))
                .forEach(comboProductos::addItem);
                
            comboProductos.setSelectedItem(itemSeleccionado);
        }
    }

    private void agregarProducto() {
        if (comboProductos.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un producto de la lista.", "Validación Fallida", JOptionPane.WARNING_MESSAGE);
            comboProductos.requestFocus();
            return;
        }

        // --- INICIO DE LOS CAMBIOS ---
        double cantidad; // <-- CAMBIO: de int a double
        try {
            // Reemplazamos Integer.parseInt por Double.parseDouble
            cantidad = Double.parseDouble(txtCantidad.getText().trim().replace(',', '.')); // Acepta , y . como decimal
            if (cantidad <= 0) {
                JOptionPane.showMessageDialog(this, "La cantidad debe ser un número positivo.", "Validación Fallida", JOptionPane.WARNING_MESSAGE);
                txtCantidad.requestFocus();
                txtCantidad.selectAll();
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El valor ingresado en 'Cantidad' no es un número válido.", "Validación Fallida", JOptionPane.WARNING_MESSAGE);
            txtCantidad.requestFocus();
            txtCantidad.selectAll();
            return;
        }
        // --- FIN DE LOS CAMBIOS ---

        if (factura == null) {
            String nombre = txtNombre.getText().trim();
            String cedula = txtCedula.getText().trim();
            String direccion = txtDireccion.getText().trim();
            // Asegúrate de haber declarado txtEmail como campo de clase en FacturaGUI
            String email = (txtEmail != null) ? txtEmail.getText().trim() : ""; 

            Cliente clienteParaFactura;
            
            if (!nombre.isEmpty() && !cedula.isEmpty() && !cedula.equals("0") && !ClienteStorage.clienteExiste(cedula)) {
                int respuesta = JOptionPane.showConfirmDialog(this, "El cliente '" + nombre + "' con cédula '" + cedula + "' no existe.\n¿Desea guardarlo?", "Crear Nuevo Cliente", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (respuesta == JOptionPane.YES_OPTION) {
                    // AQUÍ ESTABA EL ERROR: Usamos el constructor de 4 argumentos
                    Cliente nuevoCliente = new Cliente(nombre, cedula, direccion, email);
                    ClienteStorage.guardarCliente(nuevoCliente);
                    clienteParaFactura = nuevoCliente;
                    // Actualizamos la vista (asumiendo que modificaste setDatosCliente para recibir email, si no, usa el antiguo)
                    setDatosCliente(nombre, cedula, direccion, email);
                } else {
                    // Cliente Ocasional con datos temporales (incluyendo email)
                    clienteParaFactura = new Cliente("Cliente Ocasional", "0", direccion, email);
                }
            } else {
                // Cliente existente o genérico
                clienteParaFactura = new Cliente(
                    nombre.isEmpty() ? "Cliente Ocasional" : nombre, 
                    cedula.isEmpty() ? "0" : cedula, 
                    direccion,
                    email
                );
            }
            String numeroFacturaStr = ConfiguracionManager.getSiguienteNumeroFactura();
            lblNumeroFactura.setText("Factura Nº: " + numeroFacturaStr);
            factura = new Factura(clienteParaFactura, this.empresa, numeroFacturaStr);
        }

        Producto productoSeleccionado = (Producto) comboProductos.getSelectedItem();
        String detalle = txtDetalle.getText().trim();

        if (!detalle.isEmpty() && cantidad != 1) {
            cantidad = 1;
            txtCantidad.setText("1");
        }

        Producto productoInventario = listaProductos.stream()
            .filter(p -> p.getCodigo().equals(productoSeleccionado.getCodigo()))
            .findFirst()
            .orElse(null);

        if (productoInventario == null) {
            JOptionPane.showMessageDialog(this, "Error: Producto no encontrado en inventario.", "Error Interno", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (productoInventario.getCantidad() < cantidad) {
            JOptionPane.showMessageDialog(this, "Inventario insuficiente. Disponibles: " + productoInventario.getCantidad(), "Stock Insuficiente", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        DetalleFactura nuevoDetalle = new DetalleFactura(productoSeleccionado, cantidad, detalle);
        
        if (descuentoParaSiguienteProducto > 0) {
            nuevoDetalle.setDescuento(descuentoParaSiguienteProducto);
            descuentoParaSiguienteProducto = 0.0; 
            actualizarEstadoBotonDescuento();
        }

        if (factura.agregarDetalle(nuevoDetalle)) {
            // --- INICIO DEL CAMBIO ---
            // 1. Actualizamos la memoria local para que la GUI reaccione rápido
            productoInventario.setCantidad(productoInventario.getCantidad() - cantidad);
            
            // 2. IMPORTANTE: Actualizamos la Base de Datos inmediatamente
            ProductoStorage.actualizarStock(productoInventario.getCodigo(), productoInventario.getCantidad());
            // --- FIN DEL CAMBIO ---
            
            actualizarVistaFactura();
            limpiarFormularioDeProducto();
        } else {
            String etiqueta = ConfiguracionManager.cargarEtiquetaDetalle();
            JOptionPane.showMessageDialog(this, "Este producto con el mismo " + etiqueta + " ya ha sido agregado.", "Duplicado", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormularioDeProducto() {
        comboProductos.setSelectedIndex(0);
        txtCantidad.setText("1");
        txtPrecio.setText("");
        txtDetalle.setText("");
        txtBuscarCodigo.setText("");
        comboProductos.requestFocus();
    }
    
    public void actualizarVistaFactura() {
        modeloListaFactura.clear();
        NumberFormat formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        formatoPesos.setMaximumFractionDigits(0);
        String etiqueta = ConfiguracionManager.cargarEtiquetaDetalle();
        
        // --- CAMBIO EN EL ENCABEZADO PARA DAR MÁS ESPACIO ---
        modeloListaFactura.addElement(String.format("%-28s %10s %15s", "PRODUCTO / " + etiqueta.toUpperCase(), "CANTIDAD", "SUBTOTAL"));
        modeloListaFactura.addElement("--------------------------------------------------------------------------");
    
        if (factura != null && !factura.getDetalles().isEmpty()) {
            for (DetalleFactura detalle : factura.getDetalles()) {
                String nombre = detalle.getProducto().getNombre();
                if (nombre.length() > 26) nombre = nombre.substring(0, 26);

                // Creamos una cadena para la cantidad y la unidad juntas (ej. "1.50 Kg")
                String cantidadConUnidad = String.format("%.2f %s", detalle.getCantidad(), detalle.getProducto().getUnidadDeMedida());
                
                // Usamos la nueva cadena en el formato de la línea
                String lineaProducto = String.format("%-28s %15s %15s",
                    nombre, cantidadConUnidad, formatoPesos.format(detalle.getSubtotal()));
    
                modeloListaFactura.addElement(lineaProducto);
    
                if (detalle.getDescuento() > 0) {
                    String lineaDescuento = String.format("  %-28s -%s", "Descuento Aplicado:", formatoPesos.format(detalle.getDescuento()));
                    modeloListaFactura.addElement(lineaDescuento);
                }
    
                if (detalle.getDetalle() != null && !detalle.getDetalle().isEmpty()) {
                    String lineaDetalle = String.format("  %s: %-23s", etiqueta, detalle.getDetalle());
                    modeloListaFactura.addElement(lineaDetalle);
                }
            }
            lblTotal.setText("Total: " + formatoPesos.format(factura.calcularTotal()));
        } else {
            lblTotal.setText("Total: $0");
        }
    }
    
    public void actualizarDatosEmpresa() {
        this.empresa = ConfiguracionManager.cargarEmpresa();
        
        lblRazon.setText("Razón Social: " + empresa.getRazonSocial());
        lblNit.setText("NIT: " + empresa.getNit());
        lblTelefono.setText("Teléfono: " + empresa.getTelefono());
        
        lblDetalle.setText(ConfiguracionManager.cargarEtiquetaDetalle() + ":");
        
        actualizarLogo();
        repaint();
        revalidate();
    }
    
    private void actualizarLogo() {
        if (lblImagen != null) {
            panelContenidoCliente.remove(lblImagen);
        }
        try {
            File logoExterno = PathsHelper.getLogoFile();
            URL imgUrl;
            if (logoExterno.exists()) {
                imgUrl = logoExterno.toURI().toURL();
            } else {
                imgUrl = getClass().getResource("/img/logo.jpeg");
            }
            if (imgUrl != null) {
                ImageIcon icono = new ImageIcon(imgUrl);
                Image imagenEscalada = icono.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                lblImagen = new JLabel(new ImageIcon(imagenEscalada));
                panelContenidoCliente.add(lblImagen, BorderLayout.EAST);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void limpiarFormulario() {
        // Llama al método que se encarga de la parte del cliente
        limpiarDatosCliente(); 
        
        // Limpia la parte de los productos
        limpiarFormularioDeProducto();
        
        descuentoParaSiguienteProducto = 0.0; // Resetea la variable
        actualizarEstadoBotonDescuento();     // Resetea la apariencia del botón
        factura = null; 
        
        // Actualiza la interfaz para reflejar el estado inicial
        lblNumeroFactura.setText("Factura Nº: ---");
        actualizarVistaFactura();
    }
    

    public void cargarProductoDesdeInventario(Producto producto) {
        comboProductos.setSelectedItem(producto);
    }

    @Override // Asegúrate de mantener la anotación @Override
    public void setDatosCliente(String nombre, String cedula, String direccion, String email) {
        txtNombre.setText(nombre);
        txtCedula.setText(cedula);
        txtDireccion.setText(direccion);

        if (txtEmail != null) {
            txtEmail.setText(email);
        }
        
        lblNombreClienteTarjeta.setText(nombre);
        lblCedulaClienteTarjeta.setText("C.C. " + cedula);
    
        // --- INICIO DE LA NUEVA LÓGICA ---
        // 1. Calcular el saldo del cliente
        double saldo = GeneradorReportesGUI.calcularSaldoAFavorCliente(cedula);
    
        // 2. Si tiene saldo, mostrarlo en la tarjeta de cliente
        if (saldo > 0) {
            NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
            formato.setMaximumFractionDigits(0);
            lblCedulaClienteTarjeta.setText("C.C. " + cedula + " | Saldo a Favor: " + formato.format(saldo));
            lblCedulaClienteTarjeta.setForeground(new Color(0, 128, 0)); // Color verde
        } else {
            lblCedulaClienteTarjeta.setForeground(Color.GRAY); // Color normal
        }
        // --- FIN DE LA NUEVA LÓGICA ---
    }

    private void abrirDialogoEliminar() {
        if (factura == null || factura.getDetalles().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay productos en la factura para eliminar.", "Factura Vacía", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new EliminarProductoFacturaGUI(this, factura).setVisible(true);
    }

    public void procesarEliminacionProducto(DetalleFactura detalleAEliminar) {
        Producto productoDevuelto = detalleAEliminar.getProducto();
        double cantidadDevuelta = detalleAEliminar.getCantidad();
    
        // Buscamos el producto en la lista en memoria
        for (Producto p : listaProductos) {
            if (p.getCodigo().equals(productoDevuelto.getCodigo())) {
                // Calculamos el nuevo stock
                double nuevoStock = p.getCantidad() + cantidadDevuelta;
                p.setCantidad(nuevoStock); // Actualizamos memoria
                
                // --- ACTUALIZAMOS BASE DE DATOS ---
                ProductoStorage.actualizarStock(p.getCodigo(), nuevoStock);
                break;
            }
        }
        // Ya no necesitamos 'sobrescribirInventario' completo
        factura.getDetalles().remove(detalleAEliminar);
        actualizarVistaFactura();
    }
    

    private void iniciarProcesoDeRegistro() {
        if (factura == null || factura.getDetalles().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay productos en la factura para registrar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
    
        if (radioContado.isSelected()) {
            double total = factura.calcularTotal();
            ProcesarPagoGUI dialogoPago = new ProcesarPagoGUI(this, total, factura.getCliente().getCedula());
            dialogoPago.setVisible(true);
            
            if (dialogoPago.isPagoConfirmado()) {
                // Establecemos los datos de pago
                factura.setMetodoPago(dialogoPago.getMetodoPagoSeleccionado());
                double saldoUsado = dialogoPago.getSaldoAFavorAplicado();
            
                // --- PASO 1: GUARDAR EN BASE DE DATOS LOCAL ---
                boolean guardadoExitoso = FacturaStorage.guardarFacturaCompleta(factura);
    
                if (guardadoExitoso) {
                    // --- PASO 2: MOVER DINERO (CAJA/BANCO) ---
                    if ("Efectivo".equals(factura.getMetodoPago())) {
                        CuentasStorage.agregarACaja(factura.calcularTotal() - saldoUsado);
                    } else {
                        CuentasStorage.agregarABanco(factura.calcularTotal() - saldoUsado);
                    }
        
                    if (saldoUsado > 0) {
                        marcarCreditosComoUsados(factura.getCliente().getCedula(), saldoUsado);
                    }
                    // --- PASO 3: GENERAR RESPALDO TXT E IMPRIMIR ---
                    File archivoGuardado = registrarFacturaEnArchivo(); // Mantenemos el TXT como respaldo
                    
                    int respuesta = JOptionPane.showConfirmDialog(this, "Factura registrada en Base de Datos. ¿Desea imprimirla?", "Éxito", JOptionPane.YES_NO_OPTION);
                    if (respuesta == JOptionPane.YES_OPTION) {
                        imprimirFactura(archivoGuardado);
                    }
                    limpiarFormulario();
                }
            }
        } else {
            // Nota: Deberías aplicar una lógica similar dentro de este método si también quieres facturar a crédito electrónicamente
            registrarFacturaACredito();
        }
    }

    private void registrarFacturaACredito() {
        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Registrar venta a CRÉDITO en la Base de Datos?",
            "Confirmar Crédito", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    
        if (confirmacion == JOptionPane.YES_OPTION) {
            factura.setMetodoPago("Crédito");
    
            // --- PASO 1: GUARDAR EN BASE DE DATOS LOCAL ---
            boolean guardadoExitoso = FacturaStorage.guardarFacturaCompleta(factura);
    
            if (guardadoExitoso) {
                // --- PASO 2: GENERAR DOCUMENTOS LOCALES ---
                File archivoFactura = registrarFacturaEnArchivo();
                GeneradorReportesGUI.generarReciboCreditoAutomatico(factura);
    
                JOptionPane.showMessageDialog(this, "Venta a crédito registrada exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                
                if (JOptionPane.showConfirmDialog(this, "¿Imprimir factura?", "Imprimir", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    imprimirFactura(archivoFactura);
                }
    
                limpiarFormulario();
            }
        }
    }

    // Copia y pega este método. Este es el ajuste final.
    public void imprimirFactura(java.io.File archivoFactura) {
        try {
            List<String> lineasOriginales = Files.readAllLines(archivoFactura.toPath());
            StringBuilder contenidoParaImprimir = new StringBuilder();
            NumberFormat formatoMonedaImpresion = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
            formatoMonedaImpresion.setMaximumFractionDigits(0);

            for (String linea : lineasOriginales) {
                if (linea.contains("#venta_u:")) {
                    String parteVisible = linea.split("#")[0].trim();
                    String parteDatos = linea.split("#")[1];

                    int indiceIgual = parteVisible.lastIndexOf(" = ");
                    int indiceUltimaX = parteVisible.lastIndexOf(" x ", indiceIgual);
                    
                    String nombreProducto = parteVisible.substring(parteVisible.indexOf(']') + 1, indiceUltimaX).trim();
                    String cantidadFormateada = parteVisible.substring(indiceUltimaX + 3, indiceIgual).trim();
                    String subtotalStr = parteVisible.split(" = ")[1].trim();
                    
                    double precioVentaUnitario = 0.0;
                    try {
                        precioVentaUnitario = Double.parseDouble(parteDatos.split(";")[0].split(":")[1]);
                    } catch (Exception e) { /* Ignorar si hay error */ }

                    String precioUnitarioStr = formatoMonedaImpresion.format(precioVentaUnitario);

                    // LÍNEA 1: Nombre del producto (sin cambios)
                    contenidoParaImprimir.append(String.format("%s x %s\n", cantidadFormateada, nombreProducto));
                    
                    // --- INICIO DEL AJUSTE FINAL ---
                    // LÍNEA 2: Precio unitario en formato de dos columnas
                    contenidoParaImprimir.append(String.format("  %-20s %10s\n", "Precio Unitario:", precioUnitarioStr));
                    
                    // LÍNEA 3: Subtotal en formato de dos columnas
                    contenidoParaImprimir.append(String.format("  %-20s %10s\n", "Subtotal:", subtotalStr));
                    // --- FIN DEL AJUSTE FINAL ---

                } else {
                    contenidoParaImprimir.append(linea).append("\n");
                }
            }

            // El código de configuración de la impresora no cambia
            java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
            double paperWidthMM = 80.0;
            double paperWidthInches = paperWidthMM / 25.4;
            double paperWidthUnits = paperWidthInches * 72.0;
            java.awt.print.PageFormat pageFormat = job.defaultPage();
            java.awt.print.Paper paper = pageFormat.getPaper();
            double margin = 10;
            paper.setSize(paperWidthUnits, Double.MAX_VALUE);
            paper.setImageableArea(margin, margin, paperWidthUnits - (margin * 2), Double.MAX_VALUE);
            pageFormat.setPaper(paper);
            job.setPrintable(new FacturaPrinter(contenidoParaImprimir.toString()), pageFormat);

            if (job.printDialog()) {
                job.print();
                DrawerManager.openCashDrawer(this);
            }

        } catch (java.io.IOException | java.awt.print.PrinterException e) {
            JOptionPane.showMessageDialog(this, "Error de Impresión: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editarProducto() {
        // 1. Validar que haya qué editar
        if (factura == null || factura.getDetalles().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay productos en la factura para editar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. Pedir al usuario qué línea quiere editar
        String lineaStr = JOptionPane.showInputDialog(this, "Ingrese el número de la línea de producto que desea editar (ej. 1, 2, 3...):", "Editar Cantidad", JOptionPane.QUESTION_MESSAGE);
        if (lineaStr == null) return;

        try {
            int indice = Integer.parseInt(lineaStr) - 1;
            
            // Validar índice
            if (indice < 0 || indice >= factura.getDetalles().size()) {
                JOptionPane.showMessageDialog(this, "El número de línea no es válido.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            DetalleFactura detalleAEditar = factura.getDetalles().get(indice);
            
            // 3. Pedir la nueva cantidad
            String cantidadStr = JOptionPane.showInputDialog(this, 
                "Producto: " + detalleAEditar.getProducto().getNombre() + 
                "\nCantidad actual: " + detalleAEditar.getCantidad() + 
                "\n\nIngrese la nueva cantidad:", 
                "Nueva Cantidad", JOptionPane.QUESTION_MESSAGE);
            
            if (cantidadStr == null) return;
    
            double nuevaCantidad = Double.parseDouble(cantidadStr.replace(',', '.'));
            if (nuevaCantidad <= 0) {
                JOptionPane.showMessageDialog(this, "La cantidad debe ser un número positivo.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 4. Calcular la diferencia para ajustar el inventario
            double cantidadOriginal = detalleAEditar.getCantidad();
            double diferencia = nuevaCantidad - cantidadOriginal; // Positivo = necesito más stock; Negativo = devuelvo stock
            
            // Buscamos el producto en la lista en memoria (que está sincronizada con la BD)
            Producto productoInventario = listaProductos.stream()
                .filter(p -> p.getCodigo().equals(detalleAEditar.getProducto().getCodigo()))
                .findFirst()
                .orElse(null);

            if (productoInventario != null) {
                // Si la diferencia es positiva, verificamos que haya suficiente stock extra
                if (diferencia > 0 && productoInventario.getCantidad() < diferencia) {
                    JOptionPane.showMessageDialog(this, "No hay suficiente stock para aumentar la cantidad.\nDisponibles: " + productoInventario.getCantidad(), "Stock Insuficiente", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // 5. ACTUALIZAR STOCK (Memoria + Base de Datos)
                double nuevoStock = productoInventario.getCantidad() - diferencia;
                
                // Actualizamos memoria
                productoInventario.setCantidad(nuevoStock);
                
                // Actualizamos PostgreSQL inmediatamente
                ProductoStorage.actualizarStock(productoInventario.getCodigo(), nuevoStock);
            }

            // 6. Actualizar la factura visualmente
            detalleAEditar.setCantidad(nuevaCantidad);
            actualizarVistaFactura();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese un número válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ocurrió un error al editar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarPermisos() {
        // Opciones del menú "Opciones"
        itemCrearProducto.setEnabled(UserSession.tienePermiso("GESTIONAR_PRODUCTOS"));
        itemConsultarCliente.setEnabled(UserSession.tienePermiso("GESTIONAR_CLIENTES"));
        itemDevoluciones.setEnabled(UserSession.tienePermiso("REGISTRAR_DEVOLUCION"));
        itemConsultarDevoluciones.setEnabled(UserSession.tienePermiso("CONSULTAR_DEVOLUCIONES"));
        itemReporteVentas.setEnabled(UserSession.tienePermiso("VER_REPORTES"));
        itemGestionUsuarios.setEnabled(UserSession.tienePermiso("GESTIONAR_USUARIOS"));
        itemConfiguracion.setEnabled(UserSession.tienePermiso("CONFIGURAR_EMPRESA"));
        
        // Menú "Inventario"
        menuInventario.setEnabled(UserSession.tienePermiso("MENU_INVENTARIO"));

        // Menú "Archivo" (para backups)
        menuArchivo.setEnabled(UserSession.tienePermiso("GESTIONAR_BACKUPS"));
    }

    private void limpiarDatosCliente() {
        // Limpia los campos de texto
        txtNombre.setText("");
        txtCedula.setText("");
        txtDireccion.setText("");
        
        // Resetea la "Tarjeta de Cliente" al estado por defecto
        lblNombreClienteTarjeta.setText("Cliente Ocasional");
        lblCedulaClienteTarjeta.setText("C.C. 0");
        txtEmail.setText("");
        
        // Pone el foco de nuevo en el campo del nombre
        txtNombre.requestFocus();
    }

    // Reemplaza este método completo en FacturaGUI.java
    private void marcarCreditosComoUsados(String cedulaCliente, double montoUsado) {
        File carpetaOrdenes = PathsHelper.getOrdenesFolder();
        File[] archivos = carpetaOrdenes.listFiles(
            (dir, name) -> name.startsWith("NotadeCredito_") && !name.startsWith("USADA_")
        );

        if (archivos == null) return;

        // Ordenamos los archivos para usar los más antiguos primero (opcional pero buena práctica)
        java.util.Arrays.sort(archivos, java.util.Comparator.comparing(File::getName));
        
        // Obtenemos una instancia del formateador de moneda para usarlo al reescribir el archivo
        NumberFormat formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        formatoPesos.setMaximumFractionDigits(0);

        for (File archivo : archivos) {
            if (montoUsado <= 0) break; // Si ya cubrimos el monto total, nos detenemos.

            try {
                List<String> lineas = new java.util.ArrayList<>(Files.readAllLines(archivo.toPath()));
                
                // Verificamos que la nota de crédito pertenezca al cliente
                Optional<String> cedulaEnArchivoOpt = lineas.stream()
                    .filter(l -> l.startsWith("Cédula/ID:"))
                    .findFirst();

                if (cedulaEnArchivoOpt.isPresent() && cedulaEnArchivoOpt.get().split(":")[1].trim().equals(cedulaCliente)) {
                    
                    // Buscamos el valor de la nota de crédito
                    Optional<String> valorStrOpt = lineas.stream()
                        .filter(l -> l.startsWith("VALOR A FAVOR DEL CLIENTE:"))
                        .findFirst();
                    
                    if (valorStrOpt.isPresent()) {
                        String valorNumerico = valorStrOpt.get().replaceAll("[^0-9]", "");
                        double valorNota = Double.parseDouble(valorNumerico);

                        // --- INICIO DE LA NUEVA LÓGICA ---

                        // CASO 1: La nota de crédito se consume por completo (o exactamente).
                        if (montoUsado >= valorNota) {
                            File archivoUsado = new File(carpetaOrdenes, "USADA_" + archivo.getName());
                            Files.move(archivo.toPath(), archivoUsado.toPath());
                            montoUsado -= valorNota; // Restamos lo que usamos y continuamos
                        } 
                        // CASO 2: La nota de crédito se consume solo parcialmente.
                        else {
                            double nuevoSaldo = valorNota - montoUsado;
                            
                            // Buscamos el índice de la línea que vamos a reemplazar
                            int indiceDeLaLinea = -1;
                            for(int i=0; i < lineas.size(); i++){
                                if(lineas.get(i).startsWith("VALOR A FAVOR DEL CLIENTE:")){
                                    indiceDeLaLinea = i;
                                    break;
                                }
                            }
                            
                            // Si encontramos la línea, la actualizamos con el nuevo saldo
                            if(indiceDeLaLinea != -1){
                                lineas.set(indiceDeLaLinea, "VALOR A FAVOR DEL CLIENTE: " + formatoPesos.format(nuevoSaldo));
                            }
                            
                            // Sobrescribimos el archivo con el saldo actualizado
                            Files.write(archivo.toPath(), lineas);
                            
                            montoUsado = 0; // El monto de la compra ya fue cubierto
                        }
                        // --- FIN DE LA NUEVA LÓGICA ---
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Ocurrió un error al procesar el saldo a favor.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void actualizarEstadoBotonDescuento() {
        if (descuentoParaSiguienteProducto > 0) {
            // --- Estado ACTIVO ---
            String textoDescuento = formatoPesos.format(descuentoParaSiguienteProducto);
            btnAplicarDescuento.setText("✓ Descuento: " + textoDescuento + " (Cancelar)");
            btnAplicarDescuento.setBackground(new Color(223, 240, 216)); // Verde suave
            btnAplicarDescuento.setToolTipText("Hay un descuento activo de " + textoDescuento + ". Haz clic para cancelarlo.");
        } else {
            // --- Estado por DEFECTO ---
            btnAplicarDescuento.setText("Descuento");
            btnAplicarDescuento.setBackground(UIManager.getColor("Button.background")); // Color por defecto
            btnAplicarDescuento.setToolTipText("Aplica un descuento al próximo producto que se agregue.");
        }
    }

    // Método auxiliar para guardar el respaldo TXT e imprimir
    private File registrarFacturaEnArchivo() {
        if (factura == null) return null;
        try {
            File carpeta = PathsHelper.getFacturasFolder();
            String nombreArchivo = "factura_" + factura.getNumeroFactura() + ".txt";
            File archivoFactura = new File(carpeta, nombreArchivo);
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivoFactura))) {
                writer.write(factura.generarTextoFactura()); // Este método debe estar en tu clase Factura
            }
            return archivoFactura; 
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al guardar respaldo TXT: " + e.getMessage());
            return null;
        }
    }

}