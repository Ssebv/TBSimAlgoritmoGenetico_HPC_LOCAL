import io.jenetics.*;
import io.jenetics.engine.*;
import io.jenetics.util.Factory;

import java.io.FileWriter;
import java.io.IOException;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;

public class MainJenetics {
    private static final FuncionEvaluacionJenetics FUNCION_EVALUACION = new FuncionEvaluacionJenetics(); // Función de                                                                                // evaluación

    public static void main(String[] args) throws InterruptedException {

        Factory<Genotype<DoubleGene>> genotypeFactory = Genotype.of(
                DoubleChromosome.of(0, 10, 38) // 36 genes with values between 0 and 10
        );

        // Crear el motor genético
        Engine<DoubleGene, Double> engine = Engine
                .builder(MainJenetics::evaluar, genotypeFactory) // Función de evaluación y genotipo
                .populationSize(100) // Tamaño de la población aqui se podria aimentar en cada generacion
                .survivorsSelector(new TournamentSelector<>(5)) // Seleccionar los 5 mejores individuos
                .offspringSelector(new RouletteWheelSelector<>()) // Seleccionar los padres para la reproducción
                .alterers(
                        new Mutator<>(0.05), // Mutar el 5% de los genes
                        new SinglePointCrossover<>(0.5)) // Cruzar el 50% de los genes

                // .executor(Executors.newFixedThreadPool(8)) // 8 hilos sin utilizar parallel()
                .build();

        // Crear estadísticas
        final EvolutionStatistics<Double, ?> statistics = EvolutionStatistics.ofNumber();

        final long[] sumatime = new long[1];
        try (FileWriter csvWriter = new FileWriter("ResultadosAlgoritmoGenetico.csv")) {
            csvWriter.append(
                    "Generacion,Aptitud Mejor Individuo,disAtack,disDefend,disGoal,disGoalie,disWall,disTeammate,disOpponent,disBall,disCenter,disGoalCenter,disGoalOpponent,disGoalieOpponent,disWallOpponent,disTeammateOpponent,disBallOpponent,disCenterOpponent,disGoalCenterOpponent,disGoalOpponentOpponent,disGoalieOpponentOpponent,disWallOpponentOpponent,disTeammateOpponentOpponent,disBallOpponentOpponent,disCenterOpponentOpponent,disGoalCenterOpponentOpponent,disGoalOpponentOpponentOpponent,disGoalieOpponentOpponentOpponent,disWallOpponentOpponentOpponent,disTeammateOpponentOpponentOpponent,disBallOpponentOpponentOpponent,disCenterOpponentOpponentOpponent,disGoalCenterOpponentOpponentOpponent,tiempo,tiempo acumulado,uso de CPU\n");
            // Ejecutar el algoritmo genético
            Phenotype<DoubleGene, Double> resultado = engine.stream()
                    .limit(Limits.bySteadyFitness(10000))
                    .limit(15)

                    // .parallel() // Corregir si utilizo parallel() no puedo escribir el CSV porque
                    // se corrompe dado al paralelismo

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

                            System.out.println("Mejor individuo: " + mejorGenotipo);
                            System.out.println("Mejor aptitud: " + result.bestFitness());

                        } catch (IOException e) {
                            Thread.currentThread().interrupt();
                        }
                    })
                    .peek(statistics)
                    .collect(EvolutionResult.toBestPhenotype());
            // Finalizar executor
            // ((ExecutorService) engine.executor()).shutdown();

            System.out.println("Mejor resultado: " + resultado.genotype());
            System.out.println("Mejor aptitud: " + resultado.fitness());
            System.out.println("Estadísticas: " + statistics);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double evaluar(Genotype<DoubleGene> genotype) {
        return FUNCION_EVALUACION.evaluar(genotype);
    }
}