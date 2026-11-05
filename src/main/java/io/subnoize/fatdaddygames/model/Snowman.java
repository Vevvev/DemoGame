package io.subnoize.fatdaddygames.model;

import org.springframework.stereotype.Component;

import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;

import io.subnoize.fatdaddygames.configuration.GameConfiguration;

/**
 * Builds the procedural snowman player without an external model or texture.
 * The visual parts share one scene-graph parent, while the existing character
 * capsule remains the sole authority for jumping and obstacle collisions.
 */
@Component
public class Snowman {

	private static final Vector3f PLAYER_DEFAULT = new Vector3f(-3f, 10f, 0f);

	private final GameConfiguration configuration;

	public Snowman(GameConfiguration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Creates three overlapping snowballs, coal eyes and buttons, a carrot nose,
	 * and two stick arms as one movable player node. The parts are built facing
	 * local +Z, then the visual parent turns toward +X (screen right) so the
	 * snowman faces the approaching golem. There are no accessories or animations.
	 *
	 * Rotate only the artwork, not the character controller: facing the obstacle
	 * must not change movement, jump tuning, or the collision shape. The snowballs
	 * span -1.35 to +1.35 on local Y, matching the capsule's foot and head heights;
	 * keep this origin aligned with the physics location.
	 *
	 * @return an independently movable snowman visual at the player spawn point
	 */
	public Node makeSnowmanBody() {
		Node body = new Node("Player");
		Material snow = this.makeMaterial(ColorRGBA.White);
		Material coal = this.makeMaterial(ColorRGBA.Black);
		Material carrot = this.makeMaterial(ColorRGBA.Orange);
		Material wood = this.makeMaterial(new ColorRGBA(0.3f, 0.15f, 0.06f, 1f));

		this.addSphere(body, "Bottom snowball", 0.7f, snow, 0f, -0.65f, 0f);
		this.addSphere(body, "Middle snowball", 0.52f, snow, 0f, 0.25f, 0f);
		this.addSphere(body, "Head", 0.37f, snow, 0f, 0.98f, 0f);
		this.addSphere(body, "Left eye", 0.06f, coal, -0.135f, 1.075f, 0.31f);
		this.addSphere(body, "Right eye", 0.06f, coal, 0.135f, 1.075f, 0.31f);
		this.addSphere(body, "Top button", 0.07f, coal, 0f, 0.5f, 0.47f);
		this.addSphere(body, "Middle button", 0.07f, coal, 0f, 0.15f, 0.515f);
		this.addSphere(body, "Bottom button", 0.07f, coal, 0f, -0.48f, 0.68f);

		// A tiny positive tip radius keeps the carrot compatible with jME cylinders.
		this.addPart(body, "Carrot nose", new Cylinder(2, 16, 0.008f, 0.105f, 0.35f, true, false),
				carrot, new Vector3f(0f, 0.98f, 0.48f));
		this.addArm(body, "Left arm", new Vector3f(-0.43f, 0.3f, 0f),
				new Vector3f(-0.93f, 0.67f, 0f), wood);
		this.addArm(body, "Right arm", new Vector3f(0.43f, 0.3f, 0f),
				new Vector3f(0.93f, 0.67f, 0f), wood);

		// Local fill keeps the snow readable without changing other scene objects.
		AmbientLight fill = new AmbientLight();
		fill.setColor(ColorRGBA.White.mult(0.35f));
		body.addLight(fill);
		body.setLocalRotation(new Quaternion().lookAt(Vector3f.UNIT_X, Vector3f.UNIT_Y));
		body.setLocalTranslation(PLAYER_DEFAULT);
		return body;
	}

	private Material makeMaterial(ColorRGBA color) {
		Material material = new Material(this.configuration.assetManager(), "Common/MatDefs/Light/Lighting.j3md");
		material.setBoolean("UseMaterialColors", true);
		material.setColor("Diffuse", color.clone());
		material.setColor("Ambient", color.clone());
		return material;
	}

	private void addSphere(Node body, String name, float radius, Material material, float x, float y, float z) {
		this.addPart(body, name, new Sphere(16, 24, radius), material, new Vector3f(x, y, z));
	}

	/**
	 * Rotates a Z-axis cylinder between its shoulder and hand positions. Keeping
	 * both endpoints local to the player makes the stick arms follow every jump.
	 */
	private void addArm(Node body, String name, Vector3f shoulder, Vector3f hand, Material material) {
		Vector3f direction = hand.subtract(shoulder);
		Geometry arm = this.addPart(body, name, new Cylinder(2, 8, 0.045f, direction.length(), true),
				material, shoulder.add(hand).multLocal(0.5f));
		arm.setLocalRotation(new Quaternion().lookAt(direction.normalizeLocal(), Vector3f.UNIT_Y));
	}

	private Geometry addPart(Node body, String name, Mesh mesh, Material material, Vector3f location) {
		Geometry part = new Geometry(name, mesh);
		part.setMaterial(material);
		part.setLocalTranslation(location);
		body.attachChild(part);
		return part;
	}

	/**
	 * Creates the unchanged character capsule and jump settings. Visual snowballs
	 * and decorations deliberately have no individual physics controls, so their
	 * shape cannot introduce extra obstacle contacts or alter the jump behavior.
	 *
	 * @return the character control used by the game loop
	 */
	public CharacterControl makeSnowman() {
		CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1f, 0.7f, 1);
		CharacterControl player = new CharacterControl(capsuleShape, 0.05f);
		player.setJumpSpeed(15);
		player.setFallSpeed(30);
		player.setGravity(30);
		player.setPhysicsLocation(PLAYER_DEFAULT);
		return player;
	}
}
