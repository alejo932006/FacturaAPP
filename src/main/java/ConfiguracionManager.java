import java.io.*;
import java.util.Properties;

import javax.swing.JOptionPane;

/**
 * Maneja la configuración local de FacturaAPP (archivo config.properties).
 *
 * NOTA: En esta versión se eliminó toda la lógica de facturación electrónica / proveedores externos.
 */
public class ConfiguracionManager {

    // Numeración de facturas
    private static final String KEY_ULTIMO_NUMERO = "ultimo_numero_factura";

    // Datos de la empresa
    private static final String KEY_RAZON_SOCIAL = "empresa.razon_social";
    private static final String KEY_NIT = "empresa.nit";
    private static final String KEY_TELEFONO = "empresa.telefono";

    // Ajustes varios
    private static final String KEY_ETIQUETA_DETALLE = "custom.etiqueta_detalle";
    private static final String KEY_ADMIN_PASSWORD = "admin.password";
    private static final String KEY_GESTION_AREAS = "feature.gestion_por_areas";
    private static final String KEY_PRINTER_NAME = "printer.thermal_name";

    // Resolución / prefijo para impresión
    private static final String KEY_PREFIJO = "factura.prefijo";
    private static final String KEY_TEXTO_RESOLUCION = "factura.resolucion_texto";
    private static final String KEY_RANGO_ID = "factura.rango_id";

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

    /**
     * Genera el siguiente número de factura y lo persiste en config.properties.
     * Formato final: {prefijo}{consecutivo} (sin guiones forzados).
     */
    public static synchronized String getSiguienteNumeroFactura() {
        try {
            Properties props = cargarPropiedades();
            int ultimoNumero = Integer.parseInt(props.getProperty(KEY_ULTIMO_NUMERO, "0"));
            String prefijo = props.getProperty(KEY_PREFIJO, "FV");

            int nuevoNumero = ultimoNumero + 1;
            props.setProperty(KEY_ULTIMO_NUMERO, String.valueOf(nuevoNumero));
            guardarPropiedades(props);

            return prefijo + nuevoNumero;
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR-000";
        }
    }

    // --- Empresa ---
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

    public static Empresa cargarEmpresa() {
        try {
            Properties props = cargarPropiedades();
            String razonSocial = props.getProperty(KEY_RAZON_SOCIAL, "Nombre de tu Empresa");
            String nit = props.getProperty(KEY_NIT, "123.456.789-0");
            String telefono = props.getProperty(KEY_TELEFONO, "300-000-0000");
            return new Empresa(razonSocial, nit, telefono);
        } catch (IOException e) {
            e.printStackTrace();
            return new Empresa("Empresa (Error al cargar)", "N/A", "N/A");
        }
    }

    // --- Limpieza de datos ---
    public static void limpiarTodaLaBaseDeDatos() {
        File[] archivosAEliminar = new File[] {
            PathsHelper.getClientesCSV(),
            PathsHelper.getProductosCSV(),
            PathsHelper.getDevolucionesCSV(),
            PathsHelper.getConfigProperties(),
            PathsHelper.getLogoFile()
        };

        for (File archivo : archivosAEliminar) {
            if (archivo.exists()) {
                archivo.delete();
            }
        }

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

    // --- Etiqueta de detalle (IMEI/Serial/etc.) ---
    public static void guardarEtiquetaDetalle(String etiqueta) {
        guardarPropiedad(KEY_ETIQUETA_DETALLE, etiqueta);
    }

    public static String cargarEtiquetaDetalle() {
        return cargarPropiedad(KEY_ETIQUETA_DETALLE, "IMEI");
    }

    // --- Clave administrativa ---
    public static String getAdminPassword() {
        return cargarPropiedad(KEY_ADMIN_PASSWORD, "admin123");
    }

    public static void saveAdminPassword(String nuevaClave) {
        try {
            Properties props = cargarPropiedades();
            props.setProperty(KEY_ADMIN_PASSWORD, nuevaClave);
            guardarPropiedades(props);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al guardar la nueva clave administrativa.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- Gestión por áreas ---
    public static void guardarGestionPorAreas(boolean habilitado) {
        guardarPropiedad(KEY_GESTION_AREAS, String.valueOf(habilitado));
    }

    public static boolean isGestionPorAreasHabilitada() {
        return Boolean.parseBoolean(cargarPropiedad(KEY_GESTION_AREAS, "false"));
    }

    // --- Propiedades genéricas ---
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

    // --- Impresora térmica ---
    public static void savePrinterName(String name) {
        guardarPropiedad(KEY_PRINTER_NAME, name);
    }

    public static String getPrinterName() {
        return cargarPropiedad(KEY_PRINTER_NAME, "");
    }

    // --- Resolución / prefijo para impresión ---
    public static void guardarResolucion(String prefijo, String textoResolucion, String rangoId) {
        try {
            Properties props = cargarPropiedades();
            props.setProperty(KEY_PREFIJO, prefijo);
            props.setProperty(KEY_TEXTO_RESOLUCION, textoResolucion);
            props.setProperty(KEY_RANGO_ID, rangoId);
            guardarPropiedades(props);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getPrefijo() {
        return cargarPropiedad(KEY_PREFIJO, "FV");
    }

    public static String getTextoResolucion() {
        return cargarPropiedad(KEY_TEXTO_RESOLUCION, "Sin Resolución Configurada");
    }

    public static String getRangoId() {
        return cargarPropiedad(KEY_RANGO_ID, "");
    }

    /**
     * Útil para reiniciar la numeración cuando se cambia de resolución.
     */
    public static void setUltimoNumero(int numero) {
        guardarPropiedad(KEY_ULTIMO_NUMERO, String.valueOf(numero));
    }
}
