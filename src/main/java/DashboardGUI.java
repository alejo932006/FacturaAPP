import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DashboardGUI extends JFrame {

    // 1. Variable estática para guardar la única instancia del Dashboard
    private static DashboardGUI instance;

    // 2. Método público estático para obtener esa instancia
    public static DashboardGUI getInstance() {
        return instance;
    }

    // 3. Método público para que otras ventanas puedan pedirle que se actualice
    public void refrescarDatos() {
        actualizarMetricas();
    }

    // --- Componentes de la Interfaz ---
    private JLabel lblValorVentas, lblValorFacturas, lblValorBajoStock, lblValorClientes, lblValorGastos, lblValorDevoluciones;
    private DefaultListModel<String> modeloListaVencimientos;
    private JComboBox<String> comboFiltroFecha;
    private JDateChooser selectorFechaInicio;
    private JDateChooser selectorFechaFin;
    private JLabel lblSaldoCaja, lblSaldoBanco;

    public DashboardGUI() {
        instance = this;
        setTitle("Panel Principal - Sistema de Facturación");
        setSize(1000, 700); 
        setExtendedState(JFrame.MAXIMIZED_BOTH); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents();
        actualizarMetricas();
        setVisible(true);
    }

    private void initComponents() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel de Encabezado ---
        JPanel panelEncabezado = new JPanel(new BorderLayout());
        JLabel lblTitulo = new JLabel("Resumen del Negocio", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        JLabel lblBienvenida = new JLabel("Bienvenido, " + UserSession.getLoggedInUsername(), SwingConstants.CENTER);
        lblBienvenida.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        panelEncabezado.add(lblTitulo, BorderLayout.NORTH);
        panelEncabezado.add(lblBienvenida, BorderLayout.CENTER);
        
        // --- Panel de Filtros de Fecha ---
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panelFiltros.setBorder(BorderFactory.createTitledBorder("Seleccionar Período"));
        comboFiltroFecha = new JComboBox<>(new String[]{"Hoy", "Este Mes", "Rango Personalizado"});
        selectorFechaInicio = new JDateChooser();
        selectorFechaFin = new JDateChooser();
        JButton btnActualizar = new JButton("Actualizar");
        panelFiltros.add(new JLabel("Mostrar datos de:"));
        panelFiltros.add(comboFiltroFecha);
        panelFiltros.add(new JLabel("Desde:"));
        panelFiltros.add(selectorFechaInicio);
        panelFiltros.add(new JLabel("Hasta:"));
        panelFiltros.add(selectorFechaFin);
        panelFiltros.add(btnActualizar);
        panelEncabezado.add(panelFiltros, BorderLayout.SOUTH);
        
        panelPrincipal.add(panelEncabezado, BorderLayout.NORTH);

        // --- Panel de Métricas ---
        JPanel panelMetricas = new JPanel(new GridLayout(2, 3, 15, 15));
        panelMetricas.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        lblValorVentas = new JLabel("$0", SwingConstants.CENTER);
        lblValorFacturas = new JLabel("0", SwingConstants.CENTER);
        lblValorGastos = new JLabel("$0", SwingConstants.CENTER);
        lblValorDevoluciones = new JLabel("0", SwingConstants.CENTER);
        lblValorBajoStock = new JLabel("0", SwingConstants.CENTER);
        lblValorClientes = new JLabel("0", SwingConstants.CENTER);
        lblSaldoCaja = new JLabel("$0", SwingConstants.CENTER);
        lblSaldoBanco = new JLabel("$0", SwingConstants.CENTER);

        panelMetricas.add(crearTarjetaMetrica("Ventas del Período", lblValorVentas, new Color(223, 240, 216)));
        panelMetricas.add(crearTarjetaMetrica("Saldo Actual en Caja", lblSaldoCaja, new Color(217, 237, 247))); // Nueva
        panelMetricas.add(crearTarjetaMetrica("Saldo Actual en Banco", lblSaldoBanco, new Color(232, 234, 237))); // Nueva
        panelMetricas.add(crearTarjetaMetrica("Facturas del Período", lblValorFacturas, new Color(217, 237, 247)));
        panelMetricas.add(crearTarjetaMetrica("Gastos del Período", lblValorGastos, new Color(252, 225, 225)));
        panelMetricas.add(crearTarjetaMetrica("Devoluciones del Período", lblValorDevoluciones, new Color(255, 243, 205))); // Color anaranjado
        panelMetricas.add(crearTarjetaMetrica("Productos Bajos de Stock (< 5)", lblValorBajoStock, new Color(255, 243, 205)));
        panelMetricas.add(crearTarjetaMetrica("Total Clientes Registrados", lblValorClientes, new Color(232, 234, 237)));
        
        // --- Panel de Vencimientos (VERSIÓN CORREGIDA) ---
        // 1. Creamos un panel contenedor para la lista de vencimientos.
        JPanel panelVencimientos = new JPanel(new BorderLayout());
        // 2. Le ponemos el borde al panel contenedor, NO al JScrollPane.
        panelVencimientos.setBorder(BorderFactory.createTitledBorder("Próximos Vencimientos (5 días)"));
        // 3. Le damos un ancho preferido para que no sea demasiado pequeño.
        panelVencimientos.setPreferredSize(new Dimension(300, 0));

        modeloListaVencimientos = new DefaultListModel<>();
        JList<String> listaVencimientos = new JList<>(modeloListaVencimientos);
        listaVencimientos.setCellRenderer(new VencimientoRenderer());
        JScrollPane scrollVencimientos = new JScrollPane(listaVencimientos);

        // 4. Añadimos la lista (con su scroll) al panel contenedor.
        panelVencimientos.add(scrollVencimientos, BorderLayout.CENTER);

        // --- Ensamblaje (VERSIÓN CORREGIDA) ---
        // Ya no usamos JSplitPane. Añadimos los paneles directamente al BorderLayout del panel principal.
        panelPrincipal.add(panelMetricas, BorderLayout.CENTER); // Las tarjetas van en el centro.
        panelPrincipal.add(panelVencimientos, BorderLayout.EAST);   // La lista de vencimientos va a la derecha (este).

        JToolBar barraNavegacion = new JToolBar();

        barraNavegacion.setFloatable(false);
        JButton btnFacturacion = new JButton("Ir a Facturación");
        
        btnFacturacion.setBackground(new Color(40, 167, 69)); // Tono de verde
        btnFacturacion.setForeground(Color.WHITE); // Texto en color blanco
        btnFacturacion.setOpaque(true);
        btnFacturacion.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnFacturacion.setFocusPainted(false); // Opcional: quita el borde al hacer clic

        // Asegúrate de que el usuario tenga permiso antes de abrir la ventana
        if (UserSession.tienePermiso("FACTURAR")) {
            btnFacturacion.addActionListener(e -> new FacturaGUI(this).setVisible(true));
        } else {
            btnFacturacion.setEnabled(false);
        }

        barraNavegacion.add(btnFacturacion);
        panelPrincipal.add(barraNavegacion, BorderLayout.SOUTH);

        setJMenuBar(crearMenuBar());
        add(panelPrincipal);

        // --- Lógica de los Filtros ---
        comboFiltroFecha.addActionListener(e -> configurarFechas());
        btnActualizar.addActionListener(e -> actualizarMetricas());
        configurarFechas();
    }
    
    private void configurarFechas() {
        String seleccion = (String) comboFiltroFecha.getSelectedItem();
        LocalDate hoy = LocalDate.now();
        
        if ("Hoy".equals(seleccion)) {
            selectorFechaInicio.setDate(Date.from(hoy.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            selectorFechaFin.setDate(Date.from(hoy.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            selectorFechaInicio.setEnabled(false);
            selectorFechaFin.setEnabled(false);
        } else if ("Este Mes".equals(seleccion)) {
            LocalDate primerDia = hoy.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate ultimoDia = hoy.with(TemporalAdjusters.lastDayOfMonth());
            selectorFechaInicio.setDate(Date.from(primerDia.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            selectorFechaFin.setDate(Date.from(ultimoDia.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            selectorFechaInicio.setEnabled(false);
            selectorFechaFin.setEnabled(false);
        } else {
            selectorFechaInicio.setEnabled(true);
            selectorFechaFin.setEnabled(true);
        }
    }

    private void actualizarMetricas() {
        if (selectorFechaInicio.getDate() == null || selectorFechaFin.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un rango de fechas válido.", "Fechas Requeridas", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate inicio = selectorFechaInicio.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate fin = selectorFechaFin.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        lblValorVentas.setText(calcularVentas(inicio, fin));
        lblValorFacturas.setText(contarFacturas(inicio, fin));
        lblValorGastos.setText(calcularGastos(inicio, fin));
        lblValorDevoluciones.setText(contarDevoluciones(inicio, fin));
        lblValorBajoStock.setText(contarProductosBajosDeStock());
        lblValorClientes.setText(contarClientes());
        NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        formato.setMaximumFractionDigits(0);
        lblSaldoCaja.setText(formato.format(CuentasStorage.getSaldoCaja()));
        lblSaldoBanco.setText(formato.format(CuentasStorage.getSaldoBanco()));
        
        actualizarVencimientos();
    }
    
    private String calcularGastos(LocalDate inicio, LocalDate fin) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        formato.setMaximumFractionDigits(0);

        double totalGastos = GastoStorage.cargarGastos().stream()
            .filter(gasto -> !gasto.getFecha().isBefore(inicio) && !gasto.getFecha().isAfter(fin))
            .mapToDouble(Gasto::getMonto)
            .sum();
        return formatearNumeroAbreviado(totalGastos);
    }

    // Reemplaza tu método calcularVentas() existente con este:

    private String calcularVentas(LocalDate inicio, LocalDate fin) {
        double totalVentasBrutas = 0;
        double totalDevoluciones = 0;

        // --- Parte 1: Sumar las ventas de las facturas VIGENTES ---
        File[] archivosFactura = PathsHelper.getFacturasFolder().listFiles((dir, name) -> name.endsWith(".txt"));
        if (archivosFactura != null) {
            for (File archivo : archivosFactura) {
                // MEJORA: Ignora las facturas anuladas
                if (archivo.getName().startsWith("ANULADA_")) {
                    continue;
                }
                try {
                    List<String> lineas = Files.readAllLines(archivo.toPath());
                    String fechaStr = lineas.stream().filter(l -> l.startsWith("Fecha:")).findFirst().orElse("").split(":")[1].trim();
                    LocalDate fechaFactura = LocalDate.parse(fechaStr);
                    
                    // Comprueba si la factura está dentro del rango
                    if (!fechaFactura.isBefore(inicio) && !fechaFactura.isAfter(fin)) {
                        String totalLinea = lineas.stream().filter(l -> l.startsWith("TOTAL A PAGAR:")).findFirst().orElse("");
                        if (!totalLinea.isEmpty()) {
                            String valorNumerico = totalLinea.replaceAll("[^\\d]", "");
                            totalVentasBrutas += Double.parseDouble(valorNumerico);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error al procesar la factura " + archivo.getName() + ": " + e.getMessage());
                }
            }
        }

        // --- Parte 2: Restar el valor de las DEVOLUCIONES en el período ---
        File[] archivosDevolucion = PathsHelper.getDevolucionesFolder().listFiles((dir, name) -> name.startsWith("devolucion_"));
        if (archivosDevolucion != null) {
            for (File archivoDev : archivosDevolucion) {
                try {
                    List<String> lineas = Files.readAllLines(archivoDev.toPath());
                    String fechaDevStr = lineas.stream().filter(l -> l.startsWith("Fecha de Devolución:")).findFirst().get().split(":")[1].trim();
                    LocalDate fechaDevolucion = LocalDate.parse(fechaDevStr);

                    // Comprueba si la devolución está dentro del rango
                    if (!fechaDevolucion.isBefore(inicio) && !fechaDevolucion.isAfter(fin)) {
                        String valorDevueltoStr = lineas.stream().filter(l -> l.startsWith("Valor Devuelto:")).findFirst().get().split(":")[1].trim();
                        totalDevoluciones += Double.parseDouble(valorDevueltoStr);
                    }
                } catch (Exception e) {
                    System.err.println("Error procesando devolución: " + archivoDev.getName() + " -> " + e.getMessage());
                }
            }
        }
        
        // --- Parte 3: Calcular el total neto y formatear ---
        double ventasNetas = totalVentasBrutas - totalDevoluciones;
        NumberFormat formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        formatoPesos.setMaximumFractionDigits(0);
        return formatoPesos.format(ventasNetas);
    }
    
    private String contarFacturas(LocalDate inicio, LocalDate fin) {
        int contador = 0;
        File[] archivos = PathsHelper.getFacturasFolder().listFiles((dir, name) -> name.endsWith(".txt"));
        if (archivos == null) return "0";
        for (File archivo : archivos) {
            try {
                String fechaStr = Files.lines(archivo.toPath()).filter(l -> l.startsWith("Fecha:")).findFirst().orElse("").split(":")[1].trim();
                LocalDate fechaFactura = LocalDate.parse(fechaStr);
                
                if (!fechaFactura.isBefore(inicio) && !fechaFactura.isAfter(fin)) {
                    contador++;
                }
            } catch (Exception e) { /* Ignorar errores */ }
        }
        return String.valueOf(contador);
    }

    private String contarProductosBajosDeStock() {
        long count = ProductoStorage.cargarProductos().stream()
            .filter(p -> p.getCantidad() < 5 && p.getEstado().equals("Activo"))
            .count();
        return String.valueOf(count);
    }

    private String contarClientes() {
        return String.valueOf(ClienteStorage.cargarClientes().size());
    }
    
    private void actualizarVencimientos() {
        modeloListaVencimientos.clear();
        List<Compromiso> compromisos = CompromisoStorage.cargarCompromisos();
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(5);

        List<Compromiso> proximos = compromisos.stream()
            .filter(c -> {
                List<Abono> abonos = AbonoStorage.cargarTodosLosAbonos();
                double capitalAbonado = abonos.stream()
                    .filter(a -> a.getCompromisoId().equals(c.getId()))
                    .mapToDouble(Abono::getMontoCapital).sum();
                return (c.getMonto() - capitalAbonado) > 0;
            })
            .filter(c -> !c.getFechaVencimiento().isBefore(hoy))
            .filter(c -> !c.getFechaVencimiento().isAfter(limite))
            .sorted((c1, c2) -> c1.getFechaVencimiento().compareTo(c2.getFechaVencimiento()))
            .collect(Collectors.toList());

        if (proximos.isEmpty()) {
            modeloListaVencimientos.addElement("¡Felicidades! No hay vencimientos próximos.");
        } else {
            NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
            formato.setMaximumFractionDigits(0);
            for (Compromiso c : proximos) {
                modeloListaVencimientos.addElement(String.format("<html><b>Vence %s:</b> %s <font color='gray'>%s</font></html>",
                    c.getFechaVencimiento().toString(),
                    formato.format(c.getMonto()),
                    c.getDescripcion()
                ));
            }
        }
    }
    
    private JPanel crearTarjetaMetrica(String titulo, JLabel labelValor, Color colorFondo) {
        JPanel tarjeta = new JPanel(new BorderLayout());
        tarjeta.setBackground(colorFondo);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(colorFondo.darker(), 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        labelValor.setFont(new Font("Segoe UI", Font.BOLD, 28));
        JLabel lblTituloTarjeta = new JLabel(titulo, SwingConstants.CENTER);
        lblTituloTarjeta.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        tarjeta.add(labelValor, BorderLayout.CENTER);
        tarjeta.add(lblTituloTarjeta, BorderLayout.SOUTH);

        return tarjeta;
    }

    private JMenuBar crearMenuBar() {
        JMenuBar barraMenu = new JMenuBar();
        JMenu menuOpciones = new JMenu("Opciones");
        JMenu menuArchivo = new JMenu("Archivo");

        JMenuItem itemConsultarFacturas = new JMenuItem("Consultar Facturas");
        itemConsultarFacturas.addActionListener(e -> new ConsultarFacturasGUI(null));
        menuOpciones.add(itemConsultarFacturas);

        JMenuItem itemGestionarProductos = new JMenuItem("Gestionar Productos");
        itemGestionarProductos.addActionListener(e -> new CrearProductoGUI(null).setVisible(true));
        menuOpciones.add(itemGestionarProductos);

        JMenuItem itemConsultarOrdenes = new JMenuItem("Consultar Órdenes/Reportes");
        itemConsultarOrdenes.addActionListener(e -> new ConsultarOrdenesGUI(this));
        menuOpciones.add(itemConsultarOrdenes);

        menuOpciones.addSeparator();

        JMenuItem itemPedidosWeb = new JMenuItem("Gestionar Pedidos Web");
        itemPedidosWeb.addActionListener(e -> new PedidosWebGUI().setVisible(true)); 
        menuOpciones.add(itemPedidosWeb);

        menuOpciones.addSeparator();

        JMenuItem itemGestionarGastos = new JMenuItem("Gestionar Gastos");
        itemGestionarGastos.addActionListener(e -> new GastosGUI(null).setVisible(true));
        menuOpciones.add(itemGestionarGastos);

        JMenuItem itemGestionarCompromisos = new JMenuItem("Gestionar Compromisos");
        itemGestionarCompromisos.addActionListener(e -> new CompromisosGUI(this).setVisible(true));
        //itemGestionarCompromisos.setEnabled(UserSession.tienePermiso("GESTIONAR_COMPROMISOS"));
        menuOpciones.add(itemGestionarCompromisos);

        menuOpciones.addSeparator();

        JMenuItem itemAjustesSaldo = new JMenuItem("Ajustes de Caja y Banco");
        itemAjustesSaldo.addActionListener(e -> new AjustesSaldoGUI(this).setVisible(true));
        itemAjustesSaldo.setEnabled(UserSession.tienePermiso("GESTIONAR_AJUSTES_SALDO"));
        menuOpciones.add(itemAjustesSaldo);

        menuOpciones.addSeparator();
        

        JMenuItem itemGestionCaja = new JMenuItem("Control de Caja (Arqueo)");
        itemGestionCaja.addActionListener(e -> new CajaGUI(this).setVisible(true));
        itemGestionCaja.setEnabled(UserSession.tienePermiso("GESTIONAR_CAJA"));
        menuOpciones.add(itemGestionCaja);
        
        JMenuItem itemConsultarArqueos = new JMenuItem("Consultar Historial de Arqueos");
        itemConsultarArqueos.addActionListener(e -> new ConsultarArqueosGUI(this).setVisible(true));
        itemConsultarArqueos.setEnabled(UserSession.tienePermiso("GESTIONAR_CAJA"));
        menuOpciones.add(itemConsultarArqueos);

        menuOpciones.addSeparator();

        JMenuItem itemRentabilidad = new JMenuItem("Análisis de Rentabilidad");
        itemRentabilidad.setFont(new Font("Segoe UI", Font.BOLD, 12));
        itemRentabilidad.addActionListener(e -> new RentabilidadGUI(this).setVisible(true));
        itemRentabilidad.setEnabled(UserSession.tienePermiso("VER_REPORTES_FINANCIEROS"));
        menuOpciones.add(itemRentabilidad);

        menuOpciones.addSeparator();
        
        JMenuItem itemConfiguracion = new JMenuItem("Configurar Empresa");
        itemConfiguracion.addActionListener(e -> new ConfiguracionEmpresaGUI(null).setVisible(true));
        itemConfiguracion.setEnabled(UserSession.tienePermiso("CONFIGURAR_EMPRESA"));
        menuOpciones.add(itemConfiguracion);

        JMenuItem itemCrearBackup = new JMenuItem("Crear Copia de Seguridad...");
        itemCrearBackup.addActionListener(e -> BackupManager.crearCopiaDeSeguridad(this));
        menuArchivo.add(itemCrearBackup);

        JMenuItem itemRestaurarBackup = new JMenuItem("Restaurar Copia de Seguridad...");
        itemRestaurarBackup.addActionListener(e -> BackupManager.restaurarCopiaDeSeguridad(this));
        menuArchivo.add(itemRestaurarBackup);

        barraMenu.add(menuOpciones);
        barraMenu.add(menuArchivo);
        return barraMenu;
    }
    
    class VencimientoRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String text = value.toString();
            if (text.contains(LocalDate.now().toString())) {
                label.setBackground(isSelected ? new Color(255, 100, 100) : new Color(255, 220, 220));
            } else if (text.contains(LocalDate.now().plusDays(1).toString())) {
                label.setBackground(isSelected ? new Color(255, 180, 100) : new Color(255, 240, 220));
            }
            return label;
        }
    }

    // --- MÉTODO NUEVO PARA ABREVIAR NÚMEROS ---
    private String formatearNumeroAbreviado(double valor) {
        if (valor < 1_000_000) {
            // Si es menor a un millón, lo muestra normal
            NumberFormat formatoNormal = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
            formatoNormal.setMaximumFractionDigits(0);
            return formatoNormal.format(valor);
        }
        if (valor < 1_000_000_000) {
            // Si está en los millones, lo muestra como "M"
            double valorEnMillones = valor / 1_000_000.0;
            return String.format(Locale.US, "$ %.1f M", valorEnMillones);
        }
        // Si es más grande, lo muestra como "B" (Billones)
        double valorEnBillones = valor / 1_000_000_000.0;
        return String.format(Locale.US, "$ %.1f B", valorEnBillones);
    }

    // --- MÉTODO NUEVO PARA CONTAR DEVOLUCIONES ---
    private String contarDevoluciones(LocalDate inicio, LocalDate fin) {
        int contador = 0;
        File[] archivosDevolucion = PathsHelper.getDevolucionesFolder().listFiles((dir, name) -> name.startsWith("devolucion_"));
        if (archivosDevolucion == null) return "0";

        for (File archivoDev : archivosDevolucion) {
            try {
                List<String> lineas = Files.readAllLines(archivoDev.toPath());
                String fechaDevStr = lineas.stream()
                    .filter(l -> l.startsWith("Fecha de Devolución:"))
                    .findFirst().get().split(":")[1].trim();
                LocalDate fechaDevolucion = LocalDate.parse(fechaDevStr);

                // Comprueba si la devolución está dentro del rango
                if (!fechaDevolucion.isBefore(inicio) && !fechaDevolucion.isAfter(fin)) {
                    contador++;
                }
            } catch (Exception e) {
                // Ignorar si un archivo de devolución está corrupto o no se puede leer
                System.err.println("Error procesando archivo de devolución: " + archivoDev.getName());
            }
        }
        return String.valueOf(contador);
    }
}