public interface ClienteSeleccionListener {
    // Agregamos el cuarto parámetro: String email
    void setDatosCliente(String nombre, String cedula, String direccion, String email);
}