import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class MainJenetics {
    private static final Logger LOGGER = Logger.getLogger(MainJenetics.class.getName());

    public static void main(String[] args) {
        CSVManager csvManager = null;
        try {
            // 1. Instanciar la Configuración
            Configuracion config = ConfiguracionSingleton.getInstance();

            // 2. Crear LogManager (con colores si está habilitado)
            LogManager logManager = new LogManager(LOGGER, config.ENABLE_COLORS, config);

            // 3. Determinar entorno (HPC o local) y asignar nombre al CSV
            boolean isHPC = config.IS_HPC;
            String csvFileName = isHPC ? "hpc_stats.csv" : "local_stats.csv";
            File debugFile = new File(csvFileName);
            System.out.println("[DEBUG] El CSV se usará en -> " + debugFile.getAbsolutePath());

            // 4. Crear CSVManager (sobrescribe el archivo)
            csvManager = new CSVManager(csvFileName, isHPC, config);

            // 5. Loguear detalles del entorno
            logManager.logDetallesDelEntorno(isHPC);

            // 6. Manejo de checkpoint
            CheckpointData checkpointData = null;
            File checkpointFile = new File(config.CHECKPOINT_FILE);
            if (checkpointFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                    System.out.println("¿Deseas USAR el checkpoint existente? (s/n)");
                    String resp = reader.readLine().trim().toLowerCase();
                    if (resp.equals("s") || resp.equals("si")) {
                        checkpointData = CheckpointManager.cargarCheckpoint(config.CHECKPOINT_FILE);
                        if (checkpointData == null) {
                            LOGGER.warning("No se pudo cargar el checkpoint. Se iniciará vacío.");
                        }
                    } else {
                        System.out.println("¿Quieres ELIMINAR el checkpoint existente? (s/n)");
                        String respDel = reader.readLine().trim().toLowerCase();
                        if (respDel.equals("s") || respDel.equals("si")) {
                            if (checkpointFile.delete()) {
                                LOGGER.info("Checkpoint eliminado a petición del usuario.");
                            } else {
                                LOGGER.warning("No se pudo eliminar el archivo de checkpoint.");
                            }
                        } else {
                            LOGGER.info("Se mantiene el checkpoint en disco, pero no se usará.");
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warning("Error leyendo respuesta del usuario: " + e.getMessage());
                }
            } else {
                LOGGER.info("No existe un checkpoint previo. Se iniciará desde cero.");
            }

            // 7. Crear y ejecutar el EvolutionManager
            EvolutionManager evolutionManager = new EvolutionManager(csvManager, config.CHECKPOINT_FILE, LOGGER, config);
            if (checkpointData != null) {
                evolutionManager.setCheckpointData(checkpointData);
            }
            evolutionManager.runGeneticEngine();
            
        } catch (Exception e) {
            LOGGER.severe("Error en la ejecución principal: " + e.getMessage());
        } finally {
            if (csvManager != null) {
                csvManager.cerrarCSV();
            }
            LOGGER.info("Recursos cerrados correctamente.");
        }
    }
}
