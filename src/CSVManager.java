import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.jenetics.DoubleGene;
import io.jenetics.Genotype;

/**
 * CSVManager gestiona la creación y escritura segura de datos de la simulación
 * evolutiva en un archivo CSV para posteriores análisis.
 */
public class CSVManager {

    private static final Logger LOGGER = Logger.getLogger(CSVManager.class.getName());
    private static final int MAX_CORES_TO_LOG = 8;

    private final PrintWriter csvWriter;
    private final String fileName;
    private final Configuracion config;
    private final Object lock;

    public CSVManager(String fileName, boolean isHPC, Configuracion config, Object sharedLock) {
        this.fileName = fileName;
        this.config = config;
        this.lock = sharedLock;
        this.csvWriter = prepararCSV(isHPC);
    }

    /**
     * Prepara el archivo CSV escribiendo los encabezados, eliminando la columna
     * "Chromosoma".
     */
    private PrintWriter prepararCSV(boolean isHPC) {
        synchronized (lock) {
            try {
                File archivo = new File(fileName);
                PrintWriter writer = new PrintWriter(new FileWriter(archivo, false));

                StringBuilder header = new StringBuilder();
                header.append(String.join(",", new String[] {
                        "Generación", "Mejor Fitness Generación", "Fitness Global", "Fitness Promedio",
                        "Diversidad", "Peor Fitness", "CPU (%)", "Memoria (%)", "Tiempo (s)",
                        "Goles Favor", "Goles Contra",
                        "OS", "OS Version", "Java Version", "OS Arquitectura",
                        "CPUs (configurados)", "Population Size", "Mutation Rate", "Crossover Rate"
                }));

                for (int i = 0; i < MAX_CORES_TO_LOG; i++) {
                    header.append(",Core").append(i).append(" (%)");
                }

                writer.println(header);
                writer.flush();

                LOGGER.info("✅ CSV creado: " + archivo.getAbsolutePath());
                return writer;
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "❌ No se pudo crear el archivo CSV: " + e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Escribe una línea de resultados en el CSV, eliminando el cromosoma.
     */
    public void escribirLineaCSV(
            int generacion,
            double mejorFitnessGen,
            double fitnessGlobal,
            double avgFitness,
            double diversity,
            double worstFitness,
            double cpuLoad,
            double memoryLoad,
            long elapsedTime,
            int golesFavor,
            int golesContra,
            double[] coreLoads) {

        synchronized (lock) {
            try {
                StringBuilder sb = new StringBuilder();

                // Obtener los valores del sistema
                String osName = System.getProperty("os.name");
                String osVersion = System.getProperty("os.version");
                String javaVersion = System.getProperty("java.version");
                String osArch = System.getProperty("os.arch");
                String numCores = String.valueOf(config.getNumCores());

                // Escribimos la línea sin el cromosoma
                sb.append(String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d,%s,%s,%s,%s,%s,%d,%.2f,%.2f",
                        generacion,
                        mejorFitnessGen,
                        fitnessGlobal,
                        avgFitness,
                        diversity,
                        worstFitness,
                        cpuLoad * 100,
                        memoryLoad * 100,
                        elapsedTime / 1000.0,
                        golesFavor,
                        golesContra,
                        osName,
                        osVersion,
                        javaVersion,
                        osArch,
                        numCores,
                        config.INITIAL_POPULATION_SIZE,
                        config.MUTATION_RATE,
                        config.CROSSOVER_RATE));

                // Agregar los valores de carga de cada núcleo al CSV
                for (int i = 0; i < MAX_CORES_TO_LOG; i++) {
                    if (i < coreLoads.length) {
                        sb.append(String.format(",%.2f", coreLoads[i] * 100)); // Si hay datos, lo agregamos
                    } else {
                        sb.append(",0.00"); // Si no hay datos, agregamos 0.00
                    }
                }

                // Escribir la línea en el archivo CSV
                csvWriter.println(sb);
                csvWriter.flush();

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "❌ Error escribiendo en CSV: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Cierra el archivo CSV correctamente.
     */
    public void cerrarCSV() {
        synchronized (lock) {
            try {
                csvWriter.flush();
                csvWriter.close();
                LOGGER.info("📁 CSV cerrado correctamente.");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "⚠️ Error al cerrar el archivo CSV: " + e.getMessage(), e);
            }
        }
    }
}
