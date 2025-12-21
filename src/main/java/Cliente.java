public class Cliente {
    private final String nombre;
    private final String cedula;
    private final String direccion;
    private final String email; // <--- NUEVO CAMPO

    // Constructor actualizado
    public Cliente(String nombre, String cedula, String direccion, String email) {
        this.nombre = nombre;
        this.cedula = cedula;
        this.direccion = direccion;
        this.email = (email == null || email.trim().isEmpty()) ? "no-email@cliente.com" : email;
    }

    public String getNombre() { return nombre; }
    public String getCedula() { return cedula; }
    public String getDireccion() { return direccion; }
    public String getEmail() { return email; } // <--- NUEVO GETTER

    @Override
    public String toString() {
        return nombre + " (" + email + ")";
    }
}