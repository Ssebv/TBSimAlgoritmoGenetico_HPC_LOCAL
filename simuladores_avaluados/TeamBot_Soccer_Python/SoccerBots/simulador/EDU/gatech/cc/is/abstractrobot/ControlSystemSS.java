/*
 * ControlSystemSS.java
 */

package EDU.gatech.cc.is.abstractrobot;

/**
 * This is the superclass for a SocSmall robot Control System.
 * When you create a contol system by extending this class,
 * it can run within JavaBotHard to control a real robot (maybe someday)
 * or JavaBotSim in simulation.
 * <p>
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1997, 1998 Tucker Balch
 *
 * @author Tucker Balch
 * @version $Revision: 1.1 $
 * @see Simple
 */

public class ControlSystemSS extends ControlSystemS {
    protected SocSmall abstract_robot;

    /**
     * Initialize the object.  Don't override this method, use
     * Configure instead.
     */
    public final void init(Simple ar, long s) {
        super.init(ar, s);
        abstract_robot = ((SocSmall) ar);
    }
}
