import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Compromiso {

    public enum Tipo {
        DEUDA_TOTAL,
        PAGO_PERIODICO
    }

    public enum Estado {
        PENDIENTE,
        PAGADO
    }
    
    private Tipo tipo;
    private List<LocalDate> historialDePagos;
    private final String id;
    private String descripcion;
    private double monto;
    private LocalDate fechaVencimiento;
    private Estado estado;
    private LocalDate fechaPago;
    private String notas;
    private boolean esRecurrente;
    private int diaDeVencimiento;
    private boolean tieneIntereses;

    public Compromiso(String descripcion, double monto, LocalDate fechaVencimiento, String notas, Tipo tipo) {
        this.id = UUID.randomUUID().toString();
        this.descripcion = descripcion;
        this.monto = monto;
        this.fechaVencimiento = fechaVencimiento;
        this.notas = notas;
        this.tipo = tipo; // <-- CORRECCIÓN: Se usa "this."
        this.historialDePagos = new ArrayList<>();
        this.estado = Estado.PENDIENTE;
        this.fechaPago = null;
        this.esRecurrente = false;
        this.diaDeVencimiento = fechaVencimiento.getDayOfMonth();
        this.tieneIntereses = false;
    }

    public Compromiso(String id, String descripcion, double monto, LocalDate fechaVencimiento, Estado estado, LocalDate fechaPago, String notas, boolean esRecurrente, int diaDeVencimiento, boolean tieneIntereses, Tipo tipo, List<LocalDate> historialPagos) {
        this.id = id;
        this.descripcion = descripcion;
        this.monto = monto;
        this.fechaVencimiento = fechaVencimiento;
        this.estado = estado;
        this.fechaPago = fechaPago;
        this.notas = notas;
        this.esRecurrente = esRecurrente;
        this.diaDeVencimiento = diaDeVencimiento;
        this.tieneIntereses = tieneIntereses;
        this.tipo = tipo; // <-- CORRECCIÓN: Se usa "this."
        this.historialDePagos = historialPagos; // <-- CORRECCIÓN: El parámetro es "historialPagos"
    }

    public void pagarPeriodoActual() {
        if (this.tipo == Tipo.PAGO_PERIODICO) {
            historialDePagos.removeIf(fecha -> YearMonth.from(fecha).equals(YearMonth.now()));
            historialDePagos.add(LocalDate.now());
        }
    }

    public boolean isPagadoEsteMes() {
        if (this.tipo != Tipo.PAGO_PERIODICO) {
            return false;
        }
        YearMonth mesActual = YearMonth.now();
        return historialDePagos.stream().anyMatch(fecha -> YearMonth.from(fecha).equals(mesActual));
    }

    // --- Getters y Setters ---
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public List<LocalDate> getHistorialDePagos() { return historialDePagos; }
    public String getId() { return id; }
    public String getDescripcion() { return descripcion; }
    public double getMonto() { return monto; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public Estado getEstado() { return estado; }
    public String getNotas() { return notas; }
    public LocalDate getFechaPago() { return fechaPago; }
    public boolean esRecurrente() { return esRecurrente; }
    public int getDiaDeVencimiento() { return diaDeVencimiento; }
    public boolean tieneIntereses() { return tieneIntereses; }

    public void setDescripcion(String d) { this.descripcion = d; }
    public void setMonto(double m) { this.monto = m; }
    public void setFechaVencimiento(LocalDate f) { this.fechaVencimiento = f; }
    public void setNotas(String n) { this.notas = n; }
    public void setEsRecurrente(boolean esRecurrente) { this.esRecurrente = esRecurrente; }
    public void setDiaDeVencimiento(int dia) { this.diaDeVencimiento = dia; }
    public void setTieneIntereses(boolean tieneIntereses) { this.tieneIntereses = tieneIntereses; }
    
    public void marcarComoPagado() {
        this.estado = Estado.PAGADO;
        this.fechaPago = LocalDate.now();
    }
}