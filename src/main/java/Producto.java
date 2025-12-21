import java.util.Objects;

public class Producto {
    private final String codigo;
    private String nombre;
    private double precioVenta;
    private double costo;
    private double cantidad;
    private String estado;
    private String unidadDeMedida;
    private String areaEncargada;
    // --- NUEVOS CAMPOS ---
    private String nombreProveedor;
    private String nitProveedor;

    // Constructor para productos nuevos (desde la GUI)
    public Producto(String codigo, String nombre, double precio, double costo, double cantidad, String unidadDeMedida) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.precioVenta = precio;
        this.costo = costo;
        this.cantidad = cantidad;        
        this.unidadDeMedida = unidadDeMedida;
        this.estado = "Activo";
        this.areaEncargada = "";
        this.nombreProveedor = ""; // Por defecto vacío
        this.nitProveedor = "";    // Por defecto vacío
    }

    // Constructor completo para leer desde archivo (Actualizado)
    public Producto(String codigo, String nombre, double precio, double costo, double cantidad, String estado, String unidadDeMedida, String areaEncargada, String nombreProveedor, String nitProveedor) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.precioVenta = precio;
        this.costo = costo;
        this.cantidad = cantidad;
        this.estado = estado;
        this.unidadDeMedida = unidadDeMedida;
        this.areaEncargada = areaEncargada;
        this.nombreProveedor = nombreProveedor;
        this.nitProveedor = nitProveedor;
    }

    // --- Getters y Setters ---
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public double getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(double precioVenta) { this.precioVenta = precioVenta; }
    public double getCosto() { return costo; }
    public void setCosto(double costo) { this.costo = costo; }
    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getUnidadDeMedida() { return unidadDeMedida; }
    public void setUnidadDeMedida(String unidadDeMedida) { this.unidadDeMedida = unidadDeMedida; }
    public String getAreaEncargada() { return areaEncargada; }
    public void setAreaEncargada(String areaEncargada) { this.areaEncargada = areaEncargada; }

    // --- NUEVOS GETTERS Y SETTERS ---
    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }
    public String getNitProveedor() { return nitProveedor; }
    public void setNitProveedor(String nitProveedor) { this.nitProveedor = nitProveedor; }

    @Override
    public String toString() { return nombre; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Producto producto = (Producto) o;
        return Objects.equals(codigo, producto.codigo);
    }

    @Override
    public int hashCode() { return Objects.hash(codigo); }
}