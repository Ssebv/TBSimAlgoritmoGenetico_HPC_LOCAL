import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;
import java.util.stream.Collectors;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;

public class LogManager {
    private final Logger logger;
    private final boolean enableColors;
    private final Configuracion config;
    private final ExecutorService logExecutor;

    public LogManager(Logger existingLogger, boolean enableColors, Configuracion config) {
        this.logger = existingLogger;
        this.enableColors = enableColors;
        this.config = config;
        this.logExecutor = Executors.newSingleThreadExecutor();
        configureLogger();
    }

    public Configuracion getConfig() {
        return config;
    }

    private void configureLogger() {
        // Deshabilitar los handlers por defecto
        logger.setUseParentHandlers(false);
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }

        // Usar ConsoleHandler para mostrar los logs en la consola
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(new SimpleFormatter() {
            @Override
            public String format(LogRecord record) {
                String clean = record.getMessage().replaceAll("[\\r\\n]+", " ").trim();
                return clean.isEmpty() ? "" : clean + System.lineSeparator();
            }
        });
        logger.addHandler(consoleHandler);

        // Establecer nivel de log a INFO para mostrar todos los logs
        logger.setLevel(Level.ALL);
    }

    private void submitLog(Level level, String message) {
        if (message == null || message.trim().isEmpty())
            return;
        logExecutor.submit(() -> {
            synchronized (logger) {
                logger.log(level, message.trim());
                for (Handler h : logger.getHandlers())
                    h.flush();
            }
        });
    }

    public void logGeneracion(EvolutionResult<DoubleGene, Double> result,
            double avgFitness, double diversity,
            double worstFitness, long elapsedTime,
            FuncionEvaluacionJenetics evaluator,
            CSVManager csvManager) {

        int gen = GenerationTracker.getCurrentGeneration();
        double bestFit = result.bestFitness();
        double globalFit = evaluator.getBestFitness();

        logHeader("GENERACIÓN " + gen);
        logInfo(String.format("Mejor Fitness Generación: %.2f", bestFit));
        logInfo(String.format("Fitness Global: %.2f", globalFit));
        logInfo(String.format("Promedio Fitness: %.2f", avgFitness));
        logInfo(String.format("Diversidad: %.2f", diversity));
        logWarning(String.format("Peor Fitness: %.2f", Math.max(worstFitness, 1.0)));
        logInfo(String.format("Tiempo Transcurrido: %.2f s", elapsedTime / 1000.0));
        logLine();

        // No pasamos el cromosoma a escribir en el CSV
        csvManager.escribirLineaCSV(gen, bestFit, globalFit, avgFitness, diversity,
                Math.max(worstFitness, 1.0), RuntimeConfig.getSystemCpuLoad(),
                RuntimeConfig.getSystemMemoryLoad(), elapsedTime,
                evaluator.getBestGolesFavor(), evaluator.getBestGolesContra(),
                RuntimeConfig.getPerCoreLoad());
    }

    public void logResultadoPartido(FuncionEvaluacionJenetics.ResultadoPartido r) {
        logHighlight(String.format("[RESULTADO] GF: %d | GC: %d | Fitness: %.2f",
                r.getGolesFavor(), r.getGolesContra(), r.getFitness()));

    }

    public void logResumenFinal(int generacion, FuncionEvaluacionJenetics fitnessEvaluator) {
        logHeader("RESUMEN FINAL");
        logInfo("Generaciones ejecutadas: " + generacion);
        logInfo(String.format("Mejor Fitness Global: %.2f", fitnessEvaluator.getBestFitness()));
        logInfo("Goles a Favor: " + fitnessEvaluator.getBestGolesFavor());
        logInfo("Goles en Contra: " + fitnessEvaluator.getBestGolesContra());

        // Imprimir los mejores parámetros
        logInfo("Mejores parámetros:");
        StringBuilder params = new StringBuilder();
        for (int i = 0; i < fitnessEvaluator.getBestParams().length; i++) {
            params.append(String.format(" - Param %d: %.2f%n", i, fitnessEvaluator.getBestParams()[i]));
        }
        logInfo(params.toString());
        logInfo("Mejor Cromosoma: " + fitnessEvaluator.getBestChromosome());

        logLine();
    }

    public void logElitesSeleccionados(List<Double> elites) {
        if (elites.isEmpty()) {
            logInfo("[ELITE] No se encontraron élites.");
        } else {
            logInfo("[ELITE] Fitness de los seleccionados:");
            elites.forEach(e -> logInfo(String.format(" - %.2f", e)));
        }
    }

    public void logDetallesDelEntorno(boolean isHPC) {
        logHeader("ENTORNO");
        logInfo(isHPC ? "[HPC] Entorno detectado (SLURM)." : "[LOCAL] Ejecución en entorno local.");
        logInfo("Sistema Operativo: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        logInfo("Versión de Java: " + System.getProperty("java.version"));
        logInfo("CPUs (JVM): " + Runtime.getRuntime().availableProcessors());
        logInfo("CPUs Config (config): " + config.getNumCores());
        logLine();
    }

    // Métodos de log con colores (solo para consola)
    public void logInfo(String msg) {
        submitLog(Level.INFO, format(msg, "\u001B[37m"));
    }

    public void logWarning(String msg) {
        submitLog(Level.WARNING, format(msg, "\u001B[33m"));
    }

    public void logError(String msg) {
        submitLog(Level.SEVERE, format(msg, "\u001B[31m"));
    }

    public void logHighlight(String m) {
        submitLog(Level.INFO, format(m, "\u001B[35m"));
    }

    private void logHeader(String title) {
        String line = "=".repeat(50);
        submitLog(Level.INFO, format(line, "\u001B[34m"));
        submitLog(Level.INFO, format("[INFO] " + title, "\u001B[34m"));
        submitLog(Level.INFO, format(line, "\u001B[34m"));
    }

    private void logLine() {
        submitLog(Level.INFO, format("-".repeat(50), "\u001B[34m"));
    }

    private String format(String msg, String color) {
        return enableColors ? color + msg + "\u001B[0m" : msg;
    }

    public void shutdown() {
        logExecutor.shutdown();
        try {
            if (!logExecutor.awaitTermination(5, TimeUnit.MINUTES)) {
                logger.warning("Log executor no terminó a tiempo. Forzando cierre...");
                logExecutor.shutdownNow();
                if (!logExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.severe("No se pudo cerrar el log executor.");
                }
            }
        } catch (InterruptedException ie) {
            logger.severe("Interrumpido al cerrar log executor: " + ie.getMessage());
            logExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        for (Handler h : logger.getHandlers())
            h.flush();
    }
}
