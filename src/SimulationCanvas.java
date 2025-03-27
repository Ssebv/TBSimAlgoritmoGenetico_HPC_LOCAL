/*
 * SimulationCanvas.java
 * 
 * 
 */

 import java.io.*;
 import java.awt.*;
 import java.lang.System;
 import java.lang.Class;
 import EDU.gatech.cc.is.abstractrobot.*;
 import EDU.gatech.cc.is.simulation.*;
 import EDU.gatech.cc.is.util.*;
 import EDU.cmu.cs.coral.util.*;
 import EDU.cmu.cs.coral.simulation.*;
 
 import java.util.concurrent.CountDownLatch;
 import java.util.concurrent.Semaphore;
 
 public class SimulationCanvas extends Canvas implements Runnable {
 
     private Frame parent;
     private int height, width;
     private boolean preserveSize = false;
     private Color bgcolor = new Color(0xFFFFFF);
     private Image bgimage;
     private Image buffer;
     private Graphics bufferg;
     private boolean read_once = false; //indicates if we've read a dsc file
     private boolean pause = true;
     private boolean graphics_on = true;
     public SimulatedObject simulated_objects[] = new SimulatedObject[0];
     public ControlSystemS control_systems[];// = new ControlSystemS[0];
     private double top, bottom, left, right;
     private long sim_time = 0;
     private long timestep = 100;
     private long timeout = -1;
     private long seed = -1;
     private int trials = -1;
     private Thread run_sim_thread;
     private String descriptionfile;
     private int idcounter = 0;
     private boolean to_draw = false;
 
     public String e1 = "";
     public String e2 = "";
     public long new_seed;
     public long new_time;
     public long new_timeout;
     public long new_maxtimestep;
     public boolean new_use_file;
     public NewRobotSpec[] new_robotos = new NewRobotSpec[20];
     public int nrobot = 0;
     public boolean parada = false;
     public Label label = null;
 
     private boolean keep_running = true;
     private CountDownLatch simulationEndLatch;
 
     public void setSimulationEndLatch(CountDownLatch latch) {
         this.simulationEndLatch = latch;
     }
 
     public Semaphore sem3 = new Semaphore(0);;
 
     /*make these package scope so TBSim can access for updating menu on startup*/
     boolean draw_ids = false; //don't draw robot ids
     boolean draw_icons = false; //don't draw robot icons
     boolean draw_robot_state = false; //don't draw robot state
     boolean draw_object_state = false; //don't draw robot state
     boolean draw_trails = false; //don't draw object trails
     /*end package scope*/
     private double visionNoiseMean;
     private double visionNoiseStddev; //the standard deviation for vision noise
     private long visionNoiseSeed; //the seed value
 
     private long startrun = 0;
     private long frames = 0;
 
     /**
      * The maximum number of objects in a simulation.
      */
     public static final int MAX_SIM_OBJS = 1000;
 
     public void setLabel(Label l) {
         this.label = l;
     }
 
     /**
      * Read the description of the world from a file.
      */
     private void loadEnvironmentDescription() throws IOException {
         this.nrobot = 0;
         FileReader file = new FileReader(descriptionfile);
         //System.out.print("111"); 
         Reader raw_in = new PreProcessor(file);
         //System.out.print("222");
         StreamTokenizer in = new StreamTokenizer(raw_in);
         String token;
         SimulatedObject[] temp_objs = new SimulatedObject[MAX_SIM_OBJS];
         int temp_objs_count = 0;
         ControlSystemS[] temp_css = new ControlSystemS[MAX_SIM_OBJS];
         int temp_css_count = 0;
         double x, y, t, r;
         double x1, y1, x2, y2;
         int color1, color2;
         int vc;
         idcounter = 0;
         String string1, string2;
         TBDictionary bboard = new TBDictionary();
         /*--- assume success. reset later if failure ---*/
         boolean dfl = true; //description_file_loaded;
     
         /*--- set default bounds before reading ---*/
         top = 5;
         bottom = -5;
         left = -5;
         right = 5;
 
         /*--- set up tokenizer ---*/
         in.wordChars('A', '_'); // let _ be a word character
         in.quoteChar('"');     // " is the quote char
 
         /*--- tokenize the file ---*/
         token = "beginning of file";
         try {
             while (in.nextToken() != StreamTokenizer.TT_EOF) {
                 if (in.ttype == StreamTokenizer.TT_WORD) {
                     token = in.sval;
                     ///if (false) //System.out.println(token);
 
                     /*--- check for "dictionary" statements ---*/
                     //FORMAT: dictionary KEY "some string"
                     if (token.equalsIgnoreCase("dictionary")) {
                         String key, obj;
                         if (in.nextToken()
                                 == StreamTokenizer.TT_WORD) {
                             key = in.sval;
 
                         } else {
                             token = in.sval;
                             throw new IOException();
                         }
 
                         in.nextToken();
                         obj = in.sval;
 
                         bboard.put(key, obj);
                     }
 
                     /*--- this affects the vision sensor noise ---*/
                     //FORMAT: vision_noise MEAN STDDEV SEED
                     if (token.equalsIgnoreCase("vision_noise")) {
                         //the next token is the value for the mean
                         //and should be a double
 
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             visionNoiseMean = (double) in.nval;
                         } else {
                             //we are looking for number, not string
                             token = in.sval;
                             throw new IOException();
                         }
 
                         //this is the stddev
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             visionNoiseStddev = (double) in.nval;
                         } else {
                             token = in.sval;
                             throw new IOException();
                         }
 
                         //the next one is a long for the seed value
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             visionNoiseSeed = (long) in.nval;
                         } else {
                             //not what we wanted!
                             token = in.sval;
                             throw new IOException();
                         }
 
                     }
 
                     /*--- it is to turn trails on/off ---*/
                     //FORMAT: view_robot_trails on
                     if (token.equalsIgnoreCase("view_robot_trails")) {
 
                         if (in.nextToken()
                                 == StreamTokenizer.TT_NUMBER) {
                             token = in.sval; // for error report
                             throw new IOException();
                         } else {
                             if (in.sval.equalsIgnoreCase("on")) {
                                 draw_trails = true;
                             }
                         }
                     }
 
                     /*--- it is to turn IDs on/off ---*/
                     //FORMAT: view_robot_IDs on
                     if (token.equalsIgnoreCase("view_robot_IDs")) {
 
                         if (in.nextToken()
                                 == StreamTokenizer.TT_NUMBER) {
                             token = in.sval; // for error report
                             throw new IOException();
                         } else {
                             if (in.sval.equalsIgnoreCase("on")) {
                                 draw_ids = true;
                             }
                         }
                     }
 
                     /*--- it is to turn robot state on/off ---*/
                     //FORMAT: view_robot_state on
                     if (token.equalsIgnoreCase("view_robot_state")) {
 
                         if (in.nextToken()
                                 == StreamTokenizer.TT_NUMBER) {
                             token = in.sval; // for error report
                             throw new IOException();
                         } else {
                             if (in.sval.equalsIgnoreCase("on")) {
                                 draw_robot_state = true;
                             }
                         }
                     }
 
 
                     /*--- it is to turn objec info IDs on/off ---*/
                     //FORMAT: view_object_into on
                     if (token.equalsIgnoreCase("view_object_info")) {
 
                         if (in.nextToken()
                                 == StreamTokenizer.TT_NUMBER) {
                             token = in.sval; // for error report
                             throw new IOException();
                         } else {
                             if (in.sval.equalsIgnoreCase("on")) {
                                 draw_object_state = true;
                             }
                         }
                     }
 
                     /*--- it is to turn icons on/off ---*/
                     //FORMAT: view_icons on
                     if (token.equalsIgnoreCase("view_icons")) {
 
                         if (in.nextToken()
                                 == StreamTokenizer.TT_NUMBER) {
                             token = in.sval; // for error report
                             throw new IOException();
                         } else {
                             if (in.sval.equalsIgnoreCase("on")) {
                                 draw_icons = true;
                             }
                         }
                     }
 
 
                     /*--- it is a background_image statement ---*/
                     //FORMAT: background_image filename
                     if (token.equalsIgnoreCase("background_image")) {
                         in.nextToken(); // get the filename
                         String img_filename = in.sval;
                         /*//System.out.println("SO:" + "loading "
                                 + "background image file "
                                 + img_filename);*/
                         Toolkit tk = Toolkit.getDefaultToolkit();
                         bgimage = tk.getImage(img_filename);
                         tk.prepareImage(bgimage, -1, -1, this);
 
                     }
 
                     /*--- it is a background statement ---*/
                     //FORMAT: background color
                     if (token.equalsIgnoreCase("background")) {
                         if (in.nextToken()
                                 == StreamTokenizer.TT_WORD) {
                             String tmp = in.sval;
                             tmp = tmp.replace('x', '0');
                             tmp = tmp.replace('X', '0');
                             bgcolor = new Color(
                                     Integer.parseInt(tmp, 16));
                         } else {
                             bgcolor = new Color((int) in.nval);
                         }
                     }
 
                     /*--- it is a time statement ---*/
                     //FORMAT: time accel_rate
                     if (token.equalsIgnoreCase("time")) {
                         if (in.nextToken()
                                 == StreamTokenizer.TT_NUMBER) {
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                     }
 
                     /*--- it is a timeout statement ---*/
                     //FORMAT: timeout time
                     if (token.equalsIgnoreCase("timeout")) {
                         if (in.nextToken()
                                 == StreamTokenizer.TT_NUMBER) {
                             timeout = (long) in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                     }
 
                     /*--- it is a seed statement ---*/
                     //FORMAT: seed seed_val
                     if (token.equalsIgnoreCase("seed")) {
                         if (in.nextToken()
                                 == StreamTokenizer.TT_NUMBER) {
                             // skip for subsequent trials
                             if (!read_once) {
                                 if (this.new_use_file) {
                                     seed = (long) in.nval;
                                 } else {
                                     seed = this.new_seed;
                                 }
                             }
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                     }
 
                     /*--- it is a graphics statement ---*/
                     //FORMAT: graphics on/off
                     if (token.equalsIgnoreCase("graphics")) {
                         if (in.nextToken()
                                 == StreamTokenizer.TT_NUMBER) {
                             token = in.sval; // for error report
                             throw new IOException();
                         } else {
                             if (in.sval.equalsIgnoreCase("off")) {
                                 graphics_on = false;
                             }
 
                         }
                     }
 
                     /*--- it is a trials statement ---*/
                     //FORMAT: trials num_trials
                     if (token.equalsIgnoreCase("trials")) {
                         if (in.nextToken()
                                 == StreamTokenizer.TT_NUMBER) {
                             if (trials == -1) {
                                 trials = (int) in.nval;
                             }
                             if (trials < 0) {
                                 throw new IOException();
                             }
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                     }
 
                     /*--- it is a maxtimestep statement ---*/
                     //FORMAT: maxtimestep milliseconds
                     //DEPRECATED!
                     if (token.equalsIgnoreCase("maxtimestep")) {
                         if (in.nextToken()
                                 == StreamTokenizer.TT_NUMBER) {
                             if (this.new_use_file) {
                                 timestep = (long) in.nval;
                             } else {
                                 timestep = this.new_maxtimestep;
                             }
                             /////System.out.println("maxtimestep statement read, treated as timestep");
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                     }
 
                     /*--- it is a timestep statement ---*/
                     //FORMAT: timestep milliseconds
                     //DEPRECATED!
                     if (token.equalsIgnoreCase("timestep")) {
                         if (in.nextToken()
                                 == StreamTokenizer.TT_NUMBER) {
                             if (this.new_use_file) {
                                 timestep = (long) in.nval;
                             } else {
                                 timestep = this.new_time;
                             }
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                     }
 
                     /*--- it is a bounds statement ---*/
                     //FORMAT: bounds left right bottom top
                     if (token.equalsIgnoreCase("bounds")) {
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             left = in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             right = in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             bottom = in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             top = in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                     }
 
                     /*--- it is a windowsize statement ---*/
                     //FORMAT: windowsize width height
                     if (token.equalsIgnoreCase("windowsize")) {
                         int localWidth = width;
                         int localHeight = height;
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             localWidth = (int) in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             localHeight = (int) in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (!preserveSize) {
                             setSize(localWidth, localHeight);
                             reSizeWindow();
                         }
                     }
 
                     /*--- it is an object statement ---*/
                     //FORMAT object objectclass 
                     //	x y t r color1 color2 visionclass
                     if (token.equalsIgnoreCase("object")) {
                         if (in.nextToken() == StreamTokenizer.TT_WORD) {
                             string1 = in.sval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             x = in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             y = in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             t = in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             r = in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken()
                                 == StreamTokenizer.TT_WORD) {
                             String tmp = in.sval;
                             tmp = tmp.replace('x', '0');
                             tmp = tmp.replace('X', '0');
                             color1 = Integer.parseInt(tmp, 16);
                         } else {
                             color1 = (int) in.nval;
                         }
                         if (in.nextToken()
                                 == StreamTokenizer.TT_WORD) {
                             String tmp = in.sval;
                             tmp = tmp.replace('x', '0');
                             tmp = tmp.replace('X', '0');
                             color2 = Integer.parseInt(tmp, 16);
                         } else {
                             color2 = (int) in.nval;
                         }
 
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             vc = (int) in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         /*--- instantiate the obj ---*/
                         token = string1; // in case of error
                         /////System.out.println("CLASE3: ("+string1+")");
                         Class rclass = Class.forName(string1);
                         SimulatedObject obj = (SimulatedObject) rclass.newInstance();
                         obj.init(x, y, t, r, new Color(color1),
                                 new Color(color2), vc,
                                 idcounter++, seed++);
                         temp_objs[temp_objs_count++] = obj;
                     }
 
 
                     /*--- it is a linearobject statement ---*/
                     //FORMAT linearobject objectclass 
                     //	x1 y1 x2 y2 r color1 color2 visionclass
                     if (token.equalsIgnoreCase("linearobject")) {
                         if (in.nextToken() == StreamTokenizer.TT_WORD) {
                             string1 = in.sval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             x1 = in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             y1 = in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             x2 = in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             y2 = in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             r = in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken()
                                 == StreamTokenizer.TT_WORD) {
                             String tmp = in.sval;
                             tmp = tmp.replace('x', '0');
                             tmp = tmp.replace('X', '0');
                             color1 = Integer.parseInt(tmp, 16);
                         } else {
                             color1 = (int) in.nval;
                         }
                         if (in.nextToken()
                                 == StreamTokenizer.TT_WORD) {
                             String tmp = in.sval;
                             tmp = tmp.replace('x', '0');
                             tmp = tmp.replace('X', '0');
                             color2 = Integer.parseInt(tmp, 16);
                         } else {
                             color2 = (int) in.nval;
                         }
 
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             vc = (int) in.nval;
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         /*--- instantiate the obj ---*/
                         token = string1; // in case of error
                         /////System.out.println("CLASE4: ("+string1+")");
                         Class rclass = Class.forName(string1);
                         SimulatedLinearObject obj = (SimulatedLinearObject) rclass.newInstance();
                         obj.init(x1, y1, x2, y2, r, new Color(color1),
                                 new Color(color2), vc,
                                 idcounter++, seed++);
                         temp_objs[temp_objs_count++] = obj;
                     }
 
 
                     /*--- it is a robot statement ---*/
                     //FORMAT robot robotclass controlsystemclass 
                     //	x y t color1 color2 visionclass
                     if (token.equalsIgnoreCase("robot")) {
                         NewRobotSpec robotspec = this.getNextRobot();
                         if (in.nextToken() == StreamTokenizer.TT_WORD) {
                             if (this.new_use_file) {
                                 string1 = in.sval;
                             } else {
                                 string1 = robotspec.robottype;
                             }
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_WORD) {
                             if (this.new_use_file) {
                                 string2 = in.sval;
                             } else {
                                 string2 = robotspec.controlsystem;
                             }
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             if (this.new_use_file) {
                                 x = in.nval;
                             } else {
                                 x = robotspec.x;
                             }
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             if (this.new_use_file) {
                                 y = in.nval;
                             } else {
                                 y = robotspec.y;
                             }
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             if (this.new_use_file) {
                                 t = in.nval;
                             } else {
                                 t = robotspec.theta;
                             }
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
                         if (in.nextToken()
                                 == StreamTokenizer.TT_WORD) {
                             String tmp = "";
                             if (this.new_use_file) {
                                 tmp = in.sval;
                             } else {
                                 tmp = robotspec.forecolor;
                             }
 
                             tmp = tmp.replace('x', '0');
                             tmp = tmp.replace('X', '0');
                             color1 = Integer.parseInt(tmp, 16);
                         } else {
                             color1 = (int) in.nval;
                         }
                         if (in.nextToken()
                                 == StreamTokenizer.TT_WORD) {
                             String tmp = "";
                             if (this.new_use_file) {
                                 tmp = in.sval;
                             } else {
                                 tmp = robotspec.backcolor;
                             }
                             tmp = tmp.replace('x', '0');
                             tmp = tmp.replace('X', '0');
                             color2 = Integer.parseInt(tmp, 16);
                         } else {
                             color2 = (int) in.nval;
                         }
 
                         if (in.nextToken() == StreamTokenizer.TT_NUMBER) {
                             if (this.new_use_file) {
                                 vc = (int) in.nval;
                             } else {
                                 vc = robotspec.visionclass;
                             }
                         } else {
                             token = in.sval; // for error report
                             throw new IOException();
                         }
 
                         /*--- the robot ---*/
                         token = string1; // in case of error
                         Class rclass = Class.forName(string1);
                         SimulatedObject obj = (SimulatedObject) rclass.newInstance();
                         obj.init(x, y, t, 0, new Color(color1),
                                 new Color(color2), vc,
                                 idcounter++, seed++);
                         temp_objs[temp_objs_count++] = obj;
 
                         /*--- set the dictionary ---*/
                         ((Simple) obj).setDictionary(bboard);
 
                         /*--- the control system ---*/
                         token = string2; // in case of error
                         ////System.out.println("SO:" + "robot "+string2);
                         Class csclass = Class.forName(string2);
                         ControlSystemS css
                                 = (ControlSystemS) csclass.newInstance();
                         css.init((Simple) obj, seed++);
                         //css.Configure();//save for later
                         temp_css[temp_css_count++]
                                 = (ControlSystemS) css;
                     }
                 } else {
                     throw new IOException();
                 }
                 file.close();
                 raw_in.close();
             }
 
             /*--- catch any exceptions thrown in the parsing ---*/
         } catch (IOException e) {
             dfl = false;
             simulated_objects = new SimulatedObject[0];
             String msg
                     = "bad format"
                     + " at line " + in.lineno()
                     + " in " + descriptionfile
                     + " near "
                     + "'" + token + "'";
             Dialog tmp;
             if (graphics_on) {
                 tmp = new DialogMessage(parent,
                         "TBSim Error", msg);
             } else {
                 System.out.println("SO:" + msg);
             }
             descriptionfile = null;
         } catch (ClassNotFoundException e) {
             dfl = false;
             simulated_objects = new SimulatedObject[0];
             String msg
                     = "unable to find class "
                     + "'" + token + "'"
                     + " at line " + in.lineno()
                     + " in " + descriptionfile + ".\n"
                     + "You may need to check your CLASSPATH.";
             Dialog tmp;
             if (graphics_on) {
                 tmp = new DialogMessage(parent,
                         "TBSim Error", msg);
             } else {
                 //System.out.println("SO:" + msg);
             }
             descriptionfile = null;
         } catch (IllegalAccessException e) {
             dfl = false;
             simulated_objects = new SimulatedObject[0];
             String msg
                     = "illegal to access class "
                     + "'" + token + "'"
                     + " at line " + in.lineno()
                     + " in " + descriptionfile;
             Dialog tmp;
             if (graphics_on) {
                 tmp = new DialogMessage(parent,
                         "TBSim Error", msg);
             } else {
                 //System.out.println("SO:" + msg);
             }
             descriptionfile = null;
         } catch (InstantiationException e) {
             dfl = false;
             simulated_objects = new SimulatedObject[0];
             String msg
                     = "instantiation error for "
                     + "'" + token + "'"
                     + " at line " + in.lineno()
                     + " in " + descriptionfile;
             Dialog tmp;
             if (graphics_on) {
                 tmp = new DialogMessage(parent,
                         "TBSim Error", msg);
             } else {
                 //System.out.println("SO:" + msg);
             }
             descriptionfile = null;
         } catch (ClassCastException e) {
             dfl = false;
             simulated_objects = new SimulatedObject[0];
             String msg
                     = "class conflict for "
                     + "'" + token + "'"
                     + " at line " + in.lineno()
                     + " in " + descriptionfile + "."
                     + " It could be that the control system was not "
                     + " written for the type of robot you "
                     + " specified.";
             Dialog tmp;
             if (graphics_on) {
                 tmp = new DialogMessage(parent,
                         "TBSim Error", msg);
             } else {
                 //System.out.println("SO:" + msg);
             }
             descriptionfile = null;
         }
 
         /*--- set up global arrays of objs and cont systems ---*/
         simulated_objects = new SimulatedObject[temp_objs_count];
         for (int i = 0; i < temp_objs_count; i++) {
             //System.out.println("SS: "+ i + ":" + temp_objs[i].getClass().getName());
             simulated_objects[i] = temp_objs[i];
         }
         for (int i = 0; i < temp_objs_count; i++) {
             // let everyone take a step to update their pointers
             simulated_objects[i].takeStep(0, simulated_objects);
             if (simulated_objects[i] instanceof VisualObjectSensor) {
                 //we need to tell it the noise parameters...
                 //we do it here so that it doesnt matter where in dsc they
                 //declare visionnoise
 
                 ((VisualObjectSensor) simulated_objects[i]).setVisionNoise(visionNoiseMean,
                         visionNoiseStddev,
                         visionNoiseSeed);
             }
         }
         control_systems = new ControlSystemS[temp_css_count];
         for (int i = 0; i < temp_css_count; i++) {
             control_systems[i] = temp_css[i];
             control_systems[i].configure();
         }
 
         description_file_loaded = dfl;
         read_once = true;
 
     }
 
     public NewRobotSpec getNextRobot() {
         if (this.new_robotos != null) {
             this.nrobot++;
             /////System.out.println(this.nequipo-1);
             return this.new_robotos[this.nrobot - 1];
         }
         return null;
     }
 
     public SimulationCanvas(Frame p, int w, int h,
             String dscfile, NewRobotSpec[] robotos, long new_seed, long new_time, long new_maxtimestep) {
         this(p, w, h, dscfile, true, robotos, new_seed, new_time, new_maxtimestep);
 
         visionNoiseStddev = 0.0; //default is no noise
         visionNoiseSeed = 31337; //default noise seed
     }
 
     /**
      * Set up the SimulationCanvas.
      */
     public SimulationCanvas(Frame p, int w, int h,
             String dscfile, boolean preserveSize, NewRobotSpec[] new_robotos, long new_seed, long new_time, long new_maxtimestep) {
         this.new_seed = new_seed;
         this.new_time = new_time;
         this.new_maxtimestep = new_maxtimestep;
         this.new_robotos = new_robotos;
         this.new_use_file = new_robotos == null;
         if (this.new_robotos != null) {
             this.e1 = new_robotos[0].controlsystem;
             this.e2 = new_robotos[5].controlsystem; // Equipo o equipos que se van a enfrentar
             // System.out.println("..."+this.e1); // Imprime el equipo 1 en este caso el del algoritmo genetico parametrizado
         }
 
         ////System.out.println("USE FILE: " + this.new_use_file);
         if (p == null) {
             ////System.out.println("false");
             graphics_on = false;
             pause = false;
         } else {
             ////System.out.println("true");
             graphics_on = true;
             pause = true;
         }
         parent = p;
         simulated_objects = new SimulatedObject[0];
         control_systems = new ControlSystemS[0];
         this.preserveSize = preserveSize;
 
         descriptionfile = dscfile;
 
         if (graphics_on) {
             setSize(w, h);
             setBackground(Color.white);
             ////System.out.println("G ON");
         }
 
 
         /*--- instantitate thread ---*/
         run_sim_thread = new Thread(this);
         run_sim_thread.start();
     }
 
     private boolean description_file_loaded = false; // Para saber si se ha cargado el fichero de descripcion
 
     /**
      * Provide info about whether we have successufully loaded the file.
      *
      * @return true if a file is loaded, false otherwise.
      */
     public boolean descriptionLoaded() {
         return (description_file_loaded);
     }
 
     /**
      * Run the simulation.
      */
     public void run() {
         //pause = true;
         long start_time = System.currentTimeMillis();
         long sim_timestep = 0;
         boolean robots_done = false;
         while (keep_running) {
             while (pause || (description_file_loaded == false)) {
                 if (graphics_on) {
                     this.repaint();
                 }
                 try {
                     Thread.sleep(200);
                 } catch (InterruptedException e) {
                 }
             }
 
             System.currentTimeMillis();
             sim_timestep = timestep;
 
             //--- deprecated
             //sim_timestep = (long)(
             //(double)(current_time - last_time)*
             //time_compression);
             //if (sim_timestep>maxtimestep)
             //sim_timestep = maxtimestep;
 
             /*--- run control systems and check for done ---*/
             robots_done = true;
             for (int i = 0; i < control_systems.length; i++) {
                 int stat = control_systems[i].takeStep();
                 if (stat != ControlSystemS.CSSTAT_DONE) {
                     robots_done = false;
                 }
             }
 
             /*--- run the physics ---*/
             for (int i = 0; i < simulated_objects.length; i++) {
                 simulated_objects[i].takeStep(
                         sim_timestep,
                         simulated_objects);
                 if (this.graphics_on && i == 5) {
                     String[] txt = ((NewSim) simulated_objects[i]).getStat(false).split(" ");
                     this.label.setText(this.e1 + " (" + txt[0] + ") vs " + this.e2 + "(" + txt[1] + ") [" + txt[2] + "]");
                 }
             }
 
             /*--- draw everything ---*/
             to_draw = true;
             if (graphics_on) {
                 this.repaint();
             }
             if (to_draw && graphics_on) {
                 try {
                     Thread.sleep(10);
                 } catch (InterruptedException e) {
                 }
             }
 
             /*--- garbage collect every time ---*/
             // this is to make cycle times more homogeneous
             //System.gc();  // too slow!
 
             /*--- count frames ---*/
             frames++;// for statistics gathering
 
             /*--- check for timeout or done ---*/
             if (((timeout > 0) && (sim_time >= timeout))
                     || robots_done) {
                 if (trials <= 1) {
                     for (int i = 0;
                             i < control_systems.length; i++) {
                         control_systems[i].trialEnd();
                         control_systems[i].quit();
 
                     }
                     keep_running = false;
                     if (graphics_on == false) {
                         showRuntimeStats();
                     }
 
                     //this.parada = true;
                     this.sem3.release();
                     System.err.println();
                     Thread.currentThread().interrupt();
                     return;
                     ///System.exit(0);
                 } else {
                     for (int i = 0;
                             i < control_systems.length; i++) {
                         control_systems[i].trialEnd();
                     }
                     trials--;
                     sim_time = 0;
                     reset();
                     start();
                 }
             }
 
             /*--- increment simulation time ---*/
             sim_time += sim_timestep;
         }
     }
     /**
      * Handle a drawing request.
      */
     public synchronized void update(Graphics g) {
         if ((bufferg != null) && (graphics_on)) {
             /*--- if no bgimage, draw bgcolor ---*/
             if (bgimage == null) {
                 bufferg.setColor(bgcolor);
                 bufferg.fillRect(0, 0, width, height);
             }
 
             /*--- draw the background image first ---*/
             if (bgimage != null) {
                 bufferg.drawImage(bgimage, 0, 0, this);
             }
 
             /*--- draw robot trails first ---*/
             for (int i = 0; i < simulated_objects.length; i++) {
                 // if robot
                 if (simulated_objects[i] instanceof Simple) {
                     // draw trail
                     if (draw_trails) {
                         simulated_objects[i].drawTrail(bufferg,
                                 width, height,
                                 top, bottom, left, right);
                     }
                 }
             }
 
             /*--- draw IDs and state ---*/
             for (int i = 0; i < simulated_objects.length; i++) {
                 // if robot
                 if (simulated_objects[i] instanceof Simple) {
                     if (draw_ids) {
                         simulated_objects[i].drawID(bufferg,
                                 width, height,
                                 top, bottom, left, right);
                     }
                     if (draw_robot_state) {
                         simulated_objects[i].drawState(bufferg,
                                 width, height,
                                 top, bottom, left, right);
                     }
                 }
 
                 /*--- draw the object ---*/
                 if (draw_icons) {
                     simulated_objects[i].drawIcon(
                             bufferg, width, height,
                             top, bottom, left, right);
                 } else {
                     simulated_objects[i].draw(
                             bufferg, width, height,
                             top, bottom, left, right);
                 }
 
                 /*--- draw object state ---*/
                 // if not a robot
                 if (!(simulated_objects[i] instanceof Simple)) {
                     if (draw_object_state) {
                         simulated_objects[i].drawState(bufferg,
                                 width, height,
                                 top, bottom, left, right);
                     }
                 }
             }
             g.drawImage(buffer, 0, 0, this);
         }
         to_draw = false;
     }
 
     /**
      * Resize the SimulationCanvas.
      */
     public void setSize(int w, int h) {
         width = w;
         height = h;
         super.setSize(width, height);
     }
 
     public Dimension getPreferredSize() {
         return new Dimension(width, height);
     }
 
     public void reSizeWindow() {
         invalidate();
         Container parent = getParent();
         while (parent.getParent() != null) {
             parent = parent.getParent();
         }
         parent.setSize(parent.getPreferredSize());
         parent.validate();
     }
 
     /**
      * Handle a quit event.
      */
     public void quit() {
         //call all the control system .quit methods
         for (int i = 0; i < control_systems.length; i++) {
             /////System.out.println(control_systems.length);
             control_systems[i].trialEnd();
             control_systems[i].quit();
         }
     }
 
     /**
      * Handle a reset event.
      */
     public void reset() {
         if (graphics_on == true) {
             pause = true;
         } else {
             pause = false;
         }
 
         for (int i = 0; i < control_systems.length; i++) {
             control_systems[i].trialEnd();
             control_systems[i].quit();
         }
 
         if (descriptionfile != null) {
             try {
                 loadEnvironmentDescription();
             } catch (FileNotFoundException e) {
                 Dialog tmp;
                 description_file_loaded = false;
                 simulated_objects = new SimulatedObject[0];
                 String msg = "file not found: "
                         + descriptionfile;
                 if (graphics_on) {
                     tmp = new DialogMessage(parent,
                             "TBSim Error", msg);
                 } else {
                     //System.out.println("SO:" + msg);
                 }
                 descriptionfile = null;
             } catch (IOException e) {
                 Dialog tmp;
                 description_file_loaded = false;
                 simulated_objects = new SimulatedObject[0];
                 String msg = "error trying to load "
                         + descriptionfile;
                 if (graphics_on) {
                     tmp = new DialogMessage(parent,
                             "TBSim Error", msg);
                 } else {
                     //System.out.println("SO:" + msg);
                 }
                 descriptionfile = null;
             }
             if (graphics_on) {
                 buffer = createImage(width, height);
                 bufferg = buffer.getGraphics();
                 bufferg.setColor(Color.white);
                 bufferg.fillRect(0, 0, width, height);
                 this.repaint();
                 pause = true;
             }
         } else {
             Dialog tmp;
             String msg = "Error: no description file";
             if (graphics_on) {
                 tmp = new DialogMessage(parent,
                         "TBSim Error",
                         "You must choose description file first.\n"
                         + "Use the `load' option under the `file' menu.");
             }
         }
     }
 
     /**
      * Handle a start/resume event.
      */
     public void start() {
         if (description_file_loaded) {
             pause = false;
             if (graphics_on) {
                 this.repaint();
             }
             // tell the control systems the trial is beginning
             for (int i = 0; i < control_systems.length; i++) {
                 control_systems[i].trialInit();
             }
             startrun = System.currentTimeMillis();
             frames = 0;
         } else {
             Dialog tmp;
             if (graphics_on) {
                 tmp = new DialogMessage(parent,
                         "TBSim Error",
                         "You must load a description file first.\n"
                         + "Use the `load' option under the `file' menu.");
             } else
                 System.out.println("Error cargarndo archivo");
         }
     }
 
     /**
      * Handle a Runtime Stats event
      */
     public void showRuntimeStats() {
         long f = frames;
         long t = System.currentTimeMillis() - startrun;
 
         Runtime r = Runtime.getRuntime();
 
         String this_sim
                 = " trial number	: " + trials + " (counts down)\n"
                 + " sim time    	: " + sim_time + " milliseconds\n"
                 + " timestep 		: " + timestep + " milliseconds\n"
                 + " timeout      	: " + timeout + " milliseconds\n";
 
         if (pause) {
             this_sim = this_sim
                     + " frames/second	: N/A while paused\n"
                     + " free memory  	: " + r.freeMemory() + "\n"
                     + " total memory	: " + r.totalMemory() + "\n"
                     + " os.name      	: " + System.getProperty("os.name") + "\n"
                     + " os.version   	: " + System.getProperty("os.version") + "\n"
                     + " os.arch      	: " + System.getProperty("os.arch") + "\n"
                     + " java.version	: " + System.getProperty("java.version") + "\n";
         } else {
             double rate = 1000 * (double) frames / (double) t;
             this_sim = this_sim
                     + " frames/second	: " + rate + "\n"
                     + " free memory	: " + r.freeMemory() + "\n"
                     + " total memory	: " + r.totalMemory() + "\n"
                     + " os.name	: " + System.getProperty("os.name") + "\n"
                     + " os.version	: " + System.getProperty("os.version") + "\n"
                     + " os.arch	: " + System.getProperty("os.arch") + "\n"
                     + " java.version	: " + System.getProperty("java.version") + "\n";
         }
         Dialog tmp;
         if (graphics_on) {
             tmp = new DialogMessage(parent, "Runtime Stats",
                     this_sim);
         }
         ///else
         // System.out.println(this_sim); // Para que lo muestre en consola en cada iteracion
     }
 
     /**
      * Handle a pause event.
      */
     public void pause() {
         pause = true;
     }
 
     /**
      * Handle setDrawIDs
      */
     public void setDrawIDs(boolean v) {
         draw_ids = v;
     }
 
     /**
      * Handle setDrawIcons
      */
     public void setDrawIcons(boolean v) {
         draw_icons = v;
     }
 
     /**
      * Handle setGraphics
      */
     public void setGraphics(boolean v) {
         graphics_on = v;
     }
 
     /**
      * Handle setDrawRobotState
      */
     public void setDrawRobotState(boolean v) {
         draw_robot_state = v;
     }
 
     /**
      * Handle setDrawObjectState
      */
     public void setDrawObjectState(boolean v) {
         draw_object_state = v;
     }
 
     /**
      * Handle setDrawTrails
      */
     public void setDrawTrails(boolean v) {
         draw_trails = v;
     }
 
     /**
      * Handle a load request.
      */
     public void load(String df) {
         pause();
         descriptionfile = df;
         reset();
     }
 
 }