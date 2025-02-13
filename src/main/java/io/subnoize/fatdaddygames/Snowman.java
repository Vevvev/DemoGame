package io.subnoize.fatdaddygames;

import org.springframework.stereotype.Component;

import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Vector3f;

@Component
public class Snowman {
	
	private static final Vector3f playerDefault = new Vector3f(-3f, 10f, 0f);

	public CharacterControl makeSnowman() {
		
		CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1f, 0.7f, 1);
		CharacterControl player = new CharacterControl(capsuleShape, 0.05f);
		player.setJumpSpeed(15);
		player.setFallSpeed(30);
		player.setGravity(30);
		player.setPhysicsLocation(playerDefault);
		player.isContactResponse();
		
		return player;
	}
}
