import java.util.logging.*;
import java.io.*;
import io.jenetics.*;
import io.jenetics.engine.*;
import io.jenetics.util.ISeq;

public class LoggerUtility {
    private static PrintWriter csvWriter;
    private static final Logger LOGGER = Logger.getLogger(LoggerUtility.class.getName());

    public static Logger configurarLogger() {
        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());
        rootLogger.addHandler(handler);
        rootLogger.setLevel(Level.INFO);
        return rootLogger;
    }

    public static void configurarInterrupcion() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> LOGGER.info("Interrupción detectada.")));
    }

    public static void prepararArchivos() {
        try {
            File archivoMetricas = new File("metricas_ag.csv");
            if (!archivoMetricas.exists()) {
                try (FileWriter fw = new FileWriter(archivoMetricas)) {
                    fw.write("Generación,Mejor Fitness,Fitness Promedio,Diversidad,Peor Fitness\n");
                }
            }
        } catch (IOException e) {
            LOGGER.warning("Error preparando archivos: " + e.getMessage());
        }
    }

    public static void prepararCSV(boolean isHPC) {
        try {
            String nombreArchivo = isHPC ? "hpc_stats.csv" : "local_stats.csv";
            csvWriter = new PrintWriter(new FileWriter(nombreArchivo));
            csvWriter.println(
                    "Generación,Mejor Fitness,Fitness Promedio,Diversidad,Peor Fitness,CPU (%),Memoria (%),Tiempo (s)");
        } catch (IOException e) {
            LOGGER.warning("No se pudo crear el archivo CSV.");
        }
    }

    public static void cerrarCSV() {
        if (csvWriter != null) {
            csvWriter.flush();
            csvWriter.close();
        }
    }

    public static void logResultadoPartido(FuncionEvaluacionJenetics.ResultadoPartido resultado) {
        LOGGER.info(String.format(
                "[RESULTADO PARTIDO] Goles Favor: %d, Goles Contra: %d, Fitness: %.2f",
                resultado.getGolesFavor(),
                resultado.getGolesContra(),
                resultado.getFitness()));
    }

    public static void logElitesSeleccionados(ISeq<Phenotype<DoubleGene, Double>> elites) {
        LOGGER.info("[ELITE] Selección de élites:");
        elites.forEach(elite -> LOGGER.info(String.format("[ELITE] Fitness: %.2f", elite.fitness())));
    }

    public static void logGeneracion(EvolutionResult<DoubleGene, Double> result, Configuracion config) {
        LOGGER.info(String.format("[GENERACIÓN %d] Mejor Fitness: %.2f", config.currentGeneration,
                result.bestFitness()));
    }

    public static void handleFatalError(Exception e, String checkpointFile, EvolutionResult<DoubleGene, Double> lastResult) {
        LOGGER.log(Level.SEVERE, "Error fatal: ", e);
        if (lastResult != null) {
            CheckpointManager.guardarCheckpoint(lastResult, checkpointFile);
            LOGGER.info("Checkpoint guardado tras el error.");
        }
    }
}
