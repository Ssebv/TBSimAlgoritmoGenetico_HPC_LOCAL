/*
 * i_FSA_ba.java
 */

package EDU.gatech.cc.is.clay;

/**
 * A Finite State Automoton that generates an integer output.
 * While in the current state, if trigger[current_state][edge].Value()
 * is true, the new state is follow_on[current_state][edge].
 * You must fill in the values of these arrays yourself.  It is
 * best to look at an example.
 * <p>
 * This node may be used to implement "Temporal Sequencing" as
 * developed in the Mobile Robot Lab at Georgia Tech.
 * <p>
 * The source code in this module is based on "first principles"
 * (e.g. published papers) and is not derived from any previously
 * existing software.
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


public class i_FSA_ba extends NodeInt {
    /**
     * Maximum number of states.
     */
    public static final int MAX_STATES = 50;
    private final int count = 0;
    /**
     * The initial state.  Default is 0.
     */
    public int state = 0;
    /**
     * The triggers that lead to new states.  Indexed as follows:
     * trigger[current_state][edge].
     */
    public NodeScalar[][] triggers = new NodeScalar[MAX_STATES][MAX_STATES];
    /**
     * The follow on states that triggers lead to.  Indexed as follows:
     * follow_on[current_state][edge]
     */
    public int[][] follow_on = new int[MAX_STATES][MAX_STATES];
    private long lasttime = -1;
    private int lastval;

    /**
     * Instantiate an FSA.  Configuration is by setting
     * trigger and follow_on arrays.
     */
    public i_FSA_ba() {
    }

    /**
     * Get the value of the node.
     *
     * @param timestamp long, the time of the request.
     */
    public int Value(long timestamp) {
        if ((timestamp != lasttime) || (timestamp == -1)) {
            if (timestamp != -1) lasttime = timestamp;
            int i = 0;
            while (triggers[state][i] != null) {
                if (triggers[state][i].booleanValue(timestamp)) {
                    state = follow_on[state][i];
                    break;
                }
                i++;
            }
        }
        return state;
    }
}
