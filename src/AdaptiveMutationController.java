public class AdaptiveMutationController {
    private int stagnationCount = 0;
    private final int STAGNATION_THRESHOLD = 5;
    private final double baseMutationRate;
    private final double maxMutationRate;
    
    public AdaptiveMutationController(double baseMutationRate, double maxMutationRate) {
        this.baseMutationRate = baseMutationRate;
        this.maxMutationRate = maxMutationRate;
    }
    
    public void update(boolean stagnant) {
        if (stagnant) {
            stagnationCount++;
        } else {
            stagnationCount = 0;
        }
    }
    
    public double getAdaptiveMutationRate() {
        if (stagnationCount >= STAGNATION_THRESHOLD) {
            double factor = Math.exp(0.3 * (stagnationCount - STAGNATION_THRESHOLD + 1));
            double newRate = baseMutationRate * factor;
            return Math.min(newRate, maxMutationRate);
        }
        return baseMutationRate;
    }
    
    public int getStagnationCount() {
        return stagnationCount;
    }
}
