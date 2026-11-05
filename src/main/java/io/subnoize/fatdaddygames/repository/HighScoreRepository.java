package io.subnoize.fatdaddygames.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.subnoize.fatdaddygames.model.HighScore;

/**
 * Persistence operations for the local leaderboard. The ordered bounded lookup is used
 * during score submission to retain only the best thousand rows without materializing the
 * full table for the menu or game-over screens.
 */
public interface HighScoreRepository extends JpaRepository<HighScore, Long> {

	List<HighScore> findTop1000ByOrderByScoreDescIdAsc();

	void deleteAllByIdNotIn(Collection<Long> ids);
}
