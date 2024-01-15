/*
 * DoogHomoG.java
 */

import EDU.gatech.cc.is.util.Vec2;
import EDU.gatech.cc.is.abstractrobot.*;

public class DoogHomoG extends ControlSystemSS {
    // Variables para el algoritmo genético
    public double[] disAttack = new double[3]; // Distancias para el modo de ataque
    public double[] disDefend = new double[3]; // Distancias para el modo de defensa
    public double[] disGoalie = new double[3]; // Distancias para el modo de portero
    public double[] disKick = new double[3]; // Distancias para el modo de patear
    public double[] disTeam = new double[3]; // Distancias para el modo de equipo
    public double[] disPos = new double[3]; // Distancias para el modo de posicionamiento
    public double[] disBall = new double[3]; // Distancias para el modo de pelota
    public double[] disGoal = new double[3]; // Distancias para el modo de gol
    public double[] disOpponent = new double[3]; // Distancias para el modo de oponente
    public double[] disTeammate = new double[3]; // Distancias para el modo de compañero
    public double[] disGoalieBall = new double[3]; // Distancias para el modo de portero con pelota
    public double[] disGoalieOpponent = new double[3]; // Distancias para el modo de portero con oponente
    public double[] disGoalieTeammate = new double[3]; // Distancias para el modo de portero con compañero

    // Current state variables
    Vec2 CurMyPos, CurBallPos, CurBallPosEgo;
    long CurTime;
    int CurMode;
    boolean KickIt = false;
    int MyNum;
    Vec2 CurTeammates[], CurOpponents[];
    Vec2 CurOurGoal, CurOpponentsGoal;
    int StuckCount = 0;

    // Previous step state variables
    Vec2 PrevMyPos, PrevBallPos, PrevBallPosEgo;
    long PrevTime;
    int PrevMode;
    Vec2 PrevTeammates[], PrevOpponents[];

    // Modes
    final int MODE_ATTACK = 0;
    final int MODE_GOON = 1;
    final int MODE_GOALIE = 2;

    // Useful constants
    final double PI = Math.PI;
    final double DEFAULT_SPEED = 1.0;
    final double DEFENDED_DISTANCE = 0.03;
    final double BETWEEN_GOAL_ANGLE = PI / 6;
    final double ROBOT_RADIUS = 0.06;
    final double BALL_RADIUS = 0.02;
    final double GOAL_WIDTH = .4;
    final double STUCK_LIMIT = 50;
    final double KICK_DISTANCE = 1.0;

    // Initialize the important previous values
    public void Configure() {
        PrevMyPos = new Vec2(0, 0);
        PrevBallPos = new Vec2(0, 0);
        PrevBallPosEgo = new Vec2(0, 0);
    }

    // Método para configurar los parámetros genéticos
    public void setParam(Integer[] params) {
        // Asegúrate de que params tiene la longitud adecuada
        if (params.length != 38) {
            throw new IllegalArgumentException("Se esperan 38 parámetros genéticos");
        }

        int index = 0;
        for (int i = 0; i < 3; i++, index++) {
            this.disAttack[i] = params[index] / 10.0;
            // System.out.println("Asignando params[" + index + "] a algunArray[" + i +
            // "]");
        }
        for (int i = 0; i < 3; i++, index++) {
            this.disDefend[i] = params[index] / 10.0;
            // System.out.println("Asignando params[" + index + "] a algunArray[" + i +
            // "]");
        }
        for (int i = 0; i < 3; i++, index++) {
            this.disGoalie[i] = params[index] / 10.0;
            // System.out.println("Asignando params[" + index + "] a algunArray[" + i +
            // "]");
        }
        for (int i = 0; i < 3; i++, index++) {
            this.disKick[i] = params[index] / 10.0;
            // System.out.println("Asignando params[" + index + "] a algunArray[" + i +
            // "]");
        }
        for (int i = 0; i < 3; i++, index++) {
            this.disTeam[i] = params[index] / 10.0;
            // System.out.println("Asignando params[" + index + "] a algunArray[" + i +
            // "]");
        }
        for (int i = 0; i < 3; i++, index++) {
            this.disPos[i] = params[index] / 10.0;
            // System.out.println("Asignando params[" + index + "] a algunArray[" + i +
            // "]");
        }
        for (int i = 0; i < 3; i++, index++) {
            this.disBall[i] = params[index] / 10.0;
            // System.out.println("Asignando params[" + index + "] a algunArray[" + i +
            // "]");
        }
        for (int i = 0; i < 3; i++, index++) {
            this.disGoal[i] = params[index] / 10.0;
            // System.out.println("Asignando params[" + index + "] a algunArray[" + i +
            // "]");
        }
        for (int i = 0; i < 3; i++, index++) {
            this.disOpponent[i] = params[index] / 10.0;
            // System.out.println("Asignando params[" + index + "] a algunArray[" + i +
            // "]");
        }
        for (int i = 0; i < 3; i++, index++) {
            this.disTeammate[i] = params[index] / 10.0;
            // System.out.println("Asignando params[" + index + "] a algunArray[" + i +
            // "]");
        }
        for (int i = 0; i < 3; i++, index++) {
            this.disGoalieBall[i] = params[index] / 10.0;
            // System.out.println("Asignando params[" + index + "] a algunArray[" + i +
            // "]");
        }
        for (int i = 0; i < 3; i++, index++) {
            this.disGoalieOpponent[i] = params[index] / 10.0;
            // System.out.println("Asignando params[" + index + "] a algunArray[" + i +
            // "]");
        }
        for (int i = 0; i < 2; i++, index++) {
            this.disGoalieTeammate[i] = params[index] / 10.0;
            // System.out.println("Asignando params[" + index + "] a algunArray[" + i +
            // "]");
        }
    }

    /**
     * Called every timestep to allow the control system to
     * run.
     */

    public int TakeStep() {
        // Used to hold random heading in case we're stuck.
        double RandomHeading = 0;

        // This var will hold final steering command at end.
        Vec2 Cmd = new Vec2(0, DEFAULT_SPEED);

        // My number, for debugging purposes only!
        MyNum = abstract_robot.getPlayerNumber(CurTime);

        // Read current time
        CurTime = abstract_robot.getTime();
        // Get current position
        CurMyPos = abstract_robot.getPosition(CurTime);
        // Get egocentric ball position, then convert to absolute position
        CurBallPosEgo = abstract_robot.getBall(CurTime);
        CurBallPos = new Vec2(CurBallPosEgo.x, CurBallPosEgo.y);
        CurBallPos.add(CurMyPos);
        // Get goal positions
        CurOurGoal = abstract_robot.getOurGoal(CurTime);
        CurOpponentsGoal = abstract_robot.getOpponentsGoal(CurTime);
        // Get team positions
        CurTeammates = abstract_robot.getTeammates(CurTime);
        CurOpponents = abstract_robot.getOpponents(CurTime);

        // Determine my current mode
        if (ShouldAttack()) {
            CurMode = MODE_ATTACK;
        } else if (ShouldBeGoalie()) {
            CurMode = MODE_GOALIE;
        } else {
            CurMode = MODE_GOON;
        }

        // Act based on current mode
        switch (CurMode) {
            case (MODE_ATTACK):
                // Go for the glory!
                Cmd = AttackMode();
                break;

            case (MODE_GOON):
                // Find the nearest undefended opponent, and kick his ass!
                Cmd = GoonMode();

                break;
            case (MODE_GOALIE):
                // Play some serious defense. Unless we get greedy.
                Cmd = GoalieMode();
                break;

            default:
                System.out.println("Unknown Mode!");
                break;
        }

        // Check to see whether we're locked up.
        if ((Math.abs(CurMyPos.x - PrevMyPos.x) == 0.0) &&
                (Math.abs(CurMyPos.y - PrevMyPos.y) == 0.0)) {
            StuckCount++;
        } else {
            StuckCount = 0;
        }

        // If we've been stuck too long, get away!
        if (StuckCount > STUCK_LIMIT) {
            RandomHeading = Math.random() * 2 * PI;
            abstract_robot.setSteerHeading(CurTime, RandomHeading);
            if (StuckCount > 2 * STUCK_LIMIT)
                StuckCount = 0;
        }
        // Otherwise, carry on with Cmd.
        else
            abstract_robot.setSteerHeading(CurTime, Cmd.t);
        abstract_robot.setSpeed(CurTime, 1.0);

        // Save old values of everything.
        PrevTime = CurTime;
        PrevMyPos = new Vec2(CurMyPos.x, CurMyPos.y);
        PrevBallPos = new Vec2(CurBallPos.x, CurBallPos.y);

        // Kick, if we can and wish to.
        if (abstract_robot.canKick(CurTime) && KickIt) {
            abstract_robot.kick(CurTime);
        }

        // tell the parent we're OK
        return (CSSTAT_OK);
    }

    boolean ShouldBeGoalie() {
        // If we're the closest to our goal, we should be goalie.
        if (ClosestTo(CurMyPos, EgoToAbs(CurOurGoal)))
            return (true);
        return (false);
    }

    boolean ShouldAttack() {

        if (ClosestTo(CurMyPos, CurBallPos)) {
            return (true);
        } else {
            return (false);
        }
    }

    // Modo de ataque
    Vec2 AttackMode() {
        abstract_robot.setDisplayString("Attack");

        // Obtiene la distancia de ataque basada en el número del jugador
        double attackDistance = disAttack[MyNum % disAttack.length];
        double attackBallDistance = disBall[MyNum % disBall.length];
        double attackGoalDistance = disGoal[MyNum % disGoal.length];
        double attackTeammateDistance = disTeammate[MyNum % disTeammate.length];
        double attackOpponentDistance = disOpponent[MyNum % disOpponent.length];

        // Posición egocéntrica del balón
        Vec2 BallSpot = new Vec2(CurBallPosEgo.x, CurBallPosEgo.y);
        // Posición de la portería del oponente
        Vec2 GoalSpot = new Vec2(CurOpponentsGoal.x, CurOpponentsGoal.y);

        // Si el balón está en nuestra zona, nos acercamos
        if (BallInOwnZone()) {
            BallSpot.setr(attackBallDistance);
            BallSpot.add(CurMyPos);
            return BallSpot;
        }

        // Si hay un compañero en nuestra zona, nos acercamos
        Vec2 TeammateSpot = new Vec2(99999, 0);
        for (int i = 0; i < CurTeammates.length; i++) {
            if (BetweenGoal(CurTeammates[i], CurOurGoal) && (CurTeammates[i].r < TeammateSpot.r)) {
                TeammateSpot = CurTeammates[i];
            }
        }
        if (TeammateSpot.r < attackTeammateDistance) {
            return TeammateSpot;
        }

        // Si hay un oponente en nuestra zona, nos acercamos
        Vec2 OpponentSpot = new Vec2(99999, 0);
        for (int i = 0; i < CurOpponents.length; i++) {
            if (BetweenGoal(CurOpponents[i], CurOurGoal) && (CurOpponents[i].r < OpponentSpot.r)) {
                OpponentSpot = CurOpponents[i];
            }
        }
        if (OpponentSpot.r < attackOpponentDistance) {
            return OpponentSpot;
        }

        // Si estamos demasiado lejos del balón, nos acercamos
        if (CurBallPosEgo.r > attackDistance) {
            BallSpot.setr(attackDistance);
            BallSpot.add(CurMyPos);
            return BallSpot;
        }

        // Si estamos demasiado lejos de la portería, nos acercamos
        if (CurMyPos.r > attackGoalDistance) {
            GoalSpot.setr(attackGoalDistance);
            GoalSpot.add(CurMyPos);
            return GoalSpot;
        }

        // Ajustes en función de la posición del robot en el campo
        if (CurMyPos.y > 0)
            GoalSpot.y += 0.9 * (GOAL_WIDTH / 2.0);
        if (CurMyPos.y < 0)
            GoalSpot.y -= 0.9 * (GOAL_WIDTH / 2.0);

        // Calcula la posición objetivo restando la posición de la portería del oponente
        // de la posición del balón
        GoalSpot.sub(CurOpponentsGoal);
        GoalSpot.setr(ROBOT_RADIUS);
        GoalSpot.add(CurBallPosEgo);

        // Decide si patear el balón
        KickIt = Math.abs(CurOpponentsGoal.r) < KICK_DISTANCE;

        // Devuelve el punto objetivo
        return GoalSpot;
    }

    // Modo de Defensa
    Vec2 GoonMode() {
        double goonDistance = disDefend[MyNum % disDefend.length];
        double goonBallDistance = disBall[MyNum % disBall.length];
        double goonGoalDistance = disGoal[MyNum % disGoal.length];
        double goonTeammateDistance = disTeammate[MyNum % disTeammate.length];
        double goonOpponentDistance = disOpponent[MyNum % disOpponent.length];

        Vec2 BallSpot = new Vec2(CurBallPos.x, CurBallPos.y);
        Vec2 GoalSpot = new Vec2(CurOpponentsGoal.x, CurOpponentsGoal.y);
        Vec2 TeammateSpot = new Vec2(99999, 0);
        Vec2 OpponentSpot = new Vec2(99999, 0);

        // Si el balón está en nuestra zona, nos acercamos
        if (BallInOwnZone()) {
            BallSpot.setr(goonBallDistance);
            BallSpot.add(CurMyPos);
            return BallSpot;
        }

        // Si hay un compañero en nuestra zona, nos acercamos
        for (int i = 0; i < CurTeammates.length; i++) {
            if (BetweenGoal(CurTeammates[i], CurOurGoal) && (CurTeammates[i].r < TeammateSpot.r)) {
                TeammateSpot = CurTeammates[i];
            }
        }
        if (TeammateSpot.r < goonTeammateDistance) {
            return TeammateSpot;
        }

        // Si hay un oponente en nuestra zona, nos acercamos
        for (int i = 0; i < CurOpponents.length; i++) {
            if (BetweenGoal(CurOpponents[i], CurOurGoal) && (CurOpponents[i].r < OpponentSpot.r)) {
                OpponentSpot = CurOpponents[i];
            }
        }

        if (OpponentSpot.r < goonOpponentDistance) {
            return OpponentSpot;
        }

        // Si estamos demasiado lejos del balón, nos acercamos
        if (CurBallPos.r > goonDistance) {
            BallSpot.setr(goonDistance);
            BallSpot.add(CurMyPos);
            return BallSpot;
        }

        // Si estamos demasiado lejos de la portería, nos acercamos
        if (CurMyPos.r > goonGoalDistance) {
            GoalSpot.setr(goonGoalDistance);
            GoalSpot.add(CurMyPos);
            return GoalSpot;
        }

        // Ajustes en función de la posición del robot en el campo
        if (CurMyPos.y > 0)
            GoalSpot.y += 0.9 * (GOAL_WIDTH / 2.0);
        if (CurMyPos.y < 0)
            GoalSpot.y -= 0.9 * (GOAL_WIDTH / 2.0);

        // Calcula la posición objetivo restando la posición de la portería del oponente
        // de la posición del balón
        GoalSpot.sub(CurOpponentsGoal);
        GoalSpot.setr(ROBOT_RADIUS);
        GoalSpot.add(CurBallPosEgo);

        // Decide si patear el balón
        KickIt = Math.abs(CurOpponentsGoal.r) < KICK_DISTANCE;

        // Devuelve el punto objetivo
        return GoalSpot;
    }

    // Modo de Arquero
    Vec2 GoalieMode() {
        abstract_robot.setDisplayString("Goalie");
        double goalieDistance = disGoalie[MyNum % disGoalie.length];
        double goalieBallDistance = disGoalieBall[MyNum % disGoalieBall.length];
        double goalieOpponentDistance = disGoalieOpponent[MyNum % disGoalieOpponent.length];
        double goalieTeammateDistance = disGoalieTeammate[MyNum % disGoalieTeammate.length];

        Vec2 GoalieSpot = new Vec2(CurOurGoal.x, CurOurGoal.y);
        Vec2 BallSpot = new Vec2(CurBallPos.x, CurBallPos.y);
        Vec2 OpponentSpot = new Vec2(99999, 0);
        Vec2 TeammateSpot = new Vec2(99999, 0);

        // Si el balón está en nuestra zona, nos acercamos
        if (BallInOwnZone()) {
            BallSpot.setr(goalieBallDistance);
            BallSpot.add(CurMyPos);
            return BallSpot;
        }

        // Si hay un oponente en nuestra zona, nos acercamos
        for (int i = 0; i < CurOpponents.length; i++) {
            if (BetweenGoal(CurOpponents[i], CurOurGoal) && (CurOpponents[i].r < OpponentSpot.r)) {
                OpponentSpot = CurOpponents[i];
            }
        }
        if (OpponentSpot.r < goalieOpponentDistance) {
            return OpponentSpot;
        }

        // Si hay un compañero en nuestra zona, nos acercamos
        for (int i = 0; i < CurTeammates.length; i++) {
            if (BetweenGoal(CurTeammates[i], CurOurGoal) && (CurTeammates[i].r < TeammateSpot.r)) {
                TeammateSpot = CurTeammates[i];
            }
        }
        if (TeammateSpot.r < goalieTeammateDistance) {
            return TeammateSpot;
        }

        // Si estamos demasiado lejos de la portería, nos acercamos
        if (CurMyPos.r > goalieDistance) {
            GoalieSpot.setr(goalieDistance);
            GoalieSpot.add(CurMyPos);
            return GoalieSpot;
        }

        // Si estamos demasiado lejos de la portería, nos acercamos
        if (CurMyPos.r > goalieDistance) {
            GoalieSpot.setr(goalieDistance);
            GoalieSpot.add(CurMyPos);
            return GoalieSpot;
        }

        // Ajustes en función de la posición del robot en el campo
        if (CurMyPos.y > 0)
            GoalieSpot.y += 0.9 * (GOAL_WIDTH / 2.0);
        if (CurMyPos.y < 0)
            GoalieSpot.y -= 0.9 * (GOAL_WIDTH / 2.0);

        // Calcula la posición objetivo restando la posición de la portería del oponente
        // de la posición del balón
        GoalieSpot.sub(CurOpponentsGoal);
        GoalieSpot.setr(ROBOT_RADIUS);
        GoalieSpot.add(CurBallPosEgo);

        // Decide si patear el balón
        KickIt = Math.abs(CurOpponentsGoal.r) < KICK_DISTANCE;

        // Devuelve el punto objetivo
        return GoalieSpot;
    }

    boolean Undefended(Vec2 opponent) {
        Vec2 AbsOpp = EgoToAbs(opponent);

        // return true if there is no teammate within
        // DEFENDED_DISTANCE of opponent.
        for (int i = 0; i < CurTeammates.length; i++) {
            Vec2 AbsPos = EgoToAbs(CurTeammates[i]);
            Vec2 DiffPos = new Vec2(AbsOpp.x - AbsPos.x, AbsOpp.y - AbsPos.y);

            if (DiffPos.r < 2 * ROBOT_RADIUS + DEFENDED_DISTANCE)
                return (false);
        }
        return (true);
    }

    Vec2 EgoToAbs(Vec2 EgoPos) {
        Vec2 AbsPosition = new Vec2(EgoPos.x, EgoPos.y);
        AbsPosition.add(CurMyPos);
        return (AbsPosition);
    }

    boolean DefendingEast() {
        return (CurOurGoal.x < 0);
    }

    boolean BetweenGoal(Vec2 Opp, Vec2 Goal) {
        Vec2 OpptoGoal = new Vec2(Goal.x, Goal.y);
        OpptoGoal.sub(Opp);

        Vec2 OpptoMe = new Vec2(CurMyPos.x, CurMyPos.y);
        OpptoMe.sub(Opp);

        if (Math.abs(OpptoGoal.t - OpptoMe.t) < BETWEEN_GOAL_ANGLE) {
            return (true);
        }
        return (false);
    }

    boolean BallInOwnZone() {
        if (CurOurGoal.x * CurBallPos.x > 0)
            return (true);
        return (false);
    }

    boolean ClosestTo(Vec2 Me, Vec2 SpotAbs) {
        // Stolen from Kechze
        Vec2 temp = new Vec2(Me.x, Me.y);
        temp.sub(SpotAbs);

        double MyDist = temp.r;
        for (int i = 0; i < CurTeammates.length; i++) {
            temp = new Vec2(CurTeammates[i].x, CurTeammates[i].y);
            temp.add(Me);
            temp.sub(SpotAbs);
            double TheirDist = temp.r;
            if (TheirDist <= MyDist)
                return false;
        }
        return true;
    }
}
