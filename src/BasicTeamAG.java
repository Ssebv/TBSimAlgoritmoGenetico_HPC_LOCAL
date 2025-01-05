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
 import java.util.logging.Logger;
 
 public class BasicTeamAG extends ControlSystemSS {
 
     private static final Logger LOGGER = Logger.getLogger(BasicTeamAG.class.getName());
 
     // Número de jugadores controlados por este equipo
     private static final int NUM_PLAYERS = 5;
 
     // Posición “inaccesible” por defecto para indicar que no se halló una posición válida
     private static final Vec2 DEFAULT_POSITION = new Vec2(99999, 0);
 
     // Número total de parámetros esperados (50, según tu setParam)
     private static final int PARAM_COUNT = 50;
 
     // Índices en el array de parámetros (agrupados de 10 en 10 por cada jugador)
     private static final int IDX_DISPOS = 0;
     private static final int IDX_DISKICK = 1;
     private static final int IDX_PASSTHRESH = 6;
     private static final int IDX_KICKTHRESH = 7;
     private static final int IDX_TEAMMATE = 8;
     private static final int IDX_COVERANGLE = 9;
 
     // Multiplicadores/escalas para algunos parámetros
     private static final double DIV_FACTOR = 10.0;     // Para “disPos”, “disKick”, etc.
     private static final double ANGLE_FACTOR = Math.PI / 2.0; // Para “opponentCoverAngle”
 
     // Distancias de referencia
     private static final double BACK_SPOT_MULTIPLIER = 5.0;  // Usa 5 radios
     private static final double GOALIE_POS_FACTOR = 0.5;      // Posicionar portero a mitad de camino
 
     /**
      * Clase interna para almacenar los parámetros de cada jugador.
      */
     private static class PlayerParams {
         double disPos;             // Distancia para posicionarse
         double disKick;            // Distancia para “ir a patear”
         double passThreshold;      // Distancia/umbral para pasar
         double kickThreshold;      // Distancia/umbral para “preparar un tiro”
         double teammateThreshold;  // Distancia para no “chocar” a compañero
         double opponentCoverAngle; // Ángulo para cubrir a un oponente
     }
 
     // Cada instancia de este control posee parámetros para 5 jugadores
     private final PlayerParams[] playersParams = new PlayerParams[NUM_PLAYERS];
 
     public BasicTeamAG() {
         // Inicializa los parámetros de cada jugador
         for (int i = 0; i < NUM_PLAYERS; i++) {
             playersParams[i] = new PlayerParams();
         }
     }
 
     @Override
     public void Configure() {
         // Configuración inicial si es necesaria
         // (e.g. seteo de flags, inicialización de estructuras, etc.)
     }
 
     /**
      * Establece los parámetros para cada jugador a partir de un arreglo de Double.
      * Espera exactamente 50 parámetros (10 por cada uno de los 5 jugadores).
      *
      * @param params Arreglo de 50 parámetros double.
      */
     public void setParam(Double[] params) {
         if (params == null || params.length != PARAM_COUNT) {
             throw new IllegalArgumentException(
                     "Se esperan exactamente " + PARAM_COUNT + " parámetros."
             );
         }
 
         for (int i = 0; i < NUM_PLAYERS; i++) {
             PlayerParams p = playersParams[i];
             int baseIndex = i * 10;  // Cada jugador “consume” 10 parámetros seguidos
 
             // Se normalizan y validan los parámetros
             p.disPos = clampAndWarn(params[baseIndex + IDX_DISPOS]) / DIV_FACTOR;
             p.disKick = clampAndWarn(params[baseIndex + IDX_DISKICK]) / DIV_FACTOR;
             p.passThreshold = clampAndWarn(params[baseIndex + IDX_PASSTHRESH]) / DIV_FACTOR;
             p.kickThreshold = clampAndWarn(params[baseIndex + IDX_KICKTHRESH]) / DIV_FACTOR;
             p.teammateThreshold = clampAndWarn(params[baseIndex + IDX_TEAMMATE]) / DIV_FACTOR;
 
             // Ojo: para “opponentCoverAngle” se deja en [0,1] y luego se multiplica
             double rawAngle = clamp(params[baseIndex + IDX_COVERANGLE], 0.0, 1.0);
             p.opponentCoverAngle = rawAngle * ANGLE_FACTOR;
         }
     }
 
     /**
      * Valida que el parámetro no sea nulo, infinito o NaN.
      * De serlo, retorna 1.0 y registra un warning.
      */
     private double clampAndWarn(Double param) {
         if (param == null || param.isNaN() || param.isInfinite()) {
             LOGGER.warning("Parámetro inválido detectado. Usando valor predeterminado 1.0");
             return 1.0;
         }
         return param;
     }
 
     /**
      * Limita (clamp) un valor dentro de [min, max].
      */
     private double clamp(double value, double min, double max) {
         if (value < min) return min;
         if (value > max) return max;
         return value;
     }
 
     @Override
     public int TakeStep() {
         long curr_time = abstract_robot.getTime();
 
         // Información básica de la cancha
         Vec2 ball = abstract_robot.getBall(curr_time);
         Vec2 ourGoal = abstract_robot.getOurGoal(curr_time);
         Vec2 theirGoal = abstract_robot.getOpponentsGoal(curr_time);
 
         // Información de compañeros y rivales
         Vec2[] teammates = validateArray(abstract_robot.getTeammates(curr_time));
         Vec2[] opponents = validateArray(abstract_robot.getOpponents(curr_time));
 
         // Este robot es el “mynum” dentro del equipo
         int mynum = abstract_robot.getPlayerNumber(curr_time);
         PlayerParams p = playersParams[mynum];
 
         // Calcula la acción a tomar
         Vec2 result = calculateAction(
                 mynum,
                 ball,
                 theirGoal,
                 ourGoal,
                 teammates,
                 opponents,
                 p
         );
 
         // Se actualizan mandos de movimiento
         abstract_robot.setSteerHeading(curr_time, result.t);
         abstract_robot.setSpeed(curr_time, 1.0);
 
         // Intenta patear si está en rango
         if (abstract_robot.canKick(curr_time)) {
             abstract_robot.kick(curr_time);
         }
 
         return CSSTAT_OK;
     }
 
     /**
      * Verifica que el arreglo no sea nulo o vacío y, si lo es, retorna un arreglo vacío.
      */
     private Vec2[] validateArray(Vec2[] array) {
         if (array == null || array.length == 0) {
             return new Vec2[0];
         }
         return array;
     }
 
     /**
      * Decide qué hacer según el rol del jugador (portero vs. jugadores de campo)
      * y la situación actual de balón/oponentes.
      */
     private Vec2 calculateAction(
             int mynum,
             Vec2 ball,
             Vec2 theirGoal,
             Vec2 ourGoal,
             Vec2[] teammates,
             Vec2[] opponents,
             PlayerParams p
     ) {
         // Predicción muy simple de la posición futura del balón
         // (aquí se asume que “theirGoal” se usa como referencia del “ballVelocity”,
         //  pero podría mejorarse calculando la velocidad real del balón)
         Vec2 ballVelocity = new Vec2(ball.x - theirGoal.x, ball.y - theirGoal.y);
         Vec2 predictedBallPosition = predictBallPosition(ball, ballVelocity);
 
         // Posiciones de interés
         Vec2 kickSpot = calcKickSpot(predictedBallPosition, theirGoal);
         Vec2 backSpot = calcBackSpot(predictedBallPosition, theirGoal);
         Vec2 goaliePos = calcGoaliePos(ourGoal, predictedBallPosition);
 
         // Identifica al oponente más peligroso (más cercano a “nuestra portería”)
         Vec2 dangerousOpponent = detectDangerousOpponent(opponents, ourGoal);
 
         // Lógica diferenciada: 0 será nuestro portero
         if (mynum == 0) {
             return actionGoalie(p, ball, kickSpot, goaliePos, teammates);
         } else if (opponents.length > 0) {
             // Jugador de campo que busca cubrir a oponentes peligrosos
             return coverOpponent(dangerousOpponent, ball, p);
         } else {
             // Jugador genérico cuando no hay oponentes (?)
             return actionGenericPlayer(p, ball, kickSpot, backSpot, teammates, opponents, ourGoal);
         }
     }
 
     /**
      * Retorna el oponente más cercano a nuestra portería,
      * asumiendo que ése es el más “peligroso”.
      */
     private Vec2 detectDangerousOpponent(Vec2[] opponents, Vec2 ourGoal) {
         return Arrays.stream(opponents)
                 .min(Comparator.comparingDouble(opponent -> calculateDistance(opponent, ourGoal)))
                 .orElse(DEFAULT_POSITION);
     }
 
     /**
      * Lógica del portero.
      */
     private Vec2 actionGoalie(
             PlayerParams p,
             Vec2 ball,
             Vec2 kickSpot,
             Vec2 goaliePos,
             Vec2[] teammates
     ) {
         // Si la pelota está lejos, posicionarse en la zona de portero
         if (ball.r > p.disPos) {
             return goaliePos;
         }
         // Si la pelota está a media distancia, ir a “kickSpot” (si en rango),
         // o buscar pase
         else if (ball.r > p.disKick) {
             if (ball.r < p.kickThreshold) {
                 return kickSpot;
             } else {
                 return passToTeammate(teammates, p);
             }
         }
         // Si está muy cerca de la pelota (en rango de patear), ve a la pelota
         else {
             return ball;
         }
     }
 
     /**
      * Lógica genérica para jugadores de campo.
      */
     private Vec2 actionGenericPlayer(
             PlayerParams p,
             Vec2 ball,
             Vec2 kickSpot,
             Vec2 backSpot,
             Vec2[] teammates,
             Vec2[] opponents,
             Vec2 ourGoal
     ) {
         // Si la pelota está muy lejos, posicionarse en “backSpot”
         if (ball.r > p.disPos) {
             return backSpot;
         }
         // Si está a media distancia,
         // patea a puerta si está en rango o pasa a un compañero
         else if (ball.r > p.disKick) {
             if (ball.r < p.kickThreshold) {
                 return kickSpot;
             } else {
                 return passToTeammate(teammates, p);
             }
         }
         // Si ya está en rango de patear, ve directo a la pelota
         else {
             return ball;
         }
     }
 
     /**
      * Calcula el punto al que queremos dirigirnos para chutar al arco rival.
      */
     private Vec2 calcKickSpot(Vec2 ball, Vec2 theirGoal) {
         // Ajuste: p. ej. usa el radio de SocSmall para reubicar la pelota
         // (Esta parte puede ajustarse según la lógica que necesites)
         Vec2 ks = new Vec2(ball.x - theirGoal.x, ball.y - theirGoal.y);
         ks.setr(SocSmall.RADIUS);
         ks.add(ball);
         return ks;
     }
 
     /**
      * Calcula un punto de “retorno” (backSpot) para reubicarse detrás de la pelota.
      */
     private Vec2 calcBackSpot(Vec2 ball, Vec2 theirGoal) {
         Vec2 bs = new Vec2(ball.x - theirGoal.x, ball.y - theirGoal.y);
         bs.setr(SocSmall.RADIUS * BACK_SPOT_MULTIPLIER);
         bs.add(ball);
         return bs;
     }
 
     /**
      * Calcula la posición del portero (entre nuestra portería y la posición actual del balón).
      */
     private Vec2 calcGoaliePos(Vec2 ourGoal, Vec2 ball) {
         // Ubica al portero en la mitad de la línea “ourGoal -> balón”
         Vec2 gpos = new Vec2(ball.x - ourGoal.x, ball.y - ourGoal.y);
         gpos.setr(gpos.r * GOALIE_POS_FACTOR);
         gpos.add(ourGoal);
         return gpos;
     }
 
     /**
      * Predice la posición del balón sumándole una “velocidad” naive.
      */
     private Vec2 predictBallPosition(Vec2 ball, Vec2 ballVelocity) {
         Vec2 predicted = new Vec2(ball);
         predicted.add(ballVelocity);
         return predicted;
     }
 
     /**
      * El jugador se coloca para “cubrir” al oponente dirigiéndose en la dirección
      * entre balón y oponente, con un ángulo dado por “opponentCoverAngle”.
      */
     private Vec2 coverOpponent(Vec2 opponent, Vec2 ball, PlayerParams p) {
         // Por si no se encontró oponente
         if (opponent == DEFAULT_POSITION) {
             return ball; // fallback
         }
         Vec2 direction = new Vec2(opponent);
         direction.sub(ball);
         // Reduce su radio al “ángulo de cobertura”
         direction.setr(p.opponentCoverAngle);
         return direction;
     }
 
     /**
      * Elige un compañero al que pasarle la pelota, buscando el que esté más cerca
      * y por debajo del threshold de pase.
      */
     private Vec2 passToTeammate(Vec2[] teammates, PlayerParams p) {
         return Arrays.stream(teammates)
                 .filter(t -> t.r < p.passThreshold)
                 .min(Comparator.comparingDouble(t -> t.r))
                 .orElse(DEFAULT_POSITION);
     }
 
     /**
      * Calcula la distancia euclidiana entre dos posiciones.
      */
     private double calculateDistance(Vec2 v1, Vec2 v2) {
         double dx = (v1.x - v2.x);
         double dy = (v1.y - v2.y);
         return Math.sqrt(dx * dx + dy * dy);
     }
 }
 