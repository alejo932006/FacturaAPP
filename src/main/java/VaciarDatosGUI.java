import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class VaciarDatosGUI extends JFrame {

    // Checkboxes para Base de Datos
    private JCheckBox chkDbClientes, chkDbProductos, chkDbFacturas, chkDbCaja, chkDbGastos, chkDbDevoluciones;
    
    // Checkboxes para Archivos CSV/TXT
    private JCheckBox chkCsvClientes, chkCsvProductos, chkCsvArqueos, chkCsvDevoluciones, chkTxtFacturas;

    public VaciarDatosGUI() {
        setTitle("Mantenimiento de Datos - VACIADO");
        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initComponents();
    }

    private void initComponents() {
        JPanel panelPrincipal = new JPanel(new GridLayout(1, 2, 10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- COLUMNA 1: BASE DE DATOS (POSTGRESQL) ---
        JPanel panelBD = new JPanel();
        panelBD.setLayout(new BoxLayout(panelBD, BoxLayout.Y_AXIS));
        panelBD.setBorder(BorderFactory.createTitledBorder("Base de Datos (PostgreSQL)"));

        chkDbProductos = new JCheckBox("Productos (Inventario)");
        chkDbClientes = new JCheckBox("Clientes");
        chkDbFacturas = new JCheckBox("Facturas y Detalles (Ventas)");
        chkDbCaja = new JCheckBox("Historial de Cajas (Arqueos)");
        chkDbGastos = new JCheckBox("Gastos Registrados");
        chkDbDevoluciones = new JCheckBox("Devoluciones");

        // Tooltips para explicar qué pasa
        chkDbClientes.setToolTipText("¡CUIDADO! Al borrar clientes, se borrarán sus facturas asociadas (Cascade).");
        chkDbFacturas.setToolTipText("Elimina todas las ventas y el detalle de productos vendidos.");

        panelBD.add(chkDbProductos);
        panelBD.add(chkDbClientes);
        panelBD.add(chkDbFacturas);
        panelBD.add(chkDbCaja);
        panelBD.add(chkDbGastos);
        panelBD.add(chkDbDevoluciones);

        // --- COLUMNA 2: ARCHIVOS LOCALES (CSV/TXT) ---
        JPanel panelArchivos = new JPanel();
        panelArchivos.setLayout(new BoxLayout(panelArchivos, BoxLayout.Y_AXIS));
        panelArchivos.setBorder(BorderFactory.createTitledBorder("Archivos Locales (CSV/TXT)"));

        chkCsvProductos = new JCheckBox("productos.csv");
        chkCsvClientes = new JCheckBox("clientes.csv");
        chkCsvArqueos = new JCheckBox("arqueos.csv");
        chkCsvDevoluciones = new JCheckBox("devoluciones.csv");
        chkTxtFacturas = new JCheckBox("Carpeta Facturas (.txt)");

        panelArchivos.add(chkCsvProductos);
        panelArchivos.add(chkCsvClientes);
        panelArchivos.add(chkCsvArqueos);
        panelArchivos.add(chkCsvDevoluciones);
        panelArchivos.add(chkTxtFacturas);

        panelPrincipal.add(panelBD);
        panelPrincipal.add(panelArchivos);

        // --- BOTÓN DE ACCIÓN ---
        JButton btnEjecutar = new JButton("VACIAR DATOS SELECCIONADOS");
        btnEjecutar.setBackground(Color.RED);
        btnEjecutar.setForeground(Color.WHITE);
        btnEjecutar.setFont(new Font("Arial", Font.BOLD, 14));
        btnEjecutar.addActionListener(e -> confirmarYEjecutar());

        JPanel panelSur = new JPanel();
        panelSur.add(btnEjecutar);

        add(panelPrincipal, BorderLayout.CENTER);
        add(panelSur, BorderLayout.SOUTH);
    }

    private void confirmarYEjecutar() {
        // 1. Verificar si hay algo seleccionado
        boolean algoSeleccionado = chkDbProductos.isSelected() || chkDbClientes.isSelected() || chkDbFacturas.isSelected() ||
                                   chkDbCaja.isSelected() || chkDbGastos.isSelected() || chkDbDevoluciones.isSelected() ||
                                   chkCsvProductos.isSelected() || chkCsvClientes.isSelected() || chkCsvArqueos.isSelected() ||
                                   chkCsvDevoluciones.isSelected() || chkTxtFacturas.isSelected();

        if (!algoSeleccionado) {
            JOptionPane.showMessageDialog(this, "Seleccione al menos una opción para borrar.");
            return;
        }

        // 2. Advertencia de Seguridad
        String input = JOptionPane.showInputDialog(this, 
            "ESTA ACCIÓN ES IRREVERSIBLE.\n\n" +
            "Para confirmar el borrado, escriba la palabra: ELIMINAR", 
            "Confirmación de Seguridad", JOptionPane.WARNING_MESSAGE);

        if (input != null && input.equals("ELIMINAR")) {
            ejecutarLimpieza();
        } else {
            if (input != null) JOptionPane.showMessageDialog(this, "Palabra incorrecta. Operación cancelada.");
        }
    }

    private void ejecutarLimpieza() {
        // 1. Limpiar Base de Datos
        List<String> tablas = new ArrayList<>();
        if (chkDbFacturas.isSelected()) {
            tablas.add("detalle_facturas"); // Primero detalles
            tablas.add("facturas");         // Luego cabeceras
        }
        if (chkDbDevoluciones.isSelected()) tablas.add("devoluciones");
        if (chkDbGastos.isSelected()) tablas.add("gastos");
        if (chkDbCaja.isSelected()) tablas.add("arqueos_caja");
        if (chkDbProductos.isSelected()) tablas.add("productos");
        if (chkDbClientes.isSelected()) tablas.add("clientes"); // Cuidado: esto puede borrar facturas por cascada si no se seleccionaron antes

        if (!tablas.isEmpty()) {
            DataCleaner.vaciarTablasSeleccionadas(tablas);
        }

        // 2. Limpiar Archivos
        DataCleaner.vaciarArchivosSeleccionados(
            chkCsvClientes.isSelected(),
            chkCsvProductos.isSelected(),
            chkCsvArqueos.isSelected(),
            chkCsvDevoluciones.isSelected(),
            chkTxtFacturas.isSelected()
        );
    }
}