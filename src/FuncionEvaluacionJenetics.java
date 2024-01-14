
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.IntegerGene;

import java.util.concurrent.Semaphore;

import EDU.gatech.cc.is.util.Vec2;

public class FuncionEvaluacionJenetics {
    
    private int MAXDIF = 1000; // Valor maximo de diferenca de goles favorables, se utiliza para considerar que el individuo es el mejor posible.   
    // Se utiliza para evaluar a los individuos de una población

    void FuncionEvaluaciona(int ptos) { // Constructor, se puede parametrizar un objetivo o cualquier valor necesario
        MAXDIF = ptos; // En este caso solo se inicializa el valor de MAXDIF
    }

    public double evaluar(Genotype<DoubleGene> genotype) {
        // Verificar si el genotipo tiene cromosomas y genes
        if (genotype.length() == 0 || genotype.get(0).length() == 0) {
            System.out.println("El genotipo no tiene ningún cromosoma o el primer cromosoma no tiene ningún gen");
            return -2; // Valor específico para genotipo vacío
        }

        // Extrae y valida los parámetros del genotipo
        if (!sonParametrosValidos(genotype)) {
            return -1; // Parámetros inválidos
        }else{
            // System.out.println("Parámetros válidos");
        }

         // Extracción y escalado de parámetros.
         double margin = genotype.get(0).get(0).allele(); // Escalado a rango [0, 1]
         double range = genotype.get(1).get(0).allele(); // Escalado a rango [0, 1]
         double teammateG = genotype.get(2).get(0).allele(); // Escalado a rango [0, 10]
         double wallG = genotype.get(3).get(0).allele(); // Escalado a rango [0, 10]
         double goalieG = genotype.get(4).get(0).allele(); // Escalado a rango [0, 10]
         double forceLimit = genotype.get(5).get(0).allele(); // Escalado a rango [0, 10]
         double sideAllele = genotype.get(6).get(0).allele(); // Escalado a rango [-1, 1]
 
 
         int side;
         if (sideAllele > 0) {
             side = 1;
         } else {
             side = -1;
         }
 
         double forward_angle = genotype.get(7).get(0).allele(); // Escalado a rango [0, 2*PI]
         double goalie_x = genotype.get(8).get(0).allele(); // Escalado a rango [-1, 1]
         Vec2 offensive_pos1 = new Vec2(genotype.get(9).get(0).allele(), genotype.get(10).get(0).allele()); // Escalado a rango [-1, 1]
         Vec2 offensive_pos2 = new Vec2(genotype.get(11).get(0).allele(), genotype.get(12).get(0).allele()); // Escalado a rango [-1, 1]

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
            new_robotos[i] = new NewRobotSpec("EDU.gatech.cc.is.abstractrobot.SocSmallSim", "AIKHomoG",
                    posx[i], posy[i], theta[i], forecolor1, backcolor1, vclas[i]);
        for (int i = 5; i < 10; i++)
            new_robotos[i] = new NewRobotSpec("EDU.gatech.cc.is.abstractrobot.SocSmallSim", "BrianTeam",
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
        // Enviamos parámetros a los agentes (robots) del equipo AIKHomoG en este caso
        // Enviamos parámetros a los agentes (robots) del equipo AIKHomoG en este caso

        // Enviamos parámetros a los agentes (robots) del equipo 1 (BasicTeamAG) en este caso
        for (int ri = 0; ri < 5; ri++)
            ((AIKHomoG) (tb.simulation.control_systems[ri])).setParam(margin, range, teammateG, wallG, goalieG,
                    forceLimit, side, forward_angle, goalie_x, offensive_pos1, offensive_pos2);

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
        //System.out.println(genotype.toString());
        // System.out.println("\t(FITNESS:" + (MAXDIF + diff) + " DIFF. GOLES:" + diff +")"); // 
        

        // Retorna la evaluación del cromosoma basada en la diferencia de goles.
        // A mayor valor, mejor es la evaluación.
        return Math.max(0, MAXDIF + Math.abs(diff));

    }

    private boolean sonParametrosValidos(Genotype<DoubleGene> genotype) {
        
        // Extracción y escalado de parámetros.
        double margin = genotype.get(0).get(0).allele(); // Escalado a rango [0, 1]
        double range = genotype.get(1).get(0).allele(); // Escalado a rango [0, 1]
        double teammateG = genotype.get(2).get(0).allele(); // Escalado a rango [0, 10]
        double wallG = genotype.get(3).get(0).allele(); // Escalado a rango [0, 10]
        double goalieG = genotype.get(4).get(0).allele(); // Escalado a rango [0, 10]
        double forceLimit = genotype.get(5).get(0).allele(); // Escalado a rango [0, 10]
        double sideAllele = genotype.get(6).get(0).allele(); // Escalado a rango [-1, 1]


        int side;
        if (sideAllele > 0) {
            side = 1;
        } else {
            side = -1;
        }

        double forward_angle = genotype.get(7).get(0).allele(); // Escalado a rango [0, 2*PI]
        double goalie_x = genotype.get(8).get(0).allele(); // Escalado a rango [-1, 1]
        Vec2 offensive_pos1 = new Vec2(genotype.get(9).get(0).allele(), genotype.get(10).get(0).allele()); // Escalado a rango [-1, 1]
        Vec2 offensive_pos2 = new Vec2(genotype.get(11).get(0).allele(), genotype.get(12).get(0).allele()); // Escalado a rango [-1, 1]

        // Validación de parámetros
        if (margin < 0 || margin > 1) {
            System.out.println("Parámetro inválido: margin = " + margin);
            return false;
        }
        if (range < 0 || range > 1) {
            System.out.println("Parámetro inválido: range = " + range);
            return false;
        }
        if (teammateG < 0 || teammateG > 10) {
            System.out.println("Parámetro inválido: teammateG = " + teammateG);
            return false;
        }
        if (wallG < 0 || wallG > 10) {
            System.out.println("Parámetro inválido: wallG = " + wallG);
            return false;
        }
        if (goalieG < 0 || goalieG > 10) {
            System.out.println("Parámetro inválido: goalieG = " + goalieG);
            return false;
        }
        if (forceLimit < 0 || forceLimit > 10) {
            System.out.println("Parámetro inválido: forceLimit = " + forceLimit);
            return false;
        }
        if (side != -1 && side != 1) {
            System.out.println("Parámetro inválido: side = " + side);
            return false;
        }
        if (forward_angle < 0 || forward_angle > 2 * Math.PI) {
            System.out.println("Parámetro inválido: forward_angle = " + forward_angle);
            return false;
        }
        if (goalie_x < -1 || goalie_x > 1) {
            System.out.println("Parámetro inválido: goalie_x = " + goalie_x);
            return false;
        }

        if (offensive_pos1.x < -1 || offensive_pos1.x > 1) {
            System.out.println("Parámetro inválido: offensive_pos1.x = " + offensive_pos1.x);
            return false;
        }
        if (offensive_pos1.y < -1 || offensive_pos1.y > 1) {
            System.out.println("Parámetro inválido: offensive_pos1.y = " + offensive_pos1.y);
            return false;
        }
        if (offensive_pos2.x < -1 || offensive_pos2.x > 1) {
            System.out.println("Parámetro inválido: offensive_pos2.x = " + offensive_pos2.x);
            return false;
        }
        if (offensive_pos2.y < -1 || offensive_pos2.y > 1) {
            System.out.println("Parámetro inválido: offensive_pos2.y = " + offensive_pos2.y);
            return false;
        }

        return true;

    }

    // Método auxiliar para imprimir un Genotype.
    public static void printlna(Genotype<IntegerGene> genotype) {
        System.out.print("\tGenotype: ");


    }
}
