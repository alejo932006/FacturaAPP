import javax.swing.*;
import java.awt.*;

/**
 * Diálogo para elegir cómo entregar la factura tras registrarla: imprimir o WhatsApp.
 */
public class OpcionesEntregaFacturaDialog extends JDialog {

    public enum OpcionEntrega {
        IMPRIMIR,
        WHATSAPP,
        NINGUNA
    }

    private OpcionEntrega opcionSeleccionada = OpcionEntrega.NINGUNA;

    public OpcionesEntregaFacturaDialog(Frame owner) {
        super(owner, "Factura registrada", true);
        initUI();
        setSize(420, 220);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel lblMensaje = new JLabel(
                "<html><center>La factura se guardó correctamente.<br>¿Cómo desea entregarla al cliente?</center></html>");
        lblMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblMensaje.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblMensaje, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));

        JButton btnImprimir = crearBoton("Imprimir", new Color(52, 58, 64), e -> {
            opcionSeleccionada = OpcionEntrega.IMPRIMIR;
            dispose();
        });

        JButton btnWhatsApp = crearBoton("WhatsApp", new Color(37, 211, 102), e -> {
            opcionSeleccionada = OpcionEntrega.WHATSAPP;
            dispose();
        });

        JButton btnOmitir = crearBoton("Omitir", new Color(108, 117, 125), e -> {
            opcionSeleccionada = OpcionEntrega.NINGUNA;
            dispose();
        });

        panelBotones.add(btnImprimir);
        panelBotones.add(btnWhatsApp);
        panelBotones.add(btnOmitir);
        panel.add(panelBotones, BorderLayout.SOUTH);

        setContentPane(panel);
        getRootPane().setDefaultButton(btnImprimir);
    }

    private JButton crearBoton(String texto, Color fondo, java.awt.event.ActionListener action) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(fondo);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(110, 38));
        btn.addActionListener(action);
        return btn;
    }

    public OpcionEntrega getOpcionSeleccionada() {
        return opcionSeleccionada;
    }

    public static OpcionEntrega mostrar(Frame owner) {
        OpcionesEntregaFacturaDialog dialog = new OpcionesEntregaFacturaDialog(owner);
        dialog.setVisible(true);
        return dialog.getOpcionSeleccionada();
    }
}
