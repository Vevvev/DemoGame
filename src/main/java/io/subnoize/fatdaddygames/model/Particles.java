package io.subnoize.fatdaddygames.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

import io.subnoize.fatdaddygames.configuration.GameConfiguration;

@Component
public class Particles {
	
	@Autowired
	private GameConfiguration configuration;

	public ParticleEmitter makeBlueFire() {
		
		Material mat_blue = new Material(configuration.assetManager(), "Common/MatDefs/Misc/Particle.j3md");
		mat_blue.setTexture("Texture", configuration.assetManager().loadTexture("Effects/Explosion/flame.png"));
		
		ParticleEmitter bFire = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
		bFire.setMaterial(mat_blue);
		bFire.setImagesX(2);
		bFire.setImagesY(2);
		bFire.setStartColor(ColorRGBA.Blue);
		bFire.setEndColor(ColorRGBA.Cyan);
		bFire.getParticleInfluencer().setInitialVelocity(new Vector3f(0, -5, 0));
		bFire.setStartSize(0.5f);
		bFire.setEndSize(1.5f);
		bFire.setGravity(0, 1, 0);
		bFire.setLowLife(0.5f);
		bFire.setHighLife(0.5f);
		bFire.getParticleInfluencer().setVelocityVariation(0.1f);
		bFire.setLocalTranslation(10, 10, 0);
		
		return bFire;
	}
	
	public ParticleEmitter makeRedFire() {
		
		Material mat_red = new Material(configuration.assetManager(), "Common/MatDefs/Misc/Particle.j3md");
		mat_red.setTexture("Texture", configuration.assetManager().loadTexture("Effects/Explosion/flame.png"));
		
		ParticleEmitter fire = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
		fire.setMaterial(mat_red);
		fire.setImagesX(2);
		fire.setImagesY(2); // 2x2 texture animation
		fire.setEndColor(new ColorRGBA(1f, 0f, 0f, 1f)); // red
		fire.setStartColor(new ColorRGBA(1f, 1f, 0f, 0.5f)); // yellow
		fire.getParticleInfluencer().setInitialVelocity(new Vector3f(4, 0, 0));
		fire.setStartSize(0.5f);
		fire.setEndSize(0.1f);
		fire.setGravity(0, 0, 0);
		fire.setLowLife(0.3f);
		fire.setHighLife(0.3f);
		fire.getParticleInfluencer().setVelocityVariation(0.2f);
		fire.setLocalTranslation(10, 10, 0);
		
		return fire;
	}
}
