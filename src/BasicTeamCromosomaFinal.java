/*
 * BasicTeam.java
 * Clase de control para un equipo de robots en un juego de fútbol simulado.
 * Utiliza un enfoque de algoritmo genético para optimizar el comportamiento del equipo.
 */

import EDU.gatech.cc.is.util.Vec2; // Libreria para el uso de vectores
import EDU.gatech.cc.is.abstractrobot.*; // Libreria para el uso de robots

public class BasicTeamCromosomaFinal extends ControlSystemSS {
    public double[] disPos = {4.0, 8.0, 8.0, 1.0, 1.0};
    public double[] disKick = {3.0, 7.0, 3.0, 3.0, 2.0};
    public double[] disTeam = {7.0, 8.0, 7.0, 5.0, 4.0};
    public double[] disGoalie = {4.0, 6.0, 6.0, 3.0, 1.0};
    public double[] disDefensiveCover = {4.0, 6.0, 6.0, 6.0, 3.0};
    public double[] disOffensiveCover = {8.0, 5.0, 4.0, 7.0, 2.0};
    public double[] passThreshold = {7.0, 7.0, 7.0, 5.0, 5.0};
    public double[] kickThreshold = {6.0, 1.0, 4.0, 1.0, 6.0};
    public double[] teammateThreshold = {6.0, 6.0, 6.0, 3.0, 8.0};
    public double[] opponentCoverAngle = {5.0, 4.0, 1.0, 1.0, 1.0};

    public double sum_ballx = 0;
    public long con_ballx = 0;

    public void Configure() { 
        // Método para configuraciones iniciales del robot. Actualmente no utilizado.
    }
    
    public void setParam(Integer[] disPos, Integer[] disKick, Integer[] disTeam, Integer[] disGoalie) {
        for (int i = 0; i < 5; i++) {
            this.disPos[i] = disPos[i] / 10.0;
            this.disKick[i] = disKick[i] / 10.0;
            this.disTeam[i] = disTeam[i] / 10.0;
            this.disGoalie[i] = disGoalie[i] / 10.0;
            this.disDefensiveCover[i] = disDefensiveCover[i] / 10.0;
            this.disOffensiveCover[i] = disOffensiveCover[i] / 10.0;
            this.passThreshold[i] = passThreshold[i] / 10.0;
            this.kickThreshold[i] = kickThreshold[i] / 10.0;
            this.teammateThreshold[i] = teammateThreshold[i] / 10.0;
        }
    }

    public int TakeStep() {
        Vec2 result = new Vec2(0, 0);
        long curr_time = abstract_robot.getTime();

        Vec2 ball = abstract_robot.getBall(curr_time);
        Vec2 ourgoal = abstract_robot.getOurGoal(curr_time);
        Vec2 theirgoal = abstract_robot.getOpponentsGoal(curr_time);
        Vec2[] teammates = abstract_robot.getTeammates(curr_time);
        Vec2[] opponents = abstract_robot.getOpponents(curr_time);

        Vec2 closestteammate = new Vec2(99999, 0);
        for (int i = 0; i < teammates.length; i++) {
            if (teammates[i].r < closestteammate.r) {
                closestteammate = teammates[i];
            }
        }

        int mynum = abstract_robot.getPlayerNumber(curr_time);

        // Calcula la distancia promedio de la pelota al gol
        sum_ballx += Math.abs((ourgoal.x - ball.x) * (ourgoal.x - ball.x));
        con_ballx += 1;

        // Verificar si alguien está marcando a un oponente
        boolean isMarking = isMarkingOpponent(opponents);

        // Determinar la acción a tomar
        if (mynum == 0) {
            // Jugador 0 (portero)
            result = decideGoalkeeperAction(ball, isMarking);
        } else if (mynum == 1) {
            // Jugador 1 (defensor)
            result = decideDefenderAction(ball, isMarking);
        } else if (mynum == 2) {
            // Jugador 2 (mediocampista)
            result = decideMidfielderAction(ball, teammates, isMarking);
        } else if (mynum == 3) {
            // Jugador 3 (mediocampista)
            result = decideMidfielderAction(ball, teammates, isMarking);
        } else if (mynum == 4) {
            // Jugador 4 (delantero)
            result = decideForwardAction(ball, teammates, isMarking);
        }


        abstract_robot.setSteerHeading(curr_time, result.t);
        abstract_robot.setSpeed(curr_time, 1.0);

        if (abstract_robot.canKick(curr_time)) { 
            abstract_robot.kick(curr_time); 
        }

        return (CSSTAT_OK);
    }
   // Lógica para el portero
    private Vec2 decideGoalkeeperAction(Vec2 ball, boolean isMarking) {
        Vec2 result = new Vec2(0, 0);
        
        // Utilizar los parámetros definidos en la clase
        double distanceToBallThreshold = disPos[0]; // Ejemplo, usa el parámetro disPos[0]
        double kickThresholdValue = kickThreshold[0]; // Ejemplo, usa el parámetro kickThreshold[0]
        
        // Ahora puedes utilizar estos valores en tu lógica
        if (ball.r < distanceToBallThreshold) {
            // El balón está lo suficientemente cerca, bloquear tiros
            result.sett(ball.t);
            result.setr(1.0); // Ajusta la distancia al balón según el valor de disPos[0]
            
            if (abstract_robot.canKick(abstract_robot.getTime()) && ball.r < kickThresholdValue) {
                // Si se puede patear y el balón está cerca, patear al balón
                abstract_robot.kick(abstract_robot.getTime());
            }
        } else {
            // Moverse hacia una posición óptima o mantenerse en la portería
            // Ajusta tu lógica según los parámetros y las necesidades del juego
            // Sera su posicion inicial
            result.sett(0);
        }
        
        return result;
    }

    // Lógica para el defensor
    private Vec2 decideDefenderAction(Vec2 ball, boolean isMarking) {
        Vec2 result = new Vec2(0, 0);
        
        // Utilizar los parámetros definidos en la clase
        double distanceToBallThreshold = disPos[1]; // Ejemplo, usa el parámetro disPos[1]
        double teammateThresholdValue = teammateThreshold[1]; // Ejemplo, usa el parámetro teammateThreshold[1]
        
        // Ahora puedes utilizar estos valores en tu lógica
        if (isMarking) {
            // Si ya se está marcando a un oponente, mantener la marca
            result.sett(ball.t);
            result.setr(1.0); // Ajusta la distancia de marca
            
            if (abstract_robot.canKick(abstract_robot.getTime()) && ball.r < teammateThresholdValue) {
                // Si se puede patear y el balón está cerca de un compañero, pasar el balón
                abstract_robot.kick(abstract_robot.getTime());
            }
        } else {
            // Si no se está marcando a nadie, acercarse al balón o marcar a un oponente
            // Ajusta tu lógica según los parámetros y las necesidades del juego
            
            // Ejemplo: Utilizar distanceToBallThreshold en la lógica
            if (ball.r < distanceToBallThreshold) {
                // El balón está lo suficientemente cerca, acercarse al balón
                result.sett(ball.t);
                result.setr(0.5); // Ajusta la distancia al balón según el valor de distanceToBallThreshold
            } else {
                // Si no, marcar a un oponente o realizar otra acción
                // Ajusta tu lógica según los parámetros y las necesidades del juego
            }
        }
        
        return result;
    }

    // Lógica para el mediocampista
private Vec2 decideMidfielderAction(Vec2 ball, Vec2[] teammates, boolean isMarking) {
    Vec2 result = new Vec2(0, 0);
    
    // Utilizar los parámetros definidos en la clase
    double distanceToBallThreshold = disPos[2]; // Ejemplo, usa el parámetro disPos[2]
    double passThresholdValue = passThreshold[2]; // Ejemplo, usa el parámetro passThreshold[2]
    
    // Ahora puedes utilizar estos valores en tu lógica
    if (isMarking) {
        // Si ya se está marcando a un oponente, mantener la marca
        result.sett(ball.t);
        result.setr(1.0); // Ajusta la distancia de marca
    } else {
        // Si no se está marcando a nadie, buscar pases a compañeros
        // Ajusta tu lógica según los parámetros y las necesidades del juego
        
        // Ejemplo: Si un compañero está lo suficientemente cerca, pasar el balón
        for (Vec2 teammate : teammates) {
            if (teammate.r < passThresholdValue) {
                result.sett(teammate.t);
                result.setr(0.5); // Ajusta la distancia al compañero
                break; // Detenerse después de encontrar un compañero cercano
            }
        }
        
        if (result.r == 0) {
            // Si no se encontró un compañero cercano, moverse hacia el balón
            double distanceToBall = ball.r;
            if (distanceToBall < distanceToBallThreshold) {
                result.sett(ball.t);
                result.setr(0.5); // Ajusta la distancia al balón
            }
        }
    }
    
    return result;
}

   // Lógica para el delantero
private Vec2 decideForwardAction(Vec2 ball, Vec2[] teammates, boolean isMarking) {
    Vec2 result = new Vec2(0, 0);
    
    // Utilizar los parámetros definidos en la clase
    double distanceToBallThreshold = disPos[4]; // Ejemplo, usa el parámetro disPos[4]
    double kickThresholdValue = kickThreshold[4]; // Ejemplo, usa el parámetro kickThreshold[4]
    
    // Obtener la posición de tu propia portería
    Vec2 ourgoal = abstract_robot.getOurGoal(abstract_robot.getTime());
    
    // Ahora puedes utilizar estos valores en tu lógica
    if (isMarking) {
        // Si ya se está marcando a un oponente, mantener la marca
        result.sett(ball.t);
        result.setr(distanceToBallThreshold); // Ajusta la distancia de marca usando la variable
    } else {
        // Si no se está marcando a nadie, buscar oportunidades de gol
        // Ajusta tu lógica según los parámetros y las necesidades del juego
        
        // Ejemplo: Si el balón está en línea con el gol, disparar al gol
        double goalAngle = Math.atan2(ourgoal.y - ball.y, ourgoal.x - ball.x);
        if (Math.abs(goalAngle - ball.t) < opponentCoverAngle[4]) {
            result.sett(ball.t);
            result.setr(kickThresholdValue); // Ajusta la distancia al balón usando la variable
        } else {
            // Si no, moverse hacia una posición óptima
            result.sett(goalAngle);
            result.setr(distanceToBallThreshold); // Ajusta la distancia a la posición óptima usando la variable
        }
    }
    
    return result;
}
 
// Implementar función para verificar si alguien está marcando a un oponente
private boolean isMarkingOpponent(Vec2[] opponents) {
    boolean isMarking = false;
    
    // Lógica para determinar si alguien está marcando a un oponente
    // Puedes ajustar la lógica según los parámetros y las necesidades del juego
    
    // Ejemplo: Marcar a un oponente si está lo suficientemente cerca
    for (Vec2 opponent : opponents) {
        if (opponent.r < disDefensiveCover[2]) { // Usando el parámetro disDefensiveCover[2] como referencia
            isMarking = true;
            break; // Detenerse después de encontrar un oponente para marcar
        }
    }
    
    return isMarking;
}
}