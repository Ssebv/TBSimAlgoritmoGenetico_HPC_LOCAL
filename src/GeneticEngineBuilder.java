import io.jenetics.DoubleGene;
import io.jenetics.DoubleChromosome;
import io.jenetics.Genotype;
import io.jenetics.engine.Engine;
import io.jenetics.Mutator;
import io.jenetics.SinglePointCrossover;
import io.jenetics.UniformCrossover;
import io.jenetics.RouletteWheelSelector; // Nuevo selector para mayor exploración
import io.jenetics.EliteSelector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class GeneticEngineBuilder {
    private final Configuracion config;
    private final ExecutorService executorService;

    public GeneticEngineBuilder(Configuracion config) {
        this.config = config;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    // Se aumenta la tasa base en un 20%
    private double calcularTasaMutacion(int generation, boolean aumentoTemporal) {
        double tasaBase = config.MUTATION_RATE * 1.2;
        if (aumentoTemporal) {
            tasaBase *= 1.5;
        }
        return Math.max(0.05, tasaBase * (1.0 - (generation / 10000.0)));
    }

    private double calcularTasaCruce(int generation) {
        double tasaBase = config.CROSSOVER_RATE;
        return Math.max(0.7, tasaBase * (1.0 - (generation / 20000.0)));
    }

    public Engine<DoubleGene, Double> buildEngine(Function<Genotype<DoubleGene>, Double> fitnessFunction, int currentGeneration, boolean aumentoTemporal) {
        Genotype<DoubleGene> genotypeFactory = Genotype.of(DoubleChromosome.of(1, 5, 60));
        double tasaMutacion = calcularTasaMutacion(currentGeneration, aumentoTemporal);
        double tasaCruce = calcularTasaCruce(currentGeneration);
        
        var crossoverOperator = config.USE_UNIFORM_CROSSOVER 
            ? new UniformCrossover<DoubleGene, Double>(tasaCruce) 
            : new SinglePointCrossover<DoubleGene, Double>(tasaCruce);

        return Engine.builder(fitnessFunction, genotypeFactory)
                .executor(executorService)
                // Aplicamos el mutador y el cruce
                .alterers(
                        new Mutator<DoubleGene, Double>(tasaMutacion),
                        crossoverOperator
                )
                // Reemplazamos el TournamentSelector por un RouletteWheelSelector para permitir mayor diversidad
                .selector(new RouletteWheelSelector<>())
                // Se reduce la influencia elitista; se mantiene el EliteSelector, pero podrías ajustar su fracción según convenga
                .offspringSelector(new EliteSelector<>(7))
                .offspringFraction(0.50) // Reducción de la fracción elitista para favorecer exploración
                .populationSize(config.INITIAL_POPULATION_SIZE)
                .optimize(config.OPTIMIZE)
                .build();
    }

    public Engine<DoubleGene, Double> buildEngineWithPopulation(Function<Genotype<DoubleGene>, Double> fitnessFunction, java.util.List<Genotype<DoubleGene>> initialPopulation) {
        return Engine.builder(fitnessFunction, () -> null)
                .executor(executorService)
                .alterers(
                        new Mutator<DoubleGene, Double>(config.MUTATION_RATE),
                        config.USE_UNIFORM_CROSSOVER 
                          ? new UniformCrossover<DoubleGene, Double>(config.CROSSOVER_RATE) 
                          : new SinglePointCrossover<DoubleGene, Double>(config.CROSSOVER_RATE)
                )
                .populationSize(config.INITIAL_POPULATION_SIZE)
                .optimize(config.OPTIMIZE)
                .build();
    }

    public void shutdownExecutor() {
        executorService.shutdown();
    }
}
