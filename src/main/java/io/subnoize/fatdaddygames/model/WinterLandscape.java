package io.subnoize.fatdaddygames.model;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import com.jme3.renderer.ViewPort;

/**
 * Builds a bounded, procedural winter panorama for the fixed-camera runner.
 *
 * <p>The separate scene-graph layers give clouds, distant hills, village buildings,
 * and nearby pines distinct parallax without adding collision objects or touching
 * the obstacle random sequence. Existing geometry is translated and recycled in
 * place, so long frames and repeated games do not allocate meshes or change the
 * snowman's playable floor.</p>
 */
public final class WinterLandscape {

	private static final float LOOP_WIDTH = 48f;
	private static final float HALF_LOOP = LOOP_WIDTH / 2f;
	private static final float CLOUD_RATE = 0.12f;
	private static final float FAR_RATE = 0.2f;
	private static final float MID_RATE = 0.42f;
	private static final float NEAR_RATE = 0.68f;
	private static final ColorRGBA SKY = new ColorRGBA(0.42f, 0.72f, 0.94f, 1f);

	private final Node cloudLayer = new Node("Winter clouds");
	private final Node farLayer = new Node("Distant snowy hills");
	private final Node villageLayer = new Node("Snow-roof village");
	private final Node nearLayer = new Node("Nearby snowy pines");
	private final Node[] scrollingLayers = { this.cloudLayer, this.farLayer, this.villageLayer, this.nearLayer };
	private final float[] layerRates = { CLOUD_RATE, FAR_RATE, MID_RATE, NEAR_RATE };

	/**
	 * Creates the non-colliding background geometry and blue viewport for the game.
	 * The scenery is independent from gameplay RNG so replaying it cannot change
	 * obstacle placement or difficulty.
	 *
	 * @param assets existing JME asset manager used only for built-in material definitions
	 * @param world scene root receiving the visual-only scenery
	 * @param viewPort active camera viewport whose clear color supplies the sky
	 */
	public WinterLandscape(AssetManager assets, Node world, ViewPort viewPort) {
		Material snow = this.material(assets, new ColorRGBA(0.93f, 0.97f, 1f, 1f));
		Material hill = this.material(assets, new ColorRGBA(0.62f, 0.82f, 0.91f, 1f));
		Material pine = this.material(assets, new ColorRGBA(0.12f, 0.38f, 0.39f, 1f));
		Material wood = this.material(assets, new ColorRGBA(0.43f, 0.25f, 0.16f, 1f));
		Material house = this.material(assets, new ColorRGBA(0.86f, 0.48f, 0.31f, 1f));
		Material window = this.material(assets, new ColorRGBA(0.23f, 0.55f, 0.67f, 1f));
		Material cloud = this.material(assets, ColorRGBA.White);

		this.buildClouds(cloud);
		this.buildHills(hill, snow);
		this.buildVillage(house, snow, window);
		this.buildPines(wood, pine, snow);
		for (Node layer : this.scrollingLayers) {
			world.attachChild(layer);
		}
		viewPort.setBackgroundColor(SKY);
	}

	/**
	 * Advances each visual layer using elapsed time and the existing obstacle speed.
	 * Paused and game-over frames are ignored, preventing scenery from drifting while
	 * BulletAppState is disabled. Modulo recycling handles unusually long finite frames
	 * in constant time and preserves the fixed set of Spatial identities.
	 *
	 * @param tpf elapsed frame time in seconds
	 * @param gameSpeed current gameplay speed in world units per second
	 * @param worldRunning whether gameplay physics and obstacle motion are active
	 */
	public void update(float tpf, float gameSpeed, boolean worldRunning) {
		if (!worldRunning || !Float.isFinite(tpf) || tpf <= 0f || !Float.isFinite(gameSpeed) || gameSpeed <= 0f) {
			return;
		}
		for (int index = 0; index < this.scrollingLayers.length; index++) {
			Node layer = this.scrollingLayers[index];
			double x = layer.getLocalTranslation().x - (double) tpf * gameSpeed * this.layerRates[index];
			double wrappedX = ((x + HALF_LOOP) % LOOP_WIDTH + LOOP_WIDTH) % LOOP_WIDTH - HALF_LOOP;
			layer.setLocalTranslation((float) wrappedX, 0f, 0f);
		}
	}

	/**
	 * Returns scenery layers to their initial positions for a reproducible replay.
	 */
	public void reset() {
		for (Node layer : this.scrollingLayers) {
			layer.setLocalTranslation(0f, 0f, 0f);
		}
	}

	private Material material(AssetManager assets, ColorRGBA color) {
		Material material = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
		material.setColor("Color", color);
		return material;
	}

	private void buildClouds(Material material) {
		for (int index = 0; index < 8; index++) {
			Node cloud = new Node("Cloud " + index);
			float x = -22f + index * 6.2f;
			float y = 2.3f + (index % 3) * 0.8f;
			cloud.setLocalTranslation(x, y, -5f);
			this.addBox(cloud, "Cloud base " + index, material, 1.7f, 0.42f, 0.08f, 0f, 0f, 0f);
			this.addSphere(cloud, "Cloud puff left " + index, material, 0.58f, -0.85f, 0.24f, 0f);
			this.addSphere(cloud, "Cloud puff right " + index, material, 0.7f, 0.72f, 0.32f, 0f);
			this.cloudLayer.attachChild(cloud);
		}
	}

	private void buildHills(Material hill, Material snow) {
		for (int index = 0; index < 8; index++) {
			float x = -24f + index * 6.8f;
			float height = 1.35f + (index % 3) * 0.38f;
			this.addSphere(this.farLayer, "Distant hill " + index, hill, 3.9f, x, -1.4f + height * 0.2f, -4.2f);
			this.addSphere(this.farLayer, "Hill snowcap " + index, snow, 2.8f, x - 0.5f, -0.55f + height * 0.2f, -4.1f);
		}
	}

	private void buildVillage(Material wall, Material snow, Material window) {
		for (int index = 0; index < 6; index++) {
			Node building = new Node("Snow-roof house " + index);
			float x = -21f + index * 8.4f;
			float width = 1.15f + (index % 2) * 0.25f;
			building.setLocalTranslation(x, -0.82f, -2.7f);
			this.addBox(building, "House walls " + index, wall, width, 0.8f, 0.16f, 0f, 0f, 0f);
			float roofAngle = 0.55f;
			float roofHalfLength = width * 0.5f / FastMath.cos(roofAngle);
			float roofHeight = 0.8f + roofHalfLength * FastMath.sin(roofAngle) + 0.13f * FastMath.cos(roofAngle);
			// Each half spans wall eave to center ridge, with its lower eave face meeting the wall top.
			Geometry leftRoof = new Geometry("Left snow roof " + index, new Box(roofHalfLength, 0.13f, 0.2f));
			leftRoof.setMaterial(snow);
			leftRoof.setLocalTranslation(-width * 0.5f, roofHeight, 0f);
			leftRoof.setLocalRotation(new Quaternion().fromAngleAxis(roofAngle, Vector3f.UNIT_Z));
			building.attachChild(leftRoof);
			Geometry rightRoof = new Geometry("Right snow roof " + index, new Box(roofHalfLength, 0.13f, 0.2f));
			rightRoof.setMaterial(snow);
			rightRoof.setLocalTranslation(width * 0.5f, roofHeight, 0f);
			rightRoof.setLocalRotation(new Quaternion().fromAngleAxis(-roofAngle, Vector3f.UNIT_Z));
			building.attachChild(rightRoof);
			this.addBox(building, "House window " + index, window, 0.25f, 0.31f, 0.035f, width * 0.43f, -0.02f, 0.18f);
			this.villageLayer.attachChild(building);
		}
	}

	private void buildPines(Material wood, Material foliage, Material snow) {
		for (int index = 0; index < 12; index++) {
			float x = -23f + index * 4.2f;
			float scale = 0.75f + (index % 3) * 0.16f;
			this.addTree(this.farLayer, "Distant pine " + index, wood, foliage, snow, x, -1.45f, -3.7f, scale * 0.72f);
			this.addTree(this.nearLayer, "Near pine " + index, wood, foliage, snow, x + (index % 2) * 1.7f,
					-1.28f, -1.7f, scale);
		}
		for (int index = 0; index < 12; index++) {
			float x = -23f + index * 4.2f;
			this.addSphere(this.nearLayer, "Snowbank " + index, snow, 1.55f,
					x + 1.9f, -1.93f, -1.5f);
		}
	}

	private void addTree(Node layer, String name, Material wood, Material foliage, Material snow,
			float x, float y, float z, float scale) {
		Node tree = new Node(name);
		tree.setLocalTranslation(x, y, z);
		Geometry trunk = new Geometry(name + " trunk", new Cylinder(5, 6, 0.13f, 0.82f, true));
		trunk.setMaterial(wood);
		trunk.setLocalTranslation(0f, 0.38f * scale, 0f);
		// jME cylinders extend along local Z; rotate to scene Y and scale the full trunk with its tree.
		trunk.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
		trunk.setLocalScale(scale, scale, scale);
		tree.attachChild(trunk);
		for (int tier = 0; tier < 3; tier++) {
			float tierScale = (1.05f - tier * 0.2f) * scale;
			float tierY = (0.92f + tier * 0.62f) * scale;
			this.addBough(tree, name + " left bough " + tier, foliage, snow, tierScale, tierY, 0.58f);
			this.addBough(tree, name + " right bough " + tier, foliage, snow, tierScale, tierY, -0.58f);
		}
		layer.attachChild(tree);
	}

	private void addBough(Node tree, String name, Material foliage, Material snow,
			float width, float y, float angle) {
		Quaternion rotation = new Quaternion().fromAngleAxis(angle, Vector3f.UNIT_Z);
		Geometry branch = new Geometry(name, new Box(0.72f * width, 0.14f * width, 0.12f));
		branch.setMaterial(foliage);
		branch.setLocalTranslation(0f, y, 0f);
		branch.setLocalRotation(rotation);
		tree.attachChild(branch);
		Geometry snowCap = new Geometry(name + " snow cap", new Box(0.61f * width, 0.045f * width, 0.13f));
		snowCap.setMaterial(snow);
		snowCap.setLocalTranslation(0f, y + 0.12f * width, 0.015f);
		snowCap.setLocalRotation(rotation);
		tree.attachChild(snowCap);
	}

	private void addBox(Node target, String name, Material material, float xExtent, float yExtent,
			float zExtent, float x, float y, float z) {
		Geometry geometry = new Geometry(name, new Box(xExtent, yExtent, zExtent));
		geometry.setMaterial(material);
		geometry.setLocalTranslation(x, y, z);
		target.attachChild(geometry);
	}

	private void addSphere(Node target, String name, Material material, float xRadius, float x,
			float y, float z) {
		Geometry geometry = new Geometry(name, new Sphere(8, 12, 1f));
		geometry.setMaterial(material);
		geometry.setLocalScale(xRadius, xRadius * 0.32f, 0.18f);
		geometry.setLocalTranslation(x, y, z);
		target.attachChild(geometry);
	}
}
