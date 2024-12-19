/*
 * FuncionEvaluacionJenetics.java
 * 
 * Función de evaluación para el algoritmo genético con Jenetics.
 * Ajustada con una función de fitness que premia victorias, goles a favor 
 * y penaliza goles en contra.
 */
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Chromosome;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.Semaphore;

public class FuncionEvaluacionJenetics {
    private static final Logger LOGGER = Logger.getLogger(FuncionEvaluacionJenetics.class.getName());

    // Constantes para los colores de los equipos
    private static final String FORECOLOR1 = "xEAEA00";
    private static final String BACKCOLOR1 = "xFFFFFF";
    private static final String FORECOLOR2 = "xFF0000";
    private static final String BACKCOLOR2 = "x0000FF";

    // Posiciones iniciales y configuraciones de los robots
    private static final double[] POSX = { -1.2, -0.5, -0.15, -0.15, -0.15, 1.2, 0.5, 0.15, 0.15, 0.15 };
    private static final double[] POSY = { 0, 0, 0.5, 0, -0.5, 0, 0, 0.5, 0, -0.5 };
    private static final double[] THETA = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    private static final int[] VCLAS = { 1, 1, 1, 1, 1, 2, 2, 2, 2, 2 };

    /**
     * Evalúa un genotipo ejecutando la simulación y calculando su fitness.
     *
     * @param genotype Genotipo a evaluar.
     * @return Valor de fitness.
     */
    public double evaluar(Genotype<DoubleGene> genotype) throws InterruptedException {
        if (genotype.length() == 0 || genotype.get(0).length() == 0) {
            LOGGER.warning("Genotipo vacío. Retornando -2 como fitness.");
            return -2.0;
        }

        // Configuración de los robots
        NewRobotSpec[] new_robots = new NewRobotSpec[10];
        for (int i = 0; i < 5; i++) {
            new_robots[i] = new NewRobotSpec(
                "EDU.gatech.cc.is.abstractrobot.SocSmallSim",
                "BasicTeamAG",
                POSX[i],
                POSY[i],
                THETA[i],
                FORECOLOR1,
                BACKCOLOR1,
                VCLAS[i]
            );
        }
        for (int i = 5; i < 10; i++) {
            new_robots[i] = new NewRobotSpec(
                "EDU.gatech.cc.is.abstractrobot.SocSmallSim",
                "AIKHomoG",
                POSX[i],
                POSY[i],
                THETA[i],
                FORECOLOR2,
                BACKCOLOR2,
                VCLAS[i]
            );
        }

        // Inicialización de la simulación sin gráficos
        TBSimNoGraphics tb = new TBSimNoGraphics(null, "robocup.dsc", new_robots, 3, 2, 50);
        tb.start();
        tb.sem1 = new Semaphore(0);

        try {
            tb.sem1.acquire();
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Hilo interrumpido mientras esperaba inicio de la simulación.", e);
            Thread.currentThread().interrupt();
            return -1.0;
        }

        // Conversión del genotipo a parámetros
        Double[] params = convertGenotypeToParams(genotype);

        // Asignación de parámetros a los robots
        for (int i = 0; i < 5; i++) {
            BasicTeamAG robot = (BasicTeamAG) (tb.simulation.control_systems[i]);
            robot.setParam(params);
        }

        // Liberación del semáforo para iniciar la simulación
        tb.sem2.release();

        try {
            tb.join();
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Hilo interrumpido esperando el fin de la simulación.", e);
            Thread.currentThread().interrupt();
            return -1.0;
        }

        // Cálculo del resultado de la simulación
        return calcularResultado(tb);
    }

    /**
     * Convierte el genotipo a un arreglo de parámetros Double.
     *
     * @param genotype Genotipo a convertir.
     * @return Arreglo de parámetros Double.
     */
    public Double[] convertGenotypeToParams(Genotype<DoubleGene> genotype) {
        return genotype.stream()
                .flatMap(Chromosome::stream)
                .limit(50)
                .map(DoubleGene::doubleValue)
                .toArray(Double[]::new);
    }

    /**
     * Calcula el fitness basado en el resultado de la simulación.
     *
     * @param tb Instancia de la simulación.
     * @return Valor de fitness.
     */
    public double calcularResultado(TBSimNoGraphics tb) {
        String[] lines = tb.estado.split("\n");

        // Buscar la última línea no vacía
        String finalLine = Arrays.stream(lines)
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .reduce((first, second) -> second)
                .orElse(null);

        if (finalLine == null) {
            LOGGER.warning("No se encontró ninguna línea no vacía en el estado. Retornando 0.");
            return 0.0;
        }

        LOGGER.fine("Línea final del estado: '" + finalLine + "'");

        String[] lst = finalLine.split(",");
        if (lst.length < 2) {
            LOGGER.warning("Formato de resultado inesperado. Retornando 0.");
            return 0.0;
        }

        try {
            int golesFavor = Integer.parseInt(lst[0].trim());
            int golesContra = Integer.parseInt(lst[1].trim());
            int diff = golesFavor - golesContra;

            LOGGER.info(String.format("Goles a Favor: %d, Goles en Contra: %d, Diferencia: %d", 
                golesFavor, golesContra, diff));

            return calcularFitness(golesFavor, golesContra, diff);
        } catch (NumberFormatException e) {
            LOGGER.warning("Error al parsear los goles. Retornando 0.");
            return 0.0;
        }
    }

    /**
     * Calcula el valor de fitness basado en los goles a favor, en contra y su diferencia.
     *
     * @param golesFavor Número de goles a favor.
     * @param golesContra Número de goles en contra.
     * @param diff Diferencia de goles.
     * @return Valor de fitness.
     */
    private double calcularFitness(int golesFavor, int golesContra, int diff) {
        if (diff > 0) {
            // Penaliza los goles en contra y recompensa los goles a favor
            return 1000 + (diff * 20) + (golesFavor * 3) - (golesContra * 5);
        } else if (diff == 0) {
            return 900 + (golesFavor * 3) - (golesContra * 5);
        } else {
            return 700 + (diff * 10) - (golesContra * 5);
        }
    }
}
