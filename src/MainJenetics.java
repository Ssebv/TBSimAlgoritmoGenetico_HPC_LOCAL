import java.io.File;
import java.util.logging.Logger;

public class MainJenetics {
    private static final Logger LOGGER = Logger.getLogger(MainJenetics.class.getName());

    public static void main(String[] args) {
        CSVManager csvManager = null;
        LogManager logManager = null;
        try {
            // 1. Instanciar la Configuración
            Configuracion config = ConfiguracionSingleton.getInstance(); // Acceso global a la configuración

            // 2. Crear LogManager (con colores si está habilitado)
            logManager = new LogManager(LOGGER, config.ENABLE_COLORS, config);

            // 3. Determinar entorno (HPC o local) y asignar nombre al CSV
            boolean isHPC = config.IS_HPC;
            String csvFileName = isHPC ? "hpc_stats.csv" : "local_stats.csv";
            File debugFile = new File(csvFileName);
            logManager.logInfo("[DEBUG] El CSV se usará en -> " + debugFile.getAbsolutePath());

            // 4. Crear CSVManager (sobrescribe el archivo)
            csvManager = new CSVManager(csvFileName, isHPC, config);

            // 5. Loguear detalles del entorno
            logManager.logDetallesDelEntorno(isHPC);

            // 6. Manejo de checkpoint: cargar automáticamente si existe.
            CheckpointData checkpointData = null;
            File checkpointFile = new File(config.CHECKPOINT_FILE);
            if (checkpointFile.exists()) {
                checkpointData = CheckpointManager.cargarCheckpoint(config.CHECKPOINT_FILE);
                if (checkpointData != null) {
                    LOGGER.info("Checkpoint cargado automáticamente desde " + checkpointFile.getAbsolutePath());
                } else {
                    LOGGER.warning("No se pudo cargar el checkpoint. Se iniciará desde cero.");
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
            if (logManager != null) {
                logManager.shutdown();
            }
            LOGGER.info("Recursos cerrados correctamente.");
        }
    }
}
