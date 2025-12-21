// Archivo nuevo: Ajuste.java
import java.time.LocalDate;

public class Ajuste {
    private final LocalDate fecha;
    private final String tipo; // "INGRESO" o "RETIRO"
    private final String cuenta; // "CAJA" o "BANCO"
    private final double monto;
    private final String motivo;

    public Ajuste(LocalDate fecha, String tipo, String cuenta, double monto, String motivo) {
        this.fecha = fecha;
        this.tipo = tipo;
        this.cuenta = cuenta;
        this.monto = monto;
        this.motivo = motivo;
    }

    // --- Getters ---
    public LocalDate getFecha() { return fecha; }
    public String getTipo() { return tipo; }
    public String getCuenta() { return cuenta; }
    public double getMonto() { return monto; }
    public String getMotivo() { return motivo; }
}