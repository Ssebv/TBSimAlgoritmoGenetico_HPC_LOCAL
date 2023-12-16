import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class CpuMonitor implements Runnable {
    private final OperatingSystemMXBean osBean;
    private volatile boolean running = true;
    private double totalCpuLoad = 0;
    private int samples = 0;

    public CpuMonitor() {
        osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    }

    @Override
    public void run() {
        while (running) {
            double cpuLoad = osBean.getProcessCpuLoad();
            if (cpuLoad != -1) {
                totalCpuLoad += cpuLoad;
                samples++;
            }
            try {
               // Thread.sleep(1000); // Espera un segundo entre muestras para no saturar el procesador
                pass();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private void pass() {
        return;
    }

    public void stop() {
        running = false;
    }

    public double getAverageCpuLoad() {
        return samples > 0 ? (totalCpuLoad / samples) : 0;
    }
}