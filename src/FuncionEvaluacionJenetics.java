/*
 * FuncionEvaluacionJenetics.java
 *
 * Función de evaluación para el algoritmo genético con Jenetics.
 * Ajustada con una función de fitness que premia victorias, goles a favor
 * y penaliza goles en contra, con chequeos adicionales para ser robusta.
 */

 import io.jenetics.DoubleGene;
 import io.jenetics.Genotype;
 import io.jenetics.Chromosome;
 
 import java.util.Arrays;
 import java.util.List;
 import java.util.ArrayList;
 import java.util.concurrent.Semaphore;
 import java.util.logging.Logger;
 
 public class FuncionEvaluacionJenetics {
     private static final Logger LOGGER = Logger.getLogger(FuncionEvaluacionJenetics.class.getName());
 
     // Constantes de fitness
     private static final double BASE_FITNESS = 10000.0;
     private static final double GOLES_A_FAVOR_RECOMPENSA = 300.0;
     private static final double GOLES_EN_CONTRA_PENALIZACION = 15.0;
     private static final double DIFERENCIA_GOL_RECOMPENSA = 150.0;
     private static final double DIFERENCIA_GOL_PENALIZACION = 50.0;
     private static final double DEFENSA_IMPECABLE_BONIFICACION = 500.0;
     private static final double SIN_GOLES_PENALIZACION = -2000.0;
     private static final double LIMITE_SUPERIOR_FITNESS = 30000.0;
 
     // Si tu simulación requiere posiciones/config
     private static final double[] POSX = { -1.2, -0.5, -0.15, -0.15, -0.15, 1.2, 0.5, 0.15, 0.15, 0.15 };
     private static final double[] POSY = { 0, 0,  0.5,  0, -0.5, 0,  0, 0.5,  0, -0.5 };
     private static final double[] THETA = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
     private static final int[] VCLAS = { 1, 1, 1, 1, 1, 2, 2, 2, 2, 2 };
 
     // Genotype de 50 genes
     private static final int PARAM_COUNT = 50;
 
     // Variables para el mejor resultado
     private int bestGolesFavor = 0;
     private int bestGolesContra = 0;
     private double bestFitness = Double.NEGATIVE_INFINITY;
 
     // Almacena resultados de partidos
     private final List<String> matchResults = new ArrayList<>();
 
     public double evaluar(Genotype<DoubleGene> genotype) throws InterruptedException {
         if (genotype == null || genotype.isEmpty()) {
             LOGGER.warning("Genotipo nulo o vacío. Fitness=0");
             return 0.0;
         }
 
         long totalGenes = genotype.stream()
                 .flatMap(Chromosome::stream)
                 .count();
 
         if (totalGenes < PARAM_COUNT) {
             LOGGER.warning("Menos de 50 genes (" + totalGenes + "). Fitness=0");
             return 0.0;
         }
 
         NewRobotSpec[] newRobots = configurarRobots();
 
         // Simulación sin gráficos
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
             if (simulacion.estado == null || simulacion.estado.isEmpty()) {
                 LOGGER.warning("Estado de simulación vacío. Fitness=0");
                 return 0.0;
             }
 
             String[] lines = simulacion.estado.split("\n");
             String ultimaLinea = Arrays.stream(lines)
                     .filter(line -> !line.isBlank())
                     .reduce((a, b) -> b)
                     .orElse(null);
 
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
             int diff = golesFavor - golesContra;
 
             addMatchResult(String.format("Goles a favor: %d, Goles en contra: %d", golesFavor, golesContra));
 
             double fitness = calcularFitness(golesFavor, golesContra, diff);
             actualizarMejorResultado(fitness, golesFavor, golesContra);
 
             return fitness;
 
         } catch (NumberFormatException e) {
             LOGGER.warning("Error parseando resultados: " + e.getMessage());
             return 0.0;
         }
     }
 
     public Double[] convertGenotypeToParams(Genotype<DoubleGene> genotype) {
         return genotype.stream()
                 .flatMap(Chromosome::stream)
                 .limit(PARAM_COUNT)
                 .map(DoubleGene::doubleValue)
                 .toArray(Double[]::new);
     }
 
     public double calcularFitness(int gf, int gc, int diff) {
         double fitness = BASE_FITNESS;
 
         fitness += recompensaPorGolesFavor(gf);
         fitness -= penalizacionPorGolesContra(gc);
         fitness += recompensaPorDiferencia(diff);
         fitness += bonificacionesAdicionales(gf, gc, diff);
 
         fitness = Math.max(0.0, fitness);
         if (fitness > LIMITE_SUPERIOR_FITNESS) {
             fitness = LIMITE_SUPERIOR_FITNESS;
         }
 
         return fitness;
     }
 
     private double recompensaPorGolesFavor(int gf) {
         double r = gf * GOLES_A_FAVOR_RECOMPENSA;
         if (gf > 3) {
             r += (gf - 3) * 150.0;
         }
         return r;
     }
 
     private double penalizacionPorGolesContra(int gc) {
         return Math.pow(gc, 1.1) * GOLES_EN_CONTRA_PENALIZACION;
     }
 
     private double recompensaPorDiferencia(int diff) {
         if (diff > 0) {
             return diff * DIFERENCIA_GOL_RECOMPENSA;
         } else {
             return Math.abs(diff) * DIFERENCIA_GOL_PENALIZACION * -1;
         }
     }
 
     private double bonificacionesAdicionales(int gf, int gc, int diff) {
         double bonus = 0.0;
         if (gc == 0) {
             bonus += DEFENSA_IMPECABLE_BONIFICACION;
         }
         if (gf == 0) {
             bonus += SIN_GOLES_PENALIZACION;
         }
         if (gf >= 5 && diff >= 3) {
             bonus += 300.0;
         }
         return bonus;
     }
 
     private void actualizarMejorResultado(double fitness, int gf, int gc) {
         if (fitness > bestFitness) {
             bestFitness = fitness;
             bestGolesFavor = gf;
             bestGolesContra = gc;
         }
     }
 
     public int getBestGolesFavor() {
         return bestGolesFavor;
     }
 
     public int getBestGolesContra() {
         return bestGolesContra;
     }
 
     public void resetBest() {
         bestFitness = Double.NEGATIVE_INFINITY;
         bestGolesFavor = 0;
         bestGolesContra = 0;
     }
 
     public void addMatchResult(String result) {
         matchResults.add(result);
     }
 
     public void printAllMatches() {
         for (String result : matchResults) {
             LOGGER.info(result);
         }
         matchResults.clear();
     }
 }
 