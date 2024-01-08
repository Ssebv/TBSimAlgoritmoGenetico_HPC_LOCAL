/*
 * ImpassableTerrainSim.java
 */

package EDU.cmu.cs.coral.simulation;

/**
 * a road for simulation.
 * <p>
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1998 Tucker Balch and Carnegie Mellon University
 *
 * @author Tucker Balch
 * @version $Revision: 1.1 $
 */

public class ImpassableTerrainSim extends TerrainSim {
    private final double TRAVERSABILITY = 0.0;

    public double getTraversability() {
        return (TRAVERSABILITY);
    }
}
