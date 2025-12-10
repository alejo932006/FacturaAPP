import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
public class VerFacturaGUI extends JDialog {

    // The constructor now correctly accepts a JFrame and a File object
    public VerFacturaGUI(Window owner, File archivoFactura) {
        super(owner, "Detalle de Factura: " + archivoFactura.getName(), Dialog.ModalityType.APPLICATION_MODAL);
        setSize(450, 550);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JTextArea areaTexto = new JTextArea();
        areaTexto.setEditable(false);
        areaTexto.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaTexto.setMargin(new Insets(10, 10, 10, 10));

        try {
            // Lee todo el contenido del archivo de factura y lo pone en el JTextArea
            String contenido = Files.readString(Path.of(archivoFactura.getAbsolutePath()));
            areaTexto.setText(contenido);
            areaTexto.setCaretPosition(0); // Mueve el scroll al inicio del documento
        } catch (IOException e) {
            areaTexto.setText("Error al leer el archivo de la factura:\n\n" + e.getMessage());
            areaTexto.setForeground(Color.RED);
        }

        add(new JScrollPane(areaTexto), BorderLayout.CENTER);

        // Botón para cerrar la ventana
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        panelBoton.add(btnCerrar);

        add(panelBoton, BorderLayout.SOUTH);

        // Hacemos visible la ventana al crearla
        setVisible(true);
    }
}