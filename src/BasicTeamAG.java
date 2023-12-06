/*
 * BasicTeam.java
 * Clase de control para un equipo de robots en un juego de fútbol simulado.
 * Utiliza un enfoque de algoritmo genético para optimizar el comportamiento del equipo.
 */

import EDU.gatech.cc.is.util.Vec2; // Libreria para el uso de vectores
import EDU.gatech.cc.is.abstractrobot.*; // Libreria para el uso de robots

public class BasicTeamAG extends ControlSystemSS { // Extiende ControlSystemSS para interactuar con Soccer Server.

    /* 
     * Variables para configurar los umbrales de distancia en relación con la pelota y otros jugadores. 
     * Estos valores son fundamentales para determinar el comportamiento estratégico de los robots.
     * El objetivo de este proceso es encontrar el conjunto de parámetros (disPos, disKick, disTeam) que resulta en el mejor rendimiento del equipo en la simulación.
     */

    // disPos: Es una distancia de umbral que determina cuándo un robot debe considerarse en posición para patear la pelota.
    // disKick: Define la distancia a la que un robot intentará patear la pelota.
    // disTeam: Puede ser una distancia para mantener entre compañeros de equipo para evitar colisiones o aglomeraciones.
    
    // Los valores iniciales sirven de punto de partida para la búsqueda de parámetros, pero no son los mejores, contando asi con una base de comparación.
    public double[] disPos = {0.6, 0.5, 0.3, 0.3, 0.3}; 
    public double[] disKick = {0.1, 0.4, 0.3, 0.3, 0.3}; 
    public double[] disTeam = {0.6, 0.1, 0.5, 0.4, 0.7};

    // Recolectar y almacenar los valores de la posición de la pelota en cada iteración para calcular la desviación estándar de la posición de la pelota al final de la simulación.
    public double sum_ballx= 0;  // Sumatoria de la posición de la pelota en x
    public long con_ballx= 0; // Contador de iteraciones
    
    public void Configure() { 
        // Método para configuraciones iniciales del robot. Actualmente no utilizado.
    }
    
    public void setParam(Integer[] disPos, Integer[] disKick, Integer[] disTeam){ // 
        for (int i=0; i<5; i++){ // Se convierten a enteros para poder ser usados en el algoritmo genético y contar con una normalización de los valores ya que el algoritmo genético solo trabaja con enteros en este caso ** Preguntar si es posible usar decimales en HPC
            this.disPos[i] = disPos[i]/10.0;
            this.disKick[i] = disKick[i]/10.0;
            this.disTeam[i] = disTeam[i]/10.0;
        }
    }

    /**
     * Método que se llama en cada iteración de la simulación
     * Retorna un valor entero que indica el estado del robot (CSSTAT_OK, CSSTAT_ERROR, CSSTAT_TIMEOUT)
     */
    public int TakeStep() {

        Vec2 result = new Vec2(0, 0); // Resultado de la acción del robot
        long curr_time = abstract_robot.getTime(); // Tiempo actual de la simulación

        /*--- Obtenemos la posición de la pelota ---*/

        Vec2 ball = abstract_robot.getBall(curr_time); // Posición de la pelota
        Vec2 ourgoal = abstract_robot.getOurGoal(curr_time); // Posición de nuestra portería
        Vec2 theirgoal = abstract_robot.getOpponentsGoal(curr_time); // Posición de la portería contraria

        Vec2[] teammates = abstract_robot.getTeammates(curr_time); // Posición de los compañeros de equipo

        /*--- Obtenemos la posición de los oponentes ---*/ // ** Preguntar si es necesario obtener la posición de los oponentes para el algoritmo genético

        //Vec2[] opponents = abstract_robot.getOpponents(curr_time);
        //for (int i=0;i<teammates.length;i++)
        //{
        ////System.out.println(i+" team "+teammates[i].x+" "+
        //teammates[i].y+" ");
        ////System.out.println(i+" opp  "+opponents[i].x+" "+
        //opponents[i].y);
        //}

        /*--- Obtenemos la posición del robot ---*/
        Vec2 closestteammate = new Vec2(99999, 0); // Posición del compañero de equipo más cercano
        for (int i = 0; i < teammates.length; i++) { // Se recorren todos los compañeros de equipo
            if (teammates[i].r < closestteammate.r) { // Se verifica si el compañero de equipo actual es el más cercano
                closestteammate = teammates[i];     // Si es asi se actualiza la posición del compañero de equipo más cercano
            }
        }


        /*--- Logica de control de los robots ---*/
        
        // Calcula el punto de patada, que es un lugar estratégico detrás de la pelota desde donde el robot puede patear efectivamente hacia la portería contraria.
        Vec2 kickspot = new Vec2(ball.x, ball.y); // Punto de patada
        kickspot.sub(theirgoal); // Se calcula la distancia entre la portería contraria y la pelota
        kickspot.setr(SocSmall.RADIUS); // Se establece la distancia entre la portería contraria y la pelota como el radio del robot
        kickspot.add(ball); // Se suma la posición de la pelota para obtener el punto de patada

        // Calcula un punto detrás de la pelota a una distancia mayor, utilizado para posiciones defensivas o de preparación.
        Vec2 backspot = new Vec2(ball.x, ball.y);
        backspot.sub(theirgoal); // Se calcula la dirección hacia la portería contraria.
        backspot.setr(SocSmall.RADIUS * 5); // Se ajusta esta posición a una distancia mayor detrás de la pelota.
        backspot.add(ball); // Se suma la posición de la pelota para obtener el punto de posición defensiva.

        // Calcula posiciones estratégicas al norte y al sur de la posición defensiva, útiles para maniobras de flanqueo o esquivar a oponentes.
        Vec2 northspot = new Vec2(backspot.x, backspot.y + 0.7); // Posición al norte de la posición defensiva.
        Vec2 southspot = new Vec2(backspot.x, backspot.y - 0.7); // Posición al sur de la posición defensiva.

        // Calcula la posición ideal para el portero, entre la pelota y nuestra portería.
        Vec2 goaliepos = new Vec2(ourgoal.x, ourgoal.y + ball.y); // Posición inicial basada en nuestra portería y la posición de la pelota.
        goaliepos.setr(goaliepos.r * 0.5); // Se ajusta la distancia para estar a medio camino entre la pelota y la portería.

        // Calcula una dirección para alejarse del compañero de equipo más cercano, útil para evitar aglomeraciones y mejorar la cobertura del campo.
        Vec2 awayfromclosest = new Vec2(closestteammate.x, closestteammate.y); // Posición inicial basada en el compañero más cercano.
        awayfromclosest.sett(awayfromclosest.t + Math.PI); // Se ajusta la dirección para alejarse del compañero.

        /*--- Asigna una posición objetivo a cada robot basándose en su número (rol en el equipo) ---*/
        int mynum = abstract_robot.getPlayerNumber(curr_time); // Obtiene el número del jugador (su rol en el equipo).

        // Acumula la distancia total de la pelota respecto a nuestra portería, útil para estadísticas o análisis del juego.
        sum_ballx += Math.abs((ourgoal.x-ball.x)*(ourgoal.x-ball.x));
        con_ballx += 1; // Incrementa el contador de iteraciones.

        /*--- Define la logica de movimiento de cada robot basandose en su número (rol en el equipo) ---*/
        // La estrategia varia si el robot es portero(Goalie), defensa(Back), mediocampista(Mid), delantero(Lead) o mediocampista ofensivo(Offensive Mid).
        // Los valores parametrizados (disPos, disKick, disTeam) son fundamentales para determinar el comportamiento estratégico de los robots.

        // Northspot, southspot, backspot, kickspot, goaliepos, awayfromclosest, ball, closestteammate
        // Cada uno de estos valores representa una posición en el campo de juego, y se utilizan para determinar la posición objetivo de cada robot.

        // --Descripcion:
        // * Northspot: Posición al norte de la posición defensiva.
        // * Southspot: Posición al sur de la posición defensiva.
        // * Backspot: Posición detrás de la pelota a una distancia mayor, utilizado para posiciones defensivas o de preparación.
        // * Kickspot: Punto de patada, que es un lugar estratégico detrás de la pelota desde donde el robot puede patear efectivamente hacia la portería contraria.
        // * Goaliepos: Posición ideal para el portero, entre la pelota y nuestra portería.
        // * Awayfromclosest: Dirección para alejarse del compañero de equipo más cercano, útil para evitar aglomeraciones y mejorar la cobertura del campo.
        // * Ball: Posición de la pelota.
        // * Closestteammate: Posición del compañero de equipo más cercano.

        /*--- Goalie - Portero ---*/
        if (mynum == 0) {

            if (ball.r > this.disPos[mynum]) { // Si la pelota esta a una distancia mayor a la distancia de posición
                result = goaliepos; // Se establece la posición del portero
            } else if (ball.r > this.disKick[mynum]) { // Si la pelota esta a una distancia mayor a la distancia de pateo
                result = kickspot; // Se establece la posición de pateo
            } else { // Si la pelota esta a una distancia menor a la distancia de pateo
                result = ball;  // Se establece la posición de la pelota
            }

        } /*--- Midback - Defensa ---*/ else if (mynum == 1) {

            if (ball.r > this.disPos[mynum]) { // Si la pelota esta a una distancia mayor a la distancia de posición
                result = backspot; // Se establece la posición de la defensa
            } else if (ball.r > this.disKick[mynum]) { // Si la pelota esta a una distancia mayor a la distancia de pateo
                result = kickspot; // Se establece la posición de pateo
            } else { // Si la pelota esta a una distancia menor a la distancia de pateo
                result = ball; // Se establece la posición de la pelota
            }

            if (closestteammate.r < this.disTeam[mynum]) { // Si el compañero de equipo más cercano esta a una distancia menor a la distancia de compañero
                result = awayfromclosest; // Se establece la posición para alejarse del compañero de equipo más cercano
            }

        }  /*--- Mid - Mediocampista ---*/ else if (mynum == 2) {

            if (ball.r > this.disPos[mynum]) {
                result = northspot;
            } else if (ball.r > this.disKick[mynum]) {
                result = kickspot;
            } else {
                result = ball;
            }

            if (closestteammate.r < this.disTeam[mynum]) {
                result = awayfromclosest;
            }

        } /*--- Lead - Delantero ---*/ else if (mynum == 3) {

            if (ball.r > this.disPos[mynum]) {
                result = southspot;
            } else if (ball.r > this.disKick[mynum]) {
                result = kickspot;
            } else {
                result = ball;
            }
            if (closestteammate.r < this.disTeam[mynum]) {
                result = awayfromclosest;
            }
        } /*--- Offensive Mid - Mediocampista ofensivo ---*/ else if (mynum == 4) {
            if (ball.r > this.disPos[mynum]) {
                result = backspot;
            } else if (ball.r > this.disKick[mynum]) {
                result = kickspot;
            } else {
                result = ball;
            }            
        }

        /*--- Envia la acción al robot ---*/
        abstract_robot.setSteerHeading(curr_time, result.t); // Establece la dirección del robot
        abstract_robot.setSpeed(curr_time, 1.0); // Establece la velocidad del robot

        // Si el robot puede patear la pelota
        if (abstract_robot.canKick(curr_time)) { 
            abstract_robot.kick(curr_time); // Si es asi, patea la pelota
        }

        // Confirmacion de que el paso se ha completado correctamente
        return (CSSTAT_OK);
    }
}
