package io.subnoize.fatdaddygames;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;

import io.subnoize.fatdaddygames.model.HighScore;
import io.subnoize.fatdaddygames.model.UserInterface;
import io.subnoize.fatdaddygames.service.HighScoreService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Exercises real Lemur leaderboard widgets and click commands in a headless JME fixture.
 * These checks preserve paging behavior and verify the game-over leaderboard and actions fit
 * supported viewports without launching the native game window or using a database.
 */
class MyGameDirectorUiTests {

	@BeforeAll
	static void initializeHeadlessLemur() {
		Application application = mock(Application.class);
		JmeContext context = mock(JmeContext.class);
		given(context.getType()).willReturn(JmeContext.Type.Headless);
		AssetManager assetManager = new DesktopAssetManager(true);
		assetManager.registerLocator("", ClasspathLocator.class);
		given(application.getContext()).willReturn(context);
		given(application.getAssetManager()).willReturn(assetManager);
		GuiGlobals.initialize(application);
	}

	@Test
	void mainMenuPagingReopeningAndEmptyResetRebuildActualWidgets() {
		HighScoreServiceStub service = new HighScoreServiceStub(23);
		MyGameDirector director = director(service, new Node());
		UserInterface userInterface = new UserInterface();
		AppSettings settings = new AppSettings(true);
		settings.setResolution(640, 480);
		ReflectionTestUtils.setField(userInterface, "settings", settings);
		ReflectionTestUtils.setField(director, "userI", userInterface);
		Container scoreMenu = new Container();
		ReflectionTestUtils.setField(director, "scoreMenu", scoreMenu);

		ReflectionTestUtils.invokeMethod(director, "showScorePage", 0);
		assertThat(scoreMenu.getChildren()).hasSize(14);
		assertThat(buttons(scoreMenu)).extracting(Button::getText).containsExactly("Next", "Back");
		assertThat(labels(scoreMenu)).filteredOn(text -> text.startsWith("#")).hasSize(10);

		button(scoreMenu, "Next").click();
		assertThat(labels(scoreMenu)).anyMatch(text -> text.startsWith("#11:"));
		assertThat(buttons(scoreMenu)).extracting(Button::getText).containsExactly("Previous", "Next", "Back");
		button(scoreMenu, "Previous").click();
		assertThat(labels(scoreMenu)).anyMatch(text -> text.startsWith("#1:"));
		assertThat(service.requestedPages).containsExactly(0, 1, 0);

		service.resetScores();
		ReflectionTestUtils.invokeMethod(director, "showScorePage", 0);
		assertThat(labels(scoreMenu)).contains("No high scores yet.", "Page 1 of 1");
		assertThat(buttons(scoreMenu)).extracting(Button::getText).containsExactly("Back");
	}

	@Test
	void gameOverPagingPreservesLabelReplayAndReturnActions() {
		HighScoreServiceStub service = new HighScoreServiceStub(23);
		Node guiNode = new Node();
		MyGameDirector director = director(service, guiNode);
		UserInterface userInterface = new UserInterface();
		AppSettings settings = new AppSettings(true);
		settings.setResolution(640, 480);
		ReflectionTestUtils.setField(userInterface, "settings", settings);
		ReflectionTestUtils.setField(userInterface, "guiNode", guiNode);
		ReflectionTestUtils.setField(director, "userI", userInterface);
		Container gameOverMenu = new Container();
		gameOverMenu.addChild(new Label("Game over... Your score is 7"));
		ReflectionTestUtils.setField(director, "gameOverMenu", gameOverMenu);
		ReflectionTestUtils.setField(director, "menuPanel", new Container());

		ReflectionTestUtils.invokeMethod(director, "gameOverButtonCommands");
		Button replay = button(gameOverMenu, "Try again!");
		Button back = button(gameOverMenu, "Return to Main Menu");
		Container scoresPanel = childContainers(gameOverMenu).get(0);
		assertThat(labels(gameOverMenu)).contains("Game over... Your score is 7");
		assertThat(labels(scoresPanel)).filteredOn(text -> text.startsWith("#")).hasSize(10);

		button(scoresPanel, "Next").click();
		assertThat(labels(scoresPanel)).anyMatch(text -> text.startsWith("#11:"));
		assertThat(gameOverMenu.getChildren()).contains(replay, back, scoresPanel);
		assertThat(labels(gameOverMenu)).contains("Game over... Your score is 7");
		assertThat(buttons(gameOverMenu)).containsExactly(replay, back);
		assertThat(service.requestedPages).containsExactly(0, 1);
	}

	@Test
	void gameOverLayoutFitsTenRowsAndActionsAtSmallAndNormalViewports() {
		for (int[] viewport : List.of(new int[] { 640, 480 }, new int[] { 800, 600 })) {
			AppSettings settings = new AppSettings(true);
			settings.setResolution(viewport[0], viewport[1]);
			Node guiNode = new Node();
			UserInterface userInterface = new UserInterface();
			ReflectionTestUtils.setField(userInterface, "settings", settings);
			ReflectionTestUtils.setField(userInterface, "guiNode", guiNode);
			Container gameOverMenu = userInterface.initGameOver();
			gameOverMenu.addChild(new Label("Game over... Your score is 7"));
			HighScoreServiceStub service = new HighScoreServiceStub(23);
			MyGameDirector director = director(service, guiNode);
			ReflectionTestUtils.setField(director, "gameOverMenu", gameOverMenu);
			ReflectionTestUtils.setField(director, "userI", userInterface);

			ReflectionTestUtils.invokeMethod(director, "gameOverButtonCommands");
			assertThat(labels(childContainers(gameOverMenu).get(0)))
					.filteredOn(text -> text.startsWith("#")).hasSize(10);
			var preferredSize = gameOverMenu.getPreferredSize();
			var position = gameOverMenu.getLocalTranslation();
			assertThat(position.x).as("left bound at %dx%d", viewport[0], viewport[1]).isGreaterThanOrEqualTo(20f);
			assertThat(position.x + preferredSize.x).as("right bound at %dx%d", viewport[0], viewport[1])
					.isLessThanOrEqualTo(viewport[0] - 20f);
			assertThat(position.y).as("top bound at %dx%d", viewport[0], viewport[1])
				.isLessThanOrEqualTo(viewport[1] - 20f);
			assertThat(position.y - preferredSize.y).as("bottom bound at %dx%d", viewport[0], viewport[1])
					.isGreaterThanOrEqualTo(20f);
		}
	}

	@Test
	void bothLeaderboardPanelsFitFullPagesAcrossPagingAtSmallAndNormalViewports() {
		for (int[] viewport : List.of(new int[] { 640, 480 }, new int[] { 800, 600 })) {
			AppSettings settings = new AppSettings(true);
			settings.setResolution(viewport[0], viewport[1]);
			Node guiNode = new Node();
			UserInterface userInterface = new UserInterface();
			ReflectionTestUtils.setField(userInterface, "settings", settings);
			ReflectionTestUtils.setField(userInterface, "guiNode", guiNode);
			HighScoreServiceStub service = new HighScoreServiceStub(23);
			MyGameDirector director = director(service, guiNode);
			ReflectionTestUtils.setField(director, "userI", userInterface);

			Container scoreMenu = userInterface.initScores();
			ReflectionTestUtils.setField(director, "scoreMenu", scoreMenu);
			ReflectionTestUtils.invokeMethod(director, "showScorePage", 0);
			assertThat(labels(scoreMenu).stream().filter(text -> text.startsWith("#")).toList()).hasSize(10);
			assertWithinViewport(scoreMenu, viewport[0], viewport[1]);
			button(scoreMenu, "Next").click();
			assertThat(labels(scoreMenu)).contains("Page 2 of 3");
			assertThat(buttons(scoreMenu)).extracting(Button::getText)
					.containsExactly("Previous", "Next", "Back");
			assertWithinViewport(scoreMenu, viewport[0], viewport[1]);
			button(scoreMenu, "Previous").click();
			assertThat(labels(scoreMenu)).contains("Page 1 of 3");
			assertWithinViewport(scoreMenu, viewport[0], viewport[1]);

			Container gameOverMenu = userInterface.initGameOver();
			gameOverMenu.addChild(new Label("Game over... Your score is 7"));
			ReflectionTestUtils.setField(director, "gameOverMenu", gameOverMenu);
			ReflectionTestUtils.setField(director, "menuPanel", new Container());
			ReflectionTestUtils.invokeMethod(director, "gameOverButtonCommands");
			Container scoresPanel = childContainers(gameOverMenu).get(0);
			Button replay = button(gameOverMenu, "Try again!");
			Button back = button(gameOverMenu, "Return to Main Menu");
			assertWithinViewport(gameOverMenu, viewport[0], viewport[1]);
			button(scoresPanel, "Next").click();
			assertThat(labels(scoresPanel)).contains("Page 2 of 3");
			assertThat(gameOverMenu.getChildren()).contains(replay, back, scoresPanel);
			assertWithinViewport(gameOverMenu, viewport[0], viewport[1]);
			button(scoresPanel, "Previous").click();
			assertThat(labels(scoresPanel)).contains("Page 1 of 3");
			assertWithinViewport(gameOverMenu, viewport[0], viewport[1]);
		}
	}

	private static void assertWithinViewport(Container panel, int width, int height) {
		var preferredSize = panel.getPreferredSize();
		var position = panel.getLocalTranslation();
		assertThat(position.x).as("left bound at %dx%d", width, height).isGreaterThanOrEqualTo(20f);
		assertThat(position.x + preferredSize.x).as("right bound at %dx%d", width, height)
				.isLessThanOrEqualTo(width - 20f);
		assertThat(position.y).as("top bound at %dx%d", width, height).isLessThanOrEqualTo(height - 20f);
		assertThat(position.y - preferredSize.y).as("bottom bound at %dx%d", width, height)
				.isGreaterThanOrEqualTo(20f);
	}

	private static MyGameDirector director(HighScoreService service, Node guiNode) {
		MyGameDirector director = new MyGameDirector();
		ReflectionTestUtils.setField(director, "highScoreServ", service);
		ReflectionTestUtils.setField(director, "guiNode", guiNode);
		return director;
	}

	private static Button button(Container container, String text) {
		return buttons(container).stream().filter(candidate -> text.equals(candidate.getText())).findFirst().orElseThrow();
	}

	private static List<Button> buttons(Container container) {
		return container.getChildren().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
	}

	private static List<Container> childContainers(Container container) {
		return container.getChildren().stream().filter(Container.class::isInstance).map(Container.class::cast).toList();
	}

	private static List<String> labels(Container container) {
		return container.getChildren().stream().filter(Label.class::isInstance).map(Label.class::cast)
				.map(Label::getText).toList();
	}

	private static Page<HighScore> page(int requestedPage, int total) {
		if (total == 0) {
			return new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
		}
		int pageIndex = Math.max(0, Math.min(requestedPage, (total - 1) / 10));
		int size = Math.min(10, total - pageIndex * 10);
		List<HighScore> scores = IntStream.range(0, size)
				.mapToObj(index -> new HighScore("player-" + (pageIndex * 10 + index + 1), 100 - index)).toList();
		return new PageImpl<>(scores, PageRequest.of(pageIndex, 10), total);
	}

	/**
	 * Supplies deterministic leaderboard pages without bytecode-mocking the final application
	 * service, keeping headless UI tests independent of the configured JDK's instrumentation support.
	 */
	private static final class HighScoreServiceStub extends HighScoreService {

		private final List<Integer> requestedPages = new ArrayList<>();

		private int total;

		HighScoreServiceStub(int total) {
			super(null);
			this.total = total;
		}

		@Override
		public Page<HighScore> getScorePage(int requestedPage) {
			this.requestedPages.add(requestedPage);
			return page(requestedPage, this.total);
		}

		@Override
		public void resetScores() {
			this.total = 0;
		}
	}

}
