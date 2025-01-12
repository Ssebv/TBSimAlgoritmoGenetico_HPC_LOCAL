import java.util.concurrent.ExecutorService;

import io.jenetics.*;
import io.jenetics.engine.*;
import io.jenetics.util.*;

public class MotorGeneticoFactory {
    public static Engine<DoubleGene, Double> crear(Configuracion config, ExecutorService executor) {
        Factory<Genotype<DoubleGene>> genotypeFactory = Genotype.of(DoubleChromosome.of(1, 5, 60));
        return Engine.builder(MainJenetics::evaluar, genotypeFactory)
                .populationSize(config.INITIAL_POPULATION_SIZE)
                .alterers(
                        new Mutator<>(config.MUTATION_RATE),
                        new SinglePointCrossover<>(config.CROSSOVER_RATE))
                .survivorsSelector(new UniqueEliteSelector<>(3))
                .offspringSelector(new TournamentSelector<>(5))
                .executor(executor)
                .build();
    }

    public static void ejecutar(Engine<DoubleGene, Double> engine, int startGeneration, Configuracion config) {
        engine.stream()
                .limit(config.DEFAULT_GENERATIONS - startGeneration)
                .peek(MainJenetics::procesarResultadoGeneracion)
                .collect(EvolutionResult.toBestPhenotype());
    }

    public static Engine<DoubleGene, Double> actualizarMotorGenetico(ISeq<Phenotype<DoubleGene, Double>> nuevaPoblacion,
            Configuracion config) {
        return Engine.builder(MainJenetics::evaluar, Genotype.of(DoubleChromosome.of(1, 5, 60)))
                .populationSize((int) nuevaPoblacion.size())
                .alterers(
                        new Mutator<>(config.MUTATION_RATE),
                        new SinglePointCrossover<>(config.CROSSOVER_RATE))
                .survivorsSelector(new TournamentSelector<>(5))
                .offspringSelector(new StochasticUniversalSelector<>())
                .build();
    }
}
