import io.jenetics.Optimize;
public class Configuracion {
    public final boolean ENABLE_COLORS = true;
    public final String CHECKPOINT_FILE = "checkpoint.ser";
    public final boolean IS_HPC = (System.getenv("SLURM_JOB_ID") != null);
    
    // Aumenta la población para mayor diversidad
    public final int INITIAL_POPULATION_SIZE = IS_HPC ? 100 : 10;
    
    public final int DEFAULT_GENERATIONS = 1000;
    public final int TARGET_GENERATIONS = 1000;  
    
    // Aumenta el número de simulaciones para evaluar de forma robusta
    public final int NUM_SIMULACIONES = 4;
    
    // Ajusta tasas: incrementa mutación y aumenta ligeramente cruce en entorno local
    public final double MUTATION_RATE = 0.45;
    public final double CROSSOVER_RATE = IS_HPC ? 0.85 : 0.80;
    
    // Reducir número de élites para mayor exploración
    public final int ELITE_COUNT = 3;
    
    // Detectar estancamiento antes
    public final int MAX_GENERACIONES_SIN_MEJORA = 10;
    public final double THRESHOLD_MEJORA = 0.05;
    
    // Para la diversidad
    public final double MIN_DIVERSITY_THRESHOLD = 0.55;
    public final double DIVERSITY_INJECTION_PERCENTAGE = 0.4;
    
    // Intervalo para guardar checkpoints (podrías incrementarlo si el proceso es muy rápido)
    public final int CHECKPOINT_INTERVAL = 15;
    
    // Experimentar con cruce uniforme para mayor mezcla
    public final boolean USE_UNIFORM_CROSSOVER = true;
    
    public final Optimize OPTIMIZE = Optimize.MAXIMUM;
    public final int NUM_CORES = 8;
    
    public Configuracion() { }
    
    public Configuracion getConfig() {
        return this;
    }
}
