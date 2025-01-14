public class Configuracion {
    // Si deseas que sean todas constantes inmutables, podrías hacerlas static final.
    // Aquí las dejamos como campos para instanciarlas libremente.
    public final boolean ENABLE_COLORS = true;
    public final String CHECKPOINT_FILE = "checkpoint.ser";

    // Detectar HPC según variable de entorno
    public final boolean IS_HPC = (System.getenv("SLURM_JOB_ID") != null);

    // Tamaño de población según HPC o local
    public final int INITIAL_POPULATION_SIZE = IS_HPC ? 50 : 30;

    // Número de generaciones predeterminadas según HPC o local
    public final int DEFAULT_GENERATIONS = IS_HPC ? 2000 : 200;

    // Tasa de mutación y crossover según HPC o local
    public final double MUTATION_RATE = IS_HPC ? 0.4 : 0.3;
    public final double CROSSOVER_RATE = IS_HPC ? 0.85 : 0.8;

    // Parámetros para estancamiento (si los usas)
    public final int MAX_GENERACIONES_SIN_MEJORA = 5;
    public final double THRESHOLD_MEJORA = 0.01;

    // Variables de estado
    public volatile long currentGeneration = 0;
    public volatile boolean isRunning = false;

    // Constructor por defecto
    public Configuracion() {}
}
