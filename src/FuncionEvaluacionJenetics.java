
/*
 * FuncionEvaluacionJenetics.java 
 */
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import java.util.concurrent.Semaphore;

public class FuncionEvaluacionJenetics {

    private int MAXDIF = 1000; // Valor maximo de diferenca de goles favorables, se utiliza para considerar que
                               // el individuo es el mejor posible.
    // Se utiliza para evaluar a los individuos de una población

    void FuncionEvaluaciona(int ptos) { // Constructor, se puede parametrizar un objetivo o cualquier valor necesario
        MAXDIF = ptos; // En este caso solo se inicializa el valor de MAXDIF
    }

    public Integer[] convertGenotypeToParams(Genotype<DoubleGene> genotype) {
        Integer[] params = new Integer[38];
        int index = 0;
        for (int i = 0; i < genotype.length(); i++) {
            for (int j = 0; j < genotype.get(i).length(); j++) {
                params[index++] = (int) (genotype.get(i).get(j).doubleValue() * 10);
            }
        }
        // System.out.println("Longitud del genotipo: " + genotype.length());
        // System.out.println("Longitud total de genes: " +
        // genotype.stream().mapToInt(ch -> ch.length()).sum());

        return params;
    }

    public double evaluar(Genotype<DoubleGene> genotype) {
        // Verificar si el genotipo tiene cromosomas y genes
        if (genotype.length() == 0 || genotype.get(0).length() == 0) {
            // System.out.println("El genotipo no tiene ningún cromosoma o el primer
            // cromosoma no tiene ningún gen");
            return -2; // Valor específico para genotipo vacío
        }

        // Inicializar simulador
        int diff = 0;

        // Configuracion de los parámetros del simulador, como colores de los equipos y
        // posiciones iniciales.
        String forecolor1 = "xEAEA00"; // 1er color equipo1
        String backcolor1 = "xFFFFFF"; // 2do. color equipo1
        String forecolor2 = "xFF0000"; // 1er color equipo2
        String backcolor2 = "x0000FF"; // 2do. color equipo2
        double posx[] = { -1.2, -.5, -.15, -.15, -.15, 1.2, .5, .15, .15, .15 }; // posición x en cancha para todos
        double posy[] = { 0, 0, 0.5, 0, -0.5, 0, 0, 0.5, 0, -0.5 }; // posición y en cancha para todos
        double theta[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }; // rotación en cancha para todos
        int vclas[] = { 1, 1, 1, 1, 1, 2, 2, 2, 2, 2 }; // clase de visión (permite setear el lado)

        // Creamos 10 instancias de NewRobotSpec con la configuración para los 10 agentes
        NewRobotSpec[] new_robotos = new NewRobotSpec[10];
        for (int i = 0; i < 5; i++)
            new_robotos[i] = new NewRobotSpec("EDU.gatech.cc.is.abstractrobot.SocSmallSim", "DoogHomoG",
                    posx[i], posy[i], theta[i], forecolor1, backcolor1, vclas[i]);
        for (int i = 5; i < 10; i++)
            new_robotos[i] = new NewRobotSpec("EDU.gatech.cc.is.abstractrobot.SocSmallSim", "BrianTeam",
                    posx[i], posy[i], theta[i], forecolor2, backcolor2, vclas[i]);

        // Instanciamos simulador sin gráficos con el archivo de configuración
        // "robocup.dsc" y los 10 agentes
        TBSimNoGraphics tb = new TBSimNoGraphics(null, "robocup.dsc", new_robotos, 3, 2, 50);

        tb.start(); // Inicia simulación en un hilo aparte
        tb.sem1 = new Semaphore(0); // Semáforo para sincronizar la simulación donde se envían los parámetros a los
                                    // agentes
        try {
            tb.sem1.acquire(); // Espera a que la simulación inicie
        } catch (Exception e) {
            System.out.println(e);
        }

        // Convertir genotipo a parámetros
        Integer[] params = convertGenotypeToParams(genotype);
        // System.out.println("Longitud del arreglo de parámetros: " + params.length);

        // Configurar parámetros en robots
        for (int i = 0; i < 5; i++) {
            DoogHomoG robot = (DoogHomoG) (tb.simulation.control_systems[i]);
            robot.setParam(params);
        }


        tb.sem2.release(); // Libera la simulación para que continúe
        try {
            tb.join(); // Espera a que termine la simulación
        } catch (Exception e) {
            System.out.println(e);
        }

        // Calcula la diferencia de goles a favor al final de la simulación. Estado es
        // una cadena de texto con el detalle de los resultados de cada partido.
        String[] line = tb.estado.split("\n");
        String[] lst = line[line.length - 1].split(",");
        // Ciomprobar el tipo de dato de lst[0] y lst[1]
        // System.out.println("lst[0] = " + lst[0] + " lst[1] = " + lst[1]);

        // Validacion de no esta vacia y sean validas como numeros enteros
        if (!lst[0].isEmpty() && lst[0].matches("\\d+")) {
            diff = Integer.parseInt(lst[0]) - Integer.parseInt(lst[1]);
        } else {
            diff = 0;
            // System.out.println("Empate");
        }

        // System.out.println("Diferencia de goles: " + diff);

        return Math.max(0, MAXDIF + Math.abs(diff));

    }
}
