public class AdaptiveMutationController {
    private int stagnationCount = 0;
    // Reducir el umbral para que se activen los cambios más pronto
    private final int STAGNATION_THRESHOLD = 3;
    private final double baseMutationRate;
    private final double maxMutationRate;
    
    // Incremento más agresivo: usa 0.5 en lugar de 0.3
    private final double factorMultiplier = 0.5;
    
    public AdaptiveMutationController(double baseMutationRate, double maxMutationRate) {
        this.baseMutationRate = baseMutationRate;
        this.maxMutationRate = maxMutationRate;
    }
    
    // Llama a update(true) en generaciones sin mejora y update(false) si hay progreso
    public void update(boolean stagnant) {
        if (stagnant) {
            stagnationCount++;
        } else {
            stagnationCount = 0;
        }
    }
    
    public double getAdaptiveMutationRate() {
        if (stagnationCount >= STAGNATION_THRESHOLD) {
            double factor = Math.exp(factorMultiplier * (stagnationCount - STAGNATION_THRESHOLD + 1));
            double newRate = baseMutationRate * factor;
            return Math.min(newRate, maxMutationRate);
        }
        return baseMutationRate;
    }
    
    public int getStagnationCount() {
        return stagnationCount;
    }
}
