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
        // Sobrescribir el archivo en cada ejecución
        prepararCSV(isHPC);
    }

    private void prepararCSV(boolean isHPC) {
        try {
            csvWriter = new PrintWriter(new FileWriter(fileName, false));
            File archivo = new File(fileName);
            csvWriter.println(
                "Generación,Mejor Fitness,Fitness Promedio,Diversidad,Peor Fitness,CPU (%),Memoria (%),Tiempo (s),Goles Favor,Goles Contra," +
                "OS,OS Version,Java Version,CPUs,Population Size,Mutation Rate,Crossover Rate"
            );
            csvWriter.flush();
            LOGGER.info("Archivo CSV preparado correctamente (sobrescrito): " + fileName);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "No se pudo crear o abrir el archivo CSV: " + e.getMessage(), e);
        }
    }

    public synchronized void escribirLineaCSV(
            int generacion,
            double mejorFitness,
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
            System.out.println("[DEBUG CSV] Voy a escribir al CSV -> gen=" + generacion + ", bestFit=" + mejorFitness);
            double timeSeconds = elapsedTime / 1000.0;
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            String javaVersion = System.getProperty("java.version");
            int cpus = Runtime.getRuntime().availableProcessors();
            int populationSize = config.INITIAL_POPULATION_SIZE;
            double mutationRate = config.MUTATION_RATE;
            double crossoverRate = config.CROSSOVER_RATE;
            
            csvWriter.printf(
                "%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.3f,%d,%d," +
                "%s,%s,%s,%d,%d,%.2f,%.2f\n",
                generacion,
                mejorFitness,
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
                cpus,
                populationSize,
                mutationRate,
                crossoverRate
            );
            csvWriter.flush();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al escribir datos en el archivo CSV: " + e.getMessage(), e);
        }
    }

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
