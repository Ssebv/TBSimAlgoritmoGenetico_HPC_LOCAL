/*
 * b_NonZero_s.java
 */

package EDU.gatech.cc.is.clay;

/**
 * Return true if the value of embedded node is non-zero.
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


public class b_NonZero_s extends NodeBoolean {
    /**
     * Turn debug printing on or off.
     */
    public static final boolean DEBUG = Node.DEBUG;
    private final NodeScalar embedded1;
    boolean last_val = false;
    long lasttime = 0;

    /**
     * Instantiate a b_NonZero_s schema.
     *
     * @param im1 NodeScalar the embedded node that generates a scalar
     *            to be detected if non zero.
     */
    public b_NonZero_s(NodeScalar im1) {
        if (DEBUG) System.out.println("b_NonZero_s: instantiated.");
        embedded1 = im1;
    }

    /**
     * Return a boolean indicating if the embedded node
     * output is NonZero.
     *
     * @param timestamp long, only get new information if timestamp > than last call
     *                  or timestamp == -1.
     * @return true if NonZero, false if not.
     */
    public boolean Value(long timestamp) {
        if (DEBUG) System.out.println("b_NonZero_s: Value()");

        if ((timestamp > lasttime) || (timestamp == -1)) {
            /*--- reset the timestamp ---*/
            if (timestamp > 0) lasttime = timestamp;

            /*--- compute the output ---*/
            double tmp = embedded1.doubleValue(timestamp);
            last_val = tmp != 0;
        }

        return (last_val);
    }
}
