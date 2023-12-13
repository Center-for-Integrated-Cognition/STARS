package soargroup.mobilesim.sim;

import java.awt.Color;
import java.io.IOException;
import java.util.*;

import soargroup.mobilesim.util.BoundingBox;
import april.jmat.LinAlg;
import april.util.*;
import april.sim.*;
import april.vis.*;

import soargroup.rosie.RosieConstants;
import soargroup.mobilesim.vis.VzOpenBox;
import soargroup.mobilesim.util.ResultTypes.*;
import soargroup.mobilesim.sim.attributes.*;
import soargroup.mobilesim.sim.actions.*;

// Lcm Types
import soargroup.mobilesim.lcmtypes.object_data_t;
import soargroup.mobilesim.lcmtypes.classification_t;

public class RosieSimObject extends BaseSimObject{
	protected enum ModelType {
		BOX, WIRE_BOX, OPENBOX, CYLINDER, OPEN_CYLINDER, CONE, SPHERE, CUSTOM,
	}
	protected Integer id;
	protected String desc;
	protected String tempId = null;
	protected Color  color = Color.gray;
	protected ModelType model = ModelType.BOX;
	protected HashMap<String, String> properties = new HashMap<String, String>();

	private static int NEXT_ID = 1;

	private boolean _isVisible = true;

	public RosieSimObject(SimWorld sw){
		super(sw);
		id = RosieSimObject.NEXT_ID;
		RosieSimObject.NEXT_ID += 1;
	}

	// A RosieSimObject is composed of Attributes that define its behavior 
	//    and how it responds to different actions
	// (e.g., if it is Grabbable, then you can issue PickUp commands for the object)
	//
	// Notes: the attributes map can have multiple keys mapped to the same Attribute object
	//        whereas the uniqueAttrs set has only 1 of each object
	//
	// For example, to see if a given object is Grabbable, do:
	//    if(object.is(Grabbable.class))
	//
	// To make the object not grabbed, you would do:
	//    object.as(Grabbable.class).setGrabbed(false)
	//
	protected HashMap<Class<?>, Attribute> attributes = new HashMap<Class<?>, Attribute>();
	private HashSet<Attribute> uniqueAttrs = new HashSet<Attribute>();
	public <T extends Attribute> boolean is(Class<T> cls){
		return attributes.containsKey(cls);
	}
	public <T extends Attribute> T as(Class<T> cls){
		Attribute attr = attributes.get(cls);
		return attr == null ? null : (T)attr;
	}

	protected void addAttribute(Attribute attr){
		uniqueAttrs.add(attr);
		// Add any supertype Attributes as well (e.g. adding Surface also adds ObjectHolder)
		Class cls = attr.getClass();
		while(Attribute.class.isAssignableFrom(cls) && cls != Attribute.class){
			attributes.put(cls, attr);
			cls = cls.getSuperclass();
		}

	}
	
	public Integer getID(){
		return id;
	}

	public String getTempId(){
		// tempId is an optional identifying string used internally to let objects refer to one another
		return tempId;
	}

	public String getDesc(){
		return desc;
	}

	public String getLabel(boolean includeId){
		return desc + (includeId ? "_" + id.toString() : "");
	}

	public String toString(){
		return desc + "_" + id.toString();
	}

	public String getProperty(String prop){
		return properties.get(prop);
	}

	public boolean isVisible(){
		return _isVisible;
	}

	public void setVisible(boolean isVisible){
		this._isVisible = isVisible;
	}

	@Override
	public void setXYZRPY(double[] newpose){
		for(Attribute attr : uniqueAttrs){
			attr.moveHandler(newpose);
		}
		this.xyzrpy = newpose;
	}

	@Override
	public void init(ArrayList<SimObject> worldObjects) { 
		addAttribute(new InRegion(this));
		for(Attribute attr : uniqueAttrs){
			attr.init(worldObjects);
		}
	}

	// Action Handling Rules
	static {
		// PickUp Apply: Make object non-collidable
		ActionHandler.addApplyRule(PickUp.class, new ActionHandler.ApplyRule<PickUp>() {
			public Result apply(PickUp pickup){
				pickup.object.collide = false;
				return Result.Ok();
			}
		});
		// PutDown Apply: Make object collidable again
		ActionHandler.addApplyRule(PutDown.class, new ActionHandler.ApplyRule<PutDown>() {
			public Result apply(PutDown putdown){
				putdown.object.collide = true;
				return Result.Ok();
			}
		});

		//// SetProp Apply: Change the property
		//ActionHandler.addApplyRule(SetProp.class, new ActionHandler.ApplyRule<SetProp>() {
		//	public Result apply(SetProp setprop){
		//		setprop.object.setProprety(setprop.property, setprop.value);
		//		return Result.Ok();
		//	}
		//});
	}

	// Children can override to implement any dynamics, this is called multiple times/second
	// dt is time elapsed since last update (fraction of a second)
	@Override
	public void update(double dt, ArrayList<SimObject> worldObjects){
		for(Attribute attr : uniqueAttrs){
			attr.update(dt, worldObjects);
		}
	}

	public void setProperty(String property, String value){
		properties.put(property, value);
	}

	@Override
	public Shape getShape(){
		if(!soargroup.mobilesim.MobileSimulator.Settings.COLLIDE_OBJECTS){
			return new april.sim.SphereShape(0.0);
		}
		return super.getShape();
	}

	@Override
	public VisObject getVisObject(){
		if(visObject == null){
			VisChain vc = createVisObject();
			for(Attribute attr : uniqueAttrs){
				attr.render(vc);
			}
			visObject = vc;
		}
		return visObject;
	}

	@Override
	public VisChain createVisObject() {
		VisChain vc = new VisChain();
		switch(model){
			case BOX:
				vc.add(new VzBox(scale_xyz, new VzMesh.Style(color)));
				break;
			case OPENBOX:
				vc.add(new VzOpenBox(scale_xyz, new VzMesh.Style(color)));
				break;
			case WIRE_BOX:
				vc.add(new VzBox(scale_xyz, new VzLines.Style(Color.black, 1)));
				break;
			case CYLINDER:
				vc.add(new VzCylinder(scale_xyz[0]*0.5, scale_xyz[2], new VzMesh.Style(color)));
				break;
			case OPEN_CYLINDER:
				vc.add(new VzCylinder(scale_xyz[0]*0.5, scale_xyz[2], VzCylinder.BOTTOM, new VzMesh.Style(color)));
				break;
			case SPHERE:
				vc.add(LinAlg.scale(scale_xyz[0], scale_xyz[1], scale_xyz[2]), new VzSphere(0.5, new VzMesh.Style(color)));
				break;
			case CONE:
				// Need to move down because VzCone's circle face is on the xy plane
				vc.add(LinAlg.translate(0.0, 0.0, -scale_xyz[2]*0.5), new VzCone(scale_xyz[0]*0.5, scale_xyz[1], new VzMesh.Style(color)));
				break;
			case CUSTOM:
				String modelName = properties.get("category").replaceAll("\\d", "");
				VisChain vobj = ObjectModels.createModel(modelName, scale_xyz, color);
				if(vobj != null){
					vc = vobj;
				}
		}
		return vc;
	}

	public object_data_t getObjectData(){
		object_data_t objdat = new object_data_t();
		objdat.utime = TimeUtil.utime();
		objdat.id = id;

		objdat.xyzrpy = LinAlg.copy(xyzrpy);
		objdat.lenxyz = LinAlg.copy(scale_xyz);

		objdat.num_classifications = 0;
		objdat.classifications = new classification_t[properties.size()];
		for(Map.Entry<String, String> e : properties.entrySet()){
			classification_t cls = new classification_t();
			cls.category = e.getKey();
			cls.name = e.getValue();
			cls.confidence = 1.0;
			objdat.classifications[objdat.num_classifications] = cls;
			objdat.num_classifications += 1;
		}

		if(this.is(ObjectHolder.class)){
			ObjectHolder objHolder = this.as(ObjectHolder.class);
			List<RosieSimObject> heldObjs = objHolder.getHeldObjects();
			objdat.num_objects = heldObjs.size();
			objdat.contained_objects = new int[objdat.num_objects];
			for(int i = 0; i < heldObjs.size(); i += 1){
				objdat.contained_objects[i] = heldObjs.get(i).getID();
			}
		} else {
			objdat.num_objects = 0;
			objdat.contained_objects = new int[0];
		}

		return objdat;
	}

	
	/** Restore state that was previously written **/
	public void read(StructureReader ins) throws IOException
	{
		// [Str] description
		desc = ins.readString();

		super.read(ins); // xyz rot scale_xyz

		// [Int]x3 rgb color
		int rgb[] = ins.readInts();
		color = new Color(rgb[0], rgb[1], rgb[2]);

		// [Int] number of properties
		int num_props = ins.readInt();
		// [Str]x2 for each property (property, value)
		for(int i = 0; i < num_props; i += 1){
			String prop = ins.readString().toLowerCase().trim();
			if(prop.equals("grabbable")){
				addAttribute(new Grabbable(this));
			} else if(prop.equals("surface")){
				addAttribute(new Surface(this, true));
			} else if(prop.equals("receptacle")){
				addAttribute(new Receptacle(this, true));
			} else if(prop.equals("drain")){
				addAttribute(new Drain(this));
			} else if(prop.equals("dispenser")){
				addAttribute(new Dispenser(this));
			} else if(prop.equals("openbox")){
				model = ModelType.OPENBOX;
			} else if(prop.equals("cylinder")){
				model = ModelType.CYLINDER;
			} else if(prop.equals("opencylinder")){
				model = ModelType.OPEN_CYLINDER;
			} else if(prop.equals("cone")){
				model = ModelType.CONE;
			} else if(prop.equals("sphere")){
				model = ModelType.SPHERE;
			} else if(prop.equals("custom-model")){
				model = ModelType.CUSTOM;
			} else if(prop.contains("=")){
				String[] splitProp = prop.split("=");
				if(splitProp[0].equals("temp_id")){
					tempId = splitProp[1];
				} else {
					properties.put(splitProp[0], splitProp[1]);
					if(splitProp[0].equals(RosieConstants.LOCK)){
						addAttribute(new Lockable(this, splitProp[1].equals(RosieConstants.LOCKED)));
					}
				}
			}
		}
	}

	/** Write one or more lines that serialize this instance. No line
	 * is allowed to consist of just an asterisk. **/
	public void write(StructureWriter outs) throws IOException
	{
		outs.writeString(desc);
		super.write(outs);

		int rgb[] = new int[]{ color.getRed(), color.getGreen(), color.getBlue() };
    	outs.writeInts(rgb);

		outs.writeInt(properties.size());
		for(Map.Entry<String, String> e : properties.entrySet()){
			outs.writeString(e.getKey());
			outs.writeString(e.getValue());
		}
	}
}
