import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import java.io.Serializable;
import java.util.List;

public class CheckpointData implements Serializable {
    private static final long serialVersionUID = 1L; // Mantén este valor constante entre versiones compatibles

    private final List<Genotype<DoubleGene>> genotypes;
    private final long generation;
    private final int version;

    public CheckpointData(List<Genotype<DoubleGene>> genotypes, long generation, int version) {
        this.genotypes = genotypes;
        this.generation = generation;
        this.version = version;
    }

    public List<Genotype<DoubleGene>> getGenotypes() {
        return genotypes;
    }

    public long getGeneration() {
        return generation;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "CheckpointData{ generation=" + generation + ", genotypes.size=" + genotypes.size() + ", version=" + version + " }";
    }
}
