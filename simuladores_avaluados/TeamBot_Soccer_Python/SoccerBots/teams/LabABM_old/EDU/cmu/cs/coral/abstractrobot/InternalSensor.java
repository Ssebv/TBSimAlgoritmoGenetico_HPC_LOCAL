/*
 * InternalSensor.java
 */

package EDU.cmu.cs.coral.abstractrobot;

/**
 * Provides an abstract interface to the the robot that
 * returns information about the robot's internal state
 *
 *
 * <p>
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1999,2000 CMU
 *
 * @author Rosemary Emery
 * @version $Revision: 1.1 $
 */

public interface InternalSensor {
    /**
     * returns the status of the current command.  if the robot
     * can execute the command it is true, otherwise it is false.
     * For example, if the Cye robot cannot move as told to because
     * it would turn under the trailer this function would return true
     */
    boolean getCommandError(long timestamp);

    /**
     * returns the robot's current voltage.
     */

    double getVoltage(long timestamp);
}
