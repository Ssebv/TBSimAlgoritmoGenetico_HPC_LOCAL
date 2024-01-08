/*
 * SimulatedLinearObject.java
 */

package EDU.cmu.cs.coral.simulation;

import EDU.gatech.cc.is.simulation.SimulatedObject;

import java.awt.*;


/**
 * If you want to include a new linear object for TB simulation,
 * you must implement  this interface.
 * <p>
 * Most of these methods are used by other simulated objects to either
 * generate simulated sensor values or reproduce accurate dynamic results.
 * <p>
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1998 Tucker Balch and Carnegie Mellon University
 *
 * @author Tucker Balch
 * @version $Revision: 1.1 $
 */

public interface SimulatedLinearObject extends SimulatedObject {
    /**
     * Initialize a simulated object.  Called automatically by
     * TBSim.
     *
     * @param x1 x coord of first point
     * @param y1 y coord of first point
     * @param x2 x coord of second point
     * @param y2 y coord of second point
     * @param r  radius.
     * @param fg the foreground color of the object when drawn.
     * @param bg the background color of the object when drawn.
     * @param vc the vision class of the object - for use
     *           by simulated vision.
     * @param id a unique ID number fore the object.
     * @param s  random number seed.
     */
    void init(double x1, double y1,
              double x2, double y2, double r,
              Color fg, Color bg, int vc, int id, long s);
}

