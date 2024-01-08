/*
 * RescueVan.java
 */

package EDU.gatech.cc.is.abstractrobot;

import EDU.gatech.cc.is.communication.Transceiver;
import EDU.gatech.cc.is.util.Units;


/**
 * Provides an abstract interface to the hardware of
 * a rescue van.
 */

public interface RescueVan extends SimpleInterface,
        VisualObjectSensor, MultiCarry, KinSensor, Transceiver {
    // some useful numbers
    double VISION_RANGE = 1000;
    int VISION_FOV_DEG = 100;
    double VISION_FOV_RAD = Units.DegToRad(100);
    double PICKUP_CAPTURE_RADIUS = 200; // 0.2 km
    int MAX_CAPACITY = 1000; // 10 people
    double MAX_TRANSLATION = 44; // m/s = 100mph
    double MAX_STEER = 0.7854;
    double RADIUS = 2.5;
}
