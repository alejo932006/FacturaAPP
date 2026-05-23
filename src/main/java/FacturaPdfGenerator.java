import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Genera un PDF con diseño optimizado para envío por WhatsApp (pantalla móvil).
 * La impresión térmica sigue usando el flujo TXT + FacturaPrinter sin cambios.
 */
public class FacturaPdfGenerator {

    private static final Color COLOR_ENCABEZADO = new Color(33, 37, 41);
    private static final Color COLOR_ACENTO = new Color(40, 167, 69);
    private static final Color COLOR_FILA_ALT = new Color(248, 249, 250);
    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COLOR_ENCABEZADO);
    private static final Font FONT_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
    private static final Font FONT_SECCION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_ENCABEZADO);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font FONT_TABLA_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private static final Font FONT_TABLA = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private static final Font FONT_TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_ACENTO);

    public static File generarPdf(Factura factura) throws IOException {
        File carpeta = PathsHelper.getFacturasFolder();
        File archivo = new File(carpeta, "factura_" + factura.getNumeroFactura() + ".pdf");

        NumberFormat moneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        moneda.setMaximumFractionDigits(0);
        String etiquetaDetalle = ConfiguracionManager.cargarEtiquetaDetalle();

        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            PdfWriter.getInstance(document, fos);
            document.open();

            agregarEncabezado(document, factura);
            agregarDatosCliente(document, factura);
            agregarTablaProductos(document, factura, moneda, etiquetaDetalle);
            agregarTotal(document, factura, moneda);
            agregarPie(document, factura);

            document.close();
        }
        return archivo;
    }

    private static void agregarEncabezado(Document document, Factura factura) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1f, 2.5f});

        PdfPCell celdaLogo = new PdfPCell();
        celdaLogo.setBorder(Rectangle.NO_BORDER);
        celdaLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        File logoFile = PathsHelper.getLogoFile();
        if (logoFile.exists()) {
            try {
                Image logo = Image.getInstance(logoFile.getAbsolutePath());
                logo.scaleToFit(70, 70);
                celdaLogo.addElement(logo);
            } catch (Exception ignored) {
                celdaLogo.addElement(new Phrase(" "));
            }
        } else {
            celdaLogo.addElement(new Phrase(" "));
        }
        header.addCell(celdaLogo);

        Empresa empresa = factura.getEmpresa();
        Paragraph infoEmpresa = new Paragraph();
        infoEmpresa.add(new Chunk(empresa.getRazonSocial() + "\n", FONT_TITULO));
        infoEmpresa.add(new Chunk("NIT: " + empresa.getNit() + "\n", FONT_SUBTITULO));
        infoEmpresa.add(new Chunk("Tel: " + empresa.getTelefono(), FONT_SUBTITULO));

        PdfPCell celdaInfo = new PdfPCell(infoEmpresa);
        celdaInfo.setBorder(Rectangle.NO_BORDER);
        celdaInfo.setHorizontalAlignment(Element.ALIGN_RIGHT);
        header.addCell(celdaInfo);

        document.add(header);

        Paragraph tituloFactura = new Paragraph(
                "FACTURA No. " + factura.getNumeroFactura(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_ACENTO));
        tituloFactura.setAlignment(Element.ALIGN_CENTER);
        tituloFactura.setSpacingBefore(12);
        tituloFactura.setSpacingAfter(4);
        document.add(tituloFactura);

        Paragraph fechaHora = new Paragraph(
                "Fecha: " + factura.getFecha() + "  |  Hora: " + factura.getHora().withNano(0),
                FONT_SUBTITULO);
        fechaHora.setAlignment(Element.ALIGN_CENTER);
        fechaHora.setSpacingAfter(14);
        document.add(fechaHora);
    }

    private static void agregarDatosCliente(Document document, Factura factura) throws DocumentException {
        Cliente cliente = factura.getCliente();

        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100);
        box.setSpacingAfter(12);

        Paragraph contenido = new Paragraph();
        contenido.add(new Chunk("CLIENTE\n", FONT_SECCION));
        contenido.add(new Chunk(cliente.getNombre() + "\n", FONT_NORMAL));
        contenido.add(new Chunk("CC/NIT: " + cliente.getCedula() + "\n", FONT_NORMAL));
        if (cliente.getDireccion() != null && !cliente.getDireccion().isBlank()) {
            contenido.add(new Chunk("Dirección: " + cliente.getDireccion() + "\n", FONT_NORMAL));
        }
        contenido.add(new Chunk("Método de pago: " + factura.getMetodoPago(), FONT_NORMAL));

        PdfPCell celda = new PdfPCell(contenido);
        celda.setBackgroundColor(COLOR_FILA_ALT);
        celda.setPadding(10);
        celda.setBorderColor(new Color(222, 226, 230));
        box.addCell(celda);

        document.add(box);
    }

    private static void agregarTablaProductos(Document document, Factura factura, NumberFormat moneda,
            String etiquetaDetalle) throws DocumentException {
        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.2f, 3.5f, 1f, 1.5f, 1.5f});
        tabla.setSpacingAfter(8);

        String[] headers = {"Código", "Producto", "Cant.", "P. Unit.", "Subtotal"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_TABLA_HEADER));
            cell.setBackgroundColor(COLOR_ENCABEZADO);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(cell);
        }

        boolean alternar = false;
        for (DetalleFactura d : factura.getDetalles()) {
            Producto p = d.getProducto();
            Color fondo = alternar ? COLOR_FILA_ALT : Color.WHITE;
            alternar = !alternar;

            String cantidad = String.format(Locale.US, "%.2f %s", d.getCantidad(), p.getUnidadDeMedida());
            String nombre = p.getNombre();
            if (d.getDetalle() != null && !d.getDetalle().isEmpty()) {
                nombre += "\n" + etiquetaDetalle + ": " + d.getDetalle();
            }
            if (d.getDescuento() > 0) {
                nombre += "\nDesc: -" + moneda.format(d.getDescuento());
            }

            agregarCeldaTabla(tabla, p.getCodigo(), fondo, Element.ALIGN_CENTER);
            agregarCeldaTabla(tabla, nombre, fondo, Element.ALIGN_LEFT);
            agregarCeldaTabla(tabla, cantidad, fondo, Element.ALIGN_CENTER);
            agregarCeldaTabla(tabla, moneda.format(p.getPrecioVenta()), fondo, Element.ALIGN_RIGHT);
            agregarCeldaTabla(tabla, moneda.format(d.getSubtotal()), fondo, Element.ALIGN_RIGHT);
        }

        document.add(tabla);
    }

    private static void agregarCeldaTabla(PdfPTable tabla, String texto, Color fondo, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_TABLA));
        cell.setBackgroundColor(fondo);
        cell.setPadding(5);
        cell.setHorizontalAlignment(alineacion);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(cell);
    }

    private static void agregarTotal(Document document, Factura factura, NumberFormat moneda) throws DocumentException {
        PdfPTable totalBox = new PdfPTable(1);
        totalBox.setWidthPercentage(100);

        Paragraph total = new Paragraph("TOTAL A PAGAR: " + moneda.format(factura.calcularTotal()), FONT_TOTAL);
        total.setAlignment(Element.ALIGN_RIGHT);

        PdfPCell celda = new PdfPCell(total);
        celda.setBorder(Rectangle.BOX);
        celda.setBorderColor(COLOR_ACENTO);
        celda.setBorderWidth(2);
        celda.setPadding(10);
        celda.setBackgroundColor(new Color(232, 245, 233));
        totalBox.addCell(celda);

        document.add(totalBox);
    }

    private static void agregarPie(Document document, Factura factura) throws DocumentException {
        StringBuilder pie = new StringBuilder();
        String resolucion = ConfiguracionManager.getTextoResolucion();
        if (resolucion != null && !resolucion.isBlank()) {
            pie.append(resolucion).append("\n\n");
        }
        String garantia = GarantiaStorage.cargarPoliticaGarantia();
        if (garantia != null && !garantia.isBlank()) {
            pie.append(garantia);
        }

        if (pie.length() > 0) {
            Paragraph p = new Paragraph(pie.toString(), FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY));
            p.setSpacingBefore(14);
            p.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(p);
        }

        Paragraph gracias = new Paragraph(
                "¡Gracias por su compra!",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, COLOR_ACENTO));
        gracias.setAlignment(Element.ALIGN_CENTER);
        gracias.setSpacingBefore(16);
        document.add(gracias);
    }
}
