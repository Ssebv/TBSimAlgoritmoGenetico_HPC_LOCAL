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

    public LogManager(Logger existingLogger, boolean enableColors) {
        this.logger = existingLogger;
        this.enableColors = enableColors;
        configureLogger();
    }

    // Constructor secundario si deseas forzar sin color
    public LogManager(Logger existingLogger) {
        this(existingLogger, false);
    }

    private void configureLogger() {
        // Removemos cualquier handler previo
        logger.setUseParentHandlers(false);

        boolean hasConsole = false;
        // Revisar si hay un ConsoleHandler ya
        for (var handler : logger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                hasConsole = true;
                break;
            }
        }
        if (!hasConsole) {
            // Crear un ConsoleHandler que imprime el mensaje en sí
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            consoleHandler.setFormatter(new java.util.logging.SimpleFormatter() {
                @Override
                public String format(LogRecord record) {
                    // El 'record.getMessage()' ya vendrá con ANSI si enableColors = true
                    return record.getMessage() + "\n";
                }
            });
            logger.addHandler(consoleHandler);
        }
        logger.setLevel(Level.ALL);
    }

    /**
     * Log para las generaciones (con colores si enableColors = true).
     */
    public void logGeneracion(EvolutionResult<DoubleGene, Double> result,
                              double avgFitness,
                              double diversity,
                              double worstFitness,
                              long elapsedTime,
                              FuncionEvaluacionJenetics fitnessEvaluator,
                              CSVManager csvManager) {

        int gen = (int) result.generation();
        double bestFit = result.bestFitness();

        logger.info(
            "\n" + formatWithColor("===============================================", "\u001B[34m")
        );
        logger.info(formatWithColor("[GENÉTICO] GENERACIÓN " + gen + ":", "\u001B[34m"));
        logger.info(formatWithColor("===============================================", "\u001B[34m"));
        logger.info(formatWithColor(String.format("[GENÉTICO]    Mejor Fitness:       %.2f", bestFit), "\u001B[32m"));
        logger.info(formatWithColor(String.format("[GENÉTICO]    Promedio Fitness:    %.2f", avgFitness), "\u001B[32m"));
        logger.info(formatWithColor(String.format("[GENÉTICO]    Diversidad:          %.2f", diversity), "\u001B[32m"));
        logger.info(formatWithColor(String.format("[GENÉTICO]    Peor Fitness:        %.2f", worstFitness), "\u001B[31m"));
        logger.info(formatWithColor(
                String.format("[GENÉTICO]    Tiempo Transcurrido: %.2f s", (elapsedTime / 1000.0)),
                "\u001B[33m")
        );
        logger.info(formatWithColor("===============================================", "\u001B[34m"));

        // Escribir en CSV
        csvManager.escribirLineaCSV(
                gen,
                bestFit,
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

    /**
     * Log para élites
     */
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

    /**
     * Log del resultado del partido (ej: goles, fitness).
     */
    public void logResultadoPartido(FuncionEvaluacionJenetics.ResultadoPartido resultado) {
        logger.info(
            formatWithColor(
                String.format("[RESULTADO PARTIDO] Goles Favor: %d, Goles Contra: %d, Fitness: %.2f",
                              resultado.getGolesFavor(),
                              resultado.getGolesContra(),
                              resultado.getFitness()),
                "\u001B[35m"
            )
        );
    }

    /**
     * Log checkpoint guardado
     */
    public void logCheckpointGuardado(String filePath) {
        logger.info(formatWithColor("[CHECKPOINT] Guardado correctamente en " + filePath, "\u001B[32m"));
    }

    /**
     * Log resumen final
     */
    public void logResumenFinal(int currentGeneration, double previousBestFitness) {
        logger.info(formatWithColor("\n================== RESUMEN FINAL ==================", "\u001B[34m"));
        logger.info(formatWithColor("[INFO] Generaciones ejecutadas: " + currentGeneration, "\u001B[32m"));
        logger.info(formatWithColor(String.format("[INFO] Mejor Fitness Alcanzado: %.2f", previousBestFitness), "\u001B[32m"));
        logger.info(formatWithColor("==================================================", "\u001B[34m"));
    }

    /**
     * Log detalles del entorno (HPC o local).
     */
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

    /**
     * Log normal (info)
     */
    public void logInfo(String message) {
        logger.info(formatWithColor(message, "\u001B[37m"));
    }

    /**
     * Log de advertencia
     */
    public void logWarning(String message) {
        // logger.warning imprime con nivel WARNING pero el texto se decora con color si enableColors
        logger.warning(formatWithColor(message, "\u001B[33m"));
    }

    /**
     * Log de error
     */
    public void logError(String message) {
        logger.severe(formatWithColor(message, "\u001B[31m"));
    }

    /**
     * Aplica el color ANSI solo si enableColors == true
     */
    private String formatWithColor(String message, String ansiColor) {
        if (!enableColors) {
            return message; // Sin ANSI
        }
        return ansiColor + message + "\u001B[0m";
    }
}
