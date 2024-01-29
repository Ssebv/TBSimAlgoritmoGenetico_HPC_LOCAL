import io.jenetics.*;
import io.jenetics.engine.*;
import io.jenetics.util.Factory;

import java.io.*;
import sun.misc.Signal;
import sun.misc.SignalHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainJeneticsCheckPoint {
    private static final FuncionEvaluacionJenetics FUNCION_EVALUACION = new FuncionEvaluacionJenetics();
    private static final String CHECKPOINT_PATH = "./checkpoints";

    public static void main(String[] args) {
        // Definir el número de hilos basado en los recursos disponibles
        int taskId = args.length > 0 ? Integer.parseInt(args[0]) : 0;
        String checkpointFilename = CHECKPOINT_PATH + "/checkpoint_" + taskId + ".ser";

        // Capturar señal para guardar checkpoint
        Signal.handle(new Signal("USR1"), new SignalHandler() {
            @Override
            public void handle(Signal sig) {
                System.out.println("Señal USR1 recibida, guardando checkpoint...");
                saveCheckpoint(loadCheckpoint(checkpointFilename), checkpointFilename);
            }
        });

        String slurmCpusPerTask = System.getenv("SLURM_CPUS_PER_TASK");
        int numThreads = slurmCpusPerTask != null ? Integer.parseInt(slurmCpusPerTask) : 1;

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        // Reanuda desde el checkpoint si existe
        Genotype<DoubleGene> recoveredGenotype = null;
        if(new File(checkpointFilename).exists()) {
            recoveredGenotype = loadCheckpoint(checkpointFilename);
        }

        final Genotype<DoubleGene> finalRecoveredGenotype = recoveredGenotype;
        Factory<Genotype<DoubleGene>> genotypeFactory = finalRecoveredGenotype != null ?
                () -> finalRecoveredGenotype :
                Genotype.of(DoubleChromosome.of(0, 10, 38));


        Engine<DoubleGene, Double> engine = Engine.builder(MainJeneticsCheckPoint::evaluar, genotypeFactory)
                .populationSize(10 + taskId) // Adjust this as necessary
                .survivorsSelector(new TournamentSelector<>(5))
                .offspringSelector(new RouletteWheelSelector<>())
                .alterers(new Mutator<>(0.05), new SinglePointCrossover<>(0.5))
                .executor(executorService)
                .build();

        EvolutionStatistics<Double, ?> statistics = EvolutionStatistics.ofNumber();

        try {
            Phenotype<DoubleGene, Double> result = engine.stream()
                .limit(Limits.bySteadyFitness(7))
                .peek(r -> saveCheckpoint(r.bestPhenotype().genotype(), checkpointFilename))
                .collect(EvolutionResult.toBestPhenotype());

            System.out.println("Mejor resultado: " + result.genotype());
            System.out.println("Mejor aptitud: " + result.fitness());
            System.out.println("Estadísticas: " + statistics);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown(); // Ensure your executor service is shut down to avoid resource leaks
        }
    }

    // Guardar el estado actual del algoritmo genético (checkpointing)
    private static void saveCheckpoint(Genotype<DoubleGene> genotype, String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(genotype);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Cargar el estado guardado del algoritmo genético (checkpointing)
    private static Genotype<DoubleGene> loadCheckpoint(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return (Genotype<DoubleGene>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
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
