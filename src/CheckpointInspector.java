import io.jenetics.DoubleGene;
import io.jenetics.Genotype;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

public class CheckpointInspector {
    public static void main(String[] args) {
        String checkpointFilePath = "checkpoint.ser"; // Asegúrate de que la ruta es correcta

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(checkpointFilePath))) {
            // Deserializar el objeto CheckpointData
            CheckpointData checkpointData = (CheckpointData) ois.readObject();

            // Imprimir información general
            System.out.println("=== Checkpoint Cargado ===");
            System.out.println("Generación: " + checkpointData.getGeneration());
            System.out.println("Número de Genotipos: " + checkpointData.getGenotypes().size());
            System.out.println("Versión del Checkpoint: " + checkpointData.getVersion());
            System.out.println("==========================\n");

            // Imprimir detalles de cada genotipo
            List<Genotype<DoubleGene>> genotypes = checkpointData.getGenotypes();
            for (int i = 0; i < genotypes.size(); i++) {
                Genotype<DoubleGene> genotype = genotypes.get(i);
                System.out.println("Genotipo " + (i + 1) + ": " + genotype);
            }

        } catch (IOException e) {
            System.err.println("Error al leer el archivo de checkpoint: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Clase no encontrada durante la deserialización: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
