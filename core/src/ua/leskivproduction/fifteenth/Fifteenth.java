package ua.leskivproduction.fifteenth;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ua.leskivproduction.fifteenth.model.Board;
import ua.leskivproduction.fifteenth.model.Solver;

import java.text.DecimalFormat;

public class Fifteenth extends ApplicationAdapter {
    private static final int IMAGES_CNT = 15;
    private static final int DIMENSION = 3;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera cam;

    private Texture[] cellTextures;
    private Board curBoard;

    private BitmapFont captainFont;
    private BitmapFont jokerSmallFont;
    private BitmapFont jokerMediumFont;

    private enum State {SHUFFLING, SOLVING, IDLE, CRAFTING}

    private State curState = State.IDLE;

    private final static float SHUFFLE_TIME = 2f;
    private final static float SHUFFLE_STEPS = 2000;
    private float shufflingTime;
    private int shuffledCnt;

    private Solver solver;
    private int insertedCellNum;

    private BitmapFont genFont(String file, double size, Color color) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("core/assets/"+file));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = (int)size;
        parameter.color = color;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        return font;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        Music backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("core/assets/wastingTime.mp3"));
        backgroundMusic.setLooping(true);
        //backgroundMusic.play();

        captainFont = genFont("American Captain.ttf", Gdx.graphics.getHeight() * 0.065, Color.WHITE);
        jokerSmallFont = genFont("Jokerman-Regular.ttf", Gdx.graphics.getHeight() * 0.085, Color.WHITE);
        jokerMediumFont = genFont("Jokerman-Regular.ttf", Gdx.graphics.getHeight() * 0.100, Color.WHITE);

        cellTextures = new Texture[DIMENSION * DIMENSION];
        for (int i = 0; i < cellTextures.length; i++)
            cellTextures[i] = new Texture("core/assets/cells/" + (i % IMAGES_CNT) + ".png");

        curBoard = new Board(DIMENSION);
        cam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Gdx.input.setInputProcessor(new DummyInputController() {
            @Override
            public boolean keyDown(int keycode) {
                if (curState == State.IDLE)
                    switch (keycode) {
                        case Input.Keys.UP:
                            curBoard.move(Board.Direction.UP);
                            break;
                        case Input.Keys.DOWN:
                            curBoard.move(Board.Direction.DOWN);
                            break;
                        case Input.Keys.LEFT:
                            curBoard.move(Board.Direction.LEFT);
                            break;
                        case Input.Keys.RIGHT:
                            curBoard.move(Board.Direction.RIGHT);
                            break;
                    }
                switch (keycode) {
                    case Input.Keys.S:
                        if (solver == null || !solver.isSolving() && curState != State.CRAFTING) {
                            if (curState != State.SHUFFLING) {
                                shuffledCnt = 0;
                                shufflingTime = 0;
                                curState = State.SHUFFLING;
                            } else {
                                curState = State.IDLE;
                            }
                        }
                        break;
                    case Input.Keys.SPACE:
                        if (curState != State.CRAFTING) {
                            curState = curState != State.SOLVING ? State.SOLVING : State.IDLE;
                            if (curState == State.SOLVING && (solver == null || !solver.isSolving())) {
                                solver = new Solver(curBoard);
                            }
                            break;
                        }
                    case Input.Keys.ENTER:
                        if (curState == State.IDLE) {
                            insertedCellNum = 0;
                            curState = State.CRAFTING;
                            curBoard = new Board(DIMENSION);
                            curBoard.clear();
                        } else if (curState == State.CRAFTING) {
                            if (!curBoard.isValid())
                                curBoard = new Board(DIMENSION);
                            curState = State.IDLE;
                        }
                        break;
                }
                return true;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (curState == State.CRAFTING) {

                    int screenWidth = Gdx.graphics.getWidth();
                    int screenHeight = Gdx.graphics.getHeight();

                    int cellSize = screenWidth/(2*DIMENSION);

                    int cellX = Math.round((float)(screenX-screenWidth/2)/cellSize);
                    int cellY = Math.round((float)(screenY+(screenHeight-screenWidth/2)/2)/cellSize)-1;

                    if (curBoard.setCell(insertedCellNum, cellX, cellY)) {
                        if (++insertedCellNum == DIMENSION*DIMENSION-1) {
                            if (!curBoard.isValid())
                                curBoard = new Board(DIMENSION);
                            curState = State.IDLE;
                        }
                    }

                }
                return true;
            }
        });
    }


    private float time;
    @Override
    public void render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 0.8f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        time += Gdx.graphics.getDeltaTime();

        switch (curState) {
            case SHUFFLING:
                if (shufflingTime < SHUFFLE_TIME) {
                    shufflingTime += Gdx.graphics.getDeltaTime();
                    int toPerformSteps = (int) (shufflingTime * SHUFFLE_STEPS / SHUFFLE_TIME) - shuffledCnt;
                    Board.Direction[] directions = Board.Direction.values();
                    for (int i = 0; i < toPerformSteps; i++) {
                        int randInt = (int) (Math.random() * directions.length);
                        curBoard.move(directions[randInt]);
                    }
                    shuffledCnt += toPerformSteps;
                } else {
                    curState = State.IDLE;
                }
                break;
            case SOLVING:
                if (solver != null && !solver.isSolving()) {
                    if (solver.performAnimationSteps(curBoard, Gdx.graphics.getDeltaTime()))
                        curState = State.IDLE;
                }
                break;
        }

        cam.update();
        batch.setProjectionMatrix(cam.combined);
        shapeRenderer.setProjectionMatrix(cam.combined);

        curBoard.draw(batch, shapeRenderer, captainFont, cellTextures, Gdx.graphics.getWidth() / 5, 0,
                Gdx.graphics.getWidth() / 2);

        if (solver != null && solver.isSolving()) {
            batch.begin();
            jokerMediumFont.draw(batch, "Loading..", -Gdx.graphics.getWidth() / 2 + 10, Gdx.graphics.getHeight() / 2 - 10);
            batch.end();
        }
        if (solver != null && solver.isSolvable() && !solver.isSolving()) {
            batch.begin();
            if (curState == State.SOLVING && !solver.animationFinished()) {
                captainFont.draw(batch, solver.getAnimationStep() + "/" + solver.solution().length,
                        -Gdx.graphics.getWidth() / 2 + 10, 0);
            }
            jokerMediumFont.draw(batch, "Found in " +
                            new DecimalFormat("#.#").format(solver.getSolutionFoundingTime()) + " s",
                    -Gdx.graphics.getWidth() / 2 + 10, Gdx.graphics.getHeight() / 2 - 10);
            batch.end();
        }
        if (solver != null && !solver.isSolving() && !solver.isSolvable()) {
            batch.begin();
            jokerMediumFont.draw(batch, "Can't be solved!",
                    -Gdx.graphics.getWidth() / 2 + 10, Gdx.graphics.getHeight() / 2 - 10);
            batch.end();
        }

        String logoString = "Leskiv Production";
        batch.begin();
        for (int i = 0; i < logoString.length(); i++) {
            jokerSmallFont.draw(batch, logoString.charAt(i)+"",
                    -Gdx.graphics.getWidth()/2+i*Gdx.graphics.getWidth()/40,
                    (float)(-Gdx.graphics.getHeight()/3+(Math.sin(time/2+i*Math.PI/20)*Gdx.graphics.getHeight()/50)));
        }
        batch.end();

        if (curState == State.CRAFTING) {
            batch.begin();
            jokerMediumFont.draw(batch, ""+(insertedCellNum+1),
                    Gdx.input.getX()-Gdx.graphics.getWidth()/2,
                    Gdx.graphics.getHeight()/2-Gdx.input.getY());
            batch.end();
        }

    }

    @Override
    public void dispose() {
        batch.dispose();
        for (Texture t : cellTextures) {
            t.dispose();
        }
    }
}
