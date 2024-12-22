/* * MainJenetics.java
 *
 * Programa principal que ejecuta el algoritmo genético con Jenetics,
 * con un selector dinámico que ajusta el número de élites basado en el rendimiento promedio de la población.
 */
import java.util.Map;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.Handler;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.List; // Para usar la interfaz List
import java.util.stream.Collectors; // Para usar Collectors y convertir el Stream en una lista

import io.jenetics.*;
import io.jenetics.engine.*;
import io.jenetics.util.*;

public class MainJenetics {
    private static final Logger LOGGER = Logger.getLogger(MainJenetics.class.getName());
    private static final FuncionEvaluacionJenetics FUNCION_EVALUACION = new FuncionEvaluacionJenetics();

    private static final int INITIAL_POPULATION_SIZE = 100;
    private static final int DEFAULT_GENERATIONS = 1000;
    private static double mutationRate = 0.4;
    private static double crossoverRate = 0.8;

    private static int totalEvaluations = 0;
    private static int totalCrossovers = 0;
    private static double previousBestFitness = -1.0;
    private static double previousAverageFitness = -1.0;

    static {
        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter() {
            @Override
            public synchronized String format(java.util.logging.LogRecord record) {
                return String.format("%s: %s%n", record.getLevel(), record.getMessage());
            }
        });
        rootLogger.addHandler(consoleHandler);
    }

    public static void main(String[] args) {
        logEnvironment();

        ExecutorService executorService = configureExecutor();

        Factory<Genotype<DoubleGene>> genotypeFactory = Genotype.of(DoubleChromosome.of(0, 10, 50));

        Engine<DoubleGene, Double> engine = Engine.builder(MainJenetics::evaluar, genotypeFactory)
                .populationSize(INITIAL_POPULATION_SIZE)
                .offspringFraction(0.8)
                .alterers(new Mutator<>(mutationRate), new SinglePointCrossover<>(crossoverRate))
                .selector(new EliteSelector<>(3))  // Usamos un selector de élite que selecciona a los 3 mejores individuos
                .executor(executorService)
                .build();

        try {
            Phenotype<DoubleGene, Double> bestPhenotype = engine.stream()
                    .limit(DEFAULT_GENERATIONS)
                    .peek(MainJenetics::logGeneration)
                    .collect(EvolutionResult.toBestPhenotype());

            LOGGER.info("Mejor resultado global: " + bestPhenotype.genotype());
            LOGGER.info("Mejor aptitud global: " + bestPhenotype.fitness());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en la ejecución del algoritmo genético", e);
        } finally {
            executorService.shutdown();
        }
    }

    private static void logEnvironment() {
        if (isHPC()) {
            LOGGER.info("Ejecutando en un entorno HPC.");
        } else {
            LOGGER.info("Ejecutando en un entorno local.");
        }
    }

    private static boolean isHPC() {
        return detectByHostName() || detectByEnvVariables() || detectByResources();
    }

    private static boolean detectByHostName() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            return hostname.contains("hpc") || hostname.matches(".*cluster.*");
        } catch (UnknownHostException e) {
            LOGGER.warning("Error al obtener el nombre del host: " + e.getMessage());
            return false;
        }
    }

    private static boolean detectByEnvVariables() {
        Map<String, String> env = System.getenv();
        return env.containsKey("SLURM_JOB_ID") || env.containsKey("PBS_JOBID");
    }

    private static boolean detectByResources() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        long maxMemory = Runtime.getRuntime().maxMemory();
        return availableProcessors > 16 || maxMemory > 32L * 1024 * 1024 * 1024;
    }

    private static double evaluar(Genotype<DoubleGene> genotype) {
        try {
            return FUNCION_EVALUACION.evaluar(genotype);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error durante la evaluación del genotipo.", e);
            return -1.0;
        }
    }

    private static ExecutorService configureExecutor() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    private static void logGeneration(EvolutionResult<DoubleGene, Double> result) {
        int generation = (int) result.generation();
        Phenotype<DoubleGene, Double> bestPhenotype = result.bestPhenotype();
        double bestFitness = bestPhenotype.fitness();

        // Obtener goles a favor y en contra
        int golesFavor = FUNCION_EVALUACION.getLastGolesFavor();
        int golesContra = FUNCION_EVALUACION.getLastGolesContra();

        // Promedio de fitness y desviación estándar
        double averageFitness = result.population().stream()
                .mapToDouble(phenotype -> phenotype.fitness().doubleValue())
                .average()
                .orElse(0.0);

        double fitnessVariance = result.population().stream()
                .mapToDouble(phenotype -> Math.pow(phenotype.fitness().doubleValue() - averageFitness, 2))
                .average()
                .orElse(0.0);

        double fitnessStdDev = Math.sqrt(fitnessVariance);

        // Cálculo del porcentaje de mejora en el mejor fitness
        double improvementPercentageBest = (previousBestFitness > 0) 
            ? ((bestFitness - previousBestFitness) / previousBestFitness) * 100 
            : 0.0;

        // Cálculo del porcentaje de mejora en el promedio de fitness
        double improvementPercentageAverage = (previousAverageFitness > 0) 
            ? ((averageFitness - previousAverageFitness) / previousAverageFitness) * 100 
            : 0.0;

        // Actualizar los valores anteriores
        previousBestFitness = bestFitness;
        previousAverageFitness = averageFitness;

        // Mostrar en log
        LOGGER.info("--------------------------------------------------");
        LOGGER.info(String.format("Generación: %d", generation));
        LOGGER.info(String.format("Mejor fitness de la generación: %.4f", bestFitness));
        LOGGER.info(String.format("Goles a favor del mejor individuo: %d, Goles en contra: %d", golesFavor, golesContra));
        LOGGER.info(String.format("Promedio de fitness: %.4f, Desviación estándar: %.4f", averageFitness, fitnessStdDev));
        LOGGER.info(String.format("Porcentaje de mejora en promedio respecto a la generación anterior: %.2f%%", improvementPercentageAverage));
        LOGGER.info(String.format("Porcentaje de mejora en el mejor fitness respecto a la generación anterior: %.2f%%", improvementPercentageBest));
        LOGGER.info("--------------------------------------------------");

        // Guardar métricas en CSV
        saveMetricsToCSV(generation, bestFitness, averageFitness, fitnessStdDev, improvementPercentageAverage);
    }

    private static void saveMetricsToCSV(int generation, double bestFitness, double averageFitness, double fitnessStdDev, double improvementPercentageAverage) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("metrics.csv", true))) {
            if (generation == 1) {
                writer.write("Generación,Mejor Fitness,Promedio Fitness,Desviación Estándar,Porcentaje Mejora Promedio\n");
            }
            writer.write(String.format("%d,%.4f,%.4f,%.4f,%.2f\n", generation, bestFitness, averageFitness, fitnessStdDev, improvementPercentageAverage));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar métricas en CSV", e);
        }
    }
}
