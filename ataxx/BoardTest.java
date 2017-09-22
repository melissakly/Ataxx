package ataxx;

import org.junit.Test;
import static org.junit.Assert.*;

/** Tests of the Board class.
 *  @author Melissa Ly
 */
public class BoardTest {

    private static final String[]
        GAME1 = { "a7-b7", "a1-a2",
                  "a7-a6", "a2-a3",
                  "a6-a5", "a3-a4" };

    private static void makeMoves(Board b, String[] moves) {
        for (String s : moves) {
            b.makeMove(s.charAt(0), s.charAt(1),
                       s.charAt(3), s.charAt(4));
        }
    }

    @Test public void testUndo() {
        Board b0 = new Board();
        Board b1 = new Board(b0);
        makeMoves(b0, GAME1);
        Board b2 = new Board(b0);
        for (int i = 0; i < GAME1.length; i += 1) {
            b0.undo();
        }
        b0.toString(true);
        assertEquals("failed to return to start", b1, b0);
        makeMoves(b0, GAME1);
        assertEquals("second pass failed to reach same position", b2, b0);
    }

    @Test public void flipPieces() {
        Board b6 = new Board();
        b6.makeMove('a', '7', 'c', '6');
        b6.makeMove('g', '7', 'e', '6');
    }

    @Test public void testClear() {
        Board b = new Board();
        assertEquals(b.whoseMove(), PieceColor.RED);
        assertEquals(b.redPieces(), 2);
        assertEquals(b.bluePieces(), 2);
        assertEquals(true, b.canMove(PieceColor.RED));
        assertEquals(2, b.numPieces(PieceColor.RED));
    }

    @Test public void testBlock() {
        Board board = new Board();
        assertEquals(false, board.legalBlock('a', '1'));
        assertEquals(true, board.legalBlock('b', '1'));
        assertEquals(false, board.legalBlock('g', '1'));
        board.setBlock('c', '6');
        assertEquals(PieceColor.BLOCKED, board.get('c', '6'));
    }


    @Test public void testIndex() {
        Board b1 = new Board();
        assertEquals(24, b1.index('a', '1'));
        assertEquals(25, b1.index('b', '1'));
        assertEquals(93, b1.index('d', '7'));
        assertEquals(30, b1.index('g', '1'));
    }



}
