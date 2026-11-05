package io.subnoize.fatdaddygames;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import com.jme3.anim.AnimComposer;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.GhostControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.font.BitmapText;
import com.jme3.input.controls.ActionListener;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.TextField;

import io.subnoize.fatdaddygames.configuration.GameConfiguration;
import io.subnoize.fatdaddygames.configuration.GameDirector;
import io.subnoize.fatdaddygames.controls.GameControls;
import io.subnoize.fatdaddygames.model.Floor;
import io.subnoize.fatdaddygames.model.HighScore;
import io.subnoize.fatdaddygames.model.Obstacle;
import io.subnoize.fatdaddygames.model.Particles;
import io.subnoize.fatdaddygames.model.Snowman;
import io.subnoize.fatdaddygames.model.UserInterface;
import io.subnoize.fatdaddygames.model.WinterLandscape;
import io.subnoize.fatdaddygames.service.HighScoreService;
import io.subnoize.fatdaddygames.ui.HighScorePageView;

public class MyGameDirector implements GameDirector, ActionListener {


	@Autowired
	private HighScoreService highScoreServ;

	@Autowired
	private GameConfiguration configuration;

	@Autowired
	private Node rootNode;

	@Autowired
	private Node guiNode;

	private Boolean isLost = false;
	private int score = 0;
	private int highScore = 0;

	@Autowired
	private UserInterface userI;

	@Autowired
	private Particles part;

	@Autowired
	private Snowman snowman;

	@Autowired
	private GameControls controls;

	private Random random = new Random(19970803);

	private Boolean isRunning = false;

	private String playerName = "";
	BitmapText hudText;
	BitmapText keyText;
	Container menuPanel;
	Container settingsMenu;
	Container scoreMenu;
	Container gameOverMenu;
	Container gameOverScoresPanel;
	private int scorePageIndex = 0;
	private int gameOverPageIndex = 0;

	private CharacterControl player;
	private Node snowmanBody;
	private BulletAppState bulletAppState;
	private WinterLandscape winterLandscape;

	private AnimComposer control;

	@Autowired
	private Obstacle ob;
	private Node obstacle;
	private GhostControl golemShape;
	private ParticleEmitter fire;
	private ParticleEmitter bFire;
	@Autowired
	private Floor floor;

	private static final Vector3f playerDefault = new Vector3f(-3f, 10f, 0f);
	private static final Vector3f obstacleDefault = new Vector3f(10f, -1f, 0f);
	private static final Quaternion obstacleDefaultRotation = new Quaternion().fromAngles(0, -1.5f, 0);

	/**
	 * Advances gameplay visuals only while the retained physics state is enabled.
	 * Pause disables BulletAppState rather than modifying CharacterControl's jump/fall
	 * configuration, so an airborne player resumes with the same physical momentum.
	 *
	 * @param tpf elapsed frame time in seconds
	 */
	@Override
	public void update(float tpf) {
		if (!this.isRunning) {
			return;
		}

		this.snowmanBody.setLocalTranslation(this.player.getPhysicsLocation());
		this.obstacleMove(tpf);
		if (this.winterLandscape != null) {
			this.winterLandscape.update(tpf, this.currentGameSpeed(), this.isRunning);
		}

		if (this.golemShape.getOverlappingObjects().contains(this.player)) {
			this.setRunning(false);
			this.isLost = true;
			List<HighScore> scores = this.highScoreServ.recordScore(this.playerName, this.score);
			scores.forEach(o -> {
				if (o.getScore() > this.highScore) {
					this.highScore = o.getScore();
				}
			});
			StringBuilder sb = new StringBuilder();
			sb.append("You lose with a score of ").append(this.score).append(".   Highest score: ").append(this.highScore);
			this.hudText.setText(sb.toString());
			this.userI.displayGameOver(this.gameOverMenu, this.score);
			this.gameOverButtonCommands();
		}
	}

	/**
	 * Initialization method that starts the game up.
	 */
	@Override
	public void init() {

		this.bulletAppState = new BulletAppState();
		this.configuration.engineStateManager().attach(this.bulletAppState);
		this.bulletAppState.setEnabled(false);

		// Lemur initialize. This is for the user interface.
		GuiGlobals.initialize(configuration);

		// Disabling the camera so it's not moving, and setting up keys.
		configuration.getFlyByCamera().setEnabled(false);
		controls.setUpKeys();

		// Setting up the light in the scene.
		DirectionalLight dl = new DirectionalLight();
		dl.setDirection(new Vector3f(-0.1f, -1f, -1).normalizeLocal());
		rootNode.addLight(dl);

		// Creating the player.
		this.snowmanBody = this.snowman.makeSnowmanBody();
		this.rootNode.attachChild(this.snowmanBody);

		player = snowman.makeSnowman();
		bulletAppState.getPhysicsSpace().add(player);

		// Creating the obstacle.
		obstacle = ob.makeObstacle();
		control = obstacle.getControl(AnimComposer.class);
		control.setCurrentAction("Walk");

		golemShape = new GhostControl(new BoxCollisionShape(new Vector3f(0.5f, 0.5f, 0.1f)));
		obstacle.addControl(golemShape);
		bulletAppState.getPhysicsSpace().add(golemShape);
		rootNode.attachChild(obstacle);

		// Creating the particles to be used on the obstacle.
		fire = part.makeRedFire();
		rootNode.attachChild(fire);

		bFire = part.makeBlueFire();
		rootNode.attachChild(bFire);

		// Creating the user interface.
		List<HighScore> scores = highScoreServ.getScorePage(0).getContent();
		scores.forEach(o -> {
			if (o.getScore() > highScore) {
				highScore = o.getScore();
				playerName = o.getPlayerName();
			}
		});
		if (playerName.isBlank()) {
			//If we don't get a name we default to Player.
			playerName = "Player";
		}
		hudText = userI.initHud(highScore);
		guiNode.attachChild(hudText);

		keyText = userI.initKeys();
		guiNode.attachChild(keyText);

		menuPanel = userI.initMenu();
		settingsMenu = userI.initSettings();
		scoreMenu = userI.initScores();
		gameOverMenu = userI.initGameOver();
		menuButtonCommands();
		showScorePage(0);

		// Keep the playable collision box fixed; all panorama elements are visual-only.
		this.floor.initFloor(this.bulletAppState);
		this.winterLandscape = new WinterLandscape(
				this.configuration.assetManager(), this.rootNode, this.configuration.engineViewPort());
	}

	/**
	 * Listener for key presses.
	 */
	@Override
	public void onAction(String binding, boolean keyPressed, float tpf) {

		if (binding.equals("Jump") && isRunning.equals(true)) {
			player.jump();
		}

		if (binding.equals("Pause") && !keyPressed && !this.isLost) {
			this.setRunning(!this.isRunning);
			this.guiNode.detachChild(this.menuPanel);
		}

		if (binding.equals("Reset") && isRunning.equals(true)) {
			gameReset();
		}
	}

	/**
	 * Reset game method to return the game to its start position and refresh both
	 * leaderboard entry paths from page one so a reset cannot leave stale rows visible.
	 */
	public void gameReset() {
		this.setRunning(false);
		this.isLost = false;
		if (this.winterLandscape != null) {
			this.winterLandscape.reset();
		}
		score = 0;
		scorePageIndex = 0;
		gameOverPageIndex = 0;
		gameOverScoresPanel = null;
		List<HighScore> scores = highScoreServ.getScorePage(0).getContent();
		scores.forEach(o -> {
			if (o.getScore() > highScore) {
				highScore = o.getScore();
			}
		});
		showScorePage(0);
		player.setPhysicsLocation(playerDefault);
		obstacle.setLocalTranslation(obstacleDefault);
		control.setCurrentAction("Walk");
		obstacle.setLocalRotation(obstacleDefaultRotation);

		StringBuilder sb = new StringBuilder();
		sb.append("Your score: ").append(score).append("   Highest score: ").append(highScore);
		hudText.setText(sb.toString());
		hudText.setColor(ColorRGBA.White);

		fire.setLocalTranslation(10, 10, 0);
		bFire.setLocalTranslation(10, 10, 0);

		guiNode.attachChild(menuPanel);
		guiNode.detachChild(settingsMenu);
		guiNode.detachChild(gameOverMenu);
	}

	/**
	 * Method that creates the main menu buttons.
	 */
	public void menuButtonCommands() {
		Button playButton = menuPanel.addChild(new Button("Click me, or press P, to play!"));
		Button settingsButton = menuPanel.addChild(new Button("Settings"));
		Button scoreButton = menuPanel.addChild(new Button("High Scores"));
		Button quitButton = menuPanel.addChild(new Button("Quit"));
		settingsMenu.addChild(new Label("Player Name:"));
		TextField nameField = settingsMenu.addChild(new TextField(playerName));
		Button nameSaveButton = settingsMenu.addChild(new Button("Save Name"));
		Button scoreWipeButton = settingsMenu.addChild(new Button("Reset High Scores"));
		Button settingsBackButton = settingsMenu.addChild(new Button("Back"));
		playButton.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				MyGameDirector.this.setRunning(true);
				MyGameDirector.this.guiNode.detachChild(MyGameDirector.this.menuPanel);
			}
		});
		settingsButton.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				guiNode.detachChild(menuPanel);
				guiNode.attachChild(settingsMenu);
			}
		});
		scoreButton.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				guiNode.detachChild(menuPanel);
				scorePageIndex = 0;
				showScorePage(0);
				guiNode.attachChild(scoreMenu);
			}
		});
		quitButton.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				configuration.stop();
			}
		});
		nameSaveButton.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				playerName = nameField.getText();
			}
		});
		scoreWipeButton.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				highScoreServ.resetScores();
				highScore = 0;
				gameReset();
			}
		});
		settingsBackButton.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				guiNode.detachChild(settingsMenu);
				guiNode.attachChild(menuPanel);
			}
		});
	}

	/**
	 * Rebuilds the main-menu leaderboard from the requested database page. Clearing the
	 * same container before rendering keeps reopening and paging free of duplicate controls.
	 *
	 * @param requestedPage zero-based page requested by a navigation action
	 */
	private void showScorePage(int requestedPage) {
		Page<HighScore> page = this.highScoreServ.getScorePage(requestedPage);
		this.scorePageIndex = page.getNumber();
		this.scoreMenu.detachAllChildren();
		this.addLeaderboardContents(this.scoreMenu, page,
				new Command<Button>() {
					@Override
					public void execute(Button source) {
						MyGameDirector.this.showScorePage(MyGameDirector.this.scorePageIndex - 1);
					}
				},
				new Command<Button>() {
					@Override
					public void execute(Button source) {
						MyGameDirector.this.showScorePage(MyGameDirector.this.scorePageIndex + 1);
					}
				});
		Button scoreBackButton = this.scoreMenu.addChild(new Button("Back"));
		scoreBackButton.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				MyGameDirector.this.guiNode.detachChild(MyGameDirector.this.scoreMenu);
				MyGameDirector.this.guiNode.attachChild(MyGameDirector.this.menuPanel);
			}
		});
		this.userI.fitLeaderboardPanel(this.scoreMenu);
	}

	/**
	 * Creates the game-over actions once and keeps them outside the replaceable score area,
	 * so paging never removes the lost-score label, replay action, or return-to-menu action.
	 */
	private void gameOverButtonCommands() {
		this.gameOverPageIndex = 0;
		Button replay = this.gameOverMenu.addChild(new Button("Try again!"));
		replay.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				MyGameDirector.this.gameReset();
				MyGameDirector.this.setRunning(true);
				MyGameDirector.this.guiNode.detachChild(MyGameDirector.this.menuPanel);
			}
		});
		this.gameOverScoresPanel = this.gameOverMenu.addChild(new Container());
		this.showGameOverScorePage(0);
		Button back = this.gameOverMenu.addChild(new Button("Return to Main Menu"));
		back.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				gameReset();
				guiNode.detachChild(settingsMenu);
				guiNode.detachChild(gameOverMenu);
				guiNode.attachChild(menuPanel);
			}
		});
		this.userI.fitLeaderboardPanel(this.gameOverMenu);
	}

	/**
	 * Refreshes only the game-over score area. The separate container preserves the game-over
	 * label and existing replay/back controls across every page transition.
	 *
	 * @param requestedPage zero-based page requested by a navigation action
	 */
	private void showGameOverScorePage(int requestedPage) {
		Page<HighScore> page = this.highScoreServ.getScorePage(requestedPage);
		this.gameOverPageIndex = page.getNumber();
		this.gameOverScoresPanel.detachAllChildren();
		this.addLeaderboardContents(this.gameOverScoresPanel, page,
				new Command<Button>() {
					@Override
					public void execute(Button source) {
						MyGameDirector.this.showGameOverScorePage(MyGameDirector.this.gameOverPageIndex - 1);
					}
				},
				new Command<Button>() {
					@Override
					public void execute(Button source) {
						MyGameDirector.this.showGameOverScorePage(MyGameDirector.this.gameOverPageIndex + 1);
					}
				});
		this.userI.fitLeaderboardPanel(this.gameOverMenu);
	}

	/**
	 * Renders one ten-row database page with continuous ranks and only the navigation actions
	 * that can move in the requested direction. Empty data still has a stable Page 1 of 1 view.
	 *
	 * @param target container dedicated to this leaderboard's replaceable contents
	 * @param page database-backed score page
	 * @param previousCommand action for the preceding page
	 * @param nextCommand action for the following page
	 */
	private void addLeaderboardContents(Container target, Page<HighScore> page,
			Command<Button> previousCommand, Command<Button> nextCommand) {
		HighScorePageView view = new HighScorePageView(page);
		target.addChild(new Label("-High Scores-"));
		if (view.isEmpty()) {
			target.addChild(new Label("No high scores yet."));
		} else {
			view.scoreRows().forEach(row -> target.addChild(new Label(row)));
		}
		target.addChild(new Label(view.pageIndicator()));
		if (view.hasPrevious()) {
			Button previous = target.addChild(new Button("Previous"));
			previous.addClickCommands(previousCommand);
		}
		if (view.hasNext()) {
			Button next = target.addChild(new Button("Next"));
			next.addClickCommands(nextCommand);
		}
	}

	/**
	 * Applies one lifecycle transition to gameplay and the retained physics state.
	 * BulletAppState is disabled, rather than changing controller tuning, so its normal
	 * AppState scheduling stops world stepping without accumulating paused time.
	 *
	 * @param running whether gameplay should advance
	 */
	private void setRunning(boolean running) {
		this.isRunning = running;
		if (this.bulletAppState != null) {
			this.bulletAppState.setEnabled(running);
		}
	}

	private float currentGameSpeed() {
		if (this.score < 5) {
			return 6f;
		} else if (this.score < 10) {
			return 9f;
		} else if (this.score < 16) {
			return 12f;
		} else if (this.score < 23) {
			return 15f;
		}
		return 18f;
	}

	/**
	 * Method that determines the speed of the obstacle and its position, as well as
	 * update the score.
	 * 
	 * @param tpf The time in the game that when multiplied with the location stops
	 *            the game from changing speed with the monitor's refresh rate.
	 */
	public void obstacleMove(float tpf) {
		if (score < 5) {
			obstacle.move(tpf * -6f, 0f, 0f);
		} else if (score < 10) {
			hudText.setColor(ColorRGBA.Cyan);
			obstacle.move(tpf * -9f, 0f, 0f);
		} else if (score < 16) {
			hudText.setColor(ColorRGBA.Yellow);
			obstacle.move(tpf * -12f, 0f, 0f);
		} else if (score < 23) {
			hudText.setColor(ColorRGBA.Orange);
			obstacle.move(tpf * -15f, 0f, 0f);
			fire.setLocalTranslation(obstacle.getLocalTranslation().getX() + 0.4f,
					obstacle.getLocalTranslation().getY() + 0.6f, obstacle.getLocalTranslation().getZ());
		} else if (score <= 31 || score >= 30) {
			hudText.setColor(ColorRGBA.Red);
			obstacle.move(tpf * -18f, 0f, 0f);
			fire.setLocalTranslation(obstacle.getLocalTranslation().getX() + 0.4f,
					obstacle.getLocalTranslation().getY() + 0.6f, obstacle.getLocalTranslation().getZ());
		}

		if (obstacle.getWorldTransform().getTranslation().y >= 2) {
			bFire.setLocalTranslation(obstacle.getLocalTranslation().getX()+0.3f, obstacle.getLocalTranslation().getY() - 1f,
					obstacle.getLocalTranslation().getZ());
		}

		if (obstacle.getWorldTransform().getTranslation().x <= -10) {

			// int randomLocation = (Math.random() <= 0.5) ? 1 : 2;

			// if (obstacle.getWorldTransform().getTranslation().y < 2 && randomLocation ==
			// 2) {
			// randomLocation = 1;
			// }

			int randomLocation = random.nextInt(100) + 1;

			if (score < 6) {
				randomLocation = 1;
			}

			if (randomLocation < 80) {
				obstacle.setLocalTranslation(obstacleDefault);
				control.setCurrentAction("Walk");
				obstacle.setLocalRotation(obstacleDefaultRotation);
			} else {
				obstacle.setLocalTranslation(10f, 2.5f, 0f);
				control.setCurrentAction("stand");
				obstacle.setLocalRotation(new Quaternion().fromAngles(20f * FastMath.DEG_TO_RAD, -1.5f, 0f));
			}

			score++;
			StringBuilder sb = new StringBuilder();
			sb.append("Your score: ").append(score).append("   Highest score: ").append(highScore);
			hudText.setText(sb.toString());
		}
	}

}
