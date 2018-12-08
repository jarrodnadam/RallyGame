package car.ai;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

import car.ray.RayCarControl;
import drive.DriveRace;
import helper.Log;

public class RaceAI extends CarAI {

	private DriveRace race;
	
	public RaceAI(RayCarControl car, RayCarControl notused, DriveRace race) {
		super(car);
		this.race = race;
	}

	@Override
	public void update(float tpf) {
		Vector3f pos = car.getPhysicsLocation();
		Vector3f atPos = race.getNextCheckpoint(this, pos);
		if (atPos == null) {
			Log.e("at pos was null though?");
			return; //don't know what to do
		}
		
		Matrix3f w_angle = car.getPhysicsRotationMatrix();
		Vector3f velocity = w_angle.invert().mult(car.vel);
		int reverse = (velocity.z < 0 ? -1 : 1);
		
		Vector3f myforward = car.forward;
		
		float angF = myforward.normalize().angleBetween((atPos.subtract(pos)).normalize());
		float ang = car.left.normalize().angleBetween((atPos.subtract(pos)).normalize());
		
		//get attempted turn angle as pos or negative
		float nowTurn = angF*Math.signum(FastMath.HALF_PI-ang);
		
		//turn towards 
		if (nowTurn < 0) {
			onAction("Left", false, 0);
			onAction("Right", true, Math.abs(nowTurn)*reverse);
		} else {
			onAction("Right", false, 0);
			onAction("Left", true, Math.abs(nowTurn)*reverse);
		}
		//slow down to turn
		if (FastMath.abs(angF) < FastMath.QUARTER_PI) {
			onAction("Brake", false, 0);
			onAction("Accel", true, 1);
		} else {
			onAction("Brake", true, 1);
			onAction("Accel", false, 0);
		}
		
		//if going to slow speed up
		if (car.getLinearVelocity().length() < 10) {
			onAction("Accel", true, 1);
			onAction("Brake", false, 0);
			
			if (car.getLinearVelocity().length() < 1 && car.up.y < 0) { //very still
				onAction("Flip", true, 1);
			}
		}
		if (car.getLinearVelocity().length() > 20) {
			onAction("Accel", false, 0); //don't go too fast
			onAction("Brake", false, 0);
		}
		
		//TODO some kind of ray cast so they can drive around things at all
	}
}
