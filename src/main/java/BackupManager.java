import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.Properties;
import java.util.Map;

public class BackupManager {

    private static final String NOMBRE_DUMP_SQL = "backup_base_datos.sql";

    public static void crearCopiaDeSeguridad(JFrame parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Copia de Seguridad Completa (Híbrida)");
        fileChooser.setSelectedFile(new File("Respaldo_Sistema_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date()) + ".zip"));

        if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File archivoDestino = fileChooser.getSelectedFile();
            if (!archivoDestino.getName().toLowerCase().endsWith(".zip")) {
                archivoDestino = new File(archivoDestino.getAbsolutePath() + ".zip");
            }

            File finalArchivoDestino = archivoDestino;
            new Thread(() -> {
                try {
                    // 1. Crear carpeta temporal
                    File carpetaTemp = Files.createTempDirectory("backup_facturaapp_tmp").toFile();
                    
                    // 2. Exportar Base de Datos PostgreSQL
                    boolean dbExportada = exportarBaseDatosPostgres(new File(carpetaTemp, NOMBRE_DUMP_SQL));
                    
                    if (!dbExportada) {
                        SwingUtilities.invokeLater(() -> 
                            JOptionPane.showMessageDialog(parent, "Advertencia: No se pudo respaldar la Base de Datos.\nVerifique config.properties.", "Advertencia", JOptionPane.WARNING_MESSAGE)
                        );
                    }

                    // 3. Copiar la carpeta de Archivos del Sistema
                    // --- CORRECCIÓN AQUÍ: Usamos PathsHelper para obtener la ruta real (AppData) ---
                    File carpetaArchivosOrigen = PathsHelper.getDatosFolder();
                    
                    if (carpetaArchivosOrigen.exists()) {
                        System.out.println("Copiando archivos desde: " + carpetaArchivosOrigen.getAbsolutePath());
                        // Copiamos todo el contenido de AppData/FacturaApp a una subcarpeta 'archivos_sistema' en el temporal
                        copiarDirectorio(carpetaArchivosOrigen.toPath(), new File(carpetaTemp, "archivos_sistema").toPath());
                    } else {
                        System.err.println("No se encontró la carpeta de datos en: " + carpetaArchivosOrigen.getAbsolutePath());
                    }

                    // 4. Comprimir todo en el ZIP
                    empaquetarZip(carpetaTemp, finalArchivoDestino);

                    // 5. Limpieza
                    borrarDirectorioRecursivo(carpetaTemp);

                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(parent, "Copia de seguridad exitosa en:\n" + finalArchivoDestino.getAbsolutePath())
                    );

                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(parent, "Error al crear respaldo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
                    );
                }
            }).start();
        }
    }

    private static boolean exportarBaseDatosPostgres(File archivoSalida) {
        try {
            Properties props = new Properties();
            File archivoConfig = PathsHelper.getConfigProperties();

            if (archivoConfig.exists()) {
                try (FileInputStream in = new FileInputStream(archivoConfig)) {
                    props.load(in);
                }
            } else {
                return false;
            }
            
            String dbName = props.getProperty("db.name", "FacturaAPP");
            String dbUser = props.getProperty("db.user", "postgres");
            String dbPass = props.getProperty("db.password", "0534");
            String pgDumpPath = props.getProperty("db.pgdump_path", "pg_dump"); 

            ProcessBuilder pb = new ProcessBuilder(
                pgDumpPath,
                "-h", "localhost",
                "-p", "5432",
                "-U", dbUser,
                "-F", "p",
                "-f", archivoSalida.getAbsolutePath(),
                dbName
            );

            Map<String, String> env = pb.environment();
            env.put("PGPASSWORD", dbPass);

            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            // Consumir el stream para evitar bloqueos
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (reader.readLine() != null) {
                // Solo leemos para vaciar el buffer, no necesitamos guardar la variable 'line'
            }
            
            return p.waitFor() == 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void restaurarCopiaDeSeguridad(JFrame parent) {
        JOptionPane.showMessageDialog(parent, 
            "Para restaurar una copia híbrida:\n\n" +
            "1. Descomprima el ZIP.\n" +
            "2. Reemplace el contenido de su carpeta de datos (" + PathsHelper.getDatosFolder().getAbsolutePath() + ") con la carpeta 'archivos_sistema'.\n" +
            "3. Cargue el archivo .sql en PostgreSQL usando pgAdmin.", 
            "Instrucciones de Restauración", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void empaquetarZip(File carpetaOrigen, File archivoZipDestino) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(archivoZipDestino);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            Path sourcePath = carpetaOrigen.toPath();
            Files.walk(sourcePath)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    // Evitamos empaquetar archivos de bloqueo o temporales si existen
                    if(!path.toString().endsWith(".lock")) {
                        ZipEntry zipEntry = new ZipEntry(sourcePath.relativize(path).toString());
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            System.err.println("No se pudo comprimir el archivo: " + path);
                        }
                    }
                });
        }
    }

    private static void copiarDirectorio(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    if (!Files.exists(targetPath)) Files.createDirectory(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                // Ignoramos errores de copia en archivos bloqueados
                System.err.println("Error copiando archivo (posiblemente en uso): " + sourcePath);
            }
        });
    }
    
    private static void borrarDirectorioRecursivo(File directorio) {
        if (directorio.isDirectory()) {
            File[] archivos = directorio.listFiles();
            if (archivos != null) {
                for (File archivo : archivos) borrarDirectorioRecursivo(archivo);
            }
        }
        directorio.delete();
    }
}