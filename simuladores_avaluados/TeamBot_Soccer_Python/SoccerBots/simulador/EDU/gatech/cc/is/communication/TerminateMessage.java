/*
 * TerminateMessage.java
 */

package EDU.gatech.cc.is.communication;

import java.io.Serializable;


/**
 * A message to tell the receiving process to kill itself.
 * <p>
 * <A HREF="../COPYRIGHTTB.html">Copyright</A>
 * (c)1998 Tucker Balch
 *
 * @author Tucker Balch
 * @version $Revision: 1.1 $
 */

public class TerminateMessage extends Message
        implements Cloneable, Serializable {
    /**
     * create a TerminateMessage with default values.
     */
    public TerminateMessage() {
        super();
    }

    /**
     * test the TerminateMessage class.
     */
    public static void main(String[] args) {
        TerminateMessage fred = new TerminateMessage();
        System.out.println("The original message:");
        System.out.println(fred);
    }

    /**
     * return a printable String representation of the TerminateMessage.
     *
     * @return the String representation
     */
    public String paramString() {
        return (super.paramString());
    }
}
