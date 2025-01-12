import io.jenetics.DoubleGene;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Clase para gestionar checkpoints en procesos evolutivos.
 * Permite guardar, cargar y preguntar por el uso de checkpoints existentes.
 */
public class CheckpointManager {

    /**
     * Pregunta al usuario si desea usar un checkpoint existente.
     *
     * @param checkpointFilePath Ruta del archivo de checkpoint.
     * @return {@code true} si se debe usar el checkpoint, {@code false} si no.
     */
    public static boolean preguntarUsoDeCheckpoint(String checkpointFilePath) {
        File checkpointFile = new File(checkpointFilePath);

        if (!checkpointFile.exists()) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("Se encontró un checkpoint existente. ¿Deseas usarlo? (s/n): ");
                String respuesta = reader.readLine().trim().toLowerCase();
                switch (respuesta) {
                    case "s", "si" -> {
                        System.out.println("Usando el checkpoint existente.");
                        return true;
                    }
                    case "n", "no" -> {
                        System.out.println("Eliminando el checkpoint existente...");
                        eliminarCheckpoint(checkpointFile);
                        return false;
                    }
                    default -> System.out.println("Por favor, responde con 's' (sí) o 'n' (no).");
                }
            }
        } catch (IOException e) {
            System.err.println("Error al procesar la entrada del usuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Guarda un checkpoint en un archivo.
     *
     * @param result            Resultado del proceso evolutivo.
     * @param checkpointFilePath Ruta del archivo de checkpoint.
     */
    public static void guardarCheckpoint(EvolutionResult<DoubleGene, Double> result, String checkpointFilePath) {
        if (result == null) {
            System.err.println("El resultado de la evolución es nulo. No se puede guardar el checkpoint.");
            return;
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(checkpointFilePath))) {
            List<Phenotype<DoubleGene, Double>> population = result.population().asList();
            CheckpointData checkpointData = new CheckpointData(population, (int) result.generation());
            oos.writeObject(checkpointData);
            System.out.println("Checkpoint guardado correctamente en " + checkpointFilePath);
        } catch (IOException e) {
            System.err.println("Error guardando el checkpoint: " + e.getMessage());
        }
    }

    /**
     * Carga un checkpoint desde un archivo.
     *
     * @param checkpointFilePath Ruta del archivo de checkpoint.
     * @return Datos del checkpoint, o {@code null} si ocurre un error.
     */
    public static CheckpointData cargarCheckpoint(String checkpointFilePath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(checkpointFilePath))) {
            return (CheckpointData) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("No se pudo cargar el checkpoint: " + e.getMessage());
            return null;
        }
    }

    /**
     * Elimina un archivo de checkpoint.
     *
     * @param checkpointFile Archivo de checkpoint.
     */
    private static void eliminarCheckpoint(File checkpointFile) {
        if (checkpointFile.delete()) {
            System.out.println("Checkpoint eliminado correctamente.");
        } else {
            System.err.println("No se pudo eliminar el checkpoint.");
        }
    }

    /**
     * Clase para almacenar los datos de un checkpoint.
     */
    public static class CheckpointData implements Serializable {
        private static final long serialVersionUID = 1L;

        private final List<Genotype<DoubleGene>> genotypes; // Lista inmutable de genotipos.
        private final int generation; // Número de generación.

        /**
         * Constructor que inicializa los datos del checkpoint.
         *
         * @param population Lista de fenotipos del checkpoint.
         * @param generation Número de generación asociado al checkpoint.
         */
        public CheckpointData(List<Phenotype<DoubleGene, Double>> population, int generation) {
            this.genotypes = Collections.unmodifiableList(
                    population.stream()
                            .map(Phenotype::genotype)
                            .collect(Collectors.toList())
            );
            this.generation = generation;
        }

        /**
         * Obtiene la lista de genotipos asociados al checkpoint.
         *
         * @return Lista inmutable de genotipos.
         */
        public List<Genotype<DoubleGene>> getGenotypes() {
            return genotypes;
        }

        /**
         * Obtiene el número de generación asociado al checkpoint.
         *
         * @return Número de generación.
         */
        public int getGeneration() {
            return generation;
        }

        @Override
        public String toString() {
            return "CheckpointData{" +
                    "genotypes=" + genotypes +
                    ", generation=" + generation +
                    '}';
        }
    }
}
