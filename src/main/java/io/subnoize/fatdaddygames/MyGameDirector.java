package io.subnoize.fatdaddygames;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;

import com.jme3.anim.AnimComposer;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import io.subnoize.fatdaddygames.configuration.GameConfiguration;
import io.subnoize.fatdaddygames.configuration.GameDirector;
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
	private Material floor_mat;

	private static final Box floor;
	static {
		floor = new Box(10f, 0.1f, 5f);
		floor.scaleTextureCoordinates(new Vector2f(3, 6));
	}
	private static final Vector3f playerDefault = new Vector3f(-3f, 10f, 0f);
	private static final Vector3f obstacleDefault = new Vector3f(10f, -1f, 0f);

	// @PostConstruct
	public void start() {
		// configuration.setGameDirector(this);
		// configuration.start();
	}

	@Override
	public void update(float tpf) {
		if (isRunning) {
			player.setJumpSpeed(15);
			player.setFallSpeed(30);

			geom.setLocalTranslation(player.getPhysicsLocation());

			if (score < 6) {
				obstacle.move(tpf * -6f, 0f, 0f);
			} else if (score < 12) {
				hudText.setColor(ColorRGBA.Cyan);
				obstacle.move(tpf * -9f, 0f, 0f);
			} else if (score < 18) {
				hudText.setColor(ColorRGBA.Yellow);
				obstacle.move(tpf * -12f, 0f, 0f);
			} else if (score < 24) {
				hudText.setColor(ColorRGBA.Orange);
				obstacle.move(tpf * -15f, 0f, 0f);
				fire.setLocalTranslation(obstacle.getLocalTranslation().getX() + 0.4f,
						obstacle.getLocalTranslation().getY() + 0.6f, obstacle.getLocalTranslation().getZ());
			} else if (score <= 30 || score >= 30) {
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
		setUpKeys();

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

		initMaterials();
		initFloor();
	}

	public void initMaterials() {
		floor_mat = new Material(configuration.assetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		TextureKey key3 = new TextureKey("Textures/Terrain/Pond/Pond.jpg");
		key3.setGenerateMips(true);
		Texture tex3 = configuration.assetManager().loadTexture(key3);
		tex3.setWrap(WrapMode.Repeat);
		floor_mat.setTexture("ColorMap", tex3);
	}

	public void initFloor() {
		Geometry floor_geo = new Geometry("Floor", floor);
		floor_geo.setMaterial(floor_mat);
		floor_geo.setLocalTranslation(0, -2.1f, 0);
		this.rootNode.attachChild(floor_geo);
		/* Make the floor physical with mass 0.0f! */
		RigidBodyControl floor_phy = new RigidBodyControl(0.0f);
		floor_geo.addControl(floor_phy);
		bulletAppState.getPhysicsSpace().add(floor_phy);
	}

	private void setUpKeys() {
		configuration.inputManager().addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
		configuration.inputManager().addMapping("Jump", new KeyTrigger(KeyInput.KEY_UP));
		configuration.inputManager().addMapping("Jump", new KeyTrigger(KeyInput.KEY_W));
		configuration.inputManager().addMapping("Pause", new KeyTrigger(KeyInput.KEY_P));
		configuration.inputManager().addMapping("Reset", new KeyTrigger(KeyInput.KEY_BACK));
		configuration.inputManager().addListener(this, "Jump");
		configuration.inputManager().addListener(this, "Pause");
		configuration.inputManager().addListener(this, "Reset");
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
