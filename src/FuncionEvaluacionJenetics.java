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
import io.jenetics.engine.EvolutionResult;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;


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

    private int lastGolesFavor = 0;
    private int lastGolesContra = 0;

    public double evaluar(Genotype<DoubleGene> genotype) throws InterruptedException {
        if (genotype.length() == 0 || genotype.get(0).length() == 0) {
            LOGGER.warning("Genotipo vacío. Retornando -2 como fitness.");
            return -2.0;
        }

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
                    VCLAS[i]);
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
                    VCLAS[i]);
        }

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

        Double[] params = convertGenotypeToParams(genotype);

        for (int i = 0; i < 5; i++) {
            BasicTeamAG robot = (BasicTeamAG) (tb.simulation.control_systems[i]);
            robot.setParam(params);
        }

        tb.sem2.release();

        try {
            tb.join();
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Hilo interrumpido esperando el fin de la simulación.", e);
            Thread.currentThread().interrupt();
            return -1.0;
        }

        return ejecutarSimulacion(tb);
    }

    private double ejecutarSimulacion(TBSimNoGraphics simulacion) {
        try {
            if (simulacion.estado == null || simulacion.estado.isEmpty()) {
                return 0.0;
            }

            String[] lines = simulacion.estado.split("\n");
            if (lines.length == 0) {
                return 0.0;
            }

            String ultimaLinea = Arrays.stream(lines)
                    .filter(line -> !line.isBlank())
                    .reduce((a, b) -> b)
                    .orElse(null);

            if (ultimaLinea == null) {
                return 0.0;
            }

            String[] valores = ultimaLinea.split(",");
            if (valores.length < 2) {
                return 0.0;
            }

            int golesFavor = Integer.parseInt(valores[0].trim());
            int golesContra = Integer.parseInt(valores[1].trim());
            int diff = golesFavor - golesContra;

            lastGolesFavor = golesFavor;
            lastGolesContra = golesContra;

            return calcularFitness(golesFavor, golesContra, diff);

        } catch (NumberFormatException e) {
            LOGGER.warning("Error al parsear los resultados del estado: " + e.getMessage());
            return 0.0;
        }
    }

    public Double[] convertGenotypeToParams(Genotype<DoubleGene> genotype) {
        return genotype.stream()
                .flatMap(Chromosome::stream)
                .limit(50)
                .map(DoubleGene::doubleValue)
                .toArray(Double[]::new);
    }

    public double calcularFitness(int golesFavor, int golesContra, int diff) {
        double fitness = 1000.0;
    
        // Puntuación básica
        fitness += golesFavor * 100; // Incremento más alto para incentivar la ofensiva
        fitness -= golesContra * 10;
    
        // Diferencia de goles
        if (diff > 0) {
            fitness += diff * 50; // Incrementar la diferencia de goles a favor
        } else {
            fitness -= Math.abs(diff) * 20;
        }
    
        // Incentivar métricas colaborativas (por ejemplo, pases y tiempo de posesión)
        fitness += obtenerPasesExitosos() * 20;
        fitness += obtenerTiempoPosesion() * 10;
    
        // Penalización por no anotar goles
        if (golesFavor == 0) {
            fitness -= 200;  // Penalización mayor si no se anotan goles
        }
    
        // Penalizar goles excesivos
        if (golesContra > 10) {
            fitness -= (golesContra - 10) * 5;
        }
    
        return Math.max(fitness, 0);
    }
    
    

    private int obtenerPasesExitosos() {
        return 3; // Valor de prueba
    }

    private double obtenerTiempoPosesion() {
        return 15.0; // Valor de prueba
    }

    public int getLastGolesFavor() {
        return lastGolesFavor;
    }

    public int getLastGolesContra() {
        return lastGolesContra;
    }
}
