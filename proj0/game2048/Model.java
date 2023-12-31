package game2048;

import java.util.Arrays;
import java.util.Formatter;
import java.util.Observable;


/** The state of a game of 2048.
 *  @author Jayesh
 */
public class Model extends Observable {
    /** Current contents of the board. */
    private Board board;
    /** Current score. */
    private int score;
    /** Maximum score so far.  Updated when game ends. */
    private int maxScore;
    /** True iff game is ended. */
    private boolean gameOver;

    /* Coordinate System: column C, row R of the board (where row 0,
     * column 0 is the lower-left corner of the board) will correspond
     * to board.tile(c, r).  Be careful! It works like (x, y) coordinates.
     */

    /** Largest piece value. */
    public static final int MAX_PIECE = 2048;

    /** A new 2048 game on a board of size SIZE with no pieces
     *  and score 0. */
    public Model(int size) {
        board = new Board(size);
        score = maxScore = 0;
        gameOver = false;
    }

    /** A new 2048 game where RAWVALUES contain the values of the tiles
     * (0 if null). VALUES is indexed by (row, col) with (0, 0) corresponding
     * to the bottom-left corner. Used for testing purposes. */
    public Model(int[][] rawValues, int score, int maxScore, boolean gameOver) {
        int size = rawValues.length;
        board = new Board(rawValues, score);
        this.score = score;
        this.maxScore = maxScore;
        this.gameOver = gameOver;
    }

    /** Return the current Tile at (COL, ROW), where 0 <= ROW < size(),
     *  0 <= COL < size(). Returns null if there is no tile there.
     *  Used for testing. Should be deprecated and removed.
     *  */
    public Tile tile(int col, int row) {
        return board.tile(col, row);
    }

    /** Return the number of squares on one side of the board.
     *  Used for testing. Should be deprecated and removed. */
    public int size() {
        return board.size();
    }

    /** Return true iff the game is over (there are no moves, or
     *  there is a tile with value 2048 on the board). */
    public boolean gameOver() {
        checkGameOver();
        if (gameOver) {
            maxScore = Math.max(score, maxScore);
        }
        return gameOver;
    }

    /** Return the current score. */
    public int score() {
        return score;
    }

    /** Return the current maximum game score (updated at end of game). */
    public int maxScore() {
        return maxScore;
    }

    /** Clear the board to empty and reset the score. */
    public void clear() {
        score = 0;
        gameOver = false;
        board.clear();
        setChanged();
    }

    /** Add TILE to the board. There must be no Tile currently at the
     *  same position. */
    public void addTile(Tile tile) {
        board.addTile(tile);
        checkGameOver();
        setChanged();
    }

    /** Tilt the board toward SIDE. Return true iff this changes the board.
     * 1. If two Tile objects are adjacent in the direction of motion and have
     *    the same value, they are merged into one Tile of twice the original
     *    value and that new value is added to the score instance variable
     * 2. A tile that is the result of a merge will not merge again on that
     *    tilt. So each move, every tile will only ever be part of at most one
     *    merge (perhaps zero).
     * 3. When three adjacent tiles in the direction of motion have the same
     *    value, then the leading two tiles in the direction of motion merge,
     *    and the trailing tile does not.
     * */
    public boolean tilt(Side side) {
        boolean changed;
        changed = false;

        this.board.setViewingPerspective(side);
        int boardSize = this.board.size();

        for (int c = 0; c < boardSize; c++) {
            if (tiltHelper(c)) {
                changed = true;
            }
        }

        this.board.setViewingPerspective(Side.NORTH);

        checkGameOver();
        if (changed) {
            setChanged();
        }
        return changed;
    }

    /**
     * Returns true if tiles in a particular column of board
     * is moved or merged.
     */
    private boolean tiltHelper(int col) {
        boolean changed = false;
        int boardSize = this.board.size();

        boolean[] tileMerged = new boolean[boardSize];
        Arrays.fill(tileMerged, false);

        for (int r = boardSize - 1; r >= 0; r--) {
            if (!isTileEmpty(this.board, col, r)) {
                int changeIndex = indexToMoveOrMerge(col, r, tileMerged);
                if (changeIndex != -1) {
                    changed = true;
                    if (this.board.move(col, changeIndex, board.tile(col, r))) {
                        tileMerged[changeIndex] = true;
                        this.score += this.board.tile(col, changeIndex).value();
                    }
                }
            }
        }

        return changed;
    }

    /**
     * Returns Index At Which Move Or Merge Operation
     * Is To Be Performed Else Returns -1
     */
    private int indexToMoveOrMerge(int col, int row, boolean[] tilesMerged) {
        int rowIndex = -1;

        for (int r = this.board.size() - 1; r > row; r--) {
            if (isTileEmpty(this.board, col, r)) {
                if (rowIndex == -1) {
                    rowIndex = r;
                }
            }
            else {
                if (this.board.tile(col, row).value() == this.board.tile(col, r).value()) {
                    if (!tilesMerged[r]) {
                        rowIndex = r;
                    }
                }
                else {
                    if (rowIndex != -1) {
                        rowIndex = -1;
                    }
                }
            }
        }

        return rowIndex;
    }

    /** Checks if the game is over and sets the gameOver variable
     *  appropriately.
     */
    private void checkGameOver() {
        gameOver = checkGameOver(board);
    }

    /** Determine whether game is over. */
    private static boolean checkGameOver(Board b) {
        return maxTileExists(b) || !atLeastOneMoveExists(b);
    }

    /** Returns true if at least one space on the Board is empty.
     *  Empty spaces are stored as null.
     * */
    public static boolean emptySpaceExists(Board b) {
        int boardSize = b.size();

        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (isTileEmpty(b, c, r)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns true if any tile is equal to the maximum valid value.
     * Maximum valid value is given by MAX_PIECE. Note that
     * given a Tile object t, we get its value with t.value().
     */
    public static boolean maxTileExists(Board b) {
        int boardSize = b.size();

        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (!isTileEmpty(b, c, r) && b.tile(c, r).value() == MAX_PIECE) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if tile at jth column and ith row is not null
     */
    private static boolean isTileEmpty(Board b, int col, int row) {
        return b.tile(col, row) == null;
    }

    /**
     * Returns true if there are any valid moves on the board.
     * There are two ways that there can be valid moves:
     * 1. There is at least one empty space on the board.
     * 2. There are two adjacent tiles with the same value.
     */
    public static boolean atLeastOneMoveExists(Board b) {
        return emptySpaceExists(b) || checkIfBoardHasSameAdjacentTiles(b);
    }

    /**
     * Returns true if board has same adjacent tiles.
     */
    private static boolean checkIfBoardHasSameAdjacentTiles(Board b) {
        int boardSize = b.size();

        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (!isTileEmpty(b, c, r)) {
                    if (checkIfRightTileSame(b, c + 1, r) || checkIfDownTileSame(b, c, r + 1)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Returns true if right tile exists and has same value as
     * current tile.
     */
    private static boolean checkIfRightTileSame(Board b, int col, int row) {
        return isValidIndex(b, col, row)
                && !isTileEmpty(b, col, row)
                && (b.tile(col - 1, row).value() == b.tile(col, row).value());
    }

    /**
     * Returns true if down tile exists and has same value as
     * current tile.
     */
    private static boolean checkIfDownTileSame(Board b, int col, int row) {
        return isValidIndex(b, col, row)
                && !isTileEmpty(b, col, row)
                && (b.tile(col , row - 1).value() == b.tile(col, row).value());
    }

    /**
     * Returns true if given column index and row index
     * is valid
     */
    private static boolean isValidIndex(Board b, int col, int row) {
        int boardSize = b.size();
        return col < boardSize && row < boardSize;
    }

    @Override
     /** Returns the model as a string, used for debugging. */
    public String toString() {
        Formatter out = new Formatter();
        out.format("%n[%n");
        for (int row = size() - 1; row >= 0; row -= 1) {
            for (int col = 0; col < size(); col += 1) {
                if (tile(col, row) == null) {
                    out.format("|    ");
                } else {
                    out.format("|%4d", tile(col, row).value());
                }
            }
            out.format("|%n");
        }
        String over = gameOver() ? "over" : "not over";
        out.format("] %d (max: %d) (game is %s) %n", score(), maxScore(), over);
        return out.toString();
    }

    @Override
    /** Returns whether two models are equal. */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (getClass() != o.getClass()) {
            return false;
        } else {
            return toString().equals(o.toString());
        }
    }

    @Override
    /** Returns hash code of Model’s string. */
    public int hashCode() {
        return toString().hashCode();
    }
}
