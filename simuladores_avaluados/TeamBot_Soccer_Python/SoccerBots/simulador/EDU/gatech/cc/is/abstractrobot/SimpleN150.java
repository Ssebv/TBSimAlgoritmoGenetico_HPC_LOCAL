/*
 * SimpleN150.java
 */

package EDU.gatech.cc.is.abstractrobot;

/**
 * Provides an abstract interface to the hardware of
 * a basic Nomadic Technologies Nomad 150 robot (no vision, gripper
 * or communication).
 * In addition to the capabilities provided by a Simple robot,
 * we also get a rotating turret.
 * <p>
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1997, 1998 Tucker Balch
 *
 * @author Tucker Balch
 * @version $Revision: 1.1 $
 */

public interface SimpleN150 extends SimpleInterface {
    double MAX_TRANSLATION = 0.508;
    double MAX_STEER = 0.7854;
    double RADIUS = 0.247;

    /**
     * Max rate of turn of the turret in radians/sec.
     */
    double MAX_TURRET = 0.7854;

    /**
     * How far sonar ring is from center of robot.
     */
    double SONAR_RADIUS = 0.23;


    /**
     * Get the current heading of the turret motor.
     *
     * @param timestamp only get new information
     *                  if timestamp > than last call or timestamp == -1.
     * @return the turret heading in radians.
     * @see SimpleN150#setTurretHeading
     * @see SimpleN150#resetTurretHeading
     */
    double getTurretHeading(long timestamp);


    /**
     * Reset the turret odometry of the robot in global coordinates.
     * This might be done when reliable sensor information provides
     * a very good estimate of the robot's turret heading.
     * Do this only if you are certain you're right!
     *
     * @param heading the new turret heading in radians.
     * @see SimpleN150#getTurretHeading
     * @see SimpleN150#setTurretHeading
     */
    void resetTurretHeading(double heading);

    /**
     * Set the desired heading for the turret motor.
     *
     * @param timestamp only get new information
     *                  if timestamp > than last call or timestamp == -1.
     * @param heading   the heading in radians.
     * @see SimpleN150#getTurretHeading
     * @see SimpleN150#resetTurretHeading
     */
    void setTurretHeading(long timestamp, double heading);
}
