import io.jenetics.Chromosome;
import io.jenetics.Genotype;
import io.jenetics.IntegerGene;

import java.util.concurrent.Semaphore;

public class FuncionEvaluacionJenetics {
    
    private int MAXDIF = 1000; // Valor maximo de diferenca de goles favorables, se utiliza para considerar que el individuo es el mejor posible.   
    // Se utiliza para evaluar a los individuos de una población

    void FuncionEvaluaciona(int ptos) { // Constructor, se puede parametrizar un objetivo o cualquier valor necesario
        MAXDIF = ptos; // En este caso solo se inicializa el valor de MAXDIF
    }

    public double evaluar(Genotype<IntegerGene> genotype) {
        // Extraer los valores de los genes
        Integer[] disPos = new Integer[5];
        Integer[] disKick = new Integer[5];
        Integer[] disTeam = new Integer[5];

        // Asume que cada cromosoma representa un conjunto de parámetros para un agente (robot)
        Chromosome<IntegerGene> chromosome = genotype.chromosome();
        for (int i = 0; i < 5; i++) {
            disPos[i] = chromosome.get(i).allele();
            disKick[i] = chromosome.get(i + 5).allele();
            disTeam[i] = chromosome.get(i + 10).allele();
        }
        // Revisa si los parámetros son válidos antes de ejecutar la simulación
        for (int i = 0; i < 5; i++) { 
            if (disKick[i] >= disPos[i]) {
                return 0; // retorna la peor evaluación posible
            }
        }

        int diff = 0;

        // Configuracion de los parámetros del simulador, como colores de los equipos y posiciones iniciales.
        String forecolor1 = "xEAEA00"; // 1er color equipo1
        String backcolor1 = "xFFFFFF"; // 2do. color equipo1
        String forecolor2 = "xFF0000"; // 1er color equipo2
        String backcolor2 = "x0000FF"; // 2do. color equipo2
        double posx[] = { -1.2, -.5, -.15, -.15, -.15, 1.2, .5, .15, .15, .15 }; // posición x en cancha para todos
        double posy[] = { 0, 0, 0.5, 0, -0.5, 0, 0, 0.5, 0, -0.5 }; // posición y en cancha para todos
        double theta[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }; // rotación en cancha para todos
        int vclas[] = { 1, 1, 1, 1, 1, 2, 2, 2, 2, 2 }; // clase de visión (permite setear el lado)

        // Creamos 10 instancias de NewRobotSpec con la configuración para los 10 agentes
        // indicamos la clase "BasicTeamAG" para equipo1 y "AIKHomoG" para equipo2 pero esto no es relevante ya que se puede cambiar en el archivo de configuración
        NewRobotSpec[] new_robotos = new NewRobotSpec[10];
        for (int i = 0; i < 5; i++)
            new_robotos[i] = new NewRobotSpec("EDU.gatech.cc.is.abstractrobot.SocSmallSim", "BasicTeamAG",
                    posx[i], posy[i], theta[i], forecolor1, backcolor1, vclas[i]);
        for (int i = 5; i < 10; i++)
            new_robotos[i] = new NewRobotSpec("EDU.gatech.cc.is.abstractrobot.SocSmallSim", "AIKHomoG",
                    posx[i], posy[i], theta[i], forecolor2, backcolor2, vclas[i]);

        // Instanciamos simulador sin gráficos con el archivo de configuración "robocup.dsc" y los 10 agentes
        TBSimNoGraphics tb = new TBSimNoGraphics(null, "robocup.dsc", new_robotos, 3, 2, 50);

        tb.start(); // Inicia simulación en un hilo aparte
        tb.sem1 = new Semaphore(0); // Semáforo para sincronizar la simulación donde se envían los parámetros a los agentes
        try {
            tb.sem1.acquire(); // Espera a que la simulación inicie
        } catch (Exception e) {
            System.out.println(e);
        }

        // Enviamos parámetros a los agentes (robots) del equipo 1 (BasicTeamAG) en este caso
        for (int ri = 0; ri < 5; ri++)
            ((BasicTeamAG) (tb.simulation.control_systems[ri])).setParam(disPos, disKick, disTeam); // Setea los parámetros para el agente

        tb.sem2.release(); // Libera la simulación para que continúe
        try {
            tb.join(); // Espera a que termine la simulación
        } catch (Exception e) {
            System.out.println(e);
        }

        // Calcula la diferencia de goles a favor al final de la simulación. Estado es una cadena de texto con el detalle de los resultados de cada partido.
        String[] line = tb.estado.split("\n"); 
        String[] lst = line[line.length - 1].split(","); 
        diff = Integer.parseInt(lst[0]) - Integer.parseInt(lst[1]); // Diferencia de goles a favor


        // Se imprime el cromosoma y la evaluación del resultado
        // System.out.print("\tGenotype: ");
        // for (int i = 0; i < 5; i++) {
        //     System.out.print(genotype.get(i).get(i).allele() + "," + genotype.get(i + 5).get(i).allele() + ","
        //             + genotype.get(i + 10).get(i).allele() + " ");
        // }
        // System.out.println(genotype.toString());
        // System.out.println("\t(FITNESS:" + (MAXDIF + diff) + " DIFF. GOLES:" + diff +")"); // 
        

        // Retorna la evaluación del cromosoma basada en la diferencia de goles.
        // A mayor valor, mejor es la evaluación.
        return Math.max(0, MAXDIF + Math.abs(diff));

    }

    // Método auxiliar para imprimir un Genotype.
    public static void printlna(Genotype<IntegerGene> genotype) {
        System.out.print("\tGenotype: ");
        for (int i = 0; i < 5; i++) {
            System.out.print(genotype.get(i).get(i).allele() + "," + genotype.get(i + 5).get(i).allele() + ","
                    + genotype.get(i + 10).get(i).allele() + " ");
        }

        System.out.println();
    }
}
