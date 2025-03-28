import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.ISeq;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GenerationProcessor {
    private final LogManager logManager;
    private final CSVManager csvManager;
    private final FuncionEvaluacionJenetics fitnessEvaluator;
    private final CheckpointHandler checkpointHandler;
    private final DiversityInjector diversityInjector;
    private EvolutionResult<DoubleGene, Double> lastEvolutionResult;
    private ISeq<Phenotype<DoubleGene, Double>> nextPopulation = null;
    private long startTime;

    public GenerationProcessor(LogManager logManager, CSVManager csvManager,
                               FuncionEvaluacionJenetics fitnessEvaluator,
                               CheckpointHandler checkpointHandler,
                               DiversityInjector diversityInjector) {
        this.logManager = logManager;
        this.csvManager = csvManager;
        this.fitnessEvaluator = fitnessEvaluator;
        this.checkpointHandler = checkpointHandler;
        this.diversityInjector = diversityInjector;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void processGeneration(EvolutionResult<DoubleGene, Double> result) {
        if (result == null || result.population().isEmpty()) {
            logManager.logWarning("Resultado de generación vacío. Omitiendo...");
            return;
        }
        lastEvolutionResult = result;
        GenerationTracker.incrementGeneration();
        int genGlobal = GenerationTracker.getCurrentGeneration();

        double mejorFitness = result.bestFitness();
        double peorFitness = calculateWorstFitness(result.population());
        double diversidad = calculateDiversity(result.population());
        double avgFitness = calculateAverageFitness(result.population());
        long elapsedTimeMillis = (long)((System.nanoTime() - startTime) / 1_000_000.0);

        var elites = new UniqueEliteSelector<DoubleGene, Double>(10)
                        .select(result.population(), 6, io.jenetics.Optimize.MAXIMUM);
        var eliteFitnesses = elites.stream().map(Phenotype::fitness).collect(Collectors.toList());

        logManager.logElitesSeleccionados(eliteFitnesses);
        logManager.logGeneracion(result, avgFitness, diversidad, peorFitness, elapsedTimeMillis, fitnessEvaluator, csvManager);

        if (genGlobal % logManager.getConfig().CHECKPOINT_INTERVAL == 0) {
            checkpointHandler.saveCheckpoint(result);
            logManager.logCheckpointGuardado(checkpointHandler.getCheckpointFile());
        }

        if (isStagnant(mejorFitness)) {
            logManager.logWarning("Detectado estancamiento en la generación " + genGlobal);
            nextPopulation = diversityInjector.injectDiversity(result.population());
        }
        logManager.logInfo("[DEBUG] Generación " + genGlobal + " procesada. MejorFitness=" + mejorFitness);
    }

    private double calculateAverageFitness(ISeq<Phenotype<DoubleGene, Double>> population) {
        return population.stream()
                .mapToDouble(Phenotype::fitness)
                .average()
                .orElse(0.0);
    }

    private double calculateWorstFitness(ISeq<Phenotype<DoubleGene, Double>> population) {
        return population.stream()
                .mapToDouble(Phenotype::fitness)
                .min()
                .orElse(0.0);
    }

    private double calculateDiversity(ISeq<Phenotype<DoubleGene, Double>> population) {
        double totalDistance = 0.0;
        int comparisons = 0;
        List<Genotype<DoubleGene>> genotypes = population.stream()
                .map(Phenotype::genotype)
                .collect(Collectors.toList());
        for (int i = 0; i < genotypes.size(); i++) {
            for (int j = i + 1; j < genotypes.size(); j++) {
                totalDistance += calculateDistance(genotypes.get(i), genotypes.get(j));
                comparisons++;
            }
        }
        return (comparisons > 0) ? totalDistance / comparisons : 0.0;
    }

    private double calculateDistance(Genotype<DoubleGene> g1, Genotype<DoubleGene> g2) {
        double suma = 0.0;
        var genes1 = g1.stream().flatMap(chromosome -> chromosome.stream()).toList();
        var genes2 = g2.stream().flatMap(chromosome -> chromosome.stream()).toList();
        for (int i = 0; i < genes1.size(); i++) {
            double diff = genes1.get(i).doubleValue() - genes2.get(i).doubleValue();
            suma += diff * diff;
        }
        return Math.sqrt(suma);
    }

    private boolean isStagnant(double currentBestFitness) {
        return Math.abs(currentBestFitness - fitnessEvaluator.getBestFitness()) < logManager.getConfig().THRESHOLD_MEJORA;
    }

    public EvolutionResult<DoubleGene, Double> getLastEvolutionResult() {
        return lastEvolutionResult;
    }

    public int getCurrentGeneration() {
        return GenerationTracker.getCurrentGeneration();
    }

    public ISeq<Phenotype<DoubleGene, Double>> getNextPopulation() {
        return nextPopulation;
    }
    
    // Método para reiniciar la población inyectada
    public void resetNextPopulation() {
        nextPopulation = null;
    }
}
