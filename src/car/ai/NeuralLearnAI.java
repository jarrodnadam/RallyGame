package car.ai;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

import car.ai.ml.NeuralNetwork;
import car.ray.RayCarControl;
import drive.DriveBase;
import drive.DriveRace;
import helper.Log;
import world.wp.DefaultBuilder;

public class NeuralLearnAI extends CarAI {

	private DefaultBuilder world;
	NeuralNetwork NN;
	
	public NeuralLearnAI(RayCarControl car, DefaultBuilder world) {
		super(car);
		this.world = world;
		int[] sizes = new int[]{8, 4};		
		this.NN = new NeuralNetwork(sizes);
	}
	

	@Override
	public void update(float tpf) {
		Vector3f pos = car.getPhysicsLocation();
		Vector3f atPos = world.getNextPieceClosestTo(pos);
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
		float gForce = car.planarGForce.length();
		float driftAngle = car.driftAngle;
		float dragValue = car.dragValue;
		float suspForce = car.wheels[0].susForce / 1000f;
		
	
		
		//get attempted turn angle as pos or negative
		float nowTurn = angF*Math.signum(FastMath.HALF_PI-ang);
		
		
		float[] input = new float[] {
				nowTurn, 
				pos.subtract(atPos).length(), 
				ang, 
				suspForce, 
				gForce, 
				driftAngle, 
				dragValue, 
				car.getLinearVelocity().length()
		};
				
		
		/*
			= someFunction(X)

		  
		 */
		
		// y = someFunction(...x) 
		// y has 4 attr {'Left': -1...1, 'Right': -1...1, 'Brake':-1...1, 'Accel': -1...1}
		// y [-1, 1] where -1 means stop the key; 1 is press that key; 0 the key is unchanged
		// need to determine the cutoff for between [-1, 0] and [0, 1] (maybe its the same? but set is as |0.5| for now) 
		// 
		// keyStroke = applyProbCutoff(y[key])
		// keyStrokeMag = 
		// onAction('key', 

		float[] output = NN.feedForward(input);		
		float cutOff = 0.3f;
		
		Log.p("Input:");
		Log.p(input, ",");
		Log.p("Output:");
		Log.p(output, ",");
		
		
		if (output[0] > cutOff) {
			onAction("Left", true, reverse);
		} else {
			onAction("Left", false, 0);
		}
		if (output[1] > cutOff) {
			onAction("Right", true, reverse);
		} else {
			onAction("Right", false, 0);
		} 
		if (output[2] > cutOff) {
			onAction("Brake", true, 1);
		} else {
			onAction("Brake", false, 0);
		} 
		if (output[3] > cutOff) {
			onAction("Accel", true, 1);
		} else {
			onAction("Accel", false, 0);
		}
	}
	class Prediction {
		public float left;
		public float right;
		public float brake;
		public float accele;
		
		public Prediction(float left, float right, float brake, float accele) {
			this.left = left;
			this.right = right;
			this.brake = brake;
			this.accele = accele;					
		}
		
	}
}
