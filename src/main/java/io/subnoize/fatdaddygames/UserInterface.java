package io.subnoize.fatdaddygames;

import org.springframework.stereotype.Component;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.system.AppSettings;

@Component
public class UserInterface {

	public BitmapText initHud(BitmapFont guiFont, AppSettings settings) {
		BitmapText hudText = new BitmapText(guiFont);
		hudText.setSize(guiFont.getCharSet().getRenderedSize()); // font size
		hudText.setColor(ColorRGBA.White); // font color
		hudText.setText("Your score: 0"); // the text
		hudText.setLocalTranslation(settings.getWidth() / 2, settings.getHeight(), 0); // position
		return hudText;
	}

	public BitmapText initKeys(BitmapFont guiFont, AppSettings settings) {
		BitmapText keyText = new BitmapText(guiFont);
		keyText.setSize(guiFont.getCharSet().getRenderedSize()); // font size
		keyText.setColor(ColorRGBA.White); // font color
		keyText.setText("Space, W, or up arrow to jump, P to play and pause, Backspace to reset, and ESC to quit."); // the																									// text
		keyText.setLocalTranslation(0, settings.getHeight(), 0); // position
		return keyText;
	}

	public BitmapText initMenu(BitmapFont guiFont, AppSettings settings) {
		BitmapText menuText = new BitmapText(guiFont);
		menuText.setSize(guiFont.getCharSet().getRenderedSize() + 10); // font size
		menuText.setColor(ColorRGBA.White); // font color
		menuText.setText("Welcome to the Snowman Game! Press P to play!"); // the text
		menuText.setLocalTranslation(settings.getWidth() / 2.9f, settings.getHeight() / 2, 0); // position
		return menuText;
	}

}
