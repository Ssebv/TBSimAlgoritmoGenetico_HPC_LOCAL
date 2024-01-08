/*
 * TBSim.java
 */
package TBSim

import EDU.gatech.cc.`is`.util.DialogMessage
import EDU.gatech.cc.`is`.util.DialogMessageJoke
import EDU.gatech.cc.`is`.util.FilenameFilterByEnding
import EDU.gatech.cc.`is`.util.TBVersion
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.io.FilenameFilter
import java.net.InetAddress

/**
 * Application that runs a control system in
 * simulation.
 *
 *
 * To run this program, first ensure you have your CLASSPATH set correctly,
 * then type "java TBSim.TBSim".
 *
 *
 * For more detailed information, see the
 * <A HREF="docs/index.html">TBSim page</A>.
 *
 *
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1997 Georgia Tech Research Corporation
 *
 * @author Tucker Balch
 * @version $Revision: 1.3 $
 */
class TBSim @JvmOverloads constructor(
    file: String? = null, width: Int = 500, height: Int = 500,
    preserveSize: Boolean = false
) : Frame(), ActionListener, ItemListener {
    private val simFrame: Frame
    private val simulation: SimulationCanvas

    //private	CheckboxMenuItem view_graphics;
    private val robot_ids: CheckboxMenuItem
    private val robot_trails: CheckboxMenuItem
    private val robot_state: CheckboxMenuItem
    private val object_state: CheckboxMenuItem
    private val icons: CheckboxMenuItem
    private var dsc_file: String?
//    private val height = 0
//    private val width = 0
    private var current_directory: String? = null

//    constructor(file: String?) : this(file, 500, 500, false) {}
//    constructor(file: String?, width: Int, height: Int) : this(file, width, height, true) {}

    /**
     * Set up the frame and buttons.
     */
    init {
        simFrame = Frame("TBSim")

        /*--- Set the title ---*/
        // try to find our hostnam first
        val this_host: InetAddress?
        this_host = try {
            InetAddress.getLocalHost()
        } catch (e: Exception) {
            null
        }
        var host_name = "unknown host"
        if (this_host != null) host_name = this_host.hostName
        simFrame.title = "TBSim ($host_name)"

        /*--- Set up the menu bar ---*/
        val mb = MenuBar()
        val mf = Menu(fileMenuName)
        mf.add(loadCommandName)
        mf.add(printCommandName)
        mf.add(quitCommandName)
        mf.addActionListener(this)
        mb.add(mf)
        val mv = Menu(viewMenuName)
        //view_graphics = new CheckboxMenuItem(graphicsCommandName);
        //mv.add(view_graphics);
        //view_graphics.addItemListener(this);
        robot_ids = CheckboxMenuItem(robotIDsCommandName)
        mv.add(robot_ids)
        robot_ids.addItemListener(this)
        robot_trails = CheckboxMenuItem(trailsCommandName)
        mv.add(robot_trails)
        robot_trails.addItemListener(this)
        robot_state = CheckboxMenuItem(stateCommandName)
        mv.add(robot_state)
        robot_state.addItemListener(this)
        object_state = CheckboxMenuItem(infoCommandName)
        mv.add(object_state)
        object_state.addItemListener(this)
        icons = CheckboxMenuItem(iconsCommandName)
        mv.add(icons)
        icons.addItemListener(this)
        mb.add(mv)
        var hm = mb.helpMenu
        if (hm == null) {
            hm = Menu(helpMenuName)
            mb.helpMenu = hm
        }
        hm.add(statsCommandName)
        hm.add(aboutCommandName)
        hm.add(jokeCommandName)
        hm.addActionListener(this)
        simFrame.menuBar = mb

        /*--- Set up the buttons ---*/
        val button_area = Panel()
        val start_button = Button(resetCommandName)
        start_button.addActionListener(this)
        button_area.add(start_button)
        val resume_button = Button(startCommandName)
        resume_button.addActionListener(this)
        button_area.add(resume_button)
        val pause_button = Button(pauseCommandName)
        pause_button.addActionListener(this)
        button_area.add(pause_button)
        simFrame.add("North", button_area)

        /*--- Set up the Graphical Area ---*/
        val playing_field_panel = Panel()
        dsc_file = file
        simulation = SimulationCanvas(simFrame, width, height, dsc_file, preserveSize)
        playing_field_panel.add(simulation)
        simFrame.add("South", playing_field_panel)

        /*--- Pack Everything ---*/simFrame.pack()
        simFrame.isResizable = false

        /*--- tell the simulation to load and run ---*/if (dsc_file != null) simulation.reset()

        /*--- set the menu options we learned from a dsc file ---*/robot_ids.state = simulation.draw_ids
        robot_trails.state = simulation.draw_trails
        robot_state.state = simulation.draw_robot_state
        object_state.state = simulation.draw_object_state
        icons.state = simulation.draw_icons
        if (simulation.descriptionLoaded()) // only if loaded ok.
        {
            simulation.start()
        }
    }

    /**
     * Handle checkbox events
     */
    override fun itemStateChanged(e: ItemEvent) {
        val item = e.item.toString()

        //if (item == graphicsCommandName)
        //	{
        //	simulation.setGraphics(view_graphics.getState());
        //	}
        if (item === robotIDsCommandName) simulation.setDrawIDs(robot_ids.state)
        if (item === trailsCommandName) simulation.setDrawTrails(robot_trails.state)
        if (item === stateCommandName) {
            simulation.setDrawRobotState(robot_state.state)
        }
        if (item === infoCommandName) simulation.setDrawObjectState(object_state.state)
        if (item === iconsCommandName) simulation.setDrawIcons(icons.state)
    }

    /**
     * Handle button pushes.
     */
    override fun actionPerformed(e: ActionEvent) {
        val command = e.actionCommand

        /*--- Load ---*/if (command === loadCommandName) {
            val fd = FileDialog(
                simFrame,
                "Load New Description File",
                FileDialog.LOAD
            )

            /*--- try to filter based on extension ---*/
            // curently, this has no effect under linux and Irix
            val filt: FilenameFilter = FilenameFilterByEnding(descriptionFileSuffix)
            fd.filenameFilter = filt
            if (current_directory != null) fd.directory = current_directory
            fd.show()
            var tmpname = fd.file
            if (tmpname == null) tmpname = ""
            current_directory = fd.directory
            if (current_directory != null) tmpname = current_directory + tmpname
            if (tmpname != null) {
                dsc_file = tmpname
                simulation.load(dsc_file)
                //now update the checkboxes on the menu
                /*--- set the menu options we learned from a dsc file ---*/robot_ids.state = simulation.draw_ids
                robot_trails.state = simulation.draw_trails
                robot_state.state = simulation.draw_robot_state
                object_state.state = simulation.draw_object_state
                icons.state = simulation.draw_icons
            }
            fd.dispose()
        }

        /*--- Quit ---*/if (command === quitCommandName) {
            simulation.quit()
            System.exit(0)
        }

        /*--- Runtime Stats ---*/if (command === statsCommandName) {
            simulation.showRuntimeStats()
        } else if (command === aboutCommandName) {
            val tmp: Dialog = DialogMessage(
                simFrame,
                "About TBSim, the TeamBots Simulator",
                TBVersion.longReport() + "\n"
            )
        } else if (command === jokeCommandName) {
            val tmp: Dialog = DialogMessageJoke(
                simFrame,
                "Avoid Cursor",
                "An implementation of the avoid_cursor\nmotor schema."
            )
        } else if (command === robotIDsCommandName) {
            println(robot_ids.state)
        } else if (command === printCommandName) {
            val pjob: PrintJob? = simFrame.toolkit.getPrintJob(
                simFrame,
                "Print?", null
            )
            if (pjob != null) {
                val pg: Graphics = pjob.getGraphics()
                if (pg != null) simFrame.printAll(pg)
                pg.dispose()
            }
            pjob?.end()
        } else if (command === startCommandName) {
            simulation.start()
        } else if (command === resetCommandName) {
            simulation.reset()
        } else if (command === pauseCommandName) {
            simulation.pause()
        }
    }

    override fun show() {
        simFrame.isVisible = true
    }

    companion object {
        private const val fileMenuName = "File"

        // only used if there is no default menu
        private const val helpMenuName = "Help"
        private const val viewMenuName = "View"
        private const val loadCommandName = "Load"
        private const val printCommandName = "Print"
        private const val quitCommandName = "Quit"
        private const val graphicsCommandName = "Graphics"
        private const val robotIDsCommandName = "Robot IDs"
        private const val trailsCommandName = "robot trails"
        private const val stateCommandName = "robot state/potentials"
        private const val infoCommandName = "object info"
        private const val iconsCommandName = "icons"
        private const val resetCommandName = "reset/reload"
        private const val startCommandName = "start/resume"
        private const val pauseCommandName = "pause"
        private const val statsCommandName = "Runtime Stats"
        private const val aboutCommandName = "About"
        private const val jokeCommandName = "     "
        private const val descriptionFileSuffix = ".dsc"

        /**
         * Main for TBSim.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            val runtime: Long = 0
            var gotSize = false
            var width = -1
            var height = -1
            var dsc_file: String? = null

            /*--- check the arguments ---*/if (args.size >= 1) {
                if (args[0].equals("-version", ignoreCase = true)) {
                    println(
                        TBVersion.longReport()
                    )
                    System.exit(0)
                } else {
                    dsc_file = args[0]
                }
            }
            println(TBVersion.shortReport())
            try {
                if (args.size >= 3) {
                    width = args[1].toInt()
                    height = args[2].toInt()
                    gotSize = true
                }
            } catch (e: Exception) {
                println(
                    "usage: java TBSim.TBSim [-version] [descriptionfile] [width height]"
                )
            }
            /*--- make the window ---*/
            var jbs: TBSim? = null
            jbs = if (gotSize) TBSim(dsc_file, width, height) else dsc_file?.let { TBSim(it) } ?: TBSim()
            jbs.show()
        }
    }
}