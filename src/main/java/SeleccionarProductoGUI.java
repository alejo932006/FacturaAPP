import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.swing.table.DefaultTableCellRenderer;

public class SeleccionarProductoGUI extends JDialog {
    private JTable tablaProductos;
    private DefaultTableModel modelo;
    private TableRowSorter<DefaultTableModel> sorter;
    private FacturaGUI facturaGUI;

    // --- NUEVOS COMPONENTES PARA FILTROS ---
    private JTextField txtBusqueda;
    private JComboBox<String> comboCriterioBusqueda;
    private JCheckBox checkBajoStock;
    private List<Producto> inventarioCompleto; // Mantenemos una copia local para filtrar rápido

    public SeleccionarProductoGUI(FacturaGUI owner) {
        super(owner, "Panel de Control de Inventario", true);
        this.facturaGUI = owner;
        setSize(950, 600); // Ventana más grande para las nuevas opciones
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        cargarProductos();
    }

    private void initComponents() {
        // Contenedor principal con padding
        JPanel panelContenedor = new JPanel(new BorderLayout(10, 10));
        panelContenedor.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- 1. PANEL DE FILTROS (REEMPLAZA LA BÚSQUEDA SIMPLE) ---
        JPanel panelFiltros = new JPanel();
        panelFiltros.setBorder(BorderFactory.createTitledBorder("Búsqueda y Filtros"));
        panelFiltros.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        txtBusqueda = new JTextField(20);
        comboCriterioBusqueda = new JComboBox<>(new String[]{"Por Nombre", "Por Código"});
        checkBajoStock = new JCheckBox("Mostrar solo con stock bajo (< 5 unidades)");
        JButton btnLimpiarFiltros = new JButton();

        configurarBoton(btnLimpiarFiltros, "Limpiar todos los filtros", "clear.png", false);

        gbc.gridx = 0; gbc.gridy = 0; panelFiltros.add(new JLabel("Buscar:"), gbc);
        gbc.gridx = 1; panelFiltros.add(txtBusqueda, gbc);
        gbc.gridx = 2; panelFiltros.add(comboCriterioBusqueda, gbc);
        gbc.gridx = 3; panelFiltros.add(btnLimpiarFiltros, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3; panelFiltros.add(checkBajoStock, gbc);
        
        // --- 2. TABLA DE PRODUCTOS (CON RENDERER PERSONALIZADO) ---
        String[] columnas = {"Código", "Nombre", "Precio Venta", "Costo", "Stock"};
        modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaProductos = new JTable(modelo);
        sorter = new TableRowSorter<>(modelo);
        
        // --- CORRECCIÓN DE ORDENAMIENTO ---
        // Esto obliga a que la Columna 0 (Código) se ordene como número (1, 2, 10) y no texto (1, 10, 2)
        sorter.setComparator(0, (String s1, String s2) -> {
            try {
                long n1 = Long.parseLong(s1);
                long n2 = Long.parseLong(s2);
                return Long.compare(n1, n2);
            } catch (NumberFormatException e) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        // -----------------------------------

        tablaProductos.setRowSorter(sorter);

        // APLICAMOS EL RENDERER DE COLORES
        tablaProductos.setDefaultRenderer(Object.class, new StockStatusRenderer());

        // --- 3. PANEL DE BOTONES (ACCIONES Y HERRAMIENTAS) ---
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnCargar = new JButton("Cargar en Factura");
        JButton btnEditarStock = new JButton("Editar Stock");
        JButton btnValorInventario = new JButton("Valorizar Inventario");
        JButton btnExportar = new JButton("Exportar a CSV");
        JButton btnEliminar = new JButton("Eliminar Producto");

        // Estilo y Iconos para los botones
        configurarBoton(btnCargar, "Carga el producto seleccionado en la ventana de facturación.", "select.png", true);
        configurarBoton(btnEditarStock, "Modifica la cantidad de stock del producto seleccionado.", "stock.png", true);
        configurarBoton(btnValorInventario, "Calcula el valor total del inventario a costo y a venta.", "calculator.png", true);
        configurarBoton(btnExportar, "Exporta la vista actual del inventario a un archivo CSV.", "export.png", true);
        configurarBoton(btnEliminar, "Elimina permanentemente un producto del inventario.", "delete.png", true);
        btnEliminar.setBackground(new Color(255, 228, 225)); // Color rojo suave de advertencia

        panelBotones.add(btnCargar);
        panelBotones.add(btnEditarStock);
        panelBotones.add(btnValorInventario);
        panelBotones.add(btnExportar);
        panelBotones.add(Box.createHorizontalStrut(20)); // Espaciador
        panelBotones.add(btnEliminar);

        // --- LÓGICA DE EVENTOS ---
        txtBusqueda.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltros(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltros(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltros(); }
        });
        comboCriterioBusqueda.addActionListener(e -> aplicarFiltros());
        checkBajoStock.addActionListener(e -> aplicarFiltros());
        btnLimpiarFiltros.addActionListener(e -> {
            txtBusqueda.setText("");
            checkBajoStock.setSelected(false);
            aplicarFiltros();
        });
        
        btnCargar.addActionListener(e -> cargarProductoSeleccionado());
        btnEditarStock.addActionListener(e -> editarStockRapido());
        btnEliminar.addActionListener(e -> eliminarProductoSeleccionado());
        btnValorInventario.addActionListener(e -> calcularValorInventario());
        btnExportar.addActionListener(e -> exportarACSV());

        // --- ENSAMBLAJE FINAL ---
        panelContenedor.add(panelFiltros, BorderLayout.NORTH);
        panelContenedor.add(new JScrollPane(tablaProductos), BorderLayout.CENTER);
        panelContenedor.add(panelBotones, BorderLayout.SOUTH);
        add(panelContenedor);
    }

    // --- MÉTODOS DE LÓGICA ---

    private void aplicarFiltros() {
        List<RowFilter<Object, Object>> filtros = new ArrayList<>();
        String textoBusqueda = txtBusqueda.getText().trim();
        
        // Filtro por texto (Nombre o Código)
        if (!textoBusqueda.isEmpty()) {
            int columna = comboCriterioBusqueda.getSelectedIndex() == 0 ? 1 : 0; // 1 para Nombre, 0 para Código
            filtros.add(RowFilter.regexFilter("(?i)" + textoBusqueda, columna));
        }

        // Filtro por Stock Bajo
        if (checkBajoStock.isSelected()) {
            filtros.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, 5, 4)); // Columna 4 es Stock
        }

        sorter.setRowFilter(RowFilter.andFilter(filtros));
    }

    private void cargarProductos() {
        inventarioCompleto = ProductoStorage.cargarProductos();
        modelo.setRowCount(0);

        List<Producto> productosActivos = inventarioCompleto.stream()
            .filter(p -> p.getEstado().equalsIgnoreCase("Activo"))
            .collect(Collectors.toList());

        NumberFormat formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        formatoPesos.setMaximumFractionDigits(0);

        for (Producto p : productosActivos) {
            modelo.addRow(new Object[]{
                p.getCodigo(), p.getNombre(),
                formatoPesos.format(p.getPrecioVenta()),
                formatoPesos.format(p.getCosto()),
                p.getCantidad()
            });
        }
    }

    private void cargarProductoSeleccionado() {
        int filaVista = tablaProductos.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int filaModelo = tablaProductos.convertRowIndexToModel(filaVista);
        String codigo = modelo.getValueAt(filaModelo, 0).toString();
        
        inventarioCompleto.stream()
            .filter(p -> p.getCodigo().equals(codigo))
            .findFirst()
            .ifPresent(producto -> {
                facturaGUI.cargarProductoDesdeInventario(producto);
                dispose();
            });
    }

    private void editarStockRapido() {
        int filaVista = tablaProductos.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto para editar su stock.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1. Pedir clave de administrador (seguridad)
        JPasswordField passwordField = new JPasswordField();
        int option = JOptionPane.showConfirmDialog(this, passwordField, "Ingrese la Clave Administrativa", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (option != JOptionPane.OK_OPTION || !new String(passwordField.getPassword()).equals(ConfiguracionManager.getAdminPassword())) {
            if (option == JOptionPane.OK_OPTION) JOptionPane.showMessageDialog(this, "Clave incorrecta.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Obtener datos y pedir nueva cantidad
        int filaModelo = tablaProductos.convertRowIndexToModel(filaVista);
        String codigo = modelo.getValueAt(filaModelo, 0).toString();
        String nombre = modelo.getValueAt(filaModelo, 1).toString();
        int cantidadActual = (int) modelo.getValueAt(filaModelo, 4);

        String nuevaCantidadStr = JOptionPane.showInputDialog(this, "Producto: " + nombre + "\nStock Actual: " + cantidadActual + "\n\nIngrese la NUEVA cantidad total:", "Editar Stock", JOptionPane.QUESTION_MESSAGE);
        if (nuevaCantidadStr == null) return;
        
        // 3. Validar, actualizar y refrescar
        try {
            int nuevaCantidad = Integer.parseInt(nuevaCantidadStr);
            if (nuevaCantidad < 0) throw new NumberFormatException();

            inventarioCompleto.stream()
                .filter(p -> p.getCodigo().equalsIgnoreCase(codigo))
                .findFirst()
                .ifPresent(p -> p.setCantidad(nuevaCantidad));
            
            ProductoStorage.sobrescribirInventario(inventarioCompleto);
            cargarProductos(); // Recarga la tabla con los datos actualizados
            facturaGUI.refrescarListaProductos();
            JOptionPane.showMessageDialog(this, "Stock actualizado para '" + nombre + "'.", "Éxito", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese un número entero válido y no negativo.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void exportarACSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar como CSV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            if (!archivo.getName().toLowerCase().endsWith(".csv")) {
                archivo = new File(archivo.getParentFile(), archivo.getName() + ".csv");
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
                // Escribir encabezados
                for (int i = 0; i < modelo.getColumnCount(); i++) {
                    writer.write(modelo.getColumnName(i) + (i == modelo.getColumnCount() - 1 ? "" : ";"));
                }
                writer.newLine();

                // Escribir datos (solo las filas visibles/filtradas)
                for (int i = 0; i < tablaProductos.getRowCount(); i++) {
                    for (int j = 0; j < tablaProductos.getColumnCount(); j++) {
                        writer.write(tablaProductos.getValueAt(i, j).toString() + (j == tablaProductos.getColumnCount() - 1 ? "" : ";"));
                    }
                    writer.newLine();
                }
                JOptionPane.showMessageDialog(this, "Inventario exportado exitosamente a:\n" + archivo.getAbsolutePath(), "Exportación Completa", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al exportar el archivo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // El resto de los métodos (eliminar, calcular valor) permanecen como estaban.
    private void eliminarProductoSeleccionado() {
        // ... (el código para eliminar que ya tenías es correcto y no necesita cambios)
         int filaSeleccionada = tablaProductos.getSelectedRow();
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un producto para eliminar.", "Ningún producto seleccionado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int filaModelo = tablaProductos.convertRowIndexToModel(filaSeleccionada);
        String codigo = (String) modelo.getValueAt(filaModelo, 0);
        String nombre = (String) modelo.getValueAt(filaModelo, 1);

        if (ProductoStorage.productoTieneMovimiento(codigo)) {
            JOptionPane.showMessageDialog(this,
                "<html>El producto '" + nombre + "' no puede ser eliminado porque tiene un historial de ventas.<br><br>" +
                "<b>Eliminarlo afectaría la precisión de los reportes antiguos.</b><br><br>" +
                "En su lugar, vaya a 'Gestionar Productos' y use la opción para 'Desactivarlo'.</html>",
                "Acción no Permitida",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        JPasswordField passwordField = new JPasswordField();
        int option = JOptionPane.showConfirmDialog(this, passwordField, "Ingrese la Clave Administrativa para Eliminar", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (option != JOptionPane.OK_OPTION || !new String(passwordField.getPassword()).equals(ConfiguracionManager.getAdminPassword())) {
            if(option == JOptionPane.OK_OPTION) JOptionPane.showMessageDialog(this, "Clave incorrecta. Acción denegada.", "Error de Autenticación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de que desea eliminar PERMANENTEMENTE el producto '" + nombre + "'?",
            "Confirmación Final",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (respuesta == JOptionPane.YES_OPTION) {
            ProductoStorage.eliminarProducto(codigo);
            cargarProductos(); // Recargamos para reflejar el cambio
            facturaGUI.refrescarListaProductos();
            JOptionPane.showMessageDialog(this, "Producto eliminado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    private void calcularValorInventario() {
        // ... (el código para calcular el valor que ya tenías es correcto y no necesita cambios)
        List<Producto> productosActivos = inventarioCompleto.stream()
            .filter(p -> p.getEstado().equalsIgnoreCase("Activo"))
            .collect(Collectors.toList());
        double valorTotalCosto = 0.0, valorTotalVenta = 0.0;
        for (Producto p : productosActivos) {
            valorTotalCosto += p.getCosto() * p.getCantidad();
            valorTotalVenta += p.getPrecioVenta() * p.getCantidad();
        }
        double utilidadPotencial = valorTotalVenta - valorTotalCosto;
        NumberFormat formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        formatoPesos.setMaximumFractionDigits(0);
        String mensaje = "<html><body style='width: 300px;'>" +
                         "<h2>Valorización del Inventario (Solo Activos)</h2>" +
                         "<p><b>A Precio de Costo:</b> " + formatoPesos.format(valorTotalCosto) + "</p>" +
                         "<p><b>A Precio de Venta:</b> " + formatoPesos.format(valorTotalVenta) + "</p>" +
                         "<hr>" +
                         "<p><b>Utilidad Potencial:</b> " + formatoPesos.format(utilidadPotencial) + "</p>" +
                         "</body></html>";
        JOptionPane.showMessageDialog(this, mensaje, "Reporte de Valor de Inventario", JOptionPane.INFORMATION_MESSAGE);
    }
    private void configurarBoton(JButton boton, String tooltip, String iconName, boolean withText) {
        boton.setToolTipText(tooltip);
        if (!withText) {
            boton.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            boton.setFocusable(false);
        }
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/img/" + iconName));
            Image scaledImg = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            boton.setIcon(new ImageIcon(scaledImg));
        } catch (Exception e) {
            System.err.println("No se pudo encontrar el icono: " + iconName);
        }
    }

    // --- CLASE INTERNA PARA RENDERIZAR LA TABLA CON COLORES ---
    class StockStatusRenderer extends DefaultTableCellRenderer {
        private final Color LOW_STOCK_COLOR = new Color(255, 248, 225); // Amarillo claro
        private final Color OUT_OF_STOCK_COLOR = new Color(255, 228, 225); // Rojo claro

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                try {
                    int modelRow = table.convertRowIndexToModel(row);
                    int stock = (int) table.getModel().getValueAt(modelRow, 4); // Columna de Stock

                    if (stock == 0) {
                        c.setBackground(OUT_OF_STOCK_COLOR);
                        c.setForeground(Color.DARK_GRAY);
                    } else if (stock < 5) {
                        c.setBackground(LOW_STOCK_COLOR);
                        c.setForeground(Color.BLACK);
                    } else {
                        c.setBackground(table.getBackground());
                        c.setForeground(table.getForeground());
                    }
                } catch (Exception e) {
                    // En caso de error (ej. valor no numérico), usar colores por defecto
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }
            }
            return c;
        }
    }
}