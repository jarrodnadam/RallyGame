package world;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

import game.App;

public class FlatWorld extends World {
	
	private Geometry startGeometry; 
	
	public FlatWorld() {
		super("flatWorldRoot");
	}
	
	@Override
	public WorldType getType() {
		return WorldType.FLAT;
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		
		AssetManager am = app.getAssetManager();
		
		Material matfloor = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		matfloor.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		matfloor.setColor("Color", ColorRGBA.Green);
		
		Box start = new Box(20, 0.25f, 20);
		startGeometry = new Geometry("box", start);
		startGeometry.setMaterial(matfloor);
		startGeometry.setLocalTranslation(0, -0.1f, 0);
		startGeometry.addControl(new RigidBodyControl(0));
		
		this.rootNode.attachChild(startGeometry);
		App.rally.getPhysicsSpace().add(startGeometry);
	}
	
	@Override
	public void reset() {
		
	}

	@Override
	public void update(float tpf) {
		//TODO mod 1000 the car pos, so the grounds bumps don't occur
		Vector3f pos = App.rally.getCamera().getLocation();
		pos.y = 0;
		startGeometry.setLocalTranslation(pos);
		startGeometry.getControl(RigidBodyControl.class).setPhysicsLocation(pos);
	}
	
	public void cleanup() {
		this.rootNode.detachChild(startGeometry);
		App.rally.getPhysicsSpace().remove(startGeometry);
	}
}
