package io.subnoize.fatdaddygames.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.system.JmeContext;

import io.subnoize.fatdaddygames.Application;
import io.subnoize.fatdaddygames.MyGameDirector;
import io.subnoize.fatdaddygames.controls.GameControls;
import io.subnoize.fatdaddygames.model.UserInterface;

/**
 * Protects both sides of the Spring-to-jME boundary: Spring must inject controls before the
 * engine thread initializes gameplay, and UI consumers must read final settings only after jME
 * creates its context and applies any settings-dialog selection. The recording app keeps this
 * lifecycle regression headless and avoids window or persistence side effects.
 */
class GameConfigurationLifecycleTests {

	@Test
	void startsOnlyAfterSpringHasCreatedAndInjectedItsBeans() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class)) {
			RecordingGameConfiguration game = context.getBean(RecordingGameConfiguration.class);
			assertThat(game.startRequested).isFalse();
			assertThat(game.startObservedInjectedDependency).isFalse();
			assertThat(game.startupDependency).isNotNull();

			context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0], context, Duration.ZERO));

			assertThat(game.startRequested).isTrue();
			assertThat(game.startObservedInjectedDependency).isTrue();
		}
	}

	@Test
	void productionComponentGraphRefreshesBeforeTheEngineReadyBoundary() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("game-startup-test",
					Map.of("spring.datasource.url", "jdbc:h2:mem:game-startup-lifecycle;DB_CLOSE_DELAY=-1",
							"spring.jpa.hibernate.ddl-auto", "create-drop")));
			context.register(Application.class);
			context.refresh();

			GameConfiguration configuration = context.getBean(GameConfiguration.class);
			MyGameDirector director = context.getBean(MyGameDirector.class);
			GameControls controls = context.getBean(GameControls.class);
			UserInterface userInterface = context.getBean(UserInterface.class);

			assertThat(ReflectionTestUtils.getField(director, "controls")).isSameAs(controls);
			assertThat(ReflectionTestUtils.getField(userInterface, "configuration")).isSameAs(configuration);
			assertThat(configuration.getContext()).isNull();
		}
	}

	@Test
	void uiDoesNotCaptureTheNullSettingsFieldDuringSpringContextCreation() {
		try (AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(SettingsConfiguration.class)) {
			RecordingGameConfiguration game = context.getBean(RecordingGameConfiguration.class);
			UserInterface userInterface = context.getBean(UserInterface.class);

			assertThat(game.settings()).isNull();
			assertThat(ReflectionTestUtils.getField(userInterface, "configuration")).isSameAs(game);
		}
	}

	@Test
	void initializesTheInjectedDirectorOnceOnTheInitializedEngineThread() throws InterruptedException {
		try (AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(HeadlessConfiguration.class)) {
			HeadlessGameConfiguration game = context.getBean(HeadlessGameConfiguration.class);
			RecordingGameDirector director = new RecordingGameDirector(game);
			UserInterface userInterface = context.getBean(UserInterface.class);
			ReflectionTestUtils.setField(game, "gameDirector", director);

			try {
				context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0], context,
						Duration.ZERO));

				assertThat(director.initialized.await(10, TimeUnit.SECONDS)).isTrue();
				assertThat(director.initializationCount).isEqualTo(1);
				assertThat(director.initializationThread).isNotNull().isNotSameAs(Thread.currentThread());
				assertThat(director.stateManager).isNotNull();
				assertThat(director.viewPort).isNotNull();
				assertThat(director.assetManager).isNotNull();
				assertThat(game.settings()).isNotNull();
				assertThat(game.settings().getWidth()).isPositive();
				assertThat(game.settings().getHeight()).isPositive();
				assertThat(ReflectionTestUtils.getField(userInterface, "configuration")).isSameAs(game);
			}
			finally {
				if (game.getContext() != null) {
					game.stop(true);
				}
			}
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		StartupDependency startupDependency() {
			return new StartupDependency();
		}

		@Bean
		RecordingGameConfiguration gameConfiguration() {
			return new RecordingGameConfiguration();
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class SettingsConfiguration {

		@Bean
		StartupDependency startupDependency() {
			return new StartupDependency();
		}

		@Bean
		RecordingGameConfiguration gameConfiguration() {
			return new RecordingGameConfiguration();
		}

		@Bean
		Node guiNode(RecordingGameConfiguration gameConfiguration) {
			return gameConfiguration.guiNode();
		}

		@Bean
		UserInterface userInterface() {
			return new UserInterface();
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class HeadlessConfiguration {

		@Bean
		HeadlessGameConfiguration gameConfiguration() {
			return new HeadlessGameConfiguration();
		}

		@Bean
		Node guiNode(HeadlessGameConfiguration gameConfiguration) {
			return gameConfiguration.guiNode();
		}

		@Bean
		UserInterface userInterface() {
			return new UserInterface();
		}
	}

	private static final class HeadlessGameConfiguration extends GameConfiguration {

		@Override
		public void start() {
			this.start(JmeContext.Type.Headless, true);
		}
	}

	private static final class RecordingGameDirector implements GameDirector {

		private final GameConfiguration configuration;

		private final CountDownLatch initialized = new CountDownLatch(1);

		private volatile int initializationCount;

		private volatile Thread initializationThread;

		private volatile AppStateManager stateManager;

		private volatile ViewPort viewPort;

		private volatile AssetManager assetManager;

		RecordingGameDirector(GameConfiguration configuration) {
			this.configuration = configuration;
		}

		@Override
		public void init() {
			this.initializationCount++;
			this.initializationThread = Thread.currentThread();
			this.stateManager = this.configuration.engineStateManager();
			this.viewPort = this.configuration.engineViewPort();
			this.assetManager = this.configuration.assetManager();
			this.initialized.countDown();
		}

		@Override
		public void update(float tpf) {
		}
	}

	private static final class StartupDependency {
	}

	private static final class RecordingGameConfiguration extends GameConfiguration {

		@Autowired
		private StartupDependency startupDependency;

		private boolean startRequested;

		private boolean startObservedInjectedDependency;

		@Override
		public void start() {
			this.startRequested = true;
			this.startObservedInjectedDependency = this.startupDependency != null;
		}
	}
}
