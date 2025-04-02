/**
 * AdaptiveMutationController.java
 *
 * Controlador para ajustar dinámicamente la tasa de mutación en un algoritmo evolutivo
 * en función del estancamiento del fitness.
 */
public class AdaptiveMutationController {

    // === Configuración ===
    private final int STAGNATION_THRESHOLD = 10;          // Número de generaciones sin mejora antes de ajustar
    private final double baseMutationRate;               // Tasa base de mutación
    private final double maxMutationRate;                // Tasa máxima permitida
    private final double escalationFactor = 0.3;         // Factor de incremento exponencial

    // === Estado interno ===
    private int stagnationCount = 0;                     // Cuántas generaciones sin mejora han ocurrido

    /**
     * Constructor principal.
     *
     * @param baseMutationRate Tasa base de mutación (por ejemplo, 0.05)
     * @param maxMutationRate Tasa máxima permitida (por ejemplo, 0.5)
     */
    public AdaptiveMutationController(double baseMutationRate, double maxMutationRate) {
        this.baseMutationRate = baseMutationRate;
        this.maxMutationRate = maxMutationRate;
    }

    /**
     * Informa al controlador si la generación actual no presentó mejoras.
     *
     * @param isStagnant true si no hubo mejora de fitness, false si hubo mejora
     */
    public void update(boolean isStagnant) {
        if (isStagnant) {
            stagnationCount++;
        } else {
            stagnationCount = 0; // Reiniciar si hubo progreso
        }
    }

    /**
     * Calcula la tasa de mutación adaptativa basada en el estancamiento.
     *
     * @return Tasa de mutación ajustada
     */
    public double getAdaptiveMutationRate() {
        if (stagnationCount >= STAGNATION_THRESHOLD) {
            double factor = Math.exp(escalationFactor * (stagnationCount - STAGNATION_THRESHOLD + 1));
            return Math.min(baseMutationRate * factor, maxMutationRate);
        }
        return baseMutationRate;
    }

    /**
     * Devuelve el número actual de generaciones estancadas.
     *
     * @return número de generaciones consecutivas sin mejora
     */
    public int getStagnationCount() {
        return stagnationCount;
    }
}
