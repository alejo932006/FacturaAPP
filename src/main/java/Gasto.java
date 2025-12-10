import java.time.LocalDate;
import java.util.UUID;

public class Gasto {
    private final String id;
    private LocalDate fecha;
    private String descripcion;
    private double monto;
    private String metodoPago;

    // Constructor para un gasto nuevo
    public Gasto(LocalDate fecha, String descripcion, double monto, String metodoPago) {
        this.id = UUID.randomUUID().toString();
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.monto = monto;
        this.metodoPago = metodoPago; // Nuevo
    }

    // Constructor para leer un gasto desde el archivo
    public Gasto(String id, LocalDate fecha, String descripcion, double monto, String metodoPago) {
        this.id = id;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.monto = monto;
        this.metodoPago = metodoPago; // Nuevo
    }
    

    // Getters
    public String getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public String getDescripcion() { return descripcion; }
    public double getMonto() { return monto; }
    public String getMetodoPago() { return metodoPago; }

    // Setters (para poder editar)
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setMonto(double monto) { this.monto = monto; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
}