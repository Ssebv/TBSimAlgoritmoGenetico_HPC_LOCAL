/* This code is part of the localize package of TeamBots.
 * Copyright (c) 1999, 2000 by John Sweeney and Carnegie Mellon University
 */

package EDU.cmu.cs.coral.localize;

import EDU.cmu.cs.coral.simulation.LineSim;
import EDU.gatech.cc.is.util.Vec2;


public interface LineLocalizationRobot extends LocalizationRobot {

  LineSim[] getLines();


  Vec2[] getVisualLines(long timestamp, int channel);
}


