import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class MainJenetics {
    private static final Logger LOGGER = Logger.getLogger(MainJenetics.class.getName());

    private static CSVManager csvManager;
    private static EvolutionManager evolutionManager;
    private static LogManager logManager;

    public static void main(String[] args) {
        try {
            // 1. Instanciar la Configuración (para saber si HPC, colores, etc.)
            Configuracion config = new Configuracion();

            // 2. Crear LogManager pasando el ENABLE_COLORS y la configuración
            logManager = new LogManager(LOGGER, config.ENABLE_COLORS, config);
            // O alternativamente: logManager = new LogManager(LOGGER, config);

            // 3. Determinar HPC o local -> CSV
            boolean isHPC = config.IS_HPC;
            String csvFileName = isHPC ? "hpc_stats.csv" : "local_stats.csv";

            // 4. (Opcional) Imprimir ruta absoluta para debug
            File debugFile = new File(csvFileName);
            System.out.println("[DEBUG] El CSV se usará en -> " + debugFile.getAbsolutePath());

            // 5. Crear CSVManager (no borra ni recrea CSV a mitad)
            csvManager = new CSVManager("local_stats.csv", config.IS_HPC, config);

            // 6. Loguear detalles del entorno
            logManager.logDetallesDelEntorno(isHPC);

            // 7. Verificar si existe checkpoint. Si no, iniciamos vacío
            CheckpointData checkpointData = null;
            File checkpointFile = new File(config.CHECKPOINT_FILE);
            if (checkpointFile.exists()) {
                // 7a. Preguntar si deseamos usar el checkpoint
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                    System.out.println("¿Deseas USAR el checkpoint existente? (s/n)");
                    String resp = reader.readLine().trim().toLowerCase();

                    if (resp.equals("s") || resp.equals("si")) {
                        // 7b. Cargar checkpoint
                        checkpointData = CheckpointManager.cargarCheckpoint(config.CHECKPOINT_FILE);
                        if (checkpointData == null) {
                            LOGGER.warning("No se pudo cargar el checkpoint. Se iniciará vacío.");
                        }
                    } else {
                        // 7c. Preguntar si quieres eliminarlo
                        System.out.println("¿Quieres ELIMINAR el checkpoint existente? (s/n)");
                        String respDel = reader.readLine().trim().toLowerCase();
                        if (respDel.equals("s") || respDel.equals("si")) {
                            boolean deleted = checkpointFile.delete();
                            if (deleted) {
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

            // 8. Crear el EvolutionManager
            evolutionManager = new EvolutionManager(
                    csvManager,
                    config.CHECKPOINT_FILE,
                    LOGGER,
                    config
            );

            // 8a. Si cargamos un checkpoint, se lo pasamos al EvolutionManager
            if (checkpointData != null) {
                evolutionManager.setCheckpointData(checkpointData);
            }

            // 9. Ejecutar la evolución
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
