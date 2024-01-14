import io.jenetics.*;
import io.jenetics.engine.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainJenetics {
    private static final FuncionEvaluacionJenetics FUNCION_EVALUACION = new FuncionEvaluacionJenetics(); // Función de evaluación
    private static Phenotype<DoubleGene, Double> mejorCromosoma; // Variable para almacenar el mejor cromosoma  

    public static void main(String[] args) throws InterruptedException {

        final ExecutorService executor = Executors.newFixedThreadPool(8); // Executor para el algoritmo genético
        final Genotype<DoubleGene> GT = Genotype.of(
            DoubleChromosome.of(0, 1), // margin
            DoubleChromosome.of(0, 1), // range
            DoubleChromosome.of(0, 10), // teammateG
            DoubleChromosome.of(0, 10), // wallG
            DoubleChromosome.of(0, 10), // goalieG
            DoubleChromosome.of(0, 10), // forceLimit
            DoubleChromosome.of(-1, 1), // side
            DoubleChromosome.of(0, 2 * Math.PI), // forward_angle
            DoubleChromosome.of(-1, 1), // goalie_x
            DoubleChromosome.of(-1, 1), // offensive_pos1.x
            DoubleChromosome.of(-1, 1), // offensive_pos1.y
            DoubleChromosome.of(-1, 1), // offensive_pos2.x
            DoubleChromosome.of(-1, 1) // offensive_pos2.y
        ); 

        // Ejecución del algoritmo genético
        final long[] sumatime = new long[1];
        try (FileWriter csvWriter = new FileWriter("ResultadosAlgoritmoGenetico.csv")) {
            csvWriter.append(
                "Generacion,Aptitud Mejor Individuo,margin,range,teammateG,wallG,goalieG,forceLimit,side,forward_angle,goalie_x,offensive_pos1.x,offensive_pos1.y,offensive_pos2.x,offensive_pos2.y,tiempo,tiempo acumulado,uso de CPU\n"
            );
                
                mejorCromosoma = Engine.builder(MainJenetics::evaluar, GT)
                    .populationSize(100)
                    .optimize(Optimize.MAXIMUM)
                    .alterers(
                        new Mutator<>(0.05), 
                        new SinglePointCrossover<>(0.6))
                    .executor(executor)
                    .build()
                    .stream()
                    .limit(Limits.bySteadyFitness(10000))
                    .limit(10)
                    .peek(result -> { 
                        long startTime = System.nanoTime();
                        long generation = result.generation();

                        
                        System.out.println("Generación " + result.generation() + ": ");

                        try {
                            Genotype<DoubleGene> mejorGenotipo = result.bestPhenotype().genotype();
                            csvWriter.append(generation + "," + result.bestFitness());
                            for (Chromosome<DoubleGene> chromosome : mejorGenotipo) {
                                for (DoubleGene gene : chromosome) {
                                    csvWriter.append("," + gene.doubleValue());
                                }
                            }
                    
                            long endTime = System.nanoTime();
                            double duration = (endTime - startTime) / 1e6;
                            sumatime[0] += duration;
                    
                            csvWriter.append("," + duration + "," + sumatime[0]);
                    
                            try {
                                double cpuLoad = CpuUsageMonitor.getProcessCpuLoad();
                                csvWriter.append(",").append(String.format("%.2f", cpuLoad));
                            } catch (Exception e) {
                                csvWriter.append(",none");
                            }
                            csvWriter.append("\n");
                            // Mejor cromosoma
                            mejorCromosoma = result.bestPhenotype();  

                            System.out.println("Mejor individuo: " + mejorCromosoma);
                        } catch (IOException e) {
                            Thread.currentThread().interrupt();
                        }
                    })
                    .collect(EvolutionResult.toBestPhenotype());

        System.out.println("Mejor individuo: " + mejorCromosoma);
        System.out.println("Mejor aptitud: " + mejorCromosoma.fitness());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
      
        } 

        private static double evaluar(Genotype<DoubleGene> genotype) {
            return FUNCION_EVALUACION.evaluar(genotype);
        }
}