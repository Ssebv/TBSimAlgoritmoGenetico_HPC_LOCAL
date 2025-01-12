import io.jenetics.Gene;
import io.jenetics.Optimize;
import io.jenetics.Phenotype;
import io.jenetics.Selector;
import io.jenetics.util.ISeq;
import io.jenetics.util.Seq;

import java.util.ArrayList;
import java.util.List;

/**
 * Selector que garantiza la selección de individuos élite con valores únicos de fitness.
 * 
 * @param <G> Tipo del gen.
 * @param <C> Tipo del fitness, debe ser comparable.
 */
public class UniqueEliteSelector<G extends Gene<?, G>, C extends Comparable<? super C>> implements Selector<G, C> {

    private final int eliteCount;

    /**
     * Constructor que inicializa el número de élites.
     *
     * @param eliteCount Número máximo de élites a seleccionar.
     */
    public UniqueEliteSelector(int eliteCount) {
        this.eliteCount = eliteCount;
    }

    @Override
    public ISeq<Phenotype<G, C>> select(Seq<Phenotype<G, C>> population, int count, Optimize opt) {
        int selectionCount = validateSelectionCount(count);

        List<Phenotype<G, C>> sortedPopulation = sortPopulation(population, opt);

        List<Phenotype<G, C>> uniqueElites = filterUniqueElites(sortedPopulation, selectionCount);

        return ISeq.of(uniqueElites);
    }

    /**
     * Valida y ajusta la cantidad de selección según los parámetros proporcionados.
     *
     * @param count Número solicitado de individuos a seleccionar.
     * @return Cantidad ajustada basada en los límites de élites.
     */
    private int validateSelectionCount(int count) {
        return Math.min(count, eliteCount);
    }

    /**
     * Ordena la población según la estrategia de optimización.
     *
     * @param population Población inicial.
     * @param opt Estrategia de optimización (MAXIMUM o MINIMUM).
     * @return Lista ordenada de la población.
     */
    private List<Phenotype<G, C>> sortPopulation(Seq<Phenotype<G, C>> population, Optimize opt) {
        List<Phenotype<G, C>> sortedPopulation = new ArrayList<>(population.asList());
        sortedPopulation.sort((a, b) -> {
            int comparison = a.fitness().compareTo(b.fitness());
            return opt == Optimize.MAXIMUM ? -comparison : comparison;
        });
        return sortedPopulation;
    }

    /**
     * Filtra los individuos únicos basados en su fitness.
     *
     * @param sortedPopulation Población previamente ordenada.
     * @param selectionCount Cantidad de individuos únicos a seleccionar.
     * @return Lista de individuos únicos seleccionados.
     */
    private List<Phenotype<G, C>> filterUniqueElites(List<Phenotype<G, C>> sortedPopulation, int selectionCount) {
        List<Phenotype<G, C>> uniqueElites = new ArrayList<>();
        for (Phenotype<G, C> candidate : sortedPopulation) {
            if (uniqueElites.size() >= selectionCount) {
                break;
            }
            boolean isUnique = uniqueElites.stream()
                    .noneMatch(elite -> elite.fitness().equals(candidate.fitness()));
            if (isUnique) {
                uniqueElites.add(candidate);
                logEliteSelection(candidate);
            }
        }
        return uniqueElites;
    }

    /**
     * Registra la selección de un élite en el log.
     *
     * @param candidate Individuo seleccionado como élite.
     */
    private void logEliteSelection(Phenotype<G, C> candidate) {
        // System.out.println("Elite seleccionado: " + candidate.fitness());
    }
}
