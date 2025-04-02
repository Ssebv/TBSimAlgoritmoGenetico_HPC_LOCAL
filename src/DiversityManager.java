import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.util.ISeq;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DiversityManager.java
 *
 * Reintroduce diversidad genética a la población reemplazando un porcentaje de los
 * peores individuos con nuevos genotipos aleatorios. Siempre se conserva el mejor individuo.
 */
public class DiversityManager {

    private final LogManager logManager;
    private final Configuracion config;
    private final double porcentajeReemplazo;

    // === Constructor ===
    public DiversityManager(LogManager logManager, Configuracion config) {
        this.logManager = logManager;
        this.config = config;
        this.porcentajeReemplazo = config.DIVERSITY_INJECTION_PERCENTAGE;
    }

    /**
     * Reemplaza los individuos con peor fitness en la población por nuevos aleatorios.
     * Se garantiza que el mejor individuo no sea reemplazado.
     *
     * @param population Población actual
     * @return Nueva población con diversidad reintroducida
     */
    public ISeq<Phenotype<DoubleGene, Double>> reintroduceDiversity(ISeq<Phenotype<DoubleGene, Double>> population) {
        if (population.isEmpty()) {
            logManager.logWarning("Población vacía. No se puede aplicar reintroducción de diversidad.");
            return population;
        }

        List<Phenotype<DoubleGene, Double>> nuevaPoblacion = new ArrayList<>(population.asList());

        // Extraer el mejor individuo
        Phenotype<DoubleGene, Double> mejorFenotipo = nuevaPoblacion.stream()
                .max(Comparator.comparingDouble(Phenotype::fitness))
                .orElseThrow();

        nuevaPoblacion.sort(Comparator.comparingDouble(Phenotype::fitness)); // De peor a mejor

        int numReemplazos = Math.max(1, (int) (nuevaPoblacion.size() * porcentajeReemplazo));
        int realizados = 0;

        for (int i = 0; i < nuevaPoblacion.size() && realizados < numReemplazos; i++) {
            Phenotype<DoubleGene, Double> actual = nuevaPoblacion.get(i);
            if (actual.equals(mejorFenotipo)) continue;

            Genotype<DoubleGene> nuevoGenotipo = generateSafeRandomGenotype();
            double fitness = evaluarFitness(nuevoGenotipo);

            if (esFitnessValido(fitness)) {
                nuevaPoblacion.set(i, Phenotype.of(nuevoGenotipo, GenerationTracker.getCurrentGeneration(), fitness));
                realizados++;
            } else {
                logManager.logWarning("Fitness inválido en nuevo individuo. Reintentando...");
                i--; // Intentar reemplazo de nuevo
            }
        }

        // Asegura que el mejor fenotipo se mantenga
        if (!nuevaPoblacion.contains(mejorFenotipo)) {
            nuevaPoblacion.set(0, mejorFenotipo);
        }

        logManager.logInfo("Diversidad reintroducida: " + realizados + " individuos reemplazados.");
        return ISeq.of(nuevaPoblacion);
    }

    // === Genotype seguro ===

    private Genotype<DoubleGene> generateSafeRandomGenotype() {
        try {
            Genotype<DoubleGene> g = Genotype.of(DoubleChromosome.of(1.0, 5.0, 60));
            return (g != null && g.geneCount() >= 60) ? g : createDefaultGenotype();
        } catch (Exception e) {
            logManager.logError("Error generando nuevo genotipo aleatorio: " + e.getMessage());
            return createDefaultGenotype();
        }
    }

    private Genotype<DoubleGene> createDefaultGenotype() {
        return Genotype.of(DoubleChromosome.of(1.0, 5.0, 60));
    }

    private boolean esFitnessValido(double f) {
        return !(Double.isNaN(f) || Double.isInfinite(f));
    }

    // === Fitness stub (a reemplazar por evaluación real) ===

    private double evaluarFitness(Genotype<DoubleGene> genotype) {
        try {
            return config != null ? evaluationStub(genotype) : 0.0;
        } catch (Exception e) {
            logManager.logWarning("Error evaluando fitness: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Esta función debe ser reemplazada por una evaluación real del genotipo.
     */
    private double evaluationStub(Genotype<DoubleGene> genotype) {
        // Este es un valor fijo para pruebas.
        return 10000.0;
    }
}
