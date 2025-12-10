import java.time.LocalDate;

public class ArqueoCaja {

    public enum Estado { ABIERTA, CERRADA }

    private final LocalDate fecha;
    private double baseInicial;
    private double ventasContado;
    private double gastosEfectivo;
    private double ingresosExtra;
    private double retirosEfectivo;
    private double efectivoEsperado;
    private double efectivoFinalReal;
    private double diferencia;
    private Estado estado;
    private double ventasTransferencia; // NUEVO CAMPO

    public ArqueoCaja(LocalDate fecha, double baseInicial) {
        this.fecha = fecha;
        this.baseInicial = baseInicial;
        this.estado = Estado.ABIERTA;
        this.ventasContado = 0;
        this.ventasTransferencia = 0; // Inicializar
    }

    // Constructor para cargar desde CSV (MODIFICADO)
    public ArqueoCaja(LocalDate fecha, double baseInicial, double ventasContado, double ventasTransferencia, double gastosEfectivo, 
                      double ingresosExtra, double retirosEfectivo, double efectivoEsperado, 
                      double efectivoFinalReal, double diferencia, Estado estado) {
        this.fecha = fecha;
        this.baseInicial = baseInicial;
        this.ventasContado = ventasContado;
        // CORREGIDO: Faltaba el "this." para asignar al campo de la clase
        this.ventasTransferencia = ventasTransferencia;
        this.gastosEfectivo = gastosEfectivo;
        this.ingresosExtra = ingresosExtra;
        this.retirosEfectivo = retirosEfectivo;
        this.efectivoEsperado = efectivoEsperado;
        this.efectivoFinalReal = efectivoFinalReal;
        this.diferencia = diferencia;
        this.estado = estado;
    }
    
    public void calcularYCerrar() {
        this.efectivoEsperado = (baseInicial + ventasContado + ingresosExtra) - (gastosEfectivo + retirosEfectivo);
        this.diferencia = efectivoFinalReal - efectivoEsperado;
        this.estado = Estado.CERRADA;
    }
    
    // --- Getters y Setters ---
    public LocalDate getFecha() { return fecha; }
    public double getBaseInicial() { return baseInicial; }
    public double getVentasContado() { return ventasContado; }
    public double getGastosEfectivo() { return gastosEfectivo; }
    public double getIngresosExtra() { return ingresosExtra; }
    public double getRetirosEfectivo() { return retirosEfectivo; }
    public double getEfectivoEsperado() { return efectivoEsperado; }
    public double getEfectivoFinalReal() { return efectivoFinalReal; }
    public double getDiferencia() { return diferencia; }
    public Estado getEstado() { return estado; }
    public double getVentasTransferencia() { return ventasTransferencia; } // NUEVO

    public void setVentasContado(double ventasContado) { this.ventasContado = ventasContado; }
    public void setGastosEfectivo(double gastosEfectivo) { this.gastosEfectivo = gastosEfectivo; }
    public void setIngresosExtra(double ingresosExtra) { this.ingresosExtra = ingresosExtra; }
    public void setRetirosEfectivo(double retirosEfectivo) { this.retirosEfectivo = retirosEfectivo; }
    public void setEfectivoFinalReal(double efectivoFinalReal) { this.efectivoFinalReal = efectivoFinalReal; }
    public void setVentasTransferencia(double ventasTransferencia) { this.ventasTransferencia = ventasTransferencia; } // NUEVO
}