import io.jenetics.Optimize;


public class Configuracion {
    // Configuración de colores para los logs (true para habilitar ANSI colors)
    public final boolean ENABLE_COLORS = true;
    
    // Archivo para guardar los checkpoints
    public final String CHECKPOINT_FILE = "checkpoint.ser";
    
    // Detección de entorno HPC mediante variable de entorno (SLURM_JOB_ID)
    public final boolean IS_HPC = (System.getenv("SLURM_JOB_ID") != null);
    
    // Tamaño de población
    public final int INITIAL_POPULATION_SIZE = IS_HPC ? 100 : 10;
    
    // Número de generaciones por bloque y objetivo global
    public final int DEFAULT_GENERATIONS = 50;
    public final int TARGET_GENERATIONS = 50;  // Ajusta este valor según tu experimento
    
    // Número de partidos que se simulan para evaluar cada individuo.
    public final int NUM_SIMULACIONES = 1;
    
    // Tasas de mutación y de cruce
    public final double MUTATION_RATE = 0.4;
    public final double CROSSOVER_RATE = IS_HPC ? 0.85 : 0.7;
    
    // Número de élites a conservar
    public final int ELITE_COUNT = 5;
    
    // Parámetros para detectar estancamiento
    public final int MAX_GENERACIONES_SIN_MEJORA = 15;
    public final double THRESHOLD_MEJORA = 0.01;
    
    // Parámetros para la diversidad
    public final double MIN_DIVERSITY_THRESHOLD = 0.5;
    public final double DIVERSITY_INJECTION_PERCENTAGE = 0.1;
    
    // Intervalo para guardar checkpoints
    public final int CHECKPOINT_INTERVAL = 10;
    
    // Flag para elegir operador de cruce: true para Uniform, false para SinglePoint
    public final boolean USE_UNIFORM_CROSSOVER = false;
    
    // Estrategia de optimización (MAXIMUM para maximización, MINIMUM para minimización)
    public final Optimize OPTIMIZE = Optimize.MAXIMUM;
    
    // Número de núcleos a utilizar para la ejecución paralela (puede ser 1, 2, 4, 8, etc.)
    public final int NUM_CORES = 1;  // Cambia este valor según el experimento
    
    public Configuracion() {
        // Inicializaciones adicionales si fueran necesarias.
    }
    
    // Método para acceder a la configuración (útil en el Singleton)
    public Configuracion getConfig() {
        return this;
    }
}
