package io.subnoize.fatdaddygames;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;

import io.subnoize.fatdaddygames.model.HighScore;
import io.subnoize.fatdaddygames.repository.HighScoreRepository;
import io.subnoize.fatdaddygames.service.HighScoreService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises leaderboard retention and paging against an isolated in-memory database. The
 * slice deliberately avoids the native game configuration so tests never start graphics or
 * access the player's file-backed score store.
 */
@DataJpaTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:highscore-tests;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop" })
@Import(HighScoreService.class)
class HighScoreServiceTests {

	@Autowired
	private HighScoreRepository highScoreRepository;

	@Autowired
	private HighScoreService highScoreService;

	@Test
	void recordScorePreservesRowsWhileBelowRetentionCap() {
		this.highScoreRepository.saveAll(List.of(new HighScore("first", 20), new HighScore("second", 10)));
		this.highScoreRepository.flush();

		List<HighScore> firstPage = this.highScoreService.recordScore("third", 30);

		assertThat(this.highScoreRepository.count()).isEqualTo(3);
		assertThat(firstPage).extracting(HighScore::getPlayerName).containsExactly("third", "first", "second");
	}

	@Test
	void recordScoreKeepsBestThousandAndEvictsLowestWhenNewScoreIsHigher() {
		List<HighScore> seeded = IntStream.range(0, 1000)
				.mapToObj(index -> new HighScore("player-" + index, 1000 - index))
				.toList();
		this.highScoreRepository.saveAll(seeded);
		this.highScoreRepository.flush();
		Long lowestScoreId = seeded.get(999).getId();

		this.highScoreService.recordScore("new-leader", 2000);

		assertThat(this.highScoreRepository.count()).isEqualTo(1000);
		assertThat(this.highScoreRepository.existsById(lowestScoreId)).isFalse();
		assertThat(this.highScoreRepository.findTop1000ByOrderByScoreDescIdAsc())
				.extracting(HighScore::getPlayerName).first().isEqualTo("new-leader");
		Page<HighScore> lastPage = this.highScoreService.getScorePage(99);
		assertThat(lastPage.getContent()).hasSize(10);
		assertThat(lastPage.getContent().get(0).getPlayerName()).isEqualTo("player-989");
		assertThat(lastPage.getContent().get(9).getPlayerName()).isEqualTo("player-998");
		assertThat(lastPage.getTotalPages()).isEqualTo(100);
	}

	@Test
	void recordScoreDiscardsLowAndNewerTiedScoresAtTheRetentionBoundary() {
		List<HighScore> tiedScores = IntStream.range(0, 1000)
				.mapToObj(index -> new HighScore("older-" + index, 50))
				.toList();
		this.highScoreRepository.saveAll(tiedScores);
		this.highScoreRepository.flush();
		Long firstId = tiedScores.get(0).getId();

		this.highScoreService.recordScore("too-low", 49);
		this.highScoreService.recordScore("newer-tie", 50);

		assertThat(this.highScoreRepository.count()).isEqualTo(1000);
		assertThat(this.highScoreRepository.existsById(firstId)).isTrue();
		assertThat(this.highScoreRepository.findAll()).extracting(HighScore::getPlayerName)
				.doesNotContain("too-low", "newer-tie");
		List<Long> firstPageIds = this.highScoreService.getScorePage(0).getContent().stream()
				.map(HighScore::getId).toList();
		assertThat(firstPageIds).containsExactlyElementsOf(firstPageIds.stream().sorted().toList());
	}

	@Test
	void databasePagesCoverPartialEmptyAndOutOfRangeBoundaries() {
		this.highScoreRepository.saveAll(IntStream.range(0, 23)
				.mapToObj(index -> new HighScore("player-" + index, 100 - index)).toList());
		this.highScoreRepository.flush();

		Page<HighScore> firstPage = this.highScoreService.getScorePage(0);
		Page<HighScore> middlePage = this.highScoreService.getScorePage(1);
		Page<HighScore> lastPage = this.highScoreService.getScorePage(2);
		Page<HighScore> outOfRangePage = this.highScoreService.getScorePage(20);

		assertThat(firstPage.getContent()).hasSize(10);
		assertThat(middlePage.getContent()).hasSize(10);
		assertThat(lastPage.getContent()).hasSize(3);
		assertThat(firstPage.getContent().get(0).getPlayerName()).isEqualTo("player-0");
		assertThat(middlePage.getContent().get(0).getPlayerName()).isEqualTo("player-10");
		assertThat(lastPage.getContent().get(0).getPlayerName()).isEqualTo("player-20");
		assertThat(lastPage.getNumber()).isEqualTo(2);
		assertThat(outOfRangePage.getNumber()).isEqualTo(2);
		assertThat(lastPage.getTotalPages()).isEqualTo(3);

		this.highScoreService.resetScores();
		Page<HighScore> emptyPage = this.highScoreService.getScorePage(0);
		assertThat(emptyPage.getContent()).isEmpty();
		assertThat(emptyPage.getTotalElements()).isZero();
		assertThat(emptyPage.getTotalPages()).isZero();
		assertThat(this.highScoreService.getScorePage(5).getNumber()).isZero();
	}

}
