package io.subnoize.fatdaddygames;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;

import com.jme3.anim.AnimComposer;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.GhostControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.font.BitmapText;
import com.jme3.input.controls.ActionListener;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
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
import io.subnoize.fatdaddygames.repository.HighScoreRepository;
import io.subnoize.fatdaddygames.service.HighScoreService;

public class MyGameDirector implements GameDirector, ActionListener {

	@Autowired
	private HighScoreRepository highScoreRepo;

	@Autowired
	private HighScoreService highScoreServ;

	@Autowired
	private GameConfiguration configuration;

	@Autowired
	private AppStateManager stateManager;

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

	private CharacterControl player;
	private Geometry geom;
	private BulletAppState bulletAppState;

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

	/**
	 * The update method where we keep the physics going if the game isn't paused.
	 * Also check to see if the player collided with anything.
	 */
	@Override
	public void update(float tpf) {
		if (isRunning.equals(true)) {
			player.setJumpSpeed(15);
			player.setFallSpeed(30);

			geom.setLocalTranslation(player.getPhysicsLocation());

			obstacleMove(tpf);

			if (golemShape.getOverlappingObjects().contains(player)) {
				isRunning = false;
				isLost = true;
				List<HighScore> scores = highScoreServ.recordScore(playerName, score);
				scores.forEach(o -> {
					if (o.getScore() > highScore) {
						highScore = o.getScore();
					}
				});
				StringBuilder sb = new StringBuilder();
				sb.append("You lose with a score of ").append(score).append(".   Highest score: ").append(highScore);
				hudText.setText(sb.toString());
				userI.displayGameOver(gameOverMenu, score);
				gameOverButtonCommands(scores);
			}
		} else {
			player.setJumpSpeed(0);
			player.setFallSpeed(0);
		}
	}

	/**
	 * Initialization method that starts the game up.
	 */
	@Override
	public void init() {

		bulletAppState = new BulletAppState();
		stateManager.attach(bulletAppState);

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
		geom = snowman.makeSnowmanBody();
		rootNode.attachChild(geom);

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
		List<HighScore> scores = highScoreRepo.findTop10ByOrderByScoreDesc();
		scores.forEach(o -> {
			if (o.getScore() > highScore) {
				highScore = o.getScore();
				playerName = o.getPlayerName();
			}
		});
		if (playerName.isBlank()) {
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
		scoreButtonCommands(scores);

		// Creating the floor.
		floor.initFloor(bulletAppState);
	}

	/**
	 * Listener for key presses.
	 */
	@Override
	public void onAction(String binding, boolean keyPressed, float tpf) {

		if (binding.equals("Jump") && isRunning.equals(true)) {
			player.jump();
		}

		if (binding.equals("Pause") && !keyPressed && isLost.equals(false)) {
			isRunning = !isRunning;
			guiNode.detachChild(menuPanel);
		}

		if (binding.equals("Reset")) {
			gameReset();
		}
	}

	/**
	 * Reset game method to return the game to its start position.
	 */
	public void gameReset() {
		isRunning = false;
		isLost = false;
		score = 0;
		List<HighScore> scores = highScoreRepo.findTop10ByOrderByScoreDesc();
		scores.forEach(o -> {
			if (o.getScore() > highScore) {
				highScore = o.getScore();
			}
		});
		scoreMenu.clearChildren();
		scoreButtonCommands(scores);
		player.setPhysicsLocation(playerDefault);
		obstacle.setLocalTranslation(obstacleDefault);

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
		TextField nameField = settingsMenu.addChild(new TextField(playerName));
		Button scoreWipeButton = settingsMenu.addChild(new Button("Reset High Scores"));
		Button settingsBackButton = settingsMenu.addChild(new Button("Back"));
		playButton.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				isRunning = !isRunning;
				guiNode.detachChild(menuPanel);
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
				guiNode.attachChild(scoreMenu);
			}
		});
		quitButton.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				configuration.stop();
			}
		});
		scoreWipeButton.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				highScoreServ.resetScores();
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
	 * Method that creates the score menu buttons, and lists high scores.
	 */
	public void scoreButtonCommands(List<HighScore> scores) {
		scoreMenu.addChild(new Label("-High Scores-"));
		for (int i = 0; i < scores.size(); i++) {
			StringBuilder sb = new StringBuilder();
			sb.append("#").append(i + 1).append(": ").append(scores.get(i).getPlayerName()).append(" with a score of ")
					.append(scores.get(i).getScore());
			scoreMenu.addChild(new Label(sb.toString()));
		}
		Button scoreBackButton = scoreMenu.addChild(new Button("Back"));
		scoreBackButton.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				guiNode.detachChild(scoreMenu);
				guiNode.attachChild(menuPanel);
			}
		});
	}

	/**
	 * Method that creates the game over menu buttons, and lists high scores.
	 */
	public void gameOverButtonCommands(List<HighScore> scores) {
		Button replay = gameOverMenu.addChild(new Button("Try again!"));
		replay.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				gameReset();
				isRunning = !isRunning;
				guiNode.detachChild(menuPanel);
			}
		});
		gameOverMenu.addChild(new Label("-High Scores-"));
		for (int i = 0; i < scores.size(); i++) {
			StringBuilder sb = new StringBuilder();
			sb.append("#").append(i + 1).append(": ").append(scores.get(i).getPlayerName()).append(" with a score of ")
					.append(scores.get(i).getScore());
			gameOverMenu.addChild(new Label(sb.toString()));
		}
		Button back = gameOverMenu.addChild(new Button("Return to Main Menu"));
		back.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				gameReset();
				guiNode.detachChild(settingsMenu);
				guiNode.detachChild(gameOverMenu);
				guiNode.attachChild(menuPanel);
			}
		});
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
			bFire.setLocalTranslation(obstacle.getLocalTranslation().getX(), obstacle.getLocalTranslation().getY() - 1f,
					obstacle.getLocalTranslation().getZ());
		}

		if (obstacle.getWorldTransform().getTranslation().x <= -10) {

			// int randomLocation = (Math.random() <= 0.5) ? 1 : 2;

			// if (obstacle.getWorldTransform().getTranslation().y < 2 && randomLocation ==
			// 2) {
			// randomLocation = 1;
			// }

			// TODO Make the obstacle lean forwards
			int randomLocation = random.nextInt(100) + 1;

			if (score < 6) {
				randomLocation = 1;
			}

			if (randomLocation < 80) {
				obstacle.setLocalTranslation(10f, -1f, 0f);
				control.setCurrentAction("Walk");
			} else {
				obstacle.setLocalTranslation(10f, 2.5f, 0f);
				control.setCurrentAction("stand");
			}

			score++;
			StringBuilder sb = new StringBuilder();
			sb.append("Your score: ").append(score).append("   Highest score: ").append(highScore);
			hudText.setText(sb.toString());
		}
	}

}
