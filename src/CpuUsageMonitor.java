import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import com.sun.management.UnixOperatingSystemMXBean;

public class CpuUsageMonitor {
    private static OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    // Method to get the CPU load from the JVM
    public static double getProcessCpuLoad() {
        if (osBean instanceof UnixOperatingSystemMXBean) {
            return ((UnixOperatingSystemMXBean) osBean).getProcessCpuLoad();
        } else {
            // Handle the case for other operating systems or errors
            return -1;
        }
    }
}