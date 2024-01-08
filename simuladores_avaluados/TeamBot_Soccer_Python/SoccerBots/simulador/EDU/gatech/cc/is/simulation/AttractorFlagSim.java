/*
 * AttractorFlagSim.java
 */

package EDU.gatech.cc.is.simulation;

import java.awt.*;


/**
 * A simple flag for capture the flac simulation.
 *
 * <p>
 * Copyright (c)2000 Tucker Balch
 *
 * @author Tucker Balch
 * @version $Revision: 1.2 $
 */

public class AttractorFlagSim extends AttractorSim {
    /**
     * Draw the attractor.
     */
    public void draw(Graphics g, int w, int h,
                     double t, double b, double l, double r) {
        top = t;
        bottom = b;
        left = l;
        right = r;

        if (!picked_up) {
            double meterspp = (r - l) / (double) w;
            if (DEBUG) System.out.println("meterspp " + meterspp);
            int radius = (int) (RADIUS / meterspp);
            int xpix = (int) ((position.x - l) / meterspp);
            int ypix = (int) ((double) h - ((position.y - b) / meterspp));
            if (DEBUG) System.out.println("robot at" +
                    " at " + xpix + "," + ypix);

            /*--- draw the main body ---*/
            g.setColor(foreground);
            g.fillRect(xpix, ypix - radius, (radius / 8),
                    radius * 2);
            g.fillRect(xpix, ypix - radius,
                    (radius / 2), (radius / 2));
            //g.fillOval(xpix - radius, ypix - radius,
            //	radius + radius, radius + radius);
        }
    }
}
