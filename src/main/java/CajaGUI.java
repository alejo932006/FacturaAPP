import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CajaGUI extends JDialog {

    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private ArqueoCaja arqueoDelDia;
    private double abonosEfectivoDelDia = 0.0;

    // Componentes del panel ABRIR
    private JLabel lblBaseSugerida;
    private JTextField txtBaseInicial;

    // Componentes del panel CERRAR
    private JLabel lblBaseInicialCierre, lblVentasContado, lblGastos, lblEfectivoEsperado, lblDiferencia;
    private JLabel lblVentasTransferencia, lblAbonosEfectivo, lblAbonosTransferencia;
    private JTextField txtIngresosExtra, txtRetiros, txtEfectivoReal;

    public CajaGUI(Frame owner) {
        super(owner, "Control de Caja Registradora", true);
        setSize(550, 700); // Un poco más alto para los nuevos campos
        setLocationRelativeTo(owner);
        formatoMoneda.setMaximumFractionDigits(0);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.add(createPanelAbrirCaja(), "ABRIR");
        cardPanel.add(createPanelCerrarCaja(), "CERRAR");
        add(cardPanel);

        Optional<ArqueoCaja> cajaAbiertaOpt = CajaStorage.getCajaAbiertaHoy();
        if (cajaAbiertaOpt.isPresent()) {
            this.arqueoDelDia = cajaAbiertaOpt.get();
            actualizarDatosPanelCierre();
            cardLayout.show(cardPanel, "CERRAR");
        } else {
            Optional<ArqueoCaja> ultimaCerrada = CajaStorage.getUltimaCajaCerrada();
            double baseSugerida = ultimaCerrada.map(ArqueoCaja::getEfectivoFinalReal).orElse(0.0);
            lblBaseSugerida.setText("Base sugerida (cierre anterior): " + formatoMoneda.format(baseSugerida));
            txtBaseInicial.setText(String.format("%.0f", baseSugerida));
            cardLayout.show(cardPanel, "ABRIR");
        }
    }

    private JPanel createPanelAbrirCaja() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitulo = new JLabel("Abrir Caja Registradora");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);

        lblBaseSugerida = new JLabel("Base sugerida: $0");
        lblBaseSugerida.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        
        JLabel lblBaseActual = new JLabel("Base Inicial Real:");
        lblBaseActual.setFont(new Font("Segoe UI", Font.BOLD, 16));

        txtBaseInicial = new JTextField(15);
        txtBaseInicial.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        
        JButton btnIniciar = new JButton("Iniciar Día y Abrir Caja");
        btnIniciar.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnIniciar.setBackground(new Color(40, 167, 69));
        btnIniciar.setForeground(Color.WHITE);
        btnIniciar.addActionListener(e -> abrirCaja());

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(lblTitulo, gbc);

        gbc.gridy = 1;
        panel.add(new JSeparator(), gbc);
        
        gbc.gridy = 2;
        panel.add(lblBaseSugerida, gbc);

        gbc.gridy = 3; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST;
        panel.add(lblBaseActual, gbc);

        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(txtBaseInicial, gbc);

        gbc.gridy = 4; gbc.gridx = 0; gbc.gridwidth = 2; gbc.insets = new Insets(20, 5, 10, 5);
        panel.add(btnIniciar, gbc);

        return panel;
    }

    private JPanel createPanelCerrarCaja() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Cierre y Arqueo de Caja", TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 18), Color.BLUE));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        DocumentListener recalculateListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { calcularCierreEnTiempoReal(); }
            @Override public void removeUpdate(DocumentEvent e) { calcularCierreEnTiempoReal(); }
            @Override public void changedUpdate(DocumentEvent e) { calcularCierreEnTiempoReal(); }
        };
        
        Font fontLabel = new Font("Segoe UI", Font.PLAIN, 16);
        Font fontValor = new Font("Segoe UI", Font.BOLD, 16);
        Font fontTotal = new Font("Segoe UI", Font.BOLD, 18);

        lblBaseInicialCierre = new JLabel("$0");
        lblBaseInicialCierre.setFont(fontValor);
        lblVentasContado = new JLabel("$0");
        lblVentasContado.setFont(fontValor);
        lblVentasTransferencia = new JLabel("$0");
        lblVentasTransferencia.setFont(fontValor);
        lblVentasTransferencia.setForeground(Color.DARK_GRAY);
        lblAbonosEfectivo = new JLabel("$0");
        lblAbonosEfectivo.setFont(fontValor);
        lblAbonosTransferencia = new JLabel("$0");
        lblAbonosTransferencia.setFont(fontValor);
        lblAbonosTransferencia.setForeground(Color.DARK_GRAY);
        lblGastos = new JLabel("$0");
        lblGastos.setFont(fontValor);
        txtIngresosExtra = new JTextField("0", 10);
        txtIngresosExtra.getDocument().addDocumentListener(recalculateListener);
        txtIngresosExtra.setFont(fontValor);
        txtRetiros = new JTextField("0", 10);
        txtRetiros.getDocument().addDocumentListener(recalculateListener);
        txtRetiros.setFont(fontValor);
        lblEfectivoEsperado = new JLabel("$0");
        lblEfectivoEsperado.setFont(fontTotal);
        lblEfectivoEsperado.setForeground(Color.BLUE);
        txtEfectivoReal = new JTextField(10);
        txtEfectivoReal.getDocument().addDocumentListener(recalculateListener);
        txtEfectivoReal.setFont(fontValor);
        lblDiferencia = new JLabel("$0");
        lblDiferencia.setFont(fontTotal);
        
        int y = 0;
        gbc.gridx=0; gbc.gridy=y; panel.add(createLabel("Base Inicial:", fontLabel), gbc);
        gbc.gridx=1; panel.add(lblBaseInicialCierre, gbc); y++;
        
        gbc.gridx=0; gbc.gridy=y; panel.add(createLabel("(+) Ventas de Contado:", fontLabel), gbc);
        gbc.gridx=1; panel.add(lblVentasContado, gbc); y++;
        
        gbc.gridx=0; gbc.gridy=y; panel.add(createLabel("(i) Ventas por Transferencia:", fontLabel), gbc);
        gbc.gridx=1; panel.add(lblVentasTransferencia, gbc); y++;
        
        gbc.gridx=0; gbc.gridy=y; panel.add(createLabel("(+) Abonos en Efectivo:", fontLabel), gbc);
        gbc.gridx=1; panel.add(lblAbonosEfectivo, gbc); y++;
        
        gbc.gridx=0; gbc.gridy=y; panel.add(createLabel("(i) Abonos por Transferencia:", fontLabel), gbc);
        gbc.gridx=1; panel.add(lblAbonosTransferencia, gbc); y++;
        
        gbc.gridx=0; gbc.gridy=y; panel.add(createLabel("(-) Gastos en Efectivo:", fontLabel), gbc);
        gbc.gridx=1; panel.add(lblGastos, gbc); y++;
        
        gbc.gridx=0; gbc.gridy=y; panel.add(createLabel("(+) Ingresos Extra (Manual):", fontLabel), gbc);
        gbc.gridx=1; panel.add(txtIngresosExtra, gbc); y++;
        
        gbc.gridx=0; gbc.gridy=y; panel.add(createLabel("(-) Retiros de Efectivo:", fontLabel), gbc);
        gbc.gridx=1; panel.add(txtRetiros, gbc); y++;
        
        gbc.gridy=y; gbc.gridwidth = 2; panel.add(new JSeparator(), gbc); y++; gbc.gridwidth = 1;
        
        gbc.gridx=0; gbc.gridy=y; panel.add(createLabel("(=) Efectivo Esperado (Sistema):", fontTotal), gbc);
        gbc.gridx=1; panel.add(lblEfectivoEsperado, gbc); y++;
        
        gbc.gridx=0; gbc.gridy=y; panel.add(createLabel("Efectivo Real Contado:", fontLabel), gbc);
        gbc.gridx=1; panel.add(txtEfectivoReal, gbc); y++;
        
        gbc.gridy=y; gbc.gridwidth = 2; panel.add(new JSeparator(), gbc); y++; gbc.gridwidth = 1;
        
        gbc.gridx=0; gbc.gridy=y; panel.add(createLabel("Diferencia (Sobrante/Faltante):", fontTotal), gbc);
        gbc.gridx=1; panel.add(lblDiferencia, gbc); y++;
        
        JButton btnCerrarCaja = new JButton("Finalizar y Cerrar Caja");
        btnCerrarCaja.setFont(fontTotal);
        btnCerrarCaja.setBackground(new Color(220, 53, 69));
        btnCerrarCaja.setForeground(Color.WHITE);
        btnCerrarCaja.addActionListener(e -> cerrarCaja());
        gbc.gridy=y; gbc.gridx=0; gbc.gridwidth=2; gbc.fill=GridBagConstraints.NONE; gbc.anchor=GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 8, 8, 8);
        panel.add(btnCerrarCaja, gbc);

        return panel;
    }


    private JLabel createLabel(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        return label;
    }

    private void actualizarDatosPanelCierre() {
        if (arqueoDelDia == null) return;
        
        Map<String, Double> ventasHoy = CalculosFinancieros.calcularVentasPorMetodo(LocalDate.now());
        double ventasContado = ventasHoy.getOrDefault("Efectivo", 0.0);
        double ventasTransferencia = ventasHoy.getOrDefault("Transferencia", 0.0);
        
        Map<String, Double> abonosHoy = CalculosFinancieros.calcularAbonosPorMetodo(LocalDate.now());
        this.abonosEfectivoDelDia = abonosHoy.getOrDefault("Efectivo", 0.0);
        double abonosTransferencia = abonosHoy.getOrDefault("Transferencia", 0.0);

        double gastosHoy = GastoStorage.cargarGastos().stream()
            .filter(g -> g.getFecha().equals(LocalDate.now()) && "Efectivo".equals(g.getMetodoPago())) // <-- Filtro añadido
            .mapToDouble(Gasto::getMonto).sum();

        lblBaseInicialCierre.setText(formatoMoneda.format(arqueoDelDia.getBaseInicial()));
        lblVentasContado.setText(formatoMoneda.format(ventasContado));
        lblVentasTransferencia.setText(formatoMoneda.format(ventasTransferencia));
        lblAbonosEfectivo.setText(formatoMoneda.format(this.abonosEfectivoDelDia));
        lblAbonosTransferencia.setText(formatoMoneda.format(abonosTransferencia));
        lblGastos.setText(formatoMoneda.format(gastosHoy));
        
        arqueoDelDia.setVentasContado(ventasContado);
        arqueoDelDia.setVentasTransferencia(ventasTransferencia);
        arqueoDelDia.setGastosEfectivo(gastosHoy);
        
        calcularCierreEnTiempoReal();
    }

    private void calcularCierreEnTiempoReal() {
        if (arqueoDelDia == null) return;
        try {
            double ingresos = txtIngresosExtra.getText().isEmpty() ? 0 : Double.parseDouble(txtIngresosExtra.getText());
            double retiros = txtRetiros.getText().isEmpty() ? 0 : Double.parseDouble(txtRetiros.getText());
            double real = txtEfectivoReal.getText().isEmpty() ? 0 : Double.parseDouble(txtEfectivoReal.getText());

            double esperado = (arqueoDelDia.getBaseInicial() + arqueoDelDia.getVentasContado() + this.abonosEfectivoDelDia + ingresos) 
                            - (arqueoDelDia.getGastosEfectivo() + retiros);
            
            double diferencia = real - esperado;

            lblEfectivoEsperado.setText(formatoMoneda.format(esperado));
            lblDiferencia.setText(formatoMoneda.format(diferencia));
            
            if(diferencia > 0) lblDiferencia.setForeground(new Color(0, 128, 0));
            else if (diferencia < 0) lblDiferencia.setForeground(Color.RED);
            else lblDiferencia.setForeground(Color.BLACK);

        } catch (NumberFormatException e) {
            lblEfectivoEsperado.setText("Valor inválido");
            lblDiferencia.setText("Valor inválido");
            lblDiferencia.setForeground(Color.RED);
        }
    }


    private void abrirCaja() {
        try {
            double base = Double.parseDouble(txtBaseInicial.getText());
            if (base < 0) {
                JOptionPane.showMessageDialog(this, "La base inicial no puede ser negativa.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            this.arqueoDelDia = new ArqueoCaja(LocalDate.now(), base);
            List<ArqueoCaja> arqueos = CajaStorage.cargarArqueos();
            arqueos.add(this.arqueoDelDia);
            CajaStorage.guardarArqueos(arqueos);
            
            JOptionPane.showMessageDialog(this, "Caja abierta con " + formatoMoneda.format(base), "Éxito", JOptionPane.INFORMATION_MESSAGE);
            
            actualizarDatosPanelCierre();
            cardLayout.show(cardPanel, "CERRAR");

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese un valor numérico válido para la base.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cerrarCaja() {
        int confirm = JOptionPane.showConfirmDialog(this, "¿Está seguro de que desea cerrar la caja?\nEsta acción es final para el día de hoy.", "Confirmar Cierre", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            double ingresos = txtIngresosExtra.getText().isEmpty() ? 0 : Double.parseDouble(txtIngresosExtra.getText());
            double retiros = txtRetiros.getText().isEmpty() ? 0 : Double.parseDouble(txtRetiros.getText());
            double real = txtEfectivoReal.getText().isEmpty() ? 0 : Double.parseDouble(txtEfectivoReal.getText());

            arqueoDelDia.setIngresosExtra(ingresos);
            arqueoDelDia.setRetirosEfectivo(retiros);
            arqueoDelDia.setEfectivoFinalReal(real);
            arqueoDelDia.calcularYCerrar();

            List<ArqueoCaja> arqueos = CajaStorage.cargarArqueos().stream()
                .filter(a -> !a.getFecha().equals(LocalDate.now())).collect(Collectors.toList());
            arqueos.add(arqueoDelDia);
            CajaStorage.guardarArqueos(arqueos);

            String mensajeFinal = String.format("<html><h2>Caja Cerrada Exitosamente</h2>" +
                "<p><b>Efectivo Final Contado:</b> %s</p>" +
                "<p><b>Diferencia:</b> %s</p><hr>" +
                "<p>Este será el monto base para el próximo día.</p></html>",
                formatoMoneda.format(arqueoDelDia.getEfectivoFinalReal()),
                formatoMoneda.format(arqueoDelDia.getDiferencia()));
            
            JOptionPane.showMessageDialog(this, mensajeFinal, "Arqueo Finalizado", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Verifique que todos los valores numéricos sean correctos.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        }
    }
    
}