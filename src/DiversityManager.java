import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.util.ISeq;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DiversityManager {
    private final LogManager logManager;
    private final Configuracion config;

    public DiversityManager(LogManager logManager, Configuracion config) {
        this.logManager = logManager;
        this.config = config;
    }

    public ISeq<Phenotype<DoubleGene, Double>> reintroduceDiversity(ISeq<Phenotype<DoubleGene, Double>> population) {
        List<Phenotype<DoubleGene, Double>> nuevaPoblacion = new ArrayList<>(population.asList());

        Phenotype<DoubleGene, Double> mejorFenotipo = population.stream()
                .max(Comparator.comparingDouble(Phenotype::fitness))
                .orElseThrow();
        var mejorGenotipo = mejorFenotipo.genotype();

        int reemplazo = (int) (nuevaPoblacion.size() * 0.3);
        for (int i = 0; i < reemplazo; i++) {
            var nuevoGenotipo = Genotype.of(DoubleChromosome.of(1, 5, 60));
            nuevaPoblacion.set(i, Phenotype.of(nuevoGenotipo, 0));
        }

        // Mantener el mejor en el slot [0]
        nuevaPoblacion.set(0, Phenotype.of(mejorGenotipo, 0));
        logManager.logInfo("Diversidad reintroducida en la población.");
        return ISeq.of(nuevaPoblacion);
    }
}
