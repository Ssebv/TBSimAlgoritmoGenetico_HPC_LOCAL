/* This code is part of the localize package of TeamBots.
 * Copyright (c) 1999, 2000 by John Sweeney and Carnegie Mellon University
 */
package EDU.cmu.cs.coral.localize;

import EDU.gatech.cc.is.simulation.SimulatedObject;

public interface LocalizationRobot {


    SimulatedObject[] getLandmarks();

    boolean[] getAmbigLandmarks();

    int getNumLandmarks();

    double getSeenLandmarkConfidence(int lm);

    //  public Vec2 getLandmarkLocation(int lm);

    //  public double getLandmarkRadius(int lm);

    double getLandmarkDistance(int lm);

    double getLandmarkDistanceVariance(int lm);

    double getLandmarkAngle(int lm);

    double getLandmarkAngleVariance(int lm);

    double[] getMovementDistParams();

    // public Range getMapRangeX();
    //  public Range getMapRangeY();
    //  public Range getMapRangeTheta();

    //   public int onLandmark(double x, double y);
    boolean onMap(double x, double y);

    void clipToMap(Sample s);

    double[] clipToMap(double x, double y, double theta);
}
