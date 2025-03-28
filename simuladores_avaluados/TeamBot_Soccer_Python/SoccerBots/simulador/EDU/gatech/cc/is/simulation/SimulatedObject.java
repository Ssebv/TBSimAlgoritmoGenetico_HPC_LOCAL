/*
 * SimulatedObject.java
 */

package EDU.gatech.cc.is.simulation;

import EDU.cmu.cs.coral.util.Circle2;
import EDU.cmu.cs.coral.util.Polygon2;
import EDU.gatech.cc.is.communication.Message;
import EDU.gatech.cc.is.util.Vec2;

import java.awt.*;


/**
 * If you want to include a new object for TeamBots simulation,
 * you must implement  this interface.
 * <p>
 * Most of these methods are used by other simulated objects to either
 * generate simulated sensor values or reproduce accurate dynamic results.
 * <p>
 * Copyright (c)2000 Tucker Balch
 *
 * @author Tucker Balch
 * @version $Revision: 1.3 $
 */

public interface SimulatedObject {
    /**
     * Initialize a simulated object.  Called automatically by
     * JavaBotSim.
     *
     * @param x  the initial x position.
     * @param y  the initial y position.
     * @param t  orientation.
     * @param r  radius.
     * @param fg the foreground color of the object when drawn.
     * @param bg the background color of the object when drawn.
     * @param vc the vision class of the object - for use
     *           by simulated vision.
     * @param id a unique ID number fore the object.
     * @param s  random number seed.
     */
    void init(double x, double y, double t, double r,
              Color fg, Color bg, int vc, int id, long s);


    /**
     * Take a simulated step.
     *
     * @param time_increment how much time has elapsed since
     *                       the last call.
     * @param all_objects    the other objects in the simulation.
     */
    void takeStep(long time_increment,
                  SimulatedObject[] all_objects);


    /**
     * true if the object should be considered an obstacle, false otherwise.
     *
     * @return true if the object should be considered an obstacle,
     * false otherwise.
     */
    boolean isObstacle();


    /**
     * true if the object is pushable false otherwise.
     *
     * @return true if the object is pushable, false otherwise.
     */
    boolean isPushable();


    /**
     * true if the object can be picked up, false otherwise.
     *
     * @return true if the object can be picked up, false otherwise.
     */
    boolean isPickupable();


    /**
     * Find the closest point on the object from a particular location.
     * This is useful for obstacle avoidance and so on.
     *
     * @param from the place from which the point is determined.
     * @return the closest point.
     */
    Vec2 getClosestPoint(Vec2 from);

    /**
     * determine if the object is intersecting with a specified circle.
     * This is useful for obstacle avoidance and so on.
     *
     * @param c the circle which may be intersecting the current object.
     * @return true if collision detected.
     */
    boolean checkCollision(Circle2 c);

    /**
     * determine if the object is intersecting with a specified polygon.
     * This is useful for obstacle avoidance and so on.
     *
     * @param p the polygon which may be intersecting the current object.
     * @return true if collision detected.
     */
    boolean checkCollision(Polygon2 p);


    /**
     * Find the center point of the object in absolute coordinates.
     *
     * @return the point.
     */
    Vec2 getPosition();


    /**
     * Find the center point of the object from a particular location.
     *
     * @param from the place from which the point is determined.
     * @return the center point.
     */
    Vec2 getCenter(Vec2 from);


    /**
     * Try to push the object.
     *
     * @param d the direction and distance of the push.
     * @param v the velocity of the push.
     */
    void push(Vec2 d, Vec2 v);


    /**
     * Try to pick up the object.
     *
     * @param o the object picking it up - this is used for
     *          drawing purposes.
     */
    void pickUp(SimulatedObject o);


    /**
     * Try to put down the object.
     *
     * @param p the location of deposit.
     */
    void putDown(Vec2 p);

    /**
     * Return the vision class of the object.
     *
     * @param v the new vision class.
     */
    int getVisionClass();

    /**
     * Change the way the object is perceived by vision hardware.
     * This can be used to make old targets invisible (a simulation
     * hack.... sorry).
     *
     * @param v the new vision class.
     */
    void setVisionClass(int v);

    /**
     * Return the id.
     */
    int getID();


    /**
     * Set the id of the object.
     *
     * @param id the new id.
     */
    void setID(int id);


    /**
     * Receive a message.  This is principally for robot to robot
     * communication.  Most objects can safely ignore this.
     *
     * @param m the message.
     */
    void receive(Message m);


    /**
     * Clean up.
     */
    void quit();


    /**
     * Draw the object.
     *
     * @param g graphics area to draw the object.
     * @param w the width in pixels of g.
     * @param h the height in pixels of g.
     * @param t the y coordinate represented by the top boundary of
     *          the drawing area.
     * @param b the y coordinate represented by the bottom boundary of
     *          the drawing area.
     * @param l the x coordinate represented by the left boundary of
     *          the drawing area.
     * @param r the x coordinate represented by the right boundary of
     *          the drawing area.
     */
    void draw(Graphics g, int w, int h,
              double t, double b, double l, double r);


    /**
     * Draw the object as an icon.
     *
     * @param g graphics area to draw the object.
     * @param w the width in pixels of g.
     * @param h the height in pixels of g.
     * @param t the y coordinate represented by the top boundary of
     *          the drawing area.
     * @param b the y coordinate represented by the bottom boundary of
     *          the drawing area.
     * @param l the x coordinate represented by the left boundary of
     *          the drawing area.
     * @param r the x coordinate represented by the right boundary of
     *          the drawing area.
     */
    void drawIcon(Graphics g, int w, int h,
                  double t, double b, double l, double r);


    /**
     * Draw the object's ID.
     *
     * @param g graphics area to draw the object.
     * @param w the width in pixels of g.
     * @param h the height in pixels of g.
     * @param t the y coordinate represented by the top boundary of
     *          the drawing area.
     * @param b the y coordinate represented by the bottom boundary of
     *          the drawing area.
     * @param l the x coordinate represented by the left boundary of
     *          the drawing area.
     * @param r the x coordinate represented by the right boundary of
     *          the drawing area.
     */
    void drawID(Graphics g, int w, int h,
                double t, double b, double l, double r);


    /**
     * Draw the object's trail.
     *
     * @param g graphics area to draw the object.
     * @param w the width in pixels of g.
     * @param h the height in pixels of g.
     * @param t the y coordinate represented by the top boundary of
     *          the drawing area.
     * @param b the y coordinate represented by the bottom boundary of
     *          the drawing area.
     * @param l the x coordinate represented by the left boundary of
     *          the drawing area.
     * @param r the x coordinate represented by the right boundary of
     *          the drawing area.
     */
    void drawTrail(Graphics g, int w, int h,
                   double t, double b, double l, double r);


    /**
     * Set the length of the trail (in movement steps).
     * Non-robots can ignore this.
     *
     * @param l int, the length of the trail.
     */
    void setTrailLength(int l);


    /**
     * Clear the trail.
     * Non-robots can ignore this.
     */
    void clearTrail();


    /**
     * Draw the object's state.
     *
     * @param g graphics area to draw the object.
     * @param w the width in pixels of g.
     * @param h the height in pixels of g.
     * @param t the y coordinate represented by the top boundary of
     *          the drawing area.
     * @param b the y coordinate represented by the bottom boundary of
     *          the drawing area.
     * @param l the x coordinate represented by the left boundary of
     *          the drawing area.
     * @param r the x coordinate represented by the right boundary of
     *          the drawing area.
     */
    void drawState(Graphics g, int w, int h,
                   double t, double b, double l, double r);


    /**
     * Draw the object in a specific spot, regardless of where it really
     * is.
     *
     * @param p location to draw it at.
     * @param g graphics area to draw the object.
     * @param w the width in pixels of g.
     * @param h the height in pixels of g.
     * @param t the y coordinate represented by the top boundary of
     *          the drawing area.
     * @param b the y coordinate represented by the bottom boundary of
     *          the drawing area.
     * @param l the x coordinate represented by the left boundary of
     *          the drawing area.
     * @param r the x coordinate represented by the right boundary of
     *          the drawing area.
     */
    void draw(Vec2 p, Graphics g, int w, int h,
              double t, double b, double l, double r);
}

