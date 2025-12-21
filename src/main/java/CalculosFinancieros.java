import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculosFinancieros {

    public static Map<String, Double> calcularVentasPorMetodo(LocalDate fecha) {
        Map<String, Double> ventasPorMetodo = new HashMap<>();
        ventasPorMetodo.put("Efectivo", 0.0);
        ventasPorMetodo.put("Transferencia", 0.0);

        File[] archivosFactura = PathsHelper.getFacturasFolder().listFiles((dir, name) -> name.endsWith(".txt") && !name.startsWith("ANULADA_"));
        if (archivosFactura == null) return ventasPorMetodo;

        for (File archivo : archivosFactura) {
            try {
                List<String> lineas = Files.readAllLines(archivo.toPath());
                String fechaFacturaStr = lineas.stream().filter(l -> l.startsWith("Fecha:")).findFirst().orElse("").split(":")[1].trim();
                LocalDate fechaFactura = LocalDate.parse(fechaFacturaStr);

                if (fechaFactura.equals(fecha)) {
                    String numFactura = lineas.stream()
                        .filter(l -> l.contains("FACTURA DE VENTA:"))
                        .findFirst()
                        .orElse("")
                        .split(":")[1]
                        .replace("===", "")
                        .trim();

                    File reciboCredito = new File(PathsHelper.getOrdenesFolder(), "ReciboDeAbonoSeparado_" + numFactura + ".txt");
                    File reciboCompletado = new File(PathsHelper.getOrdenesFolder(), "COMPLETADA_ReciboDeAbonoSeparado_" + numFactura + ".txt");

                    if (!reciboCredito.exists() && !reciboCompletado.exists()) {
                        String totalLinea = lineas.stream().filter(l -> l.startsWith("TOTAL A PAGAR:")).findFirst().orElse("");
                        if (!totalLinea.isEmpty()) {
                            String valorNumerico = totalLinea.replaceAll("[^\\d]", "");
                            double totalVenta = Double.parseDouble(valorNumerico);
                            
                            String metodoPago = lineas.stream()
                                .filter(l -> l.startsWith("Método de Pago:"))
                                .findFirst()
                                .map(l -> l.split(":")[1].trim())
                                .orElse("Efectivo");

                            if (ventasPorMetodo.containsKey(metodoPago)) {
                                ventasPorMetodo.put(metodoPago, ventasPorMetodo.get(metodoPago) + totalVenta);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al procesar la factura " + archivo.getName() + " para el arqueo.");
                e.printStackTrace();
            }
        }
        return ventasPorMetodo;
    }

    public static Map<String, Double> calcularAbonosPorMetodo(LocalDate fecha) {
        Map<String, Double> abonosPorMetodo = new HashMap<>();
        abonosPorMetodo.put("Efectivo", 0.0);
        abonosPorMetodo.put("Transferencia", 0.0);

        File[] archivosOrdenes = PathsHelper.getOrdenesFolder().listFiles((dir, name) -> name.contains("ReciboDeAbonoSeparado") && name.endsWith(".txt"));
        if (archivosOrdenes == null) return abonosPorMetodo;

        for (File archivo : archivosOrdenes) {
            try {
                List<String> lineas = Files.readAllLines(archivo.toPath());
                boolean enSeccionAbonos = false;
                for (String linea : lineas) {
                    if (linea.contains("=== HISTORIAL DE ABONOS ===")) {
                        enSeccionAbonos = true;
                        continue;
                    }
                    if (enSeccionAbonos) {
                        String[] partes = linea.split(";");
                        if (partes.length >= 2) {
                            LocalDate fechaAbono = LocalDate.parse(partes[0]);
                            if (fechaAbono.equals(fecha)) {
                                double valor = Double.parseDouble(partes[1].replaceAll("[^\\d]", ""));
                                String metodo = (partes.length == 3) ? partes[2] : "Efectivo";
                                
                                abonosPorMetodo.put(metodo, abonosPorMetodo.getOrDefault(metodo, 0.0) + valor);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error procesando abonos del archivo: " + archivo.getName());
            }
        }
        return abonosPorMetodo;
    }
}