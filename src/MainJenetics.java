import io.jenetics.*;
import io.jenetics.engine.*;
import io.jenetics.util.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.stream.Collectors;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class MainJenetics {
    private static final Logger LOGGER = configurarLogger();
    private static final boolean ENABLE_COLORS = true;
    private static final FuncionEvaluacionJenetics FUNCION_EVALUACION = new FuncionEvaluacionJenetics();

    private static final String CHECKPOINT_FILE = "checkpoint.ser";
    private static final long startTime = System.currentTimeMillis();

    private static double previousBestFitness = Double.NEGATIVE_INFINITY;
    private static EvolutionResult<DoubleGene, Double> lastResult = null;

    private static Engine<DoubleGene, Double> engine;
    private static PrintWriter csvWriter;
    private static volatile boolean isRunning = true;

    private static final boolean IS_HPC = detectarEntornoHPC();

    private static long currentGeneration = 0;
    private static final int INITIAL_POPULATION_SIZE = IS_HPC ? 50 : 30;
    private static final int DEFAULT_GENERATIONS = IS_HPC ? 2000 : 100;
    private static double mutationRate = IS_HPC ? 0.4 : 0.2;
    private static double crossoverRate = IS_HPC ? 0.85 : 0.9;

    private static int generacionesSinMejora = 0;
    private static final int MAX_GENERACIONES_SIN_MEJORA = 5; // Número de generaciones para detectar estancamiento
    private static final double THRESHOLD_MEJORA = 0.01; // Umbral para considerar una mejora significativa 0.01 es
                                                         // cuando cambia el 1% del fitness un ejemplo seria de 100 a
                                                         // 101 no es significativo pero de 100 a 110 si lo es
    private static final Queue<Double> historialFitness = new LinkedList<>();

    public static void main(String[] args) {
        configurarInterrupcion();
        prepararArchivos();

        if (IS_HPC) {
            logHPCDetails();
        } else {
            logLocalDetails();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            Factory<Genotype<DoubleGene>> genotypeFactory = Genotype.of(DoubleChromosome.of(1, 5, 60));
            prepararCSV(IS_HPC);

            boolean useCheckpoint = CheckpointManager.preguntarUsoDeCheckpoint(CHECKPOINT_FILE);
            CheckpointManager.CheckpointData checkpointData = useCheckpoint
                    ? CheckpointManager.cargarCheckpoint(CHECKPOINT_FILE)
                    : null;
            int startGeneration = checkpointData != null ? checkpointData.getGeneration() : 0;

            engine = crearMotorGenetico(genotypeFactory, executorService);
            ejecutarMotorGenetico(startGeneration);

            logResumenFinal();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado durante la ejecución", e);
            if (lastResult != null) {
                CheckpointManager.guardarCheckpoint(lastResult, CHECKPOINT_FILE);
                logCheckpointGuardado(CHECKPOINT_FILE); // Log del checkpoint
            }
        } finally {
            cerrarCSV();
            executorService.shutdownNow();
        }
    }

    private static String formatWithColor(String message, String color) {
        return ENABLE_COLORS ? color + message + "\u001B[0m" : message;
    }

    private static Engine<DoubleGene, Double> crearMotorGenetico(
            Factory<Genotype<DoubleGene>> genotypeFactory, ExecutorService executorService) {
        int populationSize = determinarTamañoPoblación();
        double adjustedMutationRate = ajustarTasaMutacion(calcularDiversidadEfectiva(ISeq.empty()), mutationRate); // Inicial
                                                                                                                   // con
                                                                                                                   // diversidad
                                                                                                                   // 0

        return Engine.builder(MainJenetics::evaluar, genotypeFactory)
                .populationSize(populationSize)
                .alterers(
                        new Mutator<>(adjustedMutationRate),
                        new SinglePointCrossover<>(crossoverRate))
                .survivorsSelector(new UniqueEliteSelector<>(3))
                .offspringSelector(ajustarSelector(1.0))
                .executor(executorService)
                .build();
    }

    private static int determinarTamañoPoblación() {
        try {
            return IS_HPC ? Integer.parseInt(obtenerVariableEntorno("SLURM_CPUS_ON_NODE", "5")) * 10
                    : INITIAL_POPULATION_SIZE;
        } catch (NumberFormatException e) {
            LOGGER.warning(
                    "Valor no válido para SLURM_CPUS_ON_NODE. Usando valor predeterminado: " + INITIAL_POPULATION_SIZE);
            return INITIAL_POPULATION_SIZE;
        }
    } 

    private static void ejecutarMotorGenetico(int startGeneration) {
        engine.stream()
                .limit(DEFAULT_GENERATIONS - startGeneration)
                .takeWhile(r -> isRunning) // Cambiar el nombre del parámetro puede hacer más legible
                .peek(MainJenetics::procesarResultadoGeneracion)
                .collect(EvolutionResult.toBestPhenotype());

        LOGGER.info(isRunning ? "Optimización completada." : "Optimización interrumpida.");
    }

    private static double evaluar(Genotype<DoubleGene> genotype) {
        FuncionEvaluacionJenetics.ResultadoPartido resultado = FUNCION_EVALUACION.evaluarResultado(genotype,
                (int) currentGeneration);

        if (resultado.getFitness() == Double.NEGATIVE_INFINITY) {
            LOGGER.warning("Genotipo evaluado con fitness inválido. Saltando...");
            return 0.0; // O un valor predeterminado
        }

        logResultadoPartido(resultado);
        return resultado.getFitness();
    }

    private static void procesarResultadoGeneracion(EvolutionResult<DoubleGene, Double> result) {
        try {
            if (result == null || result.population().isEmpty()) {
                LOGGER.warning("Resultado de la generación nulo o vacío. Saltando procesamiento.");
                return;
            }
    
            double mejorFitness = result.bestFitness();
            double diversidad = calcularDiversidadEfectiva(result.population());
    
            LOGGER.info(String.format("[GENÉTICO] Generación %d: Mejor Fitness: %.2f, Diversidad: %.2f",
                    result.generation(), mejorFitness, diversidad));
    
            // Obtener y registrar los élites seleccionados utilizando UniqueEliteSelector
            UniqueEliteSelector<DoubleGene, Double> selector = new UniqueEliteSelector<>(3); // Por ejemplo, top 3 élites
            ISeq<Phenotype<DoubleGene, Double>> elites = selector.select(result.population(), 3, Optimize.MAXIMUM);
            List<Double> eliteFitnesses = elites.stream()
                    .map(Phenotype::fitness)
                    .collect(Collectors.toList());
            logElitesSeleccionados(eliteFitnesses);
    
            manejarEstancamiento(result); // Detectar y manejar estancamiento
    
            logGeneracion(result); // Registrar información de la generación
            CheckpointManager.guardarCheckpoint(result, CHECKPOINT_FILE);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error procesando la generación: " + e.getMessage(), e);
        }
    }
    

    private static void manejarEstancamiento(EvolutionResult<DoubleGene, Double> result) {
        if (isStagnant(result)) {
            LOGGER.warning(String.format(
                    "[ESTANCAMIENTO] Detectado en la generación %d. Mejor Fitness: %.2f. Reintroduciendo diversidad...",
                    result.generation(), previousBestFitness));

            // Reintroducir diversidad
            ISeq<Phenotype<DoubleGene, Double>> nuevaPoblacion = reintroducirDiversidad(result.population());
            lastResult = EvolutionResult.of(
                    Optimize.MAXIMUM,
                    nuevaPoblacion,
                    result.generation(),
                    EvolutionDurations.ZERO,
                    nuevaPoblacion.size(),
                    nuevaPoblacion.size(),
                    0);

            // Configurar el motor genético con la nueva población
            engine = Engine.builder(MainJenetics::evaluar, Genotype.of(DoubleChromosome.of(1, 5, 54)))
                    .populationSize((int) nuevaPoblacion.size())
                    .alterers(
                            new Mutator<>(
                                    ajustarTasaMutacion(calcularDiversidadEfectiva(nuevaPoblacion), mutationRate)),
                            new SinglePointCrossover<>(crossoverRate))
                    .survivorsSelector(new TournamentSelector<>(5))
                    .offspringSelector(new StochasticUniversalSelector<>())
                    .build();

            LOGGER.info("[ESTANCAMIENTO] Diversidad reintroducida. Continuando evolución...");
        } else {
            lastResult = result;
        }
    }

    private static boolean isStagnant(EvolutionResult<DoubleGene, Double> result) {
        double currentBestFitness = result.bestFitness();

        // Verificar si el fitness actual es significativamente mejor
        if (Math.abs(currentBestFitness - previousBestFitness) > THRESHOLD_MEJORA) {
            generacionesSinMejora = 0; // Reiniciar el contador si hay mejora
            previousBestFitness = currentBestFitness;
        } else {
            generacionesSinMejora++;
        }

        // Registrar el fitness en el historial
        historialFitness.add(currentBestFitness);
        if (historialFitness.size() > MAX_GENERACIONES_SIN_MEJORA) {
            historialFitness.poll(); // Mantener solo las últimas 5 generaciones
        }

        // Detectar estancamiento si no hay mejora en las últimas 5 generaciones
        return generacionesSinMejora >= MAX_GENERACIONES_SIN_MEJORA;
    }

    private static void configurarInterrupcion() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Interrupción detectada. Guardando estado...");
            logUsoSistema();

            if (lastResult != null) {
                CheckpointManager.guardarCheckpoint(lastResult, CHECKPOINT_FILE);
                LOGGER.info("Checkpoint guardado correctamente.");
            }
            cerrarCSV();
        }));
    }

    private static void prepararArchivos() {
        try {
            // Crear archivo para almacenar métricas detalladas si no existe
            File archivoMetricas = new File("metricas_ag.csv");
            if (!archivoMetricas.exists()) {
                try (FileWriter fw = new FileWriter(archivoMetricas)) {
                    fw.write("Generación,Mejor Fitness,Fitness Promedio,Diversidad,Peor Fitness,Mejor Cromosoma\n");
                }
            }
        } catch (IOException e) {
            LOGGER.warning("Error al preparar archivos iniciales: " + e.getMessage());
        }
    }

    private static void prepararCSV(boolean isHPC) {
        try {
            String nombreArchivo = isHPC ? "hpc_stats.csv" : "local_stats.csv";
            csvWriter = new PrintWriter(new FileWriter(nombreArchivo));

            // Encabezados del archivo CSV
            csvWriter.println(
                    "Generación,Mejor Fitness,Fitness Promedio,Diversidad,Peor Fitness,CPU (%),Memoria (%),Tiempo (s),Goles Favor,Goles Contra");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No se pudo crear el archivo CSV: " + e.getMessage(), e);
        }
    }

    private static void cerrarCSV() {
        if (csvWriter != null) {
            csvWriter.flush();
            csvWriter.close();
        }
    }

    private static void logUsoSistema() {
        LOGGER.info(String.format("CPU: %.2f%%, Memoria: %.2f%%",
                getSystemCpuLoad() * 100, getSystemMemoryLoad() * 100));
    }

    private static double getSystemCpuLoad() {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        return osBean.getCpuLoad();
    }

    private static double getSystemMemoryLoad() {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        long totalMemory = osBean.getTotalMemorySize();
        long freeMemory = osBean.getFreeMemorySize();
        return 1.0 - ((double) freeMemory / totalMemory);
    }

    private static Selector<DoubleGene, Double> ajustarSelector(double diversity) {
        return diversity > 0.8
                ? new TournamentSelector<>(5)
                : new StochasticUniversalSelector<>();
    }

    private static double ajustarTasaMutacion(double diversidad, double tasaBase) {
        return diversidad < 0.5 ? tasaBase * 1.5 : tasaBase * 0.8;
    }

    private static double calcularDiversidadEfectiva(ISeq<Phenotype<DoubleGene, Double>> population) {
        double totalDistance = 0;
        int comparisons = 0;

        List<Genotype<DoubleGene>> genotypes = population.stream()
                .map(Phenotype::genotype)
                .toList();

        for (int i = 0; i < genotypes.size(); i++) {
            for (int j = i + 1; j < genotypes.size(); j++) {
                totalDistance += calcularDistancia(genotypes.get(i), genotypes.get(j));
                comparisons++;
            }
        }

        return comparisons > 0 ? totalDistance / comparisons : 0;
    }

    private static double calcularDistancia(Genotype<DoubleGene> g1, Genotype<DoubleGene> g2) {
        double distance = 0;
        DoubleChromosome c1 = (DoubleChromosome) g1.chromosome();
        DoubleChromosome c2 = (DoubleChromosome) g2.chromosome();

        for (int i = 0; i < c1.length(); i++) {
            double maxRange = c1.get(i).max();
            double minRange = c1.get(i).min();
            double range = maxRange - minRange;

            distance += Math.abs(c1.get(i).doubleValue() - c2.get(i).doubleValue()) / range;
        }
        return distance;
    }

    private static Logger configurarLogger() {
        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        try {
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new java.util.logging.Formatter() {
                @Override
                public String format(LogRecord record) {
                    String color = ENABLE_COLORS
                            ? (record.getLevel() == Level.SEVERE ? "\u001B[31m" // Rojo
                                    : record.getLevel() == Level.WARNING ? "\u001B[33m" // Amarillo
                                            : "\u001B[32m") // Verde
                            : "";
                    String reset = ENABLE_COLORS ? "\u001B[0m" : "";
                    return String.format("%s[%s] %s%s%n", color, record.getLevel(), record.getMessage(), reset);
                }
            });

            rootLogger.addHandler(consoleHandler);
            rootLogger.setLevel(Level.INFO);

            return rootLogger;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rootLogger;
    }

    private static void logGeneracion(EvolutionResult<DoubleGene, Double> result) {
        try {
            double avgFitness = calcularPromedioFitness(result.population());
            double diversity = calcularDiversidadEfectiva(result.population());
            double worstFitness = calcularPeorFitness(result.population());
            long elapsedTime = getElapsedTimeMillis(startTime);

            String barraSeparadora = formatWithColor("===============================================", "\u001B[34m"); // Azul

            LOGGER.info("\n" + barraSeparadora);
            LOGGER.info(formatWithColor(String.format("[GENÉTICO] GENERACIÓN %d:", result.generation()), "\u001B[34m")); // Azul
            LOGGER.info(barraSeparadora);
            LOGGER.info(formatWithColor(String.format("[GENÉTICO]    Mejor Fitness:       %.2f", result.bestFitness()),
                    "\u001B[32m")); // Verde
            LOGGER.info(formatWithColor(String.format("[GENÉTICO]    Promedio Fitness:    %.2f", avgFitness),
                    "\u001B[32m")); // Verde
            LOGGER.info(
                    formatWithColor(String.format("[GENÉTICO]    Diversidad:          %.2f", diversity), "\u001B[32m")); // Verde
            LOGGER.info(formatWithColor(String.format("[GENÉTICO]    Peor Fitness:        %.2f", worstFitness),
                    "\u001B[31m")); // Rojo
            LOGGER.info(formatWithColor(
                    String.format("[GENÉTICO]    Tiempo Transcurrido: %.2f s", elapsedTime / 1000.0), "\u001B[33m")); // Amarillo
            LOGGER.info(barraSeparadora);

            escribirCSV(
                    (int) result.generation(),
                    FUNCION_EVALUACION.evaluarResultado(result.bestPhenotype().genotype(), (int) result.generation()),
                    avgFitness,
                    diversity,
                    worstFitness,
                    getSystemCpuLoad(),
                    getSystemMemoryLoad(),
                    elapsedTime);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al loguear la generación: " + e.getMessage(), e);
        }
    }

    private static void logElitesSeleccionados(List<Double> elites) {
        if (elites.isEmpty()) {
            LOGGER.info(formatWithColor("[ELITE] No se encontraron élites en esta generación.", "\u001B[36m")); // Cyan
            return;
        }
        LOGGER.info(formatWithColor("[ELITE] Selección de élites:", "\u001B[36m")); // Cyan
        elites.forEach(
                elite -> LOGGER.info(formatWithColor(String.format("[ELITE]    Fitness: %.2f", elite), "\u001B[33m"))); // Amarillo
    }

    private static void logResultadoPartido(FuncionEvaluacionJenetics.ResultadoPartido resultado) {
        String logEntry = formatWithColor(String.format(
                "[RESULTADO PARTIDO] Goles Favor: %d, Goles Contra: %d, Fitness: %.2f",
                resultado.getGolesFavor(),
                resultado.getGolesContra(),
                resultado.getFitness()), "\u001B[35m"); // Magenta
        LOGGER.info(logEntry);
    }

    private static void logCheckpointGuardado(String filePath) {
        LOGGER.info(String.format("[CHECKPOINT] Guardado correctamente en %s", filePath));
    }

    private static void logResumenFinal() {
        LOGGER.info("\n================== RESUMEN FINAL ==================");
        LOGGER.info(String.format("[INFO] Generaciones ejecutadas: %d", currentGeneration));
        LOGGER.info(String.format("[INFO] Mejor Fitness Alcanzado: %.2f", previousBestFitness));
        LOGGER.info("==================================================");
    }

    private static double calcularPromedioFitness(ISeq<Phenotype<DoubleGene, Double>> population) {
        return population.stream()
                .mapToDouble(Phenotype::fitness)
                .average()
                .orElse(0.0);
    }

    private static double calcularPeorFitness(ISeq<Phenotype<DoubleGene, Double>> population) {
        return population.stream()
                .mapToDouble(Phenotype::fitness)
                .min()
                .orElse(0.0);
    }

    private static long getElapsedTimeMillis(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    private static ISeq<Phenotype<DoubleGene, Double>> reintroducirDiversidad(
            ISeq<Phenotype<DoubleGene, Double>> population) {
        List<Phenotype<DoubleGene, Double>> nuevaPoblacion = new ArrayList<>(population.asList());
        Genotype<DoubleGene> mejorGenotipo = population.stream()
                .max(Comparator.comparingDouble(Phenotype::fitness))
                .orElseThrow()
                .genotype();

        // Reemplazar un porcentaje de la población con nuevos individuos aleatorios
        int reemplazo = (int) (nuevaPoblacion.size() * 0.3); // Reemplaza el 30% de la población
        for (int i = 0; i < reemplazo; i++) {
            Genotype<DoubleGene> nuevoGenotipo = Genotype.of(DoubleChromosome.of(1, 5, 54));
            Phenotype<DoubleGene, Double> nuevoIndividuo = Phenotype.of(nuevoGenotipo, currentGeneration);
            nuevaPoblacion.set(i, nuevoIndividuo);
        }

        // Mantener el mejor individuo anterior
        nuevaPoblacion.set(0, Phenotype.of(mejorGenotipo, currentGeneration));
        return ISeq.of(nuevaPoblacion);
    }

    private static String obtenerVariableEntorno(String variable, String valorPorDefecto) {
        String valor = System.getenv(variable);
        return (valor != null) ? valor : valorPorDefecto;
    }

    private static void logHPCDetails() {
        try {
            LOGGER.info("Configuración del entorno HPC detectado (SLURM):");
            LOGGER.info("Job ID: " + obtenerVariableEntorno("SLURM_JOB_ID", "N/A"));
            LOGGER.info("Node List: " + obtenerVariableEntorno("SLURM_NODELIST", "N/A"));
            LOGGER.info("CPUs por Nodo: " + obtenerVariableEntorno("SLURM_CPUS_ON_NODE", "N/A"));
            LOGGER.info("Tareas Totales: " + obtenerVariableEntorno("SLURM_NTASKS", "N/A"));
            LOGGER.info("CPUs por Tarea: " + obtenerVariableEntorno("SLURM_CPUS_PER_TASK", "N/A"));
            LOGGER.info("Memoria por Nodo: " + obtenerVariableEntorno("SLURM_MEM_PER_NODE", "N/A"));
            LOGGER.info("Partición: " + obtenerVariableEntorno("SLURM_JOB_PARTITION", "N/A"));
        } catch (Exception e) {
            LOGGER.warning("Error al obtener detalles del entorno HPC: " + e.getMessage());
        }
    }

    private static void logLocalDetails() {
        LOGGER.info("Sistema Operativo: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        LOGGER.info("Versión de Java: " + System.getProperty("java.version"));
        LOGGER.info("CPUs disponibles: " + Runtime.getRuntime().availableProcessors());
        LOGGER.info("Memoria total disponible: " + (Runtime.getRuntime().totalMemory() / (1024 * 1024)) + " MB");
    }

    private static void escribirCSV(int generacion, FuncionEvaluacionJenetics.ResultadoPartido mejorResultado,
            double avgFitness, double diversity, double worstFitness, double cpuLoad,
            double memoryLoad, long elapsedTime) {
        try {
            if (csvWriter == null) {
                LOGGER.warning("[ADVERTENCIA] CSV Writer no inicializado. No se escribirán estadísticas.");
                return;
            }

            csvWriter.printf("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d\n",
                    generacion,
                    mejorResultado != null ? mejorResultado.getFitness() : 0.0,
                    avgFitness,
                    diversity,
                    worstFitness,
                    cpuLoad * 100,
                    memoryLoad * 100,
                    elapsedTime / 1000.0,
                    mejorResultado != null ? mejorResultado.getGolesFavor() : 0,
                    mejorResultado != null ? mejorResultado.getGolesContra() : 0);

            csvWriter.flush();

            LOGGER.info("Datos de la generación " + generacion + " escritos exitosamente al CSV.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al escribir datos en el archivo CSV: " + e.getMessage(), e);
        }
    }

    private static boolean detectarEntornoHPC() {
        return System.getenv("SLURM_JOB_ID") != null;
    }
}
