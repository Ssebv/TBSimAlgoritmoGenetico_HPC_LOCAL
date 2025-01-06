/*
 * TBSimNoGraphics.java
 * Clase para la simulación del fútbol de robots sin interfaz gráfica.
 * Utilizada para evaluar el desempeño de distintas configuraciones de comportamiento de robots en un entorno simulado.
 */

 import EDU.gatech.cc.is.simulation.NewSim;

 import java.util.concurrent.Semaphore;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 public class TBSimNoGraphics extends Thread {
 
     private static final Logger LOGGER = Logger.getLogger(TBSimNoGraphics.class.getName());
 
     public SimulationCanvas simulation; // Canvas de la simulación.
     private final String dsc_file; // Archivo de descripción de la simulación.
     private final long new_seed; // Semilla para la generación aleatoria en la simulación.
     private final long new_time; // Duración de la simulación.
     private final long new_maxtimestep; // Máximo número de pasos de tiempo en la simulación.
     private final NewRobotSpec[] new_robotos; // Especificaciones de los robots participantes.
     public String estado; // Estado final de la simulación.
     public int realstart = 0; // Indica si la simulación ha comenzado realmente.
     Semaphore sem1 = null; // Semáforo para sincronización de inicio de la simulación.
     Semaphore sem2 = new Semaphore(0); // Semáforo para control de flujo de la simulación.
 
     /**
      * Constructor de la clase.
      *
      * @param args       Argumentos para la configuración.
      * @param dsc_file   Archivo de descripción.
      * @param robotos    Robots participantes.
      * @param seed       Semilla para aleatoriedad.
      * @param time       Duración de la simulación.
      * @param maxtimestep Máximo número de pasos de tiempo.
      */
     public TBSimNoGraphics(String[] args, String dsc_file, NewRobotSpec[] robotos, long seed, long time, long maxtimestep) {
         this.new_seed = seed;
         this.new_time = time;
         this.new_maxtimestep = maxtimestep;
         this.new_robotos = robotos;
         this.dsc_file = dsc_file;
     }
 
     @Override
     public void run() {
        //  LOGGER.info("Inicializando la simulación sin gráficos.");
         try {
             simulation = new SimulationCanvas(null, 0, 0, dsc_file, this.new_robotos, this.new_seed, this.new_time, this.new_maxtimestep);
             simulation.reset();
 
             if (simulation.descriptionLoaded()) {
                //  LOGGER.info("Descripción de simulación cargada exitosamente.");
                 if (sem1 != null) {
                     sem1.release(); // Libera la simulación para que continúe.
                     sem2.acquire(); // Espera a que la simulación inicie.
                 }
 
                 simulation.start(); // Inicia simulación en un hilo aparte.
                 simulation.sem3.acquire(); // Espera a que la simulación termine.
 
                 // Procesa el estado final de la simulación.
                 procesarEstadoFinal();
 
             } else {
                 manejarErrorDescripcion();
             }
         } catch (Exception e) {
             manejarErrorEjecucion(e);
         }
     }
 
     /**
      * Procesa el estado final de la simulación y guarda los resultados.
      */
     private void procesarEstadoFinal() {
         if (simulation.simulated_objects != null && simulation.simulated_objects.length > 5) {
             try {
                 this.estado = ((NewSim) simulation.simulated_objects[5]).getStat(true);
                //  LOGGER.info("Estado final de la simulación procesado correctamente: " + estado);
             } catch (ClassCastException e) {
                 LOGGER.log(Level.SEVERE, "Error al convertir el objeto simulado a NewSim: " + e.getMessage(), e);
                 this.estado = "0,0,-1";
             }
         } else {
             LOGGER.warning("No se encontró el objeto NewSim en simulation.simulated_objects[5].");
             this.estado = "0,0,-1";
         }
     }
 
     /**
      * Maneja errores relacionados con la descripción de la simulación.
      */
     private void manejarErrorDescripcion() {
         LOGGER.severe("Error en el archivo de descripción: " + dsc_file);
         if (new_robotos != null && new_robotos.length >= 6) {
             LOGGER.severe("Robot[0] ControlSystem: " + new_robotos[0].controlsystem);
             LOGGER.severe("Robot[5] ControlSystem: " + new_robotos[5].controlsystem);
         } else {
             LOGGER.severe("No hay suficientes robots configurados.");
         }
         if (simulation != null) {
             simulation.parada = true;
         }
         this.estado = "0,0,-1"; // Estado final de la simulación en caso de error.
     }
 
     /**
      * Maneja errores generales durante la ejecución de la simulación.
      *
      * @param e Excepción capturada.
      */
     private void manejarErrorEjecucion(Exception e) {
         LOGGER.log(Level.SEVERE, "Error durante la ejecución de la simulación: " + e.getMessage(), e);
         this.estado = "0,0,-1";
     }
 }
 