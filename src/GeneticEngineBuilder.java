import io.jenetics.*;
import io.jenetics.engine.Engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * GeneticEngineBuilder.java
 *
 * Construye motores genéticos configurables usando Jenetics.
 * Permite personalizar selección, cruce, mutación, y la arquitectura paralela.
 */
public class GeneticEngineBuilder {

    private final Configuracion config;
    private final LogManager logManager; // ✅ NUEVO
    private final ExecutorService executorService;
    private double mutationRate;

    public GeneticEngineBuilder(Configuracion config, LogManager logManager) {
        this.config = config;
        this.logManager = logManager; // ✅ NUEVO

        int availableCPUs = Runtime.getRuntime().availableProcessors();
        int coresToUse = Math.min(config.getNumCores(), availableCPUs);
        logManager.logInfo("CPUs disponibles (JVM): " + availableCPUs);
        logManager.logInfo("Núcleos configurados (config.NUM_CORES): " + coresToUse);

        this.executorService = Executors.newFixedThreadPool(coresToUse);
    }

    /**
     * Construye un motor genético adaptando tasas de mutación/cruce según la
     * generación.
     */
    public Engine<DoubleGene, Double> buildEngine(
            Function<Genotype<DoubleGene>, Double> fitnessFunction,
            int generation,
            boolean aumentoTemporal) {

        Genotype<DoubleGene> factory = createCustomGenotype();

        double tasaMutacion = (mutationRate > 0)
                ? mutationRate
                : calcularTasaMutacion(generation, aumentoTemporal);

        double tasaCruce = calcularTasaCruce(generation);

        Alterer<DoubleGene, Double> crossoverOperator = config.USE_UNIFORM_CROSSOVER
                ? new UniformCrossover<>(tasaCruce)
                : new SinglePointCrossover<>(tasaCruce);

        return Engine.builder(fitnessFunction, factory)
                .executor(executorService)
                .alterers(new Mutator<>(tasaMutacion), crossoverOperator)
                .selector(new TournamentSelector<>(5))
                .offspringSelector(new EliteSelector<>(config.ELITE_COUNT))
                .offspringFraction(0.50)
                .populationSize(config.INITIAL_POPULATION_SIZE)
                .optimize(config.OPTIMIZE)
                .build();
    }

    /**
     * Construye un motor genético reutilizando una población inicial ya generada.
     */
    public Engine<DoubleGene, Double> buildEngineWithPopulation(
            Function<Genotype<DoubleGene>, Double> fitnessFunction,
            List<Genotype<DoubleGene>> initialPopulation) {

        Genotype<DoubleGene> factory = createCustomGenotype();

        Alterer<DoubleGene, Double> crossoverOperator = config.USE_UNIFORM_CROSSOVER
                ? new UniformCrossover<>(config.CROSSOVER_RATE)
                : new SinglePointCrossover<>(config.CROSSOVER_RATE);

        return Engine.builder(fitnessFunction, factory)
                .executor(executorService)
                .alterers(new Mutator<>(config.MUTATION_RATE), crossoverOperator)
                .populationSize(config.INITIAL_POPULATION_SIZE)
                .optimize(config.OPTIMIZE)
                .build();
    }

    /**
     * Cambia dinámicamente la tasa de mutación usada por el motor genético.
     */
    public void setMutationRate(double newRate) {
        this.mutationRate = newRate;
    }

    /**
     * Cierra correctamente el pool de hilos al finalizar la ejecución.
     */
    public void shutdownExecutor() {
        executorService.shutdown();
    }

    /**
     * Calcula una tasa de mutación decreciente a lo largo de las generaciones.
     */
    private double calcularTasaMutacion(int generation, boolean boost) {
        double base = config.MUTATION_RATE * 1.2;
        if (boost)
            base *= 1.5;
        return Math.max(0.05, base * (1.0 - (generation / 10000.0)));
    }

    /**
     * Calcula una tasa de cruce decreciente con el paso de las generaciones.
     */
    private double calcularTasaCruce(int generation) {
        double base = config.CROSSOVER_RATE;
        return Math.max(0.7, base * (1.0 - (generation / 20000.0)));
    }

    /**
     * Crea un Genotype personalizado basado en los rangos definidos para los
     * robots.
     */
    private Genotype<DoubleGene> createCustomGenotype() {
        List<DoubleChromosome> chromosomes = new ArrayList<>();

        // Parámetros por jugador (5 jugadores × 10 parámetros)
        for (int i = 0; i < 5; i++) {
            chromosomes.add(DoubleChromosome.of(0, 100)); // forceLimit
            chromosomes.add(DoubleChromosome.of(0, 100)); // defenseWeight
            chromosomes.add(DoubleChromosome.of(0, 100)); // attackWeight
            chromosomes.add(DoubleChromosome.of(0, 1)); // passThreshold
            chromosomes.add(DoubleChromosome.of(0, 2)); // opponentAvoidance
            chromosomes.add(DoubleChromosome.of(0, 10)); // speed
            chromosomes.add(DoubleChromosome.of(0, 100)); // shootingAccuracy
            chromosomes.add(DoubleChromosome.of(0, 100)); // passingAccuracy
            chromosomes.add(DoubleChromosome.of(0, 100)); // positioning
            chromosomes.add(DoubleChromosome.of(0, 1)); // relleno u otro
        }

        // Parámetros generales del equipo (puedes ajustar según tu modelo)
        for (int i = 0; i < 10; i++) {
            chromosomes.add(DoubleChromosome.of(0, 100));
        }

        return Genotype.of(chromosomes);
    }
}
