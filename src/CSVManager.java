import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CSVManager {
    private static final Logger LOGGER = Logger.getLogger(CSVManager.class.getName());
    private PrintWriter csvWriter;
    private final String fileName;
    private final Configuracion config;

    public CSVManager(String fileName, boolean isHPC, Configuracion config) {
        this.fileName = fileName;
        this.config = config;
        prepararCSV(isHPC);
    }

    /**
     * Prepara el archivo CSV escribiendo la cabecera, incluyendo información del sistema
     * y columnas para Mejor Fitness Generación, Fitness Global y datos adicionales del OS.
     */
    private void prepararCSV(boolean isHPC) {
        try {
            csvWriter = new PrintWriter(new FileWriter(fileName, false));
            File archivo = new File(fileName);
            csvWriter.println(
                "Generación,Mejor Fitness Generación,Fitness Global,Fitness Promedio,Diversidad,Peor Fitness,CPU (%),Memoria (%),Tiempo (s),Goles Favor,Goles Contra," +
                "OS,OS Version,Java Version,OS Arquitectura,CPUs (configurados),Population Size,Mutation Rate,Crossover Rate"
            );
            csvWriter.flush();
            LOGGER.info("Archivo CSV preparado correctamente (sobrescrito): " + archivo.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "No se pudo crear o abrir el archivo CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Escribe una línea en el CSV con los datos de la generación.
     */
    public synchronized void escribirLineaCSV(
            int generacion,
            double mejorFitnessGen,
            double fitnessGlobal,
            double avgFitness,
            double diversity,
            double worstFitness,
            double cpuLoad,
            double memoryLoad,
            long elapsedTime, // En milisegundos
            int golesFavor,
            int golesContra
    ) {
        try {
            System.out.println("[DEBUG CSV] Escribiendo CSV -> gen=" + generacion + 
                               ", Mejor Fitness Generación=" + mejorFitnessGen + 
                               ", Fitness Global=" + fitnessGlobal);
            // Convertir tiempo a segundos
            double timeSeconds = elapsedTime / 1000.0;
            // Información del sistema
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            String javaVersion = System.getProperty("java.version");
            String osArch = System.getProperty("os.arch"); // Arquitectura del OS
            int cpusConfigurados = config.NUM_CORES;  // Núcleos configurados
            int populationSize = config.INITIAL_POPULATION_SIZE;
            double mutationRate = config.MUTATION_RATE;
            double crossoverRate = config.CROSSOVER_RATE;
            
            csvWriter.printf(
                "%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.3f,%.3f,%d,%d," +
                "%s,%s,%s,%s,%d,%d,%.2f,%.2f\n",
                generacion,
                mejorFitnessGen,
                fitnessGlobal,
                avgFitness,
                diversity,
                worstFitness,
                (cpuLoad * 100),
                (memoryLoad * 100),
                timeSeconds,
                golesFavor,
                golesContra,
                osName,
                osVersion,
                javaVersion,
                osArch,
                cpusConfigurados,
                populationSize,
                mutationRate,
                crossoverRate
            );
            csvWriter.flush();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al escribir datos en el archivo CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Cierra el archivo CSV, liberando los recursos.
     */
    public void cerrarCSV() {
        if (csvWriter != null) {
            try {
                csvWriter.flush();
                csvWriter.close();
                LOGGER.info("Archivo CSV cerrado correctamente.");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al cerrar el archivo CSV: " + e.getMessage(), e);
            }
        }
    }
}
