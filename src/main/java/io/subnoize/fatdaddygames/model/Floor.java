package io.subnoize.fatdaddygames.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

import io.subnoize.fatdaddygames.configuration.GameConfiguration;

/**
 * Creates the visible snow ground and its unchanged static collision surface.
 * Keeping the original box and rigid body preserves jump and landing behavior while
 * removing the pond-like visual that conflicts with the winter landscape.
 */
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
		floor_mat.setColor("Color", new ColorRGBA(0.93f, 0.97f, 1f, 1f));

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
