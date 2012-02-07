package com.chierin.game;

import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.opengl.texture.region.ITiledTextureRegion;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

/**
 * @author Jerry Ashcroft
 * @since 12:00:08 - 02-07-2012
 */

public class Creature extends AnimatedSprite {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final FixtureDef FIXTURE_DEF = PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f);
	
	// ===========================================================
	// Fields
	// ===========================================================

	private Body mBody;
	
	// ===========================================================
	// Constructors
	// ===========================================================
	
	public Creature(float pX, float pY, ITiledTextureRegion pTiledTextureRegion, PhysicsWorld pPhysicsWorld) {
		super(pX, pY, pTiledTextureRegion);
		this.SetBody(pPhysicsWorld);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public boolean SetBody(PhysicsWorld pPhysicsWorld){
		if(pPhysicsWorld != null){
			mBody = PhysicsFactory.createCircleBody(pPhysicsWorld, this, BodyType.DynamicBody, FIXTURE_DEF);
			pPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(this, mBody, true, true));
			return true;
		}
		return false;
	}
	
	public Body GetBody(){
		return mBody;
	}
	
	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================	

	// ===========================================================
	// Methods
	// ===========================================================
	
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
