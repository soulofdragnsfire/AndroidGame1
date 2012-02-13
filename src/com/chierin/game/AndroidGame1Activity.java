package com.chierin.game;

import static org.andengine.extension.physics.box2d.util.constants.PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT;

import java.util.LinkedList;
import java.util.ListIterator;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.particle.SpriteParticleSystem;
import org.andengine.entity.particle.emitter.CircleOutlineParticleEmitter;
import org.andengine.entity.particle.initializer.AlphaInitializer;
import org.andengine.entity.particle.initializer.BlendFunctionInitializer;
import org.andengine.entity.particle.initializer.ColorInitializer;
import org.andengine.entity.particle.initializer.RotationInitializer;
import org.andengine.entity.particle.initializer.VelocityInitializer;
import org.andengine.entity.particle.modifier.AlphaModifier;
import org.andengine.entity.particle.modifier.ColorModifier;
import org.andengine.entity.particle.modifier.ExpireModifier;
import org.andengine.entity.particle.modifier.ScaleModifier;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.FixedStepPhysicsWorld;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;

import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.util.Log;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.RayCastCallback;

/**
 * @author Jerry Ashcroft
 * @since 12:00:08 - 02-07-2012
 */

public class AndroidGame1Activity extends BaseClass implements IOnSceneTouchListener {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int CAMERA_WIDTH = 720;
	private static final int CAMERA_HEIGHT = 480;
	
	private static final int NUM_CREATURES = 5;
	private static final float MAX_DISTANCE_EFFECTED = 4f, MOVE_FORCE = 1.75f, SLOW_ON_COLLISION = 0.85f;

	// ===========================================================
	// Fields
	// ===========================================================

	private BitmapTextureAtlas mBitmapTextureAtlas;
	private TiledTextureRegion mCreatureTextureRegion;

	private Scene mScene;

	private PhysicsWorld mPhysicsWorld;
	private final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
	private RayCastCallback raycastCallback;
	
	private LinkedList<Creature> creatureList = new LinkedList<Creature>();
	private Vector2 touchLoc = new Vector2(0,0);
	private ContactListener mContactListener;
	private boolean isTouched = false, hitWall = false;
	
	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public EngineOptions onCreateEngineOptions() {
		//Toast.makeText(this, "Touch the screen to add objects.", Toast.LENGTH_LONG).show();

		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
	}

	@Override
	public void onCreateResources() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

		this.mBitmapTextureAtlas = new BitmapTextureAtlas(64, 128, TextureOptions.BILINEAR);
		this.mCreatureTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, "face_circle_tiled.png", 0, 32, 2, 1); 
		this.mBitmapTextureAtlas.load(this.getTextureManager());
	}

	@Override
	public Scene onCreateScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());

		this.mScene = new Scene();
		this.mScene.setBackground(new Background(0.09804f, 0.6274f, 0.8784f));
		this.mScene.setOnSceneTouchListener(this);
		
		this.mContactListener = new ContactListener(){
			@Override
			public void beginContact(Contact contact) {
				Fixture creature;
				Fixture fixA = contact.getFixtureA();
				Fixture fixB = contact.getFixtureB();
				if(fixA.getBody().getUserData() != "Creature" || fixB.getBody().getUserData() != "Creature"){
					if(fixA.getBody().getUserData() == "Creature"){
						creature = fixA;
					}
					else if(fixB.getBody().getUserData() == "Creature"){
						creature = fixB;
					}
					else{
						return;
					}
					//Log.v("Game", "Contact Point: " + contact.getWorldManifold().getPoints()[0].toString() + ", Loc of Creature: " + creature.getBody().getPosition().toString());
					Vector2 normalVector = Vector2Pool.obtain(contact.getWorldManifold().getPoints()[0].sub(creature.getBody().getPosition()).nor());
					float angle = (float) (90 * Math.PI/180);
					
					
					Vector2 slidingVector1 = Vector2Pool.obtain((float) (normalVector.x*Math.cos(angle) - normalVector.y*Math.sin(angle)), 
							(float) (normalVector.y*Math.cos(angle) + normalVector.x*Math.sin(angle)));
					Vector2 slidingVector2 = Vector2Pool.obtain((float) (normalVector.x*Math.cos(-angle) - normalVector.y*Math.sin(-angle)), 
							(float) (normalVector.y*Math.cos(-angle) + normalVector.x*Math.sin(-angle)));
					
					if(creature.getBody().getLinearVelocity().sub(slidingVector1).len() < creature.getBody().getLinearVelocity().sub(slidingVector2).len()){
						creature.getBody().setLinearVelocity(slidingVector1.mul(creature.getBody().getLinearVelocity().len() * SLOW_ON_COLLISION));
					}
					else{
						creature.getBody().setLinearVelocity(slidingVector2.mul(creature.getBody().getLinearVelocity().len() * SLOW_ON_COLLISION));
					}
					Vector2Pool.recycle(slidingVector2);
					Vector2Pool.recycle(slidingVector1);
					Vector2Pool.recycle(normalVector);
				}
			}
			@Override
			public void endContact(Contact contact) {

			}
			@Override
			public void preSolve(Contact contact, Manifold oldManifold) {
				Fixture fixA = contact.getFixtureA();
				Fixture fixB = contact.getFixtureB();
				if(fixA.getBody().getUserData() == "Creature" && fixB.getBody().getUserData() == "Creature"){
					contact.setEnabled(false);
				}
			}
			@Override
			public void postSolve(Contact contact, ContactImpulse impulse) {
				
			}
		};
		
		this.mPhysicsWorld = new FixedStepPhysicsWorld(35,new Vector2(0, 0), false, 3, 2){
			@Override
			public void onUpdate(float pSecondsElapsed){
				super.onUpdate(pSecondsElapsed);
				AndroidGame1Activity.this.onMoveUpdate(pSecondsElapsed);
			}
		};
		
		this.raycastCallback = new RayCastCallback() {
			@Override
			public float reportRayFixture(Fixture pFixture, Vector2 pPoint, Vector2 pNormal, float pFraction){
				if(pFixture.getBody().getUserData() != null){
					if(pFixture.getBody().getUserData() == "Wall"){
						AndroidGame1Activity.this.hitWall = true;
						return 0;
					}
				}
				AndroidGame1Activity.this.hitWall = false;
				return -1;
			}
		};
		
		this.initFrame();
		this.initCreatures();
		this.initLevel();
		
		this.mPhysicsWorld.setAutoClearForces(true);
		this.mPhysicsWorld.setContactListener(mContactListener);
		this.mScene.registerUpdateHandler(this.mPhysicsWorld);
		this.mScene.setTouchAreaBindingOnActionDownEnabled(true);
		return this.mScene;
	}

	@Override
	public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
		if(this.mPhysicsWorld != null) {
			if(pSceneTouchEvent.isActionDown()) {
				Vector2Pool.recycle(touchLoc);
				this.touchLoc = Vector2Pool.obtain(pSceneTouchEvent.getX()/PIXEL_TO_METER_RATIO_DEFAULT, pSceneTouchEvent.getY()/PIXEL_TO_METER_RATIO_DEFAULT);
				this.isTouched = true;
				return true;
			}
			else if(pSceneTouchEvent.isActionMove()){
				Vector2Pool.recycle(touchLoc);
				this.touchLoc = Vector2Pool.obtain(pSceneTouchEvent.getX()/PIXEL_TO_METER_RATIO_DEFAULT, pSceneTouchEvent.getY()/PIXEL_TO_METER_RATIO_DEFAULT);
				return true;
			}
			else if(pSceneTouchEvent.isActionUp()){
				this.isTouched = false;
				return true;
			}
		}
		return false;
	}

	// ===========================================================
	// Methods
	// ===========================================================	
	
	private void initFrame(){
		final Rectangle ground = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2);
		final Rectangle roof = new Rectangle(0, 0, CAMERA_WIDTH, 2);
		final Rectangle left = new Rectangle(0, 0, 2, CAMERA_HEIGHT);
		final Rectangle right = new Rectangle(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT);

		PhysicsFactory.createBoxBody(this.mPhysicsWorld, ground, BodyType.StaticBody, this.wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, roof, BodyType.StaticBody, this.wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, left, BodyType.StaticBody, this.wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, right, BodyType.StaticBody, this.wallFixtureDef);

		this.mScene.attachChild(ground);
		this.mScene.attachChild(roof);
		this.mScene.attachChild(left);
		this.mScene.attachChild(right);		
	}

	private void initCreatures(){
		final int startX = (CAMERA_WIDTH / 2) - (NUM_CREATURES*(this.mCreatureTextureRegion.getWidth()+10)/2);
		final int startY = (CAMERA_HEIGHT / 2) - (NUM_CREATURES*(this.mCreatureTextureRegion.getHeight()+10)/2);
		for(int i = 0; i < NUM_CREATURES; i++){
			Creature mCreature = new Creature(startX + i*(this.mCreatureTextureRegion.getWidth()+10), startY + i*(this.mCreatureTextureRegion.getHeight()+10), this.mCreatureTextureRegion, this.mPhysicsWorld);
			this.creatureList.add(mCreature);
			this.mScene.attachChild(mCreature);
		}
	}

	private void initLevel(){
		final Rectangle wall1 = new Rectangle(CAMERA_WIDTH/2 - 30, CAMERA_HEIGHT/2 + 100, 2, 70);
		final Rectangle wall2 = new Rectangle(CAMERA_WIDTH/2 - 30, CAMERA_HEIGHT/2 + 170, 60, 2);
		final Rectangle wall3 = new Rectangle(CAMERA_WIDTH/2 + 30, CAMERA_HEIGHT/2 + 100, 2, 70);
		
		Body mBody1 = PhysicsFactory.createBoxBody(this.mPhysicsWorld, wall1, BodyType.StaticBody, this.wallFixtureDef);
		Body mBody2 = PhysicsFactory.createBoxBody(this.mPhysicsWorld, wall2, BodyType.StaticBody, this.wallFixtureDef);
		Body mBody3 = PhysicsFactory.createBoxBody(this.mPhysicsWorld, wall3, BodyType.StaticBody, this.wallFixtureDef);
		
		mBody1.setUserData("Wall");
		mBody2.setUserData("Wall");
		mBody3.setUserData("Wall");
		
		this.mScene.attachChild(wall1);
		this.mScene.attachChild(wall2);
		this.mScene.attachChild(wall3);
	}
	
	private void onMoveUpdate(float pSecondsElapsed){
		Vector2 curLoc, velocity;
		float distanceFromTouch;
			
		for(Creature cur : this.creatureList){
			if(this.isTouched){
				curLoc = Vector2Pool.obtain(cur.getBody().getPosition());
				distanceFromTouch = curLoc.dst(this.touchLoc);
				if(distanceFromTouch < MAX_DISTANCE_EFFECTED){
					this.hitWall = false;
					mPhysicsWorld.rayCast(raycastCallback, curLoc, this.touchLoc);
					if(!this.hitWall){
						velocity =  Vector2Pool.obtain(curLoc.sub(this.touchLoc).nor().mul(MOVE_FORCE + MOVE_FORCE*(MAX_DISTANCE_EFFECTED - distanceFromTouch)/MAX_DISTANCE_EFFECTED));
						cur.setMoveVector(velocity);
						Vector2Pool.recycle(velocity);
					}
				}
				Vector2Pool.recycle(curLoc);
			}
			cur.makeNewMove();
		}		
	}
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}