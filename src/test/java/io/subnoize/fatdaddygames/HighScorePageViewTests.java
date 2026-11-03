package io.subnoize.fatdaddygames;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import io.subnoize.fatdaddygames.model.HighScore;
import io.subnoize.fatdaddygames.ui.HighScorePageView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the shared presentation rules used by both leaderboard entry paths without
 * starting the native graphics application.
 */
class HighScorePageViewTests {

	@Test
	void formatsContinuousRanksAndExposesBothDirectionsOnMiddlePage() {
		HighScorePageView view = new HighScorePageView(page(1, 1000, 10));

		assertThat(view.scoreRows().get(0)).startsWith("#11:");
		assertThat(view.pageIndicator()).isEqualTo("Page 2 of 100");
		assertThat(view.hasPrevious()).isTrue();
		assertThat(view.hasNext()).isTrue();
	}

	@Test
	void formatsRanksThroughOneThousandAndHidesNextOnLastPage() {
		HighScorePageView view = new HighScorePageView(page(99, 1000, 10));

		assertThat(view.scoreRows().get(0)).startsWith("#991:");
		assertThat(view.scoreRows().get(9)).startsWith("#1000:");
		assertThat(view.pageIndicator()).isEqualTo("Page 100 of 100");
		assertThat(view.hasPrevious()).isTrue();
		assertThat(view.hasNext()).isFalse();
	}

	@Test
	void representsEmptyAndPartialLastPagesWithoutInvalidNavigation() {
		HighScorePageView emptyView = new HighScorePageView(new PageImpl<>(List.of()));
		HighScorePageView partialView = new HighScorePageView(page(2, 23, 3));

		assertThat(emptyView.isEmpty()).isTrue();
		assertThat(emptyView.pageIndicator()).isEqualTo("Page 1 of 1");
		assertThat(emptyView.hasPrevious()).isFalse();
		assertThat(emptyView.hasNext()).isFalse();
		assertThat(partialView.scoreRows()).hasSize(3);
		assertThat(partialView.scoreRows().get(0)).startsWith("#21:");
		assertThat(partialView.scoreRows().get(2)).startsWith("#23:");
		assertThat(partialView.hasPrevious()).isTrue();
		assertThat(partialView.hasNext()).isFalse();
	}

	private Page<HighScore> page(int pageIndex, int total, int contentSize) {
		List<HighScore> scores = IntStream.range(0, contentSize)
				.mapToObj(index -> new HighScore("player-" + index, 100 - index)).toList();
		return new PageImpl<>(scores, PageRequest.of(pageIndex, 10), total);
	}

}
