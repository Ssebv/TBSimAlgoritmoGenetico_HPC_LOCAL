import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;

/**
 * RuntimeConfig.java
 * Clase utilitaria para obtener métricas de sistema en tiempo real.
 */
public class RuntimeConfig {

    private static final SystemInfo systemInfo = new SystemInfo();
    private static final CentralProcessor processor = systemInfo.getHardware().getProcessor();
    private static final GlobalMemory memory = systemInfo.getHardware().getMemory();

    private static long[] previousSystemTicks = processor.getSystemCpuLoadTicks();
    private static long[][] previousPerCoreTicks = processor.getProcessorCpuLoadTicks();

    private static final int SLEEP_MS = 100;

    /**
     * Devuelve el uso total de CPU (entre 0 y 1).
     */
    public static double getSystemCpuLoad() {
        safeSleep(SLEEP_MS);
        double load = processor.getSystemCpuLoadBetweenTicks(previousSystemTicks);
        previousSystemTicks = processor.getSystemCpuLoadTicks();
        return load;
    }

    /**
     * Devuelve el uso de CPU por núcleo (array de valores entre 0 y 1).
     */
    public static double[] getPerCoreLoad() {
        safeSleep(SLEEP_MS);
        double[] loads = processor.getProcessorCpuLoadBetweenTicks(previousPerCoreTicks);
        previousPerCoreTicks = processor.getProcessorCpuLoadTicks();
        return loads;
    }

    /**
     * Devuelve el uso de memoria RAM (entre 0 y 1).
     */
    public static double getSystemMemoryLoad() {
        long total = memory.getTotal();
        long used = total - memory.getAvailable();
        return (double) used / total;
    }

    /**
     * Utilidad para evitar repetir try/catch en todos los métodos.
     */
    private static void safeSleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static CentralProcessor getProcessor() {
        return processor;
    }

    public static GlobalMemory getMemory() {
        return memory;
    }

    public static SystemInfo getSystemInfo() {
        return systemInfo;
    }
}