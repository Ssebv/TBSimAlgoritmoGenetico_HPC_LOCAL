import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.ISeq;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GenerationProcessor.java
 *
 * Responsable del procesamiento de cada generación durante la evolución:
 * - Registro de métricas
 * - Cálculo de diversidad y estancamiento
 * - Inyección de diversidad si es necesario
 */
public class GenerationProcessor {

    private final LogManager logManager;
    private final CSVManager csvManager;
    private final FuncionEvaluacionJenetics fitnessEvaluator;
    private final DiversityInjector diversityInjector;

    private EvolutionResult<DoubleGene, Double> lastEvolutionResult;
    private ISeq<Phenotype<DoubleGene, Double>> nextPopulation = null;
    private long startTime;

    public GenerationProcessor(LogManager logManager,
                               CSVManager csvManager,
                               FuncionEvaluacionJenetics fitnessEvaluator,
                               DiversityInjector diversityInjector) {
        this.logManager = logManager;
        this.csvManager = csvManager;
        this.fitnessEvaluator = fitnessEvaluator;
        this.diversityInjector = diversityInjector;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Procesa los datos de la generación actual:
     * - Registra métricas
     * - Detecta estancamiento
     * - Inyecta diversidad si es necesario
     */
    public void processGeneration(EvolutionResult<DoubleGene, Double> result) {
        if (result == null || result.population().isEmpty()) {
            logManager.logWarning("Resultado de generación vacío. Omitiendo...");
            return;
        }

        lastEvolutionResult = result;
        GenerationTracker.incrementGeneration();
        int currentGen = GenerationTracker.getCurrentGeneration();

        double bestFitness = result.bestFitness();
        double worstFitness = calcularPeorFitness(result.population());
        double avgFitness = calcularFitnessPromedio(result.population());
        double diversity = calcularDiversidad(result.population());

        long elapsedTime = (System.nanoTime() - startTime) / 1_000_000L;

        var eliteFitness = new UniqueEliteSelector<DoubleGene, Double>(10)
                .select(result.population(), 6, io.jenetics.Optimize.MAXIMUM)
                .stream()
                .map(Phenotype::fitness)
                .collect(Collectors.toList());

        logManager.logElitesSeleccionados(eliteFitness);
        logManager.logGeneracion(result, avgFitness, diversity, worstFitness, elapsedTime, fitnessEvaluator, csvManager);

        if (estancado(bestFitness)) {
            logManager.logWarning("Estancamiento detectado en generación " + currentGen);
            nextPopulation = diversityInjector.injectDiversity(result.population());
        }

        logManager.logInfo("[DEBUG] Generación " + currentGen + " procesada. MejorFitness=" + bestFitness);

        if (currentGen >= logManager.getConfig().MAX_GENERATIONS) {
            logManager.logInfo("[INFO] Límite máximo de generaciones alcanzado: " + currentGen);
        }
    }

    private double calcularFitnessPromedio(ISeq<Phenotype<DoubleGene, Double>> population) {
        return population.stream()
                .mapToDouble(Phenotype::fitness)
                .average()
                .orElse(0.0);
    }

    private double calcularPeorFitness(ISeq<Phenotype<DoubleGene, Double>> population) {
        return population.stream()
                .mapToDouble(Phenotype::fitness)
                .min()
                .orElse(0.0);
    }

    private double calcularDiversidad(ISeq<Phenotype<DoubleGene, Double>> population) {
        List<Genotype<DoubleGene>> genotypes = population.stream()
                .map(Phenotype::genotype)
                .toList();

        double total = 0.0;
        int count = 0;

        for (int i = 0; i < genotypes.size(); i++) {
            for (int j = i + 1; j < genotypes.size(); j++) {
                total += calcularDistancia(genotypes.get(i), genotypes.get(j));
                count++;
            }
        }

        return (count > 0) ? total / count : 0.0;
    }

    private double calcularDistancia(Genotype<DoubleGene> g1, Genotype<DoubleGene> g2) {
        double sum = 0.0;
        var genes1 = g1.stream().flatMap(c -> c.stream()).toList();
        var genes2 = g2.stream().flatMap(c -> c.stream()).toList();

        for (int i = 0; i < genes1.size(); i++) {
            double diff = genes1.get(i).doubleValue() - genes2.get(i).doubleValue();
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    private boolean estancado(double currentBestFitness) {
        return Math.abs(currentBestFitness - fitnessEvaluator.getBestFitness()) < logManager.getConfig().THRESHOLD_MEJORA;
    }

    public EvolutionResult<DoubleGene, Double> getLastEvolutionResult() {
        return lastEvolutionResult;
    }

    public ISeq<Phenotype<DoubleGene, Double>> getNextPopulation() {
        return nextPopulation;
    }

    public void resetNextPopulation() {
        nextPopulation = null;
    }
}
