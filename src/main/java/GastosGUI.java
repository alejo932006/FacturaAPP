import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GastosGUI extends JDialog {

    private DefaultTableModel modeloTablaPrincipal, modeloTablaRecientes;
    private JTable tablaGastos;
    private JDateChooser selectorFecha, selectorFechaInicio, selectorFechaFin;
    private JTextField txtDescripcion, txtMonto;
    private JLabel lblTotalGastos;
    private JComboBox<String> comboMetodoPago;    
    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    private List<Gasto> listaDeGastos = new ArrayList<>();

    public GastosGUI(Frame owner) {
        super(owner, "Gestión de Gastos", true);
        setSize(900, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        formatoMoneda.setMaximumFractionDigits(0);
        
        initComponents();
        cargarGastosIniciales();
    }

    private void initComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Registrar Gasto", createRegisterPanel());
        tabbedPane.addTab("Consultar y Editar", createConsultPanel());
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createRegisterPanel() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(20, 0));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBorder(BorderFactory.createTitledBorder("Nuevo Gasto"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        selectorFecha = createCustomDateChooser();
        selectorFecha.setDate(new Date());
        txtDescripcion = new JTextField(20);
        txtMonto = new JTextField(12);
        JButton btnAgregar = new JButton("Agregar Gasto");
        gbc.gridx = 0; gbc.gridy = 0; panelFormulario.add(new JLabel("Fecha:"), gbc);
        gbc.gridx = 1; panelFormulario.add(selectorFecha, gbc);
        gbc.gridy++; gbc.gridx = 0; panelFormulario.add(new JLabel("Descripción:"), gbc);
        gbc.gridx = 1; panelFormulario.add(txtDescripcion, gbc);
        gbc.gridy++; gbc.gridx = 0; panelFormulario.add(new JLabel("Monto:"), gbc);
        gbc.gridx = 1; panelFormulario.add(txtMonto, gbc);
        gbc.gridy++; gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; panelFormulario.add(btnAgregar, gbc);
        btnAgregar.addActionListener(e -> agregarGasto());
        String[] columnasRecientes = {"Fecha", "Descripción", "Monto"};
        comboMetodoPago = new JComboBox<>(new String[]{"Efectivo", "Transferencia"});
        gbc.gridy++; gbc.gridx = 0; panelFormulario.add(new JLabel("Pagado con:"), gbc);
        gbc.gridx = 1; panelFormulario.add(comboMetodoPago, gbc);
        modeloTablaRecientes = new DefaultTableModel(columnasRecientes, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable tablaRecientes = new JTable(modeloTablaRecientes);
        JScrollPane scrollRecientes = new JScrollPane(tablaRecientes);
        scrollRecientes.setBorder(BorderFactory.createTitledBorder("Últimos 5 Gastos Registrados"));
        panelPrincipal.add(panelFormulario, BorderLayout.WEST);
        panelPrincipal.add(scrollRecientes, BorderLayout.CENTER);
        return panelPrincipal;
    }

    private JPanel createConsultPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectorFechaInicio = createCustomDateChooser();
        selectorFechaFin = createCustomDateChooser();
        JButton btnConsultar = new JButton("Consultar");
        panelFiltros.add(new JLabel("Desde:"));
        panelFiltros.add(selectorFechaInicio);
        panelFiltros.add(new JLabel("Hasta:"));
        panelFiltros.add(selectorFechaFin);
        panelFiltros.add(btnConsultar);
        String[] columnas = {"ID", "Fecha", "Descripción", "Monto"};
        modeloTablaPrincipal = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaGastos = new JTable(modeloTablaPrincipal);
        // Ocultamos la columna ID visualmente pero mantenemos el dato
        tablaGastos.getColumnModel().getColumn(0).setMinWidth(0);
        tablaGastos.getColumnModel().getColumn(0).setMaxWidth(0);
        
        JScrollPane scrollTabla = new JScrollPane(tablaGastos);
        JPanel panelInferior = new JPanel(new BorderLayout());
        lblTotalGastos = new JLabel("Total de Gastos: $0");
        lblTotalGastos.setFont(new Font("Segoe UI", Font.BOLD, 16));
        JPanel panelBotonesAccion = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnEditar = new JButton("Editar Gasto Seleccionado");
        JButton btnEliminar = new JButton("Eliminar Gasto");
        panelBotonesAccion.add(btnEditar);
        panelBotonesAccion.add(btnEliminar);
        panelInferior.add(lblTotalGastos, BorderLayout.WEST);
        panelInferior.add(panelBotonesAccion, BorderLayout.EAST);
        panel.add(panelFiltros, BorderLayout.NORTH);
        panel.add(scrollTabla, BorderLayout.CENTER);
        panel.add(panelInferior, BorderLayout.SOUTH);
        btnConsultar.addActionListener(e -> filtrarGastosPorRango());
        btnEditar.addActionListener(e -> editarGasto());
        btnEliminar.addActionListener(e -> eliminarGasto());
        return panel;
    }

    private JDateChooser createCustomDateChooser() {
        JDateChooser dateChooser = new JDateChooser();
        dateChooser.setDateFormatString("dd/MM/yyyy");
        ((JTextFieldDateEditor) dateChooser.getDateEditor()).setEditable(false);
        dateChooser.setPreferredSize(new Dimension(120, dateChooser.getPreferredSize().height));
        return dateChooser;
    }

    private void cargarGastosIniciales() {
        // Carga fresca desde la base de datos
        listaDeGastos = GastoStorage.cargarGastos();
        
        LocalDate hoy = LocalDate.now();
        LocalDate primerDiaDelMes = hoy.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate ultimoDiaDelMes = hoy.with(TemporalAdjusters.lastDayOfMonth());
        selectorFechaInicio.setDate(Date.from(primerDiaDelMes.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        selectorFechaFin.setDate(Date.from(ultimoDiaDelMes.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        
        filtrarGastosPorRango();
        actualizarTablaRecientes();
    }
    
    private void actualizarTablaRecientes() {
        modeloTablaRecientes.setRowCount(0);
        // Mostramos los primeros 5 de la lista (que ya viene ordenada por fecha DESC desde la BD)
        int limite = Math.min(listaDeGastos.size(), 5);
        for (int i = 0; i < limite; i++) {
            Gasto gasto = listaDeGastos.get(i);
            modeloTablaRecientes.addRow(new Object[]{gasto.getFecha().toString(), gasto.getDescripcion(), formatoMoneda.format(gasto.getMonto())});
        }
    }

    private void filtrarGastosPorRango() {
        Date fechaInicioDate = selectorFechaInicio.getDate();
        Date fechaFinDate = selectorFechaFin.getDate();
        if (fechaInicioDate == null || fechaFinDate == null) return;
        
        LocalDate fechaInicio = fechaInicioDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate fechaFin = fechaFinDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        
        modeloTablaPrincipal.setRowCount(0);
        double total = 0;
        
        for (Gasto gasto : listaDeGastos) {
            LocalDate fechaGasto = gasto.getFecha();
            // Filtro de fecha inclusivo
            if ((fechaGasto.isEqual(fechaInicio) || fechaGasto.isAfter(fechaInicio)) && 
                (fechaGasto.isEqual(fechaFin) || fechaGasto.isBefore(fechaFin))) {
                
                modeloTablaPrincipal.addRow(new Object[]{
                    gasto.getId(), 
                    gasto.getFecha().toString(), 
                    gasto.getDescripcion(), 
                    formatoMoneda.format(gasto.getMonto())
                });
                total += gasto.getMonto();
            }
        }
        lblTotalGastos.setText("Total de Gastos (en rango): " + formatoMoneda.format(total));
    }

    private void agregarGasto() {
        if (txtDescripcion.getText().trim().isEmpty() || txtMonto.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "La descripción y el monto son obligatorios.", "Datos Incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            LocalDate fecha = selectorFecha.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            String descripcion = txtDescripcion.getText().trim();
            double monto = Double.parseDouble(txtMonto.getText().trim());
            String metodo = (String) comboMetodoPago.getSelectedItem();
            
            // Creamos objeto temporal (el ID lo pondrá la BD)
            Gasto nuevoGasto = new Gasto(fecha, descripcion, monto, metodo);
            
            // Guardamos en BD
            boolean exito = GastoStorage.agregarGasto(nuevoGasto);
            
            if (exito) {
                // Manejo de dinero
                if ("Efectivo".equals(metodo)) {
                    CuentasStorage.restarDeCaja(monto);
                } else {
                    CuentasStorage.restarDeBanco(monto);
                }
                
                txtDescripcion.setText("");
                txtMonto.setText("");
                selectorFecha.setDate(new Date());
                
    
                cargarGastosIniciales(); 
                
                JOptionPane.showMessageDialog(this, "Gasto agregado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El monto debe ser un número válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editarGasto() {
        int filaSeleccionada = tablaGastos.getSelectedRow();
        if (filaSeleccionada < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un gasto de la tabla para editar.", "Ninguna Selección", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String idGasto = (String) modeloTablaPrincipal.getValueAt(filaSeleccionada, 0);
        Gasto gastoAEditar = listaDeGastos.stream().filter(g -> g.getId().equals(idGasto)).findFirst().orElse(null);

        if (gastoAEditar != null) {
            JPanel panelEdicion = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5,5,5,5);
            
            JDateChooser editorFecha = createCustomDateChooser();
            editorFecha.setDate(Date.from(gastoAEditar.getFecha().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            JTextField editorDescripcion = new JTextField(gastoAEditar.getDescripcion(), 20);
            JTextField editorMonto = new JTextField(String.valueOf(gastoAEditar.getMonto()), 10);
            
            gbc.gridx=0; gbc.gridy=0; panelEdicion.add(new JLabel("Fecha:"), gbc);
            gbc.gridx=1; panelEdicion.add(editorFecha, gbc);
            gbc.gridy++; gbc.gridx=0; panelEdicion.add(new JLabel("Descripción:"), gbc);
            gbc.gridx=1; panelEdicion.add(editorDescripcion, gbc);
            gbc.gridy++; gbc.gridx=0; panelEdicion.add(new JLabel("Monto:"), gbc);
            gbc.gridx=1; panelEdicion.add(editorMonto, gbc);
            
            int result = JOptionPane.showConfirmDialog(this, panelEdicion, "Editar Gasto", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                try {
                    gastoAEditar.setFecha(editorFecha.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                    gastoAEditar.setDescripcion(editorDescripcion.getText().trim());
                    gastoAEditar.setMonto(Double.parseDouble(editorMonto.getText().trim()));
                    
                    // Actualización directa en BD
                    boolean exito = GastoStorage.actualizarGasto(gastoAEditar);
                    
                    if (exito) {
                        cargarGastosIniciales(); // Recargar tabla
                        JOptionPane.showMessageDialog(this, "Gasto actualizado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "El monto debe ser un número válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void eliminarGasto() {
        int filaSeleccionada = tablaGastos.getSelectedRow();
        if (filaSeleccionada < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un gasto de la tabla para eliminar.", "Ninguna Selección", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Seguridad
        JPasswordField passwordField = new JPasswordField();
        int option = JOptionPane.showConfirmDialog(this, passwordField, "Ingrese la Clave Maestra para Eliminar", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (option == JOptionPane.OK_OPTION) {
            if (!new String(passwordField.getPassword()).equals(ConfiguracionManager.getAdminPassword())) {
                JOptionPane.showMessageDialog(this, "Clave incorrecta.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String idGasto = (String) modeloTablaPrincipal.getValueAt(filaSeleccionada, 0);
            
            // Eliminación directa en BD
            boolean exito = GastoStorage.eliminarGasto(idGasto);
            
            if (exito) {
                cargarGastosIniciales(); // Recargar tabla
                JOptionPane.showMessageDialog(this, "Gasto eliminado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
}