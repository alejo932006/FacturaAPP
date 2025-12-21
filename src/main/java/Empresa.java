public class Empresa {
    private String razonSocial;
    private String nit;
    private String telefono;

    public Empresa(String razonSocial, String nit, String telefono) {
        this.razonSocial = razonSocial;
        this.nit = nit;
        this.telefono = telefono;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public String getNit() {
        return nit;
    }

    public String getTelefono() {
        return telefono;
    }
}
