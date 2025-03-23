import io.jenetics.DoubleGene;
import io.jenetics.DoubleChromosome;
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

    // Si no deseas modificar Configuracion, puedes definir valores por defecto:
    private final double minDiversityThreshold;
    private final double diversityInjectionPercentage;

    public DiversityInjector(Configuracion config,
                             java.util.function.Function<Genotype<DoubleGene>, Double> evaluationFunction,
                             LogManager logManager) {
        this.config = config;
        this.evaluationFunction = evaluationFunction;
        this.logManager = logManager;
        // Usar valores de configuración si existen, o valores fijos de lo contrario.
        this.minDiversityThreshold = config.MIN_DIVERSITY_THRESHOLD; // o asignar un valor fijo: 0.5;
        this.diversityInjectionPercentage = config.DIVERSITY_INJECTION_PERCENTAGE; // o, por ejemplo, 0.1;
    }

    public ISeq<Phenotype<DoubleGene, Double>> injectDiversity(ISeq<Phenotype<DoubleGene, Double>> population) {
        double diversity = calculateDiversity(population);
        logManager.logInfo("Diversidad actual: " + diversity);
        if (diversity < minDiversityThreshold) {
            logManager.logInfo("Diversidad baja detectada. Inyectando nuevos individuos...");
            int numToInject = (int) (population.size() * diversityInjectionPercentage);
            List<Phenotype<DoubleGene, Double>> newIndividuals = new ArrayList<>();

            // Crear nuevos individuos aleatorios
            for (int i = 0; i < numToInject; i++) {
                Genotype<DoubleGene> randomGenotype = Genotype.of(DoubleChromosome.of(1, 5, 60));
                double fitness = evaluationFunction.apply(randomGenotype);
                // Se asume generación 0 para el nuevo individuo; ajustar si es necesario.
                Phenotype<DoubleGene, Double> pheno = Phenotype.of(randomGenotype, 0, fitness);
                newIndividuals.add(pheno);
            }
            // Ordenar la población de menor a mayor fitness
            List<Phenotype<DoubleGene, Double>> sortedPopulation = new ArrayList<>(population.asList());
            sortedPopulation.sort(Comparator.comparingDouble(Phenotype::fitness));
            // Reemplazar los peores individuos
            for (int i = 0; i < numToInject && i < sortedPopulation.size(); i++) {
                sortedPopulation.set(i, newIndividuals.get(i));
            }
            return ISeq.of(sortedPopulation);
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
