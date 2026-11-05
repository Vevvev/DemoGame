package io.subnoize.fatdaddygames;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.jme3.bullet.BulletAppState;
import com.jme3.scene.Node;

/**
 * Verifies that the game's single running-state transition owns BulletAppState scheduling.
 * This keeps start, keyboard pause/resume, reset, collision, and replay callers from leaving
 * physics enabled after gameplay stops or disabled when gameplay starts again.
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
		Node menu = new Node("Menu");
		guiNode.attachChild(menu);
		ReflectionTestUtils.setField(director, "bulletAppState", physics);
		ReflectionTestUtils.setField(director, "guiNode", guiNode);
		ReflectionTestUtils.setField(director, "menuPanel", menu);
		ReflectionTestUtils.setField(director, "isRunning", true);

		director.onAction("Pause", false, 0f);
		assertThat(ReflectionTestUtils.getField(director, "isRunning")).isEqualTo(false);
		assertThat(physics.isEnabled()).isFalse();
		director.onAction("Pause", false, 0f);
		assertThat(ReflectionTestUtils.getField(director, "isRunning")).isEqualTo(true);
		assertThat(physics.isEnabled()).isTrue();
	}
}
