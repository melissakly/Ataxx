package ataxx;

/* Author: P. N. Hilfinger */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;

import static ataxx.PieceColor.*;
import static ataxx.Game.State.*;
import static ataxx.Command.Type.*;
import static ataxx.GameException.error;

/** Controls the play of the game.
 *  @author Melissa Ly
 */
class Game {

    /** States of play. */
    enum State {
        SETUP, PLAYING, FINISHED
    }

    /** A new Game, using BOARD to play on, reading initially from
     *  BASESOURCE and using REPORTER for error and informational messages. */
    Game(Board board, CommandSource baseSource, Reporter reporter) {
        _inputs.addSource(baseSource);
        _board = board;
        _reporter = reporter;
        _bluePlayer = new AI(this, BLUE);
        _redPlayer = new Manual(this, RED);
        aiBlue = true;
        aiRed = false;
    }

    /** Run a session of Ataxx gaming.  Use an AtaxxGUI iff USEGUI. */
    void process(boolean useGUI) {
        Player red, blue, current;

        red = blue = current = null;
        _state = SETUP;

        GameLoop:
        while (true) {
            doClear(null);

            SetupLoop:
            while (_state == SETUP) {
                try {
                    doCommand();
                } catch (GameException excp)  {
                    _reporter.errMsg(excp.getMessage());
                }
            }
            if (!aiRed && aiBlue) {
                red = new Manual(this, RED);
                blue = new AI(this, BLUE);
            } else if (aiRed && aiBlue) {
                red = new AI(this, RED);
                blue = new AI(this, BLUE);
            } else if (aiRed && !aiBlue) {
                red = new AI(this, RED);
                blue = new Manual(this, BLUE);
            } else {
                red = new Manual(this, RED);
                blue = new Manual(this, BLUE);
            }
            _state = PLAYING;
            while (_state != SETUP && !_board.gameOver()) {
                try {
                    Move move;
                    Player curr;
                    boolean report = false;
                    if (_board.whoseMove() == RED) {
                        move = red.myMove();
                        if (aiRed) {
                            report = true;
                        }
                        curr = red;
                    } else {
                        move = blue.myMove();
                        if (aiBlue) {
                            report = true;
                        }
                        curr = blue;
                        if (move == null) {
                            break;
                        }
                    }
                    if (_state == PLAYING) {
                        if (move == null) {
                            move = Move.pass();
                        }
                        if (move.isPass()) {
                            if (_board.whoseMove() == RED && aiRed) {
                                reportMove("Red passes.");
                            } else if (_board.whoseMove() == BLUE && aiBlue) {
                                reportMove("Blue passes.");
                            }
                        }
                        _board.makeMove(move);
                        if (report) {
                            _reporter.outcomeMsg(curr.myColor()
                                    + " moves " + move + ".");
                        }
                    }
                } catch (GameException exp) {
                    _reporter.errMsg(exp.getMessage());
                }
            }
            if (_state != SETUP) {
                reportWinner();
            }
            if (_state == PLAYING) {
                _state = FINISHED;
            }
            while (_state == FINISHED) {
                doCommand();
            }
        }

    }

    /** Return a view of my game board that should not be modified by
     *  the caller. */
    Board board() {
        return _board;
    }

    /** Perform the next command from our input source. */
    void doCommand() {
        try {
            Command cmnd =
                Command.parseCommand(_inputs.getLine("ataxx: "));
            _commands.get(cmnd.commandType()).accept(cmnd.operands());
        } catch (GameException excp) {
            _reporter.errMsg(excp.getMessage());
        }
    }

    /** Read and execute commands until encountering a move or until
     *  the game leaves playing state due to one of the commands. Return
     *  the terminating move command, or null if the game first drops out
     *  of playing mode. If appropriate to the current input source, use
     *  PROMPT to prompt for input. */
    Command getMoveCmnd(String prompt) {
        while (_state == PLAYING) {
            try {
                Command cmnd = Command.parseCommand(_inputs.getLine(prompt));
                if (cmnd.commandType() == PIECEMOVE) {
                    return cmnd;
                } else if (cmnd.commandType() == PASS) {
                    doPass(cmnd.operands());
                } else if (cmnd.commandType() == LOAD) {
                    doLoad(cmnd.operands());
                } else if (cmnd.commandType() == CLEAR) {
                    doClear(cmnd.operands());
                } else if (cmnd.commandType() == HELP) {
                    doHelp(cmnd.operands());
                } else if (cmnd.commandType() == DUMP) {
                    doDump(cmnd.operands());
                } else if (cmnd.commandType() == QUIT) {
                    doDump(cmnd.operands());
                } else if (cmnd.commandType() == ERROR) {
                    doError(cmnd.operands());
                    throw new GameException("error, try again");
                } else if (cmnd.commandType() == EOF) {
                    System.out.println("End of input");
                } else {
                    throw new GameException("get move command error");
                }
            } catch (GameException excp) {
                _reporter.errMsg(excp.getMessage());
            }
        }
        return null;
    }

    /** Return random integer between 0 (inclusive) and MAX>0 (exclusive). */
    int nextRandom(int max) {
        return _randoms.nextInt(max);
    }

    /** Report a move, using a message formed from FORMAT and ARGS as
     *  for String.format. */
    void reportMove(String format, Object... args) {
        _reporter.moveMsg(format, args);
    }

    /** Report an error, using a message formed from FORMAT and ARGS as
     *  for String.format. */
    void reportError(String format, Object... args) {
        _reporter.errMsg(format, args);
    }

    /* Command Processors */

    /** Perform the command 'auto OPERANDS[0]'. */
    void doAuto(String[] operands) {
        checkState("auto", SETUP);
        if (operands[0].equalsIgnoreCase("red")) {
            aiRed = true;
        } else {
            aiBlue = true;
        }
    }

    /** Perform a 'help' command. */
    void doHelp(String[] unused) {
        InputStream helpIn =
            Game.class.getClassLoader().getResourceAsStream("ataxx/help.txt");
        if (helpIn == null) {
            System.err.println("No help available.");
        } else {
            try {
                BufferedReader r
                    = new BufferedReader(new InputStreamReader(helpIn));
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(line);
                }
                r.close();
            } catch (IOException e) {
                /* Ignore IOException */
            }
        }
    }

    /** Perform the command 'load OPERANDS[0]'. */
    void doLoad(String[] operands) {
        try {
            FileReader reader = new FileReader(operands[0]);
            ReaderSource parse = new ReaderSource(reader, true);
            _inputs.addSource(parse);
        } catch (IOException e) {
            throw error("Cannot open file %s", operands[0]);
        }
    }

    /** Perform the command 'manual OPERANDS[0]'. */
    void doManual(String[] operands) {
        checkState("manual", SETUP);
        if (operands[0].equalsIgnoreCase("blue")) {
            aiBlue = false;
        } else {
            aiRed = false;
        }
    }

    /** Exit the program. */
    void doQuit(String[] unused) {
        System.exit(0);
    }

    /** Perform the command 'start'. */
    void doStart(String[] unused) {
        checkState("start", SETUP);
        _state = PLAYING;
    }

    /** Perform the move OPERANDS[0]. */
    void doMove(String[] operands) {
        try {
            char col0 = operands[0].charAt(0);
            char row0 = operands[1].charAt(0);
            char col1 = operands[2].charAt(0);
            char row1 = operands[3].charAt(0);
            _board.makeMove(col0, row0, col1, row1);
        } catch (GameException excp) {
            _reporter.errMsg(excp.getMessage());
        }
    }

    /** Cause current player to pass. */
    void doPass(String[] unused) {
        checkState("pass", PLAYING);
        _board.makeMove(Move.PASS);
    }

    /** Perform the command 'clear'. */
    void doClear(String[] unused) {
        _board.clear();
        _state = SETUP;
    }

    /** Perform the command 'dump'. */
    void doDump(String[] unused) {
        System.out.print(_board.toString());
    }

    /** Execute 'seed OPERANDS[0]' command, where the operand is a string
     *  of decimal digits. Silently substitutes another value if
     *  too large. */
    void doSeed(String[] operands) {
    }


    /** Execute the command 'block OPERANDS[0]'. */
    void doBlock(String[] operands) {
        checkState("block", SETUP);
        if (!(operands.length == 1)) {
            throw new GameException("not in setup mode!");
        }
        char block1 = operands[0].charAt(0);
        char block2 = operands[0].charAt(1);
        _board.setBlock(block1, block2);
    }

    /** Execute the artificial 'error' command. */
    void doError(String[] unused) {
        throw error("Command not understood");
    }

    /** Report the outcome of the current game. */
    void reportWinner() {
        String msg;
        msg = "Draw.";
        int numRed = _board.redPieces();
        int numBlue = _board.bluePieces();
        if (numRed > numBlue) {
            _reporter.outcomeMsg("Red wins.");
        } else if (numBlue > numRed) {
            _reporter.outcomeMsg("Blue wins.");
        } else {
            _reporter.outcomeMsg(msg);
        }
    }

    /** Check that game is currently in one of the states STATES, assuming
     *  CMND is the command to be executed. */
    private void checkState(Command cmnd, State... states) {
        for (State s : states) {
            if (s == _state) {
                return;
            }
        }
        throw error("'%s' command is not allowed now.", cmnd.commandType());
    }

    /** Check that game is currently in one of the states STATES, using
     *  CMND in error messages as the name of the command to be executed. */
    private void checkState(String cmnd, State... states) {
        for (State s : states) {
            if (s == _state) {
                return;
            }
        }
        throw error("'%s' command is not allowed now.", cmnd);
    }

    /** Mapping of command types to methods that process them. */
    private final HashMap<Command.Type, Consumer<String[]>> _commands =
        new HashMap<>();

    {
        _commands.put(AUTO, this::doAuto);
        _commands.put(BLOCK, this::doBlock);
        _commands.put(CLEAR, this::doClear);
        _commands.put(DUMP, this::doDump);
        _commands.put(HELP, this::doHelp);
        _commands.put(MANUAL, this::doManual);
        _commands.put(PASS, this::doPass);
        _commands.put(PIECEMOVE, this::doMove);
        _commands.put(SEED, this::doSeed);
        _commands.put(START, this::doStart);
        _commands.put(LOAD, this::doLoad);
        _commands.put(QUIT, this::doQuit);
        _commands.put(ERROR, this::doError);
        _commands.put(EOF, this::doQuit);
    }

    /** Input source. */
    private final CommandSources _inputs = new CommandSources();

    /** My board. */
    private Board _board;
    /** Current game state. */
    private State _state;
    /** Used to send messages to the user. */
    private Reporter _reporter;
    /** Source of pseudo-random numbers (used by AIs). */
    private Random _randoms = new Random();
    /** Red player. */
    private Player _redPlayer;
    /** Blue player. */
    private Player _bluePlayer;
    /** Current player. */
    private Player currentPlayer;
    /** Boolean for AI Blue case. */
    private boolean aiBlue = true;
    /** Boolean for AI red case. */
    private boolean aiRed = false;
}
