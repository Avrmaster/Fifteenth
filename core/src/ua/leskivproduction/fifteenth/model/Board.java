package ua.leskivproduction.fifteenth.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ua.leskivproduction.fifteenth.utils.Lerper;

import java.awt.*;
import java.util.Iterator;

public class Board implements Comparable<Board> {
    private int[][] blocks;
    private final int amountOfCells;
    private final int dimension;

    private final int searchNodeNum;
    final Board previous;

    //between cells interval for drawing
    private final static float SPACING_COEF = 0.15f;

    private float translateProgress;
    private int lastMoved = -1;
    private Direction lastDir = null;
    private final static float TRANSLATE_SPEED = 0.09f;

    private float goalRed = 0;
    private float curRed = 0;

    public enum Direction {UP, DOWN, LEFT, RIGHT;
        public Direction opposite() {
            switch (this) {
                case DOWN:
                    return UP;
                case UP:
                    return DOWN;
                case LEFT:
                    return RIGHT;
                case RIGHT:
                    return LEFT;
            }
            return null;
        }
    }

    public Board(int dimension)   {
        this.dimension = dimension;
        this.amountOfCells = dimension*dimension-1;
        this.blocks = new int[dimension][dimension];

        this.searchNodeNum = 0;
        this.previous = null;

        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                int cellNum = (i* dimension + j);
                blocks[i][j] = cellNum<amountOfCells? cellNum: -1;
            }
        }
    }

    private Board(Board toCopy) {
        this.amountOfCells = toCopy.amountOfCells;
        this.dimension = toCopy.dimension;
        this.blocks = new int[dimension][dimension];

        this.searchNodeNum  = toCopy.searchNodeNum+1;
        this.previous = toCopy;

        for (int i = 0; i < dimension; i++) {
            System.arraycopy(toCopy.blocks[i], 0, this.blocks[i], 0, dimension);
        }
    }

    // кількість блоків не на своєму місці
    public int hamming() {
        int goalNum = 0;
        int misplacedCnt = 0;
        for (int cell : cellIterator()) {
            if (goalNum >= amountOfCells) {
                if (cell != -1)
                    misplacedCnt++;
            } else if (cell != goalNum++)
                misplacedCnt++;
        }
        return misplacedCnt + searchNodeNum;
    }

    // сума Манхатенських відстаней між блоками і цільовим станом
    public int manhattan() {
        int distance = 0;
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                if (this.blocks[i][j] != -1) {
                    int goalX = this.blocks[i][j]%dimension;
                    int goalY = this.blocks[i][j]/dimension;
                    distance += Math.abs(goalX-j) + Math.abs(goalY-i);
                }
            }
        }
        return ((dimension<4? 1 : dimension)*distance + searchNodeNum);
//        return *distance + searchNodeNum;
    }

    // чи є ця дошка цільовим станом
    public boolean isGoal() {
        int goalNum = 0;
        for (int cell : cellIterator()) {
            if (goalNum >= amountOfCells)
                break;
            if (cell != goalNum++)
                return false;
        }
        return true;
    }

    public boolean isValid() {
        int sum = 0;
        for (int c : cellIterator()) {
            sum += c;
        }
        return sum == (dimension*dimension-2)*(dimension*dimension-1)/2-1;
    }

    public boolean solvable() {
        Board copy = new Board(this);

        int cnt1 = 0;
        for (int c1 : copy.cellIterator()) {
            if (c1 == -1) {
                if (cnt1 != dimension*dimension-1) {
                    int leftCnt = dimension-1-cnt1%dimension;
                    int topCnt = dimension-1-cnt1/dimension;

                    while (leftCnt-->0)
                        copy.move(Direction.LEFT);
                    while (topCnt-->0)
                        copy.move(Direction.UP);
                }
                break;
            }
            cnt1++;
        }

        int swapsCnt = 0;
        int goalNum = 0;
        for (int c1 : copy.cellIterator()) {
            if (c1 != goalNum && c1 != -1) {
                int x1 = goalNum%dimension;
                int y1 = goalNum/dimension;

                int cnt = 0;
                for (int c2 : copy.cellIterator()) {
                    if (c2 == goalNum) {
                        int x2 = cnt % dimension;
                        int y2 = cnt / dimension;
                        copy.swap(x1, y1, x2, y2);
                        swapsCnt++;



                        break;
                    }
                    cnt++;
                }
            }
            goalNum++;
        }
        return (swapsCnt%2==0) && copy.isGoal();
    }
    private void swap(int x1, int y1, int x2, int y2) {
        int temp = blocks[y1][x1];
        blocks[y1][x1] = blocks[y2][x2];
        blocks[y2][x2] = temp;
    }

    public boolean setCell(int cellNum, int x, int y) {
        if (x < 0 || y < 0 || x >= dimension || y >= dimension)
            return false;
        if (blocks[y][x] != -1)
            return false;
        blocks[y][x] = cellNum;
        calcBackgroundColor();
        return true;
    }

    public Point getCell(int cellNum) {
        int cnt = 0;
        for (int c : cellIterator()) {
            if (c == cellNum)
                return new Point(cnt%dimension,cnt/dimension);
            cnt++;
        }
        return null;
    }

    public void clear() {
        goalRed = 0;
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                blocks[i][j] = -1;
            }
        }
    }

    @Override
    public int compareTo(Board o) {
        return Integer.compare(this.manhattan(), o.manhattan());
    }

    public boolean equals(Object y) {
        if (y == null || y.getClass() != this.getClass())
            return false;

        Board another = (Board)y;
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                if (this.blocks[i][j] !=
                        another.blocks[i][j])
                    return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = Integer.hashCode(amountOfCells);
        for (int c : this.cellIterator())
            hash = hash*31 + c;
        return hash;
    }

    public Iterable<Board> neighbors() {
        return () -> new Iterator<Board>() {
            Direction[] directions = Direction.values();
            int step;

            @Override
            public boolean hasNext() {
                return step < directions.length;
            }

            @Override
            public Board next() {
                Direction dir = directions[step++];
                Board res = new Board(Board.this);
                if (res.move(dir))
                    return res;
                return null;
            }
        };
    }

    public boolean move(Direction dir) {
        return move(dir, true);
    }

    /**
     * Moves cells near the open one in specified direction.
     * @return true if move is valid and false otherwise
     */
    private boolean move(Direction dir, boolean animate) {
        int cnt = 0;
        for (int cell : cellIterator()) {
            if (cell == -1) {
                int emptyX = cnt%dimension;
                int emptyY = cnt/dimension;
                int sourceX=-1, sourceY=-1;
                switch (dir) {
                    case UP:
                        sourceX = emptyX;
                        sourceY = emptyY + 1;
                        break;
                    case DOWN:
                        sourceX = emptyX;
                        sourceY = emptyY - 1;
                        break;
                    case LEFT:
                        sourceX = emptyX + 1;
                        sourceY = emptyY;
                        break;
                    case RIGHT:
                        sourceX = emptyX - 1;
                        sourceY = emptyY;
                        break;
                }
                if (sourceX < 0 || sourceY < 0 || sourceX >= dimension || sourceY >= dimension)
                    return false;

                lastMoved = blocks[emptyY][emptyX] = blocks[sourceY][sourceX];
                blocks[sourceY][sourceX] = -1;
                lastDir = dir;

                if (!animate) {
                    lastMoved = -1;
                }

                calcBackgroundColor();

                return true;
            }
            cnt++;
        }
        return false;
    }

    private void calcBackgroundColor() {
        int manh = manhattan();
        goalRed = (float)(Math.min(255, 255*1.3*manh/(dimension < 4? 20 : Math.pow(dimension, 4))));
    }

    public boolean moveTo(Board another) {
        for(Direction dir : Direction.values()) {
            boolean validMove = this.move(dir, false);
            if (validMove) {
                if (this.equals(another)) {
                    this.move(dir.opposite(), false);
                    this.move(dir, true);
                    return true;
                } else {
                    this.move(dir.opposite(), false);
                }
            }
        }
        return false;
    }

    public void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font,
                     Texture[] cells, int x, int y, int sideSize) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        curRed = Lerper.lerp(curRed, goalRed, Gdx.graphics.getDeltaTime());
        shapeRenderer.setColor(curRed/255, 1f-curRed/255, 0, 1);
        shapeRenderer.rect(x-sideSize/2, y-sideSize/2, sideSize, sideSize);
        shapeRenderer.end();

        int cellSize = sideSize / dimension;
        float cellDrawnSize = cellSize*(1-SPACING_COEF);
        float offsetX, offsetY;
        offsetX = offsetY = (sideSize*SPACING_COEF)/(2*dimension);

        if (lastMoved != -1) {
            if (translateProgress < 1)
                translateProgress += Gdx.graphics.getDeltaTime()*TRANSLATE_SPEED*cellSize;
            if (translateProgress >= 1) {
                lastMoved = -1;
                translateProgress = 0;
            }
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        batch.begin();
        int cnt = 0;
        for (int cell : cellIterator()) {

            float cx, cy;

            if (cell != lastMoved) {
                cx = (cnt%dimension)*cellSize;
                cy = (cnt/dimension +1)*cellSize;
            } else {
                cx = (cnt%dimension +
                        (lastDir == Direction.LEFT? 1-translateProgress : 0) +
                        (lastDir == Direction.RIGHT? -1+translateProgress : 0))*cellSize;
                cy = (cnt/dimension + 1 +
                        (lastDir == Direction.UP? 1-translateProgress : 0) +
                        (lastDir == Direction.DOWN? -1+translateProgress : 0))*cellSize;
            }

            if (cell != -1 ) {
                batch.draw(cells[cell],
                        x + offsetX - sideSize / 2 + cx,
                        y + offsetY + sideSize / 2 - cy,
                        cellDrawnSize, cellDrawnSize);

                font.draw(batch, "" + (cell + 1),
                        x + offsetX - sideSize / 2 + cx + cellDrawnSize * 0.8f,
                        y + offsetY + sideSize / 2 - cy + cellDrawnSize * 0.2f);
            } else if (!isValid()) {
                shapeRenderer.setColor(0, 0, 0, 1);
                shapeRenderer.rect(
                        x + offsetX - sideSize / 2 + cx,
                        y + offsetY + sideSize / 2 - cy,
                        cellDrawnSize, cellDrawnSize);
            }

            cnt++;
        }
        batch.end();
        shapeRenderer.end();
    }

    private Iterable<Integer> cellIterator() {
        return () ->  new Iterator<Integer>() {
            private int counter = 0;

            @Override
            public boolean hasNext() {
                return counter < dimension*dimension;
            }

            @Override
            public Integer next() {
                counter++;
                return blocks[(counter-1)/dimension][(counter-1)%dimension];
            }
        };
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < dimension; i++) {
            builder.append("(");
            for (int j = 0; j < dimension; j++) {
                builder.append(blocks[i][j]+1);
                for (int k = 0; k < 6-(""+(blocks[i][j]+1)).length(); k++)
                    builder.append(" ");
            }
            builder.append(")\n");
        }
        return builder.toString();
    }

}

