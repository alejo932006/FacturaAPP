import java.io.*;
import java.util.Properties;

import javax.swing.JOptionPane;

public class ConfiguracionManager {

    private static final String KEY_ULTIMO_NUMERO = "ultimo_numero_factura";
    // --- NUEVAS CLAVES PARA LOS DATOS DE LA EMPRESA ---
    private static final String KEY_RAZON_SOCIAL = "empresa.razon_social";
    private static final String KEY_NIT = "empresa.nit";
    private static final String KEY_TELEFONO = "empresa.telefono";
    private static final String KEY_ETIQUETA_DETALLE = "custom.etiqueta_detalle"; // <-- NUEVA CLAVE
    private static final String KEY_ADMIN_PASSWORD = "admin.password"; // <-- AÑADE ESTA LÍNEA
    private static final String KEY_GESTION_AREAS = "feature.gestion_por_areas";
    private static final String KEY_PRINTER_NAME = "printer.thermal_name";

    private static Properties cargarPropiedades() throws IOException {
        Properties props = new Properties();
        File archivoConfig = PathsHelper.getConfigProperties();
        if (archivoConfig.exists()) {
            try (FileInputStream in = new FileInputStream(archivoConfig)) {
                props.load(in);
            }
        }
        return props;
    }

    private static void guardarPropiedades(Properties props) throws IOException {
        File archivoConfig = PathsHelper.getConfigProperties();
        if (!archivoConfig.getParentFile().exists()) {
            archivoConfig.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(archivoConfig)) {
            props.store(out, "Configuracion de la aplicacion de facturacion");
        }
    }

    public static synchronized String getSiguienteNumeroFactura() {
        try {
            Properties props = cargarPropiedades();
            int ultimoNumero = Integer.parseInt(props.getProperty(KEY_ULTIMO_NUMERO, "0"));
            int nuevoNumero = ultimoNumero + 1;
            props.setProperty(KEY_ULTIMO_NUMERO, String.valueOf(nuevoNumero));
            guardarPropiedades(props);
            return String.format("FV-%07d", nuevoNumero);
        } catch (IOException e) {
            e.printStackTrace();
            return "FV-ERROR";
        }
    }

    // --- NUEVO: Método para guardar los datos de la empresa ---
    public static void guardarEmpresa(Empresa empresa) {
        try {
            Properties props = cargarPropiedades();
            props.setProperty(KEY_RAZON_SOCIAL, empresa.getRazonSocial());
            props.setProperty(KEY_NIT, empresa.getNit());
            props.setProperty(KEY_TELEFONO, empresa.getTelefono());
            guardarPropiedades(props);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- NUEVO: Método para cargar los datos de la empresa ---
    public static Empresa cargarEmpresa() {
        try {
            Properties props = cargarPropiedades();
            // Si no hay datos guardados, se usan valores por defecto.
            String razonSocial = props.getProperty(KEY_RAZON_SOCIAL, "Nombre de tu Empresa");
            String nit = props.getProperty(KEY_NIT, "123.456.789-0");
            String telefono = props.getProperty(KEY_TELEFONO, "300-000-0000");
            return new Empresa(razonSocial, nit, telefono);
        } catch (IOException e) {
            e.printStackTrace();
            // En caso de error, devolver una empresa con datos por defecto.
            return new Empresa("Empresa (Error al cargar)", "N/A", "N/A");
        }
    }
    public static void limpiarTodaLaBaseDeDatos() {
        // Lista de archivos a eliminar
        File[] archivosAEliminar = new File[] {
            PathsHelper.getClientesCSV(),
            PathsHelper.getProductosCSV(),
            PathsHelper.getDevolucionesCSV(),
            PathsHelper.getConfigProperties(),
            PathsHelper.getLogoFile()
        };
    
        // Elimina cada archivo de la lista
        for (File archivo : archivosAEliminar) {
            if (archivo.exists()) {
                archivo.delete();
            }
        }
    
        // Vacía la carpeta de facturas
        File carpetaFacturas = PathsHelper.getFacturasFolder();
        if (carpetaFacturas.exists() && carpetaFacturas.isDirectory()) {
            File[] facturas = carpetaFacturas.listFiles();
            if (facturas != null) {
                for (File factura : facturas) {
                    factura.delete();
                }
            }
        }
    }
    // --- NUEVOS MÉTODOS PARA LA ETIQUETA ---
    public static void guardarEtiquetaDetalle(String etiqueta) {
        try {
            Properties props = cargarPropiedades();
            props.setProperty(KEY_ETIQUETA_DETALLE, etiqueta);
            guardarPropiedades(props);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String cargarEtiquetaDetalle() {
        try {
            Properties props = cargarPropiedades();
            return props.getProperty(KEY_ETIQUETA_DETALLE, "IMEI"); // Por defecto es "IMEI"
        } catch (IOException e) {
            e.printStackTrace();
            return "IMEI";
        }
    }

    public static String getAdminPassword() {
        try {
            Properties props = cargarPropiedades();
            // Por defecto, la clave será "admin123" si no se ha definido otra.
            return props.getProperty(KEY_ADMIN_PASSWORD, "admin123");
        } catch (IOException e) {
            e.printStackTrace();
            // En caso de error, devuelve la clave por defecto.
            return "admin123";
        }
    }
    // Pega este método completo en ConfiguracionManager.java

    public static void saveAdminPassword(String nuevaClave) {
        try {
            Properties props = cargarPropiedades();
            props.setProperty(KEY_ADMIN_PASSWORD, nuevaClave);
            guardarPropiedades(props);
        } catch (IOException e) {
            e.printStackTrace();
            // Opcional: Mostrar un JOptionPane en caso de error
            JOptionPane.showMessageDialog(null, "Error al guardar la nueva clave administrativa.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void guardarGestionPorAreas(boolean habilitado) {
        try {
            Properties props = cargarPropiedades();
            props.setProperty(KEY_GESTION_AREAS, String.valueOf(habilitado));
            guardarPropiedades(props);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isGestionPorAreasHabilitada() {
        try {
            Properties props = cargarPropiedades();
            // Por defecto, la función estará deshabilitada si no se encuentra la configuración
            return Boolean.parseBoolean(props.getProperty(KEY_GESTION_AREAS, "false"));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void guardarPropiedad(String clave, String valor) {
        try {
            Properties props = cargarPropiedades();
            props.setProperty(clave, valor);
            guardarPropiedades(props);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String cargarPropiedad(String clave, String valorPorDefecto) {
        try {
            Properties props = cargarPropiedades();
            return props.getProperty(clave, valorPorDefecto);
        } catch (IOException e) {
            e.printStackTrace();
            return valorPorDefecto;
        }
    }

    public static void savePrinterName(String name) {
        guardarPropiedad(KEY_PRINTER_NAME, name);
    }

    public static String getPrinterName() {
        return cargarPropiedad(KEY_PRINTER_NAME, "");
    }

}