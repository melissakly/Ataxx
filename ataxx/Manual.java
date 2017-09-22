package ataxx;

import static ataxx.PieceColor.*;
import static ataxx.GameException.error;

/** A Player that receives its moves from its Game's getMoveCmnd method.
 *  @author Melissa Ly
 */
class Manual extends Player {

    /** A Player that will play MYCOLOR on GAME, taking its moves from
     *  GAME. */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        Command cmd = game().getMoveCmnd(myColor() + ":");
        if (cmd == null) {
            return null;
        }
        String[] myMove = cmd.operands();
        char col0 = myMove[0].charAt(0);
        char row0 = myMove[1].charAt(0);
        char col1 = myMove[2].charAt(0);
        char row1 = myMove[3].charAt(0);
        Move move = Move.move(col0, row0, col1, row1);
        if ((move == null) || !board().legalMove(move)) {
            throw error("Manual.java: Cannot move here");
        }
        return move;
    }

}

