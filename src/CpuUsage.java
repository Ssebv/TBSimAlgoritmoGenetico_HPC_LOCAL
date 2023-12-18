import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CpuUsage {
    private double usage;

    public CpuUsage(double usage) {
        this.usage = usage;
    }

    public double getUsage() {
        return this.usage;
    }

    public static CpuUsage getCpuUsage() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("top", "-l", "1");
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("CPU usage:")) {
                    String[] parts = line.split(",");
                    for (String part : parts) {
                        if (part.contains("user")) {
                            String userCpuStr = part.split(" ")[2];
                            double userCpu = Double.parseDouble(userCpuStr.replace("%", ""));
                            return new CpuUsage(userCpu);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new CpuUsage(0);
    } 
}