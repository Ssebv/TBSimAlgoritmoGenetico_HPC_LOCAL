/*
 * TBSimNoGraphics.java
 * 
 * Clase para la simulación de fútbol de robots sin interfaz gráfica.
 * Evalúa configuraciones de comportamiento de robots en un entorno simulado.
 */

 import java.awt.Frame;
 import java.util.concurrent.Semaphore;
 import java.util.concurrent.TimeUnit;
 import java.util.logging.Logger;
 
 // Importa la clase NewSim correctamente
 import EDU.gatech.cc.is.simulation.NewSim;
 
 // Asegúrate de importar LogManager si está en otro paquete
 // import com.tu_paquete.LogManager;
 
 public class TBSimNoGraphics extends Thread {
 
     private final LogManager logManager; // Instancia interna de LogManager
 
     public SimulationCanvas simulation; // Canvas de la simulación.
     private final String dscFile; // Archivo de descripción de la simulación.
     private final long seed; // Semilla para la generación aleatoria en la simulación.
     private final long time; // Duración de la simulación.
     private final long maxTimeStep; // Máximo número de pasos de tiempo en la simulación.
     private final NewRobotSpec[] robots; // Especificaciones de los robots participantes.
     public String estado; // Estado final de la simulación.
 
     public Semaphore sem1 = null; // Semáforo para sincronización de inicio de la simulación.
     public Semaphore sem2 = new Semaphore(0); // Semáforo para control de flujo de la simulación.
 
     private static final String DEFAULT_ESTADO = "0,0,-1";
 
     /**
      * Constructor de la clase.
      *
      * @param args        Argumentos para la configuración.
      * @param dscFile     Archivo de descripción.
      * @param robots      Robots participantes.
      * @param seed        Semilla para aleatoriedad.
      * @param time        Duración de la simulación.
      * @param maxTimeStep Máximo número de pasos de tiempo.
      */
     public TBSimNoGraphics(String[] args, String dscFile, NewRobotSpec[] robots, long seed, long time, long maxTimeStep) {
         // Inicializar LogManager internamente
         Logger LOGGER = Logger.getLogger(TBSimNoGraphics.class.getName());
         this.logManager = new LogManager(LOGGER, true, new Configuracion());

         this.seed = seed;
         this.time = time;
         this.maxTimeStep = maxTimeStep;
         this.robots = robots;
         this.dscFile = dscFile;
     }
 
     @Override
     public void run() {
         try {
             inicializarSimulacion();
             ejecutarSimulacion();
             procesarEstadoFinal();
         } catch (Exception e) {
             manejarErrorEjecucion(e);
         }
     }
 
     /**
      * Inicializa la simulación.
      *
      * @throws Exception Si ocurre un error durante la inicialización.
      */
     private void inicializarSimulacion() throws Exception {
         // Reemplaza los siguientes parámetros con los correctos que tu SimulationCanvas espera
         Frame param1 = null; // O proporciona una instancia de Frame si es necesario
         int param2 = 0;        // Reemplaza con el valor real si es necesario
         int param3 = 0;        // Reemplaza con el valor real si es necesario
 
         simulation = new SimulationCanvas(param1, param2, param3, dscFile, robots, seed, time, maxTimeStep);
         simulation.reset();
 
         if (!simulation.descriptionLoaded()) {
             manejarErrorDescripcion();
             throw new IllegalStateException("No se pudo cargar la descripción de la simulación.");
         }
 
         try {
             if (sem1 != null) {
                //  logManager.logInfo("Liberando sem1 para iniciar la simulación.");
                 sem1.release(); // Libera la simulación para que continúe.
                //  logManager.logInfo("Intentando adquirir sem2 con timeout de 5 segundos.");
                 if (!sem2.tryAcquire(5, TimeUnit.SECONDS)) { // Timeout en semáforo
                    //  throw new IllegalStateException("Timeout esperando liberación de semáforo sem2.");
                 }
                //  logManager.logInfo("Sem2 adquirido exitosamente.");
             }
         } catch (InterruptedException e) {
             Thread.currentThread().interrupt();
             logManager.logError("Error manejando semáforos en inicialización: " + e.getMessage());
             throw e;
         }
     }
 
     /**
      * Ejecuta la simulación en un hilo separado.
      *
      * @throws InterruptedException Si ocurre un error durante la ejecución.
      */
     private void ejecutarSimulacion() throws InterruptedException {
         simulation.start();
        //  logManager.logInfo("Simulación iniciada. Intentando adquirir sem3 con timeout de 5 segundos.");
         boolean success = simulation.sem3.tryAcquire(5, TimeUnit.SECONDS);
         if (!success) {
            //  logManager.logWarning("Timeout esperando que sem3 se libere. Reintentando...");
             // Intentar reintentar un número fijo de veces
             int retryCount = 3;
             for (int i = 0; i < retryCount && !success; i++) {
                 logManager.logInfo("Intento de reintento " + (i + 1) + " para adquirir sem3.");
                 success = simulation.sem3.tryAcquire(5, TimeUnit.SECONDS);
                 if (success) {
                    //  logManager.logInfo("Sem3 adquirido exitosamente en el intento de reintento " + (i + 1) + ".");
                     break;
                 } else {
                     logManager.logWarning("Timeout en el intento de reintento " + (i + 1) + ".");
                 }
             }
             if (!success) {
                 throw new InterruptedException("Sem3 no se liberó después de varios intentos.");
             }
         } else {
            //  logManager.logInfo("Sem3 adquirido exitosamente.");
         }
     }
 
     /**
      * Procesa el estado final de la simulación y guarda los resultados.
      */
     private void procesarEstadoFinal() {
         if (simulation.simulated_objects != null && simulation.simulated_objects.length > 5) {
             try {
                 estado = ((NewSim) simulation.simulated_objects[5]).getStat(true);
                //  logManager.logInfo("Estado final de la simulación procesado correctamente: " + estado);
             } catch (ClassCastException e) {
                 logManager.logError("Error al convertir el objeto simulado a NewSim: " + e.getMessage());
                 estado = DEFAULT_ESTADO;
             }
         } else {
             logManager.logWarning("No se encontró el objeto NewSim en simulation.simulated_objects[5].");
             estado = DEFAULT_ESTADO;
         }
     }
 
     /**
      * Maneja errores relacionados con la descripción de la simulación.
      */
     private void manejarErrorDescripcion() {
         logManager.logError("Error en el archivo de descripción: " + dscFile);
         if (robots != null && robots.length >= 6) {
             logManager.logError("Robot[0] ControlSystem: " + robots[0].controlsystem);
             logManager.logError("Robot[5] ControlSystem: " + robots[5].controlsystem);
         } else {
             logManager.logError("No hay suficientes robots configurados.");
         }
         if (simulation != null) {
             simulation.parada = true;
         }
         estado = DEFAULT_ESTADO; // Estado final de la simulación en caso de error.
     }
 
     /**
      * Maneja errores generales durante la ejecución de la simulación.
      *
      * @param e Excepción capturada.
      */
     private void manejarErrorEjecucion(Exception e) {
         logManager.logError("Error durante la ejecución de la simulación: " + e.getMessage());
         estado = DEFAULT_ESTADO;
     }
 }
 