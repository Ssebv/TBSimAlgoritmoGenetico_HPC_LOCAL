import io.jenetics.*;
import io.jenetics.engine.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class MainJenetics {
    private static final FuncionEvaluacionJenetics FUNCION_EVALUACION = new FuncionEvaluacionJenetics(); // Función de evaluación
    private static Phenotype<DoubleGene, Double> mejorCromosoma; // Variable para almacenar el mejor cromosoma  

    public static void main(String[] args) throws InterruptedException {

        final ExecutorService executor = Executors.newFixedThreadPool(8); // Executor para el algoritmo genético
        final Genotype<DoubleGene> GT = Genotype.of(
            DoubleChromosome.of(0.0, 1.0),         // margin (rango entre 0 y 1)
            DoubleChromosome.of(0.0, 1.0),         // range (rango entre 0 y 1)
            DoubleChromosome.of(0.0, 10.0),        // teammateG (rango entre 0 y 10)
            DoubleChromosome.of(0.0, 10.0),        // wallG (rango entre 0 y 10)
            DoubleChromosome.of(0.0, 10.0),        // goalieG (rango entre 0 y 10)
            DoubleChromosome.of(0.0, 10.0),        // forceLimit (rango entre 0 y 10)
            DoubleChromosome.of(-1.0, 1.0),        // side (rango entre -1 y 1)
            DoubleChromosome.of(0.0, 2 * Math.PI), // forward_angle (rango entre 0 y 2*PI)
            DoubleChromosome.of(-1.0, 1.0),        // goalie_x (rango entre -1 y 1)
            DoubleChromosome.of(-1.0, 1.0),        // offensive_pos1.x (rango entre -1 y 1)
            DoubleChromosome.of(-1.0, 1.0),         // offensive_pos1.y (rango entre -1 y 1)
            DoubleChromosome.of(-1.0, 1.0),         // offensive_pos2.x (rango entre -1 y 1)
            DoubleChromosome.of(-1.0, 1.0)         // offensive_pos2.y (rango entre -1 y 1)
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
                .limit(Limits.bySteadyFitness(100))
                .limit(10)
                .peek(result -> { 
                    long startTime = System.nanoTime();
                    long generation = result.generation();

                    Genotype<DoubleGene> mejorGenotipo = result.bestPhenotype().genotype();
                    Chromosome<DoubleGene> chromosome = mejorGenotipo.chromosome();

                    try {
                        csvWriter.append(generation + "," + result.bestFitness());
                        
                        // Itera a través de los cromosomas y escribe sus valores en el archivo CSV
                        for (int i = 0; i < GT.length(); i++) {
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
                    } catch (IOException e) {
                        Thread.currentThread().interrupt();
                    }
                    // System.out.println("Generación " + result.generation() + ": " + result.bestPhenotype());
                    // long startTime = System.nanoTime();
                    // long generation = result.generation();

                    // System.out.println("Generación " + result.generation() + ": ");

                    // Genotype<DoubleGene> mejorGenotipo = result.bestPhenotype().genotype();
                    // Chromosome<DoubleGene> chromosome = mejorGenotipo.chromosome();

                    // try {
                    //     csvWriter.append(generation + "," + result.bestFitness());
                    //     for (int i = 0; i < chromosome.length(); i++) {
                    //         csvWriter.append("," + chromosome.get(i).doubleValue());
                    //     }

                    //     long endTime = System.nanoTime();
                    //     double duration = (endTime - startTime);
                    //     sumatime[0] += duration;

                    //     csvWriter.append("," + duration + "," + sumatime[0]);

                    //     try {
                    //         double cpuLoad = CpuUsageMonitor.getProcessCpuLoad();
                    //         csvWriter.append(",").append(String.format("%.2f", cpuLoad));
                    //     } catch (Exception e) {
                    //         csvWriter.append(",none");
                    //     }
                    //     csvWriter.append("\n");
                    //     // Mejor cromosoma
                    //     mejorCromosoma = result.bestPhenotype();
                    //     System.out.println("Mejor individuo: " + mejorCromosoma); 

                    
                    // } catch (IOException e) {
                    //     e.printStackTrace();
                    // }
                })
                .collect(EvolutionResult.toBestPhenotype());


            System.out.println("Mejor individuo: " + mejorCromosoma);
            System.out.println("Mejor aptitud: " + mejorCromosoma.fitness());
            
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo CSV: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            executor.shutdown(); // Apagar el executor
            try {
                if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }}
    }

    // Función de evaluación adaptada para Jenetics
    private static double evaluar(Genotype<DoubleGene> genotype) {
        return FUNCION_EVALUACION.evaluar(genotype);
    }
}