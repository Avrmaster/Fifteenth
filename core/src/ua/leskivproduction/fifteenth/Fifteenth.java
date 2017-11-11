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
	private SpriteBatch batch;
	private ShapeRenderer shapeRenderer;
	private OrthographicCamera cam;

	private static final int IMAGES_CNT = 8;
	private static final int DIMENSION = 4;
	private Texture[] cellTextures;
	private Board curBoard;
	private BitmapFont mainFont;

	private enum State {SHUFFLING, SOLVING, IDLE};
	private State curState = State.IDLE;

	private final static float SHUFFLE_TIME = 2f;
	private final static float SHUFFLE_STEPS = 2000;
	private float shufflingTime;
	private int shuffledCnt;

	private final static float SOLVE_ANIMATION_TIME = 10;
	private long solutionFoundTime;
	private float solveInterval;
	private float solvingTime;
	private boolean solved;
	private Board[] solution;
	private int solutionStep;
	private boolean solving;

	private Music backgroundMusic;

	@Override
	public void create () {
		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();

		backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("core/assets/wastingTime.mp3"));
		backgroundMusic.setLooping(true);
		backgroundMusic.play();

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
				Gdx.files.internal("core/assets/American Captain.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = (int)(Gdx.graphics.getHeight()*0.065);
		parameter.color = Color.WHITE;
		mainFont = generator.generateFont(parameter);
		generator.dispose();

		cellTextures = new Texture[DIMENSION*DIMENSION];
		for (int i = 0; i < cellTextures.length; i++)
			cellTextures[i] = new Texture("core/assets/cells/"+(i%IMAGES_CNT)+".png");

		curBoard = new Board(DIMENSION);
		cam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		Gdx.input.setInputProcessor(new DummyInputController() {
			@Override
			public boolean keyDown(int keycode) {
				if (curState==State.IDLE)
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
						if (!solving) {
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
						curState = curState!=State.SOLVING? State.SOLVING : State.IDLE;
						if (curState == State.SOLVING) {
							solution = null;
							solutionStep = 0;
							solvingTime = 0;
							if (!solving) {
								solving = true;
								new Thread(() -> {
									solutionFoundTime = System.currentTimeMillis();
									Solver solver = new Solver(curBoard);
									if (solver.isSolvable()) {
										System.out.println("Will be done in "+solver.solution().length);
										solution = solver.solution();
										solveInterval = SOLVE_ANIMATION_TIME/solution.length;
										solveInterval = Math.max(0.02f, Math.min(0.3f, solveInterval));
									} else {
										System.out.println("This cannot be solved!");
									}
									solved = solver.isSolvable();
									solutionFoundTime = System.currentTimeMillis()-solutionFoundTime;
									solving = false;
								}).start();
							}
						}
						break;
				}
				return true;
			}
		});
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 0.8f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		switch (curState) {
			case SHUFFLING:
				if (shufflingTime < SHUFFLE_TIME) {
					shufflingTime += Gdx.graphics.getDeltaTime();
					int toPerformSteps = (int)(shufflingTime*SHUFFLE_STEPS/SHUFFLE_TIME) - shuffledCnt;
					Board.Direction[] directions = Board.Direction.values();
					for (int i = 0; i < toPerformSteps; i++) {
						int randInt = (int)(Math.random()*directions.length);
						curBoard.move(directions[randInt]);
					}
					shuffledCnt += toPerformSteps;
				} else {
					curState = State.IDLE;
				}
				break;
			case SOLVING:
				if (solution != null) {
					solvingTime += Gdx.graphics.getDeltaTime();
					int performedByNow = Math.min(solution.length, (int)(solvingTime / solveInterval));

					for (; solutionStep < performedByNow; solutionStep++) {
						curBoard.moveTo(solution[solutionStep]);
					}

					if (performedByNow == solution.length)
						curState = State.IDLE;
				}
		}

		cam.update();
		batch.setProjectionMatrix(cam.combined);
		shapeRenderer.setProjectionMatrix(cam.combined);

		curBoard.draw(batch, shapeRenderer, mainFont, cellTextures, 0, 0, Gdx.graphics.getWidth()/2);

		if (solving) {
			batch.begin();
			mainFont.draw(batch, "Loading..", -Gdx.graphics.getWidth()/2+10, Gdx.graphics.getHeight()/2-10);
			batch.end();
		}
		if (solved && solution != null) {
			batch.begin();
			if (solutionStep != solution.length && curState == State.SOLVING) {
				mainFont.draw(batch, solutionStep + "/" + solution.length,
						-Gdx.graphics.getWidth() / 2 + 10, 0);
			}
			mainFont.draw(batch, "Found in "+new DecimalFormat("#.#").format(
					(float)solutionFoundTime/1000)+" s",
					-Gdx.graphics.getWidth()/2+10, Gdx.graphics.getHeight()/2-10);
			batch.end();
		}

	}
	
	@Override
	public void dispose () {
		batch.dispose();
		for (Texture t : cellTextures) {
			t.dispose();
		}
	}
}
