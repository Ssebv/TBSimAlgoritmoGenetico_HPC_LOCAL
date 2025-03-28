/*
 * b_Not_s.java
 */

package EDU.gatech.cc.is.clay;

/**
 * Provides the inverse of an embedded scalar node's value.
 * If the embedded node is not boolean (double or int), then
 * it is considered true if non-zero.
 * <p>
 * For detailed information on how to configure behaviors, see the
 * <A HREF="../clay/docs/index.html">Clay page</A>.
 * <p>
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1997, 1998 Tucker Balch
 *
 * @author Tucker Balch
 * @version $Revision: 1.1 $
 */


public class b_Not_s extends NodeBoolean {
    /**
     * Turn debug printing on or off.
     */
    public static final boolean DEBUG = Node.DEBUG;
    private final NodeScalar embedded1;
    boolean last_val = false;
    long lasttime = 0;

    /**
     * Instantiate a b_Not_s operator.
     *
     * @param im1 NodeScalar, the embedded perceptual schema that generates a value
     *            to be inverted.
     */
    public b_Not_s(NodeScalar im1) {
        if (DEBUG) System.out.println("b_Not_s: instantiated.");
        embedded1 = im1;
    }

    /**
     * Return a boolean representing the inverse of
     * the output of the embedded schema.
     *
     * @param timestamp long, only get new information if timestamp > than last call
     *                  or timestamp == -1.
     * @return the inverse, true or false.
     */
    public boolean Value(long timestamp) {
        if (DEBUG) System.out.println("b_Not_s: Value()");

        if ((timestamp > lasttime) || (timestamp == -1)) {
            /*--- reset the timestamp ---*/
            if (timestamp > 0) lasttime = timestamp;

            /*--- compute the difference ---*/
            last_val = embedded1.booleanValue(timestamp);
            last_val = !last_val;
        }

        return (last_val);
    }
}
