package ua.leskivproduction.fifteenth;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ua.leskivproduction.fifteenth.model.Board;
import ua.leskivproduction.fifteenth.model.Solver;
import ua.leskivproduction.fifteenth.utils.Lerper;

import java.awt.*;
import java.text.DecimalFormat;

public class Fifteenth extends ApplicationAdapter {
    private static final int IMAGES_CNT = 15;
    private static final int DIMENSION = 4;

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

    private Music backgroundMusic;
    private Music beast;
    private float epicTransition;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("core/assets/wastingTime.mp3"));
        beast = Gdx.audio.newMusic(Gdx.files.internal("core/assets/beautyAndTheBeast.mp3"));

        backgroundMusic.setLooping(true);
        backgroundMusic.play();

        captainFont = genFont("American Captain.ttf", Gdx.graphics.getHeight() * 0.065, Color.WHITE);
        jokerSmallFont = genFont("Jokerman-Regular.ttf", Gdx.graphics.getHeight() * 0.085, Color.WHITE);
        jokerMediumFont = genFont("Jokerman-Regular.ttf", Gdx.graphics.getHeight() * 0.085, Color.WHITE);

        cellTextures = new Texture[DIMENSION * DIMENSION];
        for (int i = 0; i < cellTextures.length; i++)
            cellTextures[i] = new Texture("core/assets/cells/" + (i % IMAGES_CNT) + ".png");

        curBoard = new Board(DIMENSION);
        cam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        karp = new Texture("core/assets/cells/4.png");
        domogarov = new Texture("core/assets/cells/dom.png");

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
                            solver = null;
                        }
                        break;
                    case Input.Keys.SPACE:
                        if (curState != State.CRAFTING) {
                            if (solver != null && curState == State.SOLVING) {
                                solver.terminate();
                            }
                            curState = curState != State.SOLVING ? State.SOLVING : State.IDLE;
                            if (curState == State.SOLVING && (solver == null || !solver.isSolving())) {
                                solver = new Solver(curBoard);
                            } else {
                                solver = null;
                            }
                        }
                        break;
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

    private Texture karp, domogarov;

    private float time;
    @Override
    public void render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 0.8f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        time += Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyPressed(Input.Keys.A) && Gdx.input.isKeyPressed(Input.Keys.R) &&
                Gdx.input.isKeyPressed(Input.Keys.T)) {

            if (epicTransition < 0.05) {
                beast.setPosition(0);
                beast.play();
            }

            epicTransition = Lerper.lerp(epicTransition, 1, 5*Gdx.graphics.getDeltaTime());
            if (cellTextures.length >= 5)
                cellTextures[4] = domogarov;

        } else {
            epicTransition = Lerper.lerp(epicTransition, 0, 5*Gdx.graphics.getDeltaTime());
            if (cellTextures.length >= 5)
                cellTextures[4] = karp;
        }

        backgroundMusic.setVolume(1-epicTransition);
        beast.setVolume(epicTransition > 0.05? epicTransition : 0);
        cam.zoom = 1-epicTransition*2/3;

        Point domPos = curBoard.getCell(4);
        if (domPos != null && epicTransition > 0.05) {
            int cellSize = Gdx.graphics.getHeight()/DIMENSION;

            cam.position.x = Lerper.lerp(cam.position.x, (domPos.x)*cellSize,
                    5*Gdx.graphics.getDeltaTime());
            cam.position.y = Lerper.lerp(cam.position.y, Gdx.graphics.getHeight()/2-(domPos.y+0.5f)*cellSize,
                    5*Gdx.graphics.getDeltaTime());
        } else {
            cam.position.x = Lerper.lerp(cam.position.x, 0, 5*Gdx.graphics.getDeltaTime());
            cam.position.y = Lerper.lerp(cam.position.y, 0, 5*Gdx.graphics.getDeltaTime());
        }

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
        if (curState != State.SHUFFLING) {
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
        } else {
            batch.begin();
            jokerMediumFont.draw(batch, "Shuffling..",
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


        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1, 1, 1, epicTransition/3);

        shapeRenderer.circle(0, 0,
                Gdx.graphics.getWidth()*0.8f);
        shapeRenderer.end();

    }

    @Override
    public void dispose() {
        batch.dispose();
        for (Texture t : cellTextures) {
            t.dispose();
        }
    }
}
