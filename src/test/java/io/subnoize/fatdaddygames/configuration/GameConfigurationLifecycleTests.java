package io.subnoize.fatdaddygames.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Protects the Spring-to-JME startup boundary: Spring must finish creating and injecting
 * singleton dependencies before the native engine thread can initialize game controls.
 * The recording application keeps this lifecycle regression headless and window-free.
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
