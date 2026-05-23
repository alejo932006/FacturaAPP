import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abre WhatsApp Desktop/Web con el chat del cliente y deja el PDF listo en el portapapeles
 * para pegarlo con Ctrl+V en el chat.
 */
public class WhatsAppService {

    private static final Pattern PATRON_TELEFONO = Pattern.compile(
            "(?i)(?:tel[ééfono]*|celular|móvil|movil|whatsapp|wa)\\s*[:.]?\\s*([+\\d\\s\\-()]{7,20})");

    public static void enviarFacturaPorWhatsApp(Component parent, Factura factura) {
        File pdf;
        try {
            pdf = FacturaPdfGenerator.generarPdf(factura);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent,
                    "No se pudo generar el PDF de la factura:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String telefono = extraerTelefonoCliente(factura.getCliente());
        if (telefono == null || telefono.isEmpty()) {
            telefono = JOptionPane.showInputDialog(parent,
                    "Ingrese el número de WhatsApp del cliente.\n" +
                    "Incluya el indicativo del país (ej: 573001234567):",
                    "Teléfono WhatsApp", JOptionPane.QUESTION_MESSAGE);
            if (telefono == null || telefono.trim().isEmpty()) {
                return;
            }
        }
        telefono = normalizarTelefono(telefono);
        if (telefono.length() < 10) {
            JOptionPane.showMessageDialog(parent,
                    "El número ingresado no parece válido. Use el formato: 573001234567",
                    "Teléfono inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        copiarArchivoAlPortapapeles(pdf);

        String mensaje = construirMensaje(factura);
        if (!abrirWhatsApp(telefono, mensaje)) {
            JOptionPane.showMessageDialog(parent,
                    "No se pudo abrir WhatsApp automáticamente.\n" +
                    "El PDF está en:\n" + pdf.getAbsolutePath(),
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            abrirArchivo(pdf);
            return;
        }

        JOptionPane.showMessageDialog(parent,
                "WhatsApp se abrió con el chat del cliente.\n\n" +
                "El PDF de la factura ya está copiado.\n" +
                "Haga clic en el chat y presione Ctrl+V para adjuntar el archivo.\n\n" +
                "Archivo: " + pdf.getName(),
                "Enviar por WhatsApp", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String construirMensaje(Factura factura) {
        return "Hola " + factura.getCliente().getNombre() + ", le enviamos su factura No. "
                + factura.getNumeroFactura() + " de " + factura.getEmpresa().getRazonSocial() + ".";
    }

    private static boolean abrirWhatsApp(String telefono, String mensaje) {
        try {
            String textoCodificado = URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
            URI uriDesktop = URI.create("whatsapp://send?phone=" + telefono + "&text=" + textoCodificado);

            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(uriDesktop);
                    return true;
                }
            }
        } catch (Exception ignored) {
            // Intentar wa.me como respaldo
        }

        try {
            String textoCodificado = URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
            URI uriWeb = URI.create("https://wa.me/" + telefono + "?text=" + textoCodificado);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uriWeb);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private static void abrirArchivo(File archivo) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(archivo);
            }
        } catch (IOException ignored) {
        }
    }

    private static void copiarArchivoAlPortapapeles(File archivo) {
        Transferable transferable = new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{DataFlavor.javaFileListFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.javaFileListFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) {
                    throw new UnsupportedFlavorException(flavor);
                }
                return Collections.singletonList(archivo);
            }
        };
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
    }

    static String extraerTelefonoCliente(Cliente cliente) {
        String direccion = cliente.getDireccion();
        if (direccion == null || direccion.isBlank()) {
            return null;
        }
        Matcher m = PATRON_TELEFONO.matcher(direccion);
        if (m.find()) {
            return normalizarTelefono(m.group(1));
        }
        String soloDigitos = direccion.replaceAll("[^0-9]", "");
        if (soloDigitos.length() >= 10 && soloDigitos.length() <= 13) {
            return normalizarTelefono(soloDigitos);
        }
        return null;
    }

    static String normalizarTelefono(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("57") && digits.length() >= 12) {
            return digits;
        }
        if (digits.length() == 10 && digits.startsWith("3")) {
            return "57" + digits;
        }
        if (digits.length() >= 10) {
            return digits;
        }
        return digits;
    }
}
