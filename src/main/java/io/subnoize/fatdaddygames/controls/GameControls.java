package io.subnoize.fatdaddygames.controls;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;

import io.subnoize.fatdaddygames.configuration.GameConfiguration;

@Component
public class GameControls {
	
	@Autowired
	private GameConfiguration configuration;

	/**
	 * Sets up the key bindings for the game.
	 */
	public void setUpKeys() {
		configuration.inputManager().addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
		configuration.inputManager().addMapping("Jump", new KeyTrigger(KeyInput.KEY_UP));
		configuration.inputManager().addMapping("Jump", new KeyTrigger(KeyInput.KEY_W));
		configuration.inputManager().addMapping("Pause", new KeyTrigger(KeyInput.KEY_P));
		configuration.inputManager().addMapping("Reset", new KeyTrigger(KeyInput.KEY_BACK));
		configuration.inputManager().addListener(configuration, "Jump");
		configuration.inputManager().addListener(configuration, "Pause");
		configuration.inputManager().addListener(configuration, "Reset");
	}
}
