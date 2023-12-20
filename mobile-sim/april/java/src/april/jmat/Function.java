package april.jmat;

public abstract class Function
{
    /** Convenience function, equivalent to evaluate(x,null) **/
    public double[] evaluate(double x[])
    {
        return evaluate(x, null);
    }

    /** Evaluate the function at x, return y. If the argument y is
     * non-null, it is used to store the value y.
     **/
    public abstract double[] evaluate(double x[], double y[]);
}
