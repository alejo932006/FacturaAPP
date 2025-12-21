import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.Locale;

public class ProcesarPagoGUI extends JDialog {

    private double totalFactura;
    private boolean pagoConfirmado = false;
    private double saldoAFavorAplicado = 0.0;
    private double saldoAFavorDisponible = 0.0;

    private JLabel lblTotalPagar, lblDevuelta;
    private JTextField txtEfectivoRecibido;
    private JButton btnConfirmar;
    private final NumberFormat formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));

    // AÑADIDO: Declaramos las variables como campos de la clase para que sean accesibles en todos los métodos.
    private JRadioButton radioEfectivo, radioTransferencia;
    private String metodoPagoSeleccionado = "Efectivo";

    public ProcesarPagoGUI(Frame owner, double totalFactura, String cedulaCliente) {
        super(owner, "Procesar Pago", true);
        this.totalFactura = totalFactura;
        formatoPesos.setMaximumFractionDigits(0);
        this.saldoAFavorDisponible = GeneradorReportesGUI.calcularSaldoAFavorCliente(cedulaCliente);
        setSize(450, 350); // Aumentamos un poco la altura
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        initComponents();
        if (this.saldoAFavorDisponible > 0) {
            aplicarSaldoAFavor();
        }
        calcularDevuelta();
    }

    private void initComponents() {
        JPanel panelPrincipal = new JPanel(new GridLayout(3, 2, 10, 20));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panel para los Radio Buttons
        JPanel panelMetodoPago = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelMetodoPago.setBorder(BorderFactory.createTitledBorder("Método de Pago"));
        radioEfectivo = new JRadioButton("Efectivo", true);
        radioTransferencia = new JRadioButton("Transferencia");
        ButtonGroup grupoPago = new ButtonGroup();
        grupoPago.add(radioEfectivo);
        grupoPago.add(radioTransferencia);
        panelMetodoPago.add(radioEfectivo);
        panelMetodoPago.add(radioTransferencia);

        JLabel lblTituloTotal = new JLabel("Total a Pagar:");
        lblTituloTotal.setFont(new Font("Arial", Font.BOLD, 16));
        lblTotalPagar = new JLabel(formatoPesos.format(totalFactura));
        lblTotalPagar.setFont(new Font("Arial", Font.BOLD, 16));
        lblTotalPagar.setForeground(Color.BLUE);

        JLabel lblTituloEfectivo = new JLabel("Efectivo Recibido:");
        lblTituloEfectivo.setFont(new Font("Arial", Font.PLAIN, 16));
        txtEfectivoRecibido = new JTextField();
        txtEfectivoRecibido.setFont(new Font("Arial", Font.PLAIN, 16));

        JLabel lblTituloDevuelta = new JLabel("Devuelta:");
        lblTituloDevuelta.setFont(new Font("Arial", Font.BOLD, 16));
        lblDevuelta = new JLabel(formatoPesos.format(0));
        lblDevuelta.setFont(new Font("Arial", Font.BOLD, 16));
        lblDevuelta.setForeground(Color.RED);

        panelPrincipal.add(lblTituloTotal);
        panelPrincipal.add(lblTotalPagar);
        panelPrincipal.add(lblTituloEfectivo);
        panelPrincipal.add(txtEfectivoRecibido);
        panelPrincipal.add(lblTituloDevuelta);
        panelPrincipal.add(lblDevuelta);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnConfirmar = new JButton("Confirmar y Registrar");
        JButton btnCancelar = new JButton("Cancelar");
        panelBotones.add(btnConfirmar);
        panelBotones.add(btnCancelar);

        add(panelMetodoPago, BorderLayout.NORTH);
        add(panelPrincipal, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);

        // Listeners
        radioEfectivo.addActionListener(e -> actualizarEstadoCampos(true));
        radioTransferencia.addActionListener(e -> actualizarEstadoCampos(false));
        
        txtEfectivoRecibido.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                calcularDevuelta();
            }
        });

        btnCancelar.addActionListener(e -> {
            pagoConfirmado = false;
            dispose();
        });

        btnConfirmar.addActionListener(e -> {
            pagoConfirmado = true;
            dispose();
        });
    }

    private void actualizarEstadoCampos(boolean esEfectivo) {
        txtEfectivoRecibido.setEditable(esEfectivo);
        if (esEfectivo) {
            metodoPagoSeleccionado = "Efectivo";
            txtEfectivoRecibido.setBackground(Color.WHITE);
            txtEfectivoRecibido.setText(""); // Limpiamos el campo al volver a efectivo
            calcularDevuelta();
        } else {
            metodoPagoSeleccionado = "Transferencia";
            txtEfectivoRecibido.setText(String.valueOf((long)totalFactura)); // Mostramos el total exacto
            txtEfectivoRecibido.setBackground(Color.LIGHT_GRAY);
            lblDevuelta.setText(formatoPesos.format(0));
            lblDevuelta.setForeground(new Color(0, 128, 0));
            btnConfirmar.setEnabled(true);
        }
    }
    
    private void aplicarSaldoAFavor() {
        int respuesta = JOptionPane.showConfirmDialog(
            this,
            "Este cliente tiene un saldo a favor de " + formatoPesos.format(saldoAFavorDisponible) + ".\n" +
            "¿Desea aplicarlo a esta compra?",
            "Aplicar Saldo a Favor",
            JOptionPane.YES_NO_OPTION
        );

        if (respuesta == JOptionPane.YES_OPTION) {
            if (saldoAFavorDisponible >= totalFactura) {
                // El saldo cubre toda la compra (o más)
                this.saldoAFavorAplicado = totalFactura;
                txtEfectivoRecibido.setText("0");
                txtEfectivoRecibido.setEditable(false); // No se necesita efectivo
            } else {
                // El saldo cubre solo una parte
                this.saldoAFavorAplicado = saldoAFavorDisponible;
            }
            // Actualizamos el total que queda por pagar en efectivo
            this.totalFactura -= saldoAFavorAplicado; 
            lblTotalPagar.setText(formatoPesos.format(this.totalFactura));
            JOptionPane.showMessageDialog(this, "Se aplicó un saldo de " + formatoPesos.format(saldoAFavorAplicado) + ".\n" +
                                               "Nuevo total a pagar: " + formatoPesos.format(this.totalFactura),
                                               "Saldo Aplicado", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void calcularDevuelta() {
        try {
            String texto = txtEfectivoRecibido.getText().trim();
            if (texto.isEmpty()) {
                texto = "0";
            }

            double efectivoRecibido = Double.parseDouble(texto);
            
            if (efectivoRecibido >= totalFactura) {
                double devuelta = efectivoRecibido - totalFactura;
                lblDevuelta.setText(formatoPesos.format(devuelta));
                lblDevuelta.setForeground(new Color(0, 128, 0)); // Verde
                btnConfirmar.setEnabled(true);
            } else {
                lblDevuelta.setText("Faltan " + formatoPesos.format(totalFactura - efectivoRecibido));
                lblDevuelta.setForeground(Color.RED);
                btnConfirmar.setEnabled(false);
            }
        } catch (NumberFormatException e) {
            lblDevuelta.setText("Valor inválido");
            lblDevuelta.setForeground(Color.RED);
            btnConfirmar.setEnabled(false);
        }
    }

    public boolean isPagoConfirmado() {
        return pagoConfirmado;
    }

    // --- NUEVO MÉTODO para que la FacturaGUI sepa cuánto saldo se usó ---
    public double getSaldoAFavorAplicado() {
        return saldoAFavorAplicado;
    }
    
    // NUEVO: Getter para que la FacturaGUI sepa qué se eligió
    public String getMetodoPagoSeleccionado() {
        return this.metodoPagoSeleccionado;
    }
}