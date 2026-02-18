import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.HttpURLConnection;
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
    private JTextField txtBuscar; 
    
    private JPanel panelDetalleContent; 
    private JLabel lblInfoPedido; 

    private PedidoWebStorage storage;
    private List<PedidoWeb> listaPedidosActual;

    private JButton btnDespachar;
    private JButton btnCancelar;
    private JButton btnEliminar;
    private JButton btnActualizar;
    private JButton btnFacturar; 

    public PedidosWebGUI() {
        super("Gestión de Pedidos Web");
        setSize(1200, 750); 
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

        // --- Panel Izquierdo ---
        JPanel panelIzquierdo = new JPanel(new BorderLayout(5, 5));
        
        JPanel panelBusqueda = new JPanel(new BorderLayout(5, 5));
        panelBusqueda.add(new JLabel("Buscar:"), BorderLayout.WEST);
        txtBuscar = new JTextField();
        txtBuscar.putClientProperty("JTextField.placeholderText", "Filtrar por cliente, estado...");
        panelBusqueda.add(txtBuscar, BorderLayout.CENTER);
        
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

        // --- Panel Derecho ---
        JPanel panelDerecho = new JPanel(new BorderLayout(10, 10));
        panelDerecho.setPreferredSize(new Dimension(500, 0)); 
        panelDerecho.setBorder(BorderFactory.createTitledBorder("Detalle del Pedido"));

        lblInfoPedido = new JLabel("Seleccione un pedido para ver el detalle.");
        lblInfoPedido.setVerticalAlignment(SwingConstants.TOP);
        lblInfoPedido.setBorder(new EmptyBorder(5, 5, 10, 5));
        
        panelDetalleContent = new JPanel();
        panelDetalleContent.setLayout(new BoxLayout(panelDetalleContent, BoxLayout.Y_AXIS));
        panelDetalleContent.setBackground(Color.WHITE);
        
        JScrollPane scrollDetalle = new JScrollPane(panelDetalleContent);
        scrollDetalle.getVerticalScrollBar().setUnitIncrement(16);

        JPanel panelBotones = new JPanel(new GridLayout(1, 4, 5, 5));
        btnDespachar = new JButton("Despachar");
        btnCancelar = new JButton("Cancelar");
        btnEliminar = new JButton("Eliminar");
        btnActualizar = new JButton("Actualizar");
        btnFacturar = new JButton("Cargar a Factura");

        styleButton(btnFacturar, new Color(0, 123, 255), Color.WHITE);
        styleButton(btnDespachar, new Color(40, 167, 69), Color.WHITE);
        styleButton(btnCancelar, new Color(255, 193, 7), Color.BLACK);
        styleButton(btnEliminar, new Color(220, 53, 69), Color.WHITE);
        styleButton(btnActualizar, new Color(108, 117, 125), Color.WHITE);

        panelBotones.add(btnFacturar);
        panelBotones.add(btnDespachar);
        panelBotones.add(btnCancelar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnActualizar);

        panelDerecho.add(lblInfoPedido, BorderLayout.NORTH);
        panelDerecho.add(scrollDetalle, BorderLayout.CENTER);
        panelDerecho.add(panelBotones, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, panelDerecho);
        split.setDividerLocation(600);
        panelPrincipal.add(split, BorderLayout.CENTER);
        add(panelPrincipal);

        // --- Eventos ---
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
        btnFacturar.addActionListener(e -> {
            int row = tablaPedidos.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Por favor, seleccione un pedido primero.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int id = (int) tablaPedidos.getValueAt(row, 0);
            PedidoWeb pedidoSeleccionado = listaPedidosActual.stream().filter(p -> p.getId() == id).findFirst().orElse(null);     
            if (pedidoSeleccionado != null) {
                FacturaGUI nuevaFacturaGUI = new FacturaGUI();
                nuevaFacturaGUI.cargarPedidoWeb(pedidoSeleccionado);
                nuevaFacturaGUI.setVisible(true);
            }
        });
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
        panelDetalleContent.removeAll();
        
        if (row < 0) {
            lblInfoPedido.setText("Seleccione un pedido para ver el detalle.");
            panelDetalleContent.revalidate();
            panelDetalleContent.repaint();
            return;
        }
        
        int id = (int) tablaPedidos.getValueAt(row, 0);
        PedidoWeb pedido = listaPedidosActual.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
        
        if (pedido != null) {
            String htmlInfo = "<html><body style='width: 350px'>"
                    + "<b>Cliente:</b> " + pedido.getClienteNombre() + "<br>"
                    + "<b>Teléfono:</b> " + pedido.getClienteTelefono() + "<br>"
                    + "<b>Dirección:</b> " + pedido.getClienteDireccion() + "<br>"
                    + "<b>Email:</b> " + pedido.getClienteEmail() + "<br>"
                    + "<b>Método Pago:</b> " + pedido.getMetodoPago() + "<br>"
                    + "<h3 style='color:blue'>Total: " + NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).format(pedido.getTotalVenta()) + "</h3>"
                    + "</body></html>";
            lblInfoPedido.setText(htmlInfo);

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
        String nombre = item.optString("nombre", "Sin Nombre");
        String codigo = item.optString("id", "N/A");
        double precio = item.optDouble("precio", 0.0);
        double qty = item.optDouble("qty", 1.0);
        String urlImagen = item.optString("imagen_url", "");

        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(Color.WHITE);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JLabel lblImagen = new JLabel();
        lblImagen.setPreferredSize(new Dimension(80, 80));
        lblImagen.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        lblImagen.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagen.setText("Cargando...");
        
        if (!urlImagen.isEmpty()) {
            new SwingWorker<ImageIcon, Void>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    BufferedImage image = null;
                    
                    try {
                        // --- ESTRATEGIA 1: CARGA LOCAL (RÁPIDA Y SIN ERRORES) ---
                        // Ruta base donde dices que están las fotos
                        String baseLocal = "C:\\Users\\Pcmastersuperrace\\Desktop\\pagina web tshp\\backend tshop";
                        String rutaArchivo = baseLocal + urlImagen.replace("/", "\\");
                        
                        File archivoLocal = new File(rutaArchivo);
                        if (archivoLocal.exists()) {
                            System.out.println("Cargando desde DISCO: " + rutaArchivo);
                            image = ImageIO.read(archivoLocal);
                        } 
                        
                        // --- ESTRATEGIA 2: CARGA WEB (RESPALDO) ---
                        if (image == null) {
                            String urlFinal = urlImagen;
                            if (!urlFinal.startsWith("http")) {
                                urlFinal = "https://api.tshoptechnology.com" + urlFinal;
                            }
                            System.out.println("Intentando descargar de INTERNET: " + urlFinal);
                            
                            URL url = java.net.URI.create(urlFinal).toURL();
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            
                            // DISFRAZ DE NAVEGADOR (Para evitar error 403)
                            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36");
                            connection.setConnectTimeout(5000);
                            connection.setReadTimeout(5000);
                            connection.connect();
                            
                            if (connection.getResponseCode() == 200) {
                                try (java.io.InputStream input = connection.getInputStream()) {
                                    image = ImageIO.read(input);
                                }
                            }
                        }

                        if (image != null) {
                            Image scaled = image.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                            return new ImageIcon(scaled);
                        }

                    } catch (Exception e) {
                        System.err.println("Error cargando imagen (" + urlImagen + "): " + e.getMessage());
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

        String htmlTexto = "<html>"
                + "<b>" + nombre + "</b><br>"
                + "<font color='gray'>Cód: " + codigo + "</font><br>"
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
    
    class EstadoPedidoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) return c;
            
            String estado = (String) table.getModel().getValueAt(table.convertRowIndexToModel(row), 1);
            
            // Colores según el nuevo flujo de estados
            if ("DESPACHADO".equalsIgnoreCase(estado) || "PAGADO_EPAYCO".equalsIgnoreCase(estado) || "PAGADO".equalsIgnoreCase(estado)) {
                c.setBackground(new Color(212, 237, 218)); // Verde (Listo para enviar o ya enviado)
            } else if ("CANCELADO".equalsIgnoreCase(estado)) {
                c.setBackground(new Color(248, 215, 218)); // Rojo
            } else if ("PENDIENTE_PAGO".equalsIgnoreCase(estado) || "PENDIENTE".equalsIgnoreCase(estado)) {
                c.setBackground(new Color(255, 243, 205)); // Amarillo (Esperando confirmación)
            } else {
                c.setBackground(Color.WHITE);
            }
            
            c.setForeground(Color.BLACK);
            return c;
        }
    }
}