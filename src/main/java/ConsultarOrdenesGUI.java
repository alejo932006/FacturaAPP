import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.table.DefaultTableCellRenderer;

public class ConsultarOrdenesGUI extends JFrame {

    private JTable tablaOrdenes;
    private DefaultTableModel modeloTabla;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextArea areaVistaPrevia; // <-- NUEVO: Panel para previsualizar
    
    // Botones (ahora variables de clase para poder habilitarlos/deshabilitarlos)
    private JButton btnGestionarAbonos;
    private JButton btnVerOrden;
    private JButton btnImprimirOrden;
    private JButton btnCancelarOrden;
    private JButton btnEliminarOrden;
    private JButton btnEditarOrden;

    public ConsultarOrdenesGUI(Frame owner) {
        super("Consultar Órdenes y Reportes");
        setSize(1200, 700); // Ventana más grande para la nueva interfaz
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initComponents();
        cargarOrdenes();
        setVisible(true);
    }

    private void initComponents() {
        // --- Panel Principal ---
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel Izquierdo (Tabla y Búsqueda) ---
        JPanel panelIzquierdo = new JPanel(new BorderLayout(5, 5));
        
        // Búsqueda
        JPanel panelBusqueda = new JPanel(new BorderLayout(5, 0));
        JTextField txtBuscar = new JTextField();
        panelBusqueda.add(new JLabel("Buscar: "), BorderLayout.WEST);
        panelBusqueda.add(txtBuscar, BorderLayout.CENTER);
        
        // Tabla
        String[] columnas = {"Estado", "Tipo de Orden", "Cliente", "Fecha", "Nombre Archivo"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaOrdenes = new JTable(modeloTabla);
        sorter = new TableRowSorter<>(modeloTabla);
        tablaOrdenes.setRowSorter(sorter);
        tablaOrdenes.getColumnModel().getColumn(4).setMinWidth(0);
        tablaOrdenes.getColumnModel().getColumn(4).setMaxWidth(0);
        tablaOrdenes.setDefaultRenderer(Object.class, new OrderStatusRenderer());

        panelIzquierdo.add(panelBusqueda, BorderLayout.NORTH);
        panelIzquierdo.add(new JScrollPane(tablaOrdenes), BorderLayout.CENTER);

        // --- Panel Derecho (Vista Previa y Botones) ---
        JPanel panelDerecho = new JPanel(new BorderLayout(10, 10));

        // Vista Previa
        areaVistaPrevia = new JTextArea("Seleccione una orden de la tabla para ver su contenido.");
        areaVistaPrevia.setEditable(false);
        areaVistaPrevia.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaVistaPrevia.setMargin(new Insets(5, 5, 5, 5));
        JScrollPane scrollVistaPrevia = new JScrollPane(areaVistaPrevia);
        scrollVistaPrevia.setBorder(BorderFactory.createTitledBorder("Vista Previa"));

        // Panel de Botones
        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new BoxLayout(panelBotones, BoxLayout.Y_AXIS));

        btnVerOrden = new JButton("Ver en Ventana Nueva");
        btnImprimirOrden = new JButton("Imprimir");
        btnEditarOrden = new JButton("Editar Orden");
        btnGestionarAbonos = new JButton("Gestionar Abonos");
        btnCancelarOrden = new JButton("Cancelar Orden");
        btnEliminarOrden = new JButton("Eliminar Orden (Permanente)");
        JButton btnTotalizar = new JButton("Totalizar por Tipo");

        // Agrupamos y añadimos botones
        agregarBotones(panelBotones, "Acciones", btnVerOrden, btnImprimirOrden, btnEditarOrden, btnGestionarAbonos, btnTotalizar);
        panelBotones.add(Box.createRigidArea(new Dimension(0, 20))); // Espacio
        agregarBotones(panelBotones, "Estado", btnCancelarOrden, btnEliminarOrden);

        panelDerecho.add(scrollVistaPrevia, BorderLayout.CENTER);
        panelDerecho.add(panelBotones, BorderLayout.EAST);
        
        // --- JSplitPane para unir todo ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, panelDerecho);
        splitPane.setDividerLocation(650);
        panelPrincipal.add(splitPane, BorderLayout.CENTER);
        
        add(panelPrincipal);

        // --- LÓGICA DE COMPONENTES ---
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            private void filtrar() {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + txtBuscar.getText()));
            }
        });

        // Listener para la tabla (¡la clave para la vista previa!)
        tablaOrdenes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarVistaPreviaYBotones();
            }
        });

        // Listeners de los botones
        btnVerOrden.addActionListener(e -> verOrdenSeleccionada());
        btnImprimirOrden.addActionListener(e -> imprimirOrdenSeleccionada());
        btnEditarOrden.addActionListener(e -> editarOrdenSeleccionada());
        btnGestionarAbonos.addActionListener(e -> gestionarAbonos());
        btnCancelarOrden.addActionListener(e -> cancelarOrdenSeleccionada());
        btnEliminarOrden.addActionListener(e -> eliminarOrdenSeleccionada());
        btnTotalizar.addActionListener(e -> totalizarPorTipo());

        // Deshabilitar botones al inicio
        actualizarVistaPreviaYBotones(); 
    }

    /**
     * Método de ayuda para añadir botones a un panel con un título.
     */
    private void agregarBotones(JPanel panel, String titulo, JButton... botones) {
        JPanel subPanel = new JPanel();
        subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
        subPanel.setBorder(BorderFactory.createTitledBorder(titulo));
        for (JButton boton : botones) {
            boton.setAlignmentX(Component.CENTER_ALIGNMENT);
            subPanel.add(boton);
            subPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        panel.add(subPanel);
    }

    /**
     * NUEVO MÉTODO CENTRAL: Se ejecuta cada vez que se selecciona una fila.
     * Actualiza tanto la vista previa como el estado de los botones.
     */
    private void actualizarVistaPreviaYBotones() {
        int filaSeleccionada = tablaOrdenes.getSelectedRow();
        if (filaSeleccionada >= 0) {
            int filaModelo = tablaOrdenes.convertRowIndexToModel(filaSeleccionada);
            String nombreArchivo = modeloTabla.getValueAt(filaModelo, 4).toString();
            File archivoCompleto = new File(PathsHelper.getOrdenesFolder(), nombreArchivo);
            try {
                areaVistaPrevia.setText(Files.readString(archivoCompleto.toPath()));
                areaVistaPrevia.setCaretPosition(0); // Mover scroll al inicio
            } catch (IOException e) {
                areaVistaPrevia.setText("Error al leer el archivo:\n" + e.getMessage());
            }

            // Lógica para habilitar/deshabilitar botones
            String estado = modeloTabla.getValueAt(filaModelo, 0).toString();
            String tipo = modeloTabla.getValueAt(filaModelo, 1).toString();
            boolean esActiva = estado.equals("Activa");
            
            btnVerOrden.setEnabled(true);
            btnImprimirOrden.setEnabled(true);
            btnEditarOrden.setEnabled(esActiva);
            btnGestionarAbonos.setEnabled(esActiva && tipo.equals("Recibo de Abono/Separado"));
            btnCancelarOrden.setEnabled(esActiva);
            btnEliminarOrden.setEnabled(true); // Siempre se puede eliminar

        } else {
            areaVistaPrevia.setText("Seleccione una orden de la tabla para ver su contenido.");
            // Deshabilitar todos los botones si no hay selección
            btnVerOrden.setEnabled(false);
            btnImprimirOrden.setEnabled(false);
            btnEditarOrden.setEnabled(false);
            btnGestionarAbonos.setEnabled(false);
            btnCancelarOrden.setEnabled(false);
            btnEliminarOrden.setEnabled(false);
        }
    }

    // El resto de los métodos (cargarOrdenes, imprimir, etc.) no necesitan cambios drásticos
    // y se mantienen igual, ya que la lógica central no cambia.
    
    public void cargarOrdenes() {
        modeloTabla.setRowCount(0);
        File carpetaOrdenes = PathsHelper.getOrdenesFolder();
        if (!carpetaOrdenes.exists()) return;
        File[] archivos = carpetaOrdenes.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        if (archivos == null) return;
        for (File archivo : archivos) {
            String nombre = archivo.getName();
            String estado;
            if (nombre.startsWith("CANCELADA_")) {
                estado = "Cancelada";
            } else if (nombre.startsWith("COMPLETADA_")) {
                estado = "Completada";
            } else {
                estado = "Activa";
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                String linea;
                String fecha = "N/A", cliente = "N/A";
                while ((linea = reader.readLine()) != null) {
                    if (linea.startsWith("Fecha:")) fecha = linea.split(":")[1].trim();
                    if (linea.startsWith("Nombre:")) {
                        cliente = linea.split(":")[1].trim();
                        break;
                    }
                }
                String tipo = obtenerTipoOrdenDelNombreArchivo(nombre);
                modeloTabla.addRow(new Object[]{estado, tipo, cliente, fecha, archivo.getName()});
            } catch (IOException e) {
                // Manejar error
            }
        }
    }

    private void gestionarAbonos() {
        int filaSeleccionada = tablaOrdenes.getSelectedRow();
        if (filaSeleccionada >= 0) {
            int filaModelo = tablaOrdenes.convertRowIndexToModel(filaSeleccionada);
            String nombreArchivo = modeloTabla.getValueAt(filaModelo, 4).toString();
            File archivoCompleto = new File(PathsHelper.getOrdenesFolder(), nombreArchivo);
            new GestionAbonosGUI(this, archivoCompleto).setVisible(true);
        }
    }

    private void verOrdenSeleccionada() {
        int filaSeleccionada = tablaOrdenes.getSelectedRow();
        if (filaSeleccionada >= 0) {
            int filaModelo = tablaOrdenes.convertRowIndexToModel(filaSeleccionada);
            String nombreArchivo = modeloTabla.getValueAt(filaModelo, 4).toString();
            File archivoCompleto = new File(PathsHelper.getOrdenesFolder(), nombreArchivo);
            VerFacturaGUI viewer = new VerFacturaGUI(this, archivoCompleto);
            viewer.setTitle("Detalle de Orden: " + nombreArchivo);
        }
    }

    private void cancelarOrdenSeleccionada() {
        int filaSeleccionada = tablaOrdenes.getSelectedRow();
        if (filaSeleccionada < 0) return;
        
        int filaModelo = tablaOrdenes.convertRowIndexToModel(filaSeleccionada);
        String estadoActual = modeloTabla.getValueAt(filaModelo, 0).toString();
        if (!estadoActual.equals("Activa")) {
            JOptionPane.showMessageDialog(this, "Esta orden no se puede cancelar porque no está activa.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(this, "¿Está seguro de que desea cancelar esta orden?", "Confirmar Cancelación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (respuesta == JOptionPane.YES_OPTION) {
            String nombreArchivo = modeloTabla.getValueAt(filaModelo, 4).toString();
            File archivoActual = new File(PathsHelper.getOrdenesFolder(), nombreArchivo);
            File archivoCancelado = new File(PathsHelper.getOrdenesFolder(), "CANCELADA_" + nombreArchivo);
            if (archivoActual.renameTo(archivoCancelado)) {
                cargarOrdenes(); // Recargamos toda la tabla para reflejar el cambio
                JOptionPane.showMessageDialog(this, "La orden ha sido cancelada.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo cancelar la orden.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void imprimirOrdenSeleccionada() {
        int filaSeleccionada = tablaOrdenes.getSelectedRow();
        if (filaSeleccionada >= 0) {
            try {
                String contenidoOrden = areaVistaPrevia.getText();
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
                job.setPrintable(new FacturaPrinter(contenidoOrden), pageFormat);
                if (job.printDialog()) {
                    job.print();
                }
            } catch (java.awt.print.PrinterException e) {
                JOptionPane.showMessageDialog(this, "Error de Impresión: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void eliminarOrdenSeleccionada() {
        int filaSeleccionada = tablaOrdenes.getSelectedRow();
        if (filaSeleccionada < 0) return;

        int filaModelo = tablaOrdenes.convertRowIndexToModel(filaSeleccionada);
        String nombreArchivo = modeloTabla.getValueAt(filaModelo, 4).toString();
        int respuesta = JOptionPane.showConfirmDialog(this, "¿Está seguro de que desea eliminar permanentemente esta orden?", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        if (respuesta == JOptionPane.YES_OPTION) {
            File archivoAEliminar = new File(PathsHelper.getOrdenesFolder(), nombreArchivo);
            if (archivoAEliminar.delete()) {
                modeloTabla.removeRow(filaModelo);
                JOptionPane.showMessageDialog(this, "La orden ha sido eliminada.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo eliminar el archivo.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void editarOrdenSeleccionada() {
        int filaSeleccionada = tablaOrdenes.getSelectedRow();
        if (filaSeleccionada >= 0) {
            int filaModelo = tablaOrdenes.convertRowIndexToModel(filaSeleccionada);
            String nombreArchivo = modeloTabla.getValueAt(filaModelo, 4).toString();
            File archivoCompleto = new File(PathsHelper.getOrdenesFolder(), nombreArchivo);
            new GeneradorReportesGUI(this, archivoCompleto).setVisible(true);
            cargarOrdenes(); 
        }
    }

    private String obtenerTipoOrdenDelNombreArchivo(String nombreArchivo) {
        if (nombreArchivo.startsWith("CANCELADA_")) nombreArchivo = nombreArchivo.substring(10);
        if (nombreArchivo.startsWith("COMPLETADA_")) nombreArchivo = nombreArchivo.substring(11);
        int indiceGuion = nombreArchivo.indexOf("_");
        if (indiceGuion != -1) {
            String tipo = nombreArchivo.substring(0, indiceGuion);
            if ("OrdendeReparacion".equalsIgnoreCase(tipo)) return "Orden de Reparación";
            if ("ReciboDeAbonoSeparado".equalsIgnoreCase(tipo)) return "Recibo de Abono/Separado";
            if ("NotadeCredito".equalsIgnoreCase(tipo)) return "Nota de Crédito";
            if ("ReporteGeneral".equalsIgnoreCase(tipo)) return "Reporte General";
        }
        return "N/A";
    }

    private void totalizarPorTipo() {
        String[] opciones = {"Recibo de Abono/Separado", "Nota de Crédito", "Orden de Reparación", "Reporte General"};
        String tipoSeleccionado = (String) JOptionPane.showInputDialog(this, "Seleccione el tipo de orden a totalizar:", "Totalizar Órdenes", JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        if (tipoSeleccionado == null) return;

        double valorTotalAcumulado = 0.0;
        int contadorOrdenes = 0;
        String tipoArchivo = tipoSeleccionado.replace(" ", "").replace("/", "").replace("ó", "o").replace("é", "e");

        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            String nombreArchivo = modeloTabla.getValueAt(i, 4).toString();
            if (nombreArchivo.toLowerCase().contains(tipoArchivo.toLowerCase())) {
                File archivoCompleto = new File(PathsHelper.getOrdenesFolder(), nombreArchivo);
                try {
                    if (tipoSeleccionado.equals("Recibo de Abono/Separado")) {
                        double valorAsociado = 0;
                        double totalAbonado = 0;
                        List<String> lineas = Files.readAllLines(archivoCompleto.toPath());
                        for (String linea : lineas) {
                            if (linea.startsWith("VALOR ASOCIADO:")) valorAsociado = Double.parseDouble(linea.replaceAll("[^\\d]", ""));
                            if (linea.contains(";$")) totalAbonado += Double.parseDouble(linea.split(";" )[1].replaceAll("[^\\d]", ""));
                        }
                        valorTotalAcumulado += (valorAsociado - totalAbonado);
                        contadorOrdenes++;
                    } else {
                        List<String> lineas = Files.readAllLines(archivoCompleto.toPath());
                        for (String linea : lineas) {
                            if (linea.startsWith("VALOR ASOCIADO:")) {
                                valorTotalAcumulado += Double.parseDouble(linea.replaceAll("[^\\d]", ""));
                                contadorOrdenes++;
                                break; 
                            }
                        }
                    }
                } catch (IOException | NumberFormatException e) {
                    System.err.println("No se pudo procesar el archivo: " + nombreArchivo);
                }
            }
        }

        NumberFormat formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        formatoPesos.setMaximumFractionDigits(0);
        String textoResultado = tipoSeleccionado.equals("Recibo de Abono/Separado") ? "El saldo pendiente total es:" : "El valor total acumulado es:";
        String mensaje = String.format("Se encontraron %d órdenes del tipo '%s'.\n\n%s\n%s", contadorOrdenes, tipoSeleccionado, textoResultado, formatoPesos.format(valorTotalAcumulado));
        JOptionPane.showMessageDialog(this, mensaje, "Resultado de la Totalización", JOptionPane.INFORMATION_MESSAGE);
    }
    
    // El renderer no cambia
    class OrderStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) return c;
            String estado = (String) table.getModel().getValueAt(table.convertRowIndexToModel(row), 0);
            c.setFont(new Font(c.getFont().getName(), Font.PLAIN, c.getFont().getSize()));
            c.setForeground(table.getForeground());
            c.setBackground(table.getBackground());
            if (column == 0) {
                c.setFont(new Font(c.getFont().getName(), Font.BOLD, c.getFont().getSize()));
                if ("Cancelada".equals(estado)) {
                    c.setBackground(new Color(255, 228, 225));
                    c.setForeground(new Color(156, 0, 0));
                } else if ("Completada".equals(estado)) {
                    c.setBackground(new Color(223, 240, 216));
                    c.setForeground(new Color(60, 118, 61));
                } else {
                    c.setBackground(new Color(217, 237, 247));
                    c.setForeground(new Color(49, 112, 143));
                }
            }
            return c;
        }
    }
}