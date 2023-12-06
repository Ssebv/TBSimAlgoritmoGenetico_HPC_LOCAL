/*
 * TBSimNoGraphics.java
 */

import EDU.gatech.cc.is.simulation.NewSim;
import java.util.concurrent.Semaphore;

public class TBSimNoGraphics extends Thread {

    public SimulationCanvas simulation;
    private String dsc_file;
    private String[] args;
    private long new_seed = 3;
    private long new_time = 2;
    private long new_maxtimestep = 50;
    private NewRobotSpec [] new_robotos;
    public String estado = "";
    public int realstart = 0;
    Semaphore sem1 = null;
    Semaphore sem2 = new Semaphore(0);

    
    public TBSimNoGraphics(String[] args, String dsc_file, NewRobotSpec [] robotos, long seed, long time, long maxtimestep) {
        this.args = args;
        this.new_seed = seed;
        this.new_time = time;
        this.new_maxtimestep = maxtimestep;
        this.new_robotos = robotos;
        this.dsc_file = dsc_file;
    }

    @Override
    public void run() {
        simulation = new SimulationCanvas(null, 0, 0, dsc_file, this.new_robotos, this.new_seed, this.new_time, this.new_maxtimestep);
        simulation.reset();
      
        if (simulation.descriptionLoaded()) {
            try {
                if (sem1!=null){
                    this.sem1.release();
                    this.sem2.acquire();
                }
                        
                simulation.start();   
                simulation.sem3.acquire();
                /*
                while (!simulation.parada) {
                    Thread.sleep(50);
                }*/
                this.estado = ((NewSim)simulation.simulated_objects[5]).getStat(true);
            } catch (Exception e) {
                System.out.println(e);
            }

        } else {
            System.out.println("Error description file..." + new_robotos[0].controlsystem + " vs " + new_robotos[5].controlsystem);
            simulation.parada = true;
            this.estado = "0,0,-1";
        }
        
        //System.out.println("#FIN: {" + this.estado + "}");
        
        //System.out.println(((NewSim)simulation.simulated_objects[5]).getStat() );
    }

}
