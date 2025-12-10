import java.io.File;

public class PathsHelper {

    /**
     * Devuelve la carpeta de datos en %APPDATA%\FacturaApp
     */
    public static File getDatosFolder() {
        String appData = System.getenv("APPDATA");
        // En caso de que la variable APPDATA no exista (raro, pero posible), usamos el directorio del usuario
        if (appData == null) {
            appData = System.getProperty("user.home");
        }
        File datosDir = new File(appData, "FacturaApp");
        if (!datosDir.exists()) {
            datosDir.mkdirs();
        }
        return datosDir;
    }

    /**
     * Ruta al archivo productos.csv en la carpeta de datos
     */
    public static File getProductosCSV() {
        return new File(getDatosFolder(), "productos.csv");
    }

    /**
     * Ruta al archivo clientes.csv en la carpeta de datos
     */
    public static File getClientesCSV() {
        return new File(getDatosFolder(), "clientes.csv");
    }

    /**
     * Devuelve la carpeta donde se guardan las facturas .txt
     */
    public static File getFacturasFolder() {
        // Obtiene la carpeta de datos principal (APPDATA\FacturaApp)
        File datosFolder = getDatosFolder();
    
        // Crea una subcarpeta "facturas" dentro de la carpeta de datos
        File facturasDir = new File(datosFolder, "facturas");
    
        // Si la carpeta no existe, la crea
        if (!facturasDir.exists()) {
            facturasDir.mkdirs();
        }
    
        return facturasDir;
    }
    /**
     * Devuelve la ruta completa para un archivo de factura específico
     * en la carpeta de facturas
     */
    public static File getFacturaTXT(String nombreArchivo) {
        return new File(getFacturasFolder(), nombreArchivo);
    }
    
    /**
     * Devuelve la ruta al archivo de configuración
     */
    public static File getConfigProperties() {
        // --- LÍNEA CORREGIDA ---
        // Ahora usa el método getDatosFolder() para ser consistente con el resto de la clase.
        return new File(getDatosFolder(), "config.properties"); 
    }

    public static File getDevolucionesCSV() {
        return new File(getDatosFolder(), "devoluciones.csv");
    }

    public static File getLogoFile() {
        // El logo se guardará como logo.png dentro de la carpeta de datos.
        return new File(getDatosFolder(), "logo.png");
    }
    // En PathsHelper.java
    public static File getUsuariosCSV() {
        return new File(getDatosFolder(), "usuarios.csv");
    }
    // En PathsHelper.java
    public static File getFondoLoginFile() {
        return new File(getDatosFolder(), "login_background.jpg");
    }
    // En PathsHelper.java
    public static File getPoliticaGarantiaFile() {
        return new File(getDatosFolder(), "garantia.txt");
    }
    public static File getOrdenesFolder() {
        // Crea una subcarpeta "ordenes" dentro de la carpeta de datos
        File ordenesDir = new File(getDatosFolder(), "ordenes");
        if (!ordenesDir.exists()) {
            ordenesDir.mkdirs();
        }
        return ordenesDir;
    }
    public static File getDevolucionesFolder() {
        File devolucionesDir = new File(getDatosFolder(), "devoluciones");
        if (!devolucionesDir.exists()) {
            devolucionesDir.mkdirs();
        }
        return devolucionesDir;
    }

    public static File getArqueosCSV() {
        return new File(getDatosFolder(), "arqueos.csv");
    }
}