package io.subnoize.fatdaddygames;

import java.util.Random;

import com.jme3.anim.AnimComposer;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
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
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

public class SnowmanGame extends SimpleApplication implements ActionListener {

	public static void main(String args[]) {
		SnowmanGame app = new SnowmanGame();
		app.start();
	}
	
	private Random random = new Random(19970803);

	private Boolean isRunning = false;
	private Boolean isLost = false;

	BitmapText hudText;
	BitmapText keyText;
	BitmapText menuText;
	private int score = 0;

	private CharacterControl player;
	private Geometry geom;
	private BulletAppState bulletAppState;

	private AnimComposer control;
	private Node obstacle;
	private GhostControl golemShape;
	private ParticleEmitter fire;
	private ParticleEmitter bFire;

	private Material wall_mat;
	private Material stone_mat;
	private Material floor_mat;

	private static final Box box;
	private static final Sphere sphere;
	private static final Box floor;

	private static final float brickLength = 0.48f;
	private static final float brickWidth = 0.24f;
	private static final float brickHeight = 0.12f;

	private static final Vector3f playerDefault = new Vector3f(-3f, 10f, 0f);
	private static final Vector3f obstacleDefault = new Vector3f(10f, -1f, 0f);

	static {
		/** Initialize the cannon ball geometry */
		sphere = new Sphere(32, 32, 0.4f, true, false);
		sphere.setTextureMode(TextureMode.Projected);

		box = new Box(brickLength, brickHeight, brickWidth);
		box.scaleTextureCoordinates(new Vector2f(1f, .5f));

		floor = new Box(10f, 0.1f, 5f);
		floor.scaleTextureCoordinates(new Vector2f(3, 6));
	}

	@Override
	public void simpleInitApp() {

		bulletAppState = new BulletAppState();
		stateManager.attach(bulletAppState);

		flyCam.setEnabled(false);
		setUpKeys();

		DirectionalLight dl = new DirectionalLight();
		dl.setDirection(new Vector3f(-0.1f, -1f, -1).normalizeLocal());
		rootNode.addLight(dl);

		Box b = new Box(1, 1, 1);
		geom = new Geometry("Player", b);

		Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", ColorRGBA.Blue);
		geom.setMaterial(mat);

		rootNode.attachChild(geom);

		CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1f, 0.7f, 1);
		player = new CharacterControl(capsuleShape, 0.05f);
		player.setJumpSpeed(15);
		player.setFallSpeed(30);
		player.setGravity(30);
		player.setPhysicsLocation(playerDefault);
		bulletAppState.getPhysicsSpace().add(player);
		player.isContactResponse();

		obstacle = (Node) assetManager.loadModel("Models/Oto/Oto.mesh.xml");
		obstacle.setLocalScale(0.19f);
		obstacle.setLocalTranslation(obstacleDefault);
		obstacle.rotate(0, -1.5f, 0);
		control = obstacle.getControl(AnimComposer.class);
		control.setCurrentAction("Walk");

		golemShape = new GhostControl(new BoxCollisionShape(new Vector3f(0.5f, 0.5f, 0.1f)));
		obstacle.addControl(golemShape);
		bulletAppState.getPhysicsSpace().add(golemShape);
		rootNode.attachChild(obstacle);

		fire = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
		Material mat_red = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
		mat_red.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));
		fire.setMaterial(mat_red);
		fire.setImagesX(2);
		fire.setImagesY(2); // 2x2 texture animation
		fire.setEndColor(new ColorRGBA(1f, 0f, 0f, 1f)); // red
		fire.setStartColor(new ColorRGBA(1f, 1f, 0f, 0.5f)); // yellow
		fire.getParticleInfluencer().setInitialVelocity(new Vector3f(4, 0, 0));
		fire.setStartSize(0.5f);
		fire.setEndSize(0.1f);
		fire.setGravity(0, 0, 0);
		fire.setLowLife(0.3f);
		fire.setHighLife(0.3f);
		fire.getParticleInfluencer().setVelocityVariation(0.2f);
		fire.setLocalTranslation(10, 10, 0);
		rootNode.attachChild(fire);
		
		bFire = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
		Material mat_blue = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
		mat_blue.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));
		bFire.setMaterial(mat_red);
		bFire.setImagesX(2);
		bFire.setImagesY(2);
		bFire.setStartColor(ColorRGBA.Blue);
		bFire.setEndColor(ColorRGBA.Cyan);
		bFire.getParticleInfluencer().setInitialVelocity(new Vector3f(0, -5, 0));
		bFire.setStartSize(0.5f);
		bFire.setEndSize(1.5f);
		bFire.setGravity(0, 1, 0);
		bFire.setLowLife(0.5f);
		bFire.setHighLife(0.5f);
		bFire.getParticleInfluencer().setVelocityVariation(0.1f);
		bFire.setLocalTranslation(10, 10, 0);
		rootNode.attachChild(bFire);

		hudText = new BitmapText(guiFont);
		hudText.setSize(guiFont.getCharSet().getRenderedSize()); // font size
		hudText.setColor(ColorRGBA.White); // font color
		hudText.setText("Your score: " + score); // the text
		hudText.setLocalTranslation(settings.getWidth() / 2, settings.getHeight(), 0); // position
		guiNode.attachChild(hudText);

		keyText = new BitmapText(guiFont);
		keyText.setSize(guiFont.getCharSet().getRenderedSize()); // font size
		keyText.setColor(ColorRGBA.White); // font color
		keyText.setText("Space or up arrow to jump, P to play and pause, Backspace to reset, and ESC to quit."); // the text
		keyText.setLocalTranslation(0, settings.getHeight(), 0); // position
		guiNode.attachChild(keyText);
		
		menuText = new BitmapText(guiFont);
		menuText.setSize(guiFont.getCharSet().getRenderedSize()+10); // font size
		menuText.setColor(ColorRGBA.White); // font color
		menuText.setText("Welcome to the Snowman Game! Press P to play!"); // the text
		menuText.setLocalTranslation(settings.getWidth() / 2.9f, settings.getHeight() / 2, 0); // position
		guiNode.attachChild(menuText);

		initMaterials();
		initFloor();
	}

	public void initMaterials() {
		wall_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		TextureKey key = new TextureKey("Textures/Terrain/BrickWall/BrickWall.jpg");
		key.setGenerateMips(true);
		Texture tex = assetManager.loadTexture(key);
		wall_mat.setTexture("ColorMap", tex);

		stone_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		TextureKey key2 = new TextureKey("Textures/Terrain/Rock/Rock.PNG");
		key2.setGenerateMips(true);
		Texture tex2 = assetManager.loadTexture(key2);
		stone_mat.setTexture("ColorMap", tex2);

		floor_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		TextureKey key3 = new TextureKey("Textures/Terrain/Pond/Pond.jpg");
		key3.setGenerateMips(true);
		Texture tex3 = assetManager.loadTexture(key3);
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
		inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
		inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_UP));
		inputManager.addMapping("Pause", new KeyTrigger(KeyInput.KEY_P));
		inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_BACK));
		inputManager.addListener(this, "Jump");
		inputManager.addListener(this, "Pause");
		inputManager.addListener(this, "Reset");
	}

	@Override
	public void onAction(String binding, boolean keyPressed, float arg2) {
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

	@Override
	public void simpleUpdate(float tpf) {
		if (isRunning) {
			player.setJumpSpeed(15);
			player.setFallSpeed(30);

			geom.setLocalTranslation(player.getPhysicsLocation());

			if (score < 6) {
				obstacle.move(-0.06f, 0f, 0f);
			} else if (score < 12) {
				hudText.setColor(ColorRGBA.Cyan);
				obstacle.move(-0.08f, 0f, 0f);
			} else if (score < 18) {
				hudText.setColor(ColorRGBA.Yellow);
				obstacle.move(-0.10f, 0f, 0f);
			} else if (score < 24) {
				hudText.setColor(ColorRGBA.Orange);
				obstacle.move(-0.12f, 0f, 0f);
				fire.setLocalTranslation(obstacle.getLocalTranslation().getX()+0.4f, obstacle.getLocalTranslation().getY()+0.6f, obstacle.getLocalTranslation().getZ());
			} else if (score <= 30 || score >= 30) {
				hudText.setColor(ColorRGBA.Red);
				obstacle.move(-0.14f, 0f, 0f);
				fire.setLocalTranslation(obstacle.getLocalTranslation().getX()+0.4f, obstacle.getLocalTranslation().getY()+0.6f, obstacle.getLocalTranslation().getZ());
			}
			
			if (obstacle.getWorldTransform().getTranslation().y >= 2) {
				bFire.setLocalTranslation(obstacle.getLocalTranslation().getX(), obstacle.getLocalTranslation().getY()-1f, obstacle.getLocalTranslation().getZ());
			}

			if (golemShape.getOverlappingObjects().contains(player)) {
				isRunning = false;
				isLost = true;
				hudText.setText("You lose with a score of " + score + ".");
			}

			if (obstacle.getWorldTransform().getTranslation().x <= -10) {

				//int randomLocation = (Math.random() <= 0.5) ? 1 : 2;
				
				//if (obstacle.getWorldTransform().getTranslation().y < 2 && randomLocation == 2) {
				//	randomLocation = 1;
				//}
				
				int randomLocation = random.nextInt(100) + 1; 
				

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
}
