import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Chromosome;

import java.util.List;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.stream.*;

public class FuncionEvaluacionJenetics {
    private static final Logger LOGGER = Logger.getLogger(FuncionEvaluacionJenetics.class.getName());
    private static final FuncionEvaluacionJenetics INSTANCE = new FuncionEvaluacionJenetics();

    private static final double BASE_FITNESS = 44000.0;
    private static final double LIMITE_SUPERIOR_FITNESS = 150000.0;
    private static final int PARAM_COUNT = 60;

    private static final double[] POSX = { -1.2, -0.5, -0.15, -0.15, -0.15, 1.2, 0.5, 0.15, 0.15, 0.15 };
    private static final double[] POSY = { 0, 0, 0.5, 0, -0.5, 0, 0, 0.5, 0, -0.5 };
    private static final double[] THETA = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    private static final int[] VCLAS = { 1, 1, 1, 1, 1, 2, 2, 2, 2, 2 };

    private double bestFitnessGlobal = Double.NEGATIVE_INFINITY;
    private int bestGolesFavorGlobal = 0;
    private int bestGolesContraGlobal = 0;
    private Genotype<DoubleGene> bestGenotypeGlobal = null;
    private double[] ultimaCargaPorNucleo;

    private FuncionEvaluacionJenetics() {
    }

    public static FuncionEvaluacionJenetics getInstance() {
        return INSTANCE;
    }

    public int getBestGolesFavor() {
        return bestGolesFavorGlobal;
    }

    public int getBestGolesContra() {
        return bestGolesContraGlobal;
    }

    public double getBestFitness() {
        return bestFitnessGlobal;
    }

    public void setCpuLoadPerCore(double[] loads) {
        this.ultimaCargaPorNucleo = loads;
    }

    public double[] getCpuLoadPerCore() {
        return ultimaCargaPorNucleo;
    }

    public ResultadoPartido evaluarResultado(Genotype<DoubleGene> genotype, int generation) {
        if (!validarGenotipo(genotype))
            return new ResultadoPartido(0, 0, 0.0);

        int numSimulaciones = ConfiguracionSingleton.getInstance().NUM_SIMULACIONES;
        SimulationResult[] resultados = IntStream.range(0, numSimulaciones).parallel()
                .mapToObj(i -> ejecutarSimulacionIndividual(genotype))
                .filter(r -> r != null && r.isValido())
                .toArray(SimulationResult[]::new);

        if (resultados.length == 0)
            return new ResultadoPartido(0, 0, 0.0);

        int sumaGF = 0, sumaGC = 0;
        double sumaFitness = 0.0, mejorFitnessGen = Double.NEGATIVE_INFINITY;

        for (SimulationResult r : resultados) {
            sumaGF += r.golesFavor;
            sumaGC += r.golesContra;
            double fitness = calcularFitness(r.golesFavor, r.golesContra);
            sumaFitness += fitness;
            mejorFitnessGen = Math.max(mejorFitnessGen, fitness);
        }

        int promedioGF = sumaGF / resultados.length;
        int promedioGC = sumaGC / resultados.length;
        double promedioFitness = sumaFitness / resultados.length;
        actualizarGlobal(promedioGF, promedioGC, promedioFitness, genotype);

        return new ResultadoPartido(promedioGF, promedioGC, mejorFitnessGen);
    }

    private double calcularFitness(int gf, int gc) {
        double fitness = BASE_FITNESS + 5.0 * gf - 3.0 * gc;
        if (gc == 0)
            fitness += 2000;
        if (gc >= 6)
            fitness -= 500 + 250 * (gc - 5);

        int diff = gf - gc;
        fitness += diff > 0 ? 1500.0 * diff * diff : (diff == 0 ? 200 : -150 * Math.abs(diff));
        if (gf > 0)
            fitness += 1000 + 500 * gf;
        if (gf == 0)
            fitness -= (gc == 0) ? 300 : 800;

        return Math.max(0.0, Math.min(fitness, LIMITE_SUPERIOR_FITNESS));
    }

    private void actualizarGlobal(int gf, int gc, double fitness, Genotype<DoubleGene> genotype) {
        if (fitness > bestFitnessGlobal || (fitness == bestFitnessGlobal && gf >= bestGolesFavorGlobal)) {
            bestFitnessGlobal = fitness;
            bestGolesFavorGlobal = gf;
            bestGolesContraGlobal = gc;
            bestGenotypeGlobal = genotype;
        }
    }

    public double[] getBestParams() {
        Genotype<DoubleGene> bestGenotype = getBestGenotype();
        return bestGenotype.stream()
                .flatMap(Chromosome::stream)
                .mapToDouble(DoubleGene::doubleValue)
                .toArray();
    }

    public String getBestChromosome() {
        Genotype<DoubleGene> bestGenotype = getBestGenotype();
        return bestGenotype.chromosome().stream()
                .map(g -> String.format("%.2f", g.doubleValue()))
                .collect(Collectors.joining(","));
    }

    public Genotype<DoubleGene> getBestGenotype() {
        return this.bestGenotypeGlobal; 
    }

    private boolean validarGenotipo(Genotype<DoubleGene> genotype) {
        return genotype != null && !genotype.isEmpty() &&
                genotype.stream().flatMap(Chromosome::stream).count() >= PARAM_COUNT;
    }

    private TBSimNoGraphics inicializarSimulacion(Genotype<DoubleGene> genotype) {
        TBSimNoGraphics sim = new TBSimNoGraphics(null, "robocup.dsc", configurarRobots(), 3, 2, 200);
        try {
            sim.start();
            sim.sem1 = new Semaphore(0);
            sim.sem1.acquire();

            Double[] params = convertGenotypeToParams(genotype);
            for (int i = 0; i < 5; i++) {
                ((BasicTeamAG) sim.simulation.control_systems[i]).setParam(params);
            }
            sim.sem2.release();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inicializando simulación:", e);
            return null;
        }
        return sim;
    }

    private SimulationResult ejecutarSimulacionIndividual(Genotype<DoubleGene> genotype) {
        TBSimNoGraphics sim = inicializarSimulacion(genotype);
        if (sim == null)
            return null;

        String estado = ejecutarSimulacion(sim);
        if (estado == null || estado.contains("-1"))
            return null;

        String[] valores = extraerUltimaLinea(estado).split(",");
        if (valores.length < 2)
            return null;

        try {
            return new SimulationResult(Integer.parseInt(valores[0].trim()), Integer.parseInt(valores[1].trim()));
        } catch (NumberFormatException e) {
            LOGGER.warning("Error parseando resultado de simulación: " + e.getMessage());
            return null;
        }
    }

    private String ejecutarSimulacion(TBSimNoGraphics sim) {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            Future<String> future = exec.submit(() -> {
                sim.join();
                return sim.estado;
            });
            return future.get(30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.warning("Timeout o error en simulación: " + e.getMessage());
            return "0,0,-1";
        } finally {
            exec.shutdownNow();
        }
    }

    private String extraerUltimaLinea(String estado) {
        String[] lineas = estado.split("\n");
        return (lineas.length > 0) ? lineas[lineas.length - 1] : "0,0,-1";
    }

    private Double[] convertGenotypeToParams(Genotype<DoubleGene> genotype) {
        return genotype.stream().flatMap(Chromosome::stream)
                .limit(PARAM_COUNT)
                .map(DoubleGene::doubleValue)
                .toArray(Double[]::new);
    }

    private NewRobotSpec[] configurarRobots() {
        NewRobotSpec[] robots = new NewRobotSpec[10];
        for (int i = 0; i < 5; i++) {
            robots[i] = new NewRobotSpec("EDU.gatech.cc.is.abstractrobot.SocSmallSim", "BasicTeamAG",
                    POSX[i], POSY[i], THETA[i], "xEAEA00", "xFFFFFF", VCLAS[i]);
        }
        for (int i = 5; i < 10; i++) {
            robots[i] = new NewRobotSpec("EDU.gatech.cc.is.abstractrobot.SocSmallSim", "BrianTeam",
                    POSX[i], POSY[i], THETA[i], "xFF0000", "x0000FF", VCLAS[i]);
        }
        return robots;
    }

    private static class SimulationResult {
        final int golesFavor, golesContra;

        SimulationResult(int gf, int gc) {
            this.golesFavor = gf;
            this.golesContra = gc;
        }

        boolean isValido() {
            return golesFavor >= 0 && golesContra >= 0;
        }
    }

    public static class ResultadoPartido {
        private final int golesFavor, golesContra;
        private final double fitness;

        public ResultadoPartido(int gf, int gc, double f) {
            this.golesFavor = gf;
            this.golesContra = gc;
            this.fitness = f;
        }

        public int getGolesFavor() {
            return golesFavor;
        }

        public int getGolesContra() {
            return golesContra;
        }

        public double getFitness() {
            return fitness;
        }

        @Override
        public String toString() {
            return String.format("Goles Favor: %d, Goles Contra: %d, Fitness: %.2f", golesFavor, golesContra, fitness);
        }
    }
}
