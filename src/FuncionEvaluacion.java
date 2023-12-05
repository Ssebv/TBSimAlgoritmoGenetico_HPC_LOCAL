/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import java.util.concurrent.Semaphore;
import org.jgap.*;

/**
 *
 * @author kbern
 */

public class FuncionEvaluacion extends FitnessFunction {


    private int MAXDIF = 1000;
    
    //Inicializa cualquier valor necesario para poder evaluar
    FuncionEvaluacion(int ptos){
        MAXDIF = ptos;
    }


    
    //Este método debe mantener su nombre y parámetros, solo cambiar implementación del cuerpo
    //Este método es usado internamente por "evolve" para evaluar a los individuos de una población
    public double evaluate(IChromosome cromosoma){
        
        //Arreglos que se pasaran como parámetros a los 5  agentes.        
        //superada esta distancia, busca volver a su posición de juego
        Integer[] disPos = new Integer[5];  

        //superada esta distancia, busca colocarse en posición de patear la pelota
        Integer[] disKick = new Integer[5]; //distancia ea la pelota para patearla

        //si esta a menos de esta distancia de compañeros, intenta alejarse de ellos
        Integer[] disTeam = new Integer[5]; //distancia a sus compañeros
        
        //Obtiene los 15 alelos del "cromosoma" a evaluar y los almacena en 3 arreglos de tamaño 5
        for (int jj=0; jj<5; jj++){
            disPos[jj] = (Integer)cromosoma.getGene(jj).getAllele();
            disKick[jj] = (Integer)cromosoma.getGene(jj+5).getAllele();
            disTeam[jj] = (Integer)cromosoma.getGene(jj+10).getAllele();
        }
        
        //CANDIDATOS INVALIDOS o MALOS (se debe descartar asignando un MAL puntaje)               
        //Ejemplo: según la construcción del agente TeamBasic:
        // if (ball.r > this.disPos[mynum]) {
        //    result = backspot;
        // } 
        // else if (ball.r > this.disKick[mynum]) {
        //    result = kickspot;
        // } else {
        //    result = ball;
        // }
        //        
        // si no se cumple ball.r > this.disPos[mynum], entonces  this.disKick[mynum]
        // no puede ser mayor a this.disPos[mynum], pues entonces jamas patearía
        // esto quiere decir que si encontramos un cromosoma que parametrice el agente
        // con esta configuración, entonces el cromosoma deberá ser descartado evaluandolo muy mal
        for (int jj=0; jj<5; jj++){
            if (disKick[jj] >= disPos[jj]) {
                    // this.println(cromosoma);
                    // System.out.println("\t>>>>>>>(individuo descartado para simulacion)");
                    return 0; //retorna la peor evalaución posible
            }
        }
        
        //CANDIDATOS VALIDOS, evaluar según fitness
        //para este ejemplo el fitness es la diferencia de goles a favor.
        //para ello debemos ejecutar el simulador, enviar parametros al agente(s)
        //y obtener el resultado.
        int diff = 0;
        
        //Parametros para el simulador
        String forecolor1 = "xEAEA00";  //1er color equipo1
        String backcolor1 = "xFFFFFF";  //2do. color equipo1
        String forecolor2 = "xFF0000";  //1er color equipo2
        String backcolor2 = "x0000FF";  //2do. color equipo2
        double posx[] = {-1.2, -.5, -.15, -.15, -.15, 1.2, .5, .15, .15, .15}; //posición x en cancha para todos
        double posy[] = {0, 0, 0.5, 0, -0.5, 0, 0, 0.5, 0, -0.5}; //posición y en cancha para todos
        double theta[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; //rotación en cancha para todos
        int vclas[] = {1, 1, 1, 1, 1, 2, 2, 2, 2, 2}; //clase de visión (permite setear el lado)
        
        //Creamos 10 instancias de NewRobotSpec con la configuración para los 10 agentes.
        //indicamos la clase "BasicTeamAG" para equipo1 y "AIKHomoG" para equipo2
        NewRobotSpec[] new_robotos = new NewRobotSpec[10];
        for (int i=0;i<5; i++)
            new_robotos[i] = new NewRobotSpec("EDU.gatech.cc.is.abstractrobot.SocSmallSim", "BasicTeamAG",
                             posx[i],  posy[i], theta[i],  forecolor1, backcolor1, vclas[i]);
        for (int i=5;i<10; i++)
            new_robotos[i] = new NewRobotSpec("EDU.gatech.cc.is.abstractrobot.SocSmallSim", "AIKHomoG",
                             posx[i],  posy[i], theta[i],  forecolor2, backcolor2, vclas[i]);
        
        //instanciamos simulador sin gráficos
        TBSimNoGraphics tb = new TBSimNoGraphics (null, "robocup.dsc", new_robotos, 3, 2, 50);     
        tb.start();
        tb.sem1 = new Semaphore(0);
        try {tb.sem1.acquire();} catch (Exception e){System.out.println(e);}
        
        //Enviamos parámetros a los agentes
        //En este ejemplo a los primeros 5 (BasicTeamAG) se les envian los paramétros
        //propuesto por el algoritmo genético
        for (int ri=0; ri<5; ri++)     
            ((BasicTeamAG)(tb.simulation.control_systems[ri])).setParam(disPos, disKick, disTeam);
           

        //Iniciamos y esperamos a que termine simulación
        tb.sem2.release();
        try { tb.join(); } catch (Exception e){ System.out.println(e); }
        

        //El detalle del resultado se obtiene de la variable "estado" dentro del simulador
        //dividos y parseamos para obtener el último resultado y diferencia "diff"
        String[] line = tb.estado.split("\n");
        String[] lst = line[line.length - 1].split(",");
        diff = Integer.parseInt(lst[0]) - Integer.parseInt(lst[1]);  
        
        //Imprimimos resultado por pantalla
        // this.println(cromosoma);
        // System.out.println("\t(FITNESS:" + (MAXDIF+diff) + "   DIFF. GOLES:" + diff + ")");
 
        //Retornamos la evaluación del resultado, a mayor valor mejor es evaluado.
        return Math.max(0, MAXDIF+diff);
    }
    
    
    //Este método solo imprime el contenido de un cromosoma por pantalla
    public static void println(IChromosome cromosoma){
        System.out.print("\tCromosoma: ");
        for (int ri=0; ri<5; ri++)  
           System.out.print("["+(Integer)cromosoma.getGene(ri).getAllele()+","+
                   (Integer)cromosoma.getGene(ri+5).getAllele()+","+
                   (Integer)cromosoma.getGene(ri+10).getAllele()+"]");        
        System.out.println(); 
        
    }

}
