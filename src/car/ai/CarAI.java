package car.ai;

import car.ray.RayCarControl;

public abstract class CarAI {
	
	protected RayCarControl car;

	public CarAI(RayCarControl car) {
		this.car = car;
	}
	
	public abstract void update(float tpf);
	
	protected void onAction(String act, boolean ifdown, float amnt) {
		car.onAction(act, ifdown, amnt);
	}
}
