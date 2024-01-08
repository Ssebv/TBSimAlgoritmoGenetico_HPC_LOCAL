/*
 * SimulationCanvas.java
 */
package TBSim

import EDU.cmu.cs.coral.simulation.SimulatedLinearObject
import EDU.cmu.cs.coral.util.PreProcessor
import EDU.cmu.cs.coral.util.TBDictionary
import EDU.gatech.cc.`is`.abstractrobot.ControlSystemS
import EDU.gatech.cc.`is`.abstractrobot.Simple
import EDU.gatech.cc.`is`.abstractrobot.VisualObjectSensor
import EDU.gatech.cc.`is`.simulation.SimulatedObject
import EDU.gatech.cc.`is`.util.DialogMessage
import java.awt.*
import java.io.*

/**
 * Used within TBSim to control and draw a simulation.
 *
 *
 * For more detailed information, see the
 * <A HREF="docs/index.html">TBSim page</A>.
 *
 *
 * <A HREF="../EDU/cmu/cs/coral/COPYRIGHT.html">Copyright</A>
 * (c)1997 Tucker Balch and Georgia Tech Research Corporation
 * (c)1998 Tucker Balch and Carnegie Mellon University
 *
 * @author Tucker Balch
 * @version $Revision: 1.11 $
 */
class SimulationCanvas(
    p: Frame?, w: Int, h: Int,
    dscfile: String?, preserveSize: Boolean
) : Canvas(), Runnable {
    private val parent: Frame?
    private val run_sim_thread: Thread

    /*make these package scope so TBSim can access for updating menu on startup*/
    var draw_ids = false //don't draw robot ids
    var draw_icons = false //don't draw robot icons
    var draw_robot_state = false //don't draw robot state
    var draw_object_state = false //don't draw robot state
    var draw_trails = false //don't draw object trails
    private val g: Graphics? = null
    private var height2 = 0
    private var width2 = 0
    private var preserveSize = false
    private var bgcolor = Color(0xFFFFFF)
    private var bgimage: Image? = null
    private var buffer: Image? = null
    private var bufferg: Graphics? = null
    private var read_once = false //indicates if we've read a dsc file
    private var pause = true
    private var graphics_on = true
    private var simulated_objects = arrayOfNulls<SimulatedObject>(0)
    private var control_systems // = new ControlSystemS[0];
            : Array<ControlSystemS?>
    private var top = 0.0
    private var bottom = 0.0
    private var left = 0.0
    private var right = 0.0
    private var time_compression = 1.0
    private var current_time: Long = 0
    private var sim_time: Long = 0
    private var timestep: Long = 100
    private var timeout: Long = -1
    private var seed: Long = -1
    private var trials = -1
    private var descriptionfile: String?
    private var idcounter = 0
    private var to_draw = false

    /*end package scope*/
    private var visionNoiseMean = 0.0
    private var visionNoiseStddev //the standard deviation for vision noise
            = 0.0
    private var visionNoiseSeed //the seed value
            : Long = 0
    private var startrun: Long = 0
    private var frames: Long = 0
    private var description_file_loaded = false
    private var keep_running = true

    constructor(
        p: Frame?, w: Int, h: Int,
        dscfile: String?
    ) : this(p, w, h, dscfile, true) {
        visionNoiseStddev = 0.0 //default is no noise
        visionNoiseSeed = 31337 //default noise seed
    }

    /**
     * Set up the SimulationCanvas.
     */
    init {
        if (p == null) {
            graphics_on = false
            pause = false
        } else {
            graphics_on = true
            pause = true
        }
        parent = p
        simulated_objects = arrayOfNulls(0)
        control_systems = arrayOfNulls(0)
        this.preserveSize = preserveSize
        descriptionfile = dscfile
        if (graphics_on) {
            setSize(w, h)
            background = Color.white
        }


        /*--- instantitate thread ---*/run_sim_thread = Thread(this)
        run_sim_thread.start()
    }

    /**
     * Read the description of the world from a file.
     */
    @Throws(IOException::class)
    private fun loadEnvironmentDescription() {
        val file = FileReader(descriptionfile)
        val raw_in: Reader = PreProcessor(file)
        val `in` = StreamTokenizer(raw_in)
        var token: String
        val temp_objs = arrayOfNulls<SimulatedObject>(MAX_SIM_OBJS)
        var temp_objs_count = 0
        val temp_css = arrayOfNulls<ControlSystemS>(MAX_SIM_OBJS)
        var temp_css_count = 0
        var x: Double
        var y: Double
        var t: Double
        var r: Double
        var x1: Double
        var y1: Double
        var x2: Double
        var y2: Double
        var color1: Int
        var color2: Int
        var vc: Int
        idcounter = 0
        var string1: String
        var string2: String
        val bboard = TBDictionary()
        /*--- assume success. reset later if failure ---*/
        var dfl = true //description_file_loaded;

        /*--- set default bounds before reading ---*/top = 5.0
        bottom = -5.0
        left = -5.0
        right = 5.0

        /*--- set up tokenizer ---*/`in`.wordChars('A'.code, '_'.code) // let _ be a word character
        `in`.quoteChar('"'.code) // " is the quote char

        /*--- tokenize the file ---*/token = "beginning of file"
        try {
            while (`in`.nextToken() != StreamTokenizer.TT_EOF) {
                if (`in`.ttype == StreamTokenizer.TT_WORD) {
                    token = `in`.sval
                    if (false) println(token)

                    /*--- check for "dictionary" statements ---*/
                    //FORMAT: dictionary KEY "some string"
                    if (token.equals("dictionary", ignoreCase = true)) {
                        var key: String
                        var obj: String
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_WORD
                        ) {
                            key = `in`.sval
                        } else {
                            token = `in`.sval
                            throw IOException()
                        }
                        `in`.nextToken()
                        obj = `in`.sval
                        bboard[key] = obj
                    }

                    /*--- this affects the vision sensor noise ---*/
                    //FORMAT: vision_noise MEAN STDDEV SEED
                    if (token.equals("vision_noise", ignoreCase = true)) {
                        //the next token is the value for the mean
                        //and should be a double
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) {
                            visionNoiseMean = `in`.nval
                        } else {
                            //we are looking for number, not string
                            token = `in`.sval
                            throw IOException()
                        }

                        //this is the stddev
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) {
                            visionNoiseStddev = `in`.nval
                        } else {
                            token = `in`.sval
                            throw IOException()
                        }

                        //the next one is a long for the seed value
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) {
                            visionNoiseSeed = `in`.nval.toLong()
                        } else {
                            //not what we wanted!
                            token = `in`.sval
                            throw IOException()
                        }
                    }

                    /*--- it is to turn trails on/off ---*/
                    //FORMAT: view_robot_trails on
                    if (token.equals("view_robot_trails", ignoreCase = true)) {
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_NUMBER
                        ) {
                            token = `in`.sval // for error report
                            throw IOException()
                        } else {
                            if (`in`.sval.equals("on", ignoreCase = true)) draw_trails = true
                        }
                    }

                    /*--- it is to turn IDs on/off ---*/
                    //FORMAT: view_robot_IDs on
                    if (token.equals("view_robot_IDs", ignoreCase = true)) {
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_NUMBER
                        ) {
                            token = `in`.sval // for error report
                            throw IOException()
                        } else {
                            if (`in`.sval.equals("on", ignoreCase = true)) draw_ids = true
                        }
                    }

                    /*--- it is to turn robot state on/off ---*/
                    //FORMAT: view_robot_state on
                    if (token.equals("view_robot_state", ignoreCase = true)) {
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_NUMBER
                        ) {
                            token = `in`.sval // for error report
                            throw IOException()
                        } else {
                            if (`in`.sval.equals("on", ignoreCase = true)) draw_robot_state = true
                        }
                    }


                    /*--- it is to turn objec info IDs on/off ---*/
                    //FORMAT: view_object_into on
                    if (token.equals("view_object_info", ignoreCase = true)) {
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_NUMBER
                        ) {
                            token = `in`.sval // for error report
                            throw IOException()
                        } else {
                            if (`in`.sval.equals("on", ignoreCase = true)) draw_object_state = true
                        }
                    }

                    /*--- it is to turn icons on/off ---*/
                    //FORMAT: view_icons on
                    if (token.equals("view_icons", ignoreCase = true)) {
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_NUMBER
                        ) {
                            token = `in`.sval // for error report
                            throw IOException()
                        } else {
                            if (`in`.sval.equals("on", ignoreCase = true)) draw_icons = true
                        }
                    }


                    /*--- it is a background_image statement ---*/
                    //FORMAT: background_image filename
                    if (token.equals("background_image", ignoreCase = true)) {
                        `in`.nextToken() // get the filename
                        val img_filename = `in`.sval
                        println(
                            "loading "
                                    + "background image file " +
                                    img_filename
                        )
                        val tk = Toolkit.getDefaultToolkit()
                        bgimage = tk.getImage(img_filename)
                        tk.prepareImage(bgimage, -1, -1, this)
                    }

                    /*--- it is a background statement ---*/
                    //FORMAT: background color
                    if (token.equals("background", ignoreCase = true)) {
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_WORD
                        ) {
                            var tmp = `in`.sval
                            tmp = tmp.replace('x', '0')
                            tmp = tmp.replace('X', '0')
                            bgcolor = Color(tmp.toInt(16))
                        } else {
                            bgcolor = Color(`in`.nval.toInt())
                        }
                    }

                    /*--- it is a time statement ---*/
                    //FORMAT: time accel_rate
                    if (token.equals("time", ignoreCase = true)) {
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_NUMBER
                        ) {
                            time_compression = `in`.nval
                        } else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                    }

                    /*--- it is a timeout statement ---*/
                    //FORMAT: timeout time
                    if (token.equals("timeout", ignoreCase = true)) {
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_NUMBER
                        ) {
                            timeout = `in`.nval.toLong()
                        } else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                    }

                    /*--- it is a seed statement ---*/
                    //FORMAT: seed seed_val
                    if (token.equals("seed", ignoreCase = true)) {
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_NUMBER
                        ) {
                            // skip for subsequent trials
                            if (!read_once) seed = `in`.nval.toLong()
                        } else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                    }

                    /*--- it is a graphics statement ---*/
                    //FORMAT: graphics on/off
                    if (token.equals("graphics", ignoreCase = true)) {
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_NUMBER
                        ) {
                            token = `in`.sval // for error report
                            throw IOException()
                        } else {
                            if (`in`.sval.equals("off", ignoreCase = true)) graphics_on = false
                        }
                    }

                    /*--- it is a trials statement ---*/
                    //FORMAT: trials num_trials
                    if (token.equals("trials", ignoreCase = true)) {
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_NUMBER
                        ) {
                            if (trials == -1) trials = `in`.nval.toInt()
                            if (trials < 0) throw IOException()
                        } else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                    }

                    /*--- it is a maxtimestep statement ---*/
                    //FORMAT: maxtimestep milliseconds
                    //DEPRECATED!
                    if (token.equals("maxtimestep", ignoreCase = true)) {
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_NUMBER
                        ) {
                            timestep = `in`.nval.toLong()
                            println("maxtimestep statement read, treated as timestep")
                        } else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                    }

                    /*--- it is a timestep statement ---*/
                    //FORMAT: timestep milliseconds
                    //DEPRECATED!
                    if (token.equals("timestep", ignoreCase = true)) {
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_NUMBER
                        ) {
                            timestep = `in`.nval.toLong()
                        } else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                    }

                    /*--- it is a bounds statement ---*/
                    //FORMAT: bounds left right bottom top
                    if (token.equals("bounds", ignoreCase = true)) {
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) left = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) right = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) bottom = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) top = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                    }

                    /*--- it is a windowsize statement ---*/
                    //FORMAT: windowsize width height
                    if (token.equals("windowsize", ignoreCase = true)) {
                        var localWidth = width2
                        var localHeight = height2
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) localWidth = `in`.nval.toInt() else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) localHeight = `in`.nval.toInt() else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (!preserveSize) {
                            setSize(localWidth, localHeight)
                            reSizeWindow()
                        }
                    }

                    /*--- it is an object statement ---*/
                    //FORMAT object objectclass
                    //	x y t r color1 color2 visionclass
                    if (token.equals("object", ignoreCase = true)) {
                        if (`in`.nextToken() == StreamTokenizer.TT_WORD) string1 = `in`.sval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) x = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) y = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) t = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) r = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_WORD
                        ) {
                            var tmp = `in`.sval
                            tmp = tmp.replace('x', '0')
                            tmp = tmp.replace('X', '0')
                            color1 = tmp.toInt(16)
                        } else {
                            color1 = `in`.nval.toInt()
                        }
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_WORD
                        ) {
                            var tmp = `in`.sval
                            tmp = tmp.replace('x', '0')
                            tmp = tmp.replace('X', '0')
                            color2 = tmp.toInt(16)
                        } else {
                            color2 = `in`.nval.toInt()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) vc = `in`.nval.toInt() else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        /*--- instantiate the obj ---*/token = string1 // in case of error
                        val rclass = Class.forName(string1)
                        val obj = rclass.newInstance() as SimulatedObject
                        obj.init(
                            x, y, t, r, Color(color1),
                            Color(color2), vc,
                            idcounter++, seed++
                        )
                        temp_objs[temp_objs_count++] = obj
                    }


                    /*--- it is a linearobject statement ---*/
                    //FORMAT linearobject objectclass
                    //	x1 y1 x2 y2 r color1 color2 visionclass
                    if (token.equals("linearobject", ignoreCase = true)) {
                        if (`in`.nextToken() == StreamTokenizer.TT_WORD) string1 = `in`.sval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) x1 = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) y1 = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) x2 = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) y2 = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) r = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_WORD
                        ) {
                            var tmp = `in`.sval
                            tmp = tmp.replace('x', '0')
                            tmp = tmp.replace('X', '0')
                            color1 = tmp.toInt(16)
                        } else {
                            color1 = `in`.nval.toInt()
                        }
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_WORD
                        ) {
                            var tmp = `in`.sval
                            tmp = tmp.replace('x', '0')
                            tmp = tmp.replace('X', '0')
                            color2 = tmp.toInt(16)
                        } else {
                            color2 = `in`.nval.toInt()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) vc = `in`.nval.toInt() else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        /*--- instantiate the obj ---*/token = string1 // in case of error
                        println(string1)
                        val rclass = Class.forName(string1)
                        val obj = rclass.newInstance() as SimulatedLinearObject
                        obj.init(
                            x1, y1, x2, y2, r, Color(color1),
                            Color(color2), vc,
                            idcounter++, seed++
                        )
                        temp_objs[temp_objs_count++] = obj
                    }


                    /*--- it is a robot statement ---*/
                    //FORMAT robot robotclass controlsystemclass
                    //	x y t color1 color2 visionclass
                    if (token.equals("robot", ignoreCase = true)) {
                        if (`in`.nextToken() == StreamTokenizer.TT_WORD) string1 = `in`.sval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_WORD) string2 = `in`.sval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) x = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) y = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) t = `in`.nval else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_WORD
                        ) {
                            var tmp = `in`.sval
                            tmp = tmp.replace('x', '0')
                            tmp = tmp.replace('X', '0')
                            color1 = tmp.toInt(16)
                        } else {
                            color1 = `in`.nval.toInt()
                        }
                        if (`in`.nextToken() ==
                            StreamTokenizer.TT_WORD
                        ) {
                            var tmp = `in`.sval
                            tmp = tmp.replace('x', '0')
                            tmp = tmp.replace('X', '0')
                            color2 = tmp.toInt(16)
                        } else {
                            color2 = `in`.nval.toInt()
                        }
                        if (`in`.nextToken() == StreamTokenizer.TT_NUMBER) vc = `in`.nval.toInt() else {
                            token = `in`.sval // for error report
                            throw IOException()
                        }

                        /*--- the robot ---*/token = string1 // in case of error
                        val rclass = Class.forName(string1)
                        val obj = rclass.newInstance() as SimulatedObject
                        obj.init(
                            x, y, t, 0.0, Color(color1),
                            Color(color2), vc,
                            idcounter++, seed++
                        )
                        temp_objs[temp_objs_count++] = obj

                        /*--- set the dictionary ---*/(obj as Simple).dictionary = bboard

                        /*--- the control system ---*/token = string2 // in case of error
                        val csclass = Class.forName(string2)
                        val css = csclass.newInstance() as ControlSystemS
                        css.init(obj as Simple, seed++)
                        //css.Configure();//save for later
                        temp_css[temp_css_count++] = css
                    }
                } else {
                    throw IOException()
                }
                file.close()
                raw_in.close()
            }

            /*--- catch any exceptions thrown in the parsing ---*/
        } catch (e: IOException) {
            dfl = false
            simulated_objects = arrayOfNulls(0)
            val msg = "bad format" +
                    " at line " + `in`.lineno() +
                    " in " + descriptionfile +
                    " near " +
                    "'" + token + "'"
            val tmp: Dialog
            if (graphics_on) tmp = DialogMessage(
                parent,
                "TBSim Error", msg
            ) else println(msg)
            descriptionfile = null
        } catch (e: ClassNotFoundException) {
            dfl = false
            simulated_objects = arrayOfNulls(0)
            val msg = """unable to find class '$token' at line ${`in`.lineno()} in $descriptionfile.
You may need to check your CLASSPATH."""
            val tmp: Dialog
            if (graphics_on) tmp = DialogMessage(
                parent,
                "TBSim Error", msg
            ) else println(msg)
            descriptionfile = null
        } catch (e: IllegalAccessException) {
            dfl = false
            simulated_objects = arrayOfNulls(0)
            val msg = "illegal to access class " +
                    "'" + token + "'" +
                    " at line " + `in`.lineno() +
                    " in " + descriptionfile
            val tmp: Dialog
            if (graphics_on) tmp = DialogMessage(
                parent,
                "TBSim Error", msg
            ) else println(msg)
            descriptionfile = null
        } catch (e: InstantiationException) {
            dfl = false
            simulated_objects = arrayOfNulls(0)
            val msg = "instantiation error for " +
                    "'" + token + "'" +
                    " at line " + `in`.lineno() +
                    " in " + descriptionfile
            val tmp: Dialog
            if (graphics_on) tmp = DialogMessage(
                parent,
                "TBSim Error", msg
            ) else println(msg)
            descriptionfile = null
        } catch (e: ClassCastException) {
            dfl = false
            simulated_objects = arrayOfNulls(0)
            val msg = "class conflict for " +
                    "'" + token + "'" +
                    " at line " + `in`.lineno() +
                    " in " + descriptionfile + "." +
                    " It could be that the control system was not " +
                    " written for the type of robot you " +
                    " specified."
            val tmp: Dialog
            if (graphics_on) tmp = DialogMessage(
                parent,
                "TBSim Error", msg
            ) else println(msg)
            descriptionfile = null
        }


        /*--- set up global arrays of objs and cont systems ---*/simulated_objects = arrayOfNulls(temp_objs_count)
        System.arraycopy(temp_objs, 0, simulated_objects, 0, temp_objs_count)
        for (i in 0 until temp_objs_count) {
            // let everyone take a step to update their pointers
            simulated_objects[i]!!.takeStep(0, simulated_objects)
            if (simulated_objects[i] is VisualObjectSensor) {
                //we need to tell it the noise parameters...
                //we do it here so that it doesnt matter where in dsc they
                //declare visionnoise
                (simulated_objects[i] as VisualObjectSensor?)!!.setVisionNoise(
                    visionNoiseMean,
                    visionNoiseStddev,
                    visionNoiseSeed
                )
            }
        }
        control_systems = arrayOfNulls(temp_css_count)
        for (i in 0 until temp_css_count) {
            control_systems[i] = temp_css[i]
            control_systems[i]!!.configure()
        }
        description_file_loaded = dfl
        read_once = true
    }

    /**
     * Provide info about whether we have successufully
     * loaded the file.
     *
     * @return true if a file is loaded, false otherwise.
     */
    fun descriptionLoaded(): Boolean {
        return description_file_loaded
    }

    /**
     * Run the simulation.
     */
    override fun run() {
        //pause = true;
        val start_time = System.currentTimeMillis()
        var sim_timestep: Long = 0
        var robots_done = false
        while (keep_running) {
            while (pause || !description_file_loaded) {
                if (graphics_on) this.repaint()
                try {
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                }
            }
            current_time = System.currentTimeMillis()
            sim_timestep = timestep

            //--- deprecated
            //sim_timestep = (long)(
            //(double)(current_time - last_time)*
            //time_compression);
            //if (sim_timestep>maxtimestep)
            //sim_timestep = maxtimestep;

            /*--- run control systems and check for done ---*/robots_done = true
            for (i in control_systems.indices) {
                val stat = control_systems[i]!!.takeStep()
                if (stat != ControlSystemS.CSSTAT_DONE) robots_done = false
            }

            /*--- run the physics ---*/for (i in simulated_objects.indices) simulated_objects[i]!!.takeStep(
                sim_timestep,
                simulated_objects
            )

            /*--- draw everything ---*/to_draw = true
            if (graphics_on) this.repaint()
            if (to_draw && graphics_on) {
                try {
                    Thread.sleep(10)
                } catch (e: InterruptedException) {
                }
            }

            /*--- garbage collect every time ---*/
            // this is to make cycle times more homogeneous
            //System.gc();  // too slow!

            /*--- count frames ---*/frames++ // for statistics gathering

            /*--- check for timeout or done ---*/if (timeout > 0 && sim_time >= timeout
                || robots_done
            ) {
                if (trials <= 1) {
                    for (i in control_systems.indices) {
                        control_systems[i]!!.trialEnd()
                        control_systems[i]!!.quit()
                    }
                    keep_running = false
                    if (!graphics_on) showRuntimeStats()
                    System.exit(0)
                } else {
                    for (i in control_systems.indices) {
                        control_systems[i]!!.trialEnd()
                    }
                    trials--
                    sim_time = 0
                    reset()
                    start()
                }
            }

            /*--- increment simulation time ---*/sim_time += sim_timestep
        }
    }

    /**
     * Handle a drawing request.
     */
    @Synchronized
    override fun update(g: Graphics) {
        if (bufferg != null && graphics_on) {
            /*--- if no bgimage, draw bgcolor ---*/
            if (bgimage == null) {
                bufferg!!.color = bgcolor
                bufferg!!.fillRect(0, 0, width2, height2)
            }

            /*--- draw the background image first ---*/if (bgimage != null) bufferg!!.drawImage(bgimage, 0, 0, this)

            /*--- draw robot trails first ---*/for (i in simulated_objects.indices) {
                // if robot
                if (simulated_objects[i] is Simple) {
                    // draw trail
                    if (draw_trails) {
                        simulated_objects[i]?.drawTrail(
                            bufferg,
                            width2, height2,
                            top, bottom, left, right
                        )
                    }
                }
            }

            /*--- draw IDs and state ---*/for (i in simulated_objects.indices) {
                // if robot
                if (simulated_objects[i] is Simple) {
                    if (draw_ids) {
                        simulated_objects[i]?.drawID(
                            bufferg,
                            width2, height2,
                            top, bottom, left, right
                        )
                    }
                    if (draw_robot_state) {
                        simulated_objects[i]?.drawState(
                            bufferg,
                            width2, height2,
                            top, bottom, left, right
                        )
                    }
                }

                /*--- draw the object ---*/if (draw_icons) simulated_objects[i]!!.drawIcon(
                    bufferg, width2, height2,
                    top, bottom, left, right
                ) else simulated_objects[i]!!.draw(
                    bufferg, width2, height2,
                    top, bottom, left, right
                )

                /*--- draw object state ---*/
                // if not a robot
                if (simulated_objects[i] !is Simple) {
                    if (draw_object_state) {
                        simulated_objects[i]!!.drawState(
                            bufferg,
                            width2, height2,
                            top, bottom, left, right
                        )
                    }
                }
            }
            g.drawImage(buffer, 0, 0, this)
        }
        to_draw = false
    }

    /**
     * Resize the SimulationCanvas.
     */
    override fun setSize(w: Int, h: Int) {
        width2 = w
        height2 = h
        super.setSize(width2, height2)
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(width2, height2)
    }

    fun reSizeWindow() {
        invalidate()
        var parent = getParent()
        while (parent.parent != null) {
            parent = parent.parent
        }
        parent.size = parent.preferredSize
        parent.validate()
    }

    /**
     * Handle a quit event.
     */
    fun quit() {
        //call all the control system .quit methods
        for (i in control_systems.indices) {
            println(control_systems.size)
            control_systems[i]!!.trialEnd()
            control_systems[i]!!.quit()
        }
    }

    /**
     * Handle a reset event.
     */
    fun reset() {
        pause = graphics_on
        for (i in control_systems.indices) {
            control_systems[i]!!.trialEnd()
            control_systems[i]!!.quit()
        }
        if (descriptionfile != null) {
            try {
                loadEnvironmentDescription()
            } catch (e: FileNotFoundException) {
                val tmp: Dialog
                description_file_loaded = false
                simulated_objects = arrayOfNulls(0)
                val msg = ("file not found: "
                        + descriptionfile)
                if (graphics_on) tmp = DialogMessage(
                    parent,
                    "TBSim Error", msg
                ) else println(msg)
                descriptionfile = null
            } catch (e: IOException) {
                val tmp: Dialog
                description_file_loaded = false
                simulated_objects = arrayOfNulls(0)
                val msg = "error trying to load " +
                        descriptionfile
                if (graphics_on) tmp = DialogMessage(
                    parent,
                    "TBSim Error", msg
                ) else println(msg)
                descriptionfile = null
            }
            if (graphics_on) {
                buffer = createImage(width2, height2)
                bufferg = buffer?.getGraphics()
                bufferg?.setColor(Color.white)
                bufferg?.fillRect(0, 0, width2, height2)
                this.repaint()
                pause = true
            }
        } else {
            val tmp: Dialog
            val msg = "Error: no description file"
            if (graphics_on) tmp = DialogMessage(
                parent,
                "TBSim Error",
                """
                    You must choose description file first.
                    Use the `load' option under the `file' menu.
                    """.trimIndent()
            )
        }
    }

    /**
     * Handle a start/resume event.
     */
    fun start() {
        if (description_file_loaded) {
            pause = false
            if (graphics_on) this.repaint()
            // tell the control systems the trial is beginning
            for (i in control_systems.indices) {
                control_systems[i]!!.trialInit()
            }
            startrun = System.currentTimeMillis()
            frames = 0
        } else {
            val tmp: Dialog
            if (graphics_on) tmp = DialogMessage(
                parent,
                "TBSim Error",
                """
                    You must load a description file first.
                    Use the `load' option under the `file' menu.
                    """.trimIndent()
            )
        }
    }

    /**
     * Handle a Runtime Stats event
     */
    fun showRuntimeStats() {
        val f = frames
        val t = System.currentTimeMillis() - startrun
        val r = Runtime.getRuntime()
        var this_sim = """ trial number	: $trials (counts down)
 sim time    	: $sim_time milliseconds
 timestep 		: $timestep milliseconds
 timeout      	: $timeout milliseconds
"""
        this_sim = if (pause) {
            """$this_sim frames/second	: N/A while paused
 free memory  	: ${r.freeMemory()}
 total memory	: ${r.totalMemory()}
 os.name      	: ${System.getProperty("os.name")}
 os.version   	: ${System.getProperty("os.version")}
 os.arch      	: ${System.getProperty("os.arch")}
 java.version	: ${System.getProperty("java.version")}
"""
        } else {
            val rate = 1000 * frames.toDouble() / t.toDouble()
            """$this_sim frames/second	: $rate
 free memory	: ${r.freeMemory()}
 total memory	: ${r.totalMemory()}
 os.name	: ${System.getProperty("os.name")}
 os.version	: ${System.getProperty("os.version")}
 os.arch	: ${System.getProperty("os.arch")}
 java.version	: ${System.getProperty("java.version")}
"""
        }
        val tmp: Dialog
        if (graphics_on) tmp = DialogMessage(
            parent, "Runtime Stats",
            this_sim
        ) else println(this_sim)
    }

    /**
     * Handle a pause event.
     */
    fun pause() {
        pause = true
    }

    /**
     * Handle setDrawIDs
     */
    fun setDrawIDs(v: Boolean) {
        draw_ids = v
    }

    /**
     * Handle setDrawIcons
     */
    fun setDrawIcons(v: Boolean) {
        draw_icons = v
    }

    /**
     * Handle setGraphics
     */
    fun setGraphics(v: Boolean) {
        graphics_on = v
    }

    /**
     * Handle setDrawRobotState
     */
    fun setDrawRobotState(v: Boolean) {
        draw_robot_state = v
    }

    /**
     * Handle setDrawObjectState
     */
    fun setDrawObjectState(v: Boolean) {
        draw_object_state = v
    }

    /**
     * Handle setDrawTrails
     */
    fun setDrawTrails(v: Boolean) {
        draw_trails = v
    }

    /**
     * Handle a load request.
     */
    fun load(df: String?) {
        pause()
        descriptionfile = df
        reset()
    }

    companion object {
        /**
         * The maximum number of objects in a simulation.
         */
        const val MAX_SIM_OBJS = 1000
    }
}