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

	public BitmapText initHud() {
		BitmapText hudText = new BitmapText(configuration.guiFont());
		hudText.setSize(configuration.guiFont().getCharSet().getRenderedSize()); // font size
		hudText.setColor(ColorRGBA.White); // font color
		hudText.setText("Your score: 0"); // the text
		hudText.setLocalTranslation(settings.getWidth() / 2, settings.getHeight(), 0); // position
		return hudText;
	}

	public BitmapText initKeys() {
		BitmapText keyText = new BitmapText(configuration.guiFont());
		keyText.setSize(configuration.guiFont().getCharSet().getRenderedSize());
		keyText.setColor(ColorRGBA.White);
		keyText.setText("Space, W, or up arrow to jump, P to play and pause, Backspace to reset, and ESC to quit."); // text
		keyText.setLocalTranslation(0, settings.getHeight(), 0);
		return keyText;
	}

	public Container initMenu() {

		// Create a simple container for our elements
		Container MenuPanel = new Container();
		guiNode.attachChild(MenuPanel);

		// Put it somewhere that we will see it.
		// Note: Lemur GUI elements grow down from the upper left corner.
		MenuPanel.setLocalTranslation(settings.getWidth() / 2.9f, settings.getHeight() / 2, 0);

		// Add some elements
		MenuPanel.addChild(new Label("Welcome to the Snowman Game!"));

		return MenuPanel;
	}

	public Container initSettings() {

		// Create a simple container for our elements
		Container settingsPanel = new Container();
		guiNode.attachChild(settingsPanel);

		// Put it somewhere that we will see it.
		// Note: Lemur GUI elements grow down from the upper left corner.
		settingsPanel.setLocalTranslation(settings.getWidth() / 2.9f, settings.getHeight() / 2, 0);

		// Add some elements
		settingsPanel.addChild(new Label("Settings. To be made later."));

		return settingsPanel;
	}

	public Container initGameOver() {

		// Create a simple container for our elements
		Container gameOverPanel = new Container();
		guiNode.attachChild(gameOverPanel);

		// Put it somewhere that we will see it.
		// Note: Lemur GUI elements grow down from the upper left corner.
		gameOverPanel.setLocalTranslation(settings.getWidth() / 2.9f, settings.getHeight() / 2, 0);

		// Add some elements
		gameOverPanel.addChild(new Label("Game over... Your score is "));

		return gameOverPanel;
	}

}
