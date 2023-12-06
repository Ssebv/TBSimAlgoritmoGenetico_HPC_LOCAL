/*
 * TBSimNoGraphics.java
 * Clase para la simulación del fútbol de robots sin interfaz gráfica.
 * Utilizada para evaluar el desempeño de distintas configuraciones de comportamiento de robots en un entorno simulado.
 */

import EDU.gatech.cc.is.simulation.NewSim;
import java.util.concurrent.Semaphore;

public class TBSimNoGraphics extends Thread {

    public SimulationCanvas simulation; // Canvas de la simulación.
    private String dsc_file; // Archivo de descripción de la simulación.
    private long new_seed; // Semilla para la generación aleatoria en la simulación.
    private long new_time; // Duración de la simulación.
    private long new_maxtimestep; // Máximo número de pasos de tiempo en la simulación.
    private NewRobotSpec[] new_robotos; // Especificaciones de los robots participantes.
    public String estado; // Estado final de la simulación.
    public int realstart = 0; // Indica si la simulación ha comenzado realmente.
    Semaphore sem1 = null; // Semáforo para sincronización de inicio de la simulación.
    Semaphore sem2 = new Semaphore(0); // Semáforo para control de flujo de la simulación.

    // Constructor de la clase.
    public TBSimNoGraphics(String[] args, String dsc_file, NewRobotSpec[] robotos, long seed, long time,
            long maxtimestep) {
        this.new_seed = seed;
        this.new_time = time;
        this.new_maxtimestep = maxtimestep;
        this.new_robotos = robotos;
        this.dsc_file = dsc_file;
    }

    @Override
    public void run() {
        // Configuración de la simulación con los parámetros recibidos.
        simulation = new SimulationCanvas(null, 0, 0, dsc_file, this.new_robotos, this.new_seed, this.new_time,
                this.new_maxtimestep);
        simulation.reset();

        // Verifica que el archivo de descripción de la simulación se haya cargado
        // correctamente.
        if (simulation.descriptionLoaded()) {
            // Si es asi inicia la simulación.
            try {
                // Si sem1 no es nulo, libera el semáforo para comenzar la simulación.
                if (sem1 != null) {
                    this.sem1.release(); // Libera la simulación para que continúe
                    this.sem2.acquire(); // Espera a que la simulación inicie
                }

                simulation.start(); // Inicia simulación en un hilo aparte
                simulation.sem3.acquire(); // Espera a que la simulación inicie

                // Almacena el estado final de la simulación.
                this.estado = ((NewSim) simulation.simulated_objects[5]).getStat(true);

            } catch (Exception e) {
                System.out.println(e);
            }

        } else {
            System.out.println(
                    "Error description file..." + new_robotos[0].controlsystem + " vs " + new_robotos[5].controlsystem);
            simulation.parada = true;
            this.estado = "0,0,-1"; // Estado final de la simulación.
        }

        // System.out.println("#FIN: {" + this.estado + "}"); // Imprime el estado final
        // de la simulación
        // System.out.println(((NewSim)simulation.simulated_objects[5]).getStat() );
    }

}