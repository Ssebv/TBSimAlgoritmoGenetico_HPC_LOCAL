public class GenerationTracker {
    private static int currentGeneration = 0;

    public static void incrementGeneration() {
        currentGeneration++;
    }

    public static int getCurrentGeneration() {
        return currentGeneration;
    }

    public static void reset() {
        currentGeneration = 0;
    }
}
