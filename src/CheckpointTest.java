import io.jenetics.DoubleGene;
import io.jenetics.DoubleChromosome;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;

import io.jenetics.util.ISeq;

import java.util.ArrayList;
import java.util.List;

public class CheckpointTest {
    public static void main(String[] args) {
        String checkpointFilePath = "checkpoint.ser";

        // Crear una población ficticia
        List<Phenotype<DoubleGene, Double>> population = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            Genotype<DoubleGene> genotype = Genotype.of(DoubleChromosome.of(1, 5, 60));
            Phenotype<DoubleGene, Double> phenotype = Phenotype.of(genotype, 0);
            population.add(phenotype);
        }

        ISeq<Phenotype<DoubleGene, Double>> populationSeq = ISeq.of(population);

        // Definir una generación ficticia y versión
        long generation = 100L;
        int version = 1;

        // Crear CheckpointData con genotipos, generación y versión
        CheckpointData checkpointData = new CheckpointData(
                populationSeq.asList().stream().map(Phenotype::genotype).collect(java.util.stream.Collectors.toList()),
                generation,
                version
        );

        // Guardar el checkpoint
        CheckpointManager.guardarCheckpoint(checkpointData, checkpointFilePath);
        System.out.println("Checkpoint guardado.");
    }
}
