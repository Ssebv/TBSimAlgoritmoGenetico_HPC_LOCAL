import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

public class CpuSampler implements Runnable {
    private final CentralProcessor processor;
    private final ScheduledExecutorService scheduler;
    private final double[] acumuladoPorNucleo;
    private int muestras = 0;
    private volatile boolean running = true;
    private long[][] prevTicks;

    public CpuSampler() {
        SystemInfo systemInfo = new SystemInfo();
        this.processor = systemInfo.getHardware().getProcessor();
        this.prevTicks = processor.getProcessorCpuLoadTicks();
        this.acumuladoPorNucleo = new double[processor.getLogicalProcessorCount()];
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Inicia el muestreo periódico.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(this, 0, 200, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
        if (!running) return;

        try {
            double[] carga = processor.getProcessorCpuLoadBetweenTicks(prevTicks);
            prevTicks = processor.getProcessorCpuLoadTicks();

            for (int i = 0; i < carga.length; i++) {
                acumuladoPorNucleo[i] += carga[i];
            }
            muestras++;
        } catch (Exception e) {
            // Silenciar errores de OSHI para evitar logs de bajo nivel
        }
    }

    /**
     * Detiene el muestreo y apaga el scheduler.
     */
    public void stop() {
        running = false;
        scheduler.shutdownNow();  // Para forzar parada si hay tareas pendientes
    }

    /**
     * Devuelve el promedio de uso por núcleo.
     */
    public double[] getPromedioPorNucleo() {
        if (muestras == 0) return new double[acumuladoPorNucleo.length];
        double[] promedio = new double[acumuladoPorNucleo.length];
        for (int i = 0; i < acumuladoPorNucleo.length; i++) {
            promedio[i] = acumuladoPorNucleo[i] / muestras;
        }
        return promedio;
    }
}
