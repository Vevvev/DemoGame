package io.subnoize.fatdaddygames.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import io.subnoize.fatdaddygames.configuration.GameConfiguration;

@Component
public class Floor {

	@Autowired
	private GameConfiguration configuration;

	@Autowired
	private Node rootNode;

	private static final Box floor;
	static {
		floor = new Box(10f, 0.1f, 5f);
		floor.scaleTextureCoordinates(new Vector2f(3, 6));
	}

	/**
	 * Creates the floor for the game and defines its texture and position.
	 * 
	 * @param bulletAppState
	 */
	public void initFloor(BulletAppState bulletAppState) {
		Material floor_mat = new Material(configuration.assetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		TextureKey key3 = new TextureKey("Textures/Terrain/Pond/Pond.jpg");
		key3.setGenerateMips(true);
		Texture tex3 = configuration.assetManager().loadTexture(key3);
		tex3.setWrap(WrapMode.Repeat);
		floor_mat.setTexture("ColorMap", tex3);

		Geometry floor_geo = new Geometry("Floor", floor);
		floor_geo.setMaterial(floor_mat);
		floor_geo.setLocalTranslation(0, -2.1f, 0);
		this.rootNode.attachChild(floor_geo);
		/* Make the floor physical with mass 0.0f! */
		RigidBodyControl floor_phy = new RigidBodyControl(0.0f);
		floor_geo.addControl(floor_phy);
		bulletAppState.getPhysicsSpace().add(floor_phy);
	}
}
