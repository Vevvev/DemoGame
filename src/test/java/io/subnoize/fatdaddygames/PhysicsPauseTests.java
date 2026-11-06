package io.subnoize.fatdaddygames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.jme3.anim.AnimComposer;
import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.font.BitmapText;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.system.JmeContext;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.GuiGlobals;

import io.subnoize.fatdaddygames.model.HighScore;
import io.subnoize.fatdaddygames.model.UserInterface;
import io.subnoize.fatdaddygames.service.HighScoreService;

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
			float groundedY = pausedPlayer.getPhysicsLocation().y;
			pausedPlayer.jump();
			uninterruptedPlayer.jump();
			paused.step(activeFramesBeforePause);
			uninterrupted.step(activeFramesBeforePause);

			Vector3f stoppedAt = pausedPlayer.getPhysicsLocation().clone();
			assertThat(pausedPlayer.getPhysicsLocation().y).isGreaterThan(groundedY);
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

	@Test
	void restartActionsWaitForFreshPhysicsContactsAndPreserveGameplayAcrossRepeatedCycles() {
		try (PhysicsFixture fixture = new PhysicsFixture()) {
			CharacterControl player = fixture.createGroundedPlayer();
			GhostControl ghost = new GhostControl(new BoxCollisionShape(new Vector3f(0.5f, 0.5f, 0.1f)));
			Node obstacle = new Node("Obstacle");
			obstacle.setLocalTranslation(player.getPhysicsLocation());
			obstacle.addControl(ghost);
			fixture.physics.getPhysicsSpace().add(ghost);
			ghost.setPhysicsLocation(player.getPhysicsLocation());
			fixture.step(2);
			assertThat(ghost.getOverlappingObjects()).contains(player);

			Node guiNode = new Node("GUI");
			Container menu = this.headlessMenu();
			Container settings = new Container();
			Container scoreMenu = new Container();
			Container gameOver = new Container();
			guiNode.attachChild(menu);
			MyGameDirector director = new MyGameDirector();
			RestartScoreService scoreService = new RestartScoreService();
			RestartUserInterface userInterface = new RestartUserInterface(guiNode);
			AssetManager assets = new DesktopAssetManager(true);
			assets.registerLocator("", ClasspathLocator.class);
			ReflectionTestUtils.setField(director, "highScoreServ", scoreService);
			ReflectionTestUtils.setField(director, "userI", userInterface);
			ReflectionTestUtils.setField(director, "guiNode", guiNode);
			ReflectionTestUtils.setField(director, "menuPanel", menu);
			ReflectionTestUtils.setField(director, "settingsMenu", settings);
			ReflectionTestUtils.setField(director, "scoreMenu", scoreMenu);
			ReflectionTestUtils.setField(director, "gameOverMenu", gameOver);
			ReflectionTestUtils.setField(director, "bulletAppState", fixture.physics);
			ReflectionTestUtils.setField(director, "player", player);
			ReflectionTestUtils.setField(director, "snowmanBody", new Node("Snowman visual"));
			ReflectionTestUtils.setField(director, "obstacle", obstacle);
			ReflectionTestUtils.setField(director, "control", new ResetAnimComposer());
			ReflectionTestUtils.setField(director, "golemShape", ghost);
			ReflectionTestUtils.setField(director, "fire", new ParticleEmitter("Fire", ParticleMesh.Type.Triangle, 1));
			ReflectionTestUtils.setField(director, "bFire", new ParticleEmitter("Blue Fire", ParticleMesh.Type.Triangle, 1));
			ReflectionTestUtils.setField(director, "hudText",
					new BitmapText(assets.loadFont("Interface/Fonts/Default.fnt")));
			director.menuButtonCommands();

			this.button(menu, "Click me, or press P, to play!").click();
			assertThat(ReflectionTestUtils.getField(director, "isRunning")).isEqualTo(true);
			assertThat(fixture.physics.isEnabled()).isTrue();
			assertThat(guiNode.getChildren()).doesNotContain(menu);
			director.update(0f);
			this.assertLostOnce(director, scoreService, guiNode, gameOver, 1);

			this.button(gameOver, "Try again!").click();
			this.assertResetRunning(director, fixture, guiNode, menu, gameOver, player);
			assertThat(ghost.getOverlappingObjects()).contains(player);
			for (int frame = 0; frame < 4; frame++) {
				director.update(frame == 0 ? 0f : FRAME / 4f);
			}
			assertThat(ReflectionTestUtils.getField(director, "isLost")).isEqualTo(false);
			assertThat(scoreService.recordedScores).isEqualTo(1);
			assertThat(ghost.getOverlappingObjects()).contains(player);
			obstacle.updateLogicalState(FRAME);
			fixture.step(2);
			assertThat(ghost.getOverlappingObjects()).doesNotContain(player);
			director.update(0f);
			assertThat(ReflectionTestUtils.getField(director, "isLost")).isEqualTo(false);
			float resetObstacleX = obstacle.getLocalTranslation().x;
			director.update(FRAME);
			assertThat(obstacle.getLocalTranslation().x).isLessThan(resetObstacleX);
			assertThat(fixture.physics.isEnabled()).isTrue();

			this.causeCollision(fixture, director, ghost, obstacle, player);
			this.assertLostOnce(director, scoreService, guiNode, gameOver, 2);
			this.button(gameOver, "Try again!").click();
			this.assertResetRunning(director, fixture, guiNode, menu, gameOver, player);
			director.onAction("Pause", false, FRAME);
			assertThat(ReflectionTestUtils.getField(director, "isRunning")).isEqualTo(false);
			assertThat(fixture.physics.isEnabled()).isFalse();
			director.onAction("Pause", false, FRAME);
			assertThat(ReflectionTestUtils.getField(director, "isRunning")).isEqualTo(true);
			assertThat(fixture.physics.isEnabled()).isTrue();

			director.onAction("Reset", false, FRAME);
			this.assertResetAtMenu(director, fixture, guiNode, menu, gameOver, player);
			director.onAction("Pause", false, FRAME);
			this.assertRunningWithoutMenu(director, fixture, guiNode, menu);
			for (int frame = 0; frame < 3; frame++) {
				director.update(0f);
			}
			assertThat(ReflectionTestUtils.getField(director, "isLost")).isEqualTo(false);
			assertThat(scoreService.recordedScores).isEqualTo(2);
			obstacle.updateLogicalState(FRAME);
			fixture.step(2);
			director.update(0f);
			this.causeCollision(fixture, director, ghost, obstacle, player);
			this.assertLostOnce(director, scoreService, guiNode, gameOver, 3);

			this.button(gameOver, "Return to Main Menu").click();
			this.assertResetAtMenu(director, fixture, guiNode, menu, gameOver, player);
			this.button(menu, "Click me, or press P, to play!").click();
			this.assertRunningWithoutMenu(director, fixture, guiNode, menu);
			for (int frame = 0; frame < 3; frame++) {
				director.update(0f);
			}
			assertThat(ReflectionTestUtils.getField(director, "isLost")).isEqualTo(false);
			assertThat(scoreService.recordedScores).isEqualTo(3);
			obstacle.updateLogicalState(FRAME);
			fixture.step(2);
			director.update(0f);
			this.causeCollision(fixture, director, ghost, obstacle, player);
			this.assertLostOnce(director, scoreService, guiNode, gameOver, 4);

			this.button(gameOver, "Try again!").click();
			this.assertResetRunning(director, fixture, guiNode, menu, gameOver, player);
			assertThat(scoreService.recordedScores).isEqualTo(4);
		}
	}

	private void assertLostOnce(MyGameDirector director, RestartScoreService scoreService, Node guiNode,
			Container gameOver, int expectedRecords) {
		assertThat(ReflectionTestUtils.getField(director, "isLost")).isEqualTo(true);
		assertThat(ReflectionTestUtils.getField(director, "isRunning")).isEqualTo(false);
		assertThat(scoreService.recordedScores).isEqualTo(expectedRecords);
		assertThat(guiNode.getChildren()).contains(gameOver);
	}

	private void assertResetRunning(MyGameDirector director, PhysicsFixture fixture, Node guiNode,
			Container menu, Container gameOver, CharacterControl player) {
		assertThat(ReflectionTestUtils.getField(director, "isLost")).isEqualTo(false);
		assertThat(ReflectionTestUtils.getField(director, "score")).isEqualTo(0);
		assertThat(ReflectionTestUtils.getField(director, "isRunning")).isEqualTo(true);
		assertThat(fixture.physics.isEnabled()).isTrue();
		assertThat(guiNode.getChildren()).doesNotContain(menu, gameOver);
		assertThat(player.getPhysicsLocation()).isEqualTo(new Vector3f(-3f, 10f, 0f));
		assertThat(((Node) ReflectionTestUtils.getField(director, "snowmanBody")).getLocalTranslation())
				.isEqualTo(player.getPhysicsLocation());
		assertThat(((Node) ReflectionTestUtils.getField(director, "obstacle")).getLocalTranslation())
				.isEqualTo(new Vector3f(10f, -1f, 0f));
	}

	private void assertResetAtMenu(MyGameDirector director, PhysicsFixture fixture, Node guiNode,
			Container menu, Container gameOver, CharacterControl player) {
		assertThat(ReflectionTestUtils.getField(director, "isLost")).isEqualTo(false);
		assertThat(ReflectionTestUtils.getField(director, "score")).isEqualTo(0);
		assertThat(ReflectionTestUtils.getField(director, "isRunning")).isEqualTo(false);
		assertThat(fixture.physics.isEnabled()).isFalse();
		assertThat(guiNode.getChildren()).contains(menu).doesNotContain(gameOver);
		assertThat(player.getPhysicsLocation()).isEqualTo(new Vector3f(-3f, 10f, 0f));
		assertThat(((Node) ReflectionTestUtils.getField(director, "snowmanBody")).getLocalTranslation())
				.isEqualTo(player.getPhysicsLocation());
	}

	private void assertRunningWithoutMenu(MyGameDirector director, PhysicsFixture fixture, Node guiNode,
			Container menu) {
		assertThat(ReflectionTestUtils.getField(director, "isRunning")).isEqualTo(true);
		assertThat(fixture.physics.isEnabled()).isTrue();
		assertThat(guiNode.getChildren()).doesNotContain(menu);
	}

	private void causeCollision(PhysicsFixture fixture, MyGameDirector director, GhostControl ghost,
			Node obstacle, CharacterControl player) {
		obstacle.setLocalTranslation(player.getPhysicsLocation());
		ghost.setPhysicsLocation(player.getPhysicsLocation());
		fixture.step(2);
		assertThat(ghost.getOverlappingObjects()).contains(player);
		director.update(0f);
	}

	private Button button(Container container, String text) {
		return container.getChildren().stream().filter(Button.class::isInstance).map(Button.class::cast)
				.filter(candidate -> text.equals(candidate.getText())).findFirst().orElseThrow();
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

	private MyGameDirector director(BulletAppState physics) {
		MyGameDirector director = new MyGameDirector();
		Node guiNode = new Node("GUI");
		Container menu = this.headlessMenu();
		guiNode.attachChild(menu);
		ReflectionTestUtils.setField(director, "bulletAppState", physics);
		ReflectionTestUtils.setField(director, "guiNode", guiNode);
		ReflectionTestUtils.setField(director, "menuPanel", menu);
		ReflectionTestUtils.setField(director, "isRunning", true);
		return director;
	}

	private static final class ResetAnimComposer extends AnimComposer {

		@Override
		public com.jme3.anim.tween.action.Action setCurrentAction(String actionName) {
			return null;
		}
	}

	private static final class RestartScoreService extends HighScoreService {

		private int recordedScores;

		RestartScoreService() {
			super(null);
		}

		@Override
		public org.springframework.data.domain.Page<HighScore> getScorePage(int requestedPage) {
			return new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
		}

		@Override
		public List<HighScore> recordScore(String playerName, int score) {
			this.recordedScores++;
			return List.of();
		}
	}

	private static final class RestartUserInterface extends UserInterface {

		private final Node guiNode;

		RestartUserInterface(Node guiNode) {
			this.guiNode = guiNode;
		}

		@Override
		public void fitLeaderboardPanel(Container leaderboardPanel) {
			// The restart regression concerns physics contacts, not viewport layout.
		}

		@Override
		public Container displayGameOver(Container gameOverPanel, int score) {
			this.guiNode.attachChild(gameOverPanel);
			gameOverPanel.detachAllChildren();
			gameOverPanel.addChild(new Label("Game over... Your score is " + score));
			return gameOverPanel;
		}
	}

	private static final class PhysicsFixture implements AutoCloseable {

		private final Application application = mock(Application.class);
		private final AppStateManager stateManager = new AppStateManager(this.application);
		private final BulletAppState physics = new BulletAppState();

		PhysicsFixture() {
			given(this.application.getStateManager()).willReturn(this.stateManager);
			this.stateManager.attach(this.physics);
			this.stateManager.update(0f);
			RigidBodyControl floor = new RigidBodyControl(
					new BoxCollisionShape(new Vector3f(20f, 0.1f, 5f)), 0f);
			floor.setPhysicsLocation(new Vector3f(0f, -2.1f, 0f));
			this.physics.getPhysicsSpace().add(floor);
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
