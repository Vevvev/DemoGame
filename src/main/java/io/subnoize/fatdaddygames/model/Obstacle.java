package io.subnoize.fatdaddygames.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import io.subnoize.fatdaddygames.configuration.GameConfiguration;

@Component
public class Obstacle {

	@Autowired
	private GameConfiguration configuration;

	private static final Vector3f obstacleDefault = new Vector3f(10f, -1f, 0f);

	public Node makeObstacle() {

		Node obstacle = (Node) configuration.assetManager().loadModel("Models/Oto/Oto.mesh.xml");
		obstacle.setLocalScale(0.19f);
		obstacle.setLocalTranslation(obstacleDefault);
		obstacle.rotate(0, -1.5f, 0);

		return obstacle;
	}
}
