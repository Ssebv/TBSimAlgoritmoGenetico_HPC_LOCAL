import io.jenetics.Optimize;

public class Configuracion {
    // Habilitar colores en los logs (ANSI)
    public final boolean ENABLE_COLORS = true;
    
    // Archivo para guardar checkpoints (estado del AG)
    public final String CHECKPOINT_FILE = "checkpoint.ser";
    
    // Detecta si se ejecuta en un entorno HPC (por ejemplo, usando SLURM)
    public final boolean IS_HPC = (System.getenv("SLURM_JOB_ID") != null);
    
    // Tamaño de población; se aumenta en entorno local para mayor diversidad
    public final int INITIAL_POPULATION_SIZE = IS_HPC ? 100 : 100;
    
    // Generaciones por bloque y objetivo global de generaciones
    public final int DEFAULT_GENERATIONS = 500;
    public final int TARGET_GENERATIONS = 500;
    
    // Número de simulaciones para evaluar cada individuo, para obtener estimaciones más robustas
    public final int NUM_SIMULACIONES = 2;
    
    // Tasas de mutación y cruce. Se incrementa la mutación base y se ajusta el cruce según el entorno.
    public final double MUTATION_RATE = 0.45;
    public final double CROSSOVER_RATE = IS_HPC ? 0.85 : 0.80;
    
    // Número de élites a conservar para fomentar la exploración (menos élites = más diversidad)
    public final int ELITE_COUNT = 3;
    
    // Parámetros para detectar estancamiento: si no se mejora en MAX_GENERACIONES_SIN_MEJORA generaciones
    // y la mejora es menor que THRESHOLD_MEJORA, se considera estancamiento.
    public final int MAX_GENERACIONES_SIN_MEJORA = 10;
    public final double THRESHOLD_MEJORA = 0.05;
    
    // Parámetros para diversidad: si la diversidad cae por debajo de este umbral, se inyectan nuevos individuos
    public final double MIN_DIVERSITY_THRESHOLD = 0.55;
    public final double DIVERSITY_INJECTION_PERCENTAGE = 0.4;
    
    // Intervalo de generaciones en el que se guarda un checkpoint
    public final int CHECKPOINT_INTERVAL = 15;
    
    // Selección de operador de cruce: usar cruce uniforme para mayor mezcla
    public final boolean USE_UNIFORM_CROSSOVER = true;
    
    // Estrategia de optimización (en este caso, maximización del fitness)
    public final Optimize OPTIMIZE = Optimize.MAXIMUM;
    
    // Número de núcleos a utilizar para la ejecución paralela
    public final int NUM_CORES = 8;
    
    public Configuracion() {
        // Constructor vacío (se pueden agregar inicializaciones adicionales si se requiere)
    }
    
    // Método de acceso a la configuración (útil para el patrón Singleton)
    public Configuracion getConfig() {
        return this;
    }
}
