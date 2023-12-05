import org.jgap.*;
import org.jgap.impl.*;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author kbern
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidConfigurationException {
        try {
            Configuration conf = new DefaultConfiguration();
            conf.setPreservFittestIndividual(true);
            conf.setKeepPopulationSizeConstant(false);

            //Ejemplo de individuos:
            //Inicializa ejemplo de Genes con 15 elementos (15 alelos)
            //cada el alelo es un parametro del agente BasicTeamAG
            //cada agente tiene 3 parámetros (3x5 agentes = 15)
            //
            //InterGene permite definir los rangos máximos y mínimos que puede tomar un alelo
            //Para este ejemplo los valores pueden ser entre 1 y 9

            Gene[] sampleGenes = new Gene[15]; 
            for (int jj=0; jj<15; jj++){
                sampleGenes[jj] = new IntegerGene(conf, 1, 9); 
            }

            //Establece tamaño de la población (cantidad de genes)
            conf.setPopulationSize(10);

            //Establece la funcion de fitness como una Clase FuncionEvaluación 
            //dicha clase es la que se debe implementar con la función de fitness. 
            //Se puede parametrizar un objetivo o cualquier valor necesario
            //En este caso solo se inicializa el valor de MAXDIF
            conf.setFitnessFunction(new FuncionEvaluacion(50));

            //Se estable un cromosoma de muestra como parte de la configuración.
            IChromosome sampleChromosome = new Chromosome(conf, sampleGenes);
            conf.setSampleChromosome(sampleChromosome);

            //inicializa la población
            Genotype poblacion = Genotype.randomInitialGenotype(conf);  

            // Inicialización del Archivo CSV
            try (FileWriter csvWriter = new FileWriter("ResultadosAlgoritmoGenetico.csv")) {
                // Escribir los títulos de las columnas
                csvWriter.append("Generación");
                csvWriter.append(",");
                csvWriter.append("Aptitud Mejor Individuo");
                // Valores del cromosoma
                csvWriter.append(",");
                csvWriter.append("DisPos1");
                csvWriter.append(",");
                csvWriter.append("DisPos2");
                csvWriter.append(",");
                csvWriter.append("DisPos3");
                csvWriter.append(",");
                csvWriter.append("DisPos4");
                csvWriter.append(",");
                csvWriter.append("DisPos5");
                csvWriter.append(",");
                csvWriter.append("DisKick1");
                csvWriter.append(",");
                csvWriter.append("DisKick2");
                csvWriter.append(",");
                csvWriter.append("DisKick3");
                csvWriter.append(",");
                csvWriter.append("DisKick4");
                csvWriter.append(",");
                csvWriter.append("DisKick5");
                csvWriter.append(",");
                csvWriter.append("DisTeam1");
                csvWriter.append(",");
                csvWriter.append("DisTeam2");
                csvWriter.append(",");
                csvWriter.append("DisTeam3");
                csvWriter.append(",");
                csvWriter.append("DisTeam4");
                csvWriter.append(",");
                csvWriter.append("DisTeam5");
                csvWriter.append("\n");


                System.out.println("Mejor individuo inicial");
                FuncionEvaluacion.println(poblacion.getFittestChromosome()); //muestra su conformación
                //iteramos una cantidad fija o bajo un criterio haciendo evoluacionar la población
                for (int i=0; i<3; i++) {
                    //Hace evoluacionar a la poblacion 
                    System.out.println("\nEvoluacionando poblacion " + (i) + "...");
                    poblacion.evolve();

                    //obtiene el mejor cromosoma de la población actual
                    //IChromosome mejor = poblacion.getFittestChromosome(); 
                    //System.out.println("\nResultados Evolucion poblacion " + i);
                    //FuncionEvaluacion.println(mejor); //muestra su conformación
                    //System.out.println("\tFitness:" + mejor.getFitnessValue()); //muestra su evaluación
                    // Escribir los datos de cada generación
                    IChromosome mejor = poblacion.getFittestChromosome();
                    csvWriter.append(String.valueOf(i));
                    csvWriter.append(",");
                    csvWriter.append(String.valueOf(mejor.getFitnessValue()));
                    csvWriter.append(",");
                    for (int jj=0; jj<5; jj++){
                        csvWriter.append(String.valueOf(mejor.getGene(jj).getAllele()));
                        csvWriter.append(",");
                    }
                    for (int jj=5; jj<10; jj++){
                        csvWriter.append(String.valueOf(mejor.getGene(jj).getAllele()));
                        csvWriter.append(",");
                    }
                    for (int jj=10; jj<15; jj++){
                        csvWriter.append(String.valueOf(mejor.getGene(jj).getAllele()));
                        csvWriter.append(",");
                    }
                    csvWriter.append("\n");

                }
            }
            //Si finaliza evoluacion, obtiene el mejor y lo imprime
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
