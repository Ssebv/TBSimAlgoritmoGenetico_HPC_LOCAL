import org.jgap.*;
import org.jgap.impl.*;
/**
 *
 * @author kbern
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InvalidConfigurationException {
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
        
        System.out.println("Mejor individuo inicial");
        FuncionEvaluacion.println(poblacion.getFittestChromosome()); //muestra su conformación
        //iteramos una cantidad fija o bajo un criterio haciendo evoluacionar la población
        for (int i=0; i<100; i++) {
            //Hace evoluacionar a la poblacion 
            System.out.println("\nEvoluacionando poblacion " + (i) + "...");
            poblacion.evolve();

            //obtiene el mejor cromosoma de la población actual
            IChromosome mejor = poblacion.getFittestChromosome(); 
            System.out.println("\nResultados Evolucion poblacion " + i);
            FuncionEvaluacion.println(mejor); //muestra su conformación
            System.out.println("\tFitness:" + mejor.getFitnessValue()); //muestra su evaluación
        }
        
        //Si finaliza evoluacion, obtiene el mejor y lo imprime
        IChromosome mejor = poblacion.getFittestChromosome();
        System.out.println("\n\nMEJOR INDIVIDUO:");
        FuncionEvaluacion.println(mejor); //muestra su conformación
        System.out.println("\tFitness:" + mejor.getFitnessValue()); //muestra su evaluación
        
        
    }
}
