import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.util.ISeq;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * DiversityInjector.java
 *
 * Clase encargada de mantener la diversidad genética durante el proceso evolutivo.
 * Si la diversidad cae bajo un umbral definido, inyecta nuevos individuos aleatorios.
 */
public class DiversityInjector {

    private final Configuracion config;
    private final Function<Genotype<DoubleGene>, Double> evaluationFunction;
    private final LogManager logManager;

    private final double minDiversityThreshold;
    private final double diversityInjectionPercentage;

    // === Constructor ===
    public DiversityInjector(Configuracion config,
                             Function<Genotype<DoubleGene>, Double> evaluationFunction,
                             LogManager logManager) {
        this.config = config;
        this.evaluationFunction = evaluationFunction;
        this.logManager = logManager;
        this.minDiversityThreshold = config.MIN_DIVERSITY_THRESHOLD;
        this.diversityInjectionPercentage = config.DIVERSITY_INJECTION_PERCENTAGE;
    }

    /**
     * Evalúa la diversidad de la población y reemplaza individuos si es necesario.
     *
     * @param population Población actual
     * @return Nueva población con individuos inyectados si se detecta baja diversidad
     */
    public ISeq<Phenotype<DoubleGene, Double>> injectDiversity(ISeq<Phenotype<DoubleGene, Double>> population) {
        double diversity = calculateDiversity(population);
        logManager.logInfo(String.format("Diversidad actual: %.4f", diversity));

        if (diversity < minDiversityThreshold) {
            logManager.logWarning(String.format("Diversidad baja (%.4f). Inyectando nuevos individuos...", diversity));
            int toInject = Math.max(1, (int) (population.size() * diversityInjectionPercentage));

            List<Phenotype<DoubleGene, Double>> nuevaPoblacion = new ArrayList<>(population.asList());
            nuevaPoblacion.sort(Comparator.comparingDouble(Phenotype::fitness)); // Reemplaza los peores

            int reemplazados = 0;

            for (int i = 0; i < nuevaPoblacion.size() && reemplazados < toInject; i++) {
                Genotype<DoubleGene> candidato = generateSafeRandomGenotype();
                double fitness = evaluationFunction.apply(candidato);

                if (candidato != null && isFitnessValido(fitness)) {
                    Phenotype<DoubleGene, Double> nuevo = Phenotype.of(
                            candidato,
                            GenerationTracker.getCurrentGeneration(),
                            fitness
                    );
                    nuevaPoblacion.set(i, nuevo);
                    reemplazados++;
                } else {
                    logManager.logWarning("Se generó un individuo inválido. Reintentando...");
                    i--; // Reintento
                }
            }

            logManager.logInfo("Individuos inyectados exitosamente: " + reemplazados);
            return ISeq.of(nuevaPoblacion);
        }

        return population; // No se inyectó nada
    }

    // === Genotipo aleatorio seguro ===
    private Genotype<DoubleGene> generateSafeRandomGenotype() {
        try {
            Genotype<DoubleGene> g = Genotype.of(DoubleChromosome.of(1.0, 5.0, 60));
            return (g != null && g.geneCount() >= 60) ? g : createDefaultGenotype();
        } catch (Exception e) {
            logManager.logError("Error generando genotype aleatorio: " + e.getMessage());
            return createDefaultGenotype();
        }
    }

    private Genotype<DoubleGene> createDefaultGenotype() {
        return Genotype.of(DoubleChromosome.of(1.0, 5.0, 60));
    }

    private boolean isFitnessValido(double fitness) {
        return !(Double.isNaN(fitness) || Double.isInfinite(fitness));
    }

    // === Diversidad ===

    /**
     * Calcula la diversidad promedio entre todos los pares de individuos.
     */
    private double calculateDiversity(ISeq<Phenotype<DoubleGene, Double>> population) {
        List<Genotype<DoubleGene>> genotypes = population.stream().map(Phenotype::genotype).toList();
        double totalDistancia = 0.0;
        int comparaciones = 0;

        for (int i = 0; i < genotypes.size(); i++) {
            for (int j = i + 1; j < genotypes.size(); j++) {
                totalDistancia += calcularDistancia(genotypes.get(i), genotypes.get(j));
                comparaciones++;
            }
        }

        return (comparaciones > 0) ? totalDistancia / comparaciones : 0.0;
    }

    /**
     * Distancia euclidiana entre dos genotipos.
     */
    private double calcularDistancia(Genotype<DoubleGene> g1, Genotype<DoubleGene> g2) {
        var genes1 = g1.stream().flatMap(ch -> ch.stream()).toList();
        var genes2 = g2.stream().flatMap(ch -> ch.stream()).toList();

        double suma = 0.0;
        for (int i = 0; i < genes1.size(); i++) {
            double diff = genes1.get(i).doubleValue() - genes2.get(i).doubleValue();
            suma += diff * diff;
        }

        return Math.sqrt(suma);
    }
}
