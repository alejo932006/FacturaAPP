import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ConsultarFacturasGUI extends JFrame {

    private JTable tablaFacturas;
    private DefaultTableModel modeloTabla;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextArea areaVistaPrevia;
    private JButton btnAnularFactura;
    private final NumberFormat formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    private FacturaGUI owner;

    public ConsultarFacturasGUI(FacturaGUI owner) {
        super("Consultar y Gestionar Facturas");
        this.owner = owner;
        formatoPesos.setMaximumFractionDigits(0);
        setSize(1200, 700); // Ventana más grande para la nueva interfaz
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        initComponents();
        cargarFacturas();
        
        setVisible(true);
    }

    private void initComponents() {
        // --- Panel Principal ---
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel Izquierdo (Tabla y Búsqueda) ---
        JPanel panelIzquierdo = new JPanel(new BorderLayout(5, 5));
        
        JTextField txtBuscar = new JTextField();
        JPanel panelBusqueda = new JPanel(new BorderLayout(5, 0));
        panelBusqueda.add(new JLabel("Buscar por Nº, Cliente, Fecha o Total: "), BorderLayout.WEST);
        panelBusqueda.add(txtBuscar, BorderLayout.CENTER);
        
        String[] columnas = {"Estado", "Nº Factura", "Fecha", "Cliente", "Total", "Nombre Archivo"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaFacturas = new JTable(modeloTabla);
        sorter = new TableRowSorter<>(modeloTabla);
        tablaFacturas.setRowSorter(sorter);

        // Ocultamos la columna del nombre de archivo
        tablaFacturas.getColumnModel().getColumn(5).setMinWidth(0);
        tablaFacturas.getColumnModel().getColumn(5).setMaxWidth(0);
        
        // Aplicamos el renderer para los colores de estado
        tablaFacturas.setDefaultRenderer(Object.class, new EstadoFacturaRenderer());

        panelIzquierdo.add(panelBusqueda, BorderLayout.NORTH);
        panelIzquierdo.add(new JScrollPane(tablaFacturas), BorderLayout.CENTER);

        // --- Panel Derecho (Vista Previa y Botones) ---
        JPanel panelDerecho = new JPanel(new BorderLayout(10, 10));
        areaVistaPrevia = new JTextArea("Seleccione una factura para ver sus detalles.");
        areaVistaPrevia.setEditable(false);
        areaVistaPrevia.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaVistaPrevia.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollVistaPrevia = new JScrollPane(areaVistaPrevia);
        scrollVistaPrevia.setBorder(BorderFactory.createTitledBorder("Vista Previa Rápida"));

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnImprimir = new JButton("Imprimir Copia");
        btnAnularFactura = new JButton("Anular Factura");
        btnAnularFactura.setBackground(new Color(220, 53, 69));
        btnAnularFactura.setForeground(Color.WHITE);
        panelBotones.add(btnImprimir);
        panelBotones.add(btnAnularFactura);
        
        panelDerecho.add(scrollVistaPrevia, BorderLayout.CENTER);
        panelDerecho.add(panelBotones, BorderLayout.SOUTH);
        
        // --- JSplitPane para unir todo ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, panelDerecho);
        splitPane.setDividerLocation(750);
        panelPrincipal.add(splitPane, BorderLayout.CENTER);
        
        add(panelPrincipal);

        // --- Lógica de Componentes ---
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            private void filtrar() {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + txtBuscar.getText()));
            }
        });

        tablaFacturas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarVistaPreviaYBotones();
            }
        });

        btnImprimir.addActionListener(e -> imprimirFacturaSeleccionada());
        btnAnularFactura.addActionListener(e -> anularFacturaSeleccionada());

        actualizarVistaPreviaYBotones(); // Deshabilita botones al inicio
    }

    private void cargarFacturas() {
        modeloTabla.setRowCount(0);
        File carpetaFacturas = PathsHelper.getFacturasFolder();
        File[] archivos = carpetaFacturas.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        if (archivos == null) return;

        for (File archivo : archivos) {
            try {
                List<String> lineas = Files.readAllLines(archivo.toPath());
                String estado = archivo.getName().startsWith("ANULADA_") ? "Anulada" : "Vigente";
                String numeroFactura = "N/A", fecha = "N/A", cliente = "N/A";
                double total = 0.0;

                for (String linea : lineas) {
                    if (linea.contains("FACTURA DE VENTA:")) numeroFactura = linea.split(":")[1].replace("===", "").trim();
                    if (linea.startsWith("Fecha:")) fecha = linea.split(":")[1].trim();
                    if (linea.startsWith("Cliente:")) cliente = linea.split(":")[1].trim();
                    if (linea.startsWith("TOTAL A PAGAR:")) {
                        String totalStr = linea.replaceAll("[^\\d]", "");
                        if (!totalStr.isEmpty()) total = Double.parseDouble(totalStr);
                    }
                }
                modeloTabla.addRow(new Object[]{estado, numeroFactura, fecha, cliente, formatoPesos.format(total), archivo.getName()});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void actualizarVistaPreviaYBotones() {
        int filaVista = tablaFacturas.getSelectedRow();
        if (filaVista >= 0) {
            int filaModelo = tablaFacturas.convertRowIndexToModel(filaVista);
            String nombreArchivo = modeloTabla.getValueAt(filaModelo, 5).toString();
            File archivoCompleto = PathsHelper.getFacturaTXT(nombreArchivo);
            
            try {
                areaVistaPrevia.setText(Files.readString(archivoCompleto.toPath()));
                areaVistaPrevia.setCaretPosition(0);
            } catch (Exception e) {
                areaVistaPrevia.setText("Error al leer el archivo:\n" + e.getMessage());
            }

            String estado = modeloTabla.getValueAt(filaModelo, 0).toString();
            btnAnularFactura.setEnabled("Vigente".equals(estado));
        } else {
            areaVistaPrevia.setText("Seleccione una factura de la tabla para ver sus detalles.");
            btnAnularFactura.setEnabled(false);
        }
    }

    // Reemplaza este método completo en ConsultarFacturasGUI.java
    private void anularFacturaSeleccionada() {
        int filaVista = tablaFacturas.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una factura para anular.", "Ninguna Factura Seleccionada", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int filaModelo = tablaFacturas.convertRowIndexToModel(filaVista);
        String numFactura = modeloTabla.getValueAt(filaModelo, 1).toString();

        // --- INICIO DE LA VERIFICACIÓN DE SEGURIDAD ---
        File carpetaDevoluciones = PathsHelper.getDevolucionesFolder();
        File[] devolucionesExistentes = carpetaDevoluciones.listFiles(
            // Busca cualquier archivo que empiece con "devolucion_" seguido del número de factura
            (dir, name) -> name.startsWith("devolucion_" + numFactura)
        );

        // Si se encontró uno o más archivos de devolución...
        if (devolucionesExistentes != null && devolucionesExistentes.length > 0) {
            JOptionPane.showMessageDialog(this,
                "ACCIÓN BLOQUEADA:\n\n" +
                "Esta factura no se puede anular porque ya tiene una o más devoluciones procesadas.\n" +
                "Anularla ahora crearía una inconsistencia en el inventario.",
                "Anulación No Permitida",
                JOptionPane.ERROR_MESSAGE);
            return; // Detiene completamente el proceso de anulación
        }
        // --- FIN DE LA VERIFICACIÓN DE SEGURIDAD ---

        int respuesta = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de que desea anular esta factura?\n" +
            "Los productos de esta venta serán DEVUELTOS al inventario.",
            "Confirmar Anulación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (respuesta == JOptionPane.YES_OPTION) {
            // El resto de tu código para anular la factura y devolver el stock no cambia
            String nombreArchivo = modeloTabla.getValueAt(filaModelo, 5).toString();
            File archivoActual = PathsHelper.getFacturaTXT(nombreArchivo);

            try {
                List<Producto> inventario = ProductoStorage.cargarProductos();
                List<String> lineasFactura = Files.readAllLines(archivoActual.toPath());

                for (String linea : lineasFactura) {
                    if (linea.contains("#venta_u:")) {
                        String parteVisible = linea.split("#")[0];
                        String codigoProducto = parteVisible.substring(parteVisible.indexOf('[') + 1, parteVisible.indexOf(']')).trim();
                        
                        int indiceIgual = parteVisible.lastIndexOf(" = ");
                        int indiceUltimaX = parteVisible.lastIndexOf(" x ", indiceIgual);
                        String cantidadConUnidad = parteVisible.substring(indiceUltimaX + 3, indiceIgual).trim();
                        String cantidadNumericaStr = cantidadConUnidad.split(" ")[0].replace(',', '.');
                        double cantidadDevuelta = Double.parseDouble(cantidadNumericaStr);

                        for (Producto p : inventario) {
                            if (p.getCodigo().equals(codigoProducto)) {
                                p.setCantidad(p.getCantidad() + cantidadDevuelta);
                                break; 
                            }
                        }
                    }
                }
                ProductoStorage.sobrescribirInventario(inventario);

                File archivoAnulado = PathsHelper.getFacturaTXT("ANULADA_" + nombreArchivo);
                Files.move(archivoActual.toPath(), archivoAnulado.toPath());

                modeloTabla.setValueAt("Anulada", filaModelo, 0);
                modeloTabla.setValueAt(archivoAnulado.getName(), filaModelo, 5);
                
                JOptionPane.showMessageDialog(this, "Factura anulada y stock devuelto al inventario.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                actualizarVistaPreviaYBotones();

                if (DashboardGUI.getInstance() != null) {
                    DashboardGUI.getInstance().refrescarDatos();
                }
                
                if (owner != null) {
                    owner.refrescarListaProductos();
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al anular la factura: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    // El método de imprimir no cambia, solo asegúrate de que exista en esta clase
    private void imprimirFacturaSeleccionada() {
        int filaVista = tablaFacturas.getSelectedRow();
        if (filaVista >= 0) {
            int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Desea enviar una copia de esta factura a la impresora?",
                "Confirmar Impresión", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            
            if (respuesta == JOptionPane.YES_OPTION) {
                int filaModelo = tablaFacturas.convertRowIndexToModel(filaVista);
                String nombreArchivo = modeloTabla.getValueAt(filaModelo, 5).toString();
                File archivoParaImprimir = PathsHelper.getFacturaTXT(nombreArchivo);
                if (archivoParaImprimir.exists()) {
                    if (owner != null) {
                        owner.imprimirFactura(archivoParaImprimir);
                    } else {
                        // Lógica de impresión genérica si no hay owner
                        FacturaPrinter.imprimirContenido(areaVistaPrevia.getText());
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "No se encontró el archivo de la factura.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    // Clase interna para colorear las filas según el estado
    class EstadoFacturaRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                String estado = (String) table.getModel().getValueAt(table.convertRowIndexToModel(row), 0);
                if ("Anulada".equals(estado)) {
                    c.setBackground(new Color(255, 228, 225)); // Rojo suave
                    c.setForeground(Color.GRAY);
                } else {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }
            }
            // Pone el texto en negrita solo para la columna "Estado"
            if (column == 0) {
                c.setFont(new Font(c.getFont().getName(), Font.BOLD, c.getFont().getSize()));
            }
            return c;
        }
    }
}