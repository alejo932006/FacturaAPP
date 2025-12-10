import java.time.LocalDate;
import java.util.UUID;

public class Abono {
    private final String id;
    private final String compromisoId;
    private final LocalDate fecha;
    private final double montoCapital;
    private final double montoInteres;
    private final String metodoPago;

    // --- CONSTRUCTOR 1: Para crear un abono NUEVO desde la GUI ---
    // (Resuelve los errores en CompromisosGUI)
    public Abono(String compromisoId, LocalDate fecha, double montoCapital, double montoInteres, String metodoPago) {
        this.id = UUID.randomUUID().toString();
        this.compromisoId = compromisoId;
        this.fecha = fecha;
        this.montoCapital = montoCapital;
        this.montoInteres = montoInteres;
        this.metodoPago = metodoPago; // <-- AÑADIR
    }

    public Abono(String id, String compromisoId, LocalDate fecha, double montoCapital, double montoInteres, String metodoPago) {
        this.id = id;
        this.compromisoId = compromisoId;
        this.fecha = fecha;
        this.montoCapital = montoCapital;
        this.montoInteres = montoInteres;
        this.metodoPago = metodoPago; // <-- AÑADIR
    }

    // --- Getters (sin cambios) ---
    public String getId() { return id; }
    public String getCompromisoId() { return compromisoId; }
    public LocalDate getFecha() { return fecha; }
    public double getMontoCapital() { return montoCapital; }
    public double getMontoInteres() { return montoInteres; }
    public String getMetodoPago() { return metodoPago; }
    public double getMontoTotal() {
        return montoCapital + montoInteres;
    }
    
}