import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Chromosome;
import io.jenetics.Phenotype;

import java.util.Arrays;

import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

public class FuncionEvaluacionJenetics {
    private static final Logger LOGGER = Logger.getLogger(FuncionEvaluacionJenetics.class.getName());

    // Constantes de fitness
    private static final double BASE_FITNESS = 10000.0;
    private static final double GOLES_A_FAVOR_RECOMPENSA = 300.0;
    private static final double GOLES_EN_CONTRA_PENALIZACION = 20.0;
    private static final double DIFERENCIA_GOL_RECOMPENSA = 200.0;
    private static final double DIFERENCIA_GOL_PENALIZACION = 75.0;
    private static final double SIN_GOLES_PENALIZACION = -2500.0;
    private static final double LIMITE_SUPERIOR_FITNESS = 35000.0;

    // Parámetros para la simulación
    private static final double[] POSX = { -1.2, -0.5, -0.15, -0.15, -0.15, 1.2, 0.5, 0.15, 0.15, 0.15 };
    private static final double[] POSY = { 0, 0, 0.5, 0, -0.5, 0, 0, 0.5, 0, -0.5 };
    private static final double[] THETA = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    private static final int[] VCLAS = { 1, 1, 1, 1, 1, 2, 2, 2, 2, 2 };

    private static final int PARAM_COUNT = 50;

    // Variables para almacenar goles
    private int currentGolesFavor = 0;
    private int currentGolesContra = 0;

    public double evaluar(Genotype<DoubleGene> genotype) throws InterruptedException {
        if (genotype == null || genotype.isEmpty()) {
            LOGGER.warning("Genotipo nulo o vacío. Fitness=0");
            return 0.0;
        }

        long totalGenes = genotype.stream().flatMap(Chromosome::stream).count();
        if (totalGenes < PARAM_COUNT) {
            LOGGER.warning("Menos de 50 genes (" + totalGenes + "). Fitness=0");
            return 0.0;
        }

        NewRobotSpec[] newRobots = configurarRobots();
        TBSimNoGraphics tb = new TBSimNoGraphics(null, "robocup.dsc", newRobots, 3, 2, 50);
        tb.start();
        tb.sem1 = new Semaphore(0);
        tb.sem1.acquire();

        Double[] params = convertGenotypeToParams(genotype);
        for (int i = 0; i < 5; i++) {
            BasicTeamAG robot = (BasicTeamAG) (tb.simulation.control_systems[i]);
            robot.setParam(params);
        }

        tb.sem2.release();
        tb.join();
        return ejecutarSimulacion(tb);
    }

    private NewRobotSpec[] configurarRobots() {
        NewRobotSpec[] newRobots = new NewRobotSpec[10];
        for (int i = 0; i < 5; i++) {
            newRobots[i] = new NewRobotSpec(
                "EDU.gatech.cc.is.abstractrobot.SocSmallSim",
                "BasicTeamAG",
                POSX[i],
                POSY[i],
                THETA[i],
                "xEAEA00",
                "xFFFFFF",
                VCLAS[i]
            );
        }
        for (int i = 5; i < 10; i++) {
            newRobots[i] = new NewRobotSpec(
                "EDU.gatech.cc.is.abstractrobot.SocSmallSim",
                "AIKHomoG",
                POSX[i],
                POSY[i],
                THETA[i],
                "xFF0000",
                "x0000FF",
                VCLAS[i]
            );
        }
        return newRobots;
    }

    private double ejecutarSimulacion(TBSimNoGraphics simulacion) {
        if (simulacion == null) {
            LOGGER.warning("Simulación nula. Fitness=0");
            return 0.0;
        }

        try {
            String[] lines = simulacion.estado.split("\n");
            @SuppressWarnings("unused")
            String ultimaLinea = Arrays.stream(lines).filter(line -> !line.isBlank()).reduce((a, b) -> b).orElse(null);
            if (ultimaLinea == null) {
                LOGGER.warning("No se encontró línea válida en estado. Fitness=0");
                return 0.0;
            }

            String[] valores = ultimaLinea.split(",");
            if (valores.length < 2) {
                LOGGER.warning("Formato inválido (no hay 2 valores). Fitness=0");
                return 0.0;
            }

            int golesFavor = Integer.parseInt(valores[0].trim());
            int golesContra = Integer.parseInt(valores[1].trim());

            currentGolesFavor = golesFavor;
            currentGolesContra = golesContra;

            return calcularFitness(golesFavor, golesContra, golesFavor - golesContra);
        } catch (NumberFormatException e) {
            LOGGER.warning("Error parseando resultados: " + e.getMessage());
            return 0.0;
        }
    }

    public Double[] convertGenotypeToParams(Genotype<DoubleGene> genotype) {
        return genotype.stream().flatMap(Chromosome::stream).limit(PARAM_COUNT).map(DoubleGene::doubleValue)
                .toArray(Double[]::new);
    }

    public double calcularFitness(int gf, int gc, int diff) {
        double fitness = BASE_FITNESS;

        // Recompensas y penalizaciones
        fitness += recompensaPorGolesFavor(gf);
        fitness -= penalizacionPorGolesContra(gc);
        fitness += recompensaPorDiferencia(diff);
        fitness += penalizacionPorJuegoAburrido(gf, gc);

        return Math.min(fitness, LIMITE_SUPERIOR_FITNESS);
    }

    private double recompensaPorGolesFavor(int gf) {
        return gf * GOLES_A_FAVOR_RECOMPENSA;
    }

    private double penalizacionPorGolesContra(int gc) {
        return gc * GOLES_EN_CONTRA_PENALIZACION;
    }

    private double recompensaPorDiferencia(int diff) {
        return diff > 0 ? diff * DIFERENCIA_GOL_RECOMPENSA : diff * DIFERENCIA_GOL_PENALIZACION;
    }

    private double penalizacionPorJuegoAburrido(int gf, int gc) {
        return (gf + gc) < 3 ? SIN_GOLES_PENALIZACION : 0.0;
    }

    public int getCurrentGolesFavor() {
        return currentGolesFavor;
    }

    public int getCurrentGolesContra() {
        return currentGolesContra;
    }

    public int getGolesFavorForPhenotype(Phenotype<DoubleGene, Double> phenotype) {
        return phenotype != null ? currentGolesFavor : 0;
    }

    public int getGolesContraForPhenotype(Phenotype<DoubleGene, Double> phenotype) {
        return phenotype != null ? currentGolesContra : 0;
    }
}
