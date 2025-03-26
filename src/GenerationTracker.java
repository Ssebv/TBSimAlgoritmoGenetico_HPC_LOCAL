public class GenerationTracker {
    // Contador global de generaciones
    private static int currentGeneration = 0;

    // Incrementa de forma sincronizada para evitar condiciones de carrera
    public static synchronized void incrementGeneration() {
        currentGeneration++;
    }

    // Obtiene el número actual de generaciones
    public static synchronized int getCurrentGeneration() {
        return currentGeneration;
    }
    
    // Método opcional para reiniciar el contador
    public static synchronized void reset() {
        currentGeneration = 0;
    }
}
