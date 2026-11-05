package io.subnoize.fatdaddygames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.scene.Node;
import com.jme3.system.JmeContext;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;

/**
 * Verifies that the game's single running-state transition owns BulletAppState scheduling.
 * This protects pause/resume callers from leaving physics enabled after gameplay stops or
 * disabled when gameplay starts again, without requiring a renderer or menu UI initialization.
 */
class MyGameDirectorPhysicsLifecycleTests {

	@Test
	void runningStateControlsTheRetainedPhysicsState() {
		MyGameDirector director = new MyGameDirector();
		BulletAppState physics = new BulletAppState();
		ReflectionTestUtils.setField(director, "bulletAppState", physics);

		ReflectionTestUtils.invokeMethod(director, "setRunning", true);
		assertThat(ReflectionTestUtils.getField(director, "isRunning")).isEqualTo(true);
		assertThat(physics.isEnabled()).isTrue();
		ReflectionTestUtils.invokeMethod(director, "setRunning", false);
		assertThat(ReflectionTestUtils.getField(director, "isRunning")).isEqualTo(false);
		assertThat(physics.isEnabled()).isFalse();
	}

	@Test
	void pauseKeyDisablesPhysicsAndTheNextReleaseResumesIt() {
		MyGameDirector director = new MyGameDirector();
		BulletAppState physics = new BulletAppState();
		Node guiNode = new Node("GUI");
		Container menu = this.headlessMenu();
		guiNode.attachChild(menu);
		ReflectionTestUtils.setField(director, "bulletAppState", physics);
		ReflectionTestUtils.setField(director, "guiNode", guiNode);
		ReflectionTestUtils.setField(director, "menuPanel", menu);
		ReflectionTestUtils.setField(director, "isRunning", true);

		director.onAction("Pause", false, 0f);
		assertThat(ReflectionTestUtils.getField(director, "isRunning")).isEqualTo(false);
		assertThat(physics.isEnabled()).isFalse();
		assertThat(menu.getParent()).isNull();

		director.onAction("Pause", false, 0f);
		assertThat(ReflectionTestUtils.getField(director, "isRunning")).isEqualTo(true);
		assertThat(physics.isEnabled()).isTrue();
	}

	private Container headlessMenu() {
		Application application = mock(Application.class);
		JmeContext context = mock(JmeContext.class);
		AssetManager assets = new DesktopAssetManager(true);
		assets.registerLocator("", ClasspathLocator.class);
		given(application.getContext()).willReturn(context);
		given(application.getAssetManager()).willReturn(assets);
		given(context.getType()).willReturn(JmeContext.Type.Headless);
		GuiGlobals.initialize(application);
		return new Container();
	}
}
