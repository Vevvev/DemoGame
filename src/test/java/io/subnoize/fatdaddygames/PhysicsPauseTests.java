package io.subnoize.fatdaddygames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.test.util.ReflectionTestUtils;

import com.jme3.scene.Node;
import org.junit.jupiter.params.provider.ValueSource;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;

/**
 * Exercises real JBullet stepping through AppStateManager rather than mocking a pause flag.
 * It protects the mid-jump freeze symptom: paused wall time must not advance the controller
 * or leak into a catch-up step, and the post-resume trajectory must match active time alone.
 */
class PhysicsPauseTests {

	private static final float FRAME = 1f / 60f;

	@ParameterizedTest
	@ValueSource(ints = { 6, 30, 50 })
	void pauseDuringAscentApexOrDescentFreezesAndPreservesTheActiveTimeTrajectory(int activeFramesBeforePause) {
		try (PhysicsFixture paused = new PhysicsFixture(); PhysicsFixture uninterrupted = new PhysicsFixture()) {
			CharacterControl pausedPlayer = paused.createGroundedPlayer();
			CharacterControl uninterruptedPlayer = uninterrupted.createGroundedPlayer();
			pausedPlayer.jump();
			uninterruptedPlayer.jump();
			paused.step(activeFramesBeforePause);
			uninterrupted.step(activeFramesBeforePause);

			float groundedY = pausedPlayer.getPhysicsLocation().y;
			Vector3f stoppedAt = pausedPlayer.getPhysicsLocation().clone();
			assertThat(uninterruptedPlayer.getPhysicsLocation().y).isGreaterThan(groundedY);
			MyGameDirector director = this.director(paused.physics);
			director.onAction("Pause", false, FRAME);
			assertThat(ReflectionTestUtils.getField(director, "isRunning")).isEqualTo(false);
			paused.step(240);
			assertThat(pausedPlayer.getPhysicsLocation()).isEqualTo(stoppedAt);
			assertThat(pausedPlayer.getJumpSpeed()).isEqualTo(15f);
			assertThat(pausedPlayer.getFallSpeed()).isEqualTo(30f);
			assertThat(pausedPlayer.getGravity()).isEqualTo(30f);

			director.onAction("Pause", false, FRAME);
			assertThat(ReflectionTestUtils.getField(director, "isRunning")).isEqualTo(true);
			paused.step(12);
			uninterrupted.step(12);
			assertThat(pausedPlayer.getPhysicsLocation().distance(uninterruptedPlayer.getPhysicsLocation()))
					.isCloseTo(0f, within(0.001f));
		}
	}

	private MyGameDirector director(BulletAppState physics) {
		MyGameDirector director = new MyGameDirector();
		Node guiNode = new Node("GUI");
		Node menu = new Node("Menu");
		guiNode.attachChild(menu);
		ReflectionTestUtils.setField(director, "bulletAppState", physics);
		ReflectionTestUtils.setField(director, "guiNode", guiNode);
		ReflectionTestUtils.setField(director, "menuPanel", menu);
		ReflectionTestUtils.setField(director, "isRunning", true);
		return director;
	}

	private static final class PhysicsFixture implements AutoCloseable {

		private final Application application = mock(Application.class);
		private final AppStateManager stateManager = new AppStateManager(this.application);
		private final BulletAppState physics = new BulletAppState();

		PhysicsFixture() {
			given(this.application.getStateManager()).willReturn(this.stateManager);
			this.stateManager.attach(this.physics);
			this.stateManager.update(0f);
			this.physics.getPhysicsSpace().add(new RigidBodyControl(
					new BoxCollisionShape(new Vector3f(20f, 0.1f, 5f)), 0f));
		}

		CharacterControl createGroundedPlayer() {
			CharacterControl player = new CharacterControl(new CapsuleCollisionShape(1f, 0.7f, 1), 0.05f);
			player.setJumpSpeed(15f);
			player.setFallSpeed(30f);
			player.setGravity(30f);
			player.setPhysicsLocation(new Vector3f(-3f, 10f, 0f));
			this.physics.getPhysicsSpace().add(player);
			this.step(240);
			return player;
		}

		void step(int frameCount) {
			for (int frame = 0; frame < frameCount; frame++) {
				this.stateManager.update(FRAME);
				this.stateManager.render(null);
				this.stateManager.postRender();
			}
		}

		@Override
		public void close() {
			this.stateManager.detach(this.physics);
		}
	}
}
