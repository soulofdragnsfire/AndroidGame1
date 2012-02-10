package com.chierin.game;

import static org.andengine.extension.physics.box2d.util.constants.PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT;

import java.util.LinkedList;
import java.util.ListIterator;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.FixedStepPhysicsWorld;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.extension.physics.box2d.util.constants.PhysicsConstants;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TiledTextureRegion;

import android.hardware.SensorManager;
import android.widget.Toast;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;

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
	private static final float MAX_DISTANCE_EFFECTED = 4f, MOVE_FORCE = 1.75f;

	// ===========================================================
	// Fields
	// ===========================================================

	private BitmapTextureAtlas mBitmapTextureAtlas;
	private TiledTextureRegion mCircleFaceTextureRegion;

	private Scene mScene;

	private PhysicsWorld mPhysicsWorld;
	
	private LinkedList<Creature> creatureList = new LinkedList<Creature>();
	private Vector2 mGravity = new Vector2(0,0);
	private Vector2 touchLoc = new Vector2(0,0);
	private ContactListener mContactListener;
	private boolean isTouched = false;
	
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
		this.mCircleFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, "face_circle_tiled.png", 0, 32, 2, 1); // 64x32
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
					creature.getBody().setLinearVelocity(0, 0);
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
		this.mPhysicsWorld = new FixedStepPhysicsWorld(35,new Vector2(0, SensorManager.GRAVITY_EARTH), false, 3, 2){
			@Override
			public void onUpdate(float pSecondsElapsed){
				super.onUpdate(pSecondsElapsed);
				AndroidGame1Activity.this.onMoveUpdate(pSecondsElapsed);
			}
		};
		
		this.initFrame();
		this.initCreatures();
		this.initLevel();
		
		this.mPhysicsWorld.setGravity(mGravity);
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
				//this.moveFromPoint(pSceneTouchEvent.getX()/PIXEL_TO_METER_RATIO_DEFAULT, pSceneTouchEvent.getY()/PIXEL_TO_METER_RATIO_DEFAULT);
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

		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, roof, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, left, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, right, BodyType.StaticBody, wallFixtureDef);

		this.mScene.attachChild(ground);
		this.mScene.attachChild(roof);
		this.mScene.attachChild(left);
		this.mScene.attachChild(right);		
	}

	private void initCreatures(){
		int startX = (CAMERA_WIDTH / 2) - (NUM_CREATURES*(this.mCircleFaceTextureRegion.getWidth()+10)/2);
		int startY = (CAMERA_HEIGHT / 2) - (NUM_CREATURES*(this.mCircleFaceTextureRegion.getHeight()+10)/2);
		for(int i = 0; i < NUM_CREATURES; i++){
			Creature mCreature = new Creature(startX + i*(this.mCircleFaceTextureRegion.getWidth()+10), startY + i*(this.mCircleFaceTextureRegion.getHeight()+10), this.mCircleFaceTextureRegion, this.mPhysicsWorld);
			this.creatureList.add(mCreature);
			this.mScene.attachChild(mCreature);
		}
	}

	private void initLevel(){
		
	}
	
	private void moveFromPoint(float pTouchMetersX, float pTouchMetersY){
		final ListIterator<Creature> list = this.creatureList.listIterator();
		final Vector2 touchLoc = Vector2Pool.obtain(pTouchMetersX, pTouchMetersY);
		
		Creature cur;
		
		while(list.hasNext()){
			cur = list.next();
			final Vector2 curLoc = Vector2Pool.obtain(cur.getBody().getPosition());
			float distanceFromTouch = curLoc.dst(touchLoc);
			if(distanceFromTouch < MAX_DISTANCE_EFFECTED){
				final Vector2 velocity =  Vector2Pool.obtain(curLoc.sub(touchLoc).mul(MOVE_FORCE*(MAX_DISTANCE_EFFECTED - distanceFromTouch)));
				cur.setMoveVector(velocity);
				Vector2Pool.recycle(velocity); 
			}
			Vector2Pool.recycle(curLoc);
		}
		Vector2Pool.recycle(touchLoc);
	}
	
	private void onMoveUpdate(float pSecondsElapsed){
		final ListIterator<Creature> list = this.creatureList.listIterator();
		
		Creature cur;
		
		while(list.hasNext()){
			cur = list.next();
			if(this.isTouched){
				final Vector2 curLoc = Vector2Pool.obtain(cur.getBody().getPosition());
				float distanceFromTouch = curLoc.dst(this.touchLoc);
				if(distanceFromTouch < MAX_DISTANCE_EFFECTED){
					final Vector2 velocity =  Vector2Pool.obtain(curLoc.sub(touchLoc).nor().mul(MOVE_FORCE + MOVE_FORCE*(MAX_DISTANCE_EFFECTED - distanceFromTouch)/MAX_DISTANCE_EFFECTED));
					//final Vector2 velocity =  Vector2Pool.obtain(curLoc.sub(this.touchLoc).nor().mul(MOVE_FORCE));
					cur.setMoveVector(velocity);
					Vector2Pool.recycle(velocity); 
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