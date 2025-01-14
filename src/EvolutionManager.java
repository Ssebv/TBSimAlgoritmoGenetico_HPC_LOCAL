import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Mutator;
import io.jenetics.Optimize;
import io.jenetics.Phenotype;
import io.jenetics.SinglePointCrossover;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.Factory;
import io.jenetics.util.ISeq;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
 * EvolutionManager que:
 * 1) Acepta un CheckpointData externo (si deseas retomar una población guardada).
 * 2) No redefine el Engine durante estancamiento; en su lugar, almacena la nueva
 *    población en 'nextPopulation' para una segunda corrida.
 * 3) Captura OutOfMemoryError y guarda el checkpoint de emergencia usando el
 *    último EvolutionResult disponible.
 * 4) Evita borrar/crear CSV o redefinir Engine en mitad de la primera corrida.
 * 5) Respeta config.ENABLE_COLORS para imprimir colores (si el entorno lo soporta).
 */
public class EvolutionManager {
    private final CSVManager csvManager;
    private final String checkpointFile;
    private final FuncionEvaluacionJenetics fitnessEvaluator;
    private final LogManager logManager;
    private final Configuracion config;

    // Motor genético Jenetics
    private Engine<DoubleGene, Double> engine;
    private long startTime;
    private int currentGeneration;

    // Si el usuario cargó un checkpoint externo, se guarda aquí
    private CheckpointData checkpointData;

    // Guarda el último EvolutionResult para emergencias (OOM, etc.)
    private EvolutionResult<DoubleGene, Double> lastEvolutionResult;

    // Para detectar estancamiento
    private static double previousBestFitness = Double.NEGATIVE_INFINITY;

    // Población diversificada que se aplicará en una segunda corrida si handleStagnation() la setea
    private ISeq<Phenotype<DoubleGene, Double>> nextPopulation = null;

    public EvolutionManager(CSVManager csvManager,
                            String checkpointFile,
                            Logger logger,
                            Configuracion config) {
        this.csvManager = csvManager;
        this.checkpointFile = checkpointFile;
        this.fitnessEvaluator = FuncionEvaluacionJenetics.getInstance();
        // **Cambio esencial**: usar config.ENABLE_COLORS para que LogManager sepa si colorear.
        this.logManager = new LogManager(logger, config.ENABLE_COLORS);
        this.config = config;
    }

    /**
     * Permite inyectar un CheckpointData si se desea retomar una población guardada.
     */
    public void setCheckpointData(CheckpointData checkpointData) {
        this.checkpointData = checkpointData;
    }

    /**
     * Ejecuta la evolución genética:
     * - Primera corrida normal (con checkpoint si existe).
     * - Si 'nextPopulation' se setea en handleStagnation(), corre una segunda corrida.
     * - Captura OutOfMemoryError para guardar checkpoint de emergencia.
     */
    public void runGeneticEngine() {
        startTime = System.currentTimeMillis();

        // Pool de hilos
        ExecutorService executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );

        try {
            // Fábrica de Genotypes
            Factory<Genotype<DoubleGene>> genotypeFactory =
                    Genotype.of(DoubleChromosome.of(1, 5, 60));

            // Crear motor base
            engine = Engine.builder(this::evaluate, genotypeFactory)
                    .executor(executorService)
                    .alterers(
                            new Mutator<>(config.MUTATION_RATE),
                            new SinglePointCrossover<>(config.CROSSOVER_RATE)
                    )
                    .populationSize(config.INITIAL_POPULATION_SIZE)
                    .build();

            // Si tenemos checkpoint, reconstruimos la población
            if (checkpointData != null) {
                logManager.logInfo("Cargando población desde checkpoint...");
                final int checkpointGen = checkpointData.getGeneration();

                List<Phenotype<DoubleGene, Double>> population =
                        checkpointData.getGenotypes().stream()
                                .map(gt -> Phenotype.<DoubleGene, Double>of(gt, checkpointGen))
                                .collect(Collectors.toList());

                // Creamos un Engine con la población del checkpoint
                engine = Engine.builder(this::evaluate, genotypeFactory)
                        .executor(executorService)
                        .alterers(
                                new Mutator<>(config.MUTATION_RATE),
                                new SinglePointCrossover<>(config.CROSSOVER_RATE)
                        )
                        .populationSize(population.size())
                        .build();
            }

            // PRIMERA corrida
            engine.stream()
                    .limit(config.DEFAULT_GENERATIONS)
                    .peek(this::processGeneration)
                    .collect(EvolutionResult.toBestPhenotype());

            logManager.logInfo("Optimización (primera corrida) completada.");

            // Comprobamos si handleStagnation() definió nextPopulation
            if (nextPopulation != null) {
                logManager.logInfo("Aplicando la población diversificada en una segunda corrida...");

                // Creamos un nuevo Engine con nextPopulation
                engine = Engine.builder(this::evaluate, genotypeFactory)
                        .executor(executorService)
                        .alterers(
                                new Mutator<>(config.MUTATION_RATE),
                                new SinglePointCrossover<>(config.CROSSOVER_RATE)
                        )
                        .populationSize(nextPopulation.size())
                        .build();

                // SEGUNDA corrida, ahora con la población diversificada
                engine.stream()
                        .limit(config.DEFAULT_GENERATIONS)
                        .peek(this::processGeneration)
                        .collect(EvolutionResult.toBestPhenotype());

                logManager.logInfo("Optimización (segunda corrida con diversidad) completada.");
            }

            // Log final
            logManager.logResumenFinal(currentGeneration, previousBestFitness);

        } catch (OutOfMemoryError oom) {
            // Capturamos OOM. Guardamos checkpoint con el último EvolutionResult
            System.err.println("[FATAL] Out of Memory en gen=" + currentGeneration + ": " + oom.getMessage());
            if (lastEvolutionResult != null) {
                CheckpointManager.guardarCheckpoint(lastEvolutionResult, checkpointFile);
            }
            System.exit(1);

        } catch (Exception e) {
            logManager.logError("Error durante la ejecución: " + e.getMessage());
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * Evalúa cada individuo de la población.
     */
    private double evaluate(Genotype<DoubleGene> genotype) {
        var resultado = fitnessEvaluator.evaluarResultado(genotype, currentGeneration);

        if (resultado.getFitness() == Double.NEGATIVE_INFINITY) {
            logManager.logWarning("Genotipo con fitness inválido. Saltando...");
            return 0.0;
        }
        logManager.logResultadoPartido(resultado);
        return resultado.getFitness();
    }

    /**
     * Se llama una vez por generación, escribe datos en CSV, guarda checkpoints, etc.
     */
    private void processGeneration(EvolutionResult<DoubleGene, Double> result) {
        if (result == null || result.population().isEmpty()) {
            logManager.logWarning("Resultado de generación vacío. Omitiendo...");
            return;
        }

        // Guardamos el resultado actual por si ocurre un OOM
        lastEvolutionResult = result;

        currentGeneration = (int) result.generation();
        double mejorFitness = result.bestFitness();
        double peorFitness = calculateWorstFitness(result.population());
        double diversidad = calculateDiversity(result.population());
        double avgFitness = calculateAverageFitness(result.population());

        // Selector de élites
        var selector = new UniqueEliteSelector<DoubleGene, Double>(3);
        var elites = selector.select(result.population(), 6, Optimize.MAXIMUM);
        var eliteFitnesses = elites.stream().map(Phenotype::fitness).toList();

        // Loguear élites
        logManager.logElitesSeleccionados(eliteFitnesses);

        // Loguear generación + escribir CSV
        logManager.logGeneracion(
                result,
                avgFitness,
                diversidad,
                peorFitness,
                getElapsedTimeMillis(),
                fitnessEvaluator,
                csvManager
        );

        // Guardar checkpoint
        CheckpointManager.guardarCheckpoint(result, checkpointFile);
        logManager.logCheckpointGuardado(checkpointFile);

        // Ver si hay estancamiento
        if (isStagnant(mejorFitness)) {
            handleStagnation(result);
        }

        System.out.println("[DEBUG] Generación " + currentGeneration + 
                           " procesada. MejorFitness=" + mejorFitness);
    }

    /**
     * Detecta si el mejor fitness se ha estancado
     */
    private boolean isStagnant(double currentBestFitness) {
        if (Math.abs(currentBestFitness - previousBestFitness) > 0.01) {
            previousBestFitness = currentBestFitness;
            return false;
        }
        return true;
    }

    /**
     * Manejo de estancamiento: 
     * - Reintroducimos diversidad.
     * - Almacenamos esa población en nextPopulation, 
     *   que se aplicará en una segunda corrida tras acabar esta.
     */
    private void handleStagnation(EvolutionResult<DoubleGene, Double> result) {
        logManager.logWarning("Detectado estancamiento. Reintroduciendo diversidad...");

        ISeq<Phenotype<DoubleGene, Double>> nuevaPoblacion = reintroduceDiversity(result.population());

        // Guardamos esa población para la siguiente corrida
        this.nextPopulation = nuevaPoblacion;

        logManager.logInfo("Diversidad reintroducida. Se aplicará en la siguiente ejecución.");
    }

    /**
     * Reemplaza un 30% de la población con genotipos nuevos, manteniendo el mejor.
     */
    private ISeq<Phenotype<DoubleGene, Double>> reintroduceDiversity(ISeq<Phenotype<DoubleGene, Double>> population) {
        var nuevaPoblacion = new ArrayList<>(population.asList());

        var mejorFenotipo = population.stream()
                .max(Comparator.comparingDouble(Phenotype::fitness))
                .orElseThrow();
        var mejorGenotipo = mejorFenotipo.genotype();

        int reemplazo = (int) (nuevaPoblacion.size() * 0.3);
        for (int i = 0; i < reemplazo; i++) {
            var nuevoGenotipo = Genotype.of(DoubleChromosome.of(1, 5, 60));
            nuevaPoblacion.set(i, Phenotype.of(nuevoGenotipo, 0));
        }

        // Mantener el mejor en el slot [0]
        nuevaPoblacion.set(0, Phenotype.of(mejorGenotipo, 0));
        return ISeq.of(nuevaPoblacion);
    }

    /**
     * Calcula el fitness promedio de la población.
     */
    private double calculateAverageFitness(ISeq<Phenotype<DoubleGene, Double>> population) {
        return population.stream()
                .mapToDouble(Phenotype::fitness)
                .average()
                .orElse(0.0);
    }

    /**
     * Determina el peor fitness de la población.
     */
    private double calculateWorstFitness(ISeq<Phenotype<DoubleGene, Double>> population) {
        return population.stream()
                .mapToDouble(Phenotype::fitness)
                .min()
                .orElse(0.0);
    }

    /**
     * Calcula la "distancia" promedio entre genotipos (diversidad).
     */
    private double calculateDiversity(ISeq<Phenotype<DoubleGene, Double>> population) {
        double totalDistance = 0.0;
        int comparisons = 0;

        var genotypes = population.stream()
                .map(Phenotype::genotype)
                .toList();

        for (int i = 0; i < genotypes.size(); i++) {
            for (int j = i + 1; j < genotypes.size(); j++) {
                totalDistance += calculateDistance(genotypes.get(i), genotypes.get(j));
                comparisons++;
            }
        }
        return (comparisons > 0) ? (totalDistance / comparisons) : 0.0;
    }

    /**
     * Distancia entre dos Genotype<DoubleGene> normalizada por el rango de cada gen.
     */
    private double calculateDistance(Genotype<DoubleGene> g1, Genotype<DoubleGene> g2) {
        double distance = 0.0;
        var c1 = (DoubleChromosome) g1.chromosome();
        var c2 = (DoubleChromosome) g2.chromosome();

        for (int i = 0; i < c1.length(); i++) {
            double maxVal = c1.get(i).max();
            double minVal = c1.get(i).min();
            double range = maxVal - minVal;

            double diff = Math.abs(c1.get(i).doubleValue() - c2.get(i).doubleValue());
            distance += (diff / range);
        }
        return distance;
    }

    /**
     * Retorna el tiempo transcurrido en milisegundos desde que inició runGeneticEngine().
     */
    private long getElapsedTimeMillis() {
        return System.currentTimeMillis() - startTime;
    } 
}
