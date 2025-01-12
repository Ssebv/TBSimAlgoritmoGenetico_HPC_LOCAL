import io.jenetics.DoubleGene;
import io.jenetics.Genotype;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Clase que encapsula los datos del estado de un checkpoint en un proceso evolutivo.
 * Proporciona genotipos, generación y configuraciones relevantes del checkpoint.
 */
public class CheckpointData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Genotype<DoubleGene>> genotypes; // Lista de genotipos asociados al checkpoint.
    private final int generation; // Número de generación al guardar el checkpoint.
    private final Map<String, Object> config; // Configuraciones adicionales.

    /**
     * Constructor para inicializar los datos del checkpoint.
     *
     * @param genotypes Lista de genotipos en el checkpoint.
     * @param generation Número de generación correspondiente.
     * @param config Configuración adicional asociada al checkpoint.
     */
    public CheckpointData(List<Genotype<DoubleGene>> genotypes, int generation, Map<String, Object> config) {
        this.genotypes = Collections.unmodifiableList(genotypes); // Inmutabilidad explícita.
        this.generation = generation;
        this.config = Collections.unmodifiableMap(config); // Inmutabilidad explícita.
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

    /**
     * Obtiene la configuración asociada al checkpoint.
     * 
     * @return Mapa inmutable con las configuraciones adicionales.
     */
    public Map<String, Object> getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return "CheckpointData{" +
                "genotypes=" + genotypes +
                ", generation=" + generation +
                ", config=" + config +
                '}';
    }
}
