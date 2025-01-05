/*
 * CpuUsageMonitor.java
 *
 * Utilidad para obtener el uso de CPU del proceso en sistemas tipo Unix
 * donde com.sun.management.UnixOperatingSystemMXBean esté disponible.
 * Retorna un valor entre 0.0 (0%) y 1.0 (100%), o -1 si no está disponible.
 */

 import java.lang.management.ManagementFactory;
 import java.lang.management.OperatingSystemMXBean;
 import com.sun.management.UnixOperatingSystemMXBean;
 
 public class CpuUsageMonitor {
     private static OperatingSystemMXBean osBean =
             ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
 
     public static double getProcessCpuLoad() {
         if (osBean instanceof UnixOperatingSystemMXBean) {
             return ((UnixOperatingSystemMXBean) osBean).getProcessCpuLoad();
         } else {
             return -1;
         }
     }
 }
 