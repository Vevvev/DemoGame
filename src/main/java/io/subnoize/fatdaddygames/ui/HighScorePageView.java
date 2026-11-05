package io.subnoize.fatdaddygames.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;

import io.subnoize.fatdaddygames.model.HighScore;

/**
 * Provides the shared row labels and navigation state for the main-menu and game-over
 * leaderboard screens. Keeping rank calculation beside page metadata prevents the two entry
 * paths from restarting ranks at ten-row boundaries or disagreeing about the empty state.
 */
public final class HighScorePageView {

	private final Page<HighScore> page;

	/**
	 * Creates a display view over one database-backed score page.
	 *
	 * @param page ordered high-score page returned by the service
	 */
	public HighScorePageView(Page<HighScore> page) {
		this.page = Objects.requireNonNull(page);
	}

	/**
	 * Formats this page's rows with ranks that continue across page boundaries.
	 *
	 * @return immutable display rows in score order
	 */
	public List<String> scoreRows() {
		List<String> rows = new ArrayList<>();
		for (int rowIndex = 0; rowIndex < this.page.getContent().size(); rowIndex++) {
			HighScore highScore = this.page.getContent().get(rowIndex);
			int rank = this.page.getNumber() * this.page.getSize() + rowIndex + 1;
			rows.add("#" + rank + ": " + highScore.getPlayerName() + " with a score of " + highScore.getScore());
		}
		return List.copyOf(rows);
	}

	/**
	 * @return a human-readable one-based page indicator; empty data is shown as Page 1 of 1
	 */
	public String pageIndicator() {
		return "Page " + (this.page.getNumber() + 1) + " of " + Math.max(1, this.page.getTotalPages());
	}

	/**
	 * @return whether this page has no score rows
	 */
	public boolean isEmpty() {
		return this.page.isEmpty();
	}

	/**
	 * @return whether a previous page can be displayed
	 */
	public boolean hasPrevious() {
		return this.page.hasPrevious();
	}

	/**
	 * @return whether a next page can be displayed
	 */
	public boolean hasNext() {
		return this.page.hasNext();
	}

}
