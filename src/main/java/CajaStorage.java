import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CajaStorage {

    public static void guardarArqueos(List<ArqueoCaja> arqueos) {
        File archivo = PathsHelper.getArqueosCSV();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo, false))) {
            for (ArqueoCaja a : arqueos) {
                // MODIFICADO: Añadimos el nuevo campo de ventas por transferencia
                String linea = String.join(";",
                    a.getFecha().toString(),
                    String.valueOf(a.getBaseInicial()),
                    String.valueOf(a.getVentasContado()),
                    String.valueOf(a.getVentasTransferencia()), // NUEVO CAMPO
                    String.valueOf(a.getGastosEfectivo()),
                    String.valueOf(a.getIngresosExtra()),
                    String.valueOf(a.getRetirosEfectivo()),
                    String.valueOf(a.getEfectivoEsperado()),
                    String.valueOf(a.getEfectivoFinalReal()),
                    String.valueOf(a.getDiferencia()),
                    a.getEstado().toString()
                );
                writer.write(linea);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<ArqueoCaja> cargarArqueos() {
        List<ArqueoCaja> lista = new ArrayList<>();
        File archivo = PathsHelper.getArqueosCSV();
        if (!archivo.exists()) return lista;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] p = linea.split(";");
                // MODIFICADO: Ahora esperamos 11 campos
                if (p.length == 11) {
                    lista.add(new ArqueoCaja(
                        LocalDate.parse(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                        Double.parseDouble(p[3]), Double.parseDouble(p[4]), Double.parseDouble(p[5]),
                        Double.parseDouble(p[6]), Double.parseDouble(p[7]), Double.parseDouble(p[8]),
                        Double.parseDouble(p[9]), ArqueoCaja.Estado.valueOf(p[10])
                    ));
                } else if (p.length == 10) { // COMPATIBILIDAD CON FORMATO ANTIGUO
                    lista.add(new ArqueoCaja(
                        LocalDate.parse(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                        0.0, // Valor por defecto para ventasTransferencia
                        Double.parseDouble(p[3]), Double.parseDouble(p[4]), Double.parseDouble(p[5]),
                        Double.parseDouble(p[6]), Double.parseDouble(p[7]), Double.parseDouble(p[8]),
                        ArqueoCaja.Estado.valueOf(p[9])
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public static Optional<ArqueoCaja> getCajaAbiertaHoy() {
        return cargarArqueos().stream()
            .filter(a -> a.getFecha().equals(LocalDate.now()) && a.getEstado() == ArqueoCaja.Estado.ABIERTA)
            .findFirst();
    }

    public static Optional<ArqueoCaja> getUltimaCajaCerrada() {
        return cargarArqueos().stream()
            .filter(a -> a.getEstado() == ArqueoCaja.Estado.CERRADA)
            .max((a1, a2) -> a1.getFecha().compareTo(a2.getFecha()));
    }
}