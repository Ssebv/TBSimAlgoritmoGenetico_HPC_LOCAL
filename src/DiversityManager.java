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
    // Porcentaje configurable (podrías obtenerlo de config si lo deseas)
    private final double porcentajeReemplazo;

    public DiversityManager(LogManager logManager, Configuracion config) {
        this.logManager = logManager;
        this.config = config;
        this.porcentajeReemplazo = 0.4; // O bien, config.PORCENTAJE_REEMPLAZO si lo defines en Configuracion
    }

    /**
     * Reintroduce diversidad reemplazando un porcentaje de los peores individuos por nuevos, preservando siempre el mejor.
     */
    public ISeq<Phenotype<DoubleGene, Double>> reintroduceDiversity(ISeq<Phenotype<DoubleGene, Double>> population) {
        List<Phenotype<DoubleGene, Double>> nuevaPoblacion = new ArrayList<>(population.asList());
        if (nuevaPoblacion.isEmpty()) return population;
        
        // Obtener el mejor fenotipo
        Phenotype<DoubleGene, Double> mejorFenotipo = nuevaPoblacion.stream()
                .max(Comparator.comparingDouble(Phenotype::fitness))
                .orElseThrow();
        // Ordenar de menor a mayor fitness para identificar a los peores
        nuevaPoblacion.sort(Comparator.comparingDouble(Phenotype::fitness));
        
        // Calcular el número de individuos a reemplazar
        int numReemplazo = (int) (nuevaPoblacion.size() * porcentajeReemplazo);
        if (numReemplazo <= 0) numReemplazo = 1;
        
        // Reemplazar los peores individuos (los primeros en la lista)
        for (int i = 0; i < numReemplazo && i < nuevaPoblacion.size(); i++) {
            // No reemplazamos el mejor individuo (que estará al final tras ordenar)
            if (nuevaPoblacion.get(i).equals(mejorFenotipo)) continue;
            Genotype<DoubleGene> nuevoGenotipo = Genotype.of(DoubleChromosome.of(1, 5, 60));
            // Aquí, puedes evaluar o simplemente crear el fenotipo sin evaluación
            Phenotype<DoubleGene, Double> nuevoFenotipo = Phenotype.of(nuevoGenotipo, 0);
            nuevaPoblacion.set(i, nuevoFenotipo);
        }
        
        // Asegurar que el mejor fenotipo quede en la población: lo ubicamos en la posición 0
        nuevaPoblacion.set(0, Phenotype.of(mejorFenotipo.genotype(), 0, mejorFenotipo.fitness()));
        
        logManager.logInfo("Diversidad reintroducida: se reemplazaron " + numReemplazo + " individuos.");
        return ISeq.of(nuevaPoblacion);
    }
}
