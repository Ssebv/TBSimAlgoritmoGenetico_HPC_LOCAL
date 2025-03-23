import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Chromosome;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class FuncionEvaluacionJenetics {
    private static final Logger LOGGER = Logger.getLogger(FuncionEvaluacionJenetics.class.getName());
    private static final FuncionEvaluacionJenetics INSTANCE = new FuncionEvaluacionJenetics();

    // Constantes de fitness
    private static final double BASE_FITNESS = 50000.0;
    private static final double LIMITE_SUPERIOR_FITNESS = 100000.0;
    
    // Pesos base para cada componente
    private double pesoOfensivo = 3.0;
    private double pesoDefensivo = 1.5;
    private double bonusVictoria = 1500.0; // Bonus para victoria contundente con exponente 2.5
    private double bonusEmpate = 200.0;
    
    // Parámetros de simulación (fijos)
    private static final double[] POSX = { -1.2, -0.5, -0.15, -0.15, -0.15, 1.2, 0.5, 0.15, 0.15, 0.15 };
    private static final double[] POSY = { 0, 0, 0.5, 0, -0.5, 0, 0, 0.5, 0, -0.5 };
    private static final double[] THETA = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    private static final int[] VCLAS = { 1, 1, 1, 1, 1, 2, 2, 2, 2, 2 };
    private static final int PARAM_COUNT = 60;

    // Variables de estado para registrar los mejores resultados
    private int bestGolesFavor = 0;
    private int bestGolesContra = 0;
    private double bestFitness = Double.NEGATIVE_INFINITY;

    public int getBestGolesFavor() { return bestGolesFavor; }
    public int getBestGolesContra() { return bestGolesContra; }
    public double getBestFitness() { return bestFitness; }

    public static FuncionEvaluacionJenetics getInstance() {
        return INSTANCE;
    }

    /**
     * Evalúa el fitness del genotipo realizando NUM_SIMULACIONES partidos y promediando los resultados.
     */
    public ResultadoPartido evaluarResultado(Genotype<DoubleGene> genotype, int generation) {
        if (!validarGenotipo(genotype)) {
            LOGGER.warning("Genotipo inválido.");
            return new ResultadoPartido(0, 0, 0.0);
        }
        int numSimulaciones = ConfiguracionSingleton.getInstance().getConfig().NUM_SIMULACIONES;
        LOGGER.info("NUM_SIMULACIONES = " + numSimulaciones);
        int sumaGF = 0;
        int sumaGC = 0;
        double sumaFitness = 0.0;
        
        for (int i = 0; i < numSimulaciones; i++) {
            TBSimNoGraphics tbSim = inicializarSimulacion(genotype);
            if (tbSim == null) continue;
            String estado = ejecutarSimulacion(tbSim);
            if (!validarEstado(estado)) continue;
            
            String linea = extraerUltimaLinea(estado);
            if (linea.contains("-1")) {
                LOGGER.warning("Simulación no válida, se obtuvo: " + linea);
                continue;
            }
            String[] valores = linea.split(",");
            if (valores.length < 2) continue;
            int gf = Integer.parseInt(valores[0].trim());
            int gc = Integer.parseInt(valores[1].trim());
            
            double componenteOfensivo = gf * pesoOfensivo;
            double bonusOfensivo = (gf > gc) ? bonusVictoria * Math.pow((gf - gc), 2.5) : 0.0;
            double componenteDefensivo = -gc * pesoDefensivo;
            double componenteBalance = (gf == gc) ? bonusEmpate : 0.0;
            
            double fitness = BASE_FITNESS + componenteOfensivo + bonusOfensivo + componenteDefensivo + componenteBalance;
            fitness = Math.max(0, fitness);
            fitness = Math.min(fitness, LIMITE_SUPERIOR_FITNESS);
            
            sumaGF += gf;
            sumaGC += gc;
            sumaFitness += fitness;
        }
        
        int promedioGF = (numSimulaciones > 0) ? sumaGF / numSimulaciones : 0;
        int promedioGC = (numSimulaciones > 0) ? sumaGC / numSimulaciones : 0;
        double promedioFitness = (numSimulaciones > 0) ? sumaFitness / numSimulaciones : 0.0;
        
        actualizarMejorFitness(promedioGF, promedioGC, promedioFitness);
        return new ResultadoPartido(promedioGF, promedioGC, promedioFitness);
    }

    private void actualizarMejorFitness(int gf, int gc, double fitness) {
        if (fitness > bestFitness || (fitness == bestFitness && gf >= bestGolesFavor)) {
            bestFitness = fitness;
            bestGolesFavor = gf;
            bestGolesContra = gc;
        }
    }

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

    // Se usa maxTimeStep=200 (valor original) para que la simulación se ejecute correctamente.
    private TBSimNoGraphics inicializarSimulacion(Genotype<DoubleGene> genotype) {
        NewRobotSpec[] robots = configurarRobots();
        TBSimNoGraphics tbSim = new TBSimNoGraphics(null, "robocup.dsc", robots, 3, 2, 200);
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

    /**
     * Ejecuta la simulación con timeout para evitar bloqueos.
     */
    private String ejecutarSimulacion(TBSimNoGraphics simulacion) {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            Future<String> future = exec.submit(() -> {
                simulacion.join();
                return simulacion.estado;
            });
            // Espera un máximo de 5 segundos
            return future.get(5000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            LOGGER.warning("Timeout en la simulación. Se retorna estado por defecto.");
            return "0,0,-1";
        } catch (Exception e) {
            LOGGER.warning("Error en la simulación: " + e.getMessage());
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
            robots[i] = new NewRobotSpec("EDU.gatech.cc.is.abstractrobot.SocSmallSim", "BasicTeamAG",
                    POSX[i], POSY[i], THETA[i], "xEAEA00", "xFFFFFF", VCLAS[i]);
        }
        for (int i = 5; i < 10; i++) {
            robots[i] = new NewRobotSpec("EDU.gatech.cc.is.abstractrobot.SocSmallSim", "AIKHomoG",
                    POSX[i], POSY[i], THETA[i], "xFF0000", "x0000FF", VCLAS[i]);
        }
        return robots;
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
