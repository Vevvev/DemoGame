package io.subnoize.fatdaddygames.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Protects the bounded, visual-only winter panorama without launching a renderer.
 * The tests verify camera-layer construction, frame-time parallax, frozen lifecycle
 * behavior, and identity-preserving recycling so scenery cannot alter collision play.
 */
class WinterLandscapeTests {

	private AssetManager assets;

	@BeforeEach
	void setUp() {
		this.assets = new DesktopAssetManager(true);
		this.assets.registerLocator("", ClasspathLocator.class);
	}

	private ViewPort viewPort() {
		return new ViewPort("Winter test viewport", new Camera(800, 600));
	}

	@Test
	void buildsDistinctVisibleLayersWithNoPhysicsControlsAndBlueSky() {
		Node world = new Node("World");
		ViewPort viewPort = this.viewPort();
		new WinterLandscape(this.assets, world, viewPort);

		assertThat(world.getChildren()).extracting(Spatial::getName).containsExactly(
				"Winter clouds", "Distant snowy hills", "Snow-roof village", "Nearby snowy pines");
		assertThat(world.getChildren()).allSatisfy(spatial -> {
			Node layer = (Node) spatial;
			assertThat(layer.getNumControls()).isZero();
			assertThat(layer.getChildren()).isNotEmpty();
			assertThat(layer.getChildren()).allSatisfy(child -> assertThat(child.getNumControls()).isZero());
		});
		assertThat(viewPort.getBackgroundColor()).isEqualTo(new ColorRGBA(0.42f, 0.72f, 0.94f, 1f));
	}

	@Test
	void parallaxUsesElapsedTimeAndFreezesForPauseAndGameOver() {
		Node world = new Node("World");
		WinterLandscape landscape = new WinterLandscape(this.assets, world, this.viewPort());
		Node clouds = (Node) world.getChild("Winter clouds");
		Node hills = (Node) world.getChild("Distant snowy hills");
		Node village = (Node) world.getChild("Snow-roof village");
		Node pines = (Node) world.getChild("Nearby snowy pines");

		landscape.update(1f, 6f, true);
		float cloudX = clouds.getLocalTranslation().x;
		float hillX = hills.getLocalTranslation().x;
		float villageX = village.getLocalTranslation().x;
		float pineX = pines.getLocalTranslation().x;

		assertThat(cloudX).isCloseTo(-0.72f, within(0.00001f));
		assertThat(hillX).isCloseTo(-1.2f, within(0.00001f));
		assertThat(villageX).isCloseTo(-2.52f, within(0.00001f));
		assertThat(pineX).isCloseTo(-4.08f, within(0.00001f));
		landscape.update(60f, 18f, false);
		assertThat(clouds.getLocalTranslation().x).isEqualTo(cloudX);
		assertThat(hills.getLocalTranslation().x).isEqualTo(hillX);
		assertThat(village.getLocalTranslation().x).isEqualTo(villageX);
		assertThat(pines.getLocalTranslation().x).isEqualTo(pineX);
	}

	@Test
	void largeFiniteFramesWrapBoundedGeometryAndResetToTheSameScene() {
		Node world = new Node("World");
		WinterLandscape landscape = new WinterLandscape(this.assets, world, this.viewPort());
		List<Spatial> originalLayers = List.copyOf(world.getChildren());
		List<Integer> originalCounts = world.getChildren().stream()
				.map(spatial -> ((Node) spatial).getQuantity()).toList();

		landscape.update(Float.MAX_VALUE, 18f, true);
		assertThat(world.getChildren()).containsExactlyElementsOf(originalLayers);
		assertThat(world.getChildren()).extracting(Spatial::getLocalTranslation)
				.allSatisfy(location -> assertThat(location.x).isBetween(-24f, 24f));
		assertThat(world.getChildren()).extracting(spatial -> ((Node) spatial).getQuantity())
				.containsExactlyElementsOf(originalCounts);

		landscape.reset();
		assertThat(world.getChildren()).containsExactlyElementsOf(originalLayers);
		assertThat(world.getChildren()).extracting(Spatial::getLocalTranslation)
				.allSatisfy(location -> assertThat(location.x).isZero());
		assertThat(world.getChildren()).extracting(spatial -> ((Node) spatial).getQuantity())
				.containsExactlyElementsOf(originalCounts);
	}
}
