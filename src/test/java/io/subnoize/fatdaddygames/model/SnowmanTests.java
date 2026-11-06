package io.subnoize.fatdaddygames.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.GhostControl;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;

import io.subnoize.fatdaddygames.MyGameDirector;
import io.subnoize.fatdaddygames.configuration.GameConfiguration;

/**
 * Protects the procedural snowman appearance and its visual-only integration.
 * These tests build real meshes and materials without starting a renderer or
 * a Spring application, so checking the model cannot open a game window.
 */
class SnowmanTests {

	private Snowman snowman;

	@BeforeEach
	void setUp() {
		AssetManager assets = new DesktopAssetManager(true);
		assets.registerLocator("", ClasspathLocator.class);
		this.snowman = new Snowman(new TestGameConfiguration(assets));
	}

	@Test
	void buildsOnlyTheAgreedSnowmanPartsUnderOnePlayerNode() {
		Node body = this.snowman.makeSnowmanBody();

		assertThat(body.getName()).isEqualTo("Player");
		assertThat(body.getChildren()).extracting(Spatial::getName).containsExactly(
				"Bottom snowball", "Middle snowball", "Head", "Left eye", "Right eye",
				"Top button", "Middle button", "Bottom button", "Carrot nose", "Left arm", "Right arm");
		assertThat(body.getNumControls()).isZero();
		assertThat(body.getChildren()).allSatisfy(part -> {
			assertThat(part.getParent()).isSameAs(body);
			assertThat(part.getNumControls()).isZero();
			assertThat(part.getLocalScale()).isEqualTo(Vector3f.UNIT_XYZ);
		});
		assertThat(body.getLocalLightList().size()).isEqualTo(1);
		assertThat(body.getLocalLightList().get(0)).isInstanceOf(AmbientLight.class);
	}

	@Test
	void snowballsDecreaseInSizeAndMatchTheExistingCapsuleHeight() {
		Node body = this.snowman.makeSnowmanBody();
		Geometry bottom = this.part(body, "Bottom snowball");
		Geometry middle = this.part(body, "Middle snowball");
		Geometry head = this.part(body, "Head");
		float bottomRadius = ((Sphere) bottom.getMesh()).getRadius();
		float middleRadius = ((Sphere) middle.getMesh()).getRadius();
		float headRadius = ((Sphere) head.getMesh()).getRadius();

		assertThat(bottomRadius).isGreaterThan(middleRadius);
		assertThat(middleRadius).isGreaterThan(headRadius);
		assertThat(bottom.getLocalTranslation().y - bottomRadius).isCloseTo(-1.35f, within(0.0001f));
		assertThat(head.getLocalTranslation().y + headRadius).isCloseTo(1.35f, within(0.0001f));
		assertThat(bottom.getLocalTranslation().y + bottomRadius)
				.isGreaterThan(middle.getLocalTranslation().y - middleRadius);
		assertThat(middle.getLocalTranslation().y + middleRadius)
				.isGreaterThan(head.getLocalTranslation().y - headRadius);
	}

	@Test
	void snowCoalCarrotAndWoodHaveTheirOwnLitColors() {
		Node body = this.snowman.makeSnowmanBody();

		for (String name : List.of("Bottom snowball", "Middle snowball", "Head")) {
			this.assertColor(body, name, ColorRGBA.White);
		}
		for (String name : List.of("Left eye", "Right eye", "Top button", "Middle button", "Bottom button")) {
			this.assertColor(body, name, ColorRGBA.Black);
		}
		this.assertColor(body, "Carrot nose", ColorRGBA.Orange);
		this.assertColor(body, "Left arm", new ColorRGBA(0.3f, 0.15f, 0.06f, 1f));
		this.assertColor(body, "Right arm", new ColorRGBA(0.3f, 0.15f, 0.06f, 1f));
	}

	@Test
	void facePointsRightTowardTheGolemAndArmsReachOutFromTheBody() {
		Node body = this.snowman.makeSnowmanBody();
		Geometry nose = this.part(body, "Carrot nose");
		Cylinder carrot = (Cylinder) nose.getMesh();
		body.updateGeometricState();

		assertThat(nose.getWorldRotation().mult(Vector3f.UNIT_Z).distance(Vector3f.UNIT_X))
				.isCloseTo(0f, within(0.0001f));
		assertThat(nose.getWorldTranslation().x).isGreaterThan(this.part(body, "Head").getWorldTranslation().x);
		assertThat(carrot.getRadius2()).isLessThan(carrot.getRadius());
		assertThat(nose.getLocalRotation().mult(Vector3f.UNIT_Z)).isEqualTo(Vector3f.UNIT_Z);
		assertThat(nose.getLocalTranslation().z).isGreaterThan(this.part(body, "Left eye").getLocalTranslation().z);
		assertThat(this.part(body, "Left eye").getLocalTranslation().x).isNegative();
		assertThat(this.part(body, "Right eye").getLocalTranslation().x).isPositive();
		assertThat(this.part(body, "Left arm").getLocalRotation().mult(Vector3f.UNIT_Z).x).isNegative();
		assertThat(this.part(body, "Right arm").getLocalRotation().mult(Vector3f.UNIT_Z).x).isPositive();
	}

	@Test
	void retainsTheExistingPhysicsAndAlignsTheInitialVisualWithItsSpawn() {
		CharacterControl player = this.snowman.makeSnowman();
		Node body = this.snowman.makeSnowmanBody();
		CapsuleCollisionShape capsule = (CapsuleCollisionShape) player.getCollisionShape();

		assertThat(capsule.getRadius()).isEqualTo(1f);
		assertThat(capsule.getHeight()).isEqualTo(0.7f);
		assertThat(capsule.getAxis()).isEqualTo(1);
		assertThat(player.getJumpSpeed()).isEqualTo(15f);
		assertThat(player.getFallSpeed()).isEqualTo(30f);
		assertThat(player.getGravity()).isEqualTo(30f);
		assertThat(player.getPhysicsLocation()).isEqualTo(new Vector3f(-3f, 10f, 0f));
		assertThat(body.getLocalTranslation()).isEqualTo(player.getPhysicsLocation());
	}

	@Test
	void runningGameLoopMovesEveryPartWithThePhysicsController() {
		Node body = this.snowman.makeSnowmanBody();
		CharacterControl player = this.snowman.makeSnowman();
		GhostControl obstacleGhost = new GhostControl(new BoxCollisionShape(new Vector3f(0.5f, 0.5f, 0.1f)));
		assertThat(obstacleGhost.getOverlappingObjects()).isEmpty();
		MyGameDirector director = new MyGameDirector();
		ReflectionTestUtils.setField(director, "snowmanBody", body);
		ReflectionTestUtils.setField(director, "player", player);
		ReflectionTestUtils.setField(director, "golemShape", obstacleGhost);
		ReflectionTestUtils.setField(director, "obstacle", new Node());
		ReflectionTestUtils.setField(director, "isRunning", true);
		List<Vector3f> offsets = body.getChildren().stream()
				.map(part -> part.getLocalTranslation().clone()).toList();
		Vector3f jumpLocation = new Vector3f(-3f, 2f, 0f);
		player.setPhysicsLocation(jumpLocation);

		director.update(1f / 60f);
		body.updateGeometricState();

		assertThat(body.getWorldTranslation()).isEqualTo(jumpLocation);
		assertThat(body.getWorldRotation().mult(Vector3f.UNIT_Z).distance(Vector3f.UNIT_X))
				.isCloseTo(0f, within(0.0001f));
		for (int index = 0; index < body.getQuantity(); index++) {
			Spatial part = body.getChild(index);
			Vector3f offset = offsets.get(index);
			Vector3f rotatedOffset = new Vector3f(offset.z, offset.y, -offset.x);
			assertThat(part.getLocalTranslation()).isEqualTo(offset);
			assertThat(part.getWorldTranslation().distance(jumpLocation.add(rotatedOffset)))
					.isCloseTo(0f, within(0.0001f));
		}
	}

	@Test
	void rebuildingTheModelDoesNotReparentPartsOrShareMutableMaterials() {
		Node first = this.snowman.makeSnowmanBody();
		Node second = this.snowman.makeSnowmanBody();
		first.setLocalTranslation(0f, 0f, 0f);
		this.part(first, "Head").getMaterial().setColor("Diffuse", ColorRGBA.Red);

		assertThat(second.getLocalTranslation()).isEqualTo(new Vector3f(-3f, 10f, 0f));
		assertThat(this.part(first, "Head").getParent()).isSameAs(first);
		assertThat(this.part(second, "Head").getParent()).isSameAs(second);
		this.assertColor(second, "Head", ColorRGBA.White);
	}

	private Geometry part(Node body, String name) {
		return (Geometry) body.getChild(name);
	}

	private void assertColor(Node body, String name, ColorRGBA color) {
		Geometry part = this.part(body, name);
		assertThat(part.getMaterial().getMaterialDef().getAssetName())
				.isEqualTo("Common/MatDefs/Light/Lighting.j3md");
		assertThat(part.getMaterial().getParam("UseMaterialColors").getValue()).isEqualTo(true);
		assertThat(part.getMaterial().getParam("Diffuse").getValue()).isEqualTo(color);
		assertThat(part.getMaterial().getParam("Ambient").getValue()).isEqualTo(color);
	}

	private static final class TestGameConfiguration extends GameConfiguration {

		private final AssetManager assets;

		TestGameConfiguration(AssetManager assets) {
			this.assets = assets;
		}

		@Override
		public AssetManager assetManager() {
			return this.assets;
		}
	}
}
