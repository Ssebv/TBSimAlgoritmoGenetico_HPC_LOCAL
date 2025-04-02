import java.awt.Frame;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import EDU.gatech.cc.is.simulation.NewSim;

public class TBSimNoGraphics extends Thread {

    private static final String DEFAULT_ESTADO = "0,0,-1";
    private static final int SEMAPHORE_TIMEOUT_SECONDS = 10;
    private static final int SEM3_RETRIES = 3;

    public SimulationCanvas simulation;
    public String estado;
    public Semaphore sem1 = null;
    public Semaphore sem2 = new Semaphore(0);

    private final LogManager logManager;
    private final String dscFile;
    private final long seed;
    private final long time;
    private final long maxTimeStep;
    private final NewRobotSpec[] robots;

    public TBSimNoGraphics(String[] args, String dscFile, NewRobotSpec[] robots, long seed, long time, long maxTimeStep) {
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

    private void inicializarSimulacion() throws Exception {
        Frame frame = null;
        simulation = new SimulationCanvas(frame, 0, 0, dscFile, robots, seed, time, maxTimeStep);
        simulation.reset();

        if (!simulation.descriptionLoaded()) {
            manejarErrorDescripcion();
            throw new IllegalStateException("No se pudo cargar la descripción de la simulación.");
        }

        if (sem1 != null) {
            sem1.release();
            if (!sem2.tryAcquire(SEMAPHORE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logManager.logWarning("Timeout esperando sem2, pero se continúa la simulación.");
            }
        }
    }

    private void ejecutarSimulacion() throws InterruptedException {
        simulation.start();
        boolean success = simulation.sem3.tryAcquire(5, TimeUnit.SECONDS);

        for (int i = 0; i < SEM3_RETRIES && !success; i++) {
            logManager.logInfo("Intento " + (i + 1) + ": esperando sem3...");
            success = simulation.sem3.tryAcquire(5, TimeUnit.SECONDS);
        }

        if (!success) {
            throw new InterruptedException("Sem3 no se liberó después de varios intentos.");
        }
    }

    private void procesarEstadoFinal() {
        try {
            if (simulation.simulated_objects != null && simulation.simulated_objects.length > 5) {
                estado = ((NewSim) simulation.simulated_objects[5]).getStat(true);
            } else {
                logManager.logWarning("No se encontró el objeto NewSim en simulation.simulated_objects[5].");
                estado = DEFAULT_ESTADO;
            }
        } catch (ClassCastException e) {
            logManager.logError("Error al convertir a NewSim: " + e.getMessage());
            estado = DEFAULT_ESTADO;
        }
    }

    private void manejarErrorDescripcion() {
        logManager.logError("Error en el archivo de descripción: " + dscFile);
        if (robots != null && robots.length >= 6) {
            logManager.logError("Robot[0]: " + robots[0].controlsystem);
            logManager.logError("Robot[5]: " + robots[5].controlsystem);
        } else {
            logManager.logError("Cantidad insuficiente de robots configurados.");
        }
        if (simulation != null) simulation.parada = true;
        estado = DEFAULT_ESTADO;
    }

    private void manejarErrorEjecucion(Exception e) {
        logManager.logError("Error durante la simulación: " + e.getMessage());
        estado = DEFAULT_ESTADO;
    }
}