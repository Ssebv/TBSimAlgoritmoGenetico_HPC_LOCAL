import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import io.jenetics.DoubleGene;
import io.jenetics.engine.EvolutionResult;

public class LogManager {
    private final Logger logger;
    private final boolean enableColors;
    private final Configuracion config;

    // Constructor que recibe Logger, enableColors y la configuración
    public LogManager(Logger existingLogger, boolean enableColors, Configuracion config) {
        this.logger = existingLogger;
        this.enableColors = enableColors;
        this.config = config;
        configureLogger();
    }

    // Constructor secundario (sin colores) si se prefiere
    public LogManager(Logger existingLogger, Configuracion config) {
        this(existingLogger, false, config);
    }

    public Configuracion getConfig() {
        return config;
    }

    private void configureLogger() {
        logger.setUseParentHandlers(false);
        boolean hasConsole = false;
        for (var handler : logger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                hasConsole = true;
                break;
            }
        }
        if (!hasConsole) {
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            consoleHandler.setFormatter(new java.util.logging.SimpleFormatter() {
                @Override
                public String format(LogRecord record) {
                    return record.getMessage() + "\n";
                }
            });
            logger.addHandler(consoleHandler);
        }
        logger.setLevel(Level.ALL);
    }

    /**
     * Registra información de la generación, incluyendo el mejor fitness por generación
     * y el fitness global.
     */
    public void logGeneracion(EvolutionResult<DoubleGene, Double> result,
                              double avgFitness,
                              double diversity,
                              double worstFitness,
                              long elapsedTime,
                              FuncionEvaluacionJenetics fitnessEvaluator,
                              CSVManager csvManager) {

        int gen = GenerationTracker.getCurrentGeneration();
        // bestFit es el mejor fitness de la generación actual
        double bestFitGeneration = result.bestFitness();
        // fitnessGlobal se obtiene de la instancia de FuncionEvaluacionJenetics
        double fitnessGlobal = fitnessEvaluator.getBestFitness();

        logger.info("\n" + formatWithColor("===============================================", "\u001B[34m"));
        logger.info(formatWithColor("[GENÉTICO] GENERACIÓN " + gen + ":", "\u001B[34m"));
        logger.info(formatWithColor("===============================================", "\u001B[34m"));
        logger.info(formatWithColor(String.format("[GENÉTICO]    Mejor Fitness Generación: %.2f", bestFitGeneration), "\u001B[32m"));
        logger.info(formatWithColor(String.format("[GENÉTICO]    Fitness Global:         %.2f", fitnessGlobal), "\u001B[32m"));
        logger.info(formatWithColor(String.format("[GENÉTICO]    Promedio Fitness:       %.2f", avgFitness), "\u001B[32m"));
        logger.info(formatWithColor(String.format("[GENÉTICO]    Diversidad:             %.2f", diversity), "\u001B[32m"));
        logger.info(formatWithColor(String.format("[GENÉTICO]    Peor Fitness:           %.2f", worstFitness), "\u001B[31m"));
        logger.info(formatWithColor(String.format("[GENÉTICO]    Tiempo Transcurrido:    %.2f s", (elapsedTime / 1000.0)), "\u001B[33m"));
        logger.info(formatWithColor("===============================================", "\u001B[34m"));

        // Llamada actualizada para incluir Mejor Fitness de la generación y Fitness Global
        csvManager.escribirLineaCSV(
                gen,
                bestFitGeneration,
                fitnessGlobal,
                avgFitness,
                diversity,
                worstFitness,
                RuntimeConfig.getSystemCpuLoad(),
                RuntimeConfig.getSystemMemoryLoad(),
                elapsedTime,
                fitnessEvaluator.getBestGolesFavor(),
                fitnessEvaluator.getBestGolesContra()
        );
    }

    public void logElitesSeleccionados(List<Double> elites) {
        if (elites.isEmpty()) {
            logger.info(formatWithColor("[ELITE] No se encontraron élites.", "\u001B[36m"));
            return;
        }
        logger.info(formatWithColor("[ELITE] Selección de élites:", "\u001B[36m"));
        for (Double e : elites) {
            logger.info(formatWithColor(String.format("[ELITE]    Fitness: %.2f", e), "\u001B[33m"));
        }
    }

    public void logResultadoPartido(FuncionEvaluacionJenetics.ResultadoPartido resultado) {
        logger.info(formatWithColor(
                String.format("[RESULTADO PARTIDO] Goles Favor: %d, Goles Contra: %d, Fitness: %.2f",
                              resultado.getGolesFavor(),
                              resultado.getGolesContra(),
                              resultado.getFitness()),
                "\u001B[35m"
        ));
    }

    public void logCheckpointGuardado(String filePath) {
        logger.info(formatWithColor("[CHECKPOINT] Guardado correctamente en " + filePath, "\u001B[32m"));
    }

    public void logResumenFinal(int currentGeneration, double bestFitness) {
        logger.info(formatWithColor("\n================== RESUMEN FINAL ==================", "\u001B[34m"));
        logger.info(formatWithColor("[INFO] Generaciones ejecutadas: " + currentGeneration, "\u001B[32m"));
        logger.info(formatWithColor(String.format("[INFO] Mejor Fitness Alcanzado: %.2f", bestFitness), "\u001B[32m"));
        logger.info(formatWithColor("==================================================", "\u001B[34m"));
    }

    // Registra detalles del entorno de ejecución
    public void logDetallesDelEntorno(boolean isHPC) {
        logger.info(formatWithColor("======================================================", "\u001B[34m"));
        logger.info(formatWithColor("[INFO] Configuración del Entorno", "\u001B[32m"));
        logger.info(formatWithColor("======================================================", "\u001B[34m"));

        if (isHPC) {
            logger.info(formatWithColor("[HPC] Entorno HPC detectado (SLURM).", "\u001B[33m"));
        } else {
            logger.info(formatWithColor("[LOCAL] Ejecutando en un entorno local:", "\u001B[36m"));
            logger.info(formatWithColor("[LOCAL] Sistema Operativo: " + System.getProperty("os.name")
                    + " " + System.getProperty("os.version"), "\u001B[37m"));
            logger.info(formatWithColor("[LOCAL] Versión de Java: " + System.getProperty("java.version"), "\u001B[37m"));
            logger.info(formatWithColor("[LOCAL] CPUs disponibles: " + Runtime.getRuntime().availableProcessors(), "\u001B[37m"));
        }
        logger.info(formatWithColor("======================================================", "\u001B[34m"));
    }

    public void logInfo(String message) {
        logger.info(formatWithColor(message, "\u001B[37m"));
    }

    public void logWarning(String message) {
        logger.warning(formatWithColor(message, "\u001B[33m"));
    }

    public void logError(String message) {
        logger.severe(formatWithColor(message, "\u001B[31m"));
    }

    private String formatWithColor(String message, String ansiColor) {
        if (!enableColors) {
            return message;
        }
        return ansiColor + message + "\u001B[0m";
    }
}
