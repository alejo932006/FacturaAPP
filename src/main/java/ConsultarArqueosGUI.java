import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.table.DefaultTableCellRenderer;

public class ConsultarArqueosGUI extends JDialog {

    private JTable tablaArqueos;
    private DefaultTableModel modeloTabla;
    private JDateChooser selectorFechaInicio, selectorFechaFin;
    private JLabel lblTotalBase, lblTotalVentas, lblTotalGastos, lblTotalDiferencia;
    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));

    public ConsultarArqueosGUI(Frame owner) {
        super(owner, "Historial de Arqueos de Caja", true);
        setSize(900, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        formatoMoneda.setMaximumFractionDigits(0);

        initComponents();
        filtrarYMostrarArqueos(); // Carga inicial
    }

    private void initComponents() {
        // --- Panel de Filtros Superior ---
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panelFiltros.setBorder(BorderFactory.createTitledBorder("Filtrar por Fecha"));
        
        selectorFechaInicio = new JDateChooser(new Date());
        selectorFechaFin = new JDateChooser(new Date());
        JButton btnConsultar = new JButton("Consultar");

        panelFiltros.add(new JLabel("Desde:"));
        panelFiltros.add(selectorFechaInicio);
        panelFiltros.add(new JLabel("Hasta:"));
        panelFiltros.add(selectorFechaFin);
        panelFiltros.add(btnConsultar);

        // --- Tabla Principal ---
        String[] columnas = {"Fecha", "Estado", "Base Inicial", "Ventas Contado", "Gastos", "Efectivo Esperado", "Efectivo Real", "Diferencia"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaArqueos = new JTable(modeloTabla);
        tablaArqueos.setDefaultRenderer(Object.class, new EstadoArqueoRenderer());
        
        // --- Panel de Totales Inferior ---
        JPanel panelTotales = new JPanel(new GridLayout(1, 4, 10, 5));
        panelTotales.setBorder(BorderFactory.createTitledBorder("Totales para el Período Seleccionado"));
        Font fontTotales = new Font("Segoe UI", Font.BOLD, 14);

        lblTotalBase = new JLabel("Base: $0");
        lblTotalVentas = new JLabel("Ventas: $0");
        lblTotalGastos = new JLabel("Gastos: $0");
        lblTotalDiferencia = new JLabel("Diferencia: $0");
        
        lblTotalBase.setFont(fontTotales);
        lblTotalVentas.setFont(fontTotales);
        lblTotalGastos.setFont(fontTotales);
        lblTotalDiferencia.setFont(fontTotales);

        panelTotales.add(lblTotalBase);
        panelTotales.add(lblTotalVentas);
        panelTotales.add(lblTotalGastos);
        panelTotales.add(lblTotalDiferencia);

        // --- Ensamblaje ---
        add(panelFiltros, BorderLayout.NORTH);
        add(new JScrollPane(tablaArqueos), BorderLayout.CENTER);
        add(panelTotales, BorderLayout.SOUTH);

        // --- Lógica de Eventos ---
        btnConsultar.addActionListener(e -> filtrarYMostrarArqueos());
    }

    private void filtrarYMostrarArqueos() {
        modeloTabla.setRowCount(0);
        if (selectorFechaInicio.getDate() == null || selectorFechaFin.getDate() == null) return;
        
        LocalDate fechaInicio = selectorFechaInicio.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate fechaFin = selectorFechaFin.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        List<ArqueoCaja> todosLosArqueos = CajaStorage.cargarArqueos();
        double totalBase = 0, totalVentas = 0, totalGastos = 0, totalDiferencia = 0;

        for (ArqueoCaja arqueo : todosLosArqueos) {
            LocalDate fechaArqueo = arqueo.getFecha();
            if (!fechaArqueo.isBefore(fechaInicio) && !fechaArqueo.isAfter(fechaFin)) {
                modeloTabla.addRow(new Object[]{
                    arqueo.getFecha().toString(),
                    arqueo.getEstado(),
                    formatoMoneda.format(arqueo.getBaseInicial()),
                    formatoMoneda.format(arqueo.getVentasContado()),
                    formatoMoneda.format(arqueo.getGastosEfectivo()),
                    formatoMoneda.format(arqueo.getEfectivoEsperado()),
                    formatoMoneda.format(arqueo.getEfectivoFinalReal()),
                    formatoMoneda.format(arqueo.getDiferencia())
                });

                // Sumar a los totales
                totalBase += arqueo.getBaseInicial();
                totalVentas += arqueo.getVentasContado();
                totalGastos += arqueo.getGastosEfectivo();
                totalDiferencia += arqueo.getDiferencia();
            }
        }
        
        // Actualizar los JLabels de totales
        lblTotalBase.setText("Base: " + formatoMoneda.format(totalBase));
        lblTotalVentas.setText("Ventas: " + formatoMoneda.format(totalVentas));
        lblTotalGastos.setText("Gastos: " + formatoMoneda.format(totalGastos));
        lblTotalDiferencia.setText("Diferencia: " + formatoMoneda.format(totalDiferencia));
        
        // Colorear la diferencia total
        if (totalDiferencia > 0) lblTotalDiferencia.setForeground(new Color(0, 128, 0));
        else if (totalDiferencia < 0) lblTotalDiferencia.setForeground(Color.RED);
        else lblTotalDiferencia.setForeground(Color.BLACK);
    }
    
    // Clase interna para dar color a las filas según el estado y la diferencia
    class EstadoArqueoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                int modelRow = table.convertRowIndexToModel(row);
                ArqueoCaja.Estado estado = (ArqueoCaja.Estado) table.getModel().getValueAt(modelRow, 1);
                String diferenciaStr = table.getModel().getValueAt(modelRow, 7).toString();
                
                try {
                    double diferencia = formatoMoneda.parse(diferenciaStr).doubleValue();
                    if (estado == ArqueoCaja.Estado.ABIERTA) {
                        c.setBackground(new Color(217, 237, 247)); // Azul claro
                    } else if (diferencia < 0) {
                        c.setBackground(new Color(255, 228, 225)); // Rojo claro
                    } else if (diferencia > 0) {
                        c.setBackground(new Color(255, 248, 225)); // Amarillo claro
                    } else {
                        c.setBackground(new Color(223, 240, 216)); // Verde claro
                    }
                } catch (Exception e) {
                    c.setBackground(table.getBackground());
                }
            }
            return c;
        }
    }
    }