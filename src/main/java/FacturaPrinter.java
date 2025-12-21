import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class FacturaPrinter implements Printable {

    private final List<String> lines;
    private final Font font;
    
    // Configuración para papel de 80mm y fuente tamaño 8
    // 48 caracteres es un ancho seguro para evitar cortes
    private static final int MAX_CARACTERES_POR_LINEA = 48; 

    public FacturaPrinter(String textContent) {
        this.lines = new ArrayList<>();
        
        // 1. Limpiamos tildes para evitar problemas de codificación en impresoras viejas
        String textoSinTildes = TextUtil.quitarTildes(textContent);
        
        // 2. Dividimos por los saltos de línea que ya traiga el texto
        String[] parrafosOriginales = textoSinTildes.split("\n");
        
        // 3. Procesamos cada párrafo para ajustar el ancho (Word Wrap)
        for (String parrafo : parrafosOriginales) {
            this.lines.addAll(ajustarTextoAlAncho(parrafo));
        }
        
        this.font = new Font("Monospaced", Font.PLAIN, 8);
    }

    /**
     * Método auxiliar que toma una línea larga y la divide en varias
     * líneas cortas sin cortar las palabras a la mitad.
     */
    private List<String> ajustarTextoAlAncho(String texto) {
        List<String> lineasAjustadas = new ArrayList<>();
        
        if (texto == null || texto.isEmpty()) {
            lineasAjustadas.add("");
            return lineasAjustadas;
        }

        // Si la línea ya cabe, la agregamos tal cual
        if (texto.length() <= MAX_CARACTERES_POR_LINEA) {
            lineasAjustadas.add(texto);
            return lineasAjustadas;
        }

        // Algoritmo de ajuste palabra por palabra
        String[] palabras = texto.split(" ");
        StringBuilder lineaActual = new StringBuilder();

        for (String palabra : palabras) {
            // Verificamos si al agregar la palabra nos pasamos del límite
            if (lineaActual.length() + palabra.length() + 1 > MAX_CARACTERES_POR_LINEA) {
                // Si ya tenemos texto, guardamos la línea actual e iniciamos una nueva
                if (lineaActual.length() > 0) {
                    lineasAjustadas.add(lineaActual.toString());
                    lineaActual = new StringBuilder();
                }
                
                // Si la palabra es GIGANTE (más ancha que toda la hoja), la cortamos
                if (palabra.length() > MAX_CARACTERES_POR_LINEA) {
                     lineasAjustadas.add(palabra.substring(0, MAX_CARACTERES_POR_LINEA));
                     // El resto de la palabra se pierde o se podría mandar a la sig linea
                     // (simplificado para recibos)
                } else {
                    lineaActual.append(palabra);
                }
            } else {
                // Si cabe, la agregamos con un espacio
                if (lineaActual.length() > 0) {
                    lineaActual.append(" ");
                }
                lineaActual.append(palabra);
            }
        }
        
        // Agregar lo que quedó en el buffer
        if (lineaActual.length() > 0) {
            lineasAjustadas.add(lineaActual.toString());
        }

        return lineasAjustadas;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g2d.setFont(font);

        int y = 15; // Margen superior inicial
        int alturaLinea = g2d.getFontMetrics().getHeight();

        for (String line : lines) {
            g2d.drawString(line, 10, y); // X=10 es el margen izquierdo
            y += alturaLinea;
        }

        return PAGE_EXISTS;
    }

    // Método estático para imprimir desde cualquier parte (cotizaciones, reportes, etc)
    public static void imprimirContenido(String texto) {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            
            // Configuración para papel de 80mm
            double paperWidthMM = 80.0;
            double paperWidthInches = paperWidthMM / 25.4;
            double paperWidthUnits = paperWidthInches * 72.0;

            PageFormat pageFormat = job.defaultPage();
            Paper paper = pageFormat.getPaper();
            
            double margin = 10; 
            // Altura MAX_VALUE para permitir facturas muy largas (impresión continua)
            paper.setSize(paperWidthUnits, Double.MAX_VALUE);
            paper.setImageableArea(margin, margin, paperWidthUnits - (margin * 2), Double.MAX_VALUE);
            pageFormat.setPaper(paper);

            job.setPrintable(new FacturaPrinter(texto), pageFormat);

            if (job.printDialog()) {
                job.print();
            }

        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(null, "Error de Impresión: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}