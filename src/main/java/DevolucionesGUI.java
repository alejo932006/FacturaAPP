// Reemplaza el contenido completo de DevolucionesGUI.java con este código

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DevolucionesGUI extends JDialog {

    private JTable tablaFacturas, tablaProductosFactura;
    private DefaultTableModel modeloTablaFacturas, modeloTablaProductos;
    private TableRowSorter<DefaultTableModel> sorterFacturas;
    private JButton btnDevolverProducto;

    private FacturaGUI facturaGUI;
    private final NumberFormat formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    private Map<String, Double> cantidadesYaDevueltas = new HashMap<>();

    public DevolucionesGUI(FacturaGUI owner) {
        super(owner, "Módulo de Devoluciones", true);
        this.facturaGUI = owner;
        formatoPesos.setMaximumFractionDigits(0);
        setSize(800, 700);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        
        initComponents();
        cargarFacturasVigentes();
    }

    private void initComponents() {
        JPanel panelFacturas = new JPanel(new BorderLayout(5, 5));
        panelFacturas.setBorder(BorderFactory.createTitledBorder("Paso 1: Seleccione la Factura"));

        JTextField txtBuscarFactura = new JTextField();
        JPanel panelBusqueda = new JPanel(new BorderLayout(5,0));
        panelBusqueda.add(new JLabel("Buscar Factura: "), BorderLayout.WEST);
        panelBusqueda.add(txtBuscarFactura, BorderLayout.CENTER);
        
        String[] columnasFacturas = {"Nº Factura", "Fecha", "Cliente", "Total", "Nombre Archivo", "Cédula Cliente"};
        modeloTablaFacturas = new DefaultTableModel(columnasFacturas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaFacturas = new JTable(modeloTablaFacturas);
        sorterFacturas = new TableRowSorter<>(modeloTablaFacturas);
        tablaFacturas.setRowSorter(sorterFacturas);
        
        tablaFacturas.getColumnModel().getColumn(4).setMinWidth(0);
        tablaFacturas.getColumnModel().getColumn(4).setMaxWidth(0);
        tablaFacturas.getColumnModel().getColumn(5).setMinWidth(0);
        tablaFacturas.getColumnModel().getColumn(5).setMaxWidth(0);

        panelFacturas.add(panelBusqueda, BorderLayout.NORTH);
        panelFacturas.add(new JScrollPane(tablaFacturas), BorderLayout.CENTER);

        JPanel panelProductos = new JPanel(new BorderLayout(5, 5));
        panelProductos.setBorder(BorderFactory.createTitledBorder("Paso 2: Seleccione el Producto a Devolver"));
        
        String[] columnasProductos = {"Producto", "Cant. Vendida", "Cant. Devuelta", "Subtotal", "Código", "IMEI/Detalle", "Costo Original"};
        modeloTablaProductos = new DefaultTableModel(columnasProductos, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaProductosFactura = new JTable(modeloTablaProductos);
        tablaProductosFactura.setDefaultRenderer(Object.class, new DevolucionRenderer());
        
        tablaProductosFactura.getColumnModel().getColumn(4).setMinWidth(0);
        tablaProductosFactura.getColumnModel().getColumn(4).setMaxWidth(0);
        tablaProductosFactura.getColumnModel().getColumn(5).setMinWidth(0);
        tablaProductosFactura.getColumnModel().getColumn(5).setMaxWidth(0);
        tablaProductosFactura.getColumnModel().getColumn(6).setMinWidth(0); // <-- AÑADE ESTA LÍNEA
        tablaProductosFactura.getColumnModel().getColumn(6).setMaxWidth(0);
        
        JPanel panelAccionDevolver = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnDevolverProducto = new JButton("Devolver Producto Seleccionado");
        btnDevolverProducto.setEnabled(false);
        panelAccionDevolver.add(btnDevolverProducto);

        panelProductos.add(new JScrollPane(tablaProductosFactura), BorderLayout.CENTER);
        panelProductos.add(panelAccionDevolver, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelFacturas, panelProductos);
        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);

        txtBuscarFactura.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { sorterFacturas.setRowFilter(RowFilter.regexFilter("(?i)" + txtBuscarFactura.getText())); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { sorterFacturas.setRowFilter(RowFilter.regexFilter("(?i)" + txtBuscarFactura.getText())); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { sorterFacturas.setRowFilter(RowFilter.regexFilter("(?i)" + txtBuscarFactura.getText())); }
        });
        
        tablaFacturas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarProductosDeFacturaSeleccionada();
            }
        });

        tablaProductosFactura.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int filaSeleccionada = tablaProductosFactura.getSelectedRow();
                if (filaSeleccionada >= 0) {
                    // Extraemos los datos de la fila
                    String codigo = modeloTablaProductos.getValueAt(filaSeleccionada, 4).toString();
                    String imei = modeloTablaProductos.getValueAt(filaSeleccionada, 5).toString();
                    double cantidadVendida = Double.parseDouble(modeloTablaProductos.getValueAt(filaSeleccionada, 1).toString().split(" ")[0].replace(',', '.'));
                    
                    // Creamos el identificador
                    String identificadorUnico = codigo + (imei.isEmpty() ? "_base" : "_" + imei);
                    
                    // Obtenemos lo que ya se devolvió de nuestro Map
                    double yaDevuelto = cantidadesYaDevueltas.getOrDefault(identificadorUnico, 0.0);
        
                    // Habilitamos el botón SOLO si la cantidad devuelta es menor a la vendida
                    btnDevolverProducto.setEnabled(yaDevuelto < cantidadVendida);
                } else {
                    btnDevolverProducto.setEnabled(false);
                }
            }
        });
        btnDevolverProducto.addActionListener(e -> procesarDevolucion());
    }
           

    private void cargarFacturasVigentes() {
        modeloTablaFacturas.setRowCount(0);
        File[] archivos = PathsHelper.getFacturasFolder().listFiles((dir, name) -> name.toLowerCase().endsWith(".txt") && !name.startsWith("ANULADA_"));
        if (archivos == null) return;

        for (File archivo : archivos) {
            try {
                List<String> lineas = Files.readAllLines(archivo.toPath());
                String numeroFactura = "N/A", fecha = "N/A", cliente = "N/A", cedula = "N/A";
                double total = 0.0;

                for (String linea : lineas) {
                    if (linea.contains("FACTURA DE VENTA:")) numeroFactura = linea.split(":")[1].replace("===", "").trim();
                    if (linea.startsWith("Fecha:")) fecha = linea.split(":", 2)[1].trim();
                    if (linea.startsWith("Cliente:")) cliente = linea.split(":", 2)[1].trim();
                    if (linea.startsWith("Cédula:")) cedula = linea.split(":", 2)[1].trim();
                    if (linea.startsWith("TOTAL A PAGAR:")) {
                        String totalStr = linea.replaceAll("[^\\d]", "");
                        if (!totalStr.isEmpty()) total = Double.parseDouble(totalStr);
                    }
                }
                modeloTablaFacturas.addRow(new Object[]{numeroFactura, fecha, cliente, formatoPesos.format(total), archivo.getName(), cedula});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void cargarProductosDeFacturaSeleccionada() {
        modeloTablaProductos.setRowCount(0);
        cantidadesYaDevueltas.clear();
        btnDevolverProducto.setEnabled(false);

        int filaVista = tablaFacturas.getSelectedRow();
        if (filaVista < 0) return;

        int filaModelo = tablaFacturas.convertRowIndexToModel(filaVista);
        String nombreArchivo = modeloTablaFacturas.getValueAt(filaModelo, 4).toString();
        File archivoFactura = PathsHelper.getFacturaTXT(nombreArchivo);

        if (!archivoFactura.exists()) return;

        try {
            List<String> lineas = Files.readAllLines(archivoFactura.toPath());
            String numFactura = modeloTablaFacturas.getValueAt(filaModelo, 0).toString();
            String etiquetaDetalle = ConfiguracionManager.cargarEtiquetaDetalle();
            
            cargarCantidadesYaDevueltas(numFactura);

            for (int i = 0; i < lineas.size(); i++) {
                String linea = lineas.get(i);
                if (linea.trim().startsWith("- [")) {
                    String[] datosProducto = parsearLineaProducto(linea);
                    String imei = "";
                    if (i + 1 < lineas.size() && lineas.get(i + 1).trim().startsWith(etiquetaDetalle + ":")) {
                        imei = lineas.get(i + 1).split(":")[1].trim();
                    }
                    double costoOriginal = 0.0;
                    if (linea.contains("#venta_u:") && linea.contains("costo_u:")) {
                        try {
                            String parteDatos = linea.split("#")[1];
                            costoOriginal = Double.parseDouble(parteDatos.split(";")[1].split(":")[1]);
                        } catch (Exception parseEx) {
                            System.err.println("No se pudo parsear el costo para la línea: " + linea);
                        }
                    }
                    String identificadorUnico = datosProducto[0] + (imei.isEmpty() ? "_base" : "_" + imei);
                    double yaDevuelto = cantidadesYaDevueltas.getOrDefault(identificadorUnico, 0.0);
                    modeloTablaProductos.addRow(new Object[]{datosProducto[1], datosProducto[2], yaDevuelto, datosProducto[3], datosProducto[0], imei, costoOriginal});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Reemplaza este método completo en DevolucionesGUI.java

    private void procesarDevolucion() {
        int filaFacturaSel = tablaFacturas.getSelectedRow();
        int filaProductoSel = tablaProductosFactura.getSelectedRow();

        if (filaFacturaSel < 0 || filaProductoSel < 0) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una factura y un producto.", "Selección Requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int filaModeloFactura = tablaFacturas.convertRowIndexToModel(filaFacturaSel);
        String numFactura = modeloTablaFacturas.getValueAt(filaModeloFactura, 0).toString();
        String nombreCliente = modeloTablaFacturas.getValueAt(filaModeloFactura, 2).toString();
        String cedulaCliente = modeloTablaFacturas.getValueAt(filaModeloFactura, 5).toString();
        
        double cantidadEnFactura = Double.parseDouble(modeloTablaProductos.getValueAt(filaProductoSel, 1).toString().split(" ")[0].replace(',', '.'));
        double cantidadYaDevuelta = (double) modeloTablaProductos.getValueAt(filaProductoSel, 2);
        double cantidadRestante = cantidadEnFactura - cantidadYaDevuelta;
        String nombreProducto = modeloTablaProductos.getValueAt(filaProductoSel, 0).toString();
        
        String cantidadStr = JOptionPane.showInputDialog(this, 
            "Producto: " + nombreProducto + "\nCantidad en factura: " + cantidadEnFactura + 
            "\nUnidades ya devueltas: " + cantidadYaDevuelta +
            "\n\n¿Cuántas unidades (de las " + cantidadRestante + " restantes) desea devolver?", 
            "Confirmar Devolución", 
            JOptionPane.QUESTION_MESSAGE);
        
        if (cantidadStr == null || cantidadStr.trim().isEmpty()) return;
        
        try {
            double cantidadADevolver = Double.parseDouble(cantidadStr.replace(',', '.'));
            if (cantidadADevolver <= 0 || cantidadADevolver > cantidadRestante) {
                JOptionPane.showMessageDialog(this, "La cantidad a devolver es inválida o excede las unidades restantes.", "Cantidad Inválida", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // --- INICIO DE LA LÓGICA MODIFICADA ---

            // 1. Determina si fue una venta a crédito
            Double totalPagado = obtenerTotalPagadoDeCredito(numFactura);
            boolean esVentaCredito = (totalPagado != null);
            
            // 2. Pregunta CÓMO se devuelve el dinero (solo si fue venta de contado)
            String metodoReembolso = "Efectivo"; // Valor por defecto
            
            if (!esVentaCredito) {
                Object[] opcionesReembolso = {"Efectivo (desde Caja)", "Transferencia (desde Banco)"};
                int seleccion = JOptionPane.showOptionDialog(this,
                    "¿Cómo se realizará el reembolso al cliente por esta devolución?",
                    "Método de Reembolso",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opcionesReembolso, opcionesReembolso[0]);
                
                if (seleccion == JOptionPane.CLOSED_OPTION) {
                    return; // Si el usuario cierra la ventana, se cancela la devolución
                }
                
                metodoReembolso = (seleccion == 0) ? "Efectivo" : "Transferencia";
            }

            // 3. Obtiene los datos del producto y calcula el valor a devolver
            String codigoProducto = modeloTablaProductos.getValueAt(filaProductoSel, 4).toString();
            Producto productoEncontrado = ProductoStorage.cargarProductos().stream().filter(p -> p.getCodigo().equalsIgnoreCase(codigoProducto)).findFirst().orElse(null);

            if (productoEncontrado == null) {
                JOptionPane.showMessageDialog(this, "Error: El producto no se encontró en el inventario actual.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String subtotalTexto = modeloTablaProductos.getValueAt(filaProductoSel, 3).toString();
            double subtotal = formatoPesos.parse(subtotalTexto).doubleValue();
            double valorDelProductoDevuelto = (subtotal / cantidadEnFactura) * cantidadADevolver;

            // 4. Procesa la devolución según el tipo de venta
            if (esVentaCredito) {
                // --- Bloque para ventas a CRÉDITO (sin cambios, ya es correcto) ---
                File archivoOrden = obtenerArchivoDeCredito(numFactura);
                if (archivoOrden == null) {
                    JOptionPane.showMessageDialog(this, "Error: No se pudo encontrar la orden de crédito original.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            
                double valorTotalOriginal = obtenerValorOriginalDeOrden(archivoOrden);
                double montoYaPagado = (totalPagado != null) ? totalPagado : 0.0;
                double montoYaDevueltoAnteriormente = obtenerTotalDevueltoDeOrden(archivoOrden);
                double valorDelProductoActual = valorDelProductoDevuelto;
            
                double nuevoValorTotalDeLaCompra = valorTotalOriginal - montoYaDevueltoAnteriormente - valorDelProductoActual;
                double saldoAFavor = montoYaPagado - nuevoValorTotalDeLaCompra;
            
            // <-- INICIO DE LA NUEVA LÓGICA DE DECISIÓN
            if (saldoAFavor > 0) {
                Object[] opciones = {"Generar Nota de Crédito", "Reembolsar en Efectivo (Caja)", "Reembolsar por Transferencia (Banco)", "Cancelar"};
                int seleccion = JOptionPane.showOptionDialog(this,
                    "Esta devolución genera un saldo a favor del cliente por " + formatoPesos.format(saldoAFavor) + ".\n\n" +
                    "¿Qué desea hacer con este monto?",
                    "Cliente con Saldo a Favor",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
            
                switch (seleccion) {
                    case 0: // Generar Nota de Crédito
                        generarNotaCredito(numFactura, productoEncontrado, cantidadADevolver, saldoAFavor, nombreCliente, cedulaCliente);
                        JOptionPane.showMessageDialog(this, "Se generó una Nota de Crédito por: " + formatoPesos.format(saldoAFavor), "Éxito", JOptionPane.INFORMATION_MESSAGE);
                        break;
            
                    case 1: // Reembolsar en Efectivo
                        CuentasStorage.restarDeCaja(saldoAFavor);
                        JOptionPane.showMessageDialog(this, "Se reembolsó " + formatoPesos.format(saldoAFavor) + " desde la Caja.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                        break;
            
                    case 2: // Reembolsar por Transferencia
                        CuentasStorage.restarDeBanco(saldoAFavor);
                        JOptionPane.showMessageDialog(this, "Se reembolsó " + formatoPesos.format(saldoAFavor) + " desde el Banco.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                        break;
            
                    default: // Cancelar o cerrar
                        return; 
                }
                
                // Al finalizar la devolución que salda la cuenta, la marcamos como completada.
                File archivoCompletado = new File(PathsHelper.getOrdenesFolder(), "COMPLETADA_" + archivoOrden.getName());
                archivoOrden.renameTo(archivoCompletado);
                
            } else { // Si no hay saldo a favor, solo se reduce la deuda.
                registrarDevolucionParcial(archivoOrden, montoYaDevueltoAnteriormente, valorDelProductoDevuelto);
            }
            
            } else {
                // --- Bloque para ventas de CONTADO (NUEVA LÓGICA) ---
                if ("Efectivo".equals(metodoReembolso)) {
                    CuentasStorage.restarDeCaja(valorDelProductoDevuelto);
                } else {
                    CuentasStorage.restarDeBanco(valorDelProductoDevuelto);
                }
                JOptionPane.showMessageDialog(this, "Devolución procesada. Se descontó " + formatoPesos.format(valorDelProductoDevuelto) + " de la cuenta: " + metodoReembolso, "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
            
            // --- FIN DE LA LÓGICA MODIFICADA ---
            
            // 5. El resto del proceso es común para ambos casos (actualizar stock, guardar registro)
            devolverProductoAStock(productoEncontrado.getCodigo(), cantidadADevolver);
            
            String imei = modeloTablaProductos.getValueAt(filaProductoSel, 5).toString();
            String identificadorUnico = productoEncontrado.getCodigo() + (imei.isEmpty() ? "_base" : "_" + imei);
            String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(java.time.LocalDateTime.now());
            
            double costoUnitarioOriginal = (double) modeloTablaProductos.getValueAt(filaProductoSel, 6);
            
            File devolucionFile = new File(PathsHelper.getDevolucionesFolder(), "devolucion_" + numFactura + "_" + identificadorUnico + "_" + timestamp + ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(devolucionFile))) {
                writer.write("Número de Factura: " + numFactura + "\n");
                writer.write("Fecha de Devolución: " + LocalDate.now().toString() + "\n");
                writer.write("Producto: " + productoEncontrado.getNombre() + "\n");
                writer.write("Código: " + productoEncontrado.getCodigo() + "\n");
                if (imei != null && !imei.isEmpty()) {
                    writer.write(ConfiguracionManager.cargarEtiquetaDetalle() + ": " + imei + "\n");
                }
                writer.write("Cantidad Devuelta: " + cantidadADevolver + "\n");
                writer.write("Valor Devuelto: " + valorDelProductoDevuelto + "\n");
                writer.write("Costo Unitario Original: " + costoUnitarioOriginal + "\n");
            }

            if(facturaGUI != null) facturaGUI.refrescarListaProductos();
            
            // Aseguramos que el Dashboard se actualice para reflejar el cambio en Caja o Banco
            if (DashboardGUI.getInstance() != null) {
                DashboardGUI.getInstance().refrescarDatos();
            }

            cargarProductosDeFacturaSeleccionada();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al procesar la devolución: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void devolverProductoAStock(String codigoProducto, double cantidadADevolver) {
        List<Producto> inventario = ProductoStorage.cargarProductos();
        inventario.stream()
            .filter(p -> p.getCodigo().equalsIgnoreCase(codigoProducto))
            .findFirst()
            .ifPresent(p -> p.setCantidad(p.getCantidad() + cantidadADevolver));
        ProductoStorage.sobrescribirInventario(inventario);
    }
    
    private void cargarCantidadesYaDevueltas(String numFactura) {
        cantidadesYaDevueltas.clear();
        File carpetaDevoluciones = PathsHelper.getDevolucionesFolder();
    
        // Filtra todos los archivos de devolución para la factura actual.
        File[] archivos = carpetaDevoluciones.listFiles(
            (dir, name) -> name.startsWith("devolucion_" + numFactura)
        );
    
        if (archivos == null) return;
    
        for (File archivo : archivos) {
            try {
                List<String> lineas = Files.readAllLines(archivo.toPath());
                String codigo = "";
                String imei = ""; // Asumimos que puede no tener IMEI
                double cantidadDevuelta = 0;
    
                // Leemos los datos clave del archivo de devolución
                for (String linea : lineas) {
                    if (linea.startsWith("Código:")) {
                        codigo = linea.split(":")[1].trim();
                    } else if (linea.startsWith(ConfiguracionManager.cargarEtiquetaDetalle() + ":")) {
                        imei = linea.split(":")[1].trim();
                    } else if (linea.startsWith("Cantidad Devuelta:")) {
                        cantidadDevuelta = Double.parseDouble(linea.split(":")[1].trim());
                    }
                }
    
                if (!codigo.isEmpty() && cantidadDevuelta > 0) {
                    String identificadorUnico = codigo + (imei.isEmpty() ? "_base" : "_" + imei);
                    // Sumamos la cantidad devuelta al total para ese producto.
                    cantidadesYaDevueltas.merge(identificadorUnico, cantidadDevuelta, Double::sum);
                }
            } catch (Exception e) {
                System.err.println("Error al procesar el archivo de devolución: " + archivo.getName());
                e.printStackTrace();
            }
        }
    }
    
    private String[] parsearLineaProducto(String linea) {
        try {
            String codigo = linea.substring(linea.indexOf('[') + 1, linea.indexOf(']')).trim();
            String resto = linea.substring(linea.indexOf(']') + 1).trim();
            String[] partesPrincipales = resto.split(" = ");
            String subtotal = partesPrincipales[1];
            String[] partesProducto = partesPrincipales[0].split(" x ");
            String nombre = partesProducto[0].trim();
            String cantidad = partesProducto[1].trim();
            return new String[]{codigo, nombre, cantidad, subtotal};
        } catch (Exception e) {
            System.err.println("Error al parsear línea de producto: " + linea);
            return new String[]{"error", "error", "0", "$0"};
        }
    }

    private Double obtenerTotalPagadoDeCredito(String numeroFactura) {
        File carpetaOrdenes = PathsHelper.getOrdenesFolder();
        File[] archivos = carpetaOrdenes.listFiles(
            (dir, name) -> name.startsWith("ReciboDeAbonoSeparado_") && name.endsWith(".txt")
        );
    
        if (archivos == null) return null;
    
        File archivoOrden = null;
    
        for (File archivo : archivos) {
            try {
                if (Files.lines(archivo.toPath()).anyMatch(linea -> linea.contains(numeroFactura))) {
                    archivoOrden = archivo;
                    break;
                }
            } catch (IOException e) {
                System.err.println("Error al leer archivo de orden: " + archivo.getName());
            }
        }
    
        if (archivoOrden == null) {
            return null; 
        }
    
        double totalAbonado = 0.0;
        try {
            List<String> lineas = Files.readAllLines(archivoOrden.toPath());
            boolean enSeccionAbonos = false;
            
            for (String linea : lineas) {
                if (linea.contains("=== HISTORIAL DE ABONOS ===")) {
                    enSeccionAbonos = true;
                    continue;
                }
                
                if (enSeccionAbonos && linea.contains(";")) {
                    String[] partes = linea.split(";", 2);
                    if (partes.length == 2) {
                        String valorSoloNumeros = partes[1].replaceAll("[^0-9]", "");
                        if (!valorSoloNumeros.isEmpty()) {
                            totalAbonado += Double.parseDouble(valorSoloNumeros);
                        }
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    
        return totalAbonado;
    }
    


    class DevolucionRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                int modelRow = table.convertRowIndexToModel(row);
                String codigo = (String) table.getModel().getValueAt(modelRow, 4);
                String imei = (String) table.getModel().getValueAt(modelRow, 5);
                double cantidadVendida = Double.parseDouble(table.getModel().getValueAt(modelRow, 1).toString().split(" ")[0].replace(',', '.'));
                
                String identificadorUnico = codigo + (imei.isEmpty() ? "_base" : "_" + imei);
                double yaDevuelto = cantidadesYaDevueltas.getOrDefault(identificadorUnico, 0.0);

                // Si lo devuelto es igual o más que lo vendido, se deshabilita la fila
                if (yaDevuelto >= cantidadVendida) {
                    c.setBackground(new Color(235, 235, 235));
                    c.setForeground(Color.GRAY);
                } else {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }
            }
            return c;
        }
    }

    private File obtenerArchivoDeCredito(String numeroFactura) {
        File carpetaOrdenes = PathsHelper.getOrdenesFolder();
        File[] archivos = carpetaOrdenes.listFiles(
            (dir, name) -> name.startsWith("ReciboDeAbonoSeparado_") && !name.startsWith("COMPLETADA_")
        );
        if (archivos == null) return null;

        for (File archivo : archivos) {
            try {
                if (Files.lines(archivo.toPath()).anyMatch(linea -> linea.contains(numeroFactura))) {
                    return archivo;
                }
            } catch (IOException e) {
                System.err.println("Error al leer archivo de orden: " + archivo.getName());
            }
        }
        return null;
    }

    private double obtenerValorOriginalDeOrden(File archivoOrden) {
        if (archivoOrden == null) return 0;
        try {
            List<String> lineas = Files.readAllLines(archivoOrden.toPath());
            for (String linea : lineas) {
                if (linea.startsWith("VALOR ASOCIADO:")) {
                    String valorNumerico = linea.replaceAll("[^0-9]", "");
                    if (!valorNumerico.isEmpty()) {
                        return Double.parseDouble(valorNumerico);
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void generarNotaCredito(String numFactura, Producto producto, double cantidad, double valor, String nombreCliente, String cedulaCliente) {
        Empresa empresa = ConfiguracionManager.cargarEmpresa();
        NumberFormat formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        formatoPesos.setMaximumFractionDigits(0);
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(empresa.getRazonSocial().toUpperCase()).append(" ===\n");
        sb.append("NIT: ").append(empresa.getNit()).append("\n\n");
        sb.append("   *** NOTA DE CRÉDITO ***\n\n");
        sb.append("Fecha: ").append(LocalDate.now().toString()).append("\n");
        sb.append("Factura Asociada: ").append(numFactura).append("\n\n");
        sb.append("--- CLIENTE ---\n");
        sb.append("Nombre: ").append(nombreCliente).append("\n");
        sb.append("Cédula/ID: ").append(cedulaCliente).append("\n\n");
        sb.append("--- DETALLE DE DEVOLUCIÓN ---\n");
        sb.append("Producto: ").append(producto.getNombre()).append("\n");
        sb.append("Cantidad Devuelta: ").append(cantidad).append(" ").append(producto.getUnidadDeMedida()).append("\n\n");
        sb.append("VALOR A FAVOR DEL CLIENTE: ").append(formatoPesos.format(valor)).append("\n");
        
        String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(java.time.LocalDateTime.now());
        String nombreArchivo = String.format("NotadeCredito_%s_%s.txt", numFactura.replace("FV-", ""), timestamp);
        File archivoFinal = new File(PathsHelper.getOrdenesFolder(), nombreArchivo);
    
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivoFinal))) {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al generar la Nota de Crédito.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double obtenerTotalDevueltoDeOrden(File archivoOrden) {
        if (archivoOrden == null) return 0;
        try {
            List<String> lineas = Files.readAllLines(archivoOrden.toPath());
            for (String linea : lineas) {
                if (linea.startsWith("TOTAL DEVUELTO:")) {
                    String valorNumerico = linea.replaceAll("[^0-9]", "");
                    if (!valorNumerico.isEmpty()) {
                        return Double.parseDouble(valorNumerico);
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return 0; // Devuelve 0 si no encuentra la línea o hay un error
    }
    
    private void registrarDevolucionParcial(File archivoOrden, double montoYaDevuelto, double valorDelProductoDevuelto) throws IOException {
        double nuevoTotalDevuelto = montoYaDevuelto + valorDelProductoDevuelto;
        
        List<String> lineas = new java.util.ArrayList<>(Files.readAllLines(archivoOrden.toPath()));
        boolean totalDevueltoActualizado = false;
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).startsWith("TOTAL DEVUELTO:")) {
                lineas.set(i, "TOTAL DEVUELTO: " + formatoPesos.format(nuevoTotalDevuelto));
                totalDevueltoActualizado = true;
                break;
            }
        }
        if (!totalDevueltoActualizado) {
            lineas.add(lineas.size() - 1, "TOTAL DEVUELTO: " + formatoPesos.format(nuevoTotalDevuelto) + "\n");
        }
        
        Files.write(archivoOrden.toPath(), lineas);
        JOptionPane.showMessageDialog(this, "Devolución parcial registrada. Puede continuar devolviendo otros productos.", "Paso Completado", JOptionPane.INFORMATION_MESSAGE);
    }
    
}