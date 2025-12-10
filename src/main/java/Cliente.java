public class Cliente {
    private final String nombre;
    private final String cedula;
    private final String direccion;

    public Cliente(String nombre, String cedula, String direccion) {
        this.nombre = nombre;
        this.cedula = cedula;
        this.direccion = direccion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCedula() {
        return cedula;
    }

    public String getDireccion() {
        return direccion;
    }

    // Opcional: método para mostrar al cliente en un resumen o impresión
    @Override
    public String toString() {
        return nombre + " - Cédula: " + cedula + ", Dirección: " + direccion;
    }
}
