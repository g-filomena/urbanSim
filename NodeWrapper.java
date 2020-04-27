package sim.app.geo.urbanSim;

import sim.util.geo.GeomPlanarGraphDirectedEdge;

public class NodeWrapper 
{
	public NodeGraph node;
	public NodeGraph nodeFrom;
    public GeomPlanarGraphDirectedEdge edgeFrom;
    public NodeGraph commonPrimalJunction;
    public double gx, hx, fx, landmarkness;
    public int nodesSoFar;
    public double pathCost, nodeLandmarkness, pathLandmarkness;
    
    public NodeWrapper(NodeGraph n)
    {
        node = n;
        gx = 0;
        hx = 0;
        fx = 0;
        nodeFrom = null;
        edgeFrom = null;
        commonPrimalJunction = null;
        pathCost = 0.0;
        nodeLandmarkness = 0.0;
        pathLandmarkness = 0.0;
    }
}


    
