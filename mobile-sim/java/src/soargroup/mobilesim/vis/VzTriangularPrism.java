package soargroup.mobilesim.vis;

import java.awt.*;
import java.io.*;
import april.vis.*;

/* A triangular prism - A triangle extended in the third dimension
 * The vertices are defined within a 1x1x1 cube centered at the origin
 * Each triangular face lies in a YZ plane (one at X=0.5 and one at X=-0.5) 
 *    oriented with a flat side at the bottom and the point in the +Z direction
 *
 **/
public class VzTriangularPrism implements VisObject, VisSerializable
{
    Style styles[];

    static VzMesh mesh;

    static {
        final float sqrt2 = (float) Math.sqrt(2);

        float vd[] = new float[] { 
			// Triangular faces 1 + 2
			 0.5f,  0.5f, -0.5f,     0.5f,  0.0f,  0.5f,     0.5f, -0.5f, -0.5f,
			-0.5f, -0.5f, -0.5f,    -0.5f,  0.0f,  0.5f,    -0.5f,  0.5f, -0.5f,

			// Bottom Rectangle
			 0.5f,  0.5f, -0.5f,     0.5f, -0.5f, -0.5f,    -0.5f,  0.5f, -0.5f,
			 0.5f, -0.5f, -0.5f,    -0.5f, -0.5f, -0.5f,    -0.5f,  0.5f, -0.5f,

			// Side 1 Rectangle
			 0.5f,  0.0f,  0.5f,    -0.5f,  0.0f,  0.5f,     0.5f, -0.5f, -0.5f,
			 0.5f, -0.5f, -0.5f,    -0.5f,  0.0f,  0.5f,    -0.5f, -0.5f, -0.5f,

			 // Side 2 Rectangle
			-0.5f,  0.5f, -0.5f,    -0.5f,  0.0f,  0.5f,     0.5f,  0.0f,  0.5f,
			-0.5f,  0.5f, -0.5f,     0.5f,  0.0f,  0.5f,     0.5f,  0.5f, -0.5f,
		};

        VisVertexData fillVertices = new VisVertexData(vd, vd.length / 3, 3);

		final float cos60 = (float)Math.cos(Math.PI/3.0);
		final float sin60 = (float)Math.sin(Math.PI/3.0);

        float nd[] = new float[] { 
			// Triangular faces 1 + 2
			1.0f, 0.0f, 0.0f,    1.0f, 0.0f, 0.0f,     1.0f, 0.0f, 0.0f,
			-1.0f, 0.0f, 0.0f,   -1.0f, 0.0f, 0.0f,    -1.0f, 0.0f, 0.0f,

			// Bottom Rectangle
			0.0f, 0.0f, -1.0f,   0.0f, 0.0f, -1.0f,    0.0f, 0.0f, -1.0f,   
			0.0f, 0.0f, -1.0f,   0.0f, 0.0f, -1.0f,    0.0f, 0.0f, -1.0f,   

			// Side 1 Rectangle
			0.0f, -cos60, sin60,   0.0f, -cos60, sin60,   0.0f, -cos60, sin60,     
			0.0f, -cos60, sin60,   0.0f, -cos60, sin60,   0.0f, -cos60, sin60,     

			// Side 2 Rectangle
			0.0f,  cos60, sin60,   0.0f,  cos60, sin60,   0.0f,  cos60, sin60,     
			0.0f,  cos60, sin60,   0.0f,  cos60, sin60,   0.0f,  cos60, sin60,     
		};

        VisVertexData fillNormals = new VisVertexData(nd, nd.length / 3, 3);
        mesh = new VzMesh(fillVertices, fillNormals, VzMesh.TRIANGLES);
    }

    public VzTriangularPrism(Style ... styles) {
		this.styles = styles;
    }

    public void render(VisCanvas vc, VisLayer layer, VisCanvas.RenderInfo rinfo, GL gl, Style style)
    {
        if (style instanceof VzMesh.Style) {
            mesh.render(vc, layer, rinfo, gl, (VzMesh.Style) style);
        }
    }

    public void render(VisCanvas vc, VisLayer layer, VisCanvas.RenderInfo rinfo, GL gl)
    {
        for (Style style : styles)
            render(vc, layer, rinfo, gl, style);
    }

    public VzTriangularPrism(ObjectReader ins)
    {
    }

    public void writeObject(ObjectWriter outs) throws IOException
    {
        outs.writeInt(styles.length);
        for (int sidx = 0; sidx < styles.length; sidx++)
            outs.writeObject(styles[sidx]);
    }

    public void readObject(ObjectReader ins) throws IOException
    {
        int nstyles = ins.readInt();
        styles = new Style[nstyles];
        for (int sidx = 0; sidx < styles.length; sidx++)
            styles[sidx] = (Style) ins.readObject();

    }
}
