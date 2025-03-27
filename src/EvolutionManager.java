import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import java.util.List;
import java.util.logging.Logger;

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

    /**
     * Constructor de la clase EvolutionManager.
     *
     * @param csvManager       Instancia de CSVManager para manejar archivos CSV.
     * @param checkpointFile   Ruta del archivo de checkpoint.
     * @param logger           Instancia de Logger para registrar información.
     * @param config           Configuración del sistema.
     */
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
        // logManager.logResultadoPartido(resultado);
        return resultado.getFitness();
    }

    public void runGeneticEngine() {
        long startTime = System.nanoTime();
        generationProcessor.setStartTime(startTime);
    
        int target = config.TARGET_GENERATIONS;
        // Reiniciar el contador al inicio (opcional, en caso de múltiples ejecuciones)
        GenerationTracker.reset();
    
        while (GenerationTracker.getCurrentGeneration() < target) {
            int remaining = target - GenerationTracker.getCurrentGeneration();
            int currentBlockSize = Math.min(config.DEFAULT_GENERATIONS, remaining);
    
            Engine<DoubleGene, Double> engine = engineBuilder.buildEngine(
                this::evaluate,
                GenerationTracker.getCurrentGeneration(),
                false
            );
    
            engine.stream()
                  .limit(currentBlockSize)
                  .peek(result -> generationProcessor.processGeneration(result))
                  .collect(EvolutionResult.toBestPhenotype());
    
            logManager.logInfo("Bloque de " + currentBlockSize + " generaciones completado. Generación global actual: " 
                                + GenerationTracker.getCurrentGeneration());
        }
    
        logManager.logResumenFinal(GenerationTracker.getCurrentGeneration(),
                generationProcessor.getLastEvolutionResult().bestFitness());
    
        // Guardar último checkpoint y cerrar el executor
        List<Genotype<DoubleGene>> genotypeList = generationProcessor.getLastEvolutionResult().population().stream()
                .map(Phenotype::genotype)
                .toList();
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
