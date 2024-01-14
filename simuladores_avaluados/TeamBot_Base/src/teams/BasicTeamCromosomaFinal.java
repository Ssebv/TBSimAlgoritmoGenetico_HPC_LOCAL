/*
 * BasicTeam.java
 * Clase de control para un equipo de robots en un juego de fútbol simulado.
 * Utiliza un enfoque de algoritmo genético para optimizar el comportamiento del equipo.
 */

import EDU.gatech.cc.is.util.Vec2; // Libreria para el uso de vectores
import EDU.gatech.cc.is.abstractrobot.*; // Libreria para el uso de robots

public class BasicTeamCromosomaFinal extends ControlSystemSS {
    public double[] disPos = {4.0, 8.0, 8.0, 1.0, 1.0}; // Distancia para posicionamiento
    public double[] disKick = {3.0, 7.0, 3.0, 3.0, 2.0}; // Distancia para patear
    public double[] disTeam = {7.0, 8.0, 7.0, 5.0, 4.0}; // Distancia para buscar compañeros
    public double[] disGoalie = {4.0, 6.0, 6.0, 3.0, 1.0}; // Distancia para buscar portero
    public double[] disDefensiveCover = {4.0, 6.0, 6.0, 6.0, 3.0}; // Distancia para cobertura defensiva
    public double[] disOffensiveCover = {8.0, 5.0, 4.0, 7.0, 2.0}; // Distancia para cobertura ofensiva
    public double[] passThreshold = {7.0, 7.0, 7.0, 5.0, 5.0}; // Distancia para pase
    public double[] kickThreshold = {6.0, 1.0, 4.0, 1.0, 6.0}; // Distancia para patear
    public double[] teammateThreshold = {6.0, 6.0, 6.0, 3.0, 8.0}; // Distancia para buscar compañeros
    public double[] opponentCoverAngle = {5.0, 4.0, 1.0, 1.0, 1.0}; // Ángulo para marcar a un oponente

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
            result = decideDefenderAction(ball, isMarking, teammates);
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
    private Vec2 decideGoalkeeperAction(Vec2 ball, boolean isMarking) {
        Vec2 result = new Vec2(0, 0);
        double distanceToBallThreshold = disPos[0];
        double kickThresholdValue = kickThreshold[0];
    
        // Obtener la posición actual del arquero en el campo
        Vec2 goaliePosition = abstract_robot.getPosition(abstract_robot.getTime());
    
        // Calcular la distancia entre el arquero y el balón
        double distanceToBall = goaliePosition.r - ball.r;
    
        // Verificar si un oponente se acerca
        boolean opponentNearby = getNearestOpponent(goaliePosition) != null;
    
        // Verificar si el arquero debe mantener su posición inicial
        if (shouldStayInInitialPosition(goaliePosition, ball) && !opponentNearby) {
            // Mantener la posición inicial del arquero
            result.sett(abstract_robot.getOurGoal(abstract_robot.getTime()).t);
        } else {
            // Lógica ofensiva para el arquero cuando no debe mantener su posición inicial
            // Implementa aquí la lógica ofensiva según tus necesidades
    
            if (opponentNearby) {
                // Un oponente está cerca, puedes implementar una lógica para enfrentar al oponente aquí
                // Por ejemplo, moverse en dirección al oponente o bloquear el disparo.
                Vec2 opponentPosition = getNearestOpponentPosition(goaliePosition);
                result = new Vec2(opponentPosition.x - goaliePosition.x, opponentPosition.y - goaliePosition.y);
                result.setr(1.0); // Ajusta la velocidad de disparo o movimiento
            } else if (distanceToBall < distanceToBallThreshold && abstract_robot.canKick(abstract_robot.getTime())) {
                // El balón está lo suficientemente cerca para patear, patea en dirección al objetivo rival
                Vec2 opponentGoal = abstract_robot.getOpponentsGoal(abstract_robot.getTime());
                result = new Vec2(opponentGoal.x - goaliePosition.x, opponentGoal.y - goaliePosition.y);
                result.setr(1.0); // Ajusta la velocidad de disparo
                abstract_robot.kick(abstract_robot.getTime());
            } else {
                // Mantener posición si no hay oponentes cerca ni el balón está lo suficientemente cerca
                result.sett(abstract_robot.getOurGoal(abstract_robot.getTime()).t);
            }
        }
    
        // Resto de la lógica para el arquero
        // ...
    
        return result;
    }

    // Lógica para el defensor
    private Vec2 decideDefenderAction(Vec2 ball, boolean isMarking, Vec2[] teammates) {
        Vec2 result = new Vec2(0, 0);
        double distanceToBallThreshold = disPos[1];
        double defensiveCoverThreshold = disDefensiveCover[1];
    
        if (isMarking) {
            // Si se está marcando a un oponente, mantener la marca
            result.sett(ball.t);
            result.setr(1.0); 
        } else if (ball.r < distanceToBallThreshold) {
            // Si el balón está cerca, posicionarse para la cobertura defensiva
            result = positionForDefensiveCover(ball, teammates); // Posicionarse para la cobertura defensiva
        } else if (ball.r < defensiveCoverThreshold) {
            result.sett(ball.t); // Enfrentar el balón y prepararse para interceptar o despejar
        } else {
            result.sett(abstract_robot.getOurGoal(abstract_robot.getTime()).t); // Mantener posición defensiva cerca de la portería
        }

        double disTeamThreshold = disTeam[1];
        double opponentCoverAngleThreshold = opponentCoverAngle[1];

        if (ball.r < disTeamThreshold) {
            // Realiza una acción cuando la distancia al balón es menor que disTeamThreshold
            // Escribe codigo
            // ...

        } else if (ball.r < opponentCoverAngleThreshold) {
            // Realiza otra acción cuando la distancia al balón está entre disTeamThreshold y opponentCoverAngleThreshold
            // ...
        }
    
        return result;
    }

    

   // Lógica para el mediocampista
   private Vec2 decideMidfielderAction(Vec2 ball, Vec2[] teammates, boolean isMarking) {
    Vec2 result = new Vec2(0, 0);
    double distanceToBallThreshold = disPos[2];
    double passThresholdValue = passThreshold[2];

    if (ball.r < distanceToBallThreshold) {
        // Decidir entre avanzar con el balón o buscar pases
        Vec2 bestPassOption = findBestPassOption(teammates, ball);
        if (bestPassOption != null) {
            result.sett(bestPassOption.t); // Preparar pase
        } else {
            result.sett(ball.t); // Avanzar hacia el balón
        }
    } else {
        result = positionForMidfieldSupport(ball, teammates); // Posicionamiento táctico
    }

    double disOffensiveCoverThreshold = disOffensiveCover[2];
    double teammateThresholdValue = teammateThreshold[2];

    if (ball.r < disOffensiveCoverThreshold) {
        // Realiza una acción cuando la distancia al balón es menor que disOffensiveCoverThreshold
        // Escribe codigo
        // ...

    } else if (ball.r < teammateThresholdValue) {
        // Realiza otra acción cuando la distancia al balón está entre disOffensiveCoverThreshold y teammateThresholdValue
        // ...
    }

    return result;
}


    // Lógica para el delantero
    private Vec2 decideForwardAction(Vec2 ball, Vec2[] teammates, boolean isMarking) {
        Vec2 result = new Vec2(0, 0);

        double distanceToBallThreshold = disPos[4];
        double kickThresholdValue = kickThreshold[4];

        if (isMarking) {
            // Si se está marcando a un oponente, mantener la marca
            result.sett(ball.t);
            result.setr(distanceToBallThreshold);
        } else {
            // Buscar oportunidades de gol o avanzar con el balón
            if (ball.r < distanceToBallThreshold) {
                // Si el balón está cerca, decidir entre disparar o avanzar
                Vec2 goalDirection = calculateGoalDirection(abstract_robot.getOpponentsGoal(abstract_robot.getTime()), ball);
                result.sett(goalDirection.t);
                if (ball.r < kickThresholdValue) {
                    // Si está en posición para disparar
                    abstract_robot.kick(abstract_robot.getTime());
                }
                result.setr(1.0);
            } else {
                // Posicionarse estratégicamente para recibir el balón
                result = positionForForwardAttack(ball, teammates);
            }
        }

        double disKickThreshold = disKick[4];
        double opponentCoverAngleThreshold = opponentCoverAngle[4];

        if (ball.r < disKickThreshold) {
            // Realiza una acción cuando la distancia al balón es menor que disKickThreshold
            // Escribe codigo
            // ...

        } else if (ball.r < opponentCoverAngleThreshold) {
            // Realiza otra acción cuando la distancia al balón está entre disKickThreshold y opponentCoverAngleThreshold
            // ...
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
// Método para encontrar la mejor opción de pase para el mediocampista
private Vec2 findBestPassOption(Vec2[] teammates, Vec2 ball) {
    Vec2 bestOption = null;
    double minDistance = Double.MAX_VALUE;

    for (Vec2 teammate : teammates) {
        double distance = teammate.r;
        if (distance < minDistance && distance < passThreshold[2]) {
            bestOption = teammate;
            minDistance = distance;
        }
    }

    return bestOption;
}

// Método para el posicionamiento táctico de apoyo del mediocampista
private Vec2 positionForMidfieldSupport(Vec2 ball, Vec2[] teammates) {
    // Posicionamiento basado en la posición del balón y los compañeros
    Vec2 optimalPosition = new Vec2();
    
    // Lógica para encontrar una posición óptima en el campo
    // Puede ser una combinación de mantener una formación y estar cerca del balón
    // ...

    return optimalPosition;
}
// Método para calcular la dirección hacia el gol del equipo contrario
private Vec2 calculateGoalDirection(Vec2 theirgoal, Vec2 ball) {
    Vec2 goalDirection = new Vec2(theirgoal.x - ball.x, theirgoal.y - ball.y);
    double magnitude = Math.sqrt(goalDirection.x * goalDirection.x + goalDirection.y * goalDirection.y);
    goalDirection.x /= magnitude;
    goalDirection.y /= magnitude;
    return goalDirection;
}


// Método para el posicionamiento estratégico del delantero
private Vec2 positionForForwardAttack(Vec2 ball, Vec2[] teammates) {
    // Posicionamiento basado en la posición del balón y los compañeros
    Vec2 optimalPosition = new Vec2();
    
    // Lógica para encontrar la mejor posición para recibir el balón y crear oportunidades de gol
    // ...

    return optimalPosition;
}

// Método para el posicionamiento estratégico del defensor
private Vec2 positionForDefensiveCover(Vec2 ball, Vec2[] teammates) {
    // Posicionamiento basado en la posición del balón y los compañeros
    Vec2 optimalPosition = new Vec2();
    
    // Lógica para encontrar la mejor posición para marcar a un oponente
    // ...

    return optimalPosition;
}

// Función para determinar si el arquero debe mantener su posición inicial
private boolean shouldStayInInitialPosition(Vec2 goaliePosition, Vec2 ball) {
    // Define una distancia mínima para que el arquero se mueva
    double minDistanceToMove = 5.0; // Ajusta esta distancia según tu preferencia

    // Define una distancia máxima para que el arquero se adelante
    double maxDistanceToAdvance = 2.0; // Ajusta esta distancia según tu preferencia

    // Calcula la distancia entre el arquero y el balón
    double distanceToBall = goaliePosition.r - ball.r;

    // Calcula la distancia en el eje x entre el arquero y el balón
    double distanceXToBall = Math.abs(goaliePosition.x - ball.x);

    // Verifica si el balón está lo suficientemente cerca en la diagonal para avanzar
    if (distanceToBall > minDistanceToMove && distanceXToBall < maxDistanceToAdvance) {
        return false; // Permitir que el arquero avance y tome acciones ofensivas
    } else {
        return true; // Mantener posición inicial
    }
}

// Función para obtener la posición del oponente más cercano al arquero
private Vec2 getNearestOpponent(Vec2 goaliePosition) {
    Vec2[] opponents = abstract_robot.getOpponents(abstract_robot.getTime());
    Vec2 nearestOpponent = null;
    double nearestDistance = Double.MAX_VALUE;

    for (Vec2 opponent : opponents) {
        double distance = goaliePosition.r - opponent.r;
        if (distance < nearestDistance) {
            nearestDistance = distance;
            nearestOpponent = opponent;
        }
    }

    return nearestOpponent;
}

// Función para obtener la posición del oponente más cercano al arquero
private Vec2 getNearestOpponentPosition(Vec2 goaliePosition) {
    Vec2[] opponents = abstract_robot.getOpponents(abstract_robot.getTime());
    Vec2 nearestOpponent = null;
    double nearestDistance = Double.MAX_VALUE;

    for (Vec2 opponent : opponents) {
        double distance = goaliePosition.r - opponent.r;
        if (distance < nearestDistance) {
            nearestDistance = distance;
            nearestOpponent = opponent;
        }
    }

    return nearestOpponent;
}
}