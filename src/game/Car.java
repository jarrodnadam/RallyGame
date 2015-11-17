package game;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public abstract class Car {
	
	//model strings
	String carOBJModel = "assets/car4.obj";
	String wheelOBJModel = "assets/wheel.obj";
	
	//physical things
	float mass = 800;
	float width = 1.4f; //x size, door handle to door handle
	float height = 1f; //y size, roof to ground
	float length = 3f; //z size, from front to back
	
	//wheels
	Vector3f wheelDirection = new Vector3f(0, -1, 0); //vertical
	Vector3f wheelAxle = new Vector3f(-1, 0, 0); //horizontal

	float wheelWidth = 0.15f;
	float wheelRadius = 0.3f;
	
	float w_xOff = 0.68f; //wheels x offset (side)
	float w_yOff = 0f; //wheels no height
	float w_zOff = 1.1f; //wheels z offset (front and back)
	
	
	//suspension values for wheels
	//see for details: https://docs.google.com/Doc?docid=0AXVUZ5xw6XpKZGNuZG56a3FfMzU0Z2NyZnF4Zmo&hl=en
	float stiffness = 60.0f;//200=f1 car
	float compValue = 0.3f; //(should be lower than damp)
	float dampValue = 0.4f;
	float restLength = -0.05f;
	
	float susCompression = compValue * 2 * FastMath.sqrt(stiffness);
	float susDamping = dampValue * 2 * FastMath.sqrt(stiffness);
	
	//grip constants
	//my physics work mostly with 0.01f, but feels better with:
	float wheel1Slip = 0.9f;//0.9
	float wheel2Slip = 0.9f;//0.9
	float wheel3Slip = 0.9f;//0.6
	float wheel4Slip = 0.9f;//0.6
	
	//my physics grip constants - TODO tweak numbers
	float DRAG = 4.5f;
	float RESISTANCE = 30.0f;
	float CA_R = -5;
	float CA_F = -5;
	float MAX_GRIP = 1.5f;
	
	//other
	float MAX_ACCEL = 90;
	float MAX_STEERING = 0.5f;
	float MAX_BRAKE = 100;
	Vector3f JUMP_FORCE = new Vector3f(0, 5*mass, 0);
}

class RallyCar extends Car {
	
	RallyCar() {
		//jarrod
		wheel1Slip = 1.2f;
		wheel2Slip = 1.2f;
	}
}

class TrackCar extends Car { //TODO maybe massive enum?
	
	TrackCar() {
		carOBJModel = "assets/f1.obj";
		wheelOBJModel = "assets/f1_wheel.obj";
		
		DRAG = 3f;
		RESISTANCE = 20;
		
		MAX_STEERING = 0.25f;
		MAX_ACCEL = 140;
		MAX_BRAKE = 200;
		
		stiffness = 100.0f;
		restLength = 0.05f;
		compValue=0.8f;
		dampValue = 0.9f;
		
		susCompression = compValue * 2 * FastMath.sqrt(stiffness);
		susDamping = dampValue * 2 * FastMath.sqrt(stiffness);
		
		width = 1.5f;
		height = 0.7f;
		length = 5f;
		
		w_xOff = 0.62f; //wheels x offset (side)
		w_yOff = 0.1f; //wheels y offset (height)
		w_zOff = 1.6f; //wheels z offset (front and back)
		
	}
}


