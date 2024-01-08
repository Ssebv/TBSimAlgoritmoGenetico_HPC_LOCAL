/*
 * b_Equal_i.java
 */

package EDU.gatech.cc.is.clay;

/**
 * Return true if the value of embedded node equals a set integer value.
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


public class b_Equal_i extends NodeBoolean {
    /**
     * Turn debug printing on or off.
     */
    public static final boolean DEBUG = Node.DEBUG;
    private final NodeInt embedded1;
    private final int target;
    boolean last_val = false;
    long lasttime = 0;

    /**
     * Instantiate a b_Equal_i schema.
     *
     * @param t   int, the target value.
     * @param im1 NodeInt, the embedded node that generates an int
     *            to be detected if equal to target.
     */
    public b_Equal_i(int t, NodeInt im1) {
        if (DEBUG) System.out.println("b_Equal_i: instantiated.");
        target = t;
        embedded1 = im1;
    }

    /**
     * Return a boolean indicating if the embedded schema
     * output is equal to a desired value.
     *
     * @param timestamp long, only get new information if timestamp > than last call
     *                  or timestamp == -1.
     * @return true if equal, false if not.
     */
    public boolean Value(long timestamp) {
        if (DEBUG) System.out.println("b_Equal_i: Value()");

        if ((timestamp > lasttime) || (timestamp == -1)) {
            /*--- reset the timestamp ---*/
            if (timestamp > 0) lasttime = timestamp;

            /*--- compute the output ---*/
            int tmp = embedded1.Value(timestamp);
            last_val = tmp == target;
        }

        return (last_val);
    }
}
