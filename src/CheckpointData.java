import io.jenetics.DoubleGene;
import io.jenetics.Genotype;

import java.io.Serializable;
import java.util.List;

public class CheckpointData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Genotype<DoubleGene>> genotypes;
    private final int generation;

    public CheckpointData(List<Genotype<DoubleGene>> genotypes, int generation) {
        this.genotypes = genotypes;
        this.generation = generation;
    }

    public List<Genotype<DoubleGene>> getGenotypes() {
        return genotypes;
    }

    public int getGeneration() {
        return generation;
    }

    @Override
    public String toString() {
        return "CheckpointData{ gen=" + generation + ", size=" + genotypes.size() + " }";
    }
}
