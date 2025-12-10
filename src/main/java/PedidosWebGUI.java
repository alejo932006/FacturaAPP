import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONObject;

public class PedidosWebGUI extends JFrame {

    private JTable tablaPedidos;
    private DefaultTableModel modeloTabla;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField txtBuscar; // Campo de búsqueda
    
    // Cambiamos el JTextArea por un JPanel dentro de un ScrollPane para elementos visuales
    private JPanel panelDetalleContent; 
    private JLabel lblInfoPedido; // Cabecera del detalle

    private PedidoWebStorage storage;
    private List<PedidoWeb> listaPedidosActual;

    // Botones de acción
    private JButton btnDespachar;
    private JButton btnCancelar;
    private JButton btnEliminar;
    private JButton btnActualizar;

    public PedidosWebGUI() {
        super("Gestión de Pedidos Web");
        setSize(1200, 750); // Un poco más grande para ver imágenes
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        storage = new PedidoWebStorage();
        initComponents();
        cargarPedidos();
        setVisible(true);
    }

    private void initComponents() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel Izquierdo: Buscador y Tabla ---
        JPanel panelIzquierdo = new JPanel(new BorderLayout(5, 5));
        
        // 1. Buscador
        JPanel panelBusqueda = new JPanel(new BorderLayout(5, 5));
        panelBusqueda.add(new JLabel("Buscar:"), BorderLayout.WEST);
        txtBuscar = new JTextField();
        txtBuscar.putClientProperty("JTextField.placeholderText", "Filtrar por cliente, estado...");
        panelBusqueda.add(txtBuscar, BorderLayout.CENTER);
        
        // 2. Tabla
        String[] columnas = {"ID", "Estado", "Cliente", "Fecha", "Total", "Pago"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaPedidos = new JTable(modeloTabla);
        sorter = new TableRowSorter<>(modeloTabla);
        tablaPedidos.setRowSorter(sorter);
        tablaPedidos.setDefaultRenderer(Object.class, new EstadoPedidoRenderer());

        panelIzquierdo.add(panelBusqueda, BorderLayout.NORTH);
        panelIzquierdo.add(new JScrollPane(tablaPedidos), BorderLayout.CENTER);

        // --- Panel Derecho: Detalle Visual ---
        JPanel panelDerecho = new JPanel(new BorderLayout(10, 10));
        panelDerecho.setPreferredSize(new Dimension(500, 0)); // Más ancho para las fotos
        panelDerecho.setBorder(BorderFactory.createTitledBorder("Detalle del Pedido"));

        // Cabecera del detalle (Datos del cliente)
        lblInfoPedido = new JLabel("Seleccione un pedido para ver el detalle.");
        lblInfoPedido.setVerticalAlignment(SwingConstants.TOP);
        lblInfoPedido.setBorder(new EmptyBorder(5, 5, 10, 5));
        
        // Contenedor de productos (Lista vertical)
        panelDetalleContent = new JPanel();
        panelDetalleContent.setLayout(new BoxLayout(panelDetalleContent, BoxLayout.Y_AXIS));
        panelDetalleContent.setBackground(Color.WHITE);
        
        JScrollPane scrollDetalle = new JScrollPane(panelDetalleContent);
        scrollDetalle.getVerticalScrollBar().setUnitIncrement(16);

        // Panel de Botones
        JPanel panelBotones = new JPanel(new GridLayout(1, 4, 5, 5));
        btnDespachar = new JButton("Despachar");
        btnCancelar = new JButton("Cancelar");
        btnEliminar = new JButton("Eliminar");
        btnActualizar = new JButton("Actualizar");

        styleButton(btnDespachar, new Color(40, 167, 69), Color.WHITE);
        styleButton(btnCancelar, new Color(255, 193, 7), Color.BLACK);
        styleButton(btnEliminar, new Color(220, 53, 69), Color.WHITE);
        styleButton(btnActualizar, new Color(108, 117, 125), Color.WHITE);

        panelBotones.add(btnDespachar);
        panelBotones.add(btnCancelar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnActualizar);

        panelDerecho.add(lblInfoPedido, BorderLayout.NORTH);
        panelDerecho.add(scrollDetalle, BorderLayout.CENTER);
        panelDerecho.add(panelBotones, BorderLayout.SOUTH);

        // Split Pane
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, panelDerecho);
        split.setDividerLocation(600);
        panelPrincipal.add(split, BorderLayout.CENTER);
        add(panelPrincipal);

        // --- Eventos ---
        // Listener de búsqueda
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
        });

        tablaPedidos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) mostrarDetalleSeleccionado();
        });

        btnActualizar.addActionListener(e -> cargarPedidos());
        btnDespachar.addActionListener(e -> cambiarEstado("DESPACHADO"));
        btnCancelar.addActionListener(e -> cambiarEstado("CANCELADO"));
        btnEliminar.addActionListener(e -> eliminarPedido());
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
    }

    private void filtrar() {
        String texto = txtBuscar.getText();
        if (texto.trim().length() == 0) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto));
        }
    }

    private void cargarPedidos() {
        modeloTabla.setRowCount(0);
        listaPedidosActual = storage.obtenerPedidos();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        // CORREGIDO: Uso de Locale.of para Java 21 o Locale.forLanguageTag
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));

        for (PedidoWeb p : listaPedidosActual) {
            modeloTabla.addRow(new Object[]{
                p.getId(),
                p.getEstado(),
                p.getClienteNombre(),
                sdf.format(p.getFechaPedido()),
                nf.format(p.getTotalVenta()),
                p.getMetodoPago()
            });
        }
    }

    private void mostrarDetalleSeleccionado() {
        int row = tablaPedidos.getSelectedRow();
        panelDetalleContent.removeAll(); // Limpiar productos anteriores
        
        if (row < 0) {
            lblInfoPedido.setText("Seleccione un pedido para ver el detalle.");
            panelDetalleContent.revalidate();
            panelDetalleContent.repaint();
            return;
        }
        
        int id = (int) tablaPedidos.getValueAt(row, 0);
        PedidoWeb pedido = listaPedidosActual.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
        
        if (pedido != null) {
            // 1. Mostrar cabecera del pedido (HTML para formateo fácil)
            String htmlInfo = "<html><body style='width: 350px'>"
                    + "<b>Cliente:</b> " + pedido.getClienteNombre() + "<br>"
                    + "<b>Teléfono:</b> " + pedido.getClienteTelefono() + "<br>"
                    + "<b>Dirección:</b> " + pedido.getClienteDireccion() + "<br>"
                    + "<b>Email:</b> " + pedido.getClienteEmail() + "<br>"
                    + "<b>Método Pago:</b> " + pedido.getMetodoPago() + "<br>"
                    + "<h3 style='color:blue'>Total: " + NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).format(pedido.getTotalVenta()) + "</h3>"
                    + "</body></html>";
            lblInfoPedido.setText(htmlInfo);

            // 2. Renderizar lista de productos
            try {
                JSONArray productos = new JSONArray(pedido.getDetalleProductos());
                for (int i = 0; i < productos.length(); i++) {
                    JSONObject item = productos.getJSONObject(i);
                    agregarTarjetaProducto(item);
                }
            } catch (Exception e) {
                JLabel lblError = new JLabel("Error leyendo productos: " + e.getMessage());
                lblError.setForeground(Color.RED);
                panelDetalleContent.add(lblError);
            }
        }
        panelDetalleContent.revalidate();
        panelDetalleContent.repaint();
    }

    private void agregarTarjetaProducto(JSONObject item) {
        // Datos del JSON
        String nombre = item.optString("nombre", "Sin Nombre");
        String codigo = item.optString("id", "N/A"); // <-- AQUÍ OBTENEMOS EL CÓDIGO
        double precio = item.optDouble("precio", 0.0);
        double qty = item.optDouble("qty", 1.0);
        String urlImagen = item.optString("imagen_url", "");

        // Panel Tarjeta
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(Color.WHITE);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110)); // Altura fija aprox

        // --- Imagen ---
        JLabel lblImagen = new JLabel();
        lblImagen.setPreferredSize(new Dimension(80, 80));
        lblImagen.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        lblImagen.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagen.setText("Cargando...");
        
        // Cargar imagen en hilo separado para no congelar la UI
        if (!urlImagen.isEmpty()) {
            new SwingWorker<ImageIcon, Void>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    try {
                        // CORRECCIÓN: Usar URI.create().toURL() en lugar de new URL()
                        URL url = java.net.URI.create(urlImagen).toURL();
                        BufferedImage image = ImageIO.read(url);
                        if (image != null) {
                            // Escalar imagen suavemente
                            Image scaled = image.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                            return new ImageIcon(scaled);
                        }
                    } catch (Exception e) {
                        System.err.println("Error cargando imagen: " + urlImagen + " -> " + e.getMessage());
                    }
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        ImageIcon icon = get();
                        if (icon != null) {
                            lblImagen.setText("");
                            lblImagen.setIcon(icon);
                        } else {
                            lblImagen.setText("Sin Foto");
                        }
                    } catch (Exception e) { lblImagen.setText("Error"); }
                }
            }.execute();
        } else {
            lblImagen.setText("No URL");
        }

        // --- Texto ---
        String htmlTexto = "<html>"
                + "<b>" + nombre + "</b><br>"
                + "<font color='gray'>Cód: " + codigo + "</font><br>" // <-- MOSTRAMOS EL CÓDIGO
                + "Cant: <b>" + qty + "</b> &nbsp;|&nbsp; "
                + "Precio: " + NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).format(precio)
                + "</html>";
        
        JLabel lblTexto = new JLabel(htmlTexto);

        card.add(lblImagen, BorderLayout.WEST);
        card.add(lblTexto, BorderLayout.CENTER);

        panelDetalleContent.add(card);
    }

    private void cambiarEstado(String nuevoEstado) {
        int row = tablaPedidos.getSelectedRow();
        if (row < 0) return;
        int id = (int) tablaPedidos.getValueAt(row, 0);
        if (storage.actualizarEstado(id, nuevoEstado)) {
            cargarPedidos();
            panelDetalleContent.removeAll();
            lblInfoPedido.setText("Seleccione un pedido...");
            panelDetalleContent.repaint();
        }
    }

    private void eliminarPedido() {
        int row = tablaPedidos.getSelectedRow();
        if (row < 0) return;
        int id = (int) tablaPedidos.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar pedido?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            storage.eliminarPedido(id);
            cargarPedidos();
            panelDetalleContent.removeAll();
            lblInfoPedido.setText("Seleccione un pedido...");
            panelDetalleContent.repaint();
        }
    }
    
    // Renderizador para colorear filas
    class EstadoPedidoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) return c;
            String estado = (String) table.getModel().getValueAt(table.convertRowIndexToModel(row), 1);
            if ("DESPACHADO".equalsIgnoreCase(estado)) c.setBackground(new Color(212, 237, 218));
            else if ("CANCELADO".equalsIgnoreCase(estado)) c.setBackground(new Color(248, 215, 218));
            else c.setBackground(Color.WHITE);
            c.setForeground(Color.BLACK);
            return c;
        }
    }
}