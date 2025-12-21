public class Proveedor {
    private String nit;
    private String nombre;
    private String telefono;
    private String direccion;
    private String email;

    public Proveedor(String nit, String nombre, String telefono, String direccion, String email) {
        this.nit = nit;
        this.nombre = nombre;
        this.telefono = telefono;
        this.direccion = direccion;
        this.email = email;
    }

    // Getters y Setters
    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // Este método es clave para que el ComboBox muestre el nombre
    @Override
    public String toString() {
        return nombre;
    }
}