import java.io.*;
import java.util.List;
import java.util.logging.*;
import java.util.stream.Collectors;

import io.jenetics.DoubleGene;
import io.jenetics.Phenotype;
import io.jenetics.engine.EvolutionResult;

public class CheckpointManager {
    private static final Logger LOGGER = Logger.getLogger(CheckpointManager.class.getName());

    static {
        configureLogger();
    }

    private static void configureLogger() {
        LOGGER.setUseParentHandlers(false);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return String.format("%s: %s\n", record.getLevel(), record.getMessage());
            }
        });
        LOGGER.addHandler(consoleHandler);
        LOGGER.setLevel(Level.ALL);
    }

    public static void guardarCheckpoint(EvolutionResult<DoubleGene, Double> result, String checkpointFilePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(checkpointFilePath))) {
            List<io.jenetics.Genotype<DoubleGene>> genotypes = result.population().asList().stream()
                    .map(Phenotype::genotype)
                    .collect(Collectors.toList());

            CheckpointData checkpointData = new CheckpointData(genotypes, (int) result.generation());
            oos.writeObject(checkpointData);
            LOGGER.info("[INFO] Checkpoint guardado correctamente en " + checkpointFilePath);
        } catch (IOException e) {
            LOGGER.severe("[ERROR] Error guardando el checkpoint: " + e.getMessage());
        }
    }

    public static CheckpointData cargarCheckpoint(String checkpointFilePath) {
        File f = new File(checkpointFilePath);
        if (!f.exists()) {
            LOGGER.severe("El archivo de checkpoint no existe en la ruta especificada.");
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(checkpointFilePath))) {
            CheckpointData checkpointData = (CheckpointData) ois.readObject();
            LOGGER.info("[INFO] Checkpoint cargado correctamente desde " + checkpointFilePath);
            return checkpointData;
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.severe("[ERROR] Error cargando el checkpoint: " + e.getMessage());
            return null;
        }
    }
}
