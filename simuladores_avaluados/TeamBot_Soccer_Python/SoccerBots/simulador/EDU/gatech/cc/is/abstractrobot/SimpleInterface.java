/*
 * SimpleInterface.java
 */

package EDU.gatech.cc.is.abstractrobot;

import EDU.cmu.cs.coral.util.TBDictionary;
import EDU.gatech.cc.is.util.Vec2;

import java.awt.*;


/**
 * Defines the basic capabilites all robot classes should provide.
 * A simple robot can detect obstacles, its position, and turn and move.
 * The intention is for this interface to be implemented for several
 * types of physical robots (e.g. Nomad 150s, Hummers, Dennings, etc.).
 * <p>
 * <B>Frames of reference</B><BR>
 * We use a standard cartesian coordinate system in
 * meters and radians.  Pretend you are looking down
 * on the robot: +x
 * goes out to your right (East), +y goes up (North).
 * When the robot is initialized, it is facing the +x direction.
 * Headings are given in radians, with East=0, North=PI/2 and so on CCW
 * around to 2*PI.
 * Some methods return "egocentric" vectors.
 * An egocentric vector is given relative to the center of
 * the robot in the same heading reference frame as global coordinates.
 * An object one meter east of the robot is at (1,0) egocentrically.
 * <p>
 * <B>Sensors</B><BR>
 * The Simple robot can sense it's location and obstacles.
 * <p>
 * <B>Timestamps</B><BR>
 * Many of the sensor and motor command
 * methods (e.g. get* and set*) require a timestamp as a parameter.
 * This is to help reduce the amount of I/O to the physical robot.
 * If the timestamp is less than or equal to the value sent on the
 * last call to one of these methods, old data is returned.
 * If the timestamp is -1 or greater than the last timestamp, the
 * robot is queried, and new data is returned.  The idea is
 * that during each control cycle the higher level software will
 * increment the timestamp and use it for all calls to these methods.
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1998 Tucker Balch
 *
 * @author Tucker Balch
 * @version $Revision: 1.2 $
 */

public interface SimpleInterface {
    /**
     * Quit the robot and release any resources it has reserved.
     */
    void quit();


    /**
     * Gets time elapsed since the robot was instantiated.  This
     * does not necessarily match real elapsed time since it
     * may run faster than that in simulation.  This is the
     * appropriate source of time for timestamps.
     */
    long getTime();


    /**
     * Get an array of Vec2s that point egocentrically from the
     * center of the robot to the obstacles currently sensed by the
     * robot's sensors
     *
     * @param timestamp only get new information
     *                  if timestamp > than last call or == -1.
     * @return the sensed obstacles.
     */
    Vec2[] getObstacles(long timestamp);


    /**
     * Set the maximum range at which a sensor reading should be considered
     * an obstacle.  Beyond this range, the readings are ignored.
     * The default range on startup is 1 meter.
     *
     * @param range the range in meters.
     */
    void setObstacleMaxRange(double range);


    /**
     * Get the position of the robot in global coordinates.
     *
     * @param timestamp only get new information
     *                  if timestamp > than last call or timestamp == -1.
     * @return the position.
     */
    Vec2 getPosition(long timestamp);

    /**
     * Get the unique ID of the robot (>=0).  This number
     * unique, but no other guarantees are made.  It is possible
     * for instance, for four robots to be numbered 8, 9, 11, 37.
     * To get sequential numbers for a team, use methods in the
     * KinSensor class.
     *
     * @param timestamp only get new information
     *                  if timestamp > than last call or timestamp == -1.
     * @return the id.
     */
    int getID(long timestamp);

    /**
     * Get the unique ID of the robot.
     *
     * @return the id.
     */
    int getID();

    /**
     * Set the unique ID of the robot.
     *
     * @param set the robot's ID.
     * @return the id.
     */
    void setID(int id);

    /**
     * Reset the odometry of the robot in global coordinates.
     * This might be done when reliable sensor information provides
     * a very good estimate of the robot's location.
     * Do this only if you are certain you're right!
     *
     * @param position the new position.
     * @see Simple#getPosition
     */
    void resetPosition(Vec2 position);


    /**
     * Get the current heading of the steering motor.
     *
     * @param timestamp only get new information
     *                  if timestamp > than last call or timestamp == -1.
     * @return the heading in radians.
     * @see Simple#setSteerHeading
     */
    double getSteerHeading(long timestamp);


    /**
     * Reset the steering odometry of the robot in global coordinates.
     * This might be done when reliable sensor information provides
     * a very good estimate of the robot's heading.
     * Do this only if you are certain you're right!
     *
     * @param heading the new heading in radians.
     * @see Simple#getSteerHeading
     * @see Simple#setSteerHeading
     */
    void resetSteerHeading(double heading);


    /**
     * Set the desired heading for the steering motor.
     *
     * @param heading   the heading in radians.
     * @param timestamp only get new information
     *                  if timestamp > than last call or timestamp == -1.
     * @see Simple#getSteerHeading
     */
    void setSteerHeading(long timestamp, double heading);


    /**
     * Set the desired speed for the robot (translation).
     * The speed must be between 0 and 1; where 0 is stopped
     * and 1.0 is "full blast".  It will be clipped
     * to whichever limit it exceeds.  Also, underlying
     * software will keep the actual speed at zero until the
     * steering motor is close to the desired heading.
     * Use setBaseSpeed to adjust the top speed.
     *
     * @param timestamp only get new information
     *                  if timestamp > than last call or timestamp == -1.
     * @param speed     the desired speed from 0 to 1.0, where 1.0 is the
     *                  base speed.
     * @see Simple#setSteerHeading
     * @see Simple#setBaseSpeed
     */
    void setSpeed(long timestamp, double speed);


    /**
     * Set the String that is printed on the robot's display.
     * For real robots, this is printed on the console.
     * For simulated robots, this appears printed below the agent
     * when view "Robot State" is selected.
     *
     * @param s String, the text to display.
     */
    void setDisplayString(String s);

    /**
     * Set the base speed for the robot (translation) in
     * meters per second.
     * Base speed is how fast the robot will move when
     * setSpeed(1.0) is called.
     * The speed must be between 0 and MAX_TRANSLATION.
     * It will be clipped to whichever limit it exceeds.
     *
     * @param speed the desired speed from 0 to 1.0, where 1.0 is the
     *              base speed.
     * @see Simple#setSpeed
     */
    void setBaseSpeed(double speed);

    /**
     * Gets the TBDictionary holding parameters defined using the
     * "dictionary" keyword in the dsc file.
     */
    TBDictionary getDictionary();

    /**
     * Sets the TBDictionary for the robots.
     *
     * @param tbd the dictionary to set it to
     */
    void setDictionary(TBDictionary tbd);

    /**
     * Gets the foreground color of the robot.
     */
    Color getForegroundColor();

    /**
     * Gets the background color of the robot.
     */
    Color getBackgroundColor();
}
