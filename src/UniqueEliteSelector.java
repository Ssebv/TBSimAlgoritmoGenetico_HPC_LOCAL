import io.jenetics.Gene;
import io.jenetics.Optimize;
import io.jenetics.Phenotype;
import io.jenetics.Selector;
import io.jenetics.util.ISeq;
import io.jenetics.util.Seq;

import java.util.ArrayList;
import java.util.List;

/**
 * Selector de élites que garantiza la unicidad de los individuos seleccionados
 * según su valor de fitness.
 *
 * @param <G> Tipo del gen.
 * @param <C> Tipo del valor de fitness, debe ser Comparable.
 */
public class UniqueEliteSelector<G extends Gene<?, G>, C extends Comparable<? super C>> implements Selector<G, C> {

    private final int eliteCount;

    /**
     * Crea un selector de élites únicos.
     *
     * @param eliteCount Número máximo de élites a seleccionar.
     */
    public UniqueEliteSelector(int eliteCount) {
        this.eliteCount = eliteCount;
    }

    /**
     * Selecciona élites únicos en función del valor de fitness y la estrategia de optimización.
     *
     * @param population Población total.
     * @param count Número solicitado de individuos a seleccionar.
     * @param opt Estrategia de optimización (MAXIMUM o MINIMUM).
     * @return Secuencia de élites únicos.
     */
    @Override
    public ISeq<Phenotype<G, C>> select(Seq<Phenotype<G, C>> population, int count, Optimize opt) {
        int maxSeleccion = Math.min(count, eliteCount);
        List<Phenotype<G, C>> ordenados = ordenarPoblacion(population, opt);

        List<Phenotype<G, C>> unicos = new ArrayList<>();
        for (Phenotype<G, C> candidato : ordenados) {
            if (unicos.size() >= maxSeleccion) break;
            if (esUnico(candidato, unicos)) {
                unicos.add(candidato);
                // logEliteSeleccionado(candidato); // Descomenta si quieres log
            }
        }

        return ISeq.of(unicos);
    }

    /**
     * Ordena la población según la estrategia de optimización.
     */
    private List<Phenotype<G, C>> ordenarPoblacion(Seq<Phenotype<G, C>> poblacion, Optimize opt) {
        List<Phenotype<G, C>> lista = new ArrayList<>(poblacion.asList());
        lista.sort((a, b) -> {
            int cmp = a.fitness().compareTo(b.fitness());
            return (opt == Optimize.MAXIMUM) ? -cmp : cmp;
        });
        return lista;
    }

    /**
     * Verifica si un candidato ya existe en la lista según su fitness.
     */
    private boolean esUnico(Phenotype<G, C> candidato, List<Phenotype<G, C>> seleccionados) {
        return seleccionados.stream()
                .noneMatch(elite -> elite.fitness().equals(candidato.fitness()));
    }

    /**
     * Loggea la selección de un élite. (Desactivado por defecto)
     */
    @SuppressWarnings("unused")
    private void logEliteSeleccionado(Phenotype<G, C> elite) {
        // System.out.println("Elite seleccionado: " + elite.fitness());
    }
}
