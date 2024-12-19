/*
 * MainJenetics.java
 *
 * Programa principal que ejecuta el algoritmo genético con Jenetics,
 * con impresión del mejor cromosoma por generación, detalles de alteraciones,
 * y tasa de mejora.
 */

 import io.jenetics.*;
 import io.jenetics.engine.*;
 import io.jenetics.util.Factory;
 
 import java.util.concurrent.Executors;
 import java.util.concurrent.ExecutorService;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 public class MainJenetics {
     private static final Logger LOGGER = Logger.getLogger(MainJenetics.class.getName());
     private static final FuncionEvaluacionJenetics FUNCION_EVALUACION = new FuncionEvaluacionJenetics();
 
     // Parámetros configurables
     private static final int DEFAULT_POPULATION_SIZE = 100;
     private static final int DEFAULT_GENERATIONS = 100;
     private static final double MUTATION_RATE = 0.2;
     private static final double CROSSOVER_RATE = 0.7;
 
     public static void main(String[] args) {
         // Parámetro opcional para ajustar el tamaño de la población según el ID de la tarea
         final int taskId = getTaskId(args);
 
         // Configuración del pool de threads
         ExecutorService executorService = configureExecutor();
 
         // Configuración del genotipo
         Factory<Genotype<DoubleGene>> genotypeFactory = Genotype.of(DoubleChromosome.of(0, 10, 50));
 
         // Configuración del motor genético
         Engine<DoubleGene, Double> engine = Engine.builder(MainJenetics::evaluar, genotypeFactory)
                 .populationSize(DEFAULT_POPULATION_SIZE + taskId * 10)
                 .survivorsSelector(new EliteSelector<>(2)) // Elitismo
                 .offspringSelector(new TournamentSelector<>(5)) // Selección por torneo
                 .alterers(
                         new Mutator<>(MUTATION_RATE),
                         new SinglePointCrossover<>(CROSSOVER_RATE)
                 )
                 .executor(executorService)
                 .build();
 
         EvolutionStatistics<Double, ?> statistics = EvolutionStatistics.ofNumber();
 
         // Ejecución del motor genético
         try {
             Phenotype<DoubleGene, Double> bestPhenotype = engine.stream()
                     .limit(Limits.byFitnessThreshold(900.0)) // Detener cuando el fitness supere 900.0
                     .limit(DEFAULT_GENERATIONS)
                     .peek(statistics) // Registrar estadísticas
                     .peek(result -> logGeneration(result, taskId))
                     .collect(EvolutionResult.toBestPhenotype());
 
             // Resultados finales
             LOGGER.info("Mejor resultado global: " + bestPhenotype.genotype());
             LOGGER.info("Mejor aptitud global: " + bestPhenotype.fitness());
             LOGGER.info("Estadísticas: " + statistics);
 
         } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error en la ejecución del algoritmo genético", e);
         } finally {
             executorService.shutdown();
         }
     }
 
     /**
      * Método de evaluación para el motor genético.
      *
      * @param genotype Genotipo a evaluar.
      * @return Valor de fitness.
      */
     private static double evaluar(Genotype<DoubleGene> genotype) {
         try {
             return FUNCION_EVALUACION.evaluar(genotype);
         } catch (InterruptedException e) {
             LOGGER.log(Level.SEVERE, "Evaluación interrumpida.", e);
             Thread.currentThread().interrupt();
             return -1.0;
         } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error durante la evaluación del genotipo.", e);
             return -1.0;
         }
     }
 
     /**
      * Obtiene el ID de la tarea a partir de los argumentos del programa.
      *
      * @param args Argumentos de línea de comando.
      * @return ID de la tarea.
      */
     private static int getTaskId(String[] args) {
         if (args.length > 0) {
             try {
                 return Integer.parseInt(args[0]);
             } catch (NumberFormatException e) {
                 LOGGER.log(Level.WARNING, "No se pudo parsear el taskId. Usando valor por defecto 0.", e);
             }
         }
         return 0;
     }
 
     /**
      * Configura un pool de threads basado en el entorno (local o HPC).
      *
      * @return ExecutorService configurado.
      */
     private static ExecutorService configureExecutor() {
         String slurmCpusPerTask = System.getenv("SLURM_CPUS_PER_TASK");
         boolean hpcMode = (slurmCpusPerTask != null);
 
         if (hpcMode) {
             LOGGER.info("Modo HPC detectado (variable SLURM_CPUS_PER_TASK presente).");
         } else {
             LOGGER.info("Modo Local detectado (no se encontró SLURM_CPUS_PER_TASK).");
         }
 
         int numThreads = (slurmCpusPerTask != null) ? Integer.parseInt(slurmCpusPerTask) : 1;
         return Executors.newWorkStealingPool(numThreads);
     }
 
     /**
      * Registra detalles sobre cada generación durante la ejecución.
      *
      * @param result Resultado de la generación.
      * @param taskId ID de la tarea.
      */
     private static void logGeneration(EvolutionResult<DoubleGene, Double> result, int taskId) {
         LOGGER.info(String.format("Generación: %d", result.generation()));
         LOGGER.info(String.format("Task ID: %d", taskId));
         LOGGER.info(String.format("Tamaño de la población: %d", result.population().size()));
         LOGGER.info(String.format("Mejor fitness de la generación: %.4f", result.bestFitness()));
     }
 }
 