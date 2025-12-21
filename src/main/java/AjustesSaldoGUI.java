// Archivo nuevo: AjustesSaldoGUI.java
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class AjustesSaldoGUI extends JDialog {

    private JComboBox<String> comboCuenta, comboTipo;
    private JTextField txtMonto, txtMotivo;

    public AjustesSaldoGUI(Frame owner) {
        super(owner, "Ajustes Manuales de Caja y Banco", true);
        setSize(450, 300);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        comboCuenta = new JComboBox<>(new String[]{"Caja (Efectivo)", "Banco (Transferencias)"});
        comboTipo = new JComboBox<>(new String[]{"Ingresar Dinero", "Retirar Dinero"});
        txtMonto = new JTextField();
        txtMotivo = new JTextField();

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Seleccione Cuenta:"), gbc);
        gbc.gridx = 1; panel.add(comboCuenta, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Seleccione Acción:"), gbc);
        gbc.gridx = 1; panel.add(comboTipo, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Monto:"), gbc);
        gbc.gridx = 1; panel.add(txtMonto, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Motivo/Justificación:"), gbc);
        gbc.gridx = 1; panel.add(txtMotivo, gbc);

        JButton btnConfirmar = new JButton("Confirmar Ajuste");
        gbc.gridy++; gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST;
        panel.add(btnConfirmar, gbc);

        add(panel, BorderLayout.CENTER);

        btnConfirmar.addActionListener(e -> procesarAjuste());
    }

    private void procesarAjuste() {
        try {
            double monto = Double.parseDouble(txtMonto.getText());
            String motivo = txtMotivo.getText().trim();
            if (monto <= 0 || motivo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El monto debe ser positivo y el motivo no puede estar vacío.", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String cuenta = comboCuenta.getSelectedIndex() == 0 ? "CAJA" : "BANCO";
            String tipo = comboTipo.getSelectedIndex() == 0 ? "INGRESO" : "RETIRO";

            if (tipo.equals("INGRESO")) {
                if (cuenta.equals("CAJA")) CuentasStorage.agregarACaja(monto);
                else CuentasStorage.agregarABanco(monto);
            } else { // RETIRO
                if (cuenta.equals("CAJA")) CuentasStorage.restarDeCaja(monto);
                else CuentasStorage.restarDeBanco(monto);
            }

            Ajuste nuevoAjuste = new Ajuste(LocalDate.now(), tipo, cuenta, monto, motivo);
            AjusteStorage.guardarAjuste(nuevoAjuste);

            JOptionPane.showMessageDialog(this, "Ajuste realizado con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            if (DashboardGUI.getInstance() != null) DashboardGUI.getInstance().refrescarDatos();
            dispose();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese un monto numérico válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        }
    }
}