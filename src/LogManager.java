import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    private final ExecutorService logExecutor;

    // Constructor que recibe Logger, enableColors y la configuración
    public LogManager(Logger existingLogger, boolean enableColors, Configuracion config) {
        this.logger = existingLogger;
        this.enableColors = enableColors;
        this.config = config;
        configureLogger();
        // Executor de un solo hilo para mantener el orden de los mensajes sin bloquear el hilo principal
        this.logExecutor = Executors.newSingleThreadExecutor();
    }

    // Constructor secundario (sin colores)
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
            // Formatter que elimina saltos de línea internos (convertidos a espacio) y añade solo un salto al final.
            consoleHandler.setFormatter(new java.util.logging.SimpleFormatter() {
                @Override
                public String format(LogRecord record) {
                    String msg = record.getMessage().replaceAll("[\\r\\n]+", " ").trim();
                    return msg + "\n";
                }
            });
            logger.addHandler(consoleHandler);
        }
        logger.setLevel(Level.ALL);
    }

    // Método para enviar mensajes al logger de forma asíncrona
    private void submitLog(Level level, String message) {
        logExecutor.submit(() -> logger.log(level, message));
    }

    public void logGeneracion(EvolutionResult<DoubleGene, Double> result,
                              double avgFitness,
                              double diversity,
                              double worstFitness,
                              long elapsedTime,
                              FuncionEvaluacionJenetics fitnessEvaluator,
                              CSVManager csvManager) {

        int gen = GenerationTracker.getCurrentGeneration();
        double bestFitGeneration = result.bestFitness();
        double fitnessGlobal = fitnessEvaluator.getBestFitness();

        submitLog(Level.INFO, formatWithColor("===============================================", "\u001B[34m"));
        submitLog(Level.INFO, formatWithColor("[GENÉTICO] GENERACIÓN " + gen + ":", "\u001B[34m"));
        submitLog(Level.INFO, formatWithColor("===============================================", "\u001B[34m"));
        submitLog(Level.INFO, formatWithColor(String.format("[GENÉTICO]    Mejor Fitness Generación: %.2f", bestFitGeneration), "\u001B[32m"));
        submitLog(Level.INFO, formatWithColor(String.format("[GENÉTICO]    Fitness Global:         %.2f", fitnessGlobal), "\u001B[32m"));
        submitLog(Level.INFO, formatWithColor(String.format("[GENÉTICO]    Promedio Fitness:       %.2f", avgFitness), "\u001B[32m"));
        submitLog(Level.INFO, formatWithColor(String.format("[GENÉTICO]    Diversidad:             %.2f", diversity), "\u001B[32m"));
        submitLog(Level.INFO, formatWithColor(String.format("[GENÉTICO]    Peor Fitness:           %.2f", worstFitness), "\u001B[31m"));
        submitLog(Level.INFO, formatWithColor(String.format("[GENÉTICO]    Tiempo Transcurrido:    %.2f s", (elapsedTime / 1000.0)), "\u001B[33m"));
        submitLog(Level.INFO, formatWithColor("===============================================", "\u001B[34m"));

        // Escritura en CSV se mantiene síncrona, pues es crítica para el análisis
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
            submitLog(Level.INFO, formatWithColor("[ELITE] No se encontraron élites.", "\u001B[36m"));
            return;
        }
        submitLog(Level.INFO, formatWithColor("[ELITE] Selección de élites:", "\u001B[36m"));
        for (Double e : elites) {
            submitLog(Level.INFO, formatWithColor(String.format("[ELITE]    Fitness: %.2f", e), "\u001B[33m"));
        }
    }

    public void logResultadoPartido(FuncionEvaluacionJenetics.ResultadoPartido resultado) {
        submitLog(Level.INFO, formatWithColor(
                String.format("[RESULTADO PARTIDO] Goles Favor: %d, Goles Contra: %d, Fitness: %.2f",
                              resultado.getGolesFavor(),
                              resultado.getGolesContra(),
                              resultado.getFitness()),
                "\u001B[35m"
        ));
    }

    public void logCheckpointGuardado(String filePath) {
        submitLog(Level.INFO, formatWithColor("[CHECKPOINT] Guardado correctamente en " + filePath, "\u001B[32m"));
    }

    public void logResumenFinal(int currentGeneration, double bestFitness) {
        submitLog(Level.INFO, formatWithColor("================== RESUMEN FINAL ==================", "\u001B[34m"));
        submitLog(Level.INFO, formatWithColor("[INFO] Generaciones ejecutadas: " + currentGeneration, "\u001B[32m"));
        submitLog(Level.INFO, formatWithColor(String.format("[INFO] Mejor Fitness Alcanzado: %.2f", bestFitness), "\u001B[32m"));
        submitLog(Level.INFO, formatWithColor("==================================================", "\u001B[34m"));
    }

    public void logDetallesDelEntorno(boolean isHPC) {
        submitLog(Level.INFO, formatWithColor("======================================================", "\u001B[34m"));
        submitLog(Level.INFO, formatWithColor("[INFO] Configuración del Entorno", "\u001B[32m"));
        submitLog(Level.INFO, formatWithColor("======================================================", "\u001B[34m"));

        if (isHPC) {
            submitLog(Level.INFO, formatWithColor("[HPC] Entorno HPC detectado (SLURM).", "\u001B[33m"));
        } else {
            submitLog(Level.INFO, formatWithColor("[LOCAL] Ejecutando en un entorno local:", "\u001B[36m"));
            submitLog(Level.INFO, formatWithColor("[LOCAL] Sistema Operativo: " + System.getProperty("os.name")
                    + " " + System.getProperty("os.version"), "\u001B[37m"));
            submitLog(Level.INFO, formatWithColor("[LOCAL] Versión de Java: " + System.getProperty("java.version"), "\u001B[37m"));
            submitLog(Level.INFO, formatWithColor("[LOCAL] CPUs disponibles: " + Runtime.getRuntime().availableProcessors(), "\u001B[37m"));
        }
        submitLog(Level.INFO, formatWithColor("======================================================", "\u001B[34m"));
    }

    public void logInfo(String message) {
        submitLog(Level.INFO, formatWithColor(message, "\u001B[37m"));
    }

    public void logWarning(String message) {
        submitLog(Level.WARNING, formatWithColor(message, "\u001B[33m"));
    }

    public void logError(String message) {
        submitLog(Level.SEVERE, formatWithColor(message, "\u001B[31m"));
    }

    private String formatWithColor(String message, String ansiColor) {
        if (!enableColors) {
            return message;
        }
        return ansiColor + message + "\u001B[0m";
    }

    public void shutdown() {
        logExecutor.shutdown();
        try {
            if (!logExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                logExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logExecutor.shutdownNow();
        }
    }
}
