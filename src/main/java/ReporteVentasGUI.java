import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Locale;


public class ReporteVentasGUI extends JDialog {

    // --- Componentes de la Interfaz ---
    private JTabbedPane tabbedPane;
    private JTable tablaResumen, tablaDetalle;
    private DefaultTableModel modeloResumen, modeloDetalle;
    private JLabel lblVentasTotales, lblCostoTotal, lblUtilidadTotal, lblTitulo;
    private JDateChooser selectorFechaInicio, selectorFechaFin;
    private JButton btnGenerarReporte;
    private JComboBox<String> comboFiltroArea;    
    private final NumberFormat formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    private JLabel lblFiltroArea;
    

    public ReporteVentasGUI(Frame owner) {
        super(owner, "Reporte Avanzado de Ventas", true);
        setSize(950, 700);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        formatoPesos.setMaximumFractionDigits(0);
        initComponents();
    }

    private void initComponents() {
        // --- Panel Superior con Controles ---
        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelControles.setBorder(BorderFactory.createTitledBorder("Seleccionar Período"));
        
        selectorFechaInicio = new JDateChooser();
        selectorFechaFin = new JDateChooser();
        btnGenerarReporte = new JButton("Generar Reporte");

        lblFiltroArea = new JLabel("Filtrar por Área:");
        comboFiltroArea = new JComboBox<>();
        
        // Solo cargamos las áreas si la función está habilitada
        if (ConfiguracionManager.isGestionPorAreasHabilitada()) {
            cargarAreasDisponibles();
        }
        
        panelControles.add(lblFiltroArea);
        panelControles.add(comboFiltroArea);
        
        // Ocultamos los componentes si la función está deshabilitada
        boolean gestionAreasHabilitada = ConfiguracionManager.isGestionPorAreasHabilitada();
        lblFiltroArea.setVisible(gestionAreasHabilitada);
        comboFiltroArea.setVisible(gestionAreasHabilitada);
        
        panelControles.add(new JLabel("Desde:"));
        panelControles.add(selectorFechaInicio);
        panelControles.add(new JLabel("Hasta:"));
        panelControles.add(selectorFechaFin);
        panelControles.add(btnGenerarReporte);

        lblTitulo = new JLabel("Seleccione un rango de fechas y genere el reporte", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 18));
        
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.add(panelControles, BorderLayout.NORTH);
        panelSuperior.add(lblTitulo, BorderLayout.CENTER);
        add(panelSuperior, BorderLayout.NORTH);
        
        // --- Pestañas para Resumen y Detalle ---
        tabbedPane = new JTabbedPane();
        
        // Pestaña 1: Resumen por Producto (la vista que ya tenías)
        JPanel panelResumen = new JPanel(new BorderLayout());
        String[] columnasResumen = {"Producto", "Cantidad Vendida", "Total Venta", "Costo Total", "Utilidad"};
        modeloResumen = new DefaultTableModel(columnasResumen, 0);
        tablaResumen = new JTable(modeloResumen);
        panelResumen.add(new JScrollPane(tablaResumen), BorderLayout.CENTER);
        // Aquí podrías añadir un panel para el gráfico en el futuro
        // JPanel panelGrafico = new JPanel();
        // panelResumen.add(panelGrafico, BorderLayout.EAST);
        
        // Pestaña 2: Detalle de Ventas (la nueva vista)
        JPanel panelDetalle = new JPanel(new BorderLayout());
        String[] columnasDetalle = {"Fecha", "Factura", "Producto", "Cantidad", "Venta Bruta", "Descuento", "Venta Neta", "Utilidad"};
        modeloDetalle = new DefaultTableModel(columnasDetalle, 0);
        tablaDetalle = new JTable(modeloDetalle);
        panelDetalle.add(new JScrollPane(tablaDetalle), BorderLayout.CENTER);

        tabbedPane.addTab("Resumen por Producto", panelResumen);
        tabbedPane.addTab("Detalle de Ventas", panelDetalle);
        add(tabbedPane, BorderLayout.CENTER);

        // --- Panel Inferior con Totales y Botones ---
        JPanel panelInferior = new JPanel(new BorderLayout());
        JPanel panelTotales = new JPanel(new GridLayout(1, 3, 10, 5));
        panelTotales.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        lblVentasTotales = new JLabel("Ventas Totales: $0");
        lblCostoTotal = new JLabel("Costo Total: $0");
        lblUtilidadTotal = new JLabel("Utilidad Total: $0");
        Font fuenteTotales = new Font("Arial", Font.BOLD, 14);
        lblVentasTotales.setFont(fuenteTotales);
        lblCostoTotal.setFont(fuenteTotales);
        lblUtilidadTotal.setFont(fuenteTotales);
        panelTotales.add(lblVentasTotales);
        panelTotales.add(lblCostoTotal);
        panelTotales.add(lblUtilidadTotal);
        
        JButton btnExportar = new JButton("Exportar a CSV");
        JPanel panelExportar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelExportar.add(btnExportar);

        panelInferior.add(panelTotales, BorderLayout.CENTER);
        panelInferior.add(panelExportar, BorderLayout.EAST);
        add(panelInferior, BorderLayout.SOUTH);

        // --- Lógica de Eventos ---
        btnGenerarReporte.addActionListener(e -> ejecutarReporte());
        btnExportar.addActionListener(e -> exportarACSV());
    }
    
    private void ejecutarReporte() {
        if (selectorFechaInicio.getDate() == null || selectorFechaFin.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una fecha de inicio y una de fin.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        LocalDate fechaInicio = selectorFechaInicio.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate fechaFin = selectorFechaFin.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        if (fechaInicio.isAfter(fechaFin)) {
            JOptionPane.showMessageDialog(this, "La fecha de inicio no puede ser posterior a la fecha de fin.", "Error de Rango", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        lblTitulo.setText("Mostrando Reporte desde " + fechaInicio + " hasta " + fechaFin);
        
        generarReporteResumido(fechaInicio, fechaFin);
        generarReporteDetallado(fechaInicio, fechaFin);
    }
    
    private void generarReporteDetallado(LocalDate fechaInicio, LocalDate fechaFin) {
        modeloDetalle.setRowCount(0);
        String areaSeleccionada = (String) comboFiltroArea.getSelectedItem();
        Map<String, Producto> mapaProductos = ProductoStorage.cargarProductos().stream()
        .collect(Collectors.toMap(Producto::getCodigo, p -> p));
        File[] archivosFactura = PathsHelper.getFacturasFolder().listFiles((dir, name) -> name.endsWith(".txt") && !name.startsWith("ANULADA_"));
        if (archivosFactura == null) return;
        
        for (File archivo : archivosFactura) {
            try {
                List<String> lineas = Files.readAllLines(archivo.toPath());
                String fechaFacturaStr = lineas.stream().filter(l -> l.startsWith("Fecha:")).findFirst().orElse("").split(":")[1].trim();
                LocalDate fechaFactura = LocalDate.parse(fechaFacturaStr);
    
                if (!fechaFactura.isBefore(fechaInicio) && !fechaFactura.isAfter(fechaFin)) {
                    String numFactura = lineas.stream().filter(l -> l.contains("FACTURA DE VENTA:")).findFirst().orElse("").split(":")[1].trim();
                    for (int i = 0; i < lineas.size(); i++) { // <-- Usamos un bucle con índice
                        String linea = lineas.get(i);
                        if (linea.contains("#venta_u:") && linea.contains("costo_u:")) {
                            String parteVisible = linea.split("#")[0];
                            String codigoProducto = parteVisible.substring(parteVisible.indexOf('[') + 1, parteVisible.indexOf(']')).trim();
                            String parteDatos = linea.split("#")[1];
                            int indiceIgual = parteVisible.lastIndexOf(" = ");
                            int indiceUltimaX = parteVisible.lastIndexOf(" x ", indiceIgual);
                            
                            String nombreProducto = parteVisible.substring(parteVisible.indexOf(']') + 1, indiceUltimaX).trim();
                            String cantidadConUnidad = parteVisible.substring(indiceUltimaX + 3, indiceIgual).trim();
                            
                            Producto productoVendido = mapaProductos.get(codigoProducto);
                            if (productoVendido == null) continue; // Si el producto ya no existe, lo saltamos
        
                            String areaProducto = productoVendido.getAreaEncargada() == null ? "" : productoVendido.getAreaEncargada();
                            boolean incluirEnReporte = "Todas las Áreas".equals(areaSeleccionada) || areaSeleccionada.equals(areaProducto);
        
                            if (!incluirEnReporte) {
                                continue; // Si no coincide con el filtro, saltamos al siguiente producto
                            }

                            double ventaUnitaria = Double.parseDouble(parteDatos.split(";")[0].split(":")[1]);
                            double costoUnitario = Double.parseDouble(parteDatos.split(";")[1].split(":")[1]);
                            double cantidad = Double.parseDouble(cantidadConUnidad.split(" ")[0].replace(',', '.'));
                            
                            double descuento = 0;
                            if (i + 1 < lineas.size()) {
                                String siguienteLinea = lineas.get(i + 1).trim();
                                if (siguienteLinea.startsWith("Descuento:")) {
                                    String descuentoStr = siguienteLinea.replaceAll("[^\\d]", "");
                                    if (!descuentoStr.isEmpty()) {
                                        descuento = Double.parseDouble(descuentoStr);
                                    }
                                }
                            }
    
                            double ventaBruta = ventaUnitaria * cantidad;
                            double ventaNeta = ventaBruta - descuento;
                            double costoTotal = costoUnitario * cantidad;
                            double utilidad = ventaNeta - costoTotal;
                            
                            modeloDetalle.addRow(new Object[]{
                                fechaFactura.toString(), numFactura, nombreProducto, cantidadConUnidad,
                                formatoPesos.format(ventaBruta), // Venta antes de descuento
                                formatoPesos.format(descuento),  // Descuento
                                formatoPesos.format(ventaNeta),  // Venta después de descuento
                                formatoPesos.format(utilidad)    // Utilidad correcta
                            });
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error procesando detalle de factura: " + archivo.getName());
            }
        }

    // --- INICIO DE LA NUEVA LÓGICA PARA MOSTRAR DEVOLUCIONES EN EL DETALLE ---
    File[] archivosDevolucion = PathsHelper.getDevolucionesFolder().listFiles((dir, name) -> name.startsWith("devolucion_"));
    if (archivosDevolucion != null) {
        for (File archivoDev : archivosDevolucion) {
            try {
                List<String> lineas = Files.readAllLines(archivoDev.toPath());
            
                String fechaDevStr = lineas.stream().filter(l -> l.startsWith("Fecha de Devolución:")).map(l -> l.split(":", 2)[1].trim()).findFirst().orElse("");
                if (fechaDevStr.isEmpty()) continue;
                LocalDate fechaDevolucion = LocalDate.parse(fechaDevStr);
            
                if (!fechaDevolucion.isBefore(fechaInicio) && !fechaDevolucion.isAfter(fechaFin)) {
                    String codigoProductoDevuelto = lineas.stream().filter(l -> l.startsWith("Código:")).map(l -> l.split(":", 2)[1].trim()).findFirst().orElse("");
                    if (codigoProductoDevuelto.isEmpty()) continue;
            
                    Producto productoDevuelto = mapaProductos.get(codigoProductoDevuelto);
                    if (productoDevuelto == null) continue;
            
                    String areaProducto = productoDevuelto.getAreaEncargada() == null ? "" : productoDevuelto.getAreaEncargada();
                    boolean incluirEnReporte = "Todas las Áreas".equals(areaSeleccionada) || areaSeleccionada.equals(areaProducto);
                    if (!incluirEnReporte) {
                        continue;
                    }
            
                    String numFactura = lineas.stream().filter(l -> l.startsWith("Número de Factura:")).map(l -> l.split(":", 2)[1].trim()).findFirst().orElse("N/A");
                    String nombreProducto = lineas.stream().filter(l -> l.startsWith("Producto:")).map(l -> l.split(":", 2)[1].trim()).findFirst().orElse("Desconocido");
                    
                    // --- LÓGICA CORREGIDA PARA LEER NÚMEROS ---
                    String cantidadStr = lineas.stream().filter(l -> l.startsWith("Cantidad Devuelta:")).map(l -> l.split(":", 2)[1].trim()).findFirst().orElse("0");
                    String valorStr = lineas.stream().filter(l -> l.startsWith("Valor Devuelto:")).map(l -> l.split(":", 2)[1].trim()).findFirst().orElse("0");
                    String costoStr = lineas.stream().filter(l -> l.startsWith("Costo Unitario Original:")).map(l -> l.split(":", 2)[1].trim()).findFirst().orElse("0");
            
                    double cantidadDevuelta = Double.parseDouble(cantidadStr);
                    double valorDevuelto = Double.parseDouble(valorStr);
                    double costoOriginal = Double.parseDouble(costoStr);
                    // --- FIN DE LA LÓGICA CORREGIDA ---
            
                    double costoTotalDevuelto = costoOriginal * cantidadDevuelta;
                    double utilidadPerdida = valorDevuelto - costoTotalDevuelto;
            
                    modeloDetalle.addRow(new Object[]{
                        fechaDevolucion.toString(), numFactura, nombreProducto + " (DEVOLUCIÓN)", "-" + cantidadDevuelta,
                        formatoPesos.format(-valorDevuelto),
                        formatoPesos.format(0),
                        formatoPesos.format(-valorDevuelto),
                        formatoPesos.format(-utilidadPerdida)
                    });
                }
            } catch (Exception e) {
                // Ya no debería entrar aquí, pero se deja por seguridad
                System.err.println("Error procesando detalle de devolución: " + archivoDev.getName());
            }
        }
    }
    // --- FIN DE LA NUEVA LÓGICA ---
}

    private void generarReporteResumido(LocalDate fechaInicio, LocalDate fechaFin) {
        Map<String, ReporteItem> ventasAgregadas = new HashMap<>();
        String areaSeleccionada = (String) comboFiltroArea.getSelectedItem();
        Map<String, Producto> mapaProductos = ProductoStorage.cargarProductos().stream()
            .collect(Collectors.toMap(Producto::getCodigo, p -> p));
        File[] archivosFactura = PathsHelper.getFacturasFolder().listFiles((dir, name) -> name.endsWith(".txt") && !name.startsWith("ANULADA_"));
        if (archivosFactura != null) {
            for (File archivo : archivosFactura) {
                try {
                    List<String> lineas = Files.readAllLines(archivo.toPath());
                    String fechaFacturaStr = lineas.stream().filter(l -> l.startsWith("Fecha:")).findFirst().orElse("").split(":")[1].trim();
                    LocalDate fechaFactura = LocalDate.parse(fechaFacturaStr);

                    if (!fechaFactura.isBefore(fechaInicio) && !fechaFactura.isAfter(fechaFin)) {
                        for (int i = 0; i < lineas.size(); i++) {
                            String linea = lineas.get(i);
                            if (linea.contains("#venta_u:") && linea.contains("costo_u:")) {
                                String parteVisible = linea.split("#")[0];
                                String parteDatos = linea.split("#")[1];
                                String codigoProducto = parteVisible.substring(parteVisible.indexOf('[') + 1, parteVisible.indexOf(']')).trim();
                                Producto productoVendido = mapaProductos.get(codigoProducto);
                                if (productoVendido == null) continue;
    
                                String areaProducto = productoVendido.getAreaEncargada() == null ? "" : productoVendido.getAreaEncargada();
                                boolean incluirEnReporte = "Todas las Áreas".equals(areaSeleccionada) || areaSeleccionada.equals(areaProducto);
    
                                if (!incluirEnReporte) {
                                    continue;
                                }
                                int indiceIgual = parteVisible.lastIndexOf(" = ");
                                int indiceUltimaX = parteVisible.lastIndexOf(" x ", indiceIgual);
                                
                                String nombreProducto = parteVisible.substring(parteVisible.indexOf(']') + 1, indiceUltimaX).trim();
                                String cantidadConUnidad = parteVisible.substring(indiceUltimaX + 3, indiceIgual).trim();
                                double cantidadVendida = Double.parseDouble(cantidadConUnidad.split(" ")[0].replace(',', '.'));
                                double ventaUnitaria = Double.parseDouble(parteDatos.split(";")[0].split(":")[1]);
                                double costoUnitario = Double.parseDouble(parteDatos.split(";")[1].split(":")[1]);
                                
                                double descuento = 0;
                                if (i + 1 < lineas.size() && lineas.get(i + 1).trim().startsWith("Descuento:")) {
                                    String descuentoStr = lineas.get(i + 1).replaceAll("[^\\d]", "");
                                    if (!descuentoStr.isEmpty()) descuento = Double.parseDouble(descuentoStr);
                                }
                                
                                ReporteItem item = ventasAgregadas.computeIfAbsent(codigoProducto, k -> new ReporteItem(nombreProducto));
                                item.cantidadTotal += cantidadVendida;
                                item.ventaTotal += (ventaUnitaria * cantidadVendida) - descuento;
                                item.costoTotal += (costoUnitario * cantidadVendida);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error procesando resumen de factura: " + archivo.getName());
                }
            }
        }

        // --- Parte 2: Restar las DEVOLUCIONES del período (LÓGICA MEJORADA) ---
        File[] archivosDevolucion = PathsHelper.getDevolucionesFolder().listFiles((dir, name) -> name.startsWith("devolucion_"));
        if (archivosDevolucion != null) {
            
            for (File archivoDev : archivosDevolucion) {
                try {
                    List<String> lineas = Files.readAllLines(archivoDev.toPath());
                    
                    String fechaDevStr = lineas.stream().filter(l -> l.startsWith("Fecha de Devolución:")).map(l -> l.split(":", 2)[1].trim()).findFirst().orElse("");
                    if (fechaDevStr.isEmpty()) continue;
                    LocalDate fechaDevolucion = LocalDate.parse(fechaDevStr);
                
                    if (!fechaDevolucion.isBefore(fechaInicio) && !fechaDevolucion.isAfter(fechaFin)) {
                        String codigoProducto = lineas.stream().filter(l -> l.startsWith("Código:")).map(l -> l.split(":", 2)[1].trim()).findFirst().orElse("");
                        if (codigoProducto.isEmpty()) continue;
                
                        Producto productoDevuelto = mapaProductos.get(codigoProducto);
                        if (productoDevuelto == null) continue;
                
                        String areaProducto = productoDevuelto.getAreaEncargada() == null ? "" : productoDevuelto.getAreaEncargada();
                        boolean incluirEnReporte = "Todas las Áreas".equals(areaSeleccionada) || areaSeleccionada.equals(areaProducto);
                        if (!incluirEnReporte) {
                            continue;
                        }
                
                        String nombreProducto = lineas.stream().filter(l -> l.startsWith("Producto:")).map(l -> l.split(":", 2)[1].trim()).findFirst().orElse("Desconocido");
                        
                        // --- LÓGICA CORREGIDA PARA LEER NÚMEROS ---
                        String cantidadStr = lineas.stream().filter(l -> l.startsWith("Cantidad Devuelta:")).map(l -> l.split(":", 2)[1].trim()).findFirst().orElse("0");
                        String valorStr = lineas.stream().filter(l -> l.startsWith("Valor Devuelto:")).map(l -> l.split(":", 2)[1].trim()).findFirst().orElse("0");
                        String costoStr = lineas.stream().filter(l -> l.startsWith("Costo Unitario Original:")).map(l -> l.split(":", 2)[1].trim()).findFirst().orElse("0");
                
                        double cantidadDevuelta = Double.parseDouble(cantidadStr);
                        double valorDevuelto = Double.parseDouble(valorStr);
                        double costoUnitarioOriginal = Double.parseDouble(costoStr);
                        // --- FIN DE LA LÓGICA CORREGIDA ---
                
                        ReporteItem itemAfectado = ventasAgregadas.get(codigoProducto);
                        
                        if (itemAfectado != null) {
                            itemAfectado.cantidadTotal -= cantidadDevuelta;
                            itemAfectado.ventaTotal -= valorDevuelto;
                            itemAfectado.costoTotal -= (costoUnitarioOriginal * cantidadDevuelta);
                        } else {
                            ReporteItem itemDevolvido = new ReporteItem(nombreProducto);
                            itemDevolvido.cantidadTotal = -cantidadDevuelta;
                            itemDevolvido.ventaTotal = -valorDevuelto;
                            itemDevolvido.costoTotal = -(costoUnitarioOriginal * cantidadDevuelta);
                            ventasAgregadas.put(codigoProducto, itemDevolvido);
                        }
                    }
                } catch (Exception e) {
                    // Ya no debería entrar aquí, pero se deja por seguridad
                    System.err.println("Error procesando devolución para resumen: " + archivoDev.getName());
                }
            }
        }
        
        // --- Parte 3: Actualizar la tabla con los totales NETOS ---
        actualizarTablaYTotales(ventasAgregadas);
    }
    
    private void actualizarTablaYTotales(Map<String, ReporteItem> ventas) {
        modeloResumen.setRowCount(0);
        double granTotalVentas = 0, granTotalCosto = 0;

        for (ReporteItem item : ventas.values()) {
            modeloResumen.addRow(new Object[]{
                item.nombreProducto, item.cantidadTotal, formatoPesos.format(item.ventaTotal),
                formatoPesos.format(item.costoTotal), formatoPesos.format(item.getUtilidad())
            });
            granTotalVentas += item.ventaTotal;
            granTotalCosto += item.costoTotal;
        }
        double granUtilidad = granTotalVentas - granTotalCosto;
        lblVentasTotales.setText("Ventas Totales: " + formatoPesos.format(granTotalVentas));
        lblCostoTotal.setText("Costo Total: " + formatoPesos.format(granTotalCosto));
        lblUtilidadTotal.setText("Utilidad Total: " + formatoPesos.format(granUtilidad));
        lblUtilidadTotal.setForeground(granUtilidad >= 0 ? new Color(0, 128, 0) : Color.RED);
    }

    private void exportarACSV() {
        // Determina qué tabla está activa
        Component selectedComponent = tabbedPane.getSelectedComponent();
        if (selectedComponent == null) return;
        
        // Encuentra la JTable dentro del JScrollPane de la pestaña activa
        JScrollPane scrollPane = (JScrollPane) ((JPanel) selectedComponent).getComponent(0);
        JTable tablaAExportar = (JTable) scrollPane.getViewport().getView();
        DefaultTableModel modeloAExportar = (DefaultTableModel) tablaAExportar.getModel();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Reporte como CSV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            if (!archivo.getName().toLowerCase().endsWith(".csv")) {
                archivo = new File(archivo.getParentFile(), archivo.getName() + ".csv");
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
                for (int i = 0; i < modeloAExportar.getColumnCount(); i++) {
                    writer.write(modeloAExportar.getColumnName(i) + (i == modeloAExportar.getColumnCount() - 1 ? "" : ";"));
                }
                writer.newLine();

                for (int i = 0; i < modeloAExportar.getRowCount(); i++) {
                    for (int j = 0; j < modeloAExportar.getColumnCount(); j++) {
                        writer.write(modeloAExportar.getValueAt(i, j).toString().replaceAll("\\$", "").replaceAll("\\.", "") + (j == modeloAExportar.getColumnCount() - 1 ? "" : ";"));
                    }
                    writer.newLine();
                }
                JOptionPane.showMessageDialog(this, "Reporte exportado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al exportar el archivo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

        private static class ReporteItem {
            String nombreProducto;
            double cantidadTotal = 0.0;
            double ventaTotal = 0.0;
            double costoTotal = 0.0;
            
            ReporteItem(String nombreProducto) { this.nombreProducto = nombreProducto; }
            double getUtilidad() { return ventaTotal - costoTotal; }
        }

        private void cargarAreasDisponibles() {
        comboFiltroArea.removeAllItems();
        comboFiltroArea.addItem("Todas las Áreas");

        // Obtenemos una lista de áreas únicas y no vacías del inventario
        List<String> areas = ProductoStorage.cargarProductos().stream()
                                            .map(Producto::getAreaEncargada)
                                            .filter(area -> area != null && !area.isEmpty())
                                            .distinct()
                                            .sorted()
                                            .collect(Collectors.toList());

        for (String area : areas) {
            comboFiltroArea.addItem(area);
        }
    }
}