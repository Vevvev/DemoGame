package io.subnoize.fatdaddygames;

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

import io.subnoize.fatdaddygames.configuration.GameConfiguration;
import io.subnoize.fatdaddygames.configuration.GameDirector;
import io.subnoize.fatdaddygames.controls.GameControls;
import io.subnoize.fatdaddygames.model.Floor;
import io.subnoize.fatdaddygames.model.Obstacle;
import io.subnoize.fatdaddygames.model.Particles;
import io.subnoize.fatdaddygames.model.Snowman;
import io.subnoize.fatdaddygames.model.UserInterface;

public class MyGameDirector implements GameDirector, ActionListener {

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

	BitmapText hudText;
	BitmapText keyText;
	BitmapText menuText;

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

	@Override
	public void update(float tpf) {
		if (isRunning) {
			player.setJumpSpeed(15);
			player.setFallSpeed(30);

			geom.setLocalTranslation(player.getPhysicsLocation());

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
				bFire.setLocalTranslation(obstacle.getLocalTranslation().getX(),
						obstacle.getLocalTranslation().getY() - 1f, obstacle.getLocalTranslation().getZ());
			}

			if (golemShape.getOverlappingObjects().contains(player)) {
				isRunning = false;
				isLost = true;
				hudText.setText("You lose with a score of " + score + ".");
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
					obstacle.setLocalTranslation(10f, -1f, 0f);
					control.setCurrentAction("Walk");
				} else {
					obstacle.setLocalTranslation(10f, 2.5f, 0f);
					control.setCurrentAction("stand");
				}

				score++;
				hudText.setText("Your score: " + score);
			}
		} else {
			player.setJumpSpeed(0);
			player.setFallSpeed(0);
		}
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
		bulletAppState = new BulletAppState();
		stateManager.attach(bulletAppState);

		configuration.getFlyByCamera().setEnabled(false);
		controls.setUpKeys();

		DirectionalLight dl = new DirectionalLight();
		dl.setDirection(new Vector3f(-0.1f, -1f, -1).normalizeLocal());
		rootNode.addLight(dl);

		geom = snowman.makeSnowmanBody();
		rootNode.attachChild(geom);

		player = snowman.makeSnowman();
		bulletAppState.getPhysicsSpace().add(player);

		obstacle = ob.makeObstacle();
		control = obstacle.getControl(AnimComposer.class);
		control.setCurrentAction("Walk");

		golemShape = new GhostControl(new BoxCollisionShape(new Vector3f(0.5f, 0.5f, 0.1f)));
		obstacle.addControl(golemShape);
		bulletAppState.getPhysicsSpace().add(golemShape);
		rootNode.attachChild(obstacle);

		fire = part.makeRedFire();
		rootNode.attachChild(fire);

		bFire = part.makeBlueFire();
		rootNode.attachChild(bFire);

		hudText = userI.initHud();
		guiNode.attachChild(hudText);

		keyText = userI.initKeys();
		guiNode.attachChild(keyText);

		menuText = userI.initMenu();
		guiNode.attachChild(menuText);

		floor.initFloor(bulletAppState);
	}

	

	@Override
	public void onAction(String binding, boolean keyPressed, float tpf) {

		if (binding.equals("Jump")) {
			if (isRunning) {
				player.jump();
			}
		}
		if (binding.equals("Pause") && !keyPressed && isLost == false) {
			isRunning = !isRunning;
			guiNode.detachChild(menuText);
		}

		if (binding.equals("Reset")) {
			gameReset();
		}
	}

	public void gameReset() {
		isRunning = false;
		isLost = false;
		score = 0;
		player.setPhysicsLocation(playerDefault);
		obstacle.setLocalTranslation(obstacleDefault);
		hudText.setText("Your score: " + score);
		hudText.setColor(ColorRGBA.White);
		fire.setLocalTranslation(10, 10, 0);
		bFire.setLocalTranslation(10, 10, 0);
		guiNode.attachChild(menuText);
	}

}
