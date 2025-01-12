import EDU.gatech.cc.is.simulation.NewSim;

import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase para la simulación de fútbol de robots sin interfaz gráfica.
 * Evalúa configuraciones de comportamiento de robots en un entorno simulado.
 */
public class TBSimNoGraphics extends Thread {

    private static final Logger LOGGER = Logger.getLogger(TBSimNoGraphics.class.getName());

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
        // LOGGER.info("Inicializando la simulación sin gráficos...");
        simulation = new SimulationCanvas(null, 0, 0, dscFile, robots, seed, time, maxTimeStep);
        simulation.reset();

        if (!simulation.descriptionLoaded()) {
            manejarErrorDescripcion();
            throw new IllegalStateException("No se pudo cargar la descripción de la simulación.");
        }

        if (sem1 != null) {
            sem1.release(); // Libera la simulación para que continúe.
            sem2.acquire(); // Espera a que la simulación inicie.
        }

        // LOGGER.info("Simulación inicializada correctamente.");
    }

    /**
     * Ejecuta la simulación en un hilo separado.
     *
     * @throws InterruptedException Si ocurre un error durante la ejecución.
     */
    private void ejecutarSimulacion() throws InterruptedException {
        simulation.start();
        simulation.sem3.acquire(); // Espera a que la simulación termine.
    }

    /**
     * Procesa el estado final de la simulación y guarda los resultados.
     */
    private void procesarEstadoFinal() {
        if (simulation.simulated_objects != null && simulation.simulated_objects.length > 5) {
            try {
                estado = ((NewSim) simulation.simulated_objects[5]).getStat(true);
                // LOGGER.info("Estado final de la simulación procesado correctamente: " + estado);
            } catch (ClassCastException e) {
                LOGGER.log(Level.SEVERE, "Error al convertir el objeto simulado a NewSim.", e);
                estado = DEFAULT_ESTADO;
            }
        } else {
            LOGGER.warning("No se encontró el objeto NewSim en simulation.simulated_objects[5].");
            estado = DEFAULT_ESTADO;
        }
    }

    /**
     * Maneja errores relacionados con la descripción de la simulación.
     */
    private void manejarErrorDescripcion() {
        LOGGER.severe("Error en el archivo de descripción: " + dscFile);
        if (robots != null && robots.length >= 6) {
            LOGGER.severe("Robot[0] ControlSystem: " + robots[0].controlsystem);
            LOGGER.severe("Robot[5] ControlSystem: " + robots[5].controlsystem);
        } else {
            LOGGER.severe("No hay suficientes robots configurados.");
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
        LOGGER.log(Level.SEVERE, "Error durante la ejecución de la simulación.", e);
        estado = DEFAULT_ESTADO;
    }
}
