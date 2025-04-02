import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.ISeq;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * EvolutionManager.java
 *
 * Coordina la ejecución del motor evolutivo, integrando evaluación,
 * logging, diversidad, adaptación de mutación y control de CPU.
 */
public class EvolutionManager {

    private final LogManager logManager;
    private final CSVManager csvManager;
    private final Configuracion config;
    private final GeneticEngineBuilder engineBuilder;
    private final FuncionEvaluacionJenetics fitnessEvaluator;
    private final GenerationProcessor generationProcessor;
    private final DiversityInjector diversityInjector;
    private final AdaptiveMutationController adaptiveController;

    private List<Genotype<DoubleGene>> currentPopulation = null;
    private boolean stopRequested = false;

    public EvolutionManager(CSVManager csvManager, Logger logger, Configuracion config) {
        this.csvManager = csvManager;
        this.config = config;
        this.logManager = new LogManager(logger, config.ENABLE_COLORS, config);
        this.fitnessEvaluator = FuncionEvaluacionJenetics.getInstance();
        this.engineBuilder = new GeneticEngineBuilder(config, logManager);

        this.diversityInjector = new DiversityInjector(config, this::evaluate, logManager);
        this.generationProcessor = new GenerationProcessor(logManager, csvManager, fitnessEvaluator, diversityInjector);
        this.adaptiveController = new AdaptiveMutationController(config.MUTATION_RATE, 1.0);
    }

    /**
     * Ejecuta el motor evolutivo por bloques hasta alcanzar el máximo de
     * generaciones.
     */
    public void runGeneticEngine() {
        long startTime = System.nanoTime();
        generationProcessor.setStartTime(startTime);

        GenerationTracker.reset();
        currentPopulation = null;

        CpuSampler sampler = new CpuSampler();
        Thread samplerThread = new Thread(sampler);
        samplerThread.start();

        while (!stopRequested && GenerationTracker.getCurrentGeneration() < config.MAX_GENERATIONS) {
            logManager.logInfo("Generación actual: " + GenerationTracker.getCurrentGeneration());

            int generacionesRestantes = config.MAX_GENERATIONS - GenerationTracker.getCurrentGeneration();
            int blockSize = Math.min(generacionesRestantes, config.MAX_GENERATIONS_PER_BLOCK);

            // Adaptación dinámica de tasa de mutación
            double adaptiveMutationRate = adaptiveController.getAdaptiveMutationRate();
            engineBuilder.setMutationRate(adaptiveMutationRate);

            Engine<DoubleGene, Double> engine = engineBuilder.buildEngine(this::evaluate,
                    GenerationTracker.getCurrentGeneration(), false);
            EvolutionResult<DoubleGene, Double> result;

            if (currentPopulation == null) {
                result = engine.stream()
                        .limit(blockSize)
                        .peek(generationProcessor::processGeneration) // Procesamos la generación
                        .collect(EvolutionResult.toBestEvolutionResult());
            } else {
                ISeq<Phenotype<DoubleGene, Double>> initial = ISeq.of(
                        currentPopulation.stream()
                                .map(gt -> Phenotype.of(gt, GenerationTracker.getCurrentGeneration(), 0.0))
                                .collect(Collectors.toList()));

                result = engine.stream(initial, GenerationTracker.getCurrentGeneration())
                        .limit(blockSize)
                        .peek(generationProcessor::processGeneration)
                        .collect(EvolutionResult.toBestEvolutionResult());
            }

            currentPopulation = result.population().stream()
                    .map(Phenotype::genotype)
                    .collect(Collectors.toList());

            logManager.logInfo("Bloque de " + blockSize + " generaciones completado. Generación actual: " +
                    GenerationTracker.getCurrentGeneration());

            if (GenerationTracker.getCurrentGeneration() >= config.MAX_GENERATIONS) {
                logManager.logInfo("[INFO] Máximo de generaciones alcanzado. Finalizando evolución...");
                stopRequested = true; // Detenemos la ejecución
                logManager.logInfo("[INFO] Valor de stopRequested: " + stopRequested);
            }

            // Incrementa la generación al final de cada bloque
            GenerationTracker.incrementGeneration(); // Asegúrate de incrementar la generación
            logManager.logInfo("Generación incrementada a: " + GenerationTracker.getCurrentGeneration()); // Verifica la
                                                                                                          // generación
        }

        // Finalización y métricas
        sampler.stop();
        try {
            samplerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        fitnessEvaluator.setCpuLoadPerCore(sampler.getPromedioPorNucleo());
        logManager.logResumenFinal(GenerationTracker.getCurrentGeneration(), fitnessEvaluator);
        engineBuilder.shutdownExecutor();
    }

    /**
     * Evalúa el genotipo, registra estancamiento y actualiza mutación.
     */
    private double evaluate(Genotype<DoubleGene> genotype) {
        int genActual = GenerationTracker.getCurrentGeneration();
        var resultado = fitnessEvaluator.evaluarResultado(genotype, genActual);

        if (resultado.getFitness() == Double.NEGATIVE_INFINITY) {
            logManager.logWarning("Genotipo con fitness inválido. Saltando...");
            return 0.0;
        }

        boolean estancado = isStagnant(resultado.getFitness());
        adaptiveController.update(estancado);

        return resultado.getFitness();
    }

    private boolean isStagnant(double currentFitness) {
        return Math.abs(currentFitness - fitnessEvaluator.getBestFitness()) < config.THRESHOLD_MEJORA;
    }

    public void requestStop() {
        stopRequested = true; // Solicitamos la parada cuando sea necesario
    }
}
