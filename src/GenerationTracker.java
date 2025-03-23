public class GenerationTracker {
    private static int currentGeneration = 0;
    
    public static synchronized int getCurrentGeneration() {
        return currentGeneration;
    }
    
    public static synchronized void incrementGeneration() {
        currentGeneration++;
    }
    
    public static synchronized void setCurrentGeneration(int generation) {
        currentGeneration = generation;
    }
}
