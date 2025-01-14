import EDU.gatech.cc.is.util.Vec2;
import EDU.gatech.cc.is.abstractrobot.*;

import java.util.Arrays;

/**
 * BasicTeamAG.java
 * Optimized robot team with parameters for coordination, adaptability, and alignment,
 * con énfasis mejorado en la ofensiva (hacer goles).
 */
public class BasicTeamAG extends ControlSystemSS {

    private static final int NUM_PLAYERS = 5;
    private static final double FIELD_LENGTH = 2.74;
    private static final double GOAL_WIDTH = 0.5;
    private static final double RANGE = 0.3;
    private static final double IDEAL_DISTANCE = 0.4;

    private final PlayerParams[] playerParams = new PlayerParams[NUM_PLAYERS];
    private Vec2 offensivePos1, offensivePos2;

    private double teamCoordinationWeight;
    private double proximityWeight;
    private double goalAlignmentWeight;
    private double gameStateAdaptation;
    private double aggressivenessWeight;
    private double defensiveLineWeight;

    // Extra: factor para disparar directo si cerca del arco
    private static final double CLOSE_TO_GOAL_DISTANCE = 1.0;  // Ajusta a tu preferencia

    private static class PlayerParams {
        double forceLimit;
        double defenseWeight;
        double attackWeight;
        double passThreshold;
        double opponentAvoidance;
        double speed;
        double shootingAccuracy;
        double passingAccuracy;
        double positioning;
    }

    public BasicTeamAG() {
        for (int i = 0; i < NUM_PLAYERS; i++) {
            playerParams[i] = new PlayerParams();
        }
    }

    @Override
    public void Configure() {
        Vec2 goal = abstract_robot.getOpponentsGoal(0L);
        // Calcula posiciones ofensivas (arriba y abajo) dependiendo de si la portería rival está a la derecha
        offensivePos1 = calculateOffensivePosition(goal.x > 0.0, true);
        offensivePos2 = calculateOffensivePosition(goal.x > 0.0, false);
    }

    private Vec2 calculateOffensivePosition(boolean isRightSide, boolean isUpper) {
        double x = isRightSide ? 6.0 * FIELD_LENGTH / 10.0 : -6.0 * FIELD_LENGTH / 10.0;
        double y = isUpper ? GOAL_WIDTH / 2.0 : -GOAL_WIDTH / 2.0;
        return new Vec2(x, y);
    }

    /**
     * Configura los 60 parámetros (10 para cada uno de los 5 jugadores).
     */
    public void setParam(Double[] params) {
        if (params == null || params.length != 60) {
            throw new IllegalArgumentException("Exactly 60 parameters are required.");
        }

        for (int i = 0; i < NUM_PLAYERS; i++) {
            PlayerParams p = playerParams[i];
            int baseIndex = i * 10;

            p.forceLimit = params[baseIndex];
            p.defenseWeight = params[baseIndex + 1];
            p.attackWeight = params[baseIndex + 2];
            p.passThreshold = params[baseIndex + 3];
            p.opponentAvoidance = params[baseIndex + 4];
            p.speed = params[baseIndex + 5];
            p.shootingAccuracy = params[baseIndex + 6];
            p.passingAccuracy = params[baseIndex + 7];
            p.positioning = params[baseIndex + 8];
        }

        this.teamCoordinationWeight = params[50];
        this.proximityWeight = params[51];
        this.goalAlignmentWeight = params[52];
        this.gameStateAdaptation = params[53];
        this.aggressivenessWeight = params[54];
        this.defensiveLineWeight = params[55];
    }

    @Override
    public int TakeStep() {
        long time = abstract_robot.getTime();
        Vec2 ball = abstract_robot.getBall(time);
        int mynum = abstract_robot.getPlayerNumber(time);
        PlayerParams p = playerParams[mynum];

        adaptWeightsBasedOnGameState(time);

        // Calcular fuerzas globales
        Vec2 force = calculateForces(time, p, ball);

        // Manejo de movimiento
        handlePlayerMovement(time, force, ball, p);

        // Lógica ofensiva o defensiva
        if (isOffensiveState(ball, time)) {
            handleOffensivePlay(time, ball, p);
        } else {
            handleDefensivePlay(time, ball, p);
        }

        return CSSTAT_OK;
    }

    // Adapta pesos según si estamos en ofensiva o defensiva
    private void adaptWeightsBasedOnGameState(long time) {
        if (isOffensiveState(abstract_robot.getBall(time), time)) {
            adaptTeamWeightsForOffense();
        } else {
            adaptTeamWeightsForDefense();
        }
    }

    private void adaptTeamWeightsForOffense() {
        teamCoordinationWeight *= gameStateAdaptation;
        proximityWeight /= gameStateAdaptation;
        goalAlignmentWeight *= 1.5;
        aggressivenessWeight *= 1.2;
    }

    private void adaptTeamWeightsForDefense() {
        teamCoordinationWeight /= gameStateAdaptation;
        proximityWeight *= gameStateAdaptation;
        defensiveLineWeight *= 1.1;
    }

    /**
     * Control de movimiento base (evita oponentes, decide si moverse o disparar).
     */
    private void handlePlayerMovement(long time, Vec2 force, Vec2 ball, PlayerParams p) {
        Vec2 closestOpponent = findClosestOpponent(time);
        if (closestOpponent != null && closestOpponent.r < p.opponentAvoidance) {
            // Evasión de oponentes
            Vec2 avoidanceForce = new Vec2(-closestOpponent.x, -closestOpponent.y);
            avoidanceForce.setr(p.opponentAvoidance);
            abstract_robot.setSteerHeading(time, avoidanceForce.t);
            abstract_robot.setSpeed(time, p.speed / 2);
            return;
        }

        // Si la fuerza total es menor al límite, paramos para patear
        if (force.r < p.forceLimit) {
            abstract_robot.setSteerHeading(time, ball.t);
            abstract_robot.setSpeed(time, 0.0);
        } else {
            // Movimiento libre
            abstract_robot.setSteerHeading(time, getFreeDirection(force, RANGE, time));
            abstract_robot.setSpeed(time, p.speed);
        }
    }

    /**
     * Calcula la fuerza resultante, sumando defensiva, ofensiva, proximidad, etc.
     */
    private Vec2 calculateForces(long time, PlayerParams p, Vec2 ball) {
        Vec2 pos = abstract_robot.getPosition(time);
        Vec2 force = new Vec2(0, 0);

        addDefensiveForce(pos, p, force);
        addAttackForce(pos, p, force);
        addProximityForce(ball, force);
        addAlignmentForce(time, force);
        addCoordinationForce(pos, time, force);

        return force;
    }

    private void addDefensiveForce(Vec2 pos, PlayerParams p, Vec2 force) {
        Vec2 defenseForce = new Vec2(-p.defenseWeight * pos.x, -p.defenseWeight * pos.y);
        force.add(defenseForce);
    }

    private void addAttackForce(Vec2 pos, PlayerParams p, Vec2 force) {
        force.add(calculateAttackForce(pos, p));
    }

    private void addProximityForce(Vec2 ball, Vec2 force) {
        if (ball != null && ball.r != 0) {
            Vec2 proximityForce = new Vec2(ball);
            proximityForce.setr(proximityWeight / (ball.r * ball.r));
            force.add(proximityForce);
        }
    }

    private void addAlignmentForce(long time, Vec2 force) {
        Vec2 goal = abstract_robot.getOpponentsGoal(time);
        if (goal != null) {
            Vec2 alignmentForce = new Vec2(goal);
            alignmentForce.setr(goalAlignmentWeight);
            force.add(alignmentForce);
        }
    }

    private void addCoordinationForce(Vec2 pos, long time, Vec2 force) {
        Vec2 teamCoordinationForce = calculateTeamCoordinationForce(pos, time);
        force.add(teamCoordinationForce);
    }

    private Vec2 calculateAttackForce(Vec2 pos, PlayerParams p) {
        Vec2 force = new Vec2(0, 0);
        calculateTargetForce(pos, p, offensivePos1, force);
        calculateTargetForce(pos, p, offensivePos2, force);
        return force;
    }

    private void calculateTargetForce(Vec2 pos, PlayerParams p, Vec2 target, Vec2 force) {
        Vec2 targetForce = new Vec2(target);
        targetForce.sub(pos);
        if (targetForce.r != 0) {
            // Múltiple p.attackWeight, p.shootingAccuracy, aggressivenessWeight
            targetForce.setr(p.attackWeight * p.shootingAccuracy * aggressivenessWeight / (targetForce.r * targetForce.r));
            force.add(targetForce);
        }
    }

    /**
     * Determina si estamos en estado ofensivo comparando la distancia ball->ourGoal vs ball->oppGoal.
     */
    private boolean isOffensiveState(Vec2 ball, long time) {
        Vec2 ownGoal = abstract_robot.getOurGoal(time);
        Vec2 goal = abstract_robot.getOpponentsGoal(time);
        if (ball == null || ownGoal == null || goal == null) {
            return false;
        }
        return calculateDistance(ball, ownGoal) > calculateDistance(ball, goal);
    }

    private double calculateDistance(Vec2 from, Vec2 to) {
        Vec2 distance = new Vec2(from);
        distance.sub(to);
        return distance.r;
    }

    private Vec2 calculateTeamCoordinationForce(Vec2 pos, long time) {
        Vec2[] teammates = abstract_robot.getTeammates(time);
        Vec2 coordinationForce = new Vec2(0, 0);

        for (Vec2 teammate : teammates) {
            if (teammate != null) {
                Vec2 toTeammate = new Vec2(teammate);
                toTeammate.sub(pos);

                double weight = toTeammate.r < IDEAL_DISTANCE ? -teamCoordinationWeight : teamCoordinationWeight;
                toTeammate.setr(weight / toTeammate.r);

                coordinationForce.add(toTeammate);
            }
        }
        return coordinationForce;
    }

    /**
     * Lógica ofensiva mejorada. 
     * - Si estás cerca del arco y bien alineado, dispara. 
     * - Si no, pasa a un compañero mejor ubicado.
     */
    private void handleOffensivePlay(long time, Vec2 ball, PlayerParams p) {
        Vec2 goal = abstract_robot.getOpponentsGoal(time);
        Vec2 predictedBallPosition = predictBallPosition(ball, time);

        // 1. Disparo directo si estamos cerca y alineados
        double distToGoal = calculateDistance(abstract_robot.getPosition(time), goal);
        double alignScore = calculateAlignmentScore(abstract_robot.getPosition(time), goal);
        if (distToGoal < CLOSE_TO_GOAL_DISTANCE && alignScore > 0.7) {
            abstract_robot.setSteerHeading(time, goal.t);
            abstract_robot.setSpeed(time, p.speed * p.shootingAccuracy * aggressivenessWeight);
            return;
        }

        // 2. Disparo si buena alineación y la pelota está en rango
        if (goal != null && alignScore > 0.6 && ball.r < FIELD_LENGTH / 3 * aggressivenessWeight) {
            abstract_robot.setSteerHeading(time, goal.t);
            abstract_robot.setSpeed(time, p.speed * p.shootingAccuracy * aggressivenessWeight);
            return;
        }

        // 3. Pases
        // Si la pelota está suficientemente cerca para un pase
        if (ball.r < p.passThreshold * aggressivenessWeight) {
            Vec2 bestPassTarget = findBestPassTargetOffensive(time, predictedBallPosition, goal);
            if (bestPassTarget != null) {
                abstract_robot.setSteerHeading(time, bestPassTarget.t);
                abstract_robot.setSpeed(time, p.speed * p.passingAccuracy);
                return;
            }
        }

        // 4. Si nada anterior se cumple, moverse hacia la pelota
        if (predictedBallPosition != null) {
            abstract_robot.setSteerHeading(time, predictedBallPosition.t);
            abstract_robot.setSpeed(time, p.speed * p.positioning * aggressivenessWeight);
        }
    }

    private Vec2 findBestPassTargetOffensive(long time, Vec2 predictedBallPosition, Vec2 goal) {
        Vec2[] teammates = abstract_robot.getTeammates(time);
        return Arrays.stream(teammates)
                .filter(teammate -> teammate != null)
                .min((t1, t2) -> {
                    double align1 = calculateAlignmentScore(t1, goal);
                    double align2 = calculateAlignmentScore(t2, goal);

                    Vec2 t1Copy = new Vec2(t1);
                    t1Copy.sub(predictedBallPosition);
                    double dist1 = t1Copy.r;

                    Vec2 t2Copy = new Vec2(t2);
                    t2Copy.sub(predictedBallPosition);
                    double dist2 = t2Copy.r;

                    // Comparar (alineación / distancia)
                    double score1 = align1 / (dist1 + 0.001);
                    double score2 = align2 / (dist2 + 0.001);

                    return Double.compare(score2, score1); 
                })
                .orElse(null);
    }

    /**
     * Devuelve un "score" de alineación (cos del ángulo) con la portería.
     * 1.0 es perfectamente alineado, -1.0 es opuesto.
     */
    private double calculateAlignmentScore(Vec2 position, Vec2 goal) {
        Vec2 directionToGoal = new Vec2(goal);
        directionToGoal.sub(position);
        return Math.cos(directionToGoal.t);
    }

    /**
     * Lógica defensiva; no la cambiamos mucho, solo quedamos igual.
     */
    private void handleDefensivePlay(long time, Vec2 ball, PlayerParams p) {
        Vec2 ownGoal = abstract_robot.getOurGoal(time);
        Vec2 predictedBallPosition = predictBallPosition(ball, time);

        if (ownGoal != null && predictedBallPosition.r < RANGE * aggressivenessWeight) {
            // replegarse a portería
            abstract_robot.setSteerHeading(time, ownGoal.t + Math.PI);
            abstract_robot.setSpeed(time, p.speed * aggressivenessWeight);
            return;
        }

        if (ownGoal != null && ball.r > FIELD_LENGTH / 4) {
            // formamos línea defensiva
            Vec2 defensivePosition = new Vec2(ownGoal.x + defensiveLineWeight * aggressivenessWeight, ownGoal.y);
            abstract_robot.setSteerHeading(time, defensivePosition.t);
            abstract_robot.setSpeed(time, p.speed / 2);
            return;
        }

        // Si estamos agresivos, presionamos al rival más cercano
        Vec2 closestOpponent = findClosestOpponent(time);
        if (closestOpponent != null && aggressivenessWeight > 1.0) {
            abstract_robot.setSteerHeading(time, closestOpponent.t);
            abstract_robot.setSpeed(time, p.speed / 2 * aggressivenessWeight);
            return;
        }

        // Sino, ir a la pelota
        if (predictedBallPosition != null) {
            abstract_robot.setSteerHeading(time, predictedBallPosition.t);
            abstract_robot.setSpeed(time, p.speed);
        }
    }

    private Vec2 predictBallPosition(Vec2 ball, long time) {
        Vec2 velocity = abstract_robot.getBall(time);
        Vec2 predictedPosition = new Vec2(ball);
        // Ajustes para predecir la pelota un poco más lejos
        velocity.setr(velocity.r * 1.5);
        predictedPosition.add(velocity);

        predictedPosition.setr(predictedPosition.r * 1.2);
        return predictedPosition;
    }

    private Vec2 findClosestOpponent(long time) {
        return findClosest(abstract_robot.getOpponents(time), abstract_robot.getPosition(time));
    }

    private Vec2 findClosest(Vec2[] entities, Vec2 reference) {
        return entities == null ? null : Arrays.stream(entities)
                .filter(entity -> entity != null)
                .min((e1, e2) -> Double.compare(calculateDistance(e1, reference), calculateDistance(e2, reference)))
                .orElse(null);
    }

    /**
     * Retorna una dirección libre evitando obstáculos en un rango +/- range.
     */
    private double getFreeDirection(Vec2 force, double range, long time) {
        Vec2[] obstacles = abstract_robot.getObstacles(time);
        double step = Math.PI / 36;

        for (double angle = -range; angle <= range; angle += step) {
            Vec2 testDirection = new Vec2(force);
            testDirection.sett(force.t + angle);

            if (Arrays.stream(obstacles).noneMatch(obstacle -> isDirectionBlocked(testDirection, obstacle, range))) {
                return testDirection.t;
            }
        }
        return force.t;
    }

    private boolean isDirectionBlocked(Vec2 direction, Vec2 obstacle, double range) {
        Vec2 relativeObstacle = new Vec2(obstacle);
        relativeObstacle.sub(direction);
        return relativeObstacle.r < range;
    }
}
