public class DetalleFactura {
    private final Producto producto;
    private double cantidad; // <-- CAMBIO: De int a double
    private String detalle;
    private double descuento = 0.0;

    public DetalleFactura(Producto producto, double cantidad, String detalle) { // <-- CAMBIO: Acepta double
        this.producto = producto;
        this.cantidad = cantidad;
        this.detalle = detalle;
    }

    public Producto getProducto() { return producto; }
    public double getCantidad() { return cantidad; } // <-- CAMBIO: Devuelve double
    public void setCantidad(double cantidad) { this.cantidad = cantidad; } // <-- CAMBIO: Acepta double
    public String getDetalle() { return detalle; }
    public double getDescuento() { return descuento; }
    public void setDescuento(double descuento) { this.descuento = descuento; }

    public double getSubtotal() {
        return (producto.getPrecioVenta() * cantidad) - this.descuento;
    }

    @Override
    public String toString() {
        return producto.getNombre() + " x " + cantidad + " = $" + String.format("%.2f", getSubtotal());
    }
}