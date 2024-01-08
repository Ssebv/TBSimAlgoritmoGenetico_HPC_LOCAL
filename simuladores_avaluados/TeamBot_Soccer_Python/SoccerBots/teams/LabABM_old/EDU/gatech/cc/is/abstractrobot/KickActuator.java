/*
 * KickActuator.java
 */

package EDU.gatech.cc.is.abstractrobot;

/**
 * Interface to a kicking actuator for a soccer robot.
 * <p>
 * <p>
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1997, 1998 Tucker R. Balch
 *
 * @author Tucker Balch
 * @version $Revision: 1.1 $
 * @see SocSmall
 */

public interface KickActuator {
    /**
     * Reveals whether or not the ball is in a position to be kicked.
     *
     * @param timestamp only get new information
     *                  if timestamp > than last call or timestamp == -1.
     * @return true if the ball can be kicked, false otherwise.
     */
    boolean canKick(long timestamp);

    /**
     * If the ball can be kicked, kick it.
     *
     * @param timestamp not used, but retained for compatibility.
     */
    void kick(long timestamp);
}
