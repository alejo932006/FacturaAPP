import java.io.*;
import java.util.Properties;

public class CuentasStorage {

    private static File getCuentasFile() {
        return new File(PathsHelper.getDatosFolder(), "cuentas.properties");
    }

    private static Properties cargarPropiedades() {
        Properties props = new Properties();
        File archivo = getCuentasFile();
        if (archivo.exists()) {
            try (FileInputStream in = new FileInputStream(archivo)) {
                props.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return props;
    }

    private static void guardarPropiedades(Properties props) {
        try (FileOutputStream out = new FileOutputStream(getCuentasFile())) {
            props.store(out, "Saldos de las cuentas financieras");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static double getSaldoCaja() {
        return Double.parseDouble(cargarPropiedades().getProperty("saldo_caja", "0.0"));
    }

    public static double getSaldoBanco() {
        return Double.parseDouble(cargarPropiedades().getProperty("saldo_banco", "0.0"));
    }

    public static synchronized void actualizarSaldos(double nuevoSaldoCaja, double nuevoSaldoBanco) {
        Properties props = cargarPropiedades();
        props.setProperty("saldo_caja", String.valueOf(nuevoSaldoCaja));
        props.setProperty("saldo_banco", String.valueOf(nuevoSaldoBanco));
        guardarPropiedades(props);
    }

    public static synchronized void agregarACaja(double monto) {
        actualizarSaldos(getSaldoCaja() + monto, getSaldoBanco());
    }

    public static synchronized void restarDeCaja(double monto) {
        actualizarSaldos(getSaldoCaja() - monto, getSaldoBanco());
    }

    public static synchronized void agregarABanco(double monto) {
        actualizarSaldos(getSaldoCaja(), getSaldoBanco() + monto);
    }

    public static synchronized void restarDeBanco(double monto) {
        actualizarSaldos(getSaldoCaja(), getSaldoBanco() - monto);
    }
}