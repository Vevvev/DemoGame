package io.subnoize.fatdaddygames.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.subnoize.fatdaddygames.model.HighScore;
import io.subnoize.fatdaddygames.repository.HighScoreRepository;

/**
 * Stores a bounded local leaderboard and serves database-backed pages. Retaining the best
 * thousand scores preserves the game's finite-storage policy while allowing players to
 * browse beyond the first ten entries; score ties consistently favor earlier records.
 */
@Service
public class HighScoreService {

	private static final int PAGE_SIZE = 10;

	private static final Sort SCORE_ORDER = Sort.by(Sort.Order.desc("score"), Sort.Order.asc("id"));

	private final HighScoreRepository highScoreRepo;

	/**
	 * Creates the service around the repository used by both score submission and the
	 * leaderboard screens.
	 *
	 * @param highScoreRepo persistent high-score rows
	 */
	public HighScoreService(HighScoreRepository highScoreRepo) {
		this.highScoreRepo = highScoreRepo;
	}

	/**
	 * Saves a score and prunes rows outside the deterministic best-thousand order in one
	 * transaction. The returned list remains the first ten entries for the existing
	 * game-over caller; paging is exposed separately so the UI never loads the whole table.
	 *
	 * @param player player name recorded with the score
	 * @param score score achieved in the game
	 * @return the first page of retained scores, ordered by score descending then ID ascending
	 */
	@Transactional
	public List<HighScore> recordScore(String player, int score) {
		this.highScoreRepo.save(new HighScore(player, score));
		List<HighScore> retainedScores = this.highScoreRepo.findTop1000ByOrderByScoreDescIdAsc();
		this.highScoreRepo.deleteAllByIdNotIn(retainedScores.stream().map(HighScore::getId).toList());
		return this.highScoreRepo.findAll(PageRequest.of(0, PAGE_SIZE, SCORE_ORDER)).getContent();
	}

	/**
	 * Loads one bounded leaderboard page from the database. Invalid negative requests start
	 * at page one, and stale requests beyond the end are clamped to the current last page;
	 * the empty leaderboard remains represented by its single empty first page.
	 *
	 * @param requestedPage zero-based page index requested by the screen
	 * @return a database-backed page with continuous ordering metadata
	 */
	@Transactional(readOnly = true)
	public Page<HighScore> getScorePage(int requestedPage) {
		int pageIndex = Math.max(0, requestedPage);
		Pageable pageable = PageRequest.of(pageIndex, PAGE_SIZE, SCORE_ORDER);
		Page<HighScore> page = this.highScoreRepo.findAll(pageable);
		if (page.getTotalElements() == 0 && pageIndex > 0) {
			page = this.highScoreRepo.findAll(PageRequest.of(0, PAGE_SIZE, SCORE_ORDER));
		} else if (page.getTotalElements() > 0 && pageIndex >= page.getTotalPages()) {
			page = this.highScoreRepo.findAll(PageRequest.of(page.getTotalPages() - 1, PAGE_SIZE, SCORE_ORDER));
		}
		return page;
	}

	/**
	 * Deletes all local scores, preserving the existing settings action's reset semantics.
	 */
	@Transactional
	public void resetScores() {
		this.highScoreRepo.deleteAll();
	}
}
