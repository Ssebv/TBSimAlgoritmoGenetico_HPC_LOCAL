import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.util.ISeq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DiversityManager {
    private final LogManager logManager;
    private final Configuracion config;
    // Puedes definir un parámetro configurable para el porcentaje de reemplazo
    private final double porcentajeReemplazo;

    public DiversityManager(LogManager logManager, Configuracion config) {
        this.logManager = logManager;
        this.config = config;
        this.porcentajeReemplazo = 0.4;
    }

    /**
     * Reintroduce diversidad reemplazando aleatoriamente un porcentaje de la población
     * con nuevos individuos, manteniendo siempre el mejor.
     */
    public ISeq<Phenotype<DoubleGene, Double>> reintroduceDiversity(ISeq<Phenotype<DoubleGene, Double>> population) {
        List<Phenotype<DoubleGene, Double>> nuevaPoblacion = new ArrayList<>(population.asList());
        if (nuevaPoblacion.isEmpty()) return population;
        
        // Obtener el mejor fenotipo
        Phenotype<DoubleGene, Double> mejorFenotipo = nuevaPoblacion.stream()
                .max(Comparator.comparingDouble(Phenotype::fitness))
                .orElseThrow();
        var mejorGenotipo = mejorFenotipo.genotype();
        
        // Calcular el número de individuos a reemplazar
        int numReemplazo = (int) (nuevaPoblacion.size() * porcentajeReemplazo);
        if (numReemplazo <= 0) numReemplazo = 1;
        
        // Barajar la población para elegir posiciones de forma aleatoria
        Collections.shuffle(nuevaPoblacion);
        
        // Reemplazar los individuos seleccionados por nuevos aleatorios
        for (int i = 0; i < numReemplazo; i++) {
            Genotype<DoubleGene> nuevoGenotipo = Genotype.of(DoubleChromosome.of(1, 5, 60));
            // Se puede mantener generación 0 o pasar un valor configurable
            nuevaPoblacion.set(i, Phenotype.of(nuevoGenotipo, 0));
        }
        
        // Asegurar que el mejor fenotipo permanezca (lo ubicamos en la posición 0)
        nuevaPoblacion.set(0, Phenotype.of(mejorGenotipo, 0));
        
        logManager.logInfo("Diversidad reintroducida en la población. (" + numReemplazo + " individuos reemplazados)");
        return ISeq.of(nuevaPoblacion);
    }
}
