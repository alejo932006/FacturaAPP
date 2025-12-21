import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

public class EliminarProductoFacturaGUI extends JDialog {

    private JTable tablaProductosFactura;
    private DefaultTableModel modeloTabla;
    private Factura factura;
    private FacturaGUI facturaGUI; // Referencia a la ventana principal

    public EliminarProductoFacturaGUI(FacturaGUI owner, Factura factura) {
        // Hacemos que la ventana sea modal (bloquea la ventana principal)
        super(owner, "Eliminar Productos de la Factura", true);

        this.facturaGUI = owner;
        this.factura = factura;

        setSize(500, 350);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        initComponents();
        cargarProductosEnTabla();
    }

    private void initComponents() {
        // Modelo de la tabla
        String[] columnas = {"Producto", "Cantidad", "Subtotal"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            // Hacemos que las celdas no sean editables
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaProductosFactura = new JTable(modeloTabla);
        JScrollPane scrollPane = new JScrollPane(tablaProductosFactura);

        add(scrollPane, BorderLayout.CENTER);

        // Panel de botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnEliminar = new JButton("Eliminar Seleccionado");
        JButton btnCerrar = new JButton("Cerrar");

        panelBotones.add(btnEliminar);
        panelBotones.add(btnCerrar);

        add(panelBotones, BorderLayout.SOUTH);

        // --- ACCIONES DE LOS BOTONES ---

        btnCerrar.addActionListener(e -> dispose()); // Cierra la ventana

        btnEliminar.addActionListener(e -> {
            int filaSeleccionada = tablaProductosFactura.getSelectedRow();

            if (filaSeleccionada >= 0) {
                // Obtenemos el detalle de la factura de la fila seleccionada
                DetalleFactura detalleAEliminar = factura.getDetalles().get(filaSeleccionada);

                // Pedimos a la ventana principal que maneje la lógica de eliminación
                facturaGUI.procesarEliminacionProducto(detalleAEliminar);

                // Eliminamos la fila de nuestra tabla para reflejar el cambio
                modeloTabla.removeRow(filaSeleccionada);
                
                JOptionPane.showMessageDialog(this, "Producto eliminado de la factura.");

            } else {
                JOptionPane.showMessageDialog(this, "Por favor, seleccione un producto de la lista para eliminar.", "Ningún producto seleccionado", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private void cargarProductosEnTabla() {
        modeloTabla.setRowCount(0);
    
        List<DetalleFactura> detalles = factura.getDetalles();
        NumberFormat formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    
        for (DetalleFactura detalle : detalles) {
            String nombreProducto = detalle.getProducto().getNombre();
            // --- CAMBIO AQUÍ ---
            double cantidad = detalle.getCantidad(); // <-- De int a double
            // --- FIN DEL CAMBIO ---
            String subtotal = formatoPesos.format(detalle.getSubtotal());
    
            modeloTabla.addRow(new Object[]{nombreProducto, cantidad, subtotal});
        }
    }
}