/*
 * CommN150Sim.java
 */

package EDU.gatech.cc.is.abstractrobot;

import EDU.gatech.cc.is.simulation.SimulatedObject;


/**
 * CommN150Sim implements CommN150 for simulation.
 * In addition to the basit capabilities provided by
 * a SimpleN150, CommN150 robots can communicate.
 * <p>
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1998 Tucker Balch
 *
 * @author Tucker Balch
 * @version $Revision: 1.1 $
 * @see CommN150
 */

public class CommN150Sim extends SimpleN150Sim
		implements CommN150, SimulatedObject {
	// SimpleN150Sim does all the work for us
}
