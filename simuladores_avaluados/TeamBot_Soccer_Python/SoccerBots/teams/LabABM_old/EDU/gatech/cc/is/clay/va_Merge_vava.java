/*
 * va_Merge_vava.java
 */

package EDU.gatech.cc.is.clay;

import EDU.gatech.cc.is.util.Vec2;

/**
 * Merge two Vec2Arrays.
 * <p>
 * For detailed information on how to configure behaviors, see the
 * <A HREF="../clay/docs/index.html">Clay page</A>.
 * <p>
 * <A HREF="../COPYRIGHT.html">Copyright</A>
 * (c)1997, 1998 Tucker Balch
 *
 * @author Tucker Balch
 * @version $Revision: 1.1 $
 */

public class va_Merge_vava extends NodeVec2Array {
    public static final boolean DEBUG = Node.DEBUG;
    private final NodeVec2Array embedded1;
    private final NodeVec2Array embedded2;
    Vec2[] last_val = new Vec2[0];
    long lasttime = 0;

    /**
     * Instantiate a va_Merge_vava schema.
     *
     * @param im1 NodeVec2Array, embedded node that generates a list
     *            of items to merge.
     * @param im2 NodeVec2Array, the other embedded node that generates a list
     *            of items to merge.
     */
    public va_Merge_vava(NodeVec2Array im1, NodeVec2Array im2) {
        if (DEBUG) System.out.println("va_Merge_vava: instantiated.");
        embedded1 = im1;
        embedded2 = im2;
    }

    /**
     * Return a Vec2Array that is the merge of two others.
     *
     * @param timestamp long, only get new information if timestamp > than last call
     *                  or timestamp == -1.
     * @return the merged list.
     */
    public Vec2[] Value(long timestamp) {
        if ((timestamp > lasttime) || (timestamp == -1)) {
            /*--- reset the timestamp ---*/
            if (timestamp > 0) lasttime = timestamp;

            /*--- get info from imbedded schemas ---*/
            Vec2[] im1 = embedded1.Value(timestamp);
            Vec2[] im2 = embedded2.Value(timestamp);
            last_val = new Vec2[im1.length + im2.length];

            /*--- merge ---*/
            System.arraycopy(im1, 0, last_val, 0, im1.length);
            System.arraycopy(im2, im1.length - im1.length, last_val, im1.length, im1.length + im2.length - im1.length);

        }
        Vec2[] retval = new Vec2[last_val.length];
        for (int i = 0; i < last_val.length; i++)
            retval[i] = new Vec2(last_val[i].x,
                    last_val[i].y);
        return (retval);
    }
}
