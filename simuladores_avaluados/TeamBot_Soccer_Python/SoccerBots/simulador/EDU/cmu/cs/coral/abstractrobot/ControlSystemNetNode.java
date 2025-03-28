/*
 * ControlSystemNetNode.java
 */

package EDU.cmu.cs.coral.abstractrobot;

import EDU.gatech.cc.is.abstractrobot.ControlSystemS;
import EDU.gatech.cc.is.abstractrobot.Simple;

/**
 * This is the superclass for a NetNode robot Control System.
 * When you create a contol system by extending this class.
 * <p>
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1999, 2000 CMU
 *
 * @author Rosemary Emery
 * @version $Revision: 1.1 $
 * @see Simple
 */

public class ControlSystemNetNode extends ControlSystemS {
    protected NetNode abstract_robot;

    /**
     * Initialize the object.  Don't override this method, use
     * Configure instead.
     */
    public final void init(Simple ar, long s) {
        super.init(ar, s);
        abstract_robot = ((NetNode) ar);
    }
}
