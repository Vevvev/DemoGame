package io.subnoize.fatdaddygames.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

import io.subnoize.fatdaddygames.configuration.GameConfiguration;

@Component
public class Snowman {

	@Autowired
	private GameConfiguration configuration;

	private static final Vector3f playerDefault = new Vector3f(-3f, 10f, 0f);

	/**
	 * Creates the physical model used to represent the player.
	 * 
	 * @return Returns the Geometry node.
	 */
	public Geometry makeSnowmanBody() {
		Box b = new Box(1, 1, 1);
		Geometry geom = new Geometry("Player", b);

		Material mat = new Material(configuration.assetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", ColorRGBA.Blue);
		geom.setMaterial(mat);

		return geom;
	}

	/**
	 * Creates the Character Control and Collision Shape for the player so they can
	 * interact with the physical world.
	 * 
	 * @return Returns the character control.
	 */
	public CharacterControl makeSnowman() {

		CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1f, 0.7f, 1);
		CharacterControl player = new CharacterControl(capsuleShape, 0.05f);
		player.setJumpSpeed(15);
		player.setFallSpeed(30);
		player.setGravity(30);
		player.setPhysicsLocation(playerDefault);
		player.isContactResponse();

		return player;
	}
}
