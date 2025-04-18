/*
 * Transceiver.java
 */

package EDU.gatech.cc.is.communication;

import EDU.gatech.cc.is.util.CircularBufferEnumeration;


/**
 * The Transceiver class provides an abstract interface to the
 * hardware of a robot that can communicate.
 * <p>
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1998 Georgia Tech Research Corporation
 *
 * @author Tucker Balch
 * @version $Revision: 1.1 $
 */

public interface Transceiver {
    /**
     * Broadcast a message to all teammates, except self.
     *
     * @param m Message, the message to be broadcast.
     */
    void broadcast(Message m);


    /**
     * Transmit a message to just one teammate.  Transmission to
     * self is allowed.
     *
     * @param id int, the ID of the agent to receive the message.
     * @param m  Message, the message to transmit.
     * @throws CommunicationException if the receiving agent does not
     *                                exist.
     */
    void unicast(int id, Message m)
            throws CommunicationException;


    /**
     * Transmit a message to specific teammates.  Transmission to
     * self is allowed.
     *
     * @param ids int[], the IDs of the agents to receive the message.
     * @param m   Message, the message to transmit.
     * @throws CommunicationException if one of the receiving agents
     *                                does not exist.
     */
    void multicast(int[] ids, Message m)
            throws CommunicationException;


    /**
     * Get an enumeration of the incoming messages.  The messages
     * are automatically buffered by the implementation.
     * Unless the implementation guarantees it, you cannot
     * count on all messages being delivered.
     * Example, to print all incoming messages:
     * <PRE>
     * Transceiver c = new RobotComm();
     * Enumeration r = c.getReceiveChannel();
     * while (r.hasMoreElements())
     * System.out.println(r.nextElement());
     * </PRE>
     *
     * @return the Enumeration.
     */
    CircularBufferEnumeration getReceiveChannel();


    /**
     * Set the maximum range at which communication can occur.
     * In simulation, this corresponds to a simulation of physical limits,
     * on mobile robots it corresponds to a signal strength setting.
     *
     * @param r double, the maximum range.
     */
    void setCommunicationMaxRange(double r);


    /**
     * Check to see if the transceiver is connected to the server.
     */
    boolean connected();


    /**
     * Terminate the connection.
     */
    void quit();
}
