package io.subnoize.fatdaddygames.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.subnoize.fatdaddygames.model.HighScore;
import io.subnoize.fatdaddygames.repository.HighScoreRepository;

@Service
public class HighScoreService {

	@Autowired
	private HighScoreRepository highScoreRepo;

	@Transactional
	public List<HighScore> recordScore(String player, int score) {
		highScoreRepo.save(new HighScore(player, score)); // saves the score.
		List<HighScore> scores = highScoreRepo.findTop10ByOrderByScoreDesc();
		highScoreRepo.deleteAllByIdNotIn(scores.stream().map(HighScore::getId).toList()); // done to keep the database
																							// small.
		return scores;
	}
}
