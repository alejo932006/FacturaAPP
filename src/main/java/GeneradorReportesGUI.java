import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

// --- CAMBIO 1: La clase ahora implementa la interfaz "conector" ---
public class GeneradorReportesGUI extends JDialog implements ClienteSeleccionListener {

    private JComboBox<String> comboTipoReporte;
    private JTextArea areaDetalles;
    private JTextField txtNombreCliente, txtCedulaCliente, txtValor;
    private JLabel lblValorFormateado;
    private Empresa empresa;
    private File archivoAEditar = null;
    private String historialAbonosExistente = "";
    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));

    public GeneradorReportesGUI(Frame owner, File archivoExistente) {
        super(owner, archivoExistente == null ? "Generador de Órdenes y Reportes" : "Editar Orden o Reporte", true);
        this.empresa = ConfiguracionManager.cargarEmpresa();
        this.archivoAEditar = archivoExistente;
        formatoMoneda.setMaximumFractionDigits(0);
        setSize(500, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        initComponents();
        if (this.archivoAEditar != null) {
            cargarDatosDesdeArchivo();
        }
    }

    @Override
    public void setDatosCliente(String nombre, String cedula, String direccion, String email) {
        txtNombreCliente.setText(nombre);
        txtCedulaCliente.setText(cedula);
    }

    private void initComponents() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel panelTipoReporte = new JPanel(new BorderLayout(0, 5));
        comboTipoReporte = new JComboBox<>(new String[]{"Recibo de Abono/Separado", "Orden de Reparación", "Nota de Crédito", "Reporte General"});
        panelTipoReporte.add(new JLabel("Seleccione el Tipo de Documento:"), BorderLayout.NORTH);
        panelTipoReporte.add(comboTipoReporte, BorderLayout.CENTER);
        JPanel panelCentral = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel panelCliente = new JPanel(new GridBagLayout());
        panelCliente.setBorder(BorderFactory.createTitledBorder("Datos del Cliente"));
        txtNombreCliente = new JTextField(20);
        txtCedulaCliente = new JTextField(20);

        // --- CAMBIO 3: Se añade el botón de búsqueda ---
        JButton btnBuscarCliente = new JButton("Buscar...");
        btnBuscarCliente.setToolTipText("Buscar y cargar un cliente existente");
        btnBuscarCliente.addActionListener(e -> new ClientesGUI(this, this));

        GridBagConstraints gbcCliente = new GridBagConstraints();
        gbcCliente.insets = new Insets(5, 5, 5, 5);
        gbcCliente.anchor = GridBagConstraints.WEST;
        gbcCliente.gridx = 0; gbcCliente.gridy = 0; panelCliente.add(new JLabel("Nombre:"), gbcCliente);
        gbcCliente.gridx = 1; gbcCliente.gridy = 0; gbcCliente.weightx = 1.0; gbcCliente.fill = GridBagConstraints.HORIZONTAL; panelCliente.add(txtNombreCliente, gbcCliente);
        
        // El botón se posiciona al final de la fila del nombre
        gbcCliente.gridx = 2; gbcCliente.weightx = 0; gbcCliente.fill = GridBagConstraints.NONE; panelCliente.add(btnBuscarCliente, gbcCliente);
        
        gbcCliente.gridx = 0; gbcCliente.gridy = 1; panelCliente.add(new JLabel("Cédula/ID:"), gbcCliente);
        gbcCliente.gridx = 1; gbcCliente.gridy = 1; gbcCliente.gridwidth = 2; gbcCliente.weightx = 1.0; gbcCliente.fill = GridBagConstraints.HORIZONTAL; panelCliente.add(txtCedulaCliente, gbcCliente);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0;
        panelCentral.add(panelCliente, gbc);
        JPanel panelDetalles = new JPanel(new GridBagLayout());
        panelDetalles.setBorder(BorderFactory.createTitledBorder("Información de la Orden"));
        txtValor = new JTextField(20);
        lblValorFormateado = new JLabel(" ");
        lblValorFormateado.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblValorFormateado.setForeground(Color.GRAY);
        txtValor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { actualizarValorFormateado(); }
            public void removeUpdate(DocumentEvent e) { actualizarValorFormateado(); }
            public void changedUpdate(DocumentEvent e) { actualizarValorFormateado(); }
        });
        areaDetalles = new JTextArea(8, 20);
        areaDetalles.setLineWrap(true);
        areaDetalles.setWrapStyleWord(true);
        JScrollPane scrollDetalles = new JScrollPane(areaDetalles);
        GridBagConstraints gbcDetalles = new GridBagConstraints();
        gbcDetalles.insets = new Insets(5, 5, 5, 5);
        gbcDetalles.anchor = GridBagConstraints.WEST;
        gbcDetalles.gridx = 0; gbcDetalles.gridy = 0; gbcDetalles.gridwidth = 2; panelDetalles.add(new JLabel("Detalles / Observaciones:"), gbcDetalles);
        gbcDetalles.gridx = 0; gbcDetalles.gridy = 1; gbcDetalles.weightx = 1.0; gbcDetalles.weighty = 1.0; gbcDetalles.fill = GridBagConstraints.BOTH; panelDetalles.add(scrollDetalles, gbcDetalles);
        gbcDetalles.gridx = 0; gbcDetalles.gridy = 2; gbcDetalles.gridwidth = 1; gbcDetalles.weightx = 0; gbcDetalles.weighty = 0; gbcDetalles.fill = GridBagConstraints.NONE; panelDetalles.add(new JLabel("Valor Asociado (si aplica):"), gbcDetalles);
        gbcDetalles.gridx = 1; gbcDetalles.gridy = 2; gbcDetalles.weightx = 1.0; gbcDetalles.fill = GridBagConstraints.HORIZONTAL; panelDetalles.add(txtValor, gbcDetalles);
        gbcDetalles.gridx = 1; gbcDetalles.gridy = 3; gbcDetalles.insets = new Insets(0, 5, 5, 5); gbcDetalles.anchor = GridBagConstraints.EAST;
        panelDetalles.add(lblValorFormateado, gbcDetalles);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        panelCentral.add(panelDetalles, gbc);
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGuardar = new JButton("Guardar");
        JButton btnImprimir = new JButton("Guardar e Imprimir");
        panelBotones.add(btnGuardar);
        panelBotones.add(btnImprimir);
        panelPrincipal.add(panelTipoReporte, BorderLayout.NORTH);
        panelPrincipal.add(panelCentral, BorderLayout.CENTER);
        panelPrincipal.add(panelBotones, BorderLayout.SOUTH);
        add(panelPrincipal);
        btnGuardar.addActionListener(e -> generarDocumento(false));
        btnImprimir.addActionListener(e -> generarDocumento(true));
    }

    private void actualizarValorFormateado() {
        String texto = txtValor.getText().trim();
        if (texto.isEmpty()) { lblValorFormateado.setText(" "); return; }
        try {
            String textoNumerico = texto.replaceAll("[^\\d]", "");
            if (textoNumerico.isEmpty()) { lblValorFormateado.setText(" "); return; }
            long valor = Long.parseLong(textoNumerico);
            lblValorFormateado.setText(formatoMoneda.format(valor));
        } catch (NumberFormatException e) { lblValorFormateado.setText("Valor inválido"); }
    }
    
    private String construirTextoReporte() {
        StringBuilder sb = new StringBuilder();
        String tipoReporte = (String) comboTipoReporte.getSelectedItem();
        sb.append("=== ").append(empresa.getRazonSocial().toUpperCase()).append(" ===\n");
        sb.append("NIT: ").append(empresa.getNit()).append("\n");
        sb.append("Teléfono: ").append(empresa.getTelefono()).append("\n");
        sb.append("--------------------------------------------------\n\n");
        sb.append("   *** ").append(tipoReporte.toUpperCase()).append(" ***\n\n");
        sb.append("Fecha: ").append(java.time.LocalDate.now().toString()).append("\n");
        sb.append("Hora: ").append(java.time.LocalTime.now().withNano(0).toString()).append("\n\n");
        sb.append("--- DATOS DEL CLIENTE ---\n");
        sb.append("Nombre: ").append(txtNombreCliente.getText()).append("\n");
        sb.append("Cédula/ID: ").append(txtCedulaCliente.getText()).append("\n\n");
        sb.append("--- DETALLES ---\n");
        sb.append(areaDetalles.getText()).append("\n\n");
        String valorSinFormato = txtValor.getText().trim();
        if (!valorSinFormato.isEmpty()) {
            try {
                String valorNumerico = valorSinFormato.replaceAll("[^\\d]", "");
                if (!valorNumerico.isEmpty()) {
                    long valor = Long.parseLong(valorNumerico);
                    // La variable "tipoReporte" ya existe, así que la usamos directamente
            
                    // Lógica para usar la etiqueta correcta según el tipo de reporte
                    if ("Nota de Crédito".equals(tipoReporte)) {
                        sb.append("VALOR A FAVOR DEL CLIENTE: ").append(formatoMoneda.format(valor)).append("\n\n");
                    } else {
                        sb.append("VALOR ASOCIADO: ").append(formatoMoneda.format(valor)).append("\n\n");
                    }
                }
            } catch (NumberFormatException e) { /* Ignorar error de formato */ }
        }
        sb.append("--------------------------------------------------\n");
        sb.append("Firma Cliente: _____________________________\n\n");
        sb.append("Firma Recibe:  _____________________________\n\n");
        sb.append("       Gracias por su confianza.\n");
        if (this.historialAbonosExistente != null && !this.historialAbonosExistente.isEmpty()) {
            sb.append("\n\n").append(this.historialAbonosExistente);
        }
        return sb.toString();
    }

    private void generarDocumento(boolean imprimir) {
        String texto = construirTextoReporte();
        if (txtNombreCliente.getText().trim().isEmpty() || areaDetalles.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre del cliente y los detalles no pueden estar vacíos.", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String tipoReporte = comboTipoReporte.getSelectedItem().toString().replace(" ", "").replace("/", "").replace("ó", "o").replace("é", "e");
            String cliente = txtNombreCliente.getText().trim().replace(" ", "_");
            String timestamp = (archivoAEditar != null) ? archivoAEditar.getName().split("_")[2] : java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
            timestamp = timestamp.replace(".txt", "");
            String nuevoNombreArchivo = String.format("%s_%s_%s.txt", tipoReporte, cliente, timestamp);
            File archivoFinal = new File(PathsHelper.getOrdenesFolder(), nuevoNombreArchivo);
            if (archivoAEditar != null && !archivoAEditar.getName().equals(nuevoNombreArchivo)) {
                archivoAEditar.delete();
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivoFinal))) {
                writer.write(texto);
            }
            String mensajeExito = (archivoAEditar == null) ? "Reporte guardado exitosamente." : "Reporte actualizado exitosamente.";
            if (imprimir) {
                FacturaPrinter.imprimirContenido(texto);
            } else {
                JOptionPane.showMessageDialog(this, mensajeExito, "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
            dispose();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al guardar el archivo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void cargarDatosDesdeArchivo() { /*... tu método sin cambios ...*/ }

    /**
     * ========================================================================
     * NUEVO MÉTODO ESTÁTICO PARA GENERACIÓN AUTOMÁTICA (AQUÍ ESTÁ LA MAGIA)
     * ========================================================================
     * Este método es llamado desde FacturaGUI para crear el recibo de crédito.
     * @param factura La factura de venta que se está convirtiendo a crédito.
     */
    // En el archivo: GeneradorReportesGUI.java

    public static void generarReciboCreditoAutomatico(Factura factura) {
        if (factura == null) return;
        
        // --- MENSAJE DE VERIFICACIÓN ---
        if (factura.getNumeroFactura() == null || factura.getNumeroFactura().isEmpty()) {
            JOptionPane.showMessageDialog(null, "¡ALERTA! Se intentó generar un recibo de crédito pero el número de factura está vacío.", "Error de Datos", JOptionPane.ERROR_MESSAGE);
            return;
        }
    
        StringBuilder sb = new StringBuilder();
        NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        formato.setMaximumFractionDigits(0);
        
        Empresa empresa = ConfiguracionManager.cargarEmpresa();
        Cliente cliente = factura.getCliente();
    
        sb.append("=== ").append(empresa.getRazonSocial().toUpperCase()).append(" ===\n");
        sb.append("NIT: ").append(empresa.getNit()).append("\n");
        sb.append("Teléfono: ").append(empresa.getTelefono()).append("\n");
        sb.append("--------------------------------------------------\n\n");
        sb.append("   *** RECIBO DE ABONO/SEPARADO (CRÉDITO) ***\n\n");
        sb.append("Fecha: ").append(java.time.LocalDate.now().toString()).append("\n");
        sb.append("Hora: ").append(java.time.LocalTime.now().withNano(0).toString()).append("\n\n");
        sb.append("--- DATOS DEL CLIENTE ---\n");
        sb.append("Nombre: ").append(cliente.getNombre()).append("\n");
        sb.append("Cédula/ID: ").append(cliente.getCedula()).append("\n\n");
        
        // Esta es la línea clave que debe contener el número de factura
        sb.append("--- DETALLES (Basado en Factura ").append(factura.getNumeroFactura()).append(") ---\n");
        for (DetalleFactura detalle : factura.getDetalles()) {
            sb.append(String.format("- %s (Cant: %.2f %s)\n", 
                detalle.getProducto().getNombre(), 
                detalle.getCantidad(),
                detalle.getProducto().getUnidadDeMedida()
            ));
        }
        sb.append("\n");
        
        sb.append("VALOR ASOCIADO: ").append(formato.format(factura.calcularTotal())).append("\n\n");
        sb.append("TOTAL DEVUELTO: $0\n\n");
        sb.append("--------------------------------------------------\n");
        sb.append("       Este es un comprobante de deuda pendiente.\n");
        sb.append("       Realice los abonos correspondientes en caja.\n");
    
        String tipoReporte = "ReciboDeAbonoSeparado";
        String nombreArchivo = String.format("%s_%s.txt", tipoReporte, factura.getNumeroFactura());
        File archivoFinal = new File(PathsHelper.getOrdenesFolder(), nombreArchivo);
    
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivoFinal))) {
            writer.write(sb.toString());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error al generar el recibo de crédito automático: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static double calcularSaldoAFavorCliente(String cedulaCliente) {
        if (cedulaCliente == null || cedulaCliente.trim().isEmpty() || cedulaCliente.equals("0")) {
            return 0.0;
        }
    
        double saldo = 0.0;
        File carpetaOrdenes = PathsHelper.getOrdenesFolder();
        File[] archivos = carpetaOrdenes.listFiles();
    
        if (archivos == null) return 0.0;
    
        for (File archivo : archivos) {
            // Solo nos interesan las notas de crédito que NO han sido usadas
            if (archivo.getName().startsWith("NotadeCredito_") && !archivo.getName().startsWith("USADA_")) {
                try {
                    List<String> lineas = Files.readAllLines(archivo.toPath());
                    
                    String cedulaEnArchivo = lineas.stream()
                        .filter(l -> l.startsWith("Cédula/ID:"))
                        .findFirst().orElse("").split(":")[1].trim();
    
                    if (cedulaCliente.equals(cedulaEnArchivo)) {
                        // Buscamos la línea que contiene el valor del saldo
                        String valorStr = lineas.stream()
                            .filter(l -> l.contains("VALOR A FAVOR DEL CLIENTE:"))
                            .findFirst().orElse("");
    
                        if (!valorStr.isEmpty()) {
                            // Limpiamos el texto para quedarnos solo con los números
                            String valorNumerico = valorStr.replaceAll("[^0-9]", "");
                            if (!valorNumerico.isEmpty()) {
                                saldo += Double.parseDouble(valorNumerico);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignorar archivos que no se puedan leer
                    System.err.println("Error al procesar archivo de saldo: " + archivo.getName());
                }
            }
        }
        return saldo;
    }
}