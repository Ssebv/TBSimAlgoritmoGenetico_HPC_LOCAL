/*
 * MultiCarry.java
 */

package EDU.gatech.cc.is.abstractrobot;

/**
 * The MultiCarry class provides an abstract interface to the
 * hardware of a robot that can carry several things.
 * <p>
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1997, 1998 Tucker Balch
 *
 * @author Tucker Balch
 * @version $Revision: 1.1 $
 */

public interface MultiCarry {
    /**
     * Get the number of objects we are carrying.
     *
     * @param timestamp only get new information
     *                  if timestamp > than last call or timestamp == -1.
     * @return number of objects.
     */
    int getNumObjectsCarrying(long timestamp);

    /**
     * Pick up the closest pickupable object.  Will fail
     * if no object in range, or if full capacity.
     *
     * @param timestamp only get new information
     *                  if timestamp > than last call or timestamp == -1.
     * @return true if success, false otherwise.
     */
    boolean pickup(long timestamp);

    /**
     * Drop one object, in LIFO order.
     *
     * @param timestamp only get new information
     *                  if timestamp > than last call or timestamp == -1.
     * @param position  the desired position from 0 to 1.
     * @return true if success, false otherwise.
     */
    void drop(long timestamp);
}
