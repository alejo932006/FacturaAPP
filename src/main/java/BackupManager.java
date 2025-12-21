// Crea este nuevo archivo completo: BackupManager.java

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupManager {

    /**
     * Inicia el proceso de creación de una copia de seguridad.
     * Abre un diálogo para que el usuario elija dónde guardar el archivo .zip.
     */
    public static void crearCopiaDeSeguridad(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar Copia de Seguridad");
        // Sugerimos un nombre de archivo con la fecha actual
        chooser.setSelectedFile(new File("backup_facturaapp_" + LocalDate.now() + ".zip"));
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos ZIP (*.zip)", "zip"));

        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File archivoDestino = chooser.getSelectedFile();
            // Asegurarnos de que el archivo tenga la extensión .zip
            if (!archivoDestino.getName().toLowerCase().endsWith(".zip")) {
                archivoDestino = new File(archivoDestino.getParentFile(), archivoDestino.getName() + ".zip");
            }

            try {
                Path carpetaFuente = PathsHelper.getDatosFolder().toPath();
                comprimirCarpeta(carpetaFuente, archivoDestino.toPath());
                JOptionPane.showMessageDialog(parent, "Copia de seguridad creada exitosamente en:\n" + archivoDestino.getAbsolutePath(), "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(parent, "Error al crear la copia de seguridad: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Inicia el proceso de restauración desde una copia de seguridad.
     * Pide confirmación, luego un diálogo para elegir el archivo .zip y finalmente reinicia la app.
     */
    public static void restaurarCopiaDeSeguridad(Component parent) {
        int respuesta = JOptionPane.showConfirmDialog(
            parent,
            "ADVERTENCIA:\n\nRestaurar una copia de seguridad reemplazará TODOS los datos actuales.\n" +
            "Esta acción es irreversible y la aplicación se cerrará al finalizar.\n\n" +
            "¿Está seguro de que desea continuar?",
            "Confirmar Restauración",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.ERROR_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar Copia de Seguridad para Restaurar");
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos ZIP (*.zip)", "zip"));

        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File archivoFuente = chooser.getSelectedFile();
            try {
                Path carpetaDestino = PathsHelper.getDatosFolder().toPath();
                descomprimirCarpeta(archivoFuente.toPath(), carpetaDestino);
                JOptionPane.showMessageDialog(parent, "Restauración completada exitosamente.\nLa aplicación se cerrará ahora.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0); // Cierra la aplicación para que los cambios surtan efecto al reabrir
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(parent, "Error al restaurar la copia de seguridad: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Lógica interna para comprimir una carpeta a un archivo ZIP.
     */
    private static void comprimirCarpeta(Path source, Path target) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(target.toFile()))) {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(source.relativize(file).toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Lógica interna para descomprimir un archivo ZIP a una carpeta.
     */
    private static void descomprimirCarpeta(Path source, Path target) throws IOException {
        // Primero, borramos el contenido actual de la carpeta de destino
        Files.walk(target).sorted(java.util.Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(source.toFile()))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                Path newPath = target.resolve(zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    if (newPath.getParent() != null) {
                        if (Files.notExists(newPath.getParent())) {
                            Files.createDirectories(newPath.getParent());
                        }
                    }
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }
}