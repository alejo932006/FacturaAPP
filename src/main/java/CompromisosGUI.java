import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


public class CompromisosGUI extends JDialog {

    // --- Componentes de la Interfaz ---
    private DefaultTableModel modeloTabla;
    private JTable tablaCompromisos;
    private JTextField txtDescripcion, txtMonto;
    private JDateChooser selectorFechaVencimiento;
    private JTextArea areaNotas;
    private JButton btnGuardar;
    private JCheckBox chkTieneIntereses, chkEsRecurrente;
    private JSpinner spinnerDiaVencimiento;
    private JLabel lblMontoTotal, lblTotalAbonado, lblSaldoPendiente, lblTotalIntereses;
    private List<Compromiso> listaCompromisos;
    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    private Compromiso compromisoSeleccionado = null;
    private JComboBox<Compromiso.Tipo> comboTipoCompromiso;
    private JLabel lblMonto; // Para cambiar el texto de "Monto Total" a "Monto Cuota"
    private JButton btnMarcarPagado; // Nuevo botón
    private JPanel panelSaldos; // <-- AÑADIR
    private JButton btnRealizarAbono; // <-- AÑADIR

    public CompromisosGUI(Frame owner) {
        super(owner, "Gestión de Compromisos y Cuentas por Pagar", true);
        setSize(1100, 700); // Aumentamos el tamaño para la nueva info
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        formatoMoneda.setMaximumFractionDigits(0);
        
        initComponents();
        cargarYMostrarCompromisos();
    }

    private void initComponents() {
        // --- Panel de Tabla (Izquierda) ---
        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setBorder(BorderFactory.createTitledBorder("Lista de Compromisos"));
        
        String[] columnas = {"Descripción", "Monto Total", "Saldo Pendiente", "Vencimiento", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaCompromisos = new JTable(modeloTabla);
        tablaCompromisos.setDefaultRenderer(Object.class, new EstadoCompromisoRenderer());
        panelTabla.add(new JScrollPane(tablaCompromisos), BorderLayout.CENTER);

        // --- Panel de Formulario (Derecha) ---
        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBorder(BorderFactory.createTitledBorder("Detalle del Compromiso"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        comboTipoCompromiso = new JComboBox<>(Compromiso.Tipo.values());
        lblMonto = new JLabel("Monto Total a Pagar:"); 

        txtDescripcion = new JTextField();
        txtMonto = new JTextField();
        selectorFechaVencimiento = new JDateChooser();
        areaNotas = new JTextArea(4, 20);
        areaNotas.setLineWrap(true);
        btnGuardar = new JButton("Guardar Compromiso");


        
        // Nuevos componentes para recurrencia e intereses
        chkEsRecurrente = new JCheckBox("Pago Recurrente");
        spinnerDiaVencimiento = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
        chkTieneIntereses = new JCheckBox("Este compromiso genera intereses");

        gbc.gridx=0; gbc.gridy=0; gbc.gridwidth=2; panelFormulario.add(new JLabel("Tipo de Compromiso:"), gbc);
        gbc.gridy++; panelFormulario.add(comboTipoCompromiso, gbc);
        gbc.gridy++; panelFormulario.add(new JLabel("Descripción:"), gbc);
        gbc.gridy++; panelFormulario.add(txtDescripcion, gbc);
        gbc.gridy++; panelFormulario.add(lblMonto, gbc); // <-- USA LA VARIABLE
        gbc.gridy++; panelFormulario.add(txtMonto, gbc);
        gbc.gridy++; panelFormulario.add(new JLabel("Fecha de Vencimiento:"), gbc);
        gbc.gridy++; panelFormulario.add(selectorFechaVencimiento, gbc);
        gbc.gridy++; panelFormulario.add(new JLabel("Notas Adicionales:"), gbc);
        gbc.gridy++; panelFormulario.add(new JScrollPane(areaNotas), gbc);
        gbc.gridy++; panelFormulario.add(chkTieneIntereses, gbc);

        JPanel panelRecurrente = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelRecurrente.add(chkEsRecurrente);
        panelRecurrente.add(new JLabel("Vence el día:"));
        panelRecurrente.add(spinnerDiaVencimiento);
        gbc.gridy++; panelFormulario.add(panelRecurrente, gbc);

        // --- Panel de Resumen Financiero ---
        panelSaldos = new JPanel(new GridLayout(4, 2, 5, 5));
        panelSaldos.setBorder(BorderFactory.createTitledBorder("Resumen Financiero"));
        lblMontoTotal = new JLabel("$0");
        lblTotalAbonado = new JLabel("$0");
        lblTotalIntereses = new JLabel("$0");
        lblSaldoPendiente = new JLabel("$0");
        Font fuenteSaldos = new Font("Segoe UI", Font.BOLD, 14);
        lblMontoTotal.setFont(fuenteSaldos);
        lblTotalAbonado.setFont(fuenteSaldos);
        lblTotalIntereses.setFont(fuenteSaldos);
        lblSaldoPendiente.setFont(fuenteSaldos);
        panelSaldos.add(new JLabel("Monto Total:")); panelSaldos.add(lblMontoTotal);
        panelSaldos.add(new JLabel("Total Abonado a Capital:")); panelSaldos.add(lblTotalAbonado);
        panelSaldos.add(new JLabel("Total Intereses Pagados:")); panelSaldos.add(lblTotalIntereses);
        panelSaldos.add(new JLabel("Saldo Pendiente:")); panelSaldos.add(lblSaldoPendiente);
        gbc.gridy++; panelFormulario.add(panelSaldos, gbc);
        
        gbc.gridy++; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.NONE; panelFormulario.add(btnGuardar, gbc);

        // --- Panel de Acciones (Abajo) ---
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnNuevo = new JButton("Nuevo");
        btnRealizarAbono = new JButton("Realizar Abono");
        btnMarcarPagado = new JButton("Marcar Pagado (Este Mes)");
        JButton btnEliminar = new JButton("Eliminar");
        panelAcciones.add(btnNuevo);
        panelAcciones.add(btnRealizarAbono);
        panelAcciones.add(btnMarcarPagado);
        panelAcciones.add(btnEliminar);
        
        // --- Ensamblaje ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelTabla, new JScrollPane(panelFormulario));
        splitPane.setDividerLocation(650);
        add(splitPane, BorderLayout.CENTER);
        add(panelAcciones, BorderLayout.SOUTH);

        // --- Lógica de Eventos ---
        btnGuardar.addActionListener(e -> guardarCompromiso());
        btnNuevo.addActionListener(e -> limpiarFormulario());
        btnRealizarAbono.addActionListener(e -> realizarAbono());
        btnEliminar.addActionListener(e -> eliminarCompromiso());
        btnMarcarPagado.addActionListener(e -> marcarComoPagadoEsteMes()); // <-- AÑADIR
        comboTipoCompromiso.addActionListener(e -> actualizarVisibilidadFormulario()); // <-- AÑADIR

        tablaCompromisos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarDetalleSeleccionado();
            }
        });
        actualizarVisibilidadFormulario();
    }

    private void cargarYMostrarCompromisos() {
        listaCompromisos = CompromisoStorage.cargarCompromisos();
        listaCompromisos.sort((c1, c2) -> c1.getFechaVencimiento().compareTo(c2.getFechaVencimiento()));
        
        List<Abono> todosLosAbonos = AbonoStorage.cargarTodosLosAbonos();
        modeloTabla.setRowCount(0);
        for (Compromiso c : listaCompromisos) {
            double saldo = 0;
            String estadoStr;
        
            if (c.getTipo() == Compromiso.Tipo.DEUDA_TOTAL) {
                double totalAbonadoCapital = todosLosAbonos.stream()
                    .filter(abono -> abono.getCompromisoId().equals(c.getId()))
                    .mapToDouble(Abono::getMontoCapital)
                    .sum();
                saldo = c.getMonto() - totalAbonadoCapital;
                estadoStr = saldo <= 0 ? "PAGADO" : "PENDIENTE";
            } else { // Es PAGO_PERIODICO
                saldo = c.getMonto(); // El "saldo" es simplemente el valor de la cuota
                estadoStr = c.isPagadoEsteMes() ? "PAGADO ESTE MES" : "PENDIENTE DE PAGO";
            }
        
            modeloTabla.addRow(new Object[]{
                c.getDescripcion(),
                formatoMoneda.format(c.getMonto()), // Muestra Monto Total o Monto Cuota
                (c.getTipo() == Compromiso.Tipo.DEUDA_TOTAL) ? formatoMoneda.format(saldo) : "N/A", // Saldo solo para deudas
                c.getFechaVencimiento().toString(),
                estadoStr // El nuevo estado dinámico
            });
            }
        }


    private void limpiarFormulario() {
        compromisoSeleccionado = null;
        tablaCompromisos.clearSelection();
        txtDescripcion.setText("");
        txtMonto.setText("");
        selectorFechaVencimiento.setDate(null);
        areaNotas.setText("");
        chkTieneIntereses.setSelected(false);
        chkEsRecurrente.setSelected(false);
        spinnerDiaVencimiento.setValue(1);
        btnGuardar.setText("Guardar Compromiso");
        lblMontoTotal.setText("$0");
        lblTotalAbonado.setText("$0");
        lblTotalIntereses.setText("$0");
        lblSaldoPendiente.setText("$0");
        comboTipoCompromiso.setEnabled(true); // Siempre se habilita al crear uno nuevo.
        txtDescripcion.requestFocus();
    }

    private void mostrarDetalleSeleccionado() {
        int filaSeleccionada = tablaCompromisos.getSelectedRow();
        if (filaSeleccionada < 0) {
            limpiarFormulario();
            return;
        }

        compromisoSeleccionado = listaCompromisos.get(tablaCompromisos.convertRowIndexToModel(filaSeleccionada));

        // Primero, actualizamos el ComboBox para que muestre el tipo correcto del compromiso seleccionado.
        comboTipoCompromiso.setSelectedItem(compromisoSeleccionado.getTipo());

        // Ahora, aplicamos la lógica de bloqueo.
        if (compromisoSeleccionado.getTipo() == Compromiso.Tipo.PAGO_PERIODICO) {
            // Si es un PAGO PERIÓDICO, deshabilitamos el ComboBox para que no se pueda cambiar.
            comboTipoCompromiso.setEnabled(false);
        } else {
            // Si es una DEUDA TOTAL, lo dejamos habilitado por si se quisiera cambiar a periódico.
            comboTipoCompromiso.setEnabled(true);
        }

        
        List<Abono> todosLosAbonos = AbonoStorage.cargarTodosLosAbonos();
        List<Abono> abonosDelCompromiso = todosLosAbonos.stream()
            .filter(abono -> abono.getCompromisoId().equals(compromisoSeleccionado.getId()))
            .collect(Collectors.toList());
            
        double totalCapitalAbonado = abonosDelCompromiso.stream().mapToDouble(Abono::getMontoCapital).sum();
        double totalInteresPagado = abonosDelCompromiso.stream().mapToDouble(Abono::getMontoInteres).sum();
        double saldoPendiente = compromisoSeleccionado.getMonto() - totalCapitalAbonado;

        txtDescripcion.setText(compromisoSeleccionado.getDescripcion());
        txtMonto.setText(String.valueOf(compromisoSeleccionado.getMonto()));
        selectorFechaVencimiento.setDate(Date.from(compromisoSeleccionado.getFechaVencimiento().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        areaNotas.setText(compromisoSeleccionado.getNotas());
        chkTieneIntereses.setSelected(compromisoSeleccionado.tieneIntereses());
        chkEsRecurrente.setSelected(compromisoSeleccionado.esRecurrente());
        spinnerDiaVencimiento.setValue(compromisoSeleccionado.getDiaDeVencimiento());
        btnGuardar.setText("Actualizar Cambios");
        
        lblMontoTotal.setText(formatoMoneda.format(compromisoSeleccionado.getMonto()));
        lblTotalAbonado.setText(formatoMoneda.format(totalCapitalAbonado));
        lblTotalIntereses.setText(formatoMoneda.format(totalInteresPagado));
        lblSaldoPendiente.setText(formatoMoneda.format(saldoPendiente));
        lblSaldoPendiente.setForeground(saldoPendiente > 0 ? Color.RED : new Color(0, 128, 0));
    }

    private void guardarCompromiso() {
        try {
            String desc = txtDescripcion.getText().trim();
            double monto = Double.parseDouble(txtMonto.getText().trim());
            LocalDate fechaVenc = selectorFechaVencimiento.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            String notas = areaNotas.getText().trim();

            if (desc.isEmpty() || monto <= 0) {
                JOptionPane.showMessageDialog(this, "La descripción y el monto son obligatorios.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (compromisoSeleccionado == null) {
                Compromiso.Tipo tipo = (Compromiso.Tipo) comboTipoCompromiso.getSelectedItem(); // <-- Obtener el tipo
                Compromiso nuevo = new Compromiso(desc, monto, fechaVenc, notas, tipo); 
                nuevo.setTieneIntereses(chkTieneIntereses.isSelected());
                nuevo.setEsRecurrente(chkEsRecurrente.isSelected());
                nuevo.setDiaDeVencimiento((int)spinnerDiaVencimiento.getValue());
                CompromisoStorage.agregarCompromiso(nuevo);
                JOptionPane.showMessageDialog(this, "Compromiso guardado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                compromisoSeleccionado.setDescripcion(desc);
                compromisoSeleccionado.setMonto(monto);
                compromisoSeleccionado.setFechaVencimiento(fechaVenc);
                compromisoSeleccionado.setNotas(notas);
                compromisoSeleccionado.setTieneIntereses(chkTieneIntereses.isSelected());
                compromisoSeleccionado.setEsRecurrente(chkEsRecurrente.isSelected());
                compromisoSeleccionado.setDiaDeVencimiento((int)spinnerDiaVencimiento.getValue());
                compromisoSeleccionado.setTipo((Compromiso.Tipo) comboTipoCompromiso.getSelectedItem()); // <-- Actualizar el tipo
                CompromisoStorage.guardarTodosLosCompromisos(listaCompromisos);
                JOptionPane.showMessageDialog(this, "Compromiso actualizado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
            cargarYMostrarCompromisos();
            limpiarFormulario();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Verifique que todos los datos sean correctos.", "Error de Entrada", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void realizarAbono() {
        if (compromisoSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un compromiso de la lista.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (compromisoSeleccionado.tieneIntereses()) {
            mostrarDialogoAbonoComplejo();
        } else {
            mostrarDialogoAbonoSimple();
        }
    }

    private void mostrarDialogoAbonoSimple() {
        // Creamos un panel para poner varios componentes
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField txtMonto = new JTextField();
        JComboBox<String> comboMetodo = new JComboBox<>(new String[]{"Efectivo", "Transferencia"});
        
        panel.add(new JLabel("Monto a abonar:"));
        panel.add(txtMonto);
        panel.add(new JLabel("Pagado con:"));
        panel.add(comboMetodo);
    
        int result = JOptionPane.showConfirmDialog(this, panel, "Realizar Abono a Capital", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result != JOptionPane.OK_OPTION) return;
    
        try {
            double montoAbono = Double.parseDouble(txtMonto.getText());
            String metodo = (String) comboMetodo.getSelectedItem();
            
            if (montoAbono <= 0) {
                JOptionPane.showMessageDialog(this, "El monto debe ser positivo.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
    
            // Creamos el abono con el método de pago
            Abono nuevoAbono = new Abono(compromisoSeleccionado.getId(), LocalDate.now(), montoAbono, 0.0, metodo);
            
            // Descontamos de la cuenta correcta
            if ("Efectivo".equals(metodo)) {
                CuentasStorage.restarDeCaja(montoAbono);
            } else {
                CuentasStorage.restarDeBanco(montoAbono);
            }
    
            AbonoStorage.guardarAbono(nuevoAbono);
            JOptionPane.showMessageDialog(this, "Abono registrado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            
            // Actualizamos la vista (esto es importante)
            cargarYMostrarCompromisos();
            mostrarDetalleSeleccionado();
            if (DashboardGUI.getInstance() != null) DashboardGUI.getInstance().refrescarDatos();
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese un número válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarDialogoAbonoComplejo() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField txtPagoTotal = new JTextField(10);
        JTextField txtMontoInteres = new JTextField("0", 10);
        JTextField txtAbonoCapital = new JTextField(10);
        txtAbonoCapital.setEditable(false);
        txtAbonoCapital.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtAbonoCapital.setForeground(new Color(0, 128, 0));
        JComboBox<String> comboMetodo = new JComboBox<>(new String[]{"Efectivo", "Transferencia"});

        DocumentListener listener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { calcular(); }
            public void removeUpdate(DocumentEvent e) { calcular(); }
            public void insertUpdate(DocumentEvent e) { calcular(); }
            public void calcular() {
                try {
                    double total = Double.parseDouble(txtPagoTotal.getText().trim());
                    double interes = Double.parseDouble(txtMontoInteres.getText().trim());
                    txtAbonoCapital.setText(String.valueOf(total - interes));
                } catch (NumberFormatException ex) {
                    txtAbonoCapital.setText("0");
                }
            }
        };
        txtPagoTotal.getDocument().addDocumentListener(listener);
        txtMontoInteres.getDocument().addDocumentListener(listener);

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST; panel.add(new JLabel("Pago Total Realizado:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; panel.add(txtPagoTotal, gbc);
        gbc.gridy++; gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST; panel.add(new JLabel("Monto para Intereses:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; panel.add(txtMontoInteres, gbc);
        gbc.gridy++; gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST; panel.add(new JLabel("Abono a Capital (auto):"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; panel.add(txtAbonoCapital, gbc);
        gbc.gridy++; gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST; panel.add(new JLabel("Pagado con:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; panel.add(comboMetodo, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Registrar Abono (con Intereses)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                double capital = Double.parseDouble(txtAbonoCapital.getText());
                double interes = Double.parseDouble(txtMontoInteres.getText());
                String metodo = (String) comboMetodo.getSelectedItem();

                if (capital + interes <= 0) {
                    JOptionPane.showMessageDialog(this, "El pago total debe ser positivo.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Abono nuevoAbono = new Abono(compromisoSeleccionado.getId(), LocalDate.now(), capital, interes, metodo);
                if ("Efectivo".equals(metodo)) {
                    CuentasStorage.restarDeCaja(nuevoAbono.getMontoTotal());
                } else {
                    CuentasStorage.restarDeBanco(nuevoAbono.getMontoTotal());
                }
    
                AbonoStorage.guardarAbono(nuevoAbono);
                JOptionPane.showMessageDialog(this, "Abono registrado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                
                // Actualizamos la vista
                cargarYMostrarCompromisos();
                mostrarDetalleSeleccionado();
                if (DashboardGUI.getInstance() != null) DashboardGUI.getInstance().refrescarDatos();
    
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Valores numéricos inválidos.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void eliminarCompromiso() {
        if (compromisoSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un compromiso de la lista.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int resp = JOptionPane.showConfirmDialog(this, 
            "¿Está seguro de que desea eliminar este compromiso?", 
            "Confirmar", JOptionPane.YES_NO_OPTION);
            
        if (resp == JOptionPane.YES_OPTION) {
            // --- CORRECCIÓN AQUÍ ---
            
            // 1. (Opcional) Primero deberías eliminar los abonos asociados si no tienes borrado en cascada en la BD
            // AbonoStorage.eliminarAbonosPorCompromiso(compromisoSeleccionado.getId());
    
            // 2. Llamar al nuevo método que borra DIRECTAMENTE en la base de datos
            CompromisoStorage.eliminarCompromiso(compromisoSeleccionado.getId());
            
            // 3. Recargar la tabla desde la base de datos actualizada
            cargarYMostrarCompromisos();
            limpiarFormulario();
        }
    }

    class EstadoCompromisoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                String estado = (String) table.getModel().getValueAt(table.convertRowIndexToModel(row), 4);
                if ("PENDIENTE".equals(estado)) {
                    c.setBackground(new Color(255, 240, 240));
                    c.setForeground(Color.DARK_GRAY);
                } else {
                    c.setBackground(new Color(240, 255, 240));
                    c.setForeground(Color.BLACK);
                }
            }
            return c;
        }
    }

        /**
     * Muestra u oculta campos del formulario según el tipo de compromiso seleccionado.
     */
    private void actualizarVisibilidadFormulario() {
        Compromiso.Tipo tipoSeleccionado = (Compromiso.Tipo) comboTipoCompromiso.getSelectedItem();
        boolean esDeudaTotal = tipoSeleccionado == Compromiso.Tipo.DEUDA_TOTAL;

        lblMonto.setText(esDeudaTotal ? "Monto Total a Pagar:" : "Monto de la Cuota Mensual:");
        chkTieneIntereses.setVisible(esDeudaTotal);
        panelSaldos.setVisible(esDeudaTotal); // El panel de saldos solo tiene sentido para deudas totales
        
        // Habilita los botones correspondientes
        btnRealizarAbono.setVisible(esDeudaTotal);
        btnMarcarPagado.setVisible(!esDeudaTotal);
    }

    /**
     * Lógica para el nuevo botón "Marcar Pagado".
     */
    private void marcarComoPagadoEsteMes() {
        if (compromisoSeleccionado == null || compromisoSeleccionado.getTipo() != Compromiso.Tipo.PAGO_PERIODICO) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un compromiso de tipo 'Pago Periódico'.", "Acción no válida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (compromisoSeleccionado.isPagadoEsteMes()) {
            int resp = JOptionPane.showConfirmDialog(this, "Este pago ya fue registrado este mes. ¿Desea registrarlo de nuevo? (sobrescribirá el anterior)", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (resp != JOptionPane.YES_OPTION) return;
        }

        // Pedimos confirmación y método de pago
        JComboBox<String> comboMetodo = new JComboBox<>(new String[]{"Efectivo", "Transferencia"});
        int result = JOptionPane.showConfirmDialog(this, comboMetodo, "Confirmar pago con:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String metodo = (String) comboMetodo.getSelectedItem();
            double montoCuota = compromisoSeleccionado.getMonto();
            
            // Descontamos de la cuenta correcta
            if ("Efectivo".equals(metodo)) {
                CuentasStorage.restarDeCaja(montoCuota);
            } else {
                CuentasStorage.restarDeBanco(montoCuota);
            }

            compromisoSeleccionado.pagarPeriodoActual();
            CompromisoStorage.guardarTodosLosCompromisos(listaCompromisos);
            
            JOptionPane.showMessageDialog(this, "El pago periódico ha sido registrado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            
            // Refrescamos toda la información
            cargarYMostrarCompromisos();
            if (DashboardGUI.getInstance() != null) DashboardGUI.getInstance().refrescarDatos();
        }
    }
}
