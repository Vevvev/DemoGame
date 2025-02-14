package io.subnoize.fatdaddygames;

import org.springframework.beans.factory.annotation.Autowired;

import com.jme3.anim.AnimComposer;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
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
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import jakarta.annotation.PostConstruct;

public class MyGameDirector implements GameDirector, ActionListener {

	@Autowired
	private GameConfiguration configuration;
	
	@Autowired
	private AppStateManager stateManager;
	
	@Autowired
	private FlyByCamera flyCam;
	
	@Autowired 
	private Node rootNode;
	
	@Autowired 
	private Node guiNode;
	
	@Autowired 
	private AssetManager assetManager;
	
	@Autowired 
	private AppSettings settings;
	
	@Autowired 
	private BitmapFont guiFont;
	
	@Autowired 
	private InputManager inputManager;
	
	@Autowired
	private boolean paused;
	
	private Boolean isLost = false;
	private int score = 0;
	
	@Autowired
	private UserInterface userI;
	
	@Autowired
	private Particles part;
	
	@Autowired
	private Snowman snowman;
	
	BitmapText hudText;
	BitmapText keyText;
	BitmapText menuText;
	
	private CharacterControl player;
	private Geometry geom;
	private BulletAppState bulletAppState;
	
	private AnimComposer control;
	
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
	
	@PostConstruct
	public void start() {
		configuration.setGameDirector(this);
		configuration.start();
	}
	
	@Override
	public void update(float tf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
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

		player = snowman.makeSnowman();
		bulletAppState.getPhysicsSpace().add(player);

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
		
		Material mat_red = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
		mat_red.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));
		fire = part.makeRedFire(mat_red);
		rootNode.attachChild(fire);
		
		Material mat_blue = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
		mat_blue.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));
		bFire = part.makeBlueFire(mat_blue);
		rootNode.attachChild(bFire);

		hudText = userI.initHud(guiFont, settings);
		guiNode.attachChild(hudText);

		keyText = userI.initKeys(guiFont, settings);
		guiNode.attachChild(keyText);

		menuText = userI.initMenu(guiFont, settings);
		guiNode.attachChild(menuText);

		initMaterials();
		initFloor();
	}
	
	public void initMaterials() {
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
		inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_W));
		inputManager.addMapping("Pause", new KeyTrigger(KeyInput.KEY_P));
		inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_BACK));
		inputManager.addListener(this, "Jump");
		inputManager.addListener(this, "Pause");
		inputManager.addListener(this, "Reset");
	}

	@Override
	public void onAction(String binding, boolean keyPressed, float tpf) {
		// TODO Auto-generated method stub
		if (binding.equals("Jump")) {
			if (!paused) {
				player.jump();
			}
		}
		if (binding.equals("Pause") && !keyPressed && isLost == false) {
			paused = !paused;
			guiNode.detachChild(menuText);
		}
		if (binding.equals("Reset")) {
			gameReset();
		}
	}
	
	public void gameReset() {
		paused = false;
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
