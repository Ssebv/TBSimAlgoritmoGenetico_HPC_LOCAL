import io.jenetics.*;
import io.jenetics.engine.*;

import java.io.FileWriter;
import java.io.IOException;


public class MainJenetics {
    private static final FuncionEvaluacionJenetics FUNCION_EVALUACION = new FuncionEvaluacionJenetics();

    public static void main(String[] args) { // Nombre del método corregido a 'main'

        // Definición del genotipo (estructura del cromosoma)
        // ExecutorService executor = Executors.newFixedThreadPool(8);
        final Genotype<IntegerGene> GT = Genotype.of(
            IntegerChromosome.of(1, 9, 15)
        );

        // Configuración y ejecución del motor genético
        final Engine<IntegerGene, Double> engine = Engine // 
            .builder(MainJenetics::evaluar, GT)
            .populationSize(10)
            // .executor(executor)
            .alterers(
                new Mutator<>(0.1),
                new MeanAlterer<>(0.6)
            )
            // .executor(executor)

            
            .build();
        final long[] sumatime = new long[1];
        // Ejecución del algoritmo genético
        try (FileWriter csvWriter = new FileWriter("ResultadosAlgoritmoGenetico.csv")) {
            csvWriter.append("Generacion,Aptitud Mejor Individuo,DisPos1,DisPos2,DisPos3,DisPos4,DisPos5,DisKick1,DisKick2,DisKick3,DisKick4,DisKick5,DisTeam1,DisTeam2,DisTeam3,DisTeam4,DisTeam5,Tiempo por generacion,Tiempo total,Uso CPU\n");
            EvolutionStatistics<Double, ?> statistics = EvolutionStatistics.ofNumber();

            engine.stream()
                .limit(250)
                .peek(statistics)

                .peek(result -> {
                    long startTime = System.nanoTime();
                    
                    long generation = result.generation();
                    
                    System.out.println("Generación " + generation + ":");

                    // Obteniendo el mejor genotipo
                    Genotype<IntegerGene> mejorGenotipo = result.bestPhenotype().genotype();
                    Chromosome<IntegerGene> chromosome = mejorGenotipo.chromosome();

                    try {
                        csvWriter.append(generation + "," + result.bestFitness());
                        for (int i = 0; i < chromosome.length(); i++) {
                            csvWriter.append("," + chromosome.get(i).allele());
                        }

                        long endTime = System.nanoTime();
                        long duration = (endTime - startTime); 
                        sumatime[0] += duration;


                        csvWriter.append("," + duration + "," + sumatime[0]);
                        csvWriter.append(",").append(String.format("%.2f", CpuUsage.getCpuUsage().getUsage()));
                        csvWriter.append("\n");

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
                .collect(EvolutionResult.toBestPhenotype());

            System.out.println(statistics);
         } catch (IOException e) {
            System.err.println("Error al escribir el archivo CSV: " + e.getMessage()); 
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage()); 
        }
        // finally {
        //     executor.shutdown();
        // }
    }
    // Función de evaluación adaptada para Jenetics
    private static double evaluar(Genotype<IntegerGene> genotype) {
        return FUNCION_EVALUACION.evaluar(genotype);
    }
}