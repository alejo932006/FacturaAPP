import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class ConsultarDevolucionesGUI extends JDialog {

    private JTable tablaDevoluciones;
    private DefaultTableModel modeloTabla;
    private TableRowSorter<DefaultTableModel> sorter;

    public ConsultarDevolucionesGUI(Frame owner) {
        super(owner, "Consultar Historial de Devoluciones", true);
        setSize(800, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        cargarDevoluciones();
    }

    private void initComponents() {
        // --- Panel de Búsqueda ---
        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelBusqueda.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JTextField txtBuscar = new JTextField(30);
        panelBusqueda.add(new JLabel("Buscar por Factura, Producto o Fecha:"));
        panelBusqueda.add(txtBuscar);

        // --- Tabla de Resultados ---
        String[] columnas = {"Fecha", "Nº Factura", "Producto", "Código", "Valor Devuelto", "Nombre Archivo"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDevoluciones = new JTable(modeloTabla);
        sorter = new TableRowSorter<>(modeloTabla);
        tablaDevoluciones.setRowSorter(sorter);
        tablaDevoluciones.getColumnModel().getColumn(5).setMinWidth(0);
        tablaDevoluciones.getColumnModel().getColumn(5).setMaxWidth(0);
        tablaDevoluciones.getColumnModel().getColumn(5).setWidth(0);

        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            private void filtrar() {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + txtBuscar.getText()));
            }
        });

        // --- Panel de Botones ---
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnVerDetalle = new JButton("Ver Detalle de Devolución");
        panelBotones.add(btnVerDetalle);

        btnVerDetalle.addActionListener(e -> verDetalleSeleccionado());

        // --- Ensamblaje ---
        add(panelBusqueda, BorderLayout.NORTH);
        add(new JScrollPane(tablaDevoluciones), BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);
    }

    // Reemplaza este método completo en ConsultarDevolucionesGUI.java
    private void cargarDevoluciones() {
        modeloTabla.setRowCount(0);
        File carpetaDevoluciones = PathsHelper.getDevolucionesFolder();
        File[] archivos = carpetaDevoluciones.listFiles((dir, name) -> name.startsWith("devolucion_"));

        if (archivos == null) return;

        for (File archivo : archivos) {
            try {
                List<String> lineas = Files.readAllLines(archivo.toPath());
                String fecha = lineas.stream().filter(l -> l.startsWith("Fecha de Devolución:")).findFirst().orElse("").split(":")[1].trim();
                String numFactura = lineas.stream().filter(l -> l.startsWith("Número de Factura:")).findFirst().orElse("").split(":")[1].trim();
                String producto = lineas.stream().filter(l -> l.startsWith("Producto:")).findFirst().orElse("").split(":")[1].trim();
                String codigo = lineas.stream().filter(l -> l.startsWith("Código:")).findFirst().orElse("").split(":")[1].trim();
                String valor = lineas.stream().filter(l -> l.startsWith("Valor Devuelto:")).findFirst().orElse("").split(":")[1].trim();
                
                // Añadimos el nombre del archivo al final, en la columna oculta
                modeloTabla.addRow(new Object[]{fecha, numFactura, producto, codigo, "$" + valor, archivo.getName()});

            } catch (Exception e) {
                System.err.println("Error al leer el archivo de devolución: " + archivo.getName());
            }
        }
    }

    // Reemplaza este método completo en ConsultarDevolucionesGUI.java
    private void verDetalleSeleccionado() {
        int filaSeleccionada = tablaDevoluciones.getSelectedRow();
        if (filaSeleccionada >= 0) {
            int filaModelo = tablaDevoluciones.convertRowIndexToModel(filaSeleccionada);
            // Leemos el nombre del archivo de la columna 5 (la que ocultamos)
            String nombreArchivo = modeloTabla.getValueAt(filaModelo, 5).toString();
            
            File archivoCompleto = new File(PathsHelper.getDevolucionesFolder(), nombreArchivo);

            if (archivoCompleto.exists()) {
                new VerFacturaGUI(this, archivoCompleto);
            } else {
                JOptionPane.showMessageDialog(this, "No se encontró el archivo de detalle para esta devolución.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una devolución de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }
}