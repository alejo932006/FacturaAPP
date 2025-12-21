import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * GUI para generar un reporte de rentabilidad de la empresa en un período de tiempo.
 * Calcula Ventas Netas, Costo Neto, Utilidad Bruta, Gastos y Utilidad Neta.
 */
public class RentabilidadGUI extends JDialog {

    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    private JDateChooser selectorFechaInicio, selectorFechaFin;

    // Etiquetas para mostrar los resultados
    private JLabel valIngresosBrutos, valDevoluciones, valVentasNetas;
    private JLabel valCostoVentas, valCostoDevoluciones, valCostoNetoVentas;
    private JLabel valUtilidadBruta;
    private JLabel valGastosOperativos, valPagosCompromisos, valTotalGastos;
    private JLabel valUtilidadNeta;

    public RentabilidadGUI(Frame owner) {
        super(owner, "Análisis de Rentabilidad", true);
        setSize(700, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        formatoMoneda.setMaximumFractionDigits(0);

        initComponents();
    }

    private void initComponents() {
        // --- Panel de Controles (Fechas) ---
        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panelControles.setBorder(BorderFactory.createTitledBorder("Seleccione el Período"));
        selectorFechaInicio = new JDateChooser(new Date());
        selectorFechaFin = new JDateChooser(new Date());
        JButton btnGenerar = new JButton("Generar Reporte");
        panelControles.add(new JLabel("Desde:"));
        panelControles.add(selectorFechaInicio);
        panelControles.add(new JLabel("Hasta:"));
        panelControles.add(selectorFechaFin);
        panelControles.add(btnGenerar);

        // --- Panel Principal con los Resultados ---
        JPanel panelResultados = new JPanel(new GridBagLayout());
        panelResultados.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Inicializar todas las etiquetas
        valIngresosBrutos = createValueLabel(); valDevoluciones = createValueLabel(); valVentasNetas = createValueLabel(true);
        valCostoVentas = createValueLabel(); valCostoDevoluciones = createValueLabel(); valCostoNetoVentas = createValueLabel(true);
        valUtilidadBruta = createValueLabel(true);
        valGastosOperativos = createValueLabel(); valPagosCompromisos = createValueLabel(); valTotalGastos = createValueLabel(true);
        valUtilidadNeta = createValueLabel(true);
        valUtilidadNeta.setFont(new Font("Segoe UI", Font.BOLD, 22)); // Hacer la utilidad neta más grande

        int y = 0;
        // Ingresos
        addSectionTitle(panelResultados, gbc, y++, "INGRESOS");
        addRow(panelResultados, gbc, y++, "(+) Ingresos por Ventas Brutas:", valIngresosBrutos);
        addRow(panelResultados, gbc, y++, "(-) Devoluciones de Clientes:", valDevoluciones);
        addResultRow(panelResultados, gbc, y++, "(=) Ventas Netas (Ingresos Reales):", valVentasNetas);
        
        // Costos y Utilidad Bruta
        addSectionTitle(panelResultados, gbc, y++, "COSTO DE MERCANCÍA");
        addRow(panelResultados, gbc, y++, "(-) Costo de Mercancía Vendida (CMV):", valCostoVentas);
        addRow(panelResultados, gbc, y++, "(+) Costo de Mercancía Devuelta:", valCostoDevoluciones);
        addResultRow(panelResultados, gbc, y++, "(=) Costo Neto de Ventas:", valCostoNetoVentas);
        addResultRow(panelResultados, gbc, y++, "(=) Utilidad Bruta:", valUtilidadBruta);

        // Gastos
        addSectionTitle(panelResultados, gbc, y++, "GASTOS OPERATIVOS");
        addRow(panelResultados, gbc, y++, "(-) Gastos Registrados:", valGastosOperativos);
        addRow(panelResultados, gbc, y++, "(-) Pagos de Compromisos (Capital + Interés):", valPagosCompromisos);
        addResultRow(panelResultados, gbc, y++, "(=) Total Gastos Operativos:", valTotalGastos);
        
        // Utilidad Neta Final
        addSectionTitle(panelResultados, gbc, y++, "RESULTADO FINAL");
        addResultRow(panelResultados, gbc, y++, "(=) Utilidad Neta del Período:", valUtilidadNeta);

        // --- Ensamblaje ---
        add(panelControles, BorderLayout.NORTH);
        add(new JScrollPane(panelResultados), BorderLayout.CENTER);

        // --- Lógica del Botón ---
        btnGenerar.addActionListener(e -> generarReporte());
    }

    private void generarReporte() {
        if (selectorFechaInicio.getDate() == null || selectorFechaFin.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un rango de fechas.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        LocalDate inicio = selectorFechaInicio.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate fin = selectorFechaFin.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        // --- 1. Calcular Ingresos ---
        double[] ventas = calcularVentasNetas(inicio, fin);
        double ingresosBrutos = ventas[0];
        double devoluciones = ventas[1];
        double ventasNetas = ingresosBrutos - devoluciones;
        
        // --- 2. Calcular Costos ---
        double[] costos = calcularCostoNeto(inicio, fin);
        double costoVentas = costos[0];
        double costoDevoluciones = costos[1];
        double costoNeto = costoVentas - costoDevoluciones;

        // --- 3. Calcular Utilidad Bruta ---
        double utilidadBruta = ventasNetas - costoNeto;
        
        // --- 4. Calcular Gastos ---
        double[] gastos = calcularGastosOperativos(inicio, fin);
        double gastosOperativos = gastos[0];
        double pagosCompromisos = gastos[1];
        double totalGastos = gastosOperativos + pagosCompromisos;
        
        // --- 5. Calcular Utilidad Neta ---
        double utilidadNeta = utilidadBruta - totalGastos;

        // --- Actualizar la Interfaz ---
        valIngresosBrutos.setText(formatoMoneda.format(ingresosBrutos));
        valDevoluciones.setText(formatoMoneda.format(devoluciones));
        valVentasNetas.setText(formatoMoneda.format(ventasNetas));
        valCostoVentas.setText(formatoMoneda.format(costoVentas));
        valCostoDevoluciones.setText(formatoMoneda.format(costoDevoluciones));
        valCostoNetoVentas.setText(formatoMoneda.format(costoNeto));
        valUtilidadBruta.setText(formatoMoneda.format(utilidadBruta));
        colorearLabel(valUtilidadBruta, utilidadBruta);
        valGastosOperativos.setText(formatoMoneda.format(gastosOperativos));
        valPagosCompromisos.setText(formatoMoneda.format(pagosCompromisos));
        valTotalGastos.setText(formatoMoneda.format(totalGastos));
        valUtilidadNeta.setText(formatoMoneda.format(utilidadNeta));
        colorearLabel(valUtilidadNeta, utilidadNeta);
    }

    private double[] calcularVentasNetas(LocalDate inicio, LocalDate fin) {
        double totalVentas = 0, totalDevoluciones = 0;
        File[] archivosFactura = PathsHelper.getFacturasFolder().listFiles((dir, name) -> name.endsWith(".txt") && !name.startsWith("ANULADA_"));
        if (archivosFactura != null) {
            for (File archivo : archivosFactura) {
                try {
                    List<String> lineas = Files.readAllLines(archivo.toPath());
                    LocalDate fechaFactura = LocalDate.parse(lineas.stream().filter(l -> l.startsWith("Fecha:")).findFirst().orElse("").split(":")[1].trim());
                    if (!fechaFactura.isBefore(inicio) && !fechaFactura.isAfter(fin)) {
                        String totalLinea = lineas.stream().filter(l -> l.startsWith("TOTAL A PAGAR:")).findFirst().orElse("");
                        
                        // --- INICIO DE LA CORRECCIÓN ---
                        if (!totalLinea.isEmpty()) {
                            // Quitamos todo lo que NO sea un dígito para evitar errores con $, . o ,
                            String valorNumerico = totalLinea.replaceAll("[^\\d]", "");
                            if (!valorNumerico.isEmpty()) {
                                totalVentas += Double.parseDouble(valorNumerico);
                            }
                        }
                        // --- FIN DE LA CORRECCIÓN ---
                    }
                } catch (IOException | NumberFormatException e) { 
                    System.err.println("Error procesando factura: " + archivo.getName()); 
                }
            }
        }
        File[] archivosDevolucion = PathsHelper.getDevolucionesFolder().listFiles();
        if (archivosDevolucion != null) {
            for (File archivo : archivosDevolucion) {
                try {
                    List<String> lineas = Files.readAllLines(archivo.toPath());
                    LocalDate fechaDev = LocalDate.parse(lineas.stream().filter(l -> l.startsWith("Fecha de Devolución:")).findFirst().orElse("").split(":")[1].trim());
                    if (!fechaDev.isBefore(inicio) && !fechaDev.isAfter(fin)) {
                        String valorStr = lineas.stream().filter(l -> l.startsWith("Valor Devuelto:")).findFirst().orElse("");
                        totalDevoluciones += Double.parseDouble(valorStr.split(":")[1].trim());
                    }
                } catch (IOException | NumberFormatException e) { 
                    System.err.println("Error procesando devolución: " + archivo.getName()); 
                }
            }
        }
        return new double[]{totalVentas, totalDevoluciones};
    }
    
    private double[] calcularCostoNeto(LocalDate inicio, LocalDate fin) {
        double costoVentas = 0, costoDevoluciones = 0;
        File[] archivosFactura = PathsHelper.getFacturasFolder().listFiles((dir, name) -> name.endsWith(".txt") && !name.startsWith("ANULADA_"));
        if (archivosFactura != null) {
            for (File archivo : archivosFactura) {
                 try {
                    List<String> lineas = Files.readAllLines(archivo.toPath());
                    LocalDate fechaFactura = LocalDate.parse(lineas.stream().filter(l -> l.startsWith("Fecha:")).findFirst().orElse("").split(":")[1].trim());
                    if (!fechaFactura.isBefore(inicio) && !fechaFactura.isAfter(fin)) {
                        for(String linea : lineas) {
                            if(linea.contains("#venta_u:") && linea.contains("costo_u:")) {
                                String parteDatos = linea.split("#")[1];
                                double costoUnitario = Double.parseDouble(parteDatos.split(";")[1].split(":")[1]);
                                int indiceIgual = linea.lastIndexOf(" = ");
                                int indiceUltimaX = linea.lastIndexOf(" x ", indiceIgual);
                                String cantidadConUnidad = linea.substring(indiceUltimaX + 3, indiceIgual).trim();
                                String cantStr = cantidadConUnidad.split(" ")[0].replace(',', '.');
                                double cantidad = Double.parseDouble(cantStr);
                                costoVentas += costoUnitario * cantidad;
                            }
                        }
                    }
                } catch (Exception e) { 
                    System.err.println("Error procesando costo de factura: " + archivo.getName()); 
                }
            }
        }
        File[] archivosDevolucion = PathsHelper.getDevolucionesFolder().listFiles();
        if (archivosDevolucion != null) {
             for (File archivo : archivosDevolucion) {
                try {
                    List<String> lineas = Files.readAllLines(archivo.toPath());
                    LocalDate fechaDev = LocalDate.parse(lineas.stream().filter(l -> l.startsWith("Fecha de Devolución:")).findFirst().orElse("").split(":")[1].trim());
                    if (!fechaDev.isBefore(inicio) && !fechaDev.isAfter(fin)) {
                        double costoUnit = Double.parseDouble(lineas.stream().filter(l->l.startsWith("Costo Unitario Original:")).findFirst().orElse("").split(":")[1].trim());
                        double cant = Double.parseDouble(lineas.stream().filter(l->l.startsWith("Cantidad Devuelta:")).findFirst().orElse("").split(":")[1].trim());
                        costoDevoluciones += costoUnit * cant;
                    }
                } catch (Exception e) { 
                    System.err.println("Error procesando costo de devolución: " + archivo.getName()); 
                }
            }
        }
        return new double[]{costoVentas, costoDevoluciones};
    }

    private double[] calcularGastosOperativos(LocalDate inicio, LocalDate fin) {
        double totalGastos = GastoStorage.cargarGastos().stream()
            .filter(g -> !g.getFecha().isBefore(inicio) && !g.getFecha().isAfter(fin))
            .mapToDouble(Gasto::getMonto)
            .sum();
        
            double totalPagosCompromisos = AbonoStorage.cargarTodosLosAbonos().stream()
            .filter(a -> !a.getFecha().isBefore(inicio) && !a.getFecha().isAfter(fin))
            .mapToDouble(Abono::getMontoTotal)
            .sum();

            return new double[]{totalGastos, totalPagosCompromisos};
    }

    // --- Métodos de ayuda para construir la GUI ---
    private JLabel createValueLabel() { return createValueLabel(false); }
    private JLabel createValueLabel(boolean isBold) {
        JLabel label = new JLabel("$0");
        label.setFont(new Font("Segoe UI", isBold ? Font.BOLD : Font.PLAIN, 16));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int y, String title, JLabel valueLabel) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        gbc.gridx = 0; gbc.gridy = y; gbc.anchor = GridBagConstraints.WEST; p.add(titleLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST; p.add(valueLabel, gbc);
    }
    
    private void addResultRow(JPanel p, GridBagConstraints gbc, int y, String title, JLabel valueLabel) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = y; gbc.anchor = GridBagConstraints.WEST; p.add(titleLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST; p.add(valueLabel, gbc);
    }
    
    private void addSectionTitle(JPanel p, GridBagConstraints gbc, int y, String title) {
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        border.setTitleColor(Color.BLUE);
        JSeparator separator = new JSeparator();
        separator.setBorder(border);
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2; gbc.insets = new Insets(15, 0, 5, 0);
        p.add(separator, gbc);
        gbc.gridwidth = 1; gbc.insets = new Insets(8, 5, 8, 5);
    }
    
    private void colorearLabel(JLabel label, double valor) {
        if (valor >= 0) {
            label.setForeground(new Color(0, 128, 0)); // Verde
        } else {
            label.setForeground(Color.RED);
        }
    }
}