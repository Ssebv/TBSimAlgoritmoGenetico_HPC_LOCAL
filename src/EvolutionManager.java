import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.ISeq;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EvolutionManager {
    private final GeneticEngineBuilder engineBuilder;
    private final CheckpointHandler checkpointHandler;
    private final LogManager logManager;
    private final CSVManager csvManager;
    private final FuncionEvaluacionJenetics fitnessEvaluator;
    private final GenerationProcessor generationProcessor;
    private final Configuracion config;
    private final DiversityInjector diversityInjector;
    private final AdaptiveMutationController adaptiveController;

    public EvolutionManager(CSVManager csvManager,
                            String checkpointFile,
                            Logger logger,
                            Configuracion config) {
        this.csvManager = csvManager;
        this.logManager = new LogManager(logger, config.ENABLE_COLORS, config);
        this.checkpointHandler = new CheckpointHandler(checkpointFile);
        this.fitnessEvaluator = FuncionEvaluacionJenetics.getInstance();
        this.engineBuilder = new GeneticEngineBuilder(config);
        this.config = config;
        this.diversityInjector = new DiversityInjector(config, this::evaluate, logManager);
        this.generationProcessor = new GenerationProcessor(logManager, csvManager, fitnessEvaluator, checkpointHandler, diversityInjector);
        this.adaptiveController = new AdaptiveMutationController(config.MUTATION_RATE, 1.0);
    }

    private double evaluate(Genotype<DoubleGene> genotype) {
        int currentGen = GenerationTracker.getCurrentGeneration();
        var resultado = fitnessEvaluator.evaluarResultado(genotype, currentGen);
        boolean stagnant = isStagnant(resultado.getFitness());
        adaptiveController.update(stagnant);
        if (resultado.getFitness() == Double.NEGATIVE_INFINITY) {
            logManager.logWarning("Genotipo con fitness inválido. Saltando...");
            return 0.0;
        }
        return resultado.getFitness();
    }

    public void runGeneticEngine() {
        long startTime = System.nanoTime();
        generationProcessor.setStartTime(startTime);
        int target = config.TARGET_GENERATIONS;
        GenerationTracker.reset();
        List<Genotype<DoubleGene>> currentPopulation = null;

        while (GenerationTracker.getCurrentGeneration() < target) {
            int remaining = target - GenerationTracker.getCurrentGeneration();
            int currentBlockSize = Math.min(config.DEFAULT_GENERATIONS, remaining);

            Engine<DoubleGene, Double> engine;
            if (currentPopulation == null) {
                engine = engineBuilder.buildEngine(this::evaluate, GenerationTracker.getCurrentGeneration(), false);
            } else {
                engine = engineBuilder.buildEngineWithPopulation(this::evaluate, currentPopulation);
            }

            // Ejecutar el engine por un bloque de generaciones y procesar cada generación
            EvolutionResult<DoubleGene, Double> result = engine.stream()
                    .limit(currentBlockSize)
                    .peek(generationProcessor::processGeneration)
                    .collect(EvolutionResult.toBestEvolutionResult());

            // Actualizar la población para el siguiente bloque (convertir fenotipos a genotipos)
            currentPopulation = generationProcessor.getLastEvolutionResult().population().stream()
                    .map(Phenotype::genotype)
                    .collect(Collectors.toList());

            logManager.logInfo("Bloque de " + currentBlockSize 
                    + " generaciones completado. Generación global actual: " 
                    + GenerationTracker.getCurrentGeneration());

            // Si se inyectó diversidad, usar esa población para el siguiente bloque
            ISeq<Phenotype<DoubleGene, Double>> injected = generationProcessor.getNextPopulation();
            if (injected != null) {
                currentPopulation = injected.stream()
                        .map(Phenotype::genotype)
                        .collect(Collectors.toList());
                logManager.logInfo("Utilizando población con diversidad inyectada para el siguiente bloque.");
                generationProcessor.resetNextPopulation();
            }
        }

        logManager.logResumenFinal(GenerationTracker.getCurrentGeneration(),
                generationProcessor.getLastEvolutionResult().bestFitness());

        List<Genotype<DoubleGene>> genotypeList = generationProcessor.getLastEvolutionResult().population().stream()
                .map(Phenotype::genotype)
                .collect(Collectors.toList());
        CheckpointData finalCheckpoint = new CheckpointData(genotypeList, GenerationTracker.getCurrentGeneration(), 2);
        checkpointHandler.saveCheckpoint(finalCheckpoint);
        logManager.logCheckpointGuardado(checkpointHandler.getCheckpointFile());

        engineBuilder.shutdownExecutor();
    }

    public void setCheckpointData(CheckpointData checkpointData) {
        checkpointHandler.setCheckpointData(checkpointData);
    }

    private boolean isStagnant(double currentFitness) {
        return Math.abs(currentFitness - fitnessEvaluator.getBestFitness()) < config.THRESHOLD_MEJORA;
    }
}
