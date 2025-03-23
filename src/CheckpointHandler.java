import io.jenetics.DoubleGene;
import io.jenetics.engine.EvolutionResult;
import java.io.*;

public class CheckpointHandler {
    private String checkpointFile;
    private CheckpointData checkpointData; // Para almacenar datos temporalmente si se desea

    public CheckpointHandler(String checkpointFile) {
        this.checkpointFile = checkpointFile;
    }

    // Método original (si lo necesitas)
    public void saveCheckpoint(EvolutionResult<DoubleGene, Double> result) {
        // Implementación original para EvolutionResult, si se requiere.
        // Puedes dejarlo vacío o registrar un mensaje si solo usarás el método sobrecargado.
    }

    // Sobrecarga para guardar un CheckpointData
    public void saveCheckpoint(CheckpointData data) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(checkpointFile))) {
            oos.writeObject(data);
            oos.flush();
            System.out.println("[DEBUG] Checkpoint guardado correctamente en " + checkpointFile);
        } catch (IOException e) {
            System.err.println("Error guardando el checkpoint: " + e.getMessage());
        }
    }

    // Método para cargar el checkpoint
    public CheckpointData loadCheckpoint() {
        File file = new File(checkpointFile);
        if (!file.exists()) {
            return null;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(checkpointFile))) {
            Object obj = ois.readObject();
            if (obj instanceof CheckpointData) {
                return (CheckpointData) obj;
            } else {
                System.err.println("Checkpoint inválido");
                return null;
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error cargando el checkpoint: " + e.getMessage());
            return null;
        }
    }

    // Setter para almacenar datos de checkpoint (si lo requieres)
    public void setCheckpointData(CheckpointData data) {
        this.checkpointData = data;
    }

    public String getCheckpointFile() {
        return checkpointFile;
    }
}
