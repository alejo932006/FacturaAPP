import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;
import java.time.LocalTime;

public class Factura {
    private final Cliente cliente;
    private Empresa empresa;
    private final List<DetalleFactura> detalles;
    private final String numeroFactura;
    // --- CAMPO NUEVO ---
    private String metodoPago; // "Efectivo", "Transferencia", "Crédito"
    private LocalDate fecha;
    private LocalTime hora;

    public Factura(Cliente cliente, Empresa empresa, String numeroFactura) {
        this.cliente = cliente;
        this.empresa = empresa;
        this.detalles = new ArrayList<>();
        this.numeroFactura = numeroFactura;
        this.metodoPago = "Efectivo"; // Valor por defecto
        this.fecha = LocalDate.now();
        this.hora = LocalTime.now();
    }

    public String getNumeroFactura() { return numeroFactura; }
    public Cliente getCliente() { return cliente; }
    public List<DetalleFactura> getDetalles() { return detalles; }
    
    // --- NUEVOS MÉTODOS GETTER Y SETTER QUE FALTABAN ---
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    
    public boolean agregarDetalle(DetalleFactura nuevoDetalle) {
        String nuevoImei = nuevoDetalle.getDetalle();
        if (nuevoImei != null && !nuevoImei.isEmpty()) {
            for (DetalleFactura df : detalles) {
                if (df.getProducto().equals(nuevoDetalle.getProducto()) && nuevoImei.equalsIgnoreCase(df.getDetalle())) {
                    return false; 
                }
            }
            detalles.add(nuevoDetalle);
            return true; 
        } else {
            for (DetalleFactura df : detalles) {
                if (df.getProducto().equals(nuevoDetalle.getProducto()) && (df.getDetalle() == null || df.getDetalle().isEmpty())) {
                    df.setCantidad(df.getCantidad() + nuevoDetalle.getCantidad());
                    return true;
                }
            }
            detalles.add(nuevoDetalle);
            return true;
        }
    }
    
    public double calcularTotal() {
        double total = 0;
        for (DetalleFactura detalle : detalles) {
            total += detalle.getSubtotal();
        }
        return total;
    }
    
    public String generarTextoFactura() {
        StringBuilder sb = new StringBuilder();
        sb.append("Razón Social: ").append(empresa.getRazonSocial()).append("\n");
        sb.append("NIT: ").append(empresa.getNit()).append("\n");
        sb.append("Teléfono: ").append(empresa.getTelefono()).append("\n\n");
        sb.append("=== FACTURA DE VENTA: ").append(getNumeroFactura()).append(" ===\n");
        sb.append("Fecha: ").append(java.time.LocalDate.now().toString()).append("\n");
        sb.append("Hora: ").append(java.time.LocalTime.now().withNano(0).toString()).append("\n\n");
        sb.append("=== CLIENTE ===\n");
        sb.append("Cliente: ").append(cliente.getNombre()).append("\n");
        sb.append("Cédula: ").append(cliente.getCedula()).append("\n");
        sb.append("Dirección: ").append(cliente.getDireccion()).append("\n\n");
        sb.append("Método de Pago: ").append(this.metodoPago).append("\n\n");
        sb.append("Detalles:\n");
        
        NumberFormat formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        formatoPesos.setMaximumFractionDigits(0);
        String etiquetaPersonalizada = ConfiguracionManager.cargarEtiquetaDetalle();

        for (DetalleFactura d : detalles) {
            Producto p = d.getProducto();
            String cantidadFormateada = String.format(Locale.US, "%.2f %s", d.getCantidad(), p.getUnidadDeMedida());

            sb.append(String.format("- [%s] %s x %s = %s",
            p.getCodigo(), p.getNombre(), cantidadFormateada, formatoPesos.format(d.getSubtotal())));
            
            sb.append(String.format(Locale.US, " #venta_u:%f;costo_u:%f\n", p.getPrecioVenta(), p.getCosto()));

            if (d.getDescuento() > 0) {
                sb.append("  Descuento: -").append(formatoPesos.format(d.getDescuento())).append("\n");
            }
            if (d.getDetalle() != null && !d.getDetalle().isEmpty()) {
                sb.append("  ").append(etiquetaPersonalizada).append(": ").append(d.getDetalle()).append("\n");
            }
        }
        sb.append("\n\nTOTAL A PAGAR: ").append(formatoPesos.format(calcularTotal()));
        String textoRes = ConfiguracionManager.getTextoResolucion();
        if (textoRes != null && !textoRes.isEmpty()) {
            sb.append("\n\n").append(textoRes); 
            // Ej: Texto de resolución / autorización (si aplica)
        }
        String politicaDeGarantia = GarantiaStorage.cargarPoliticaGarantia();
        if (politicaDeGarantia != null && !politicaDeGarantia.isEmpty()) {
            sb.append("\n\n").append(politicaDeGarantia);
        }
        return sb.toString();
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHora() {
        return hora;
    }
}
