package car;

import com.jme3.app.state.AbstractAppState;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;

import car.ray.RayCarControl;
import game.App;
import helper.H;
import helper.Log;

public class CarCamera extends AbstractAppState implements RawInputListener {

	private Camera c;
	private RayCarControl p;
	private Vector3f prevVel; //for smoothing the velocity vector
	private Vector3f prevPos;
	
	private float lastTimeout;
	private float rotRad;
	private static final float ROT_SPEED = 0.01f;
	
	private Vector3f lastShake = new Vector3f();
	
	public CarCamera(String name, Camera c, RayCarControl p) {
		super();
		
		this.c = c;
		
		if (p != null) {
			this.p = p;
			Vector3f pPos = p.getPhysicsLocation();
			c.setLocation(pPos.add(p.getCarData().cam_offset)); //starting position of the camera
			c.lookAt(pPos.add(p.getCarData().cam_lookAt), new Vector3f(0,1,0)); //look at car
		}
	}

	@Override
	public void update(float tpf) {
		if (p == null) {
			return;
		}
		this.tpf = tpf;
	}
	private float tpf;
	
	public void setCar(RayCarControl p) {
		this.p = p;
	}
	
	@Override
	public void render(RenderManager rm) {
		//TODO use the direction of the wheels
		//TODO also react to g forces
		//TODO smooth the mouse stuff
		if (p == null)
			return;
		
		if (prevPos == null) //doesn't exist on the first loop
			prevPos = c.getLocation();
		if (prevVel == null) prevVel = p.vel.normalize();
		
		Vector3f carPos = p.getRootNode().getLocalTranslation();
		
		Quaternion pRot = p.getRootNode().getLocalRotation();
		
		if (!FastMath.approximateEquals(rotRad, 0)) {
			lastTimeout += tpf;
			if (lastTimeout > 2) { //TODO static number/setting
				rotRad *= 0.6f; //reset to back of car slowly TODO static number/setting
			}
			
			Quaternion q = new Quaternion();
			q.fromAngleAxis(rotRad*ROT_SPEED, p.up);
			pRot.multLocal(q);
		}
		
		Vector3f vec1 = pRot.mult(new Vector3f(0, 0, 1)).normalize();
		prevVel.interpolateLocal(p.vel.normalize(), 1f*tpf);
		
		Vector3f vec = new Vector3f();
		vec.interpolateLocal(vec1, prevVel, 0.4f).normalize();
		
		vec.y = 1;
		
		Vector3f next = new Vector3f();
		Vector2f vec_2 = H.v3tov2fXZ(vec).normalize();
		
		next.x = vec_2.x*p.getCarData().cam_offset.z;
		next.y = vec.y*p.getCarData().cam_offset.y;
		next.z = vec_2.y*p.getCarData().cam_offset.z;
		
		next = carPos.add(next);
		next.interpolateLocal(next, prevPos, 0.1f*tpf);//maybe
		prevPos = next; 
		
		c.setLocation(prevPos); //already set for next frame

		//lastly do a ray cast to make sure that you can still see the car
		//TODO actually avoid the car model, and maybe not so touchy...
		CollisionResults results = new CollisionResults();
		Vector3f dir = c.getLocation().subtract(carPos.add(p.getCarData().cam_lookAt));
		Ray ray = new Ray(carPos.add(p.getCarData().cam_lookAt), dir);
		App.rally.getRootNode().collideWith(ray, results);
		CollisionResult cr = results.getClosestCollision();
		if (cr != null && cr.getDistance() < dir.length()) {
			Geometry g = cr.getGeometry();
			if (!H.hasParentNode(g, p.getRootNode())) { //don't collide with the car TODO doesn't work
				c.setLocation(cr.getContactPoint());
				Log.p("Camera contact on: ", g.getName());
			}
		}
		
		//at high speeds shake the camera a little
		float shakeFactor = p.vel.length() * p.vel.length() * p.getCarData().cam_shake;
		Vector3f lookAt = carPos.add(p.getCarData().cam_lookAt);
		lastShake.addLocal(new Vector3f(FastMath.nextRandomFloat(), FastMath.nextRandomFloat(), FastMath.nextRandomFloat()).normalize().mult(shakeFactor*FastMath.nextRandomInt(-1, 1)));
		if (lastShake.length() > 0.01f)
			lastShake.interpolateLocal(Vector3f.ZERO, 0.7f); //TODO shake slower
		else
			lastShake = new Vector3f();
		
		lookAt.addLocal(lastShake);
		
		c.lookAt(lookAt, new Vector3f(0,1,0));
	}
	
	
	public void beginInput() {}
	public void endInput() {}
	
	public void onMouseButtonEvent(MouseButtonEvent arg0) {}
	public void onMouseMotionEvent(MouseMotionEvent arg0) {
		lastTimeout = 0;
		rotRad += arg0.getDX();
	}
	public void onKeyEvent(KeyInputEvent arg0) {}
	public void onTouchEvent(TouchEvent arg0) {}
	public void onJoyAxisEvent(JoyAxisEvent arg0) {}
	public void onJoyButtonEvent(JoyButtonEvent arg0) {}

}
