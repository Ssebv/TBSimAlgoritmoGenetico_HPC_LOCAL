/*
 * SimpleCye.java
 */

package EDU.cmu.cs.coral.abstractrobot;

import EDU.gatech.cc.is.abstractrobot.SimpleInterface;
import EDU.gatech.cc.is.abstractrobot.VisualObjectSensor;
import EDU.gatech.cc.is.util.Units;


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
 * @version $Revision: 1.7 $
 */

public interface SimpleCye extends SimpleInterface,
        InternalSensor, VisualObjectSensor {
    double MAX_TRANSLATION = 0.9144; // maximum speed is 3ft/sec
    double MAX_STEER = 0.7854;
    double RADIUS = 0.445; // over exaggerate
    double WIDTH = 2 * 0.115; // width is 23 cm
    double LENGTH = 0.4127; // length is 40.6 cm
    double WHEEL_RADIUS = 0.1285 / 2.0; // in actual fact is smaller than this
    double TRAILER_LENGTH = 0.445; // this is from hitch to end
    double TRAILER_WIDTH = 0.386; // this is a guesstimation
    double TRAILER_FRONT = 0.115; // from hitch to front
    double HITCH_TO_TRAILER_WHEEL = 0.445 - 0.115; //estimation for now

    // some useful numbers
    double SPORT_HITCH_TO_TRAILER_WHEEL = 0.29;
    double VISION_RANGE = 3.0;
    int VISION_FOV_DEG = 100;
    double VISION_FOV_RAD = Units.DegToRad(100);


    /**
     * How far sonar ring is from center of robot.
     */
    double SONAR_RADIUS = 0.23;


}
