import java.io.*;
import java.util.logging.Logger;

public class MainJenetics {

    private static final Logger LOGGER = Logger.getLogger(MainJenetics.class.getName());
    private static final Object SHARED_LOCK = new Object();
    private static final String LOG_FILE = "simulacion.log";

    static {
        // 🧹 Borrar log anterior ANTES DE TODO
        try {
            File log = new File(LOG_FILE);
            if (log.exists())
                log.delete();
        } catch (Exception ignored) {}

        // 🔇 Silenciar SLF4J y otras librerías
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");

        // 🔄 Redirigir stdout y stderr al archivo inmediatamente
        try {
            PrintStream out = new PrintStream(new FileOutputStream(LOG_FILE, true), true);
            System.setOut(out);
            System.setErr(out);
        } catch (Exception e) {
            System.err.println("❌ No se pudo redirigir salida al log: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        CSVManager csvManager = null;
        LogManager logManager = null;

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("💥 Excepción no capturada en " + thread.getName() + ": " + throwable.getMessage());
            throwable.printStackTrace();
        });

        try {
            // ⚙️ Obtener configuración
            Configuracion config = ConfiguracionSingleton.getInstance();

            // 📝 Iniciar Logger personalizado
            logManager = new LogManager(Logger.getLogger(MainJenetics.class.getName()), config.ENABLE_COLORS, config);
            logManager.logInfo("📂 Iniciando MainJenetics...");

            // 🔧 Aplicar núcleos desde línea de comandos o archivo
            cargarConfiguracionDinamica(config, args, logManager);

            boolean isHPC = config.IS_HPC;
            String csvFileName = isHPC ? "hpc_stats.csv" : "local_stats.csv";
            File csvFile = new File(csvFileName);
            logManager.logInfo("[DEBUG] Archivo CSV -> " + csvFile.getAbsolutePath());

            // 📊 Crear CSV
            csvManager = new CSVManager(csvFileName, isHPC, config, SHARED_LOCK);

            // 🔍 Detalles del entorno
            logManager.logDetallesDelEntorno(isHPC);

            // 🚀 Ejecutar motor evolutivo
            EvolutionManager evolutionManager = new EvolutionManager(csvManager,
                    Logger.getLogger(MainJenetics.class.getName()), config);
            logManager.logInfo("🧬 Iniciando proceso evolutivo...");
            evolutionManager.runGeneticEngine();
            logManager.logInfo("✅ Evolución completada exitosamente.");

        } catch (Exception e) {
            System.err.println("❌ Error en ejecución principal: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (csvManager != null)
                csvManager.cerrarCSV();

            if (logManager != null) {
                logManager.logInfo("📦 Recursos cerrados correctamente.");
                logManager.shutdown();
            }
        }
    }

    private static void cargarConfiguracionDinamica(Configuracion config, String[] args, LogManager logManager) {
        for (String arg : args) {
            if (arg.startsWith("--cores=")) {
                try {
                    int dynamicCores = Integer.parseInt(arg.split("=")[1]);
                    config.setNumCores(dynamicCores);
                    logManager.logInfo("💻 NUM_CORES configurado desde línea de comandos: " + dynamicCores);
                } catch (NumberFormatException e) {
                    logManager.logWarning("⚠️ Número de núcleos no válido, se usará el valor por defecto.");
                }
            }
        }

        String propCores = System.getProperty("cores");
        if (propCores != null) {
            try {
                int cores = Integer.parseInt(propCores);
                config.setNumCores(cores);
                logManager.logInfo("🧩 NUM_CORES configurado desde config.properties: " + propCores);
            } catch (NumberFormatException e) {
                logManager.logWarning("⚠️ Valor inválido en config.properties. Usando valor por defecto.");
            }
        }
    }
}
