package io.subnoize.fatdaddygames.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.system.AppSettings;

import io.subnoize.fatdaddygames.configuration.GameConfiguration;

@Component
public class UserInterface {

	@Autowired
	private GameConfiguration configuration;

	@Autowired
	private AppSettings settings;

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

	public BitmapText initMenu() {
		BitmapText menuText = new BitmapText(configuration.guiFont());
		menuText.setSize(configuration.guiFont().getCharSet().getRenderedSize() + 10);
		menuText.setColor(ColorRGBA.White);
		menuText.setText("Welcome to the Snowman Game! Press P to play!");
		menuText.setLocalTranslation(settings.getWidth() / 2.9f, settings.getHeight() / 2, 0);
		return menuText;
	}

}
