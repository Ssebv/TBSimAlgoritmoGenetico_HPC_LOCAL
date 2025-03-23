import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.util.ISeq;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DiversityInjector {
    private final Configuracion config;
    private final java.util.function.Function<Genotype<DoubleGene>, Double> evaluationFunction;
    private final LogManager logManager;
    
    // Parámetros configurables para inyección de diversidad
    private final double minDiversityThreshold;
    private final double diversityInjectionPercentage;

    public DiversityInjector(Configuracion config,
                             java.util.function.Function<Genotype<DoubleGene>, Double> evaluationFunction,
                             LogManager logManager) {
        this.config = config;
        this.evaluationFunction = evaluationFunction;
        this.logManager = logManager;
        this.minDiversityThreshold = config.MIN_DIVERSITY_THRESHOLD;
        this.diversityInjectionPercentage = config.DIVERSITY_INJECTION_PERCENTAGE;
    }

    /**
     * Inyecta nuevos individuos en la población si la diversidad cae por debajo del umbral.
     * Reemplaza aleatoriamente parte de los peores individuos.
     */
    public ISeq<Phenotype<DoubleGene, Double>> injectDiversity(ISeq<Phenotype<DoubleGene, Double>> population) {
        double diversity = calculateDiversity(population);
        logManager.logInfo("Diversidad actual: " + diversity);
        if (diversity < minDiversityThreshold) {
            logManager.logInfo("Diversidad baja detectada. Inyectando nuevos individuos...");
            int numToInject = (int) (population.size() * diversityInjectionPercentage);
            if (numToInject <= 0) numToInject = 1;
            List<Phenotype<DoubleGene, Double>> nuevaPoblacion = new ArrayList<>(population.asList());
            
            // Ordenar la población de menor a mayor fitness (los peores primero)
            nuevaPoblacion.sort(Comparator.comparingDouble(Phenotype::fitness));
            
            // Reemplazar los peores individuos
            for (int i = 0; i < numToInject && i < nuevaPoblacion.size(); i++) {
                Genotype<DoubleGene> randomGenotype = Genotype.of(DoubleChromosome.of(1, 5, 60));
                double fitness = evaluationFunction.apply(randomGenotype);
                Phenotype<DoubleGene, Double> nuevoFenotipo = Phenotype.of(randomGenotype, 0, fitness);
                nuevaPoblacion.set(i, nuevoFenotipo);
            }
            
            logManager.logInfo("Se inyectaron " + numToInject + " nuevos individuos en la población.");
            return ISeq.of(nuevaPoblacion);
        }
        return population;
    }

    private double calculateDiversity(ISeq<Phenotype<DoubleGene, Double>> population) {
        double totalDistance = 0.0;
        int comparisons = 0;
        List<Genotype<DoubleGene>> genotypes = population.stream()
                .map(Phenotype::genotype)
                .toList();
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
}
