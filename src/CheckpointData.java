import io.jenetics.Phenotype;
import io.jenetics.DoubleGene;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class CheckpointData implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Phenotype<DoubleGene, Double>> population;
    private int generation;
    private Map<String, Object> config;

    public CheckpointData(List<Phenotype<DoubleGene, Double>> population, int generation, Map<String, Object> config) {
        this.population = population;
        this.generation = generation;
        this.config = config;
    }

    public List<Phenotype<DoubleGene, Double>> getPopulation() {
        return population;
    }

    public int getGeneration() {
        return generation;
    }

    public Map<String, Object> getConfig() {
        return config;
    }
}
