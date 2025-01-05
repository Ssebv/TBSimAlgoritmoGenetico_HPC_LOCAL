/*
 * MainJenetics.java
 *
 * Programa principal que ejecuta el algoritmo genético con Jenetics,
 * generando un archivo CSV (stats.csv) con métricas de cada generación.
 */

 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.PrintWriter;
 
 import java.util.HashSet;
 import java.util.Set;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import java.util.logging.*;
 
 import io.jenetics.*;
 import io.jenetics.engine.*;
 import io.jenetics.util.*;
 
 import sun.misc.Signal;
 import sun.misc.SignalHandler;
 
 import java.lang.management.ManagementFactory;
 import java.lang.management.OperatingSystemMXBean;

 
 public class MainJenetics {
     private static final Logger LOGGER = Logger.getLogger(MainJenetics.class.getName());
     private static final FuncionEvaluacionJenetics FUNCION_EVALUACION = new FuncionEvaluacionJenetics();
 
     private static final int INITIAL_POPULATION_SIZE = 100;
     private static final int DEFAULT_GENERATIONS = 1000;
     private static double mutationRate = 0.5;
     private static double crossoverRate = 0.8;
     private static double prevAverageFitness = 0.0;
 
     // Para medir tiempo total
     private static long startTime = System.currentTimeMillis();
 
     // CSV
     private static PrintWriter csvWriter = null;
 
     static {
         // Limpiamos handlers
         Logger rootLogger = Logger.getLogger("");
         for (Handler handler : rootLogger.getHandlers()) {
             rootLogger.removeHandler(handler);
         }
 
         // Creamos un ConsoleHandler
         ConsoleHandler consoleHandler = new ConsoleHandler();
         consoleHandler.setFormatter(new SimpleFormatter() {
             @Override
             public synchronized String format(LogRecord record) {
                 return String.format("%s: %s%n", record.getLevel(), record.getMessage());
             }
         });
         rootLogger.addHandler(consoleHandler);
 
         rootLogger.setLevel(Level.INFO);
     }
 
     public static void main(String[] args) {
         // Detectar HPC vs local
         String slurmCpusPerTask = System.getenv("SLURM_CPUS_PER_TASK");
         String slurmJobId = System.getenv("SLURM_JOB_ID");
         boolean isHPC = (slurmCpusPerTask != null || slurmJobId != null);
 
         if (isHPC) {
             LOGGER.info("Iniciando en HPC...");
             try {
                 Signal.handle(new Signal("USR1"), new SignalHandler() {
                     @Override
                     public void handle(Signal sig) {
                         LOGGER.info("Señal USR1 recibida (HPC).");
                         // Podrías forzar checkpoint
                     }
                 });
             } catch (Throwable t) {
                 LOGGER.log(Level.WARNING, "No se pudo capturar USR1.", t);
             }
         } else {
             LOGGER.info("Iniciando en local...");
         }
 
         final int threads;
         if (isHPC) {
             int cpus = 1;
             try {
                 if (slurmCpusPerTask != null) {
                     cpus = Integer.parseInt(slurmCpusPerTask);
                 }
             } catch (NumberFormatException e) {
                 LOGGER.warning("Valor inválido en SLURM_CPUS_PER_TASK, usando 1 hilo.");
             }
             threads = (slurmCpusPerTask != null) ? Integer.parseInt(slurmCpusPerTask) : 1;
         } else {
             threads = Runtime.getRuntime().availableProcessors();
         }
 
         ExecutorService executorService = configureExecutor(threads);
 
         // Genotype con 50 genes en [0..10]
         Factory<Genotype<DoubleGene>> genotypeFactory =
                 Genotype.of(DoubleChromosome.of(0, 10, 50));
 
         DynamicSelector<DoubleGene, Double> dynamicSelector =
                 new DynamicSelector<>(DEFAULT_GENERATIONS);
 
         // Preparamos stats.csv
         try {
             csvWriter = new PrintWriter(new FileWriter("stats.csv"));
             // Encabezado (añade las columnas que gustes)
             csvWriter.println("Generacion,MejorFitness,PromFitness,Diversidad,Convergencia," +
                               "GolesFavor,GolesContra,CpuPercent,MemMB,TimeSec,SlurmJobID," +
                               "OSName,OSArch,OSVersion,SysCores,SysLoad,SysFreeMemMB,SysTotalMemMB");
         } catch (IOException e) {
             LOGGER.log(Level.WARNING, "No se pudo crear stats.csv", e);
         }
 
         try {
             Engine<DoubleGene, Double> engine = Engine.builder(MainJenetics::evaluar, genotypeFactory)
                     .populationSize(INITIAL_POPULATION_SIZE)
                     .offspringFraction(0.8)
                     .alterers(new Mutator<>(mutationRate), new SinglePointCrossover<>(crossoverRate))
                     .survivorsSelector(dynamicSelector)
                     .executor(executorService)
                     .build();
 
             Phenotype<DoubleGene, Double> bestPhenotype = engine.stream()
                     .peek(result -> {
                         dynamicSelector.setGeneration((int) result.generation());
                         adjustParameters(result);
                         logGeneration(result, isHPC, slurmJobId);
                     })
                     .limit(DEFAULT_GENERATIONS)
                     .collect(EvolutionResult.toBestPhenotype());
 
             LOGGER.info("Mejor genotipo: " + bestPhenotype.genotype());
             LOGGER.info("Mejor aptitud: " + bestPhenotype.fitness());
         } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error en la ejecución del AG", e);
         } finally {
             if (csvWriter != null) {
                 csvWriter.close();
             }
             executorService.shutdown();
         }
     }
 
     private static double evaluar(Genotype<DoubleGene> genotype) {
         try {
             return FUNCION_EVALUACION.evaluar(genotype);
         } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error evaluando genotipo.", e);
             return -1.0;
         }
     }
 
     private static ExecutorService configureExecutor(int nThreads) {
         return Executors.newFixedThreadPool(nThreads);
     }
 
     private static void logGeneration(EvolutionResult<DoubleGene, Double> result, boolean isHPC, String slurmJobId) {
         int generation = (int) result.generation();
         Phenotype<DoubleGene, Double> bestPhenotype = result.bestPhenotype();
 
         // CPU usage
         double cpuLoad = CpuUsageMonitor.getProcessCpuLoad();
         double cpuPercent = (cpuLoad < 0) ? -1 : (cpuLoad * 100.0);
 
         // Memoria del proceso
         long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
         double usedMemMB = usedMem / (1024.0 * 1024.0);
 
         // Tiempo transcurrido
         long now = System.currentTimeMillis();
         double elapsedSec = (now - startTime) / 1000.0;
 
         // Métricas de Jenetics
         double diversidad = calcularDiversidad(result.population());
         double bestFitness = bestPhenotype.fitness();
         double averageFitness = result.population().stream()
                 .mapToDouble(ph -> ph.fitness().doubleValue())
                 .average()
                 .orElse(0.0);
         double convergence = Math.abs(averageFitness - prevAverageFitness);
         prevAverageFitness = averageFitness;
 
         int gf = FUNCION_EVALUACION.getBestGolesFavor();
         int gc = FUNCION_EVALUACION.getBestGolesContra();
 
         // Info de OS
         String osName = System.getProperty("os.name");
         String osArch = System.getProperty("os.arch");
         String osVersion = System.getProperty("os.version");
 
         OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
         int sysCores = osBean.getAvailableProcessors();
         double sysLoad = osBean.getSystemLoadAverage(); // -1 si no soportado
 
         // Si tenemos com.sun.management.OperatingSystemMXBean
         long freeMemOS = -1, totalMemOS = -1;
         if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
             com.sun.management.OperatingSystemMXBean unixBean = (com.sun.management.OperatingSystemMXBean) osBean;
             freeMemOS = unixBean.getFreePhysicalMemorySize();
             totalMemOS = unixBean.getTotalPhysicalMemorySize();
         }
         double freeMemMB = (freeMemOS < 0) ? -1 : (freeMemOS / (1024.0 * 1024.0));
         double totalMemMB = (totalMemOS < 0) ? -1 : (totalMemOS / (1024.0 * 1024.0));
 
         // Log mínimo en consola (opcional)
         LOGGER.info(String.format("Gen %d -> BestF=%.4f, Prom=%.4f, CPU=%.2f%%, Mem=%.2fMB, OS=%s",
                                   generation, bestFitness, averageFitness,
                                   cpuPercent, usedMemMB, osName));
 
         // Escribimos en CSV si csvWriter no es null
         if (csvWriter != null) {
             csvWriter.printf(
                 "%d,%.4f,%.4f,%.4f,%.4f,%d,%d,%.2f,%.2f,%.2f,%s," +  // 11 columnas
                 "%s,%s,%s,%d,%.2f,%.2f,%.2f%n",                      // 7 columnas extra
                 generation,
                 bestFitness,
                 averageFitness,
                 diversidad,
                 convergence,
                 gf,
                 gc,
                 cpuPercent,
                 usedMemMB,
                 elapsedSec,
                 (isHPC && slurmJobId != null) ? slurmJobId : "local",
                 osName,
                 osArch,
                 osVersion,
                 sysCores,
                 (sysLoad < 0 ? -1.0 : sysLoad),
                 freeMemMB,
                 totalMemMB
             );
             csvWriter.flush();
         }
     }
 
     private static void adjustParameters(EvolutionResult<DoubleGene, Double> result) {
         double diversidad = calcularDiversidad(result.population());
         if (diversidad < 0.7) {
             mutationRate = Math.min(mutationRate + 0.05, 0.8);
             crossoverRate = Math.max(crossoverRate - 0.05, 0.5);
         }
     }
 
     private static double calcularDiversidad(ISeq<Phenotype<DoubleGene, Double>> population) {
         Set<String> uniqueGenotypes = new HashSet<>();
         for (Phenotype<DoubleGene, Double> ph : population) {
             uniqueGenotypes.add(ph.genotype().toString());
         }
         return uniqueGenotypes.size() / (double) population.size();
     }
 }
 
 /**
  * DynamicSelector para cambiar entre estrategias de selección
  * según la diversidad genética y la generación.
  */
 class DynamicSelector<G extends Gene<?, G>, C extends Number & Comparable<? super C>>
         implements Selector<G, C> {
 
     private final int maxGenerations;
     private int currentGeneration;
     private double threshold;
 
     public DynamicSelector(int maxGenerations) {
         this.maxGenerations = maxGenerations;
         this.currentGeneration = 0;
         updateThreshold();
     }
 
     public void setGeneration(int generation) {
         this.currentGeneration = generation;
         updateThreshold();
     }
 
     private void updateThreshold() {
         this.threshold = 0.9 - (0.1 * (currentGeneration / (double) maxGenerations));
     }
 
     @Override
     public ISeq<Phenotype<G, C>> select(
             Seq<Phenotype<G, C>> population,
             int count,
             Optimize opt
     ) {
         double diversidad = calcularDiversidad(population);
         Selector<G, C> selector = (diversidad > threshold)
                 ? new EliteSelector<>()
                 : new RouletteWheelSelector<>();
 
         return selector.select(population, count, opt);
     }
 
     private double calcularDiversidad(Seq<Phenotype<G, C>> population) {
         long uniqueGenotypes = population.stream()
                 .map(ph -> ph.genotype().toString().hashCode())
                 .distinct()
                 .count();
         return uniqueGenotypes / (double) population.size();
     }
 }
 