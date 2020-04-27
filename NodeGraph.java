package sim.app.geo.urbanSim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.planargraph.DirectedEdgeStar;
import com.vividsolutions.jts.planargraph.Node;
import sim.util.geo.AttributeValue;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.MasonGeometry;

public class NodeGraph extends Node
{
	

	public NodeGraph(Coordinate pt) {super(pt);}
	
	int nodeID;
	public int district;
	double bC;
	public boolean gateway;
	boolean centroid;
	public MasonGeometry masonGeometry;
	public EdgeGraph primalEdge;
	public double centrality;
		
	public List<Integer> visible2d = new ArrayList<Integer>();
	public List<Integer> localLandmarks = new ArrayList<Integer>();
	public List<Double> localScores = new ArrayList<Double>();
	public List<Integer> distantLandmarks  = new ArrayList<Integer>();
	public List<Double> distantScores = new ArrayList<Double>();
	public List<Integer> anchors = new ArrayList<Integer>();
	public List<Double> distances = new ArrayList<Double>();
	public ArrayList<Integer> adjacentRegions = new ArrayList<Integer>();
	public ArrayList<NodeGraph> adjacentEntries = new ArrayList<NodeGraph>();
	
    public void setID(int nodeID)
    {
        this.nodeID = nodeID;
    }
    
    public Integer getID()
    {
        return this.nodeID;
    }
	
    
	
    private Map<String, AttributeValue> attributes;
    public Object getAttribute(final String name)
    {
        return this.attributes.get(name);
    }

    public Integer getIntegerAttribute(final String name)
    {
        return this.attributes.get(name).getInteger();
    }
    
    

    public Double getDoubleAttribute(final String name)
    {
        return this.attributes.get(name).getDouble();
    }

    public String getStringAttribute(final String name)
    {
        return this.attributes.get(name).getString();
    }
    
    
    public void setLandmarkness(MasonGeometry geometry)
    {
	    String localString = geometry.getStringAttribute("loc_land");
		String lScoresString = geometry.getStringAttribute("loc_scor");
		String distantString = geometry.getStringAttribute("dist_land");
		String dScoresString = geometry.getStringAttribute("dist_scor");
		String anchorsString = geometry.getStringAttribute("anchors");
		String distancesString = geometry.getStringAttribute("distances");

		if (!localString.equals("[]"))
		{
			String l = localString.replaceAll("[^-?0-9]+", " ");
			String s = lScoresString.replaceAll("[^0-9.]+", " ");
	    	for(String t : (Arrays.asList(l.trim().split(" ")))) this.localLandmarks.add(Integer.valueOf(t));
	    	for(String t : (Arrays.asList(s.trim().split(" ")))) this.localScores.add(Double.valueOf(t));
		}
		else
		{
			this.localLandmarks = null;
			this.localScores = null;
		}
		
		if (!distantString.equals("[]"))
		{
			String l = distantString.replaceAll("[^-?0-9]+", " ");
			String s = dScoresString.replaceAll("[^0-9.]+", " ");
			for(String t : (Arrays.asList(l.trim().split(" ")))) this.distantLandmarks.add(Integer.valueOf(t));
			for(String t : (Arrays.asList(s.trim().split(" ")))) this.distantScores.add(Double.valueOf(t));
		}
		else
		{
			this.distantLandmarks = null;
			this.distantScores = null;
		}
	    	
		if (!anchorsString.equals("[]"))
		{
			String l = anchorsString.replaceAll("[^-?0-9]+", " ");
			String d = distancesString.replaceAll("[^0-9.]+", " ");
			for(String t : (Arrays.asList(l.trim().split(" ")))) this.anchors.add(Integer.valueOf(t));
			for(String t : (Arrays.asList(d.trim().split(" ")))) this.distances.add(Double.valueOf(t));
		}
		else this.anchors = null;  
    }
    
    
    public EdgeGraph getEdgeBetween(NodeGraph v)
    {
    	EdgeGraph edge = null;
		Collection connectingEdge = NodeGraph.getEdgesBetween(this, v);
		for (Object o : connectingEdge) edge = (EdgeGraph) o;
		return edge;
    }
    
    public ArrayList<EdgeGraph> getEdgesNode()
    {
    	ArrayList<EdgeGraph> edges = new ArrayList<EdgeGraph>();
    	List out = this.getOutEdges().getEdges();
    	Set outEdges = new HashSet(out);
    	for (Object o : outEdges)
    	{
    		EdgeGraph edge = (EdgeGraph) ((GeomPlanarGraphDirectedEdge) o).getEdge();
    		edges.add(edge);
    	}
    	return edges;
    }
    
    public ArrayList<NodeGraph> getOppositeNodes()
    {
    	ArrayList<NodeGraph> oppositeNodes = new ArrayList<NodeGraph>();
    	ArrayList<EdgeGraph> edges = getEdgesNode();
    	
        for (EdgeGraph e : edges)
        {
           NodeGraph opposite = (NodeGraph) e.getOppositeNode(this);
           oppositeNodes.add(opposite);
        }
           
    	return oppositeNodes;
    }
    
    public ArrayList<Integer> getAdjacentRegion()
    {
    	if (!this.gateway) return null;
    	
    	ArrayList<NodeGraph> oppositeNodes = new ArrayList<NodeGraph>(this.getOppositeNodes());
    	ArrayList<Integer> adjacentRegions = new ArrayList<Integer>();
    	
        for (NodeGraph opposite : oppositeNodes) 
        {
           int region = opposite.district; 
           if (region != this.district) 
           {
        	   adjacentRegions.add(region);
        	   adjacentEntries.add(opposite);
           }
        }
           
    	return adjacentRegions;
    }
    
    
    public NodeGraph getDualNode(NodeGraph originNode, NodeGraph destinationNode, boolean regionalRouting)
    {	
    	NodeGraph dualNode = null;
		double deviation = Double.MAX_VALUE;
		NodeGraph best = null;
		
		double destinationAngle = Angle.angle(originNode, destinationNode);
		double originAngle = Angle.angle(destinationNode, originNode);

		
	 	for (EdgeGraph edge : this.getEdgesNode())
	 	{
	 		if (edge.district == 999999 && regionalRouting) continue; // bridge between regions
	 		dualNode = edge.getDual();
	 		if (dualNode == null) continue;
	 		if (this != destinationNode)
	 		{
		 		double dualNodeAngle = Angle.angle(dualNode, destinationNode);
		 		double cost = Angle.differenceAngles(dualNodeAngle, destinationAngle); 
		 		if (cost < deviation)
		 		{
		 			deviation = cost;
		 			best = dualNode;
		 		}
	 		}
	 		else 
	 		{
		 		double dualNodeAngle = Angle.angle(dualNode, originNode);
		 		double cost = Angle.differenceAngles(dualNodeAngle, originAngle); 
		 		if (cost < deviation)
		 		{
		 			deviation = cost;
		 			best = dualNode;
		 		}
	 		}
	 	}
	 	dualNode = best;
	 	return dualNode;
    }
    
    public ArrayList<NodeGraph> getAdjacentNodes()
    {	
		DirectedEdgeStar startingEdges =  this.getOutEdges();
		ArrayList<NodeGraph> adjacentNodes = new ArrayList<NodeGraph>();
	 	for (Object o : startingEdges.getEdges())
	 	{
	 		GeomPlanarGraphDirectedEdge dEdge = (GeomPlanarGraphDirectedEdge) o;
	 		EdgeGraph edge = (EdgeGraph) dEdge.getEdge();
	 		NodeGraph otherNode = edge.getOtherNode(this);
	 		adjacentNodes.add(otherNode);
	 	}
	 	return adjacentNodes;
    }	
    
}

