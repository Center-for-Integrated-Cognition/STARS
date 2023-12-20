package april.graph;

public interface GraphSolver
{
    /** Perform one iteration/step **/
    public void iterate();

    /** Return false if no more iteration can be performed (such as an
     * EKF that has already processed all the edges once. **/
    public boolean canIterate();
}
