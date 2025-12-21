import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GestionAbonosGUI extends JDialog {

    private File archivoOrden;
    private double valorTotal = 0;
    private double totalAbonado = 0;

    private JLabel lblValorTotal, lblTotalAbonado, lblSaldoPendiente;
    private DefaultTableModel modeloTabla;
    private JTable tablaAbonos;
    // La variable 'txtNuevoAbono' ha sido eliminada porque ya no se utiliza.
    private ConsultarOrdenesGUI owner;

    public GestionAbonosGUI(ConsultarOrdenesGUI owner, File archivoOrden) {
        super(owner, "Gestión de Abonos", true);
        this.owner = owner;
        this.archivoOrden = archivoOrden;

        setSize(800, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        cargarDatosOrden();
    }

    private void initComponents() {
        JPanel panelTotales = new JPanel(new GridLayout(3, 2, 5, 5));
        panelTotales.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        Font fuenteBold = new Font("Arial", Font.BOLD, 14);

        lblValorTotal = new JLabel("$0");
        lblTotalAbonado = new JLabel("$0");
        lblSaldoPendiente = new JLabel("$0");
        lblSaldoPendiente.setForeground(Color.RED);
        
        panelTotales.add(new JLabel("Valor Total del Separado:"));
        panelTotales.add(lblValorTotal).setFont(fuenteBold);
        panelTotales.add(new JLabel("Total Abonado:"));
        panelTotales.add(lblTotalAbonado).setFont(fuenteBold);
        panelTotales.add(new JLabel("Saldo Pendiente:"));
        panelTotales.add(lblSaldoPendiente).setFont(fuenteBold);

        modeloTabla = new DefaultTableModel(new String[]{"Fecha", "Valor Abonado"}, 0);
        tablaAbonos = new JTable(modeloTabla);
        JScrollPane scroll = new JScrollPane(tablaAbonos);
        scroll.setBorder(BorderFactory.createTitledBorder("Historial de Abonos"));

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelAcciones.setBorder(BorderFactory.createTitledBorder("Acciones"));
        JButton btnRegistrar = new JButton("Registrar Nuevo Abono");
        JButton btnEditarAbono = new JButton("Editar Seleccionado");
        JButton btnEliminarAbono = new JButton("Eliminar Seleccionado");
        btnEliminarAbono.setBackground(new Color(220, 53, 69));
        btnEliminarAbono.setForeground(Color.WHITE);
        
        panelAcciones.add(btnRegistrar);
        panelAcciones.add(new JSeparator(SwingConstants.VERTICAL));
        panelAcciones.add(btnEditarAbono);
        panelAcciones.add(btnEliminarAbono);
        JButton btnImprimir = new JButton("Imprimir Historial");
        panelAcciones.add(btnImprimir);
        
        add(panelTotales, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(panelAcciones, BorderLayout.SOUTH);

        btnRegistrar.addActionListener(e -> registrarAbono());
        btnImprimir.addActionListener(e -> imprimirHistorialAbonos());
        btnEditarAbono.addActionListener(e -> editarAbonoSeleccionado());
        btnEliminarAbono.addActionListener(e -> eliminarAbonoSeleccionado());
    }

    private void cargarDatosOrden() {
        modeloTabla.setRowCount(0);
        totalAbonado = 0;
        double valorOriginal = 0;
        double totalDevuelto = 0;
    
        try (BufferedReader reader = new BufferedReader(new FileReader(archivoOrden))) {
            String linea;
            boolean seccionAbonos = false;
            while ((linea = reader.readLine()) != null) {
                if (linea.startsWith("VALOR ASOCIADO:")) {
                    String valorNumerico = linea.replaceAll("[^\\d]", "");
                    if (!valorNumerico.isEmpty()) {
                        valorOriginal = Double.parseDouble(valorNumerico);
                    }
                }
                if (linea.startsWith("TOTAL DEVUELTO:")) {
                    String valorNumerico = linea.replaceAll("[^\\d]", "");
                    if (!valorNumerico.isEmpty()) {
                        totalDevuelto = Double.parseDouble(valorNumerico);
                    }
                }
                if (linea.contains("=== HISTORIAL DE ABONOS ===")) {
                    seccionAbonos = true;
                    continue;
                }
                if (seccionAbonos && !linea.trim().isEmpty()) {
                    String[] partes = linea.split(";");
                    double valor = Double.parseDouble(partes[1].replaceAll("[^\\d]", ""));
                    totalAbonado += valor;
    
                    NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
                    formato.setMaximumFractionDigits(0);
                    modeloTabla.addRow(new Object[]{partes[0], formato.format(valor)});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.valorTotal = valorOriginal - totalDevuelto;
        actualizarTotales();
    }

    private void registrarAbono() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField txtMontoAbono = new JTextField();
        JComboBox<String> comboMetodoPago = new JComboBox<>(new String[]{"Efectivo", "Transferencia"});
        
        panel.add(new JLabel("Monto a Abonar:"));
        panel.add(txtMontoAbono);
        panel.add(new JLabel("Método de Pago:"));
        panel.add(comboMetodoPago);

        int result = JOptionPane.showConfirmDialog(this, panel, "Registrar Nuevo Abono", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                double nuevoAbono = Double.parseDouble(txtMontoAbono.getText());
                String metodoPago = (String) comboMetodoPago.getSelectedItem();

                if (nuevoAbono <= 0 || nuevoAbono > (valorTotal - totalAbonado)) {
                    JOptionPane.showMessageDialog(this, "El valor del abono es inválido o excede el saldo pendiente.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivoOrden, true))) {
                    if (totalAbonado == 0) {
                        writer.newLine();
                        writer.write("=== HISTORIAL DE ABONOS ===\n");
                    }
                    String fecha = java.time.LocalDate.now().toString();
                    NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
                    formato.setMaximumFractionDigits(0);
                    
                    writer.write(String.format("%s;%s;%s\n", fecha, formato.format(nuevoAbono), metodoPago));
                }

                JOptionPane.showMessageDialog(this, "Abono registrado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargarDatosOrden();

                if (totalAbonado >= valorTotal) {
                    int resp = JOptionPane.showConfirmDialog(this, "¡El saldo ha sido cubierto!\n¿Desea marcar esta orden como 'Completada'?", "Pago Completado", JOptionPane.YES_NO_OPTION);
                    if (resp == JOptionPane.YES_OPTION) {
                        marcarComoCompletada();
                    }
                }

                if ("Efectivo".equals(metodoPago)) {
                    CuentasStorage.agregarACaja(nuevoAbono);
                } else {
                    CuentasStorage.agregarABanco(nuevoAbono);
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Por favor, ingrese un valor numérico válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void marcarComoCompletada() {
        String nombreActual = archivoOrden.getName();
        if (nombreActual.startsWith("CANCELADA_")) return;
        File archivoCompletado = new File(PathsHelper.getOrdenesFolder(), "COMPLETADA_" + nombreActual);
        if (archivoOrden.renameTo(archivoCompletado)) {
            owner.cargarOrdenes();
            dispose();
        }
    }

    private void actualizarTotales() {
        NumberFormat formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        formatoPesos.setMaximumFractionDigits(0);
        double saldo = valorTotal - totalAbonado;
    
        lblValorTotal.setText(formatoPesos.format(valorTotal));
        lblTotalAbonado.setText(formatoPesos.format(totalAbonado));
        lblSaldoPendiente.setText(formatoPesos.format(saldo));
    
        if (saldo <= 0) {
            lblSaldoPendiente.setForeground(new Color(0, 128, 0));
        } else {
            lblSaldoPendiente.setForeground(Color.RED);
        }
    }

    private void imprimirHistorialAbonos() {
        if (totalAbonado == 0) {
            JOptionPane.showMessageDialog(this, "No hay abonos para imprimir en esta orden.", "Sin Abonos", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder textoParaImprimir = new StringBuilder();
        textoParaImprimir.append("=== HISTORIAL DE ABONOS ===\n");
        textoParaImprimir.append("Orden: ").append(archivoOrden.getName()).append("\n");
        textoParaImprimir.append("Valor Total: ").append(lblValorTotal.getText()).append("\n");
        textoParaImprimir.append("Total Abonado: ").append(lblTotalAbonado.getText()).append("\n");
        textoParaImprimir.append("Saldo Pendiente: ").append(lblSaldoPendiente.getText()).append("\n");
        textoParaImprimir.append("---------------------------\n\n");
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            String fecha = modeloTabla.getValueAt(i, 0).toString();
            String valor = modeloTabla.getValueAt(i, 1).toString();
            textoParaImprimir.append("Fecha: ").append(fecha).append("  -  Valor: ").append(valor).append("\n");
        }
        textoParaImprimir.append("\n---------------------------\n");
        textoParaImprimir.append("Firma Cliente: _________________\n");
        FacturaPrinter.imprimirContenido(textoParaImprimir.toString());
    }

    private void eliminarAbonoSeleccionado() {
        int filaSeleccionada = tablaAbonos.getSelectedRow();
        if (filaSeleccionada < 0) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un abono de la tabla para eliminar.", "Ningún abono seleccionado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int respuesta = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de que desea eliminar este abono? Esta acción es irreversible.",
            "Confirmar Eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (respuesta == JOptionPane.YES_OPTION) {
            try {
                List<String> todasLasLineas = Files.readAllLines(archivoOrden.toPath());
                List<String> lineasNuevas = new ArrayList<>();
                boolean enSeccionAbonos = false;
                int indiceAbono = 0;
                for (String linea : todasLasLineas) {
                    if (linea.contains("=== HISTORIAL DE ABONOS ===")) {
                        enSeccionAbonos = true;
                        lineasNuevas.add(linea);
                        continue;
                    }
                    if (enSeccionAbonos && !linea.trim().isEmpty()) {
                        if (indiceAbono != filaSeleccionada) {
                            lineasNuevas.add(linea);
                        }
                        indiceAbono++;
                    } else {
                        lineasNuevas.add(linea);
                    }
                }
                if (lineasNuevas.stream().noneMatch(l -> l.contains(";$"))) {
                    lineasNuevas.removeIf(l -> l.contains("=== HISTORIAL DE ABONOS ==="));
                }
                Files.write(archivoOrden.toPath(), lineasNuevas);
                cargarDatosOrden();
                JOptionPane.showMessageDialog(this, "Abono eliminado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al modificar el archivo de la orden.", "Error de Archivo", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void editarAbonoSeleccionado() {
        int filaSeleccionada = tablaAbonos.getSelectedRow();
        if (filaSeleccionada < 0) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un abono de la tabla para editar.", "Ningún abono seleccionado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String valorActualStr = modeloTabla.getValueAt(filaSeleccionada, 1).toString().replaceAll("[^\\d]", "");
        String nuevoValorStr = JOptionPane.showInputDialog(this, "Ingrese el nuevo valor para este abono:", valorActualStr);
        if (nuevoValorStr == null || nuevoValorStr.trim().isEmpty()) {
            return;
        }
        try {
            double nuevoValor = Double.parseDouble(nuevoValorStr);
            if (nuevoValor <= 0) {
                JOptionPane.showMessageDialog(this, "El valor del abono debe ser un número positivo.", "Valor Inválido", JOptionPane.ERROR_MESSAGE);
                return;
            }
            List<String> todasLasLineas = Files.readAllLines(archivoOrden.toPath());
            List<String> lineasNuevas = new ArrayList<>();
            boolean enSeccionAbonos = false;
            int indiceAbono = 0;
            
            for (String linea : todasLasLineas) {
                if (linea.contains("=== HISTORIAL DE ABONOS ===")) {
                    enSeccionAbonos = true;
                    lineasNuevas.add(linea);
                    continue;
                }

                if (enSeccionAbonos && !linea.trim().isEmpty()) {
                    if (indiceAbono == filaSeleccionada) {
                        String[] partes = linea.split(";");
                        String fecha = partes[0];
                        String metodo = (partes.length == 3) ? partes[2] : "Efectivo"; // Conserva el método de pago
                        NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
                        formato.setMaximumFractionDigits(0);
                        lineasNuevas.add(String.format("%s;%s;%s", fecha, formato.format(nuevoValor), metodo));
                    } else {
                        lineasNuevas.add(linea);
                    }
                    indiceAbono++;
                } else {
                    lineasNuevas.add(linea);
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivoOrden, false))) {
                for (int i = 0; i < lineasNuevas.size(); i++) {
                    writer.write(lineasNuevas.get(i));
                    if (i < lineasNuevas.size() - 1) {
                        writer.newLine();
                    }
                }
            }

            cargarDatosOrden();
            JOptionPane.showMessageDialog(this, "Abono actualizado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese un valor numérico válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al modificar el archivo de la orden.", "Error de Archivo", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}