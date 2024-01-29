/*
 * MainJenetics.java
 * Main principal para ejecutar el algoritmo genético
 */

import io.jenetics.*;
import io.jenetics.engine.*;
import io.jenetics.util.Factory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class MainJenetics {
    private static final FuncionEvaluacionJenetics FUNCION_EVALUACION = new FuncionEvaluacionJenetics();

    public static void main(String[] args) {
        // Definir el número de hilos basado en los recursos disponibles
        int taskId = args.length >  0 ? Integer.parseInt(args[0]) : 0;

        String slurmCpusPerTask = System.getenv("SLURM_CPUS_PER_TASK");
        int numThreads = slurmCpusPerTask != null ? Integer.parseInt(slurmCpusPerTask) : 1;

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        Factory<Genotype<DoubleGene>> genotypeFactory = Genotype.of(
        DoubleChromosome.of(0, 10, 38)
        );

        Engine<DoubleGene, Double> engine = Engine.builder(MainJenetics::evaluar, genotypeFactory)
            .populationSize(10 + taskId * 10)
            .survivorsSelector(new TournamentSelector<>(5))
            .offspringSelector(new RouletteWheelSelector<>())
            .alterers(new Mutator<>(0.05), new SinglePointCrossover<>(0.5))
            .executor(executorService)

            .build();

        EvolutionStatistics<Double, ?> statistics = EvolutionStatistics.ofNumber();

        try {
            Phenotype<DoubleGene, Double> resultado = engine.stream()
                .limit(Limits.bySteadyFitness(10000))
                .parallel()
                .limit(10) // Change this to the desired number of generations
                .peek(result -> {
                    System.out.println("Generación: " + result.generation());
                    // Task ID is useful for debugging
                    System.out.println("Task ID: " + taskId);
                    System.out.println("Tamaño de la población: " + result.population().size());
                    System.out.println("CPU: " + Runtime.getRuntime().availableProcessors());
                    // Si deseas imprimir toda la población, puedes descomentar la siguiente línea
                    // result.population().forEach(individual -> System.out.println(individual));
                })
                .peek(statistics)
                .collect(EvolutionResult.toBestPhenotype());

            System.out.println("Mejor resultado: " + resultado.genotype());
            System.out.println("Mejor aptitud: " + resultado.fitness());
            System.out.println("Estadísticas: " + statistics);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown(); // Ensure your executor service is shut down to avoid resource leaks
        }
    }

    private static double evaluar(Genotype<DoubleGene> genotype) {
        try {
            return FUNCION_EVALUACION.evaluar(genotype);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        }
    }
}