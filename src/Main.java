/*
 * Main.java
 * Punto de entrada principal para la ejecución del algoritmo genético utilizando JGAP.
 * Optimiza las estrategias de un equipo de fútbol robot simulado.
 */

import org.jgap.*;
import org.jgap.impl.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

// Import CpuUsageMonitor

public class Main {
    /**
     * @param args Argumentos de la línea de comandos
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
       
        try {
            // Configuración del algoritmo genético JGAP
            Configuration conf = new DefaultConfiguration();
            conf.setPreservFittestIndividual(true); // Conserva el mejor individuo
            conf.setKeepPopulationSizeConstant(false); // Permite que la población evolucione

            //Ejemplo de individuos:
            //Inicializa ejemplo de Genes con 15 elementos (15 alelos)
            //cada el alelo es un parametro del agente BasicTeamAG
            //cada agente tiene 3 parámetros (3x5 agentes = 15)


            Gene[] sampleGenes = new Gene[15]; 
            for (int jj=0; jj<15; jj++){
                sampleGenes[jj] = new IntegerGene(conf, 1, 9); // IntegerGene es un alelo de tipo entero entre 1 y 9 que permite representar los parámetros de los agentes
            }

            //Establece tamaño de la población (cantidad de genes) aqui se puede ir aumentando en cada iteración
            conf.setPopulationSize(10); // 10 individuos en la población porque son 10 agentes

            // Establece la función de evaluación (fitness) de los individuos
            conf.setFitnessFunction(new FuncionEvaluacion(50));


            // Define el cromosoma de muestra para la configuración
            IChromosome sampleChromosome = new Chromosome(conf, sampleGenes);

            conf.setSampleChromosome(sampleChromosome);

            conf.addGeneticOperator(new CrossoverOperator(conf, 0.35f)); // Operador de cruce

            // Inicializa la población de genotipos de manera aleatoria
            Genotype poblacion = Genotype.randomInitialGenotype(conf);
            
            

            // Inicialización del Archivo CSV
            // Configuración inicial del archivo CSV para almacenar resultados
            try (FileWriter csvWriter = new FileWriter("ResultadosAlgoritmoGenetico.csv")) {
                // Escribe los encabezados de las columnas en el archivo CSV
                csvWriter.append("Generación,Aptitud Mejor Individuo,DisPos1,DisPos2,DisPos3,DisPos4,DisPos5,DisKick1,DisKick2,DisKick3,DisKick4,DisKick5,DisTeam1,DisTeam2,DisTeam3,DisTeam4,DisTeam5,Tiempo por generacion,Tiempo Acumulado,Uso CPU (%)\n");
                long sumatime = 0;
                // Realiza la evolución por un número determinado de generaciones
                for (int i = 0; i < 250; i++) {
                    long startTime = System.nanoTime(); 
                    // System.out.println("\n\nGENERACION " + i + ":");
                    poblacion.evolve(); // Evoluciona la población aaaaa
                    CpuUsage usage = CpuUsage.getCpuUsage();
                    System.out.println("Cpu usage: " + usage.getUsage());
                   
                    long endTime = System.nanoTime();
                    long duration = (endTime - startTime) / 1000000; // Tiempo de ejecución de la generación actual
                    sumatime += duration;   

                    // Obtiene el mejor cromosoma de la generación actual
                    IChromosome mejor = poblacion.getFittestChromosome();

                    // Escribe los resultados de la generación actual en el archivo CSV
                    csvWriter.append(String.valueOf(i)).append(",");
                    csvWriter.append(String.valueOf(mejor.getFitnessValue())).append(",");
                    for (int jj = 0; jj < 15; jj++) {
                        csvWriter.append(String.valueOf(mejor.getGene(jj).getAllele()));
                        if (jj < 14) csvWriter.append(",");
                    }
                    csvWriter.append(",").append(String.valueOf(duration));
                    csvWriter.append(",").append(String.valueOf(sumatime));

                    csvWriter.append(",").append(String.format("%.2f", usage.getUsage())); // Uso de CPU como porcentaje

                    csvWriter.append("\n");
                    System.out.println("\tMejor cromosoma de la generación: " +  i);
                    FuncionEvaluacion.println(mejor); //muestra su conformación
                    System.out.println("\tFitness:" + mejor.getFitnessValue()); //muestra su evaluación

                }
            }

            // Imprime el mejor individuo encontrado después de todas las generaciones
            IChromosome mejor = poblacion.getFittestChromosome();
            System.out.println("\n\nMEJOR INDIVIDUO:");
            FuncionEvaluacion.println(mejor); //muestra su conformación
            System.out.println("\tFitness:" + mejor.getFitnessValue()); //muestra su evaluación

        } catch (InvalidConfigurationException e) {
            System.err.println("Error de configuración en JGAP: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo CSV: " + e.getMessage());
        }
    }
}
