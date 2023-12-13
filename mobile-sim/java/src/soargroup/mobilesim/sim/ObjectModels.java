package soargroup.mobilesim.sim;

import java.awt.Color;

import april.sim.*;
import april.vis.*;
import april.util.*;
import april.jmat.LinAlg;

import soargroup.mobilesim.vis.VzOpenBox;
import soargroup.mobilesim.vis.VzTriangularPrism;

public class ObjectModels {

	public static VisChain createModel(String modelName, double[] scale, Color color){
		modelName = modelName.toLowerCase();
		if(modelName.equals("chair"))
			return createChairModel(scale, color);
		if(modelName.equals("bunk"))
			return createBunkModel(scale, color);
		if(modelName.equals("shelf"))
			return createShelfModel(scale, color);
		if(modelName.equals("table"))
			return createTableModel(scale, color);
		if(modelName.equals("desk"))
			return createDeskModel(scale, color);
		if(modelName.equals("car"))
			return createCarModel(scale, color);
		if(modelName.equals("truck"))
			return createTruckModel(scale, color);
		if(modelName.equals("extinguisher"))
			return createExtinguisherModel(scale, color);
		if(modelName.equals("carton"))
			return createCartonModel(scale, color);
		if(modelName.equals("computer"))
			return createComputerModel(scale, color);
		return null;
	}

	/***** Helper Methods that simplify adding certain types of models *****/

	// Adds a VzSphere to the given VisChain with the given x,y,z center and radius, with the given style
	public static void addSphere(VisChain vc, double x, double y, double z, double r, VzMesh.Style style){
		vc.add(new VisChain(LinAlg.translate(x, y, z), new VzSphere(r, style)));
	}

	// Adds a VzBox to the given VisChain with the given x,y,z center and dx,dy,dz bounds, with the given style
	public static void addBox(VisChain vc, double x, double y, double z, double dx, double dy, double dz, VzMesh.Style style){
		vc.add(new VisChain(LinAlg.translate(x, y, z), new VzBox(dx, dy, dz, style)));
	}

	// Adds a VzCylinder to the given VisChain with the given x,y,z center with given radius, height, and style
	public static void addCyl(VisChain vc, double x, double y, double z, double r, double h, VzMesh.Style style){
		vc.add(new VisChain(LinAlg.translate(x, y, z), new VzCylinder(r, h, style)));
	}

	// Adds a VzCone to the given VisChain with the given x,y,z center with given face radius, height, and style
	public static void addCone(VisChain vc, double x, double y, double z, double r, double h, VzMesh.Style style){
		vc.add(new VisChain(LinAlg.translate(x, y, z-h*0.5), new VzCone(r, h, style)));
	}

	// Adds a VzTriangularPrism to the given VisChain with the given x,y,z center, scale, and style
	public static void addTriPrism(VisChain vc, double x, double y, double z, double dx, double dy, double dz, VzMesh.Style style){
		vc.add(new VisChain(LinAlg.translate(x, y, z), LinAlg.scale(dx, dy, dz), new VzTriangularPrism(style)));
	}

	/***** Specific Models ******/

	public static VisChain createChairModel(double[] scale, Color color){
		VisChain vc = new VisChain();
		VzMesh.Style chair_color = new VzMesh.Style(color);
		VzMesh.Style leg_color = new VzMesh.Style(new Color(94, 76, 28)); // brown

		final double LEG_W = 0.1;
		final double SEAT_H = 0.1;
		final double BACK_W = 0.1;

		// Chair Back
		double back_height = scale[2]/2 - SEAT_H/2;
		addBox(vc, -scale[0]/2 + BACK_W/2, 0.0, SEAT_H/2 + back_height/2,
				   BACK_W, scale[1], back_height, chair_color);

		// Seat
		addBox(vc, 0.0, 0.0, 0.0, scale[0], scale[1], SEAT_H, chair_color);

		double leg_h = (scale[2] - LEG_W)/2;
		double leg_x = (scale[0] - LEG_W)/2;
		double leg_y = (scale[1] - LEG_W)/2;
		double leg_z = -SEAT_H/2 - leg_h/2;

		// Legs of Chair
		addBox(vc,  leg_x,  leg_y, leg_z, LEG_W, LEG_W, leg_h, leg_color);
		addBox(vc, -leg_x,  leg_y, leg_z, LEG_W, LEG_W, leg_h, leg_color);
		addBox(vc, -leg_x, -leg_y, leg_z, LEG_W, LEG_W, leg_h, leg_color);
		addBox(vc,  leg_x, -leg_y, leg_z, LEG_W, LEG_W, leg_h, leg_color);
		return vc;
	}

	public static VisChain createBunkModel(double[] scale, Color color){
		VisChain vc = new VisChain();

		final double POLE_W = 0.1;
		VzMesh.Style bunk_color = new VzMesh.Style(color); 

		// Corner Poles
		double pole_x = (scale[0] - POLE_W)/2;
		double pole_y = (scale[1] - POLE_W)/2;
		addBox(vc,  pole_x,  pole_y, 0.0, POLE_W, POLE_W, scale[2], bunk_color);
		addBox(vc,  pole_x, -pole_y, 0.0, POLE_W, POLE_W, scale[2], bunk_color);
		addBox(vc, -pole_x,  pole_y, 0.0, POLE_W, POLE_W, scale[2], bunk_color);
		addBox(vc, -pole_x, -pole_y, 0.0, POLE_W, POLE_W, scale[2], bunk_color);

		// Bottom Bunk
		addBox(vc, 0.0, 0.0, -scale[2]*0.25, scale[0], scale[1], 0.2, bunk_color);

		// Top Bunk
		addBox(vc, 0.0, 0.0, scale[2]*0.25, scale[0], scale[1], 0.2, bunk_color);

		return vc;
	}

	public static VisChain createShelfModel(double[] scale, Color color){
		VisChain c = new VisChain();
		VzMesh.Style style = new VzMesh.Style(color);

		final double SHELF_SPACING = 0.50; // vertical spacing between shelves

		// Outer Bounds
		c.add(new VisChain(LinAlg.rotateY(Math.PI/2), new VzOpenBox(scale[2], scale[1], scale[0], style)));

		// Shelves
		int nshelves = (int)Math.ceil(scale[2]/SHELF_SPACING)-1;
		double sz = -(nshelves-1)/2.0;
		for(int i = 0; i < nshelves; i += 1){
			c.add(new VisChain(LinAlg.translate(0.0, 0.0, (sz + i)*SHELF_SPACING), 
						new VzRectangle(scale[0], scale[1], new VzMesh.Style(color))));
		}

		return c;
	}

	public static VisChain createTableModel(double[] scale, Color color){
		VisChain vc = new VisChain();
		VzMesh.Style style = new VzMesh.Style(color);

		// Leg Thickness
		final double THICKNESS = 0.1;

		// Top of table
		addBox(vc, 0.0, 0.0, (scale[2]-THICKNESS)*0.5, scale[0], scale[1], THICKNESS, style);

		// Legs of table
		double leg_dX = (scale[0] - THICKNESS)/2;
		double leg_dY = (scale[1] - THICKNESS)/2;
		addBox(vc,  leg_dX,  leg_dY, 0.0, THICKNESS, THICKNESS, scale[2], style);
		addBox(vc, -leg_dX,  leg_dY, 0.0, THICKNESS, THICKNESS, scale[2], style);
		addBox(vc, -leg_dX, -leg_dY, 0.0, THICKNESS, THICKNESS, scale[2], style);
		addBox(vc,  leg_dX, -leg_dY, 0.0, THICKNESS, THICKNESS, scale[2], style);

		return vc;
	}

	public static VisChain createDeskModel(double[] scale, Color color){
		VisChain vc = new VisChain();
		VzMesh.Style style = new VzMesh.Style(color);
	
		// Top of table/leg thickness
		final double THICKNESS = 0.1;

		// Top Surface
		addBox(vc, 0.0, 0.0, (scale[2]-THICKNESS)*0.5, scale[0], scale[1], THICKNESS, style);
		// Side Support
		addBox(vc, 0.0, (scale[1]-THICKNESS)*0.5, 0.0, scale[0], THICKNESS, scale[2], style);
		// Drawer
		addBox(vc, 0.0, -scale[1]*0.25, 0.0, scale[0], scale[1]*0.5, scale[2], style);

		return vc;
	}

	public static VisChain createCarModel(double[] scale, Color color){
		VisChain vc = new VisChain();
		VzMesh.Style style = new VzMesh.Style(color);
		VzMesh.Style black = new VzMesh.Style(Color.black);

		// Body
		addBox(vc, 0.0, 0.0, -scale[2]*0.10, scale[0], scale[1]*0.95, scale[2]*0.4, style);
		addBox(vc, 0.0, 0.0,  scale[2]*0.30, scale[0]*0.5, scale[1]*0.95, scale[2]*0.4, style);

		// Wheels
		final double THICKNESS = 0.2;
		double wheel_dX = scale[0]*0.25;
		double wheel_dY = (scale[1] - THICKNESS)/2;
		double wheel_z  = -scale[2]*0.2;
		vc.add(new VisChain(LinAlg.translate( wheel_dX,  wheel_dY, wheel_z), LinAlg.rotateX(Math.PI*0.5), new VzCylinder(scale[2]*0.2, THICKNESS, black)));
		vc.add(new VisChain(LinAlg.translate( wheel_dX, -wheel_dY, wheel_z), LinAlg.rotateX(Math.PI*0.5), new VzCylinder(scale[2]*0.2, THICKNESS, black)));
		vc.add(new VisChain(LinAlg.translate(-wheel_dX,  wheel_dY, wheel_z), LinAlg.rotateX(Math.PI*0.5), new VzCylinder(scale[2]*0.2, THICKNESS, black)));
		vc.add(new VisChain(LinAlg.translate(-wheel_dX, -wheel_dY, wheel_z), LinAlg.rotateX(Math.PI*0.5), new VzCylinder(scale[2]*0.2, THICKNESS, black)));

		return vc;
	}

	public static VisChain createTruckModel(double[] scale, Color color){
		VisChain vc = new VisChain();
		VzMesh.Style style = new VzMesh.Style(color);
		VzMesh.Style black = new VzMesh.Style(Color.black);

		// Body
		addBox(vc, 0.0, 0.0, -scale[2]*0.10, scale[0], scale[1]*0.95, scale[2]*0.4, style);
		addBox(vc, scale[0]*0.1, 0.0,  scale[2]*0.30, scale[0]*0.3, scale[1]*0.95, scale[2]*0.4, style);

		// Wheels
		final double THICKNESS = 0.2;
		double wheel_dX = scale[0]*0.30;
		double wheel_dY = (scale[1] - THICKNESS)/2;
		double wheel_z  = -scale[2]*0.2;
		vc.add(new VisChain(LinAlg.translate( wheel_dX,  wheel_dY, wheel_z), LinAlg.rotateX(Math.PI*0.5), new VzCylinder(scale[2]*0.2, THICKNESS, black)));
		vc.add(new VisChain(LinAlg.translate( wheel_dX, -wheel_dY, wheel_z), LinAlg.rotateX(Math.PI*0.5), new VzCylinder(scale[2]*0.2, THICKNESS, black)));
		vc.add(new VisChain(LinAlg.translate(-wheel_dX,  wheel_dY, wheel_z), LinAlg.rotateX(Math.PI*0.5), new VzCylinder(scale[2]*0.2, THICKNESS, black)));
		vc.add(new VisChain(LinAlg.translate(-wheel_dX, -wheel_dY, wheel_z), LinAlg.rotateX(Math.PI*0.5), new VzCylinder(scale[2]*0.2, THICKNESS, black)));

		return vc;
	}

	public static VisChain createExtinguisherModel(double[] scale, Color color){
		VisChain vc = new VisChain();
		VzMesh.Style style = new VzMesh.Style(color);
		VzMesh.Style black = new VzMesh.Style(Color.black);

		// Body
		addCyl(vc, 0.0, 0.0, -scale[2]*0.10, scale[0]*0.4, scale[2]*0.8, style); 
		// Top cylinder
		addCyl(vc, 0.0, 0.0, scale[2]*0.40, 0.05, scale[2]*0.2, black);
		// Side Box
		addBox(vc, -scale[0]*0.5+0.10, 0.0, scale[2]*0.2, 0.15, 0.05, scale[2]*0.6, black);

		return vc;
	}

	public static VisChain createCartonModel(double[] scale, Color color){
		VisChain vc = new VisChain();
		VzMesh.Style style = new VzMesh.Style(color);

		System.out.println("CARTON!");

		vc.add(LinAlg.scale(scale[0], scale[1], scale[2]));

		final double TOP_H = 0.3;
		final double BOT_H = 1.0 - TOP_H;
		// Top Triangular Prism
		vc.add(new VisChain(LinAlg.translate(0.0, 0.0, 0.5 - TOP_H/2), LinAlg.scale(1.0, 1.0, TOP_H), new VzTriangularPrism(style)));
		// Bottom Box
		vc.add(new VisChain(LinAlg.translate(0.0, 0.0, -0.5 + BOT_H/2), new VzBox(1.0, 1.0, BOT_H, style)));

		return vc;
	}

	public static VisChain createComputerModel(double[] scale, Color color){
		VisChain vc = new VisChain();
		VzMesh.Style style = new VzMesh.Style(color);

		vc.add(LinAlg.scale(scale[0], scale[1], scale[2]));

		// PC Tower
		addBox(vc, 0.0, -0.4, 0.0, 1.0, 0.2, 1.0, style);

		// Monitor Base
		addBox(vc, -0.2,  0.15, -0.45, 0.4, 0.4, 0.1, style);

		// Monitor Stand
		addBox(vc, -0.3,  0.15, -0.2, 0.1, 0.1, 0.6, style);

		// Monitor Screen
		addBox(vc, -0.2,  0.15, 0.1, 0.1, 0.7, 0.8, style);

		// Keyboard
		addBox(vc, 0.3, 0.15, -0.45, 0.4, 0.6, 0.1, style);

		return vc;
	}

}
