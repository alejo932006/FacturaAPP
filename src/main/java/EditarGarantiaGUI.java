// Archivo nuevo: EditarGarantiaGUI.java
import javax.swing.*;
import java.awt.*;

public class EditarGarantiaGUI extends JDialog {

    private JTextArea areaDeTexto;

    public EditarGarantiaGUI(Dialog owner) {
        super(owner, "Editar Políticas de Garantía", true);
        setSize(500, 400);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        // Área de texto con scroll
        areaDeTexto = new JTextArea();
        areaDeTexto.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaDeTexto.setLineWrap(true); // Las líneas largas pasarán a la siguiente
        areaDeTexto.setWrapStyleWord(true); // Evita cortar palabras
        
        // Cargar el texto actual
        areaDeTexto.setText(GarantiaStorage.cargarPoliticaGarantia());

        JScrollPane scrollPane = new JScrollPane(areaDeTexto);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Panel de botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGuardar = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");
        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);
        add(panelBotones, BorderLayout.SOUTH);

        // Lógica de los botones
        btnGuardar.addActionListener(e -> {
            GarantiaStorage.guardarPoliticaGarantia(areaDeTexto.getText());
            JOptionPane.showMessageDialog(this, "Políticas de garantía guardadas.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });
        btnCancelar.addActionListener(e -> dispose());
    }
}