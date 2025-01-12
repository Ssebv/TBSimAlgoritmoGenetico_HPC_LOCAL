public class Configuracion {
    public final boolean ENABLE_COLORS = true;
    public final String CHECKPOINT_FILE = "checkpoint.ser";
    public final boolean IS_HPC = System.getenv("SLURM_JOB_ID") != null;
    public final int INITIAL_POPULATION_SIZE = IS_HPC ? 50 : 30;
    public final int DEFAULT_GENERATIONS = IS_HPC ? 2000 : 100;
    public final double MUTATION_RATE = IS_HPC ? 0.4 : 0.2;
    public final double CROSSOVER_RATE = IS_HPC ? 0.85 : 0.9;
    public final int MAX_GENERACIONES_SIN_MEJORA = 5;
    public final double THRESHOLD_MEJORA = 0.01;
    public volatile long currentGeneration = 0;
}
