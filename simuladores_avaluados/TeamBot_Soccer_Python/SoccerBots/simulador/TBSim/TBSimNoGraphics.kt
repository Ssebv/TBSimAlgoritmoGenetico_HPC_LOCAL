/*
 * TBSimNoGraphics.java
 */
package TBSim

import EDU.gatech.cc.`is`.util.TBVersion

/**
 * Application that runs a control system in simulation with no graphics.
 *
 *
 * To run this program, first ensure you have set your CLASSPATH correctly,
 * then type "java TBSim.TBSimNoGraphics".
 *
 *
 * For more detailed information, see the
 * <A HREF="docs/index.html">TBSim page</A>.
 *
 *
 * <A HREF="../EDU/cmu/cs/coral/COPYRIGHT.html">Copyright</A>
 * (c)1997, 1998 Tucker Balch and GTRC
 * (c)1998 Tucker Balch and Carnegie Mellon University
 *
 * @author Tucker Balch
 * @version $Revision: 1.1 $
 */
object TBSimNoGraphics {

    private val simulation: SimulationCanvas? = null
    private val dsc_file: String? = null

    /**
     * Main for TBSimNoGraphics.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        /*--- check the arguments ---*/
        if (args.size == 1) {
            if (args[0].equals("-version", ignoreCase = true)) {
                println(
                    TBVersion.longReport()
                )
                System.exit(0)
            } else {
                var dsc_file = args[0]

                /*--- tell the simulation to load and run ---*/println(
                    TBVersion.shortReport()
                )
                val simulation = SimulationCanvas(null, 0, 0, dsc_file)
                simulation.reset()
                if (simulation.descriptionLoaded()) // only if loaded ok.
                {
                    simulation.start()
                }
            }
        } else {
            println(
                "usage: TBSimNoGraphics [-version] descriptionfile"
            )
        }
    }
}