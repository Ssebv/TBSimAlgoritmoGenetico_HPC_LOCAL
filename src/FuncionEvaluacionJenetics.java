import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Chromosome;

import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import java.util.logging.Level;

public class FuncionEvaluacionJenetics {
    private static final Logger LOGGER = Logger.getLogger(FuncionEvaluacionJenetics.class.getName());

    // Constantes de fitness
    private static final double BASE_FITNESS = 80.0; 
    private static final double LIMITE_SUPERIOR_FITNESS = 10000.0;

    // Pesos para las recompensas y penalizaciones
    private static final double PESO_GOLES_A_FAVOR = 2.0;
    private static final double PESO_GOLES_EN_CONTRA = 1.5;

    // Parámetros de simulación
    private static final double[] POSX = { -1.2, -0.5, -0.15, -0.15, -0.15, 1.2, 0.5, 0.15, 0.15, 0.15 };
    private static final double[] POSY = { 0, 0, 0.5, 0, -0.5, 0, 0, 0.5, 0, -0.5 };
    private static final double[] THETA = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    private static final int[] VCLAS = { 1, 1, 1, 1, 1, 2, 2, 2, 2, 2 };
    private static final int PARAM_COUNT = 60;

    // Variables de estado
    private int bestGolesFavor = 0;
    private int bestGolesContra = 0;
    private double bestFitness = Double.NEGATIVE_INFINITY;

    public int getBestGolesFavor() {
        return bestGolesFavor;
    }

    public int getBestGolesContra() {
        return bestGolesContra;
    }

    // Métodos públicos principales
    public ResultadoPartido evaluarResultado(Genotype<DoubleGene> genotype, int generation) {
        if (!validarGenotipo(genotype)) {
            LOGGER.warning("Genotipo inválido.");
            return new ResultadoPartido(0, 0, 0.0);
        }

        TBSimNoGraphics tbSim = inicializarSimulacion(genotype);
        if (tbSim == null) {
            LOGGER.warning("No se pudo inicializar la simulación.");
            return new ResultadoPartido(0, 0, 0.0);
        }

        String estado = ejecutarSimulacion(tbSim);
        if (!validarEstado(estado)) {
            return new ResultadoPartido(0, 0, 0.0);
        }

        return procesarEstadoYActualizarMejorFitness(estado, generation);
    }

    // Métodos auxiliares de validación
    private boolean validarGenotipo(Genotype<DoubleGene> genotype) {
        if (genotype == null || genotype.isEmpty()) {
            LOGGER.warning("Genotipo nulo o vacío.");
            return false;
        }
        long totalGenes = genotype.stream().flatMap(Chromosome::stream).count();
        if (totalGenes < PARAM_COUNT) {
            LOGGER.warning("El genotipo no tiene suficientes genes. Se requieren " + PARAM_COUNT + " genes.");
            return false;
        }
        return true;
    }

    private boolean validarEstado(String estado) {
        if (estado == null || estado.isEmpty()) {
            LOGGER.warning("Estado vacío o nulo después de ejecutar la simulación.");
            return false;
        }
        return true;
    }

    // Inicialización de la simulación
    private TBSimNoGraphics inicializarSimulacion(Genotype<DoubleGene> genotype) {
        NewRobotSpec[] robots = configurarRobots();
        TBSimNoGraphics tbSim = new TBSimNoGraphics(null, "robocup.dsc", robots, 3, 2, 50);

        try {
            tbSim.start();
            tbSim.sem1 = new Semaphore(0);
            tbSim.sem1.acquire();

            Double[] params = convertGenotypeToParams(genotype);
            if (!validarParametros(params)) {
                throw new IllegalArgumentException("Parámetros fuera de rango permitido.");
            }

            for (int i = 0; i < 5; i++) {
                BasicTeamAG robot = (BasicTeamAG) tbSim.simulation.control_systems[i];
                robot.setParam(params);
            }

            tbSim.sem2.release();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error configurando la simulación: ", e);
            return null;
        }

        return tbSim;
    }

    private String ejecutarSimulacion(TBSimNoGraphics simulacion) {
        try {
            simulacion.join();
            return simulacion.estado;
        } catch (InterruptedException e) {
            LOGGER.warning("Error ejecutando la simulación: " + e.getMessage());
            return null;
        }
    }

    // Procesamiento del estado
    private ResultadoPartido procesarEstadoYActualizarMejorFitness(String estado, int generation) {
        try {
            String[] valores = extraerUltimaLinea(estado).split(",");
            if (valores.length < 2) {
                LOGGER.warning("Formato inválido en el estado. Fitness=0");
                return new ResultadoPartido(0, 0, 0.0);
            }

            int golesFavor = Integer.parseInt(valores[0].trim());
            int golesContra = Integer.parseInt(valores[1].trim());
            double fitness = calcularFitness(golesFavor, golesContra, generation);

            actualizarMejorFitness(golesFavor, golesContra, fitness);

            // Loggear el resultado del partido
            // LOGGER.info(String.format("Resultado del partido: %s", new ResultadoPartido(golesFavor, golesContra, fitness)));

            return new ResultadoPartido(golesFavor, golesContra, fitness);
        } catch (Exception e) {
            LOGGER.warning("Error procesando el estado: " + e.getMessage());
            return new ResultadoPartido(0, 0, 0.0);
        }
    }

    private void actualizarMejorFitness(int golesFavor, int golesContra, double fitness) {
        if (fitness > bestFitness || (fitness == bestFitness && golesFavor >= bestGolesFavor)) {
            bestFitness = fitness;
            bestGolesFavor = golesFavor;
            bestGolesContra = golesContra;
        }
    }

    private String extraerUltimaLinea(String estado) {
        String[] lineas = estado.split("\n");
        return lineas.length > 0 ? lineas[lineas.length - 1] : "0,0,-1";
    }

    private Double[] convertGenotypeToParams(Genotype<DoubleGene> genotype) {
        return genotype.stream()
                .flatMap(Chromosome::stream)
                .limit(PARAM_COUNT)
                .map(DoubleGene::doubleValue)
                .toArray(Double[]::new);
    }

    private boolean validarParametros(Double[] params) {
        for (int i = 0; i < params.length; i++) {
            if (params[i] < 0 || params[i] > 5) {
                LOGGER.warning(String.format("El parámetro %d está fuera de rango: %.2f", i, params[i]));
                return false;
            }
        }
        return true;
    }

    private NewRobotSpec[] configurarRobots() {
        NewRobotSpec[] robots = new NewRobotSpec[10];
        for (int i = 0; i < 5; i++) {
            robots[i] = new NewRobotSpec("EDU.gatech.cc.is.abstractrobot.SocSmallSim", "BasicTeamAG", POSX[i], POSY[i], THETA[i], "xEAEA00", "xFFFFFF", VCLAS[i]);
        }
        for (int i = 5; i < 10; i++) {
            robots[i] = new NewRobotSpec("EDU.gatech.cc.is.abstractrobot.SocSmallSim", "AIKHomoG", POSX[i], POSY[i], THETA[i], "xFF0000", "x0000FF", VCLAS[i]);
        }
        return robots;
    }

    // Métodos para cálculo de fitness
    private double calcularFitness(int gf, int gc, int generation) {
        double fitness = BASE_FITNESS;
        fitness += calcularRecompensas(gf, gc);
        fitness -= calcularPenalizaciones(gf, gc);
        return Math.min(fitness, LIMITE_SUPERIOR_FITNESS);
    }

    private double calcularRecompensas(int gf, int gc) {
        double recompensas = 0.0;
        recompensas += gf * PESO_GOLES_A_FAVOR;
        recompensas += recompensaPorEmpate(gf, gc);
        return recompensas;
    }

    private double calcularPenalizaciones(int gf, int gc) {
        double penalizaciones = 0.0;
        penalizaciones += gc * PESO_GOLES_EN_CONTRA;
        return penalizaciones;
    }

    private double recompensaPorEmpate(int gf, int gc) {
        if (gf == gc && gf > 0) return 2000.0;
        if (gf == gc && gf == 0) return 500.0;
        return 0.0;
    }

    public static class ResultadoPartido {
        private final int golesFavor;
        private final int golesContra;
        private final double fitness;

        public ResultadoPartido(int golesFavor, int golesContra, double fitness) {
            this.golesFavor = golesFavor;
            this.golesContra = golesContra;
            this.fitness = fitness;
        }

        public int getGolesFavor() { return golesFavor; }
        public int getGolesContra() { return golesContra; }
        public double getFitness() { return fitness; }

        @Override
        public String toString() {
            return String.format("Goles Favor: %d, Goles Contra: %d, Fitness: %.2f", golesFavor, golesContra, fitness);
        }
    }
}
