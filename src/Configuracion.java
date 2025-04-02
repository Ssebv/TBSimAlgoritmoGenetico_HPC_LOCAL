import io.jenetics.Optimize;

public class Configuracion {

    // ----------------------------
    // Parámetros de entorno y sistema
    // ----------------------------

    public boolean ENABLE_COLORS = false;
    public final boolean IS_HPC = System.getenv("SLURM_JOB_ID") != null;

    private int NUM_CORES = 8; 

    public int getNumCores() {
        return NUM_CORES;
    }

    public void setNumCores(int numCores) {
        if (numCores > 0) {
            this.NUM_CORES = numCores;
        }
    }

    // ----------------------------
    // Configuración del algoritmo genético
    // ----------------------------

    public final int INITIAL_POPULATION_SIZE = IS_HPC ? 50 : 300;
    public final int MAX_GENERATIONS = 1000;
    public final int MAX_GENERATIONS_PER_BLOCK = 50;
    public final int NUM_SIMULACIONES = 6;
    public final double MUTATION_RATE = 0.35;
    public final double CROSSOVER_RATE = IS_HPC ? 0.9 : 0.85;
    public final int ELITE_COUNT = 10;
    public final Optimize OPTIMIZE = Optimize.MAXIMUM;
    public final boolean USE_UNIFORM_CROSSOVER = false;

    // ----------------------------
    // Control de estancamiento
    // ----------------------------

    public final int MAX_GENERACIONES_SIN_MEJORA = 15;
    public final double THRESHOLD_MEJORA = 0.02;

    // ----------------------------
    // Gestión de diversidad
    // ----------------------------

    public final double MIN_DIVERSITY_THRESHOLD = 0.45;
    public final double DIVERSITY_INJECTION_PERCENTAGE = 0.3;

    public Configuracion() {}

    public Configuracion getConfig() {
        return this;
    }
}
