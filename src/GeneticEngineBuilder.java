import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.DoubleChromosome;
import io.jenetics.Mutator;
import io.jenetics.Optimize;
import io.jenetics.SinglePointCrossover;
import io.jenetics.UniformCrossover;
import io.jenetics.engine.Engine;
import io.jenetics.Alterer;
import io.jenetics.TournamentSelector;
import io.jenetics.EliteSelector;
import io.jenetics.util.Factory;
import java.util.List;
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

    private double calcularTasaMutacion(int generation, boolean aumentoTemporal) {
        double tasaBase = config.MUTATION_RATE;
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
        Factory<Genotype<DoubleGene>> genotypeFactory = Genotype.of(DoubleChromosome.of(1, 5, 60));
        double tasaMutacion = calcularTasaMutacion(currentGeneration, aumentoTemporal);
        double tasaCruce = calcularTasaCruce(currentGeneration);
        
        Alterer<DoubleGene, Double> crossoverOperator = config.USE_UNIFORM_CROSSOVER 
            ? new UniformCrossover<DoubleGene, Double>(tasaCruce) 
            : new SinglePointCrossover<DoubleGene, Double>(tasaCruce);

        return Engine.builder(fitnessFunction, genotypeFactory)
                .executor(executorService)
                .alterers(
                        new Mutator<DoubleGene, Double>(tasaMutacion),
                        crossoverOperator
                )
                // Reducir el tamaño del torneo a 3 para favorecer la exploración
                .selector(new TournamentSelector<>(3))
                // Fracción elitista ajustada a 0.10
                .offspringSelector(new EliteSelector<>())
                .offspringFraction(0.10)
                .populationSize(config.INITIAL_POPULATION_SIZE)
                .optimize(Optimize.MAXIMUM)
                .build();
    }

    public Engine<DoubleGene, Double> buildEngineWithPopulation(Function<Genotype<DoubleGene>, Double> fitnessFunction, List<Genotype<DoubleGene>> initialPopulation) {
        return Engine.builder(fitnessFunction, () -> null)
                .executor(executorService)
                .alterers(
                        new Mutator<DoubleGene, Double>(config.MUTATION_RATE),
                        config.USE_UNIFORM_CROSSOVER 
                          ? new UniformCrossover<DoubleGene, Double>(config.CROSSOVER_RATE) 
                          : new SinglePointCrossover<DoubleGene, Double>(config.CROSSOVER_RATE)
                )
                .populationSize(config.INITIAL_POPULATION_SIZE)
                .optimize(Optimize.MAXIMUM)
                .build();
    }

    public void shutdownExecutor() {
        executorService.shutdown();
    }
}
