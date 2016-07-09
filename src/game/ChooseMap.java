package game;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Vector3f;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

import world.*;

public class ChooseMap extends AbstractAppState implements ScreenController {

	private enum WorldType {
		nothing, staticW, dynamicW;
	}

	private BulletAppState bulletAppState;

	private static boolean worldIsSet = false;
	private static WorldType worldType = WorldType.nothing;

	private static StaticWorld sMap;
	private HashMap<String, StaticWorld> sSet;
	
	private static WP dMap;
	private HashMap<String, WP> dSet;

	private static DropDown<String> dropdown;
	private MyCamera camNode;

	public ChooseMap() {
		//camera
		camNode = new MyCamera("Cam Node 2", App.rally.getCamera(), null);
		camNode.setLocalTranslation(100, 140, 100);
		camNode.lookAt(new Vector3f(0,1,0), new Vector3f(0,1,0));

		App.rally.getRootNode().attachChild(camNode);
		
		//init static
		sSet = new LinkedHashMap<>();
		StaticWorld[] list  = StaticWorld.values();
		for (StaticWorld s: list) {
			sSet.put(s.name(), s);
		}

		//init dynamic
		dSet = new LinkedHashMap<>(); //ordered hashmap
		dSet.put("Floating", Floating.STRAIGHT);
		dSet.put("Cliff", Cliff.STRAIGHT);
		dSet.put("Simple", Simple.STRAIGHT);
		dSet.put("Simple2", Simple2.STRAIGHT);
		dSet.put("City", City.STRAIGHT);
		dSet.put("Track", Track.STRAIGHT);
		
		//set initial values
		sMap = list[0];
		dMap = Floating.STRAIGHT;
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		bulletAppState = new BulletAppState();
		app.getStateManager().attach(bulletAppState);

	}
	private void initWorld() {
		createWorld();
	}

	private void createWorld() {
		if (worldType == WorldType.staticW) {
			StaticWorldBuilder.addStaticWorld(getPhysicsSpace(), sMap, App.rally.sky.ifShadow);
		} else if (worldType == WorldType.dynamicW) {
			WorldBuilder.placeOnePiece(dMap);
		}
	}

	public void update(float tpf) {
		if (!isEnabled()) return;
		super.update(tpf);

		if (worldType != WorldType.nothing && !worldIsSet) { //keep checking if the ui changed something
			//if the worldtype has been set, init the apprpriate world type
			initWorld();
			worldIsSet = true;
			return;
		}

		if (dropdown != null) {
			String option = dropdown.getSelection();
			if (worldType == WorldType.staticW) {
				StaticWorld w = sSet.get(option);
				if (w != null && w != sMap) {
					//remove previous map
					StaticWorldBuilder.removeStaticWorld(getPhysicsSpace(), sMap);

					sMap = w;
					StaticWorldBuilder.addStaticWorld(getPhysicsSpace(), sMap, App.rally.sky.ifShadow);
				}
				
			} else if (worldType == WorldType.dynamicW) {
				WP w = dSet.get(option);
				if (w != null && w != dMap) {
					dMap = w;
					
					WorldBuilder.placeOnePiece(dMap);
				}
			}
		}

		camNode.myUpdate(tpf);
	}

	public PhysicsSpace getPhysicsSpace() {
		return bulletAppState.getPhysicsSpace();
	}

	public void cleanup() {
		StaticWorldBuilder.removeStaticWorld(getPhysicsSpace(), sMap);
		WorldBuilder.placeOnePiece(null); // add nothing, makes it remove previous
		
		App.rally.getRootNode().detachChild(camNode);
		//TODO more
	}

	////////////////////////
	//UI stuff
	public void staticWorld() {
		worldType = WorldType.staticW;
		App.nifty.gotoScreen("chooseMap");
	}
	public void dynamicWorld() {
		worldType = WorldType.dynamicW;
		App.nifty.gotoScreen("chooseMap");
	}

	public void chooseMap() {
		if (sMap == null && dMap == null) { H.p("no return value for ChooseMap()"); }
		App.rally.next(this);
	}

	public StaticWorld getMapS() {
		if (worldType == WorldType.staticW)
			return sMap;
		else
			return null;
	}
	public WP[] getMapD() {
		if (worldType == WorldType.dynamicW)
			return dMap.getClass().getEnumConstants();
		else 
			return null;
	}

	@Override
	public void bind(Nifty arg0, Screen arg1) {
		if (arg1.getScreenId().equals("chooseMap")) {
			dropdown = H.findDropDownControl(arg1, "mapdropdown");
			if (dropdown != null) {
				if (worldType == WorldType.staticW) {
					for (String s : sSet.keySet()) {
						dropdown.addItem(s);
					}
					dropdown.selectItemByIndex(0);
				
				} else if (worldType == WorldType.dynamicW) {
					for (String s : dSet.keySet()) {
						dropdown.addItem(s);
					}
					dropdown.selectItemByIndex(0);
				}
			}
		}
	}

	public void onEndScreen() { }
	public void onStartScreen() { }
}
