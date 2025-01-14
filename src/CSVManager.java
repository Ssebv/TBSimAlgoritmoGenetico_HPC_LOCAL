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

    public CSVManager(String fileName, boolean isHPC) {
        this.fileName = fileName;
        prepararCSV(isHPC);
    }

    private void prepararCSV(boolean isHPC) {
        try {
            csvWriter = new PrintWriter(new FileWriter(fileName, true)); // Modo append

            File archivo = new File(fileName);
            if (archivo.length() == 0) {
                // Cabecera
                csvWriter.println(
                  "Generación,Mejor Fitness,Fitness Promedio,Diversidad," +
                  "Peor Fitness,CPU (%),Memoria (%),Tiempo (s),Goles Favor,Goles Contra"
                );
                csvWriter.flush();
            }

            LOGGER.info("Archivo CSV preparado correctamente: " + fileName);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "No se pudo crear o abrir el archivo CSV: " + e.getMessage(), e);
        }
    }

    public void escribirLineaCSV(
            int generacion,
            double mejorFitness,
            double avgFitness,
            double diversity,
            double worstFitness,
            double cpuLoad,
            double memoryLoad,
            long elapsedTime,
            int golesFavor,
            int golesContra
    ) {
        try {
            if (csvWriter == null) {
                LOGGER.warning("El escritor CSV no está inicializado. Se omite la escritura.");
                return;
            }

            // Debug
            System.out.println("[DEBUG CSV] Voy a escribir al CSV -> gen=" + generacion + ", bestFit=" + mejorFitness);

            csvWriter.printf(
                "%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d\n",
                generacion,
                mejorFitness,
                avgFitness,
                diversity,
                worstFitness,
                (cpuLoad * 100),
                (memoryLoad * 100),
                (elapsedTime / 1000.0),
                golesFavor,
                golesContra
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
