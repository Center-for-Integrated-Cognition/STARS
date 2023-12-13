package april.sim;

/** Implement this interface to support more varied laser movements for
 *  range checks. For example, instead of just spinning around the Z-axis,
 *  perhaps your laser traces out a sinusoid in pitch as it spins.
 **/
public interface RayGenerator
{
    public int numRanges();

    /** Get a ray based on the index of the laser beam we want to generate. */
    public double[] getRay(int i);
}
