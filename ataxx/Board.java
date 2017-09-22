package ataxx;

/* Author: P. N. Hilfinger, (C) 2008. */

import java.util.Observable;
import java.util.Stack;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.ArrayList;

import static ataxx.PieceColor.*;
import static ataxx.GameException.error;

/** An Ataxx board.   The squares are labeled by column (a char value between
 *  'a' - 2 and 'g' + 2) and row (a char value between '1' - 2 and '7'
 *  + 2) or by linearized index, an integer described below.  Values of
 *  the column outside 'a' and 'g' and of the row outside '1' to '7' denote
 *  two layers of border squares, which are always blocked.
 *  This artificial border (which is never actually printed) is a common
 *  trick that allows one to avoid testing for edge conditions.
 *  For example, to look at all the possible moves from a square, sq,
 *  on the normal board (i.e., not in the border region), one can simply
 *  look at all squares within two rows and columns of sq without worrying
 *  about going off the board. Since squares in the border region are
 *  blocked, the normal logic that prevents moving to a blocked square
 *  will apply.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author Melissa Ly
 */
class Board extends Observable {

    /** Number of squares on a side of the board. */
    static final int SIDE = 7;
    /** Length of a side + an artificial 2-deep border region. */
    static final int EXTENDED_SIDE = SIDE + 4;

    /** Number of non-extending moves before game ends. */
    static final int JUMP_LIMIT = 25;

    /** A new, cleared board at the start of the game. */
    Board() {
        _board = new PieceColor[EXTENDED_SIDE * EXTENDED_SIDE];
        numBlue = 2;
        numRed = 2;
        numBlocks = 0;
        numEmpty = (SIDE * SIDE) - 4;
        allMoves = new ArrayList<>();
        playerJumps = 0;
        playerPass = 0;
        totalMoves = 0;
        stackUndo = new Stack<>();
        flipPieces = new Stack<>();
        clear();
    }

    /** A copy of B. */
    Board(Board b) {
        _board = b._board.clone();
        _whoseMove = b.whoseMove();
        numBlue = b.bluePieces();
        numRed = b.redPieces();
        numEmpty = b.numEmpty;
        playerJumps = b.numJumps();
        playerPass = b.playerPass();
        allMoves = b.allMoves();
        stackUndo = b.getStackUndo();
        flipPieces = b.getFlipPieces();
    }

    /** Return the linearized index of square COL ROW. */
    static int index(char col, char row) {
        return (row - '1' + 2) * EXTENDED_SIDE + (col - 'a' + 2);
    }

    /** Return the linearized index of the square that is DC columns and DR
     *  rows away from the square with index SQ. */
    static int neighbor(int sq, int dc, int dr) {
        return sq + dc + dr * EXTENDED_SIDE;
    }

    /** Clear me to my starting state, with pieces in their initial
     *  positions and no blocks. */
    void clear() {
        _whoseMove = RED;
        for (char c = 'a'; c <= 'g'; c++) {
            for (char r = '1'; r <= '7'; r++) {
                unrecordedSet(c, r, EMPTY);
            }
        }
        for (int i = 0; i < _board.length; i++) {
            unrecordedSet('a', '7', RED);
            unrecordedSet('g', '1', RED);
            unrecordedSet('a', '1', BLUE);
            unrecordedSet('g', '7', BLUE);
            setChanged();
            notifyObservers();
        }
    }

    /** Return true iff the game is over: i.e., if neither side has
     *  any moves, if one side has no pieces, or if there have been
     *  MAX_JUMPS consecutive jumps without intervening extends. */
    boolean gameOver() {
        boolean jump = JUMP_LIMIT <= playerJumps;
        boolean move = (!canMove(BLUE) && !canMove(RED));
        boolean zero = (numPieces(RED) == 0) || (numPieces(BLUE) == 0);
        return jump || move || zero;
    }

    /** Return number of red pieces on the board. */
    int redPieces() {
        return numPieces(RED);
    }

    /** Return number of blue pieces on the board. */
    int bluePieces() {
        return numPieces(BLUE);
    }

    /** Return number of COLOR pieces on the board. */
    int numPieces(PieceColor color) {
        int counter = 0;
        for (PieceColor piece : _board) {
            if (color == piece) {
                counter += 1;
            }
        }
        return counter;
    }

    /** Increment numPieces(COLOR) by K. */
    private void incrPieces(PieceColor color, int k) {
        if (color.equals(RED)) {
            numRed += k;
        } else if (color.equals(BLUE)) {
            numBlue += k;
        }
    }

    /** The current contents of square CR, where 'a'-2 <= C <= 'g'+2, and
     *  '1'-2 <= R <= '7'+2.  Squares outside the range a1-g7 are all
     *  BLOCKED.  Returns the same value as get(index(C, R)). */
    PieceColor get(char c, char r) {
        return _board[index(c, r)];
    }

    /** Return the current contents of square with linearized index SQ. */
    PieceColor get(int sq) {
        return _board[sq];
    }

    /** Set get(C, R) to V, where 'a' <= C <= 'g', and
     *  '1' <= R <= '7'. */
    private void set(char c, char r, PieceColor v) {
        set(index(c, r), v);
    }

    /** Set square with linearized index SQ to V.  This operation is
     *  undoable. */
    private void set(int sq, PieceColor v) {
        addUndo(sq, v);
        unrecordedSet(sq, v);
        incrPieces(v, 1);
    }

    /** Set square at C R to V (not undoable). */
    private void unrecordedSet(char c, char r, PieceColor v) {
        _board[index(c, r)] = v;
    }

    /** Set square at linearized index SQ to V (not undoable). */
    private void unrecordedSet(int sq, PieceColor v) {
        _board[sq] = v;
    }

    /** Return true iff MOVE is legal on the current board. */
    boolean legalMove(Move move) {
        if (move == null) {
            return false;
        } else {
            PieceColor curr = get(move.fromIndex());
            PieceColor opp = get(move.toIndex());
            if (move.isPass()) {
                return !canMove(_whoseMove);
            }
            if (curr != _whoseMove) {
                return false;
            }
            if (opp == null || opp != EMPTY) {
                return false;
            }
            if (!move.isExtend() && !move.isJump()) {
                return false;
            }
        }
        return true;
    }

    /** Return true iff player WHO can move, ignoring whether it is
     *  that player's move and whether the game is over. */
    boolean canMove(PieceColor who) {
        for (char row = '7'; row >= '1'; row--) {
            for (char col = 'a'; col <= 'g'; col++) {
                int index = index(col, row);
                if (get(index) == who) {
                    for (int i = -2; i <= 2; i++) {
                        for (int j = -2; j <= 2; j++) {
                            char row2 = (char) (row + j);
                            char col2 = (char) (col + i);
                            Move currMove = Move.move(col, row, col2, row2);
                            if (legalMove(currMove)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /** Return the color of the player who has the next move.  The
     *  value is arbitrary if gameOver(). */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Return total number of moves and passes since the last
     *  clear or the creation of the board. */
    int numMoves() {
        return totalMoves;
    }

    /** Return number of non-pass moves made in the current game since the
     *  last extend move added a piece to the board (or since the
     *  start of the game). Used to detect end-of-game. */
    int numJumps() {
        return playerJumps;
    }

    /** Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     *  other than pass, assumes that legalMove(C0, R0, C1, R1). */
    void makeMove(char c0, char r0, char c1, char r1) {
        if (c0 == '-') {
            makeMove(Move.pass());
        } else {
            makeMove(Move.move(c0, r0, c1, r1));
        }
    }

    /** Make the MOVE on this Board, assuming it is legal. */
    void makeMove(Move move) {
        if (move == null) {
            throw new GameException("Null move.");
        }
        allMoves.add(move);
        if (move.isPass()) {
            pass();
            return;
        }
        if (!legalMove(move)) {
            throw new GameException("ILLEGAL");
        } else if (move.isJump()) {
            startUndo();
            set(move.toIndex(), _whoseMove);
            set(move.fromIndex(), EMPTY);
            flipPieces(move, _whoseMove);
            playerJumps += 1;
            totalMoves += 1;
        } else if (move.isExtend()) {
            startUndo();
            set(move.toIndex(), _whoseMove);
            flipPieces(move, _whoseMove);
            playerJumps = 0;
            totalMoves += 1;
        } else {
            throw new GameException("Illegal move.");
        }
        PieceColor opponent = _whoseMove.opposite();
        _whoseMove = opponent;
        setChanged();
        notifyObservers();
    }

    /** Update to indicate that the current player passes, assuming it
     *  is legal to do so.  The only effect is to change whoseMove(). */
    void pass() {
        assert !canMove(_whoseMove);
        if (canMove(_whoseMove)) {
            throw new GameException("Cannot pass");
        }
        PieceColor opponent = _whoseMove.opposite();
        _whoseMove = opponent;
        setChanged();
        notifyObservers();
    }

    /** Undo the last move. */
    void undo() {
        HashMap<Integer, PieceColor> newMove = stackUndo.pop();
        for (Integer move : newMove.keySet()) {
            unrecordedSet(move, newMove.get(move));
        }
        totalMoves -= 1;
        _whoseMove = _whoseMove.opposite();
        setChanged();
        notifyObservers();

    }

    /** Getter method for my Stack of Integers and Piececolors.
     * @return stack of hashmaps
     * */
    public Stack<HashMap<Integer, PieceColor>> getStackUndo() {
        return stackUndo;
    }

    /** Getter method for my Stack of Integers and Piececolors.
     * @return an arralist of flipped pieces
     * */
    public Stack<ArrayList<Integer>> getFlipPieces() {
        return flipPieces;
    }


    /** Changes the color of surrounding pieces in
     * accordance to the MOVE and PLAYER. */
    private void flipPieces(Move move, PieceColor player) {
        int index = move.toIndex();
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                int neigh = neighbor(index, i, j);
                if (get(neigh) == whoseMove().opposite()) {
                    set(neigh, _whoseMove);
                    incrPieces(whoseMove().opposite(), -1);
                }
            }
        }
    }

    /** Indicate beginning of a move in the undo stack. */
    private void startUndo() {
        HashMap<Integer, PieceColor> beginMove = new HashMap<>();
        stackUndo.push(beginMove);
    }

    /** Add an undo action for changing SQ to NEWCOLOR on current
     *  board. */
    private void addUndo(int sq, PieceColor newcolor) {
        if (stackUndo.peek().get(sq) != null && newcolor == EMPTY) {
            stackUndo.peek().put(sq, EMPTY);
        } else {
            stackUndo.peek().put(sq, get(sq));
        }
    }

    /** Return true iff it is legal to place a block at C R. */
    boolean legalBlock(char c, char r) {
        char col = (char) ('h' - (c - 'a' + 1));
        char row = (char) ('8' - (r - '0'));
        return !(get(col, row) != EMPTY && get(c, row) != EMPTY
                && get(col, r) != EMPTY && get(c, r) != EMPTY);
    }


    /** Return true iff it is legal to place a block at CR. */
    boolean legalBlock(String cr) {
        return legalBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Set a block on the square C R and its reflections across the middle
     *  row and/or column, if that square is unoccupied and not
     *  in one of the corners. Has no effect if any of the squares is
     *  already occupied by a block.  It is an error to place a block on a
     *  piece. */
    void setBlock(char c, char r) {
        if (!legalBlock(c, r)) {
            throw error("illegal block placement");
        }
        char col = (char) ('h' - (c - 'a' + 1));
        char row = (char) ('8' - (r - '0'));
        unrecordedSet(c, r, BLOCKED);
        unrecordedSet(c, row, BLOCKED);
        unrecordedSet(col, row, BLOCKED);
        unrecordedSet(col, r, BLOCKED);
        setChanged();
        notifyObservers();
    }

    /** Place a block at CR. */
    void setBlock(String cr) {
        setBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Return a list of all moves made since the last clear (or start of
     *  game). */
    List<Move> allMoves() {
        return allMoves;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    @Override
    public boolean equals(Object obj) {
        Board other = (Board) obj;
        return Arrays.equals(_board, other._board);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(_board);
    }

    /** Return a text depiction of the board (not a dump).  If LEGEND,
     *  supply row and column numbers around the edges. */
    String toString(boolean legend) {
        Formatter out = new Formatter();
        out.format("===");
        if (legend) {
            out.format("    " + "a b c d e f g");
        }
        for (char i = '7'; i > '0'; i--) {
            out.format("\n" + " ");
            if (legend) {
                out.format(Character.toString(i) + " ");
            } else {
                for (char m = 'a'; m <= 'g'; m++) {
                    if (_board[index(m, i)] == BLOCKED) {
                        out.format(" X");
                    } else if (_board[index(m, i)] == RED) {
                        out.format(" r");
                    } else if (_board[index(m, i)] == BLUE) {
                        out.format(" b");
                    } else {
                        out.format(" -");
                    }
                }
            }
        }
        out.format("\n" + "===%n");
        return out.toString();
    }

    /** Getter method of number of Passes.
     * @return number of passes (unneeded)
     * */
    public int playerPass() {
        return playerPass;
    }


    /** For reasons of efficiency in copying the board,
     *  we use a 1D array to represent it, using the usual access
     *  algorithm: row r, column c => index(r, c).
     *
     *  Next, instead of using a 7x7 board, we use an 11x11 board in
     *  which the outer two rows and columns are blocks, and
     *  row 2, column 2 actually represents row 0, column 0
     *  of the real board.  As a result of this trick, there is no
     *  need to special-case being near the edge: we don't move
     *  off the edge because it looks blocked.
     *
     *  Using characters as indices, it follows that if 'a' <= c <= 'g'
     *  and '1' <= r <= '7', then row c, column r of the board corresponds
     *  to board[(c -'a' + 2) + 11 (r - '1' + 2) ], or by a little
     *  re-grouping of terms, board[c + 11 * r + SQUARE_CORRECTION]. */
    private final PieceColor[] _board;

    /** Player that is on move. */
    private PieceColor _whoseMove;

    /** Keeps track of the total amount of moves in a game. */
    private int totalMoves;


    /** HashMap used to implement undo. */
    private Stack<HashMap<Integer, PieceColor>> stackUndo;

    /** A list of moves made during the game. */
    private List<Move> allMoves;

    /** A stack that tracks the pieces that change color. */
    private Stack<ArrayList<Integer>> flipPieces;

    /** Number of red pieces. */
    private int numRed;

    /** Number of blue pieces. */
    private int numBlue;

    /** Number of passes. */
    private int playerPass;

    /** Number of jumps. */
    private int playerJumps;

    /** Number of blocks. */
    private int numBlocks;

    /** Number of empty squares. */
    private int numEmpty;

}

