package io.subnoize.fatdaddygames;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.Listener;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.Timer;

@Configuration
@EnableScheduling
public class GameConfiguration extends SimpleApplication {

	private GameDirector gameDirector;
	
	public GameDirector getGameDirector() {
		return gameDirector;
	}

	public void setGameDirector(GameDirector gameDirector) {
		this.gameDirector = gameDirector;
	}

	@Override
	public void simpleInitApp() {
		// TODO Auto-generated method stub
		Optional.ofNullable(gameDirector).ifPresent(gd -> {
			gd.init();
		});
	}

	@Override
	public void simpleUpdate(float tpf) {

		Optional.ofNullable(gameDirector).ifPresent(gd -> {
			gd.update(tpf);
		});
	}

	@Bean
	FlyByCamera flyByCamera() {
		return flyCam;
	}

	@Bean
	Node rootNode() {
		return rootNode;
	}

	@Bean
	Node guiNode() {
		return guiNode;
	}

	@Bean
	BitmapFont guiFont() {
		return guiFont;
	}

	@Bean
	boolean showSettings() {
		return showSettings;
	}

	@Bean
	BitmapText fpsText() {
		return fpsText;
	}

	@Bean
	AssetManager assetManager() {
		return assetManager;
	}

	@Bean
	AudioRenderer audioRenderer() {
		return audioRenderer;
	}

	@Bean
	Renderer renderer() {
		return renderer;
	}

	@Bean
	RenderManager renderManager() {
		return renderManager;
	}

	@Bean
	ViewPort viewPort() {
		return viewPort;
	}

	@Bean
	ViewPort guiViewPort() {
		return guiViewPort;
	}

	@Bean
	JmeContext context() {
		return context;
	}

	@Bean
	AppSettings settings() {
		return settings;
	}

	@Bean
	Timer timer() {
		return timer;
	}

	@Bean
	Listener listener() {
		return listener;
	}

	@Bean
	boolean inputEnabled() {
		return inputEnabled;
	}

	@Bean
	LostFocusBehavior lostFocusBehavior() {
		return lostFocusBehavior;
	}

	@Bean
	float speed() {
		return speed;
	}

	@Bean
	boolean paused() {
		return paused;
	}

	@Bean
	MouseInput mouseInput() {
		return mouseInput;
	}

	@Bean
	KeyInput keyInput() {
		return keyInput;
	}

	@Bean
	JoyInput joyInput() {
		return joyInput;
	}

	@Bean
	TouchInput touchInput() {
		return touchInput;
	}

	@Bean
	InputManager inputManager() {
		return inputManager;
	}

	@Bean
	AppStateManager stateManager() {
		return stateManager;
	}

	@Bean
	AppProfiler prof() {
		return prof;
	}

}
