import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Chromosome;
import io.jenetics.Phenotype;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.ISeq;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class FuncionEvaluacionJenetics {
    private static final Logger LOGGER = Logger.getLogger(FuncionEvaluacionJenetics.class.getName());
    private static final FuncionEvaluacionJenetics INSTANCE = new FuncionEvaluacionJenetics();

    private static final double BASE_FITNESS = 100.0;
    private static final double LIMITE_SUPERIOR_FITNESS = 100000.0;
    
    // Parámetros de ponderación para el cálculo de fitness
    private double pesoOfensivo = 3.0;
    private double pesoDefensivo = 1.5;
    private double bonusVictoria = 1500.0;
    private double bonusEmpate = 200.0;
    
    private static final double[] POSX = { -1.2, -0.5, -0.15, -0.15, -0.15, 1.2, 0.5, 0.15, 0.15, 0.15 };
    private static final double[] POSY = { 0, 0, 0.5, 0, -0.5, 0, 0, 0.5, 0, -0.5 };
    private static final double[] THETA = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    private static final int[] VCLAS = { 1, 1, 1, 1, 1, 2, 2, 2, 2, 2 };
    private static final int PARAM_COUNT = 60;

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
     * Evalúa el fitness del genotipo promediando NUM_SIMULACIONES partidos.
     * Se utiliza procesamiento paralelo si el número de simulaciones es mayor a 1.
     * Se amplifican las diferencias entre componentes del fitness con un factor de 1.5.
     */
    public ResultadoPartido evaluarResultado(Genotype<DoubleGene> genotype, int generation) {
        if (!validarGenotipo(genotype)) {
            LOGGER.warning("Genotipo inválido.");
            return new ResultadoPartido(0, 0, 0.0);
        }
        int numSimulaciones = ConfiguracionSingleton.getInstance().NUM_SIMULACIONES;
        
        // Usamos stream paralelo para simular cada partido
        SimulationResult[] resultados = IntStream.range(0, numSimulaciones)
                .parallel()
                .mapToObj(i -> ejecutarSimulacionIndividual(genotype))
                .filter(r -> r != null && r.isValido())
                .toArray(SimulationResult[]::new);
        
        if (resultados.length == 0) {
            LOGGER.warning("Ninguna simulación arrojó resultados válidos.");
            return new ResultadoPartido(0, 0, 0.0);
        }
        
        int sumaGF = 0;
        int sumaGC = 0;
        double sumaFitness = 0.0;
        for (SimulationResult res : resultados) {
            sumaGF += res.golesFavor;
            sumaGC += res.golesContra;
            sumaFitness += calcularFitness(res.golesFavor, res.golesContra);
        }
        
        int promedioGF = sumaGF / resultados.length;
        int promedioGC = sumaGC / resultados.length;
        double promedioFitness = sumaFitness / resultados.length;
        
        actualizarMejorFitness(promedioGF, promedioGC, promedioFitness);
        return new ResultadoPartido(promedioGF, promedioGC, promedioFitness);
    }
    
    /**
     * Ejecuta una simulación individual y procesa el resultado.
     */
    private SimulationResult ejecutarSimulacionIndividual(Genotype<DoubleGene> genotype) {
        TBSimNoGraphics tbSim = inicializarSimulacion(genotype);
        if (tbSim == null) return null;
        String estado = ejecutarSimulacion(tbSim);
        if (!validarEstado(estado)) return null;
        String linea = extraerUltimaLinea(estado);
        if (linea.contains("-1")) {
            LOGGER.warning("Simulación no válida, se obtuvo: " + linea);
            return null;
        }
        String[] valores = linea.split(",");
        if (valores.length < 2) return null;
        try {
            int gf = Integer.parseInt(valores[0].trim());
            int gc = Integer.parseInt(valores[1].trim());
            return new SimulationResult(gf, gc);
        } catch (NumberFormatException e) {
            LOGGER.warning("Error parseando los valores: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Calcula el fitness a partir de goles a favor y goles en contra.
     */
    private double calcularFitness(int gf, int gc) {
        double componenteOfensivo = gf * pesoOfensivo;
        double bonusOfensivo = (gf > gc) ? bonusVictoria * Math.pow((gf - gc), 2.5) : 0.0;
        double componenteDefensivo = -gc * pesoDefensivo;
        double componenteBalance = (gf == gc) ? bonusEmpate : 0.0;
        // Amplificar las diferencias con un factor de 1.5
        double fitness = BASE_FITNESS + 1.5 * (componenteOfensivo + bonusOfensivo + componenteDefensivo + componenteBalance);
        return Math.min(Math.max(fitness, 0), LIMITE_SUPERIOR_FITNESS);
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

    private TBSimNoGraphics inicializarSimulacion(Genotype<DoubleGene> genotype) {
        NewRobotSpec[] robots = configurarRobots();
        TBSimNoGraphics tbSim = new TBSimNoGraphics(null, "robocup.dsc", robots, 3, 2, 200);
        try {
            tbSim.start();
            tbSim.sem1 = new java.util.concurrent.Semaphore(0);
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
        java.util.concurrent.ExecutorService exec = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            java.util.concurrent.Future<String> future = exec.submit(() -> {
                simulacion.join();
                return simulacion.estado;
            });
            return future.get(5000, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
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

    /**
     * Clase interna para encapsular el resultado de una simulación.
     */
    private static class SimulationResult {
        final int golesFavor;
        final int golesContra;
        
        SimulationResult(int golesFavor, int golesContra) {
            this.golesFavor = golesFavor;
            this.golesContra = golesContra;
        }
        
        boolean isValido() {
            return golesFavor >= 0 && golesContra >= 0;
        }
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
