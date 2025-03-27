import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

public class RuntimeConfig {
    // Instancias de OSHI
    private static final SystemInfo systemInfo = new SystemInfo();
    private static final HardwareAbstractionLayer hal = systemInfo.getHardware();
    private static final CentralProcessor processor = hal.getProcessor();

    /**
     * Retorna la carga de CPU de cada núcleo en forma de arreglo de doubles (valores entre 0 y 1).
     */
    public static double[] getCpuLoadPerCore() {
        // Capturamos los ticks de CPU para cada núcleo antes de la medición
        long[][] prevTicks = processor.getProcessorCpuLoadTicks();
        // Esperamos 1 segundo para medir la carga
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Capturamos los ticks después del intervalo
        long[][] currTicks = processor.getProcessorCpuLoadTicks();
        // Devuelve la carga de CPU por núcleo utilizando ambos arrays
        return processor.getProcessorCpuLoadBetweenTicks(prevTicks);
    }

    /**
     * Retorna la carga de CPU promedio de todos los núcleos (valor entre 0 y 1).
     */
    public static double getAverageCpuLoad() {
        double[] loads = getCpuLoadPerCore();
        double sum = 0.0;
        for (double load : loads) {
            sum += load;
        }
        return loads.length > 0 ? sum / loads.length : -1;
    }

    /**
     * Método agregado para compatibilidad: retorna la carga del sistema.
     * En este ejemplo, se utiliza la carga promedio de CPU.
     */
    public static double getSystemCpuLoad() {
        return getAverageCpuLoad();
    }

    /**
     * Retorna la carga de memoria del sistema (valor entre 0 y 1).
     */
    public static double getSystemMemoryLoad() {
        long totalMemory = hal.getMemory().getTotal();
        long availableMemory = hal.getMemory().getAvailable();
        if (totalMemory > 0) {
            return 1.0 - ((double) availableMemory / totalMemory);
        }
        return -1;
    }

    /**
     * Método principal para pruebas rápidas.
     */
    public static void main(String[] args) {
        double cpuLoad = getSystemCpuLoad();
        double memLoad = getSystemMemoryLoad();
        System.out.printf("Carga de CPU promedio: %.2f%%%n", cpuLoad * 100);
        System.out.printf("Carga de Memoria: %.2f%%%n", memLoad * 100);
    }
}
