import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class RuntimeConfig {
    private static final OperatingSystemMXBean osBean =
            ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    private static final com.sun.management.OperatingSystemMXBean sunOsBean =
            (osBean instanceof com.sun.management.OperatingSystemMXBean)
            ? (com.sun.management.OperatingSystemMXBean) osBean
            : null;

    public static double getSystemMemoryLoad() {
        if (sunOsBean != null) {
            long totalMemory = sunOsBean.getTotalMemorySize();
            long freeMemory = sunOsBean.getFreeMemorySize();
            if (totalMemory > 0) {
                return 1.0 - (double) freeMemory / totalMemory;
            }
        }
        return -1;
    }

    public static double getSystemCpuLoad() {
        if (sunOsBean != null) {
            double cpuLoad = sunOsBean.getCpuLoad();
            return (cpuLoad >= 0) ? cpuLoad : -1;
        }
        return -1;
    }
}
