

/*
 * BasicTeamAG.java
 *
 * Clase de control para un equipo de robots en un juego de fútbol simulado.
 * Utiliza un enfoque de algoritmo genético para optimizar el comportamiento del equipo.
 */

 import EDU.gatech.cc.is.util.Vec2;
 import EDU.gatech.cc.is.abstractrobot.*;
 
 import java.util.Arrays;
 import java.util.Comparator;
 
 public class BasicTeamAG extends ControlSystemSS {
 
     private static final int NUM_PLAYERS = 5;
 
     /**
      * Clase interna para almacenar los parámetros de cada jugador.
      */
     private static class PlayerParams {
         double disPos;
         double disKick;
         double disTeam;
         double disGoalie;
         double disDefensiveCover;
         double disOffensiveCover;
         double passThreshold;
         double kickThreshold;
         double teammateThreshold;
         double opponentCoverAngle;
     }
 
     private final PlayerParams[] playersParams = new PlayerParams[NUM_PLAYERS];
     private double sum_ballx = 0;
     private long con_ballx = 0;
 
     public BasicTeamAG() {
         for (int i = 0; i < NUM_PLAYERS; i++) {
             playersParams[i] = new PlayerParams();
         }
     }
 
     @Override
     public void Configure() {
         // Configuración inicial si es necesaria
     }
 
     /**
      * Establece los parámetros para los jugadores a partir de un arreglo de Double.
      *
      * @param params Arreglo de 50 parámetros double.
      */
     public void setParam(Double[] params) {
         if (params.length != 50) {
             throw new IllegalArgumentException("Se esperan exactamente 50 parámetros.");
         }
 
         for (int i = 0; i < NUM_PLAYERS; i++) {
             PlayerParams p = playersParams[i];
             int baseIndex = i * 10;
 
             p.disPos = validateParam(params[baseIndex]) / 10.0;
             p.disKick = validateParam(params[baseIndex + 1]) / 10.0;
             p.disTeam = validateParam(params[baseIndex + 2]) / 10.0;
             p.disGoalie = validateParam(params[baseIndex + 3]) / 10.0;
             p.disDefensiveCover = validateParam(params[baseIndex + 4]) / 10.0;
             p.disOffensiveCover = validateParam(params[baseIndex + 5]) / 10.0;
             p.passThreshold = validateParam(params[baseIndex + 6]) / 10.0;
             p.kickThreshold = validateParam(params[baseIndex + 7]) / 10.0;
             p.teammateThreshold = validateParam(params[baseIndex + 8]) / 10.0;
             p.opponentCoverAngle = Math.min(Math.max(params[baseIndex + 9], 0.0), 1.0) * (Math.PI / 2.0);
         }
     }
 
     private double validateParam(Double param) {
         if (param == null || param.isNaN() || param.isInfinite()) {
             throw new IllegalArgumentException("Parámetro inválido: " + param);
         }
         return param;
     }
 
     @Override
     public int TakeStep() {
         long curr_time = abstract_robot.getTime();
 
         Vec2 ball = abstract_robot.getBall(curr_time);
         Vec2 ourGoal = abstract_robot.getOurGoal(curr_time);
         Vec2 theirGoal = abstract_robot.getOpponentsGoal(curr_time);
         Vec2[] teammates = abstract_robot.getTeammates(curr_time);
         Vec2[] opponents = abstract_robot.getOpponents(curr_time);
 
         actualizarSumBallx(ball, ourGoal);
 
         int mynum = abstract_robot.getPlayerNumber(curr_time);
         PlayerParams p = playersParams[mynum];
 
         Vec2 result = calculateAction(mynum, ball, theirGoal, ourGoal, teammates, opponents, p);
 
         // Configuración de la dirección y velocidad del robot
         abstract_robot.setSteerHeading(curr_time, result.t);
         abstract_robot.setSpeed(curr_time, 1.0);
 
         if (abstract_robot.canKick(curr_time)) {
             abstract_robot.kick(curr_time);
         }
 
         return (CSSTAT_OK);
     }
 
     private void actualizarSumBallx(Vec2 ball, Vec2 ourGoal) {
         sum_ballx += Math.pow(ourGoal.x - ball.x, 2);
         con_ballx += 1;
     }
 
     private Vec2 calculateAction(int mynum, Vec2 ball, Vec2 theirGoal, Vec2 ourGoal, Vec2[] teammates, Vec2[] opponents, PlayerParams p) {
         Vec2 kickSpot = calcKickSpot(ball, theirGoal);
         Vec2 backSpot = calcBackSpot(ball, theirGoal);
         Vec2 goaliePos = calcGoaliePos(ourGoal, ball);
         Vec2 closestTeammate = findClosest(teammates);
 
         if (mynum == 0) {
             return actionGoalie(p, ball, kickSpot, goaliePos, teammates, opponents);
         } else {
             return actionGenericPlayer(p, ball, kickSpot, backSpot, teammates, opponents, closestTeammate, ourGoal);
         }
     }
 
     private Vec2 actionGoalie(PlayerParams p, Vec2 ball, Vec2 kickSpot, Vec2 goaliePos, Vec2[] teammates, Vec2[] opponents) {
         if (ball.r > p.disPos) {
             return goaliePos;
         } else if (ball.r > p.disKick) {
             return (ball.r < p.kickThreshold) ? kickSpot : passToTeammate(teammates, p);
         } else {
             return ball;
         }
     }
 
     private Vec2 actionGenericPlayer(PlayerParams p, Vec2 ball, Vec2 kickSpot, Vec2 backSpot, Vec2[] teammates, Vec2[] opponents, Vec2 closestTeammate, Vec2 ourGoal) {
         if (ball.r > p.disPos) {
             return backSpot;
         } else if (ball.r > p.disKick) {
             return (ball.r < p.kickThreshold) ? kickSpot : passToTeammate(teammates, p);
         } else if (closestTeammate.r < p.teammateThreshold) {
             return calcBackSpot(ball, ourGoal);
         } else {
             return ball;
         }
     }
 
     private Vec2 passToTeammate(Vec2[] teammates, PlayerParams p) {
         return Arrays.stream(teammates)
                 .filter(t -> t.r < p.passThreshold)
                 .min(Comparator.comparingDouble(t -> t.r))
                 .orElse(new Vec2(0, 0));
     }
 
     private Vec2 calcKickSpot(Vec2 ball, Vec2 theirGoal) {
         Vec2 ks = new Vec2(ball.x - theirGoal.x, ball.y - theirGoal.y);
         ks.setr(SocSmall.RADIUS);
         ks.add(ball);
         return ks;
     }
 
     private Vec2 calcBackSpot(Vec2 ball, Vec2 theirGoal) {
         Vec2 bs = new Vec2(ball.x - theirGoal.x, ball.y - theirGoal.y);
         bs.setr(SocSmall.RADIUS * 5);
         bs.add(ball);
         return bs;
     }
 
     private Vec2 calcGoaliePos(Vec2 ourGoal, Vec2 ball) {
         Vec2 gpos = new Vec2(ourGoal.x, ball.y);
         gpos.setr(gpos.r * 0.5);
         return gpos;
     }
 
     private Vec2 findClosest(Vec2[] teammates) {
         return Arrays.stream(teammates)
                 .min(Comparator.comparingDouble(t -> t.r))
                 .orElse(new Vec2(99999, 0));
     }
 }
 