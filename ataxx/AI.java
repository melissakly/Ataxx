package ataxx;

import java.util.ArrayList;

import static ataxx.PieceColor.*;

/** A Player that computes its own moves.
 *  @author Melissa Ly
 *  with Pseudocode derived from Wikipedia (alpha-beta pruning)
 */
class AI extends Player {

    /** Maximum minimax \\search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 5;
    /** A positsion magnitude indicating a win (for red if positive, blue
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        _lastFoundMove = null;
        Move move = findMove();
        return move;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == RED) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** Used to communicate best moves found by findMove, when asked for. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value >= BETA if SENSE==1,
     *  and minimal value or value <= ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels before using a static estimate. */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        int v = 0;
        int currPlayer = board.numPieces(board.whoseMove());
        int oppPlayer = board.numPieces(board.whoseMove().opposite());
        if (board.gameOver() && (currPlayer > oppPlayer)) {
            return WINNING_VALUE;
        } else if (board.gameOver() && (currPlayer < oppPlayer)) {
            return -WINNING_VALUE;
        } else {
            if (depth == 0) {
                return staticScore(board);
            } else if (sense == 1) {
                v = -INFTY;
                ArrayList<Move> listOfMoves =
                        beginStoreMoves(board, board.whoseMove());
                for (Move move : listOfMoves) {
                    Board copyBoard = new Board(board);
                    copyBoard.makeMove(move);
                    int possible = findMove(copyBoard,
                            depth - 1, false, -1, alpha, beta);
                    if (saveMove && possible > v) {
                        _lastFoundMove = move;
                    }
                    v = Math.max(v, possible);
                    alpha = Math.max(alpha, v);
                    if (beta <= alpha) {
                        break;
                    }
                    return v;
                }
            } else {
                v = INFTY;
                ArrayList<Move> listOfMoves =
                        beginStoreMoves(board, board.whoseMove());
                for (Move move : listOfMoves) {
                    Board copyBoard = new Board(board);
                    copyBoard.makeMove(move);
                    int possible = findMove(copyBoard,
                            depth - 1, false, 1, alpha, beta);
                    if (saveMove && possible < v) {
                        _lastFoundMove = move;
                    }
                    v = Math.min(v, possible);
                    beta = Math.min(beta, v);
                    if (beta <= alpha) {
                        break;
                    }
                    return v;
                }
            }
            return 0;
        }
    }


    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        int currPlayer = board.numPieces(board.whoseMove());
        int oppPlayer = board.numPieces(board.whoseMove().opposite());
        return currPlayer - oppPlayer;
    }


    /** A helper method for that gets move for storedMoves.
     * Where BOARD is iterated through and PLAYER is taken into account.
     * @return Arraylist of moves
     * */
    private ArrayList<Move> beginStoreMoves(Board board, PieceColor player) {
        ArrayList<Move> storedMoves = new ArrayList<>();
        for (char row = '7'; row >= '1'; row--) {
            for (char col = 'a'; col <= 'g'; col++) {
                int index = Board.index(col, row);
                if (board.get(index) == player) {
                    ArrayList<Move> addMoves = storedMoves(board, row, col);
                    storedMoves.addAll(addMoves);
                }
            }
        }
        return storedMoves;
    }
    /** Returns an Arraylist of legal moves using BOARD, ROW, COL
     * and legalMove. */
    private ArrayList<Move> storedMoves(Board board, char row, char col) {
        ArrayList<Move> storedMoves = new ArrayList<>();
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                char row2 = (char) (row + j);
                char col2 = (char) (col + i);
                Move currMove = Move.move(col, row, col2, row2);
                if (board.legalMove(currMove)) {
                    storedMoves.add(currMove);
                }
            }
        }
        return storedMoves;
    }

}
