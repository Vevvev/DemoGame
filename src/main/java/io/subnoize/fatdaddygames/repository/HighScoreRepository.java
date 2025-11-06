package io.subnoize.fatdaddygames.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.subnoize.fatdaddygames.model.HighScore;

public interface HighScoreRepository extends JpaRepository<HighScore, Long> {
	
	List<HighScore> findTop10ByOrderByScoreDesc();
	
	void deleteAllByIdNotIn(Collection<Long> ids);
}
