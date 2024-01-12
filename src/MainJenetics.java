import io.jenetics.*;
import io.jenetics.engine.*;


import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class MainJenetics {
    private static final FuncionEvaluacionJenetics FUNCION_EVALUACION = new FuncionEvaluacionJenetics();
    private static Phenotype<IntegerGene, Double> mejorCromosoma; // Variable para almacenar el mejor cromosoma

    public static void main(String[] args) { // Nombre del método corregido a 'main'

        // Definición del genotipo (estructura del cromosoma)
        final ExecutorService executor = Executors.newFixedThreadPool(8);
        final Genotype<IntegerGene> GT = Genotype.of(
            IntegerChromosome.of(1, 9, 15)
        );

        // Configuración y ejecución del motor genético
        final Engine<IntegerGene, Double> engine = Engine // 
            .builder(MainJenetics::evaluar, GT)
            // .executor(executor)
            .alterers(
                new Mutator<>(0.1),
                new MultiPointCrossover<>(0.6)
            )
            .executor(executor)
            .build();

        
        final long[] sumatime = new long[1];
        // Ejecución del algoritmo genético
        try (FileWriter csvWriter = new FileWriter("ResultadosAlgoritmoGenetico.csv")) {
            csvWriter.append("Generacion,Aptitud Mejor Individuo,DisPos1,DisPos2,DisPos3,DisPos4,DisPos5,DisKick1,DisKick2,DisKick3,DisKick4,DisKick5,DisTeam1,DisTeam2,DisTeam3,DisTeam4,DisTeam5,Tiempo por generacion,Tiempo total,Uso CPU\n");
            EvolutionStatistics<Double, ?> statistics = EvolutionStatistics.ofNumber();

            mejorCromosoma = engine.stream()

                .limit(100)
                // .parallel() # Tengo problemas a la hora de paralelizar el algoritmo, puede deberse a los semáforos que estan en FuncionEvaluacionJenetics y SimulationCanvas
                .peek(statistics)

                .peek(result -> {
                    long startTime = System.nanoTime();
                    
                    long generation = result.generation();
                    
                    // System.out.println("Generación " + generation + ":");

                    // Obteniendo el mejor genotipo
                    Genotype<IntegerGene> mejorGenotipo = result.bestPhenotype().genotype();
                    Chromosome<IntegerGene> chromosome = mejorGenotipo.chromosome();

                    try {
                        csvWriter.append(generation + "," + result.bestFitness());
                        for (int i = 0; i < chromosome.length(); i++) {
                            csvWriter.append("," + chromosome.get(i).allele());
                        }

                        long endTime = System.nanoTime();
                        double duration = (endTime - startTime);
                        sumatime[0] += duration;
                        
                        
                        csvWriter.append("," + duration + "," + sumatime[0]);

                        // Intento obtener la carga de CPU y manejo de errores
                        try {
                            double cpuLoad = CpuUsageMonitor.getProcessCpuLoad();
                            csvWriter.append(",").append(String.format("%.2f", cpuLoad));
                        } catch (Exception e) {
                            csvWriter.append(",none"); // Escribir 'none' si hay un error
                        }
                        csvWriter.append("\n");

                    } catch (IOException e) {
                        e.printStackTrace(); 
                    }
                })
                .collect(EvolutionResult.toBestPhenotype());

            System.out.println(statistics);
            System.out.println("Mejor individuo: " + mejorCromosoma);
            // Transformar el ultimo dato de tiempo total a segundos
         } catch (IOException e) {
            System.err.println("Error al escribir el archivo CSV: " + e.getMessage()); 
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage()); 
        } finally {
            executor.shutdownNow(); // Se cambió a shutdownNow() para terminar las tareas de forma inmediata
            try {
                if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow(); // Cancelar tareas actualmente en ejecución
                }
            } catch (InterruptedException ie) {
                executor.shutdownNow();
            }
        } 
    }
    // Función de evaluación adaptada para Jenetics
    private static double evaluar(Genotype<IntegerGene> genotype) {
        return FUNCION_EVALUACION.evaluar(genotype);
    }
}