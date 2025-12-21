import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JOptionPane;

public class FacturaPrinter implements Printable {

    private final List<String> lines;
    private final Font font;

    public FacturaPrinter(String textContent) {
        this.lines = new ArrayList<>();
        String textoSinTildes = TextUtil.quitarTildes(textContent);
        Collections.addAll(this.lines, textoSinTildes.split("\n"));
        this.font = new Font("Monospaced", Font.PLAIN, 8);
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g2d.setFont(font);

        int y = 15;
        int lineHeight = g2d.getFontMetrics().getHeight();

        for (String line : lines) {
            g2d.drawString(line, 10, y);
            y += lineHeight;
        }

        return PAGE_EXISTS;
    }

    // --- MÉTODO ESTÁTICO AÑADIDO ---
    // Este método puede ser llamado desde cualquier lugar para imprimir un texto.
    public static void imprimirContenido(String texto) {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            
            // Configuración para papel de 80mm
            double paperWidthMM = 80.0;
            double paperWidthInches = paperWidthMM / 25.4;
            double paperWidthUnits = paperWidthInches * 72.0;

            PageFormat pageFormat = job.defaultPage();
            Paper paper = pageFormat.getPaper();
            
            double margin = 10; // Margen de ~3.5mm
            paper.setSize(paperWidthUnits, Double.MAX_VALUE);
            paper.setImageableArea(margin, margin, paperWidthUnits - (margin * 2), Double.MAX_VALUE);
            pageFormat.setPaper(paper);

            // Se crea una instancia de FacturaPrinter para el trabajo de impresión
            job.setPrintable(new FacturaPrinter(texto), pageFormat);

            if (job.printDialog()) {
                job.print();
            }

        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(null, "Error de Impresión: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}