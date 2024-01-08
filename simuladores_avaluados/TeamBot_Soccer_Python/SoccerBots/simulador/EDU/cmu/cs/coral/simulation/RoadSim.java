/*
 * RoadSim.java
 */

package EDU.cmu.cs.coral.simulation;

/**
 * A road for simulation.
 * <p>
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1998 Tucker Balch and Carnegie Mellon University
 *
 * @author Tucker Balch
 * @version $Revision: 1.1 $
 */

public class RoadSim extends TerrainSim {
    private final double TRAVERSABILITY = 1.5;

    public double getTraversability() {
        return (TRAVERSABILITY);
    }
}
