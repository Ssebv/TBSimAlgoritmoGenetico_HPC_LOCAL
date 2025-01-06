/**
 * MainJenetics.java
 *
 * Programa principal que ejecuta un algoritmo genético utilizando la biblioteca Jenetics,
 * con el objetivo de optimizar los parámetros de un equipo de robots de fútbol.
 *
 * Este programa incluye las siguientes características:
 * - Configuración del algoritmo genético, incluyendo tamaño de la población, número de generaciones,
 *   tasas de mutación y cruce.
 * - Implementación de un mutador adaptativo que ajusta la tasa de mutación en función de la diversidad
 *   de la población.
 * - Implementación de un selector dinámico que alterna entre diferentes estrategias de selección
 *   (Ruleta y Torneo) basándose en la diversidad de la población.
 * - Soporte para la creación y carga de checkpoints para permitir la reanudación de ejecuciones previas.
 * - Registro detallado de la evolución del algoritmo, incluyendo métricas de fitness, diversidad,
 *   convergencia, y estadísticas de rendimiento del sistema.
 * - Generación de un archivo CSV con estadísticas de cada generación para análisis posterior.
 *
 * Dependencias:
 * - Jenetics: Biblioteca para algoritmos genéticos en Java.
 * - Java Util Logging: Para el registro de eventos y mensajes.
 * - Java IO: Para la manipulación de archivos y flujos de entrada/salida.
 * - Java Concurrency: Para la gestión de hilos y tareas concurrentes.
 *
 * Uso:
 * - El programa puede ser ejecutado tanto en entornos locales como en entornos HPC (High Performance Computing).
 * - Si se detecta un entorno HPC, se ajustan ciertos parámetros y se registra el ID del trabajo SLURM.
 * - El usuario puede optar por usar un checkpoint existente para reanudar una ejecución previa.
 *
 * Ejecución:
 * - El programa se inicia con la configuración del logger y la preparación del archivo CSV.
 * - Se crea una población inicial de genotipos y se evalúa su fitness utilizando una función de evaluación personalizada.
 * - El algoritmo genético se ejecuta durante un número definido de generaciones, ajustando dinámicamente las tasas de mutación y cruce.
 * - Se registran y guardan checkpoints periódicamente para permitir la reanudación en caso de interrupciones.
 * - Al finalizar, se registra el mejor genotipo y su fitness, y se cierra el archivo CSV.
 *
 */

import io.jenetics.*;
import io.jenetics.engine.*;
import io.jenetics.util.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.concurrent.atomic.AtomicReference;


public class MainJenetics {
    private static final Logger LOGGER = Logger.getLogger(MainJenetics.class.getName());
    private static final FuncionEvaluacionJenetics FUNCION_EVALUACION = new FuncionEvaluacionJenetics();

    // Configuración del algoritmo genético
    private static final int INITIAL_POPULATION_SIZE = 100;
    private static final int DEFAULT_GENERATIONS = 1000;
    private static final double ELITE_RATIO = 0.1;
    private static double mutationRate = 0.5;
    private static double crossoverRate = 0.8;

    private static PrintWriter csvWriter = null;
    private static final String CHECKPOINT_FILE = "checkpoint.ser";

    private static class AdaptiveMutator implements Alterer<DoubleGene, Double> {
        private final AtomicReference<Double> mutationRateRef;

        public AdaptiveMutator(double initialRate) {
            this.mutationRateRef = new AtomicReference<>(initialRate);
        }

        public void setMutationRate(double newRate) {
            mutationRateRef.set(newRate);
        }

        @Override
        public AltererResult<DoubleGene, Double> alter(Seq<Phenotype<DoubleGene, Double>> population, long generation) {
            double currentRate = mutationRateRef.get();
            List<Phenotype<DoubleGene, Double>> mutatedPopulation = new ArrayList<>();

            for (Phenotype<DoubleGene, Double> pt : population) {
                Genotype<DoubleGene> mutatedGenotype = mutateGenotype(pt.genotype(), currentRate);
                mutatedPopulation.add(Phenotype.of(mutatedGenotype, generation, pt.fitness()));
            }

            ISeq<Phenotype<DoubleGene, Double>> alteredSeq = ISeq.of(mutatedPopulation);
            return new AltererResult<>(alteredSeq, 0);
        }

        private Genotype<DoubleGene> mutateGenotype(Genotype<DoubleGene> genotype, double rate) {
            List<Chromosome<DoubleGene>> mutatedChromosomes = new ArrayList<>();
            for (Chromosome<DoubleGene> chromosome : genotype) {
                mutatedChromosomes.add(mutateChromosome(chromosome, rate));
            }
            return Genotype.of(mutatedChromosomes);
        }

        private Chromosome<DoubleGene> mutateChromosome(Chromosome<DoubleGene> chromosome, double rate) {
            List<DoubleGene> mutatedGenes = new ArrayList<>();
            DoubleRange range = DoubleRange.of(0, 10);
            java.util.Random random = new java.util.Random();

            for (DoubleGene gene : chromosome) {
                if (random.nextDouble() < rate) {
                    mutatedGenes.add(DoubleGene.of(random.nextDouble() * (range.max() - range.min()) + range.min(), range));
                } else {
                    mutatedGenes.add(gene);
                }
            }
            return DoubleChromosome.of(mutatedGenes);
        }
    }

    private static class DynamicSelector<G extends Gene<?, G>, C extends Number & Comparable<? super C>> implements Selector<G, C> {
        private final int maxGenerations;
        private int currentGeneration;
        private double threshold;

        public DynamicSelector(int maxGenerations) {
            this.maxGenerations = maxGenerations;
            this.currentGeneration = 0;
            updateThreshold();
        }

        public void setGeneration(int generation) {
            this.currentGeneration = generation;
            updateThreshold();
        }

        private void updateThreshold() {
            this.threshold = 0.9 - (0.6 * (currentGeneration / (double) maxGenerations));
        }

        @Override
        public ISeq<Phenotype<G, C>> select(Seq<Phenotype<G, C>> population, int count, Optimize opt) {
            double diversity = calcularDiversidad(population);
            int eliteCount = (int) (population.size() * ELITE_RATIO);
            ISeq<Phenotype<G, C>> elite = population.stream()
                    .sorted(opt.ascending())
                    .limit(eliteCount)
                    .collect(ISeq.toISeq());

            Selector<G, C> selector = (diversity > threshold) ? new RouletteWheelSelector<>() : new TournamentSelector<>(7);
            ISeq<Phenotype<G, C>> selected = selector.select(population, count - elite.size(), opt);
            return elite.append(selected);
        }

        private double calcularDiversidad(Seq<Phenotype<G, C>> population) {
            long uniqueGenotypes = population.stream()
                    .map(ph -> ph.genotype().toString().hashCode())
                    .distinct()
                    .count();
            return uniqueGenotypes / (double) population.size();
        }
    }

    private static final AdaptiveMutator ADAPTIVE_MUTATOR = new AdaptiveMutator(mutationRate);
    private static final DynamicSelector<DoubleGene, Double> DYNAMIC_SELECTOR = new DynamicSelector<>(DEFAULT_GENERATIONS);

    static {
        configurarLogger();
    }

    private static final long startTime = System.currentTimeMillis();

    public static void main(String[] args) {
        boolean isHPC = System.getenv("SLURM_JOB_ID") != null;
        final int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
    
        Factory<Genotype<DoubleGene>> genotypeFactory = Genotype.of(DoubleChromosome.of(0, 10, 50));
        prepararCSV(isHPC);
    
        try {
            List<Phenotype<DoubleGene, Double>> population = new ArrayList<>();
            int startGeneration = 0;
    
            if (preguntarUsoDeCheckpoint()) {
                startGeneration = cargarCheckpoint(population);
            }
    
            Engine<DoubleGene, Double> engine = Engine.builder(MainJenetics::evaluar, genotypeFactory)
                    .populationSize(INITIAL_POPULATION_SIZE)
                    .alterers(ADAPTIVE_MUTATOR, new SinglePointCrossover<>(crossoverRate))
                    .survivorsSelector(DYNAMIC_SELECTOR)
                    .executor(executorService)
                    .build();
    
            Phenotype<DoubleGene, Double> bestPhenotype = engine.stream()
                    .peek(result -> {
                        DYNAMIC_SELECTOR.setGeneration((int) result.generation());
                        ajustarTasas(result);
                        logGeneration(result);
                        guardarCheckpoint(result);
                    })
                    .limit(DEFAULT_GENERATIONS - startGeneration)
                    .collect(EvolutionResult.toBestPhenotype());
    
            LOGGER.info("Optimización completada.");
            LOGGER.info("Mejor genotipo: " + bestPhenotype.genotype());
            LOGGER.info("Mejor aptitud: " + bestPhenotype.fitness());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en la ejecución del AG", e);
        } finally {
            cerrarCSV();
            executorService.shutdown();
        }
    }

    private static double evaluar(Genotype<DoubleGene> genotype) {
        try {
            return FUNCION_EVALUACION.evaluar(genotype);
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error durante la evaluación del genotipo", e);
            return 0.0;
        }
    }

    private static void guardarCheckpoint(EvolutionResult<DoubleGene, Double> result) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CHECKPOINT_FILE))) {
            Map<String, Object> config = new HashMap<>();
            config.put("population.size", INITIAL_POPULATION_SIZE);
            config.put("generations", DEFAULT_GENERATIONS);
            config.put("mutation.rate", mutationRate);
            config.put("crossover.rate", crossoverRate);
    
            CheckpointData checkpointData = new CheckpointData(result.population().asList(), (int) result.generation(), config);
            oos.writeObject(checkpointData);
            // LOGGER.info("Checkpoint guardado: Generación " + result.generation());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error guardando el checkpoint", e);
        }
    }
    
    private static int cargarCheckpoint(List<Phenotype<DoubleGene, Double>> population) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(CHECKPOINT_FILE))) {
            CheckpointData checkpointData = (CheckpointData) ois.readObject();
            population.addAll(checkpointData.getPopulation());
            Map<String, Object> config = checkpointData.getConfig();
    
            // Restaura parámetros configurables
            mutationRate = (double) config.getOrDefault("mutation.rate", mutationRate);
            crossoverRate = (double) config.getOrDefault("crossover.rate", crossoverRate);
    
            int generation = checkpointData.getGeneration();
            LOGGER.info("Checkpoint cargado: Generación " + generation);
            return generation;
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.INFO, "No se encontró un checkpoint válido. Comenzando desde la generación 0.");
            return 0;
        }
    }
    

    private static void prepararCSV(boolean isHPC) {
        try {
            if (isHPC) {
                LOGGER.info("Ejecución detectada en un entorno HPC. SLURM_JOB_ID: " + System.getenv("SLURM_JOB_ID"));
            } else {
                LOGGER.info("Ejecución detectada en un entorno local.");
            }
            
            String filename = isHPC ? "hpc_stats.csv" : "local_stats.csv";
            csvWriter = new PrintWriter(new FileWriter(filename));
            csvWriter.printf("Generación,Mejor Fitness,Promedio Fitness,Diversidad,Convergencia,Goles Favor Mejor,Goles Contra Mejor,Goles Favor Total,Goles Contra Total,CPU,Memoria,Tiempo,OS,Arquitectura,Versión OS%n");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No se pudo crear el archivo CSV", e);
        }
    }

    private static void cerrarCSV() {
        if (csvWriter != null) {
            csvWriter.close();
        }
    }

    private static void configurarLogger() {
        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter() {
            @Override
            public synchronized String format(LogRecord record) {
                return String.format(
                    "%1$tF %1$tT [%2$s] %3$s: %4$s %n",
                    new Date(record.getMillis()),
                    record.getLoggerName(),
                    record.getLevel().getLocalizedName(),
                    record.getMessage()
                );
            }
        });
        rootLogger.addHandler(consoleHandler);
        rootLogger.setLevel(Level.INFO);
    }
    private static void ajustarTasas(EvolutionResult<DoubleGene, Double> result) {
        double diversity = calcularDiversidad(result.population());
        if (diversity > 0.7) {
            mutationRate = Math.min(mutationRate + 0.02, 0.6);
            crossoverRate = Math.max(crossoverRate - 0.01, 0.5);
        } else {
            mutationRate = Math.max(mutationRate - 0.02, 0.3);
            crossoverRate = Math.min(crossoverRate + 0.01, 0.85);
        }
        ADAPTIVE_MUTATOR.setMutationRate(mutationRate);
        LOGGER.info(String.format("Tasas ajustadas: Mutación=%.2f, Cruce=%.2f", mutationRate, crossoverRate));
    }
    
    private static void logGeneration(EvolutionResult<DoubleGene, Double> result) {
        int generation = (int) result.generation();
    
        List<Phenotype<DoubleGene, Double>> population = result.population().stream()
                .filter(ph -> ph.fitness() > 0)
                .toList();
    
        if (population.isEmpty()) {
            LOGGER.warning("Generación " + generation + " descartada por falta de individuos válidos");
            return;
        }
    
        Phenotype<DoubleGene, Double> bestPhenotype = population.stream()
                .max(Comparator.comparingDouble(Phenotype::fitness))
                .orElse(null);
    
        double bestFitness = bestPhenotype != null ? bestPhenotype.fitness() : 0;
        double avgFitness = population.stream()
                .mapToDouble(Phenotype::fitness)
                .average()
                .orElse(0.0);
    
        double diversity = calcularDiversidad(result.population());
        double convergence = 1 - diversity;
    
        int golesFavorTotal = population.stream()
                .mapToInt(FUNCION_EVALUACION::getGolesFavorForPhenotype)
                .sum();
        int golesContraTotal = population.stream()
                .mapToInt(FUNCION_EVALUACION::getGolesContraForPhenotype)
                .sum();
    
        int golesFavorMejor = FUNCION_EVALUACION.getGolesFavorForPhenotype(bestPhenotype);
        int golesContraMejor = FUNCION_EVALUACION.getGolesContraForPhenotype(bestPhenotype);
    
        double elapsedTimeSec = (System.currentTimeMillis() - startTime) / 1000.0;
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String osVersion = System.getProperty("os.version");
        double memUsedMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024.0);
    
        LOGGER.info(String.format(
                "Gen %d -> Mejor Fitness=%.4f, Prom Fitness=%.4f, Diversidad=%.4f, Convergencia=%.4f, Goles Favor Mejor=%d, Goles Contra Mejor=%d, Goles Favor Total=%d, Goles Contra Total=%d, CPU=%.2f%%, Memoria=%.0fMB, Tiempo=%.2fseg, OS=%s (%s, %s)",
                generation, bestFitness, avgFitness, diversity, convergence,
                golesFavorMejor, golesContraMejor, golesFavorTotal, golesContraTotal,
                CpuUsageMonitor.getProcessCpuLoad() * 100, memUsedMB, elapsedTimeSec,
                osName, osArch, osVersion
        ));
    
        // Guardar en el archivo CSV
        if (csvWriter != null) {
            try {
                csvWriter.printf("%d,%.4f,%.4f,%.4f,%.4f,%d,%d,%d,%d,%.2f,%.0f,%.2f,%s,%s,%s%n",
                        generation, bestFitness, avgFitness, diversity, convergence,
                        golesFavorMejor, golesContraMejor, golesFavorTotal, golesContraTotal,
                        CpuUsageMonitor.getProcessCpuLoad() * 100, memUsedMB, elapsedTimeSec,
                        osName, osArch, osVersion);
                csvWriter.flush(); // Asegurar que los datos se escriban
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error escribiendo en archivo CSV", e);
            }
        }
    
        guardarCheckpoint(result);
    }
    
    
    private static double calcularDiversidad(Seq<Phenotype<DoubleGene, Double>> population) {
        Set<String> uniqueGenotypes = new HashSet<>();
        for (Phenotype<DoubleGene, Double> ph : population) {
            uniqueGenotypes.add(ph.genotype().toString());
        }
        return uniqueGenotypes.size() / (double) population.size();
    }

    private static boolean preguntarUsoDeCheckpoint() {
        File checkpointFile = new File(CHECKPOINT_FILE);
    
        try (Scanner scanner = new Scanner(System.in)) {
            if (checkpointFile.exists()) {
                while (true) {
                    System.out.println("Se encontró un checkpoint existente. ¿Deseas usarlo? (s/n): ");
                    String respuesta = scanner.nextLine().trim().toLowerCase();
                    if (respuesta.equals("s") || respuesta.equals("si")) {
                        LOGGER.info("Usando el checkpoint existente.");
                        return true;
                    } else if (respuesta.equals("n") || respuesta.equals("no")) {
                        LOGGER.info("Eliminando el checkpoint existente...");
                        checkpointFile.delete();
                        System.out.println("Checkpoint eliminado correctamente.");
                        return false;
                    } else {
                        System.out.println("Por favor, responde con 's' (sí) o 'n' (no).");
                    }
                }
            }
            return false;
        }
    }  
}
