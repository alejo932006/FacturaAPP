// Archivo nuevo: DrawerManager.java
import javax.print.*;
import javax.swing.JOptionPane;
import java.awt.Component;

public class DrawerManager {

    /**
     * Comando ESC/POS estándar de Epson para abrir el monedero (pulso en pin 2).
     * Este es el comando más común para la mayoría de impresoras térmicas.
     */
    private static final byte[] OPEN_DRAWER_COMMAND = {27, 112, 0, 60, (byte) 240};

    /**
     * Intenta abrir el monedero de efectivo conectado a la impresora térmica.
     * Si no hay impresora configurada, la pide por primera vez.
     * @param parent El componente padre (la ventana) para mostrar los diálogos.
     */
    public static void openCashDrawer(Component parent) {
        String printerName = ConfiguracionManager.getPrinterName();

        // 1. Si no hay impresora guardada, la pide por primera vez.
        if (printerName.isEmpty()) {
            printerName = JOptionPane.showInputDialog(
                parent, 
                "No hay una impresora térmica configurada.\n" +
                "Por favor, ingrese el nombre EXACTO de su impresora (distingue mayúsculas y minúsculas):", 
                "Configurar Impresora de Recibos", 
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (printerName == null || printerName.trim().isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Acción cancelada. No se configuró ninguna impresora.", "Cancelado", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Guarda el nombre para futuras ocasiones
            ConfiguracionManager.savePrinterName(printerName);
        }

        // 2. Busca el servicio de impresión con ese nombre.
        PrintService printService = findPrintService(printerName);

        if (printService == null) {
            JOptionPane.showMessageDialog(parent, 
                "Error: No se encontró la impresora llamada '" + printerName + "'.\n" +
                "Revise el Panel de Control de Impresoras e ingrese el nombre exacto.\n" +
                "(Se ha borrado el nombre guardado para que lo ingrese de nuevo).", 
                "Impresora no encontrada", 
                JOptionPane.ERROR_MESSAGE
            );
            // Borra el nombre incorrecto para que lo pida de nuevo la próxima vez.
            ConfiguracionManager.savePrinterName("");
            return;
        }

        // 3. Envía los bytes crudos (el kick code) a la impresora.
        try {
            DocPrintJob job = printService.createPrintJob();
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(OPEN_DRAWER_COMMAND, flavor, null);
            job.print(doc, null);
        } catch (PrintException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Error al enviar el comando al monedero: " + e.getMessage(), "Error de Impresión", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Busca entre los servicios de impresión del sistema uno que coincida
     * con el nombre proporcionado.
     */
    private static PrintService findPrintService(String printerName) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : services) {
            if (service.getName().equalsIgnoreCase(printerName)) {
                return service;
            }
        }
        return null;
    }
}