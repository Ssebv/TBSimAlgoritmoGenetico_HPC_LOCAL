public class GenerationTracker {
    private static int currentGeneration = 0;

    // Incrementa el contador de generaciones de forma sincronizada
    public static synchronized void incrementGeneration() {
        currentGeneration++;
    }

    // Retorna el contador actual de generaciones de forma sincronizada
    public static synchronized int getCurrentGeneration() {
        return currentGeneration;
    }
    
    // Método opcional para reiniciar el contador
    public static synchronized void reset() {
        currentGeneration = 0;
    }
}
