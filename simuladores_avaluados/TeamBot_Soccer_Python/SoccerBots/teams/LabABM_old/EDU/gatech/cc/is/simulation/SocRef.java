/*
 * SocRef.java
 */

package EDU.gatech.cc.is.simulation;

/**
 * methods of a Soccer Referee.
 * For this simulation system they are implemented by the ball.
 * That's right, the ball is the referee.
 * <p>
 * Copyriht (c)2000 Tucker Balch
 *
 * @author Tucker Balch
 * @version $Revision: 1.2 $
 */

public interface SocRef {
    /**
     * True if the game is underway.  False means
     * the players should return to their starting
     * positions.
     */
    boolean playBall();

    /**
     * True if the east team gets to kick off this time.
     */
    boolean eastKickOff();

    /**
     * True if the west team gets to kick off this time.
     */
    boolean westKickOff();

    /**
     * True if the east team scored during the last timestep.
     */
    boolean eastJustScored();

    /**
     * True if the west team scored during the last timestep.
     */
    boolean westJustScored();
}

