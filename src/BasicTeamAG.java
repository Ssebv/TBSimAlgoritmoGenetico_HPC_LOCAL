import EDU.gatech.cc.is.util.Vec2;
import EDU.gatech.cc.is.abstractrobot.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Logger;

/**
 * BasicTeamAG.java
 * Equipo de robots controlados con lógica optimizada para AG.
 */
public class BasicTeamAG extends ControlSystemSS {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(BasicTeamAG.class.getName());

    private static final int NUM_PLAYERS = 5;
    private static final int PARAM_COUNT = 50;

    @SuppressWarnings("unused")
    private static final double FIELD_WIDTH = 1.525;
    private static final double FIELD_LENGTH = 2.74;
    private static final double GOAL_WIDTH = 0.5;

    @SuppressWarnings("unused")
    private static final double MARGIN = 0.02;
    private static final double RANGE = 0.3;

    private final PlayerParams[] playerParams = new PlayerParams[NUM_PLAYERS];
    private Vec2 offensivePos1, offensivePos2;

    /**
     * Clase interna para encapsular los parámetros de un jugador.
     */
    private static class PlayerParams {
        double forceLimit;
        double defenseWeight;
        double attackWeight;
        double passThreshold;
        double opponentAvoidance;
        double speed;
    }

    public BasicTeamAG() {
        for (int i = 0; i < NUM_PLAYERS; i++) {
            playerParams[i] = new PlayerParams();
        }
    }

    @Override
    public void Configure() {
        Vec2 goal = abstract_robot.getOpponentsGoal(0L);
        if (goal.x > 0.0) {
            offensivePos1 = new Vec2(4.0 * FIELD_LENGTH / 10.0, GOAL_WIDTH / 2.0 + SocSmall.RADIUS);
            offensivePos2 = new Vec2(4.0 * FIELD_LENGTH / 10.0, -GOAL_WIDTH / 2.0 - SocSmall.RADIUS);
        } else {
            offensivePos1 = new Vec2(-4.0 * FIELD_LENGTH / 10.0, GOAL_WIDTH / 2.0 + SocSmall.RADIUS);
            offensivePos2 = new Vec2(-4.0 * FIELD_LENGTH / 10.0, -GOAL_WIDTH / 2.0 - SocSmall.RADIUS);
        }
    }

    public void setParam(Double[] params) {
        if (params == null || params.length != PARAM_COUNT) {
            throw new IllegalArgumentException("Se requieren exactamente " + PARAM_COUNT + " parámetros.");
        }

        for (int i = 0; i < NUM_PLAYERS; i++) {
            PlayerParams p = playerParams[i];
            int baseIndex = i * 10;

            p.forceLimit = params[baseIndex];
            p.defenseWeight = params[baseIndex + 1];
            p.attackWeight = params[baseIndex + 2];
            p.passThreshold = params[baseIndex + 3];
            p.opponentAvoidance = params[baseIndex + 6];
            p.speed = params[baseIndex + 9];
        }
    }

    @Override
    public int TakeStep() {
        long time = abstract_robot.getTime();
        Vec2 ball = abstract_robot.getBall(time);
        int mynum = abstract_robot.getPlayerNumber(time);
        PlayerParams p = playerParams[mynum];

        if (!isClosestToBall(ball, time)) {
            Vec2 force = calculateForces(time, p);

            if (force.r < p.forceLimit) {
                abstract_robot.setSteerHeading(time, ball.t);
                abstract_robot.setSpeed(time, 0.0);
                abstract_robot.setDisplayString("Tracking ball");
            } else {
                abstract_robot.setSteerHeading(time, getFreeDirection(force, RANGE, time));
                abstract_robot.setSpeed(time, p.speed);
                abstract_robot.setDisplayString("Force: " + (int) (100.0 * force.r) + ", " + force.t);
            }
            return CSSTAT_OK;
        }

        if (shouldPassToTeammate(time, ball, p)) {
            Vec2 passTarget = findBestTeammate(time, ball, p);
            abstract_robot.setSteerHeading(time, passTarget.t);
            abstract_robot.setSpeed(time, p.speed);
            abstract_robot.setDisplayString("Passing");
        } else {
            Vec2 goal = getKickPosition(ball, abstract_robot.getOpponentsGoal(time), p);
            if (!abstract_robot.canKick(time)) {
                abstract_robot.setSteerHeading(time, getFreeDirection(goal, RANGE, time));
                abstract_robot.setSpeed(time, p.speed);
            } else {
                abstract_robot.kick(time);
                abstract_robot.setSpeed(time, p.speed);
                abstract_robot.setDisplayString("Kicking");
            }
        }
        return CSSTAT_OK;
    }

    private boolean isClosestToBall(Vec2 ball, long time) {
        Vec2[] teammates = abstract_robot.getTeammates(time);
        for (Vec2 teammate : teammates) {
            Vec2 diff = new Vec2(ball);
            diff.sub(teammate);
            if (diff.r < ball.r) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldPassToTeammate(long time, Vec2 ball, PlayerParams p) {
        Vec2[] teammates = abstract_robot.getTeammates(time);
        for (Vec2 teammate : teammates) {
            if (teammate.r < p.passThreshold) {
                return true;
            }
        }
        return false;
    }

    private Vec2 findBestTeammate(long time, Vec2 ball, PlayerParams p) {
        Vec2[] teammates = abstract_robot.getTeammates(time);
        return Arrays.stream(teammates)
                .filter(teammate -> teammate.r < p.passThreshold)
                .min(Comparator.comparingDouble(teammate -> teammate.r))
                .orElse(ball);
    }

    private Vec2 calculateForces(long time, PlayerParams p) {
        Vec2 pos = abstract_robot.getPosition(time);
        Vec2 force = new Vec2(0, 0);

        Vec2 defenseForce = new Vec2(-p.defenseWeight * pos.x, -p.defenseWeight * pos.y);
        force.add(defenseForce);

        Vec2 attackForce = calculateAttackForce(pos, p);
        force.add(attackForce);

        for (Vec2 opponent : abstract_robot.getOpponents(time)) {
            Vec2 temp = new Vec2(opponent);
            temp.setr(p.opponentAvoidance / (temp.r * temp.r));
            temp.rotate(Math.PI);
            force.add(temp);
        }

        return force;
    }

    private Vec2 calculateAttackForce(Vec2 pos, PlayerParams p) {
        Vec2 force = new Vec2(0, 0);
        Vec2 target1 = new Vec2(offensivePos1);
        Vec2 target2 = new Vec2(offensivePos2);

        target1.sub(pos);
        target1.setr(p.attackWeight / (target1.r * target1.r));
        force.add(target1);

        target2.sub(pos);
        target2.setr(p.attackWeight / (target2.r * target2.r));
        force.add(target2);

        return force;
    }

    private Vec2 getKickPosition(Vec2 ball, Vec2 target, PlayerParams p) {
        Vec2 v = new Vec2(target);
        v.sub(ball);
        double angleOffset = Math.PI / 6;
        v.sett(v.t + angleOffset);
        return v;
    }

    private double getFreeDirection(Vec2 goal, double range, long time) {
        return goal.t;
    }
}
