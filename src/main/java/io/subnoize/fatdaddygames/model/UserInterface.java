package io.subnoize.fatdaddygames.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import io.subnoize.fatdaddygames.configuration.GameConfiguration;

@Component
public class UserInterface {
	
	@Autowired
	private GameConfiguration configuration;

	@Autowired
	private AppSettings settings;

	@Autowired
	private Node guiNode;

	/**
	 * Initializes the head's up display that has the score.
	 * @return Returns the BitmapText of the score hud.
	 */
	public BitmapText initHud(int highScore) {
		BitmapText hudText = new BitmapText(configuration.guiFont());
		hudText.setSize(configuration.guiFont().getCharSet().getRenderedSize()); // font size
		hudText.setColor(ColorRGBA.White); // font color
		hudText.setText("Your score: 0   Highest score: " + highScore); // the text
		hudText.setLocalTranslation(settings.getWidth() / 2, settings.getHeight(), 0); // position
		return hudText;
	}

	/**
	 * Initializes the head's up display that contains the keybindings.
	 * @return Returns the BitmapText of the keybinding hud.
	 */
	public BitmapText initKeys() {
		BitmapText keyText = new BitmapText(configuration.guiFont());
		keyText.setSize(configuration.guiFont().getCharSet().getRenderedSize());
		keyText.setColor(ColorRGBA.White);
		keyText.setText("Space, W, or up arrow to jump, P to play and pause, Backspace to reset, and ESC to quit."); // text
		keyText.setLocalTranslation(0, settings.getHeight(), 0);
		return keyText;
	}
	
	/**
	 * Initializes the main menu.
	 * @return Returns the container with the main menu.
	 */
	public Container initMenu() {

		Container MenuPanel = new Container();
		guiNode.attachChild(MenuPanel);

		MenuPanel.setLocalTranslation(settings.getWidth() / 2.9f, settings.getHeight() / 2, 0);

		MenuPanel.addChild(new Label("Welcome to the Snowman Game!"));

		return MenuPanel;
	}
	
	/**
	 * Initializes the menu for the settings page.
	 * @return Returns the container with the settings.
	 */
	public Container initSettings() {

		Container settingsPanel = new Container();

		settingsPanel.setLocalTranslation(settings.getWidth() / 2.9f, settings.getHeight() / 2, 0);

		settingsPanel.addChild(new Label("Settings. To be made later."));

		return settingsPanel;
	}

	/**
	 * Initializes the menu when losing the game.
	 * @return Returns the container with the Game Over menu.
	 */
	public Container initGameOver() {

		Container gameOverPanel = new Container();

		gameOverPanel.setLocalTranslation(settings.getWidth() / 2.9f, settings.getHeight() / 2, 0);

		return gameOverPanel;
	}
	
	/**
	 * Displays the game over menu.
	 * @param gameOverPanel The Container holding the game over menu
	 * @param score The score the player has
	 * @return Returns the edited Game Over Container.
	 */
	public Container displayGameOver(Container gameOverPanel, int score) {
		
		guiNode.attachChild(gameOverPanel);

		gameOverPanel.detachAllChildren();
		gameOverPanel.addChild(new Label("Game over... Your score is " + score));

		return gameOverPanel;
	}

}
