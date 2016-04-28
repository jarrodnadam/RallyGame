package game;

import java.util.LinkedList;

import com.bulletphysics.dynamics.vehicle.DefaultVehicleRaycaster;
import com.bulletphysics.dynamics.vehicle.WheelInfo;
import com.bulletphysics.dynamics.vehicle.WheelInfo.RaycastInfo;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.bullet.util.Converter;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

//extends:
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-jbullet/src/main/java/com/jme3/bullet/objects/PhysicsVehicle.java

public class MyPhysicsVehicle extends PhysicsVehicle {

	//skid stuff
	Node skidNode;
	LinkedList<Spatial> skidList = new LinkedList<Spatial>();
	private float maxlat;
	private float maxlong;

	//sound stuff
	private AudioNode engineSound;

	//nice to use directions
	Vector3f up = new Vector3f();
	Vector3f right = new Vector3f();
	Vector3f left = new Vector3f();

	// Car controlling things
	private MyKeyListener control;
	private AI ai;

	//car data
	protected CarData car;
	protected Node carRootNode;

	//my wheel node
	MyWheelNode[] wheel = new MyWheelNode[4];

	//state stuff
	int curGear = 1;
	int curRPM = 0;

	float accelCurrent = 0;
	float steeringCurrent = 0;
	float brakeCurrent = 0;

	float steerLeft = 0;
	float steerRight= 0;

	float engineTorque = 0;
	float totalTraction = 0;
	float wheelRot = 0;

	boolean ifHandbrake = false;
	//- driving stuff

	float distance = 0;
	boolean ifLookBack = false;
	boolean ifLookSide = false;

	float redlineKillFor = 0;

	MyPhysicsVehicle(CollisionShape col, CarData cartype, Node carNode) {
		super(col, cartype.mass);
		this.car = cartype;
		this.carRootNode = carNode;
		AssetManager am = App.rally.getAssetManager();

		this.skidNode = new Node(); //attached in car builder

		this.setSuspensionCompression(car.susCompression());
		this.setSuspensionDamping(car.susDamping());
		this.setSuspensionStiffness(car.stiffness);
		this.setMaxSuspensionForce(car.maxSusForce);
		this.setMaxSuspensionTravelCm(car.maxSusTravel);

		//for each wheel
		for (int i = 0; i < 4; i++) {
			wheel[i] = new MyWheelNode("wheel "+i+" node", this, i);
			wheel[i].spat = am.loadModel(car.wheelModel);
			if (i % 2 == 1) //odd side needs fliping
				wheel[i].spat.rotate(0, FastMath.PI, 0);
			wheel[i].spat.center();

			wheel[i].attachChild(wheel[i].spat);

			float xoff = (i % 2 == 1) ? car.wheel_xOff : -car.wheel_xOff; //left side is positive
			float yoff = car.wheel_yOff; //all the same height
			float zoff = (i < 2) ? car.wheel_zOff : -car.wheel_zOff; //front is positive
			boolean front = (i < 2) ? true : false; //only the front turns

			addWheel(wheel[i], new Vector3f(xoff, yoff, zoff),
					car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, front);

			//Their friction (usually zero)
			setFrictionSlip(i, car.wheelBasicSlip);

			carNode.attachChild(wheel[i]);
		}

		//generate the slip* max force from the car wheel data
		this.maxlat = VehiclePhysicsHelper.calcSlipMax(car.wheellatdata);
		if (maxlat == Float.NaN) System.err.println("maxlat was NaN");
		this.maxlong = VehiclePhysicsHelper.calcSlipMax(car.wheellongdata);
		if (maxlong == Float.NaN) System.err.println("maxlong was NaN");
	}

	public void onAction(String binding, boolean value, float tpf) {
		//value == 'if pressed (down) - we get one back when its unpressed (up)'
		//tpf is being used as the value for joysticks. deal with it
		switch(binding) {
		case "Left": 
			steerLeft = tpf;
			break;

		case "Right": 
			steerRight = tpf;
			break;

		case "Accel":
			accelCurrent = tpf;
			break;

		case "Brake":
			brakeCurrent = tpf;
			break;

		case "Jump":
			if (value) {
				applyImpulse(car.JUMP_FORCE, new Vector3f(0,0,0)); //push up
				Vector3f old = getPhysicsLocation();
				old.y += 2; //and move up
				setPhysicsLocation(old);
			}
			break;

		case "Handbrake":
			ifHandbrake = value;
			break;

		case "Flip":
			if (value) this.flipMe();
			break;

		case "Reset":
			if (value) this.reset();
			break;

		case "Reverse":
			if (value) curGear = 0;
			else curGear = 1;
			break;

		case "Lookback":
			ifLookBack = value;
			break;

		case "Lookside":
			ifLookSide = value;
			break;

		default:
			//nothing
			System.err.println("unknown key: "+binding);
			break;
		}
	}
	//end controls


	/**Used internally, creates the actual vehicle constraint when vehicle is added to phyicsspace
	 */
	@Override
	public void createVehicle(PhysicsSpace space) {
		physicsSpace = space;
		if (space == null) {
			return;
		}
		rayCaster = new DefaultVehicleRaycaster(space.getDynamicsWorld());
		vehicle = new FancyRcV(car, rBody, rayCaster); //<-- i added it here for the change of constructor
		vehicle.setCoordinateSystem(0, 1, 2);
		for (VehicleWheel wheel : wheels) {
			wheel.setWheelInfo(vehicle.addWheel(Converter.convert(wheel.getLocation()), Converter.convert(wheel.getDirection()), Converter.convert(wheel.getAxle()),
					wheel.getRestLength(), wheel.getRadius(), tuning, wheel.isFrontWheel()));
		}
	}

	public void giveSound(AudioNode audio) {
		if (engineSound != null) carRootNode.detachChild(engineSound); //incase we have 2 sounds running
		
		engineSound = audio;
		engineSound.setLooping(true);
		engineSound.play();
		App.rally.getRootNode().attachChild(engineSound);
	}

	public void makeAI() {
		this.ai = new AI(this);
	}

	public void makeControl() {
		this.control = new MyKeyListener(this);
		App.rally.getInputManager().addRawInputListener(this.control);
	}


	//TODO Things taken out of physics:
	//- handbrake

	//TODO find SAE950311 
	//Millikin & Millikin's Race Car Vehicle Dynamics

	//TODO good info to actually read:
	//https://www.sae.org/images/books/toc_pdfs/R146.pdf
	////////////////////////////////////////////////////

	private void specialPhysics(float tpf) {
		//NOTE: that z is forward, x is side
		// - the reference notes say that x is forward and y is sideways so just be careful

		Matrix3f w_angle = getPhysicsRotationMatrix();
		Vector3f w_velocity = getLinearVelocity();

		//* Linear Accelerations: = player.car.length * player.car.yawrate (in rad/sec)
		double yawspeed = car.length * getAngularVelocity().y;

		Vector3f velocity = w_angle.invert().mult(w_velocity);

		float steeringCur = steeringCurrent;
		if (velocity.z < 0) { //to flip the steering on moving in reverse
			steeringCur *= -1;
		}

		//////////////////////////////////////////////////
		//'Wheel'less forces		
		//linear resistance and quadratic drag
		float dragx = (float)(-(car.RESISTANCE * velocity.x + car.DRAG * velocity.x * Math.abs(velocity.x)));
		float dragy = (float)(-(car.RESISTANCE * velocity.y + car.DRAG * velocity.y * Math.abs(velocity.y)));
		float dragz = (float)(-(car.RESISTANCE * velocity.z + car.DRAG * velocity.z * Math.abs(velocity.z)));

		Vector3f totalNeutral = new Vector3f(dragx, dragy, dragz);
		applyCentralForce(w_angle.mult(totalNeutral));

		//////////////////////////////////
		//Wheel Forces
		Vector3f[] wf = new Vector3f[] {
				new Vector3f(),new Vector3f(),new Vector3f(),new Vector3f(),
		};

		if (velocity.z == 0) { //to avoid the divide by zero below
			velocity.z += 0.0001;
		}

		//////////////////////////////////////
		//longitudinal forces
		engineTorque = getEngineWheelTorque(tpf, velocity.z);
		float[] torques = new float[] { 0, 0, 0, 0 };

		engineTorque /= 2; //probably on at least one axle
		if (car.driveFront && car.driveRear)
			engineTorque /= 2; //split up into 2 more wheels

		if (car.driveFront) {
			torques[0] = engineTorque;
			torques[1] = engineTorque;
		}
		if (car.driveRear) {
			torques[2] = engineTorque;
			torques[3] = engineTorque;
		}

		//http://web.archive.org/web/20050308061534/home.planet.nl/~monstrous/tutstab.html
		//http://phors.locost7.info/contents.htm

		//LARGE LOOP for each wheel
		for (int i = 0; i < 4; i++) {
			float susforce = (float)getWheel(i).getWheelInfo().wheelsSuspensionForce;

			if (ifHandbrake && i > 1) //rearwheels
				wheel[i].radSec = 0; //put the handbrake on
				
			float slipratio = (wheel[i].radSec*car.wheelRadius - velocity.z)/Math.abs(velocity.z);
			//calc the longitudinal force from the slip ratio
			wf[i].z = VehiclePhysicsHelper.tractionFormula(car.wheellongdata, slipratio) * susforce;

			float totalLongForce = torques[i] - wf[i].z - (brakeCurrent*susforce*Math.signum(velocity.z)); //TODO braking properly
			wheel[i].radSec += tpf*totalLongForce/(car.engineWheelInertia());

			float slipangle = 0;
			if (i < 2) //front
				slipangle = (float)(Math.atan((velocity.x + yawspeed) / Math.abs(velocity.z)) - steeringCur);
			else //rear
				slipangle = (float)(Math.atan((velocity.x - yawspeed) / Math.abs(velocity.z)));

			//calculate the max lat traction value here
			float tractionLeft = FastMath.clamp(1 - Math.abs(slipratio/this.maxlong), 0, 1);
			float maxslipangle = tractionLeft*this.maxlat;
			//clamp the slipangle to this value
			slipangle = FastMath.clamp(slipangle, -maxslipangle, maxslipangle);

			//latitudinal force that is calculated off the slip angle
			wf[i].x = -VehiclePhysicsHelper.tractionFormula(car.wheellatdata, slipangle) * susforce;

			wheel[i].skid = FastMath.clamp(FastMath.sqrt(slipangle*slipangle + slipratio*slipratio), 0, 1);

			//TODO handbrake
		}

		//TODO: !HARD! better code from others 'transision' between static and kinetic friction models
		// i have no static model (yet)

		//TODO stop the slow speed wobble (hint the basic vehicle code does this through impulses (integration of forces)

		float lim = 5;
		float velz = velocity.z + 0.01f;
		if (Math.abs(velz) <= lim) {//TODO doesn't work
			//			fl.multLocal(velz*velz/(lim*lim));
			//			fr.multLocal(velz*velz/(lim*lim));
			//			rl.multLocal(velz*velz/(lim*lim));
			//			rr.multLocal(velz*velz/(lim*lim));
		}

		//ui based values
		totalTraction = wf[0].length() + wf[1].length() + wf[2].length() + wf[3].length();  
		this.wheelRot = 0;
		for (MyWheelNode w: wheel) {
			this.wheelRot += w.radSec;
		}
		this.wheelRot /= 4;

		//and finally apply forces
		if (wheel[0].contact)
			applyForce(w_angle.mult(wf[0]), w_angle.mult(wheel[0].getContactPoint(car.wheelRadius, car.rollFraction)));
		if (wheel[1].contact) 
			applyForce(w_angle.mult(wf[1]), w_angle.mult(wheel[1].getContactPoint(car.wheelRadius, car.rollFraction)));
		if (wheel[2].contact) 
			applyForce(w_angle.mult(wf[2]), w_angle.mult(wheel[2].getContactPoint(car.wheelRadius, car.rollFraction)));
		if (wheel[3].contact) 
			applyForce(w_angle.mult(wf[3]), w_angle.mult(wheel[3].getContactPoint(car.wheelRadius, car.rollFraction)));

	}

	private float getEngineWheelTorque(float tpf, float vz) {
		float wheelrot = 0;
		if (car.driveFront)
			wheelrot = wheel[0].radSec; //get the drive wheels rotation speed
		if (car.driveRear) 
			wheelrot = wheel[2].radSec; //because both the wheels are rpm locked it doesn't matter which one

		float curGearRatio = car.gearRatios[curGear];//0 = reverse, >= 1 normal make sense
		float diffRatio = car.diffRatio;

		curRPM = (int)(wheelrot*curGearRatio*diffRatio*60*car.wheelRadius); //rad/(m*sec) to rad/min and the drive ratios to engine
		//wheel rad/s, gearratio, diffratio, conversion from rad/sec to rad/min

		curRPM = H.clamp(curRPM, 800, 50000);//make sure we never stall (or get rediculous)
		//TODO use engine compression value

		if (Math.abs(curRPM) > car.redline) {
			return 0; //kill engine if greater than redline
		}

		autoTransmission(curRPM, vz);
		float engineTorque = lerpTorque(curRPM)*accelCurrent;

		float engineOutTorque = engineTorque*curGearRatio*diffRatio*car.transEffic;

		float totalTorque = engineOutTorque/car.wheelRadius;
		return totalTorque;
	}

	private void autoTransmission(int rpm, float vz) {
		if (curGear == 0) return; //no changing out of reverse on me please..

		float gearUpSpeed = car.gearUp/(car.gearRatios[curGear]*car.diffRatio*60);
		float gearDownSpeed = car.gearDown/(car.gearRatios[curGear]*car.diffRatio*60);
		//TODO: error checking on making sure the one we change to doesn't instantly change back


		if (vz > gearUpSpeed && curGear < car.gearRatios.length-1) {
			curGear++;
		} else if (vz < gearDownSpeed && curGear > 1) {
			curGear--;
		}
	}

	//assumed to be a valid and useable rpm value
	private float lerpTorque(int rpm) {
		float RPM = (float)rpm / 1000;
		return H.lerpArray(RPM, car.torque);
	}

	//////////////////////////////////////////////////////////////
	public void myUpdate(float tpf) {
		distance += getLinearVelocity().length()*tpf;

		Matrix3f playerRot = new Matrix3f();
		getPhysicsRotationMatrix(playerRot);

		up = playerRot.mult(new Vector3f(0,1,0));
		left = playerRot.mult(new Vector3f(1,0,0));
		right = playerRot.mult(new Vector3f(1,0,0).negate());

		for (MyWheelNode w: wheel) {
			WheelInfo wi = getWheel(w.num).getWheelInfo();
			RaycastInfo ray = wi.raycastInfo;
			w.contact = (ray.groundObject != null);

			w.update(tpf, curRPM);
		}

		//skid marks
		addSkidLines();

		specialPhysics(tpf); //yay

		//wheel turning logic -TODO
		//trying to turn less at high speed
		steeringCurrent = 0;
		float speedFactor = 1;
		//		speedFactor = car.steerFactor/(getLinearVelocity().length()*10);
		if (steerLeft != 0) { //left
			steeringCurrent += car.steerAngle*speedFactor*steerLeft; //maxangle*speedFactor*turnfraction
		}
		if (steerRight != 0) { //right
			steeringCurrent -= car.steerAngle*speedFactor*steerRight; //maxangle*speedFactor*turnfraction
		}
		steer(steeringCurrent);
		H.pUI(steeringCurrent);

		
		//update ai if any
		if (ai != null) {
			ai.update(tpf);
		}

		if (engineSound != null) {
			//if sound exists
			float pitch = FastMath.clamp(0.5f+1.5f*(curRPM/car.redline), 0.5f, 2);
			engineSound.setPitch(pitch);
		}
	}

	///////////////////////////////////////////////////////////
	private void addSkidLines() {
		if (ai != null) return;

		if (App.rally.drive.frameCount % 4 == 0) {
			for (MyWheelNode w: wheel) {
				w.addSkidLine();
			}

			int extra = skidList.size() - 500; //so i can remove more than one (like all 4 that frame)
			for (int i = 0; i < extra; i++) {
				skidNode.detachChild(skidList.getFirst());
				skidList.removeFirst();
			}
		}
	}


	private void reset() {
		if (App.rally.drive == null) return;

		setPhysicsRotation(new Matrix3f());
		setLinearVelocity(new Vector3f());
		setAngularVelocity(new Vector3f());

		App.rally.drive.reset();

		if (App.rally.drive.dynamicWorld) {
			setPhysicsLocation(App.rally.drive.worldB.start);
			Matrix3f p = new Matrix3f();
			p.fromAngleAxis(FastMath.DEG_TO_RAD*90, new Vector3f(0,1,0));
			setPhysicsRotation(p);

			setAngularVelocity(new Vector3f());
		} else {
			setPhysicsLocation(App.rally.drive.world.start);
		}

		skidNode.detachAllChildren();
		skidList.clear();
		for (MyWheelNode w: wheel) {
			w.last = new Vector3f(0,0,0);
		}
	}

	private void flipMe() {
		if (this.up.y > 0) return; //if already the right way up

		Quaternion q = getPhysicsRotation(); 
		q.multLocal(new Quaternion().fromAngleAxis(FastMath.PI, new Vector3f(0,0,1)));
		setPhysicsRotation(q);
		setPhysicsLocation(getPhysicsLocation().add(new Vector3f(0,1,0)));
	}

	@Override
	public float getCurrentVehicleSpeedKmHour() {
		return vehicle.getCurrentSpeedKmHour();
	}

	public void cleanup() {
		//TODO more
		this.ai = null;
		if (this.control != null) {
			App.rally.getInputManager().removeRawInputListener(this.control);
			this.control = null;
		}
		
		if (this.engineSound != null) {
			carRootNode.detachChild(this.engineSound);
			App.rally.getAudioRenderer().stopSource(engineSound);
			engineSound = null;
		}
	}
}

class VehiclePhysicsHelper {
	//http://www.gamedev.net/topic/462784-simplified-pacejka-magic-formula/
	//http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/

	//There were fancy versions of the Pacejka's Formula here but there were removed.
	//Try the git repositiory to get them back. (i should say 'removed' in the git message)

	/** Pacejka's Formula simplified from the bottom of:.
	 * http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
	 * @param w Pick the lateral or longitudial version to send.
	 * @param slip Slip angle or slip ratio (it doesn't matter except for one value changes on it)
	 * @return The force expected
	 */
	static float tractionFormula(CarWheelData w, float slip) {
		return w.D * FastMath.sin(w.C * FastMath.atan(w.B*slip - w.E * (w.B*slip - FastMath.atan(w.B*slip))));
	}

	//returns the slip value that gives the closest to 1 from the magic formula (should be called twice, lat and long)
	static float calcSlipMax(CarWheelData w) {
		double error = 0.0005f; //our fixed error, we don't really care how close it is past 3 or 4 decimals

		double lastX = 0.2f; //0.2 is always our first guess (12 deg or so is good)
		double nextX = lastX + 10*error; //just so its a larger diff that error

		while (Math.abs(lastX - nextX) > error) {
			lastX = nextX;
			nextX = iterate(w, lastX, error);
		}

		return (float)nextX;
	}
	private static double iterate(CarWheelData w, double x, double error) {
		return x - ((tractionFormula(w, (float)x)-1) / dtractionFormula(w, (float)x, error)); 
		//-1 because we are trying to find a max (which happens to be 1)
	}
	private static double dtractionFormula(CarWheelData w, double slip, double error) {
		return (tractionFormula(w, (float)(slip+error)) - tractionFormula(w , (float)(slip-error)))/ (2*error);
	}


	//TODO fix (use http://phors.locost7.info/phors25.htm)
	//basically gets fraction of maximum for each force and limits the total value by it
	static float mergeSlips(float slipangle, float slipratio) {
		//TODO calculate the slip* max values, and use it to limit the other


		return 1f;
	}
}