/* Author: Paul N. Hilfinger.  (C) 2008. */

package ataxx;

import org.junit.Test;
import static org.junit.Assert.*;

import static ataxx.Move.*;

/** Test Move creation.
 *  @author P. N. Hilfinger
 */
public class MoveTest {

    @Test
    public void testPass() {
        assertTrue("bad pass", pass() != null && pass().isPass());
        assertEquals("bad pass string", "-", pass().toString());
    }

    @Test
    public void testMove() {
        Move m = move('a', '3', 'b', '2');
        assertNotNull(m);
        assertFalse("move is pass", m.isPass());
        assertTrue("move not extend", m.isExtend());
    }

    @Test
    public void testJump() {
        Move m = move('b', '2', 'b', '4');
        assertNotNull(m);
        assertFalse("move is pass", m.isPass());
        assertFalse("move is extend", m.isExtend());
        assertTrue("move is jump", m.isJump());
    }

    @Test
    public void testIsExtend() {
        Move m = move('b', '4', 'b', '5');
        assertTrue("move is extend", m.isExtend());
        assertFalse("move is pass", m.isPass());
    }


}
