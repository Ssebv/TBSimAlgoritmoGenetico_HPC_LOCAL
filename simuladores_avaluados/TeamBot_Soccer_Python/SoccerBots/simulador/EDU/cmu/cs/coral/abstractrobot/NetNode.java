/*
 * NetNode.java
 */

package EDU.cmu.cs.coral.abstractrobot;

import EDU.gatech.cc.is.abstractrobot.KinSensor;
import EDU.gatech.cc.is.abstractrobot.SimpleInterface;
import EDU.gatech.cc.is.communication.Transceiver;


/**
 * Provides an abstract interface to the hardware of
 * a basic Cye robot (no vision, gripper
 * or communication).
 *
 * <p>
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1999,2000 CMU
 *
 * @author Rosemary Emery
 * @version $Revision: 1.1 $
 */

public interface NetNode extends SimpleInterface, Transceiver, KinSensor {
    double RADIUS = 0.5;
}
