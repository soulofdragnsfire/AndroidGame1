package com.chierin.game;

import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.opengl.texture.region.ITiledTextureRegion;

import android.util.Log;

import com.badlogic.gdx.math.Vector2;
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
	private static final float CHANCE_OF_CHANGE = 0.8f, CHANCE_OF_STOP = 0.0f;
	private static final int RANGE_OF_TURN = 15;
	
	// ===========================================================
	// Fields
	// ===========================================================
	private Body mBody;
	private Vector2 moveVector = Vector2Pool.obtain(0.1f,0.1f);
	
	// ===========================================================
	// Constructors
	// ===========================================================
	
	public Creature(float pX, float pY, ITiledTextureRegion pTiledTextureRegion, PhysicsWorld pPhysicsWorld) {
		super(pX, pY, pTiledTextureRegion);
		this.setBody(pPhysicsWorld);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public Body getBody(){
		return this.mBody;
	}
	
	public boolean setBody(PhysicsWorld pPhysicsWorld){
		if(pPhysicsWorld != null){
			this.mBody = PhysicsFactory.createCircleBody(pPhysicsWorld, this, BodyType.DynamicBody, FIXTURE_DEF);
			this.mBody.setFixedRotation(true);
			this.mBody.setUserData("Creature");
			pPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(this, this.mBody, true, true));
			return true;
		}
		return false;
	}
	
	public boolean setMoveVector(Vector2 pVector){
		if(pVector != null){
			Vector2Pool.recycle(this.moveVector);
			this.moveVector = Vector2Pool.obtain(pVector);
			return true;
		}
		return false;
	}
	
	public float getPositionX(){
		if(this.mBody != null)
			return this.mBody.getPosition().x;
		return -1;
	}
	
	public float getPositionY(){
		if(this.mBody != null)
			return this.mBody.getPosition().y;
		return -1;
	}
	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================	

	@Override
	public String toString(){
		return "Creature";
	}
	
	// ===========================================================
	// Methods
	// ===========================================================
	
	public void makeNewMove(){
		
		if(this.mBody.getLinearVelocity().x != 0.0 || this.mBody.getLinearVelocity().y != 0.0){
			this.setRotation(this.angleOfVector(this.mBody.getLinearVelocity()) - 90);
		}
		else if(this.mBody.getLinearVelocity().x == 0.0 && this.mBody.getLinearVelocity().y == 0.0){
			float angle = -90;
			if(Math.random() >= 0.5 ){
				angle = 90;
			}
			
			Vector2 vector = Vector2Pool.obtain((float) (this.moveVector.x*Math.cos(angle) - this.moveVector.y*Math.sin(angle)), 
					(float) (this.moveVector.y*Math.cos(angle) + this.moveVector.x*Math.sin(angle)));
			this.setMoveVector(vector);
			Vector2Pool.recycle(vector);
		}
		else{
			this.setRotation(0);
		}
				
		if(Math.random() < CHANCE_OF_CHANGE){
			float change = (float) Math.random();
			Vector2 vector;
			if(change < CHANCE_OF_STOP){
				vector = Vector2Pool.obtain(0, 0);
			}
			else{
				float angle = (float) (((Math.random() * RANGE_OF_TURN) - RANGE_OF_TURN/2) * Math.PI/180);
				
				vector = Vector2Pool.obtain((float) (this.moveVector.x*Math.cos(angle) - this.moveVector.y*Math.sin(angle)), 
						(float) (this.moveVector.y*Math.cos(angle) + this.moveVector.x*Math.sin(angle)));
			}
			
			this.setMoveVector(vector);
			Vector2Pool.recycle(vector);
		}
		
		this.mBody.setLinearVelocity(this.moveVector);
	}
	
	private float angleOfVector(Vector2 pVector){
		if(pVector.x == 0){
			if(pVector.y > 0){
				return 90.0f;
			}
			else{
				return 270.0f;
			}
		}
		else if(pVector.x < 0){
			if(pVector.y == 0){
				return 180.0f;
			}
			return (float) (Math.atan(pVector.y/pVector.x) * 180 / Math.PI) + 180;
		}
		else{
			if(pVector.y == 0){
				return 0.0f;
			}
			else if(pVector.y < 0){
				return (float) (Math.atan(pVector.y/pVector.x) * 180 / Math.PI) + 360;
			}
			else{
				return (float) (Math.atan(pVector.y/pVector.x) * 180 / Math.PI);
			}
		}
	}
	
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
