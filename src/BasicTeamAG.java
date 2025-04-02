import EDU.gatech.cc.is.util.Vec2;
import EDU.gatech.cc.is.abstractrobot.*;

import java.util.Arrays;

/**
 * BasicTeamAG.java
 * Equipo de robots autónomos con lógica ofensiva/defensiva optimizada.
 */
public class BasicTeamAG extends ControlSystemSS {

    // === Constantes de configuración ===
    private static final int NUM_PLAYERS = 5;
    private static final double FIELD_LENGTH = 2.74;
    private static final double GOAL_WIDTH = 0.5;
    private static final double IDEAL_DISTANCE = 0.4;
    private static final double RANGE = 0.3;
    private static final double CLOSE_TO_GOAL_DISTANCE = 1.0;

    // === Parámetros individuales y de equipo ===
    private final PlayerParams[] playerParams = new PlayerParams[NUM_PLAYERS];
    private double teamCoordinationWeight;
    private double proximityWeight;
    private double goalAlignmentWeight;
    private double gameStateAdaptation;
    private double aggressivenessWeight;
    private double defensiveLineWeight;

    // === Posiciones ofensivas de referencia ===
    private Vec2 offensivePos1, offensivePos2;

    /** Clase interna para almacenar parámetros por jugador */
    private static class PlayerParams {
        double forceLimit, defenseWeight, attackWeight, passThreshold;
        double opponentAvoidance, speed, shootingAccuracy, passingAccuracy, positioning;
    }

    public BasicTeamAG() {
        for (int i = 0; i < NUM_PLAYERS; i++) {
            playerParams[i] = new PlayerParams();
        }
    }

    /** Configuración inicial al cargar el robot */
    @Override
    public void Configure() {
        Vec2 goal = abstract_robot.getOpponentsGoal(0L);
        offensivePos1 = getOffensivePosition(goal.x > 0, true);
        offensivePos2 = getOffensivePosition(goal.x > 0, false);
    }

    /** Calcula una posición ofensiva preferida según lado del campo */
    private Vec2 getOffensivePosition(boolean rightSide, boolean upper) {
        double x = rightSide ? 0.6 * FIELD_LENGTH : -0.6 * FIELD_LENGTH;
        double y = upper ? GOAL_WIDTH / 2.0 : -GOAL_WIDTH / 2.0;
        return new Vec2(x, y);
    }

    /** Recibe los 60 parámetros del cromosoma y los asigna */
    public void setParam(Double[] params) {
        if (params == null || params.length != 60) throw new IllegalArgumentException("Se esperan 60 parámetros.");

        for (int i = 0; i < NUM_PLAYERS; i++) {
            PlayerParams p = playerParams[i];
            int base = i * 10;
            p.forceLimit = 1.0 + params[base];
            p.defenseWeight = 5.0 + params[base + 1] * 0.5;
            p.attackWeight = 5.0 + params[base + 2] * 0.5;
            p.passThreshold = 0.1 + params[base + 3] * 0.5;
            p.opponentAvoidance = 1.0 + params[base + 4];
            p.speed = 2.0 + params[base + 5];
            p.shootingAccuracy = 5.0 + params[base + 6] * 0.5;
            p.passingAccuracy = 4.0 + params[base + 7] * 0.5;
            p.positioning = 4.0 + params[base + 8];

            // Roles específicos
            if (i == 0) { // Arquero
                p.defenseWeight *= 1.5;
                p.attackWeight *= 0.2;
                p.shootingAccuracy = 0;
                p.passingAccuracy *= 0.8;
                p.positioning *= 1.5;
                p.speed *= 0.7;
            } else if (i == 4) { // Defensa marcador
                p.defenseWeight *= 1.3;
                p.attackWeight *= 0.5;
                p.positioning *= 1.2;
                p.speed *= 0.9;
            }
        }

        // Parámetros de equipo
        teamCoordinationWeight = 5.0 + params[50] * 0.5;
        proximityWeight = params[51] * 0.5;
        goalAlignmentWeight = 5.0 + params[52] * 0.5;
        gameStateAdaptation = 0.5 + params[53] * 1.5;
        aggressivenessWeight = 1.0 + params[54];
        defensiveLineWeight = 5.0 + params[55] * 0.5;
    }

    /** Loop principal de cada jugador */
    @Override
    public int TakeStep() {
        long time = abstract_robot.getTime();
        Vec2 ball = abstract_robot.getBall(time);
        int mynum = abstract_robot.getPlayerNumber(time);
        PlayerParams p = playerParams[mynum];

        adaptWeights(time, ball);
        Vec2 force = calculateTotalForce(time, p, ball);
        handleMovement(time, force, ball, p);

        if (isOffensive(ball, time)) {
            executeOffense(time, ball, p);
        } else {
            executeDefense(time, ball, p);
        }

        return CSSTAT_OK;
    }

    // === Sección de movimiento y decisiones ===

    private void adaptWeights(long time, Vec2 ball) {
        if (isOffensive(ball, time)) {
            teamCoordinationWeight *= gameStateAdaptation;
            proximityWeight /= gameStateAdaptation;
            goalAlignmentWeight *= 1.5;
            aggressivenessWeight *= 1.5;
        } else {
            teamCoordinationWeight /= gameStateAdaptation;
            proximityWeight *= gameStateAdaptation;
            defensiveLineWeight *= 1.1;
        }
    }

    private Vec2 calculateTotalForce(long time, PlayerParams p, Vec2 ball) {
        Vec2 pos = abstract_robot.getPosition(time);
        Vec2 force = new Vec2(0, 0);

        if (ball != null && ball.r > FIELD_LENGTH * 1.2) return force;

        addForce(force, new Vec2(-p.defenseWeight * pos.x, -p.defenseWeight * pos.y));
        addForce(force, calculateAttackVector(pos, p));
        addProximityForce(force, ball);
        addAlignmentForce(force, time);
        addCoordinationForce(force, pos, time);
        return force;
    }

    private void addForce(Vec2 base, Vec2 add) {
        base.add(add);
    }

    private void addProximityForce(Vec2 force, Vec2 ball) {
        if (ball != null && ball.r > 0.01) {
            Vec2 prox = new Vec2(ball);
            prox.setr(proximityWeight / (Math.max(0.01, ball.r) * ball.r));
            force.add(prox);
        }
    }

    private void addAlignmentForce(Vec2 force, long time) {
        Vec2 goal = abstract_robot.getOpponentsGoal(time);
        if (goal != null) {
            Vec2 align = new Vec2(goal);
            align.setr(goalAlignmentWeight);
            force.add(align);
        }
    }

    private void addCoordinationForce(Vec2 force, Vec2 pos, long time) {
        for (Vec2 mate : abstract_robot.getTeammates(time)) {
            if (mate != null) {
                Vec2 diff = new Vec2(mate);
                diff.sub(pos);
                double w = diff.r < IDEAL_DISTANCE ? -teamCoordinationWeight : teamCoordinationWeight;
                diff.setr(w / diff.r);
                force.add(diff);
            }
        }
    }

    private Vec2 calculateAttackVector(Vec2 pos, PlayerParams p) {
        Vec2 result = new Vec2(0, 0);
        addAttackTarget(pos, p, offensivePos1, result);
        addAttackTarget(pos, p, offensivePos2, result);
        return result;
    }

    private void addAttackTarget(Vec2 pos, PlayerParams p, Vec2 target, Vec2 total) {
        Vec2 t = new Vec2(target);
        t.sub(pos);
        if (t.r != 0) {
            t.setr(p.attackWeight * p.shootingAccuracy * aggressivenessWeight * 1.5 / (t.r * t.r));
            total.add(t);
        }
    }

    private void handleMovement(long time, Vec2 force, Vec2 ball, PlayerParams p) {
        Vec2 closest = findClosestOpponent(time);
        if (closest != null && closest.r < p.opponentAvoidance) {
            Vec2 avoid = new Vec2(-closest.x, -closest.y);
            avoid.setr(p.opponentAvoidance);
            abstract_robot.setSteerHeading(time, avoid.t);
            abstract_robot.setSpeed(time, p.speed / 2);
            return;
        }
        if (force.r < p.forceLimit) {
            abstract_robot.setSpeed(time, p.speed * 0.3); // evitar congelamiento
        } else {
            abstract_robot.setSteerHeading(time, getFreeDirection(force, RANGE, time));
            abstract_robot.setSpeed(time, p.speed);
        }
    }

    private void executeOffense(long time, Vec2 ball, PlayerParams p) {
        Vec2 goal = abstract_robot.getOpponentsGoal(time);
        Vec2 predicted = predictBall(ball, time);
        double dist = distance(abstract_robot.getPosition(time), goal);
        double align = alignmentScore(abstract_robot.getPosition(time), goal);

        if (dist < CLOSE_TO_GOAL_DISTANCE * 1.2 || (align > 0.4 && ball.r < FIELD_LENGTH / 2)) {
            abstract_robot.setSteerHeading(time, goal.t);
            abstract_robot.setSpeed(time, p.speed * p.shootingAccuracy * aggressivenessWeight * 1.5);
            return;
        }

        if (ball.r < p.passThreshold * aggressivenessWeight) {
            Vec2 pass = findBestPass(time, predictBall(ball, time), goal);
            if (pass != null) {
                abstract_robot.setSteerHeading(time, pass.t);
                abstract_robot.setSpeed(time, p.speed * p.passingAccuracy * aggressivenessWeight);
                return;
            }
        }

        abstract_robot.setSteerHeading(time, predicted.t);
        abstract_robot.setSpeed(time, p.speed * p.positioning * aggressivenessWeight);
    }

    private void executeDefense(long time, Vec2 ball, PlayerParams p) {
        Vec2 ownGoal = abstract_robot.getOurGoal(time);
        Vec2 predicted = predictBall(ball, time);

        if (ownGoal != null && ball != null && distance(ball, ownGoal) < CLOSE_TO_GOAL_DISTANCE) {
            abstract_robot.setSteerHeading(time, ball.t);
            abstract_robot.setSpeed(time, p.speed * 1.5);
            return;
        }

        if (ownGoal != null && ball.r > FIELD_LENGTH / 4) {
            Vec2 pos = new Vec2(ownGoal.x + defensiveLineWeight * aggressivenessWeight, ownGoal.y);
            abstract_robot.setSteerHeading(time, pos.t);
            abstract_robot.setSpeed(time, p.speed / 2);
            return;
        }

        Vec2 closest = findClosestOpponent(time);
        if (closest != null && aggressivenessWeight > 1.0) {
            abstract_robot.setSteerHeading(time, closest.t);
            abstract_robot.setSpeed(time, p.speed / 2 * aggressivenessWeight);
            return;
        }

        abstract_robot.setSteerHeading(time, predicted.t);
        abstract_robot.setSpeed(time, p.speed);
    }

    // === Utilidades ===

    private boolean isOffensive(Vec2 ball, long time) {
        Vec2 ourGoal = abstract_robot.getOurGoal(time);
        Vec2 oppGoal = abstract_robot.getOpponentsGoal(time);
        return ball != null && ourGoal != null && oppGoal != null &&
               distance(ball, ourGoal) > distance(ball, oppGoal);
    }

    private double distance(Vec2 from, Vec2 to) {
        Vec2 temp = new Vec2(from);
        temp.sub(to);
        return temp.r;
    }

    private double alignmentScore(Vec2 pos, Vec2 goal) {
        Vec2 dir = new Vec2(goal);
        dir.sub(pos);
        return Math.cos(dir.t);
    }

    private Vec2 predictBall(Vec2 ball, long time) {
        Vec2 velocity = abstract_robot.getBall(time);
        Vec2 predicted = new Vec2(ball);
        velocity.setr(velocity.r * 1.5);
        predicted.add(velocity);
        predicted.setr(predicted.r * 1.2);
        return predicted;
    }

    private Vec2 findClosestOpponent(long time) {
        return findClosest(abstract_robot.getOpponents(time), abstract_robot.getPosition(time));
    }

    private Vec2 findClosest(Vec2[] entities, Vec2 ref) {
        return Arrays.stream(entities)
                .filter(e -> e != null)
                .min((a, b) -> Double.compare(distance(a, ref), distance(b, ref)))
                .orElse(null);
    }

    private Vec2 findBestPass(long time, Vec2 ball, Vec2 goal) {
        Vec2[] mates = abstract_robot.getTeammates(time);
        return Arrays.stream(mates)
                .filter(t -> t != null)
                .min((t1, t2) -> Double.compare(passScore(t2, ball, goal), passScore(t1, ball, goal)))
                .orElse(null);
    }

    private double passScore(Vec2 teammate, Vec2 ball, Vec2 goal) {
        double align = alignmentScore(teammate, goal);
        double dist = distance(teammate, ball);
        return align / (dist + 0.001);
    }

    private double getFreeDirection(Vec2 force, double range, long time) {
        Vec2[] obstacles = abstract_robot.getObstacles(time);
        double step = Math.PI / 36;

        for (int i = 0; i < 30; i++) {
            double angle = -range + i * step;
            Vec2 candidate = new Vec2(force);
            candidate.sett(force.t + angle);

            if (Arrays.stream(obstacles).noneMatch(o -> o != null && isBlocked(candidate, o, range)))
                return candidate.t;
        }

        return force.t + Math.random() * 0.1 - 0.05;
    }

    private boolean isBlocked(Vec2 dir, Vec2 obs, double range) {
        Vec2 rel = new Vec2(obs);
        rel.sub(dir);
        return rel.r < range;
    }
}
