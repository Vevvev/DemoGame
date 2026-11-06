package io.subnoize.fatdaddygames.configuration;

import java.util.Optional;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
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
import com.jme3.input.controls.ActionListener;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.Timer;

import io.subnoize.fatdaddygames.MyGameDirector;

@Configuration
@EnableScheduling
public class GameConfiguration extends SimpleApplication implements ActionListener {

	private GameDirector gameDirector;
	
	/**
	 * Starts the engine only after Spring has completed singleton creation and injection,
	 * preventing the original controls-null race in {@link MyGameDirector#init()}. Runtime
	 * AppStateManager and ViewPort references are resolved by that engine-thread callback,
	 * not published as Spring beans while jME still holds them as null. UI consumers query
	 * the active settings from this application after jME applies its startup/dialog choices,
	 * avoiding a stale resolution captured during Spring context creation.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void startOnApplicationReady() {
		this.start();
	}
	
	@Bean
	GameDirector getGameDirector() {
		gameDirector = new MyGameDirector();
		return gameDirector;
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

	public BitmapFont guiFont() {
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

	public AssetManager assetManager() {
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

	/**
	 * Returns the initialized render viewport for engine-thread scene setup.
	 *
	 * @return runtime viewport, available after jME initialization
	 */
	public ViewPort engineViewPort() {
		return this.viewPort;
	}

	@Bean
	ViewPort guiViewPort() {
		return guiViewPort;
	}

	@Bean
	JmeContext context() {
		return context;
	}

	/**
	 * Returns the active engine settings after jME initialization. Do not publish this value
	 * as an eager Spring bean: jME may replace it while creating the context or applying the
	 * settings dialog, and UI consumers must read the final engine resolution on the engine thread.
	 *
	 * @return active application settings; null only before jME creates its context
	 */
	public AppSettings settings() {
		return this.settings;
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

	public InputManager inputManager() {
		return inputManager;
	}

	/**
	 * Returns jME's initialized state manager for engine-thread game setup.
	 *
	 * @return runtime application state manager, available after jME initialization
	 */
	public AppStateManager engineStateManager() {
		return this.stateManager;
	}

	@Bean
	AppProfiler prof() {
		return prof;
	}

	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		Optional.ofNullable(gameDirector).ifPresent(gd -> {
			((ActionListener)gd).onAction(name, isPressed, tpf);
		});
	}

}
