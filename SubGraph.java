package sim.app.geo.urbanSim;

import java.util.*;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.LineString;
import sim.util.geo.GeomPlanarGraphDirectedEdge;



public class SubGraph extends Graph
{
	private SubGraphNodesMap subGraphNodesMap = new SubGraphNodesMap();
	private SubGraphEdgesMap subGraphEdgesMap = new SubGraphEdgesMap();
	private ArrayList<Integer> graphBarriers = new ArrayList<Integer>();
	Graph parentGraph;
	
	public SubGraph(Graph parentGraph, ArrayList <EdgeGraph> edges) 
	{
		this.parentGraph = parentGraph;
		for (EdgeGraph edge: edges)
		{
			NodeGraph u = edge.u;
			NodeGraph v = edge.v;
			addFromOtherGraph(parentGraph, edge, u,v);
		}	
	}
	
    public SubGraph() 
    {
    }

	public void addFromOtherGraph(Graph parentGraph, EdgeGraph edge, NodeGraph u, NodeGraph v)
    {
        Coordinate uCoord = u.getCoordinate();
        Coordinate vCoord = v.getCoordinate();
        
        NodeGraph newU = this.getNode(uCoord);
        NodeGraph newV = this.getNode(vCoord);
        
        LineString line = (LineString) edge.getLine();
        Coordinate[] coords = CoordinateArrays.removeRepeatedPoints(line.getCoordinates());        
        EdgeGraph newEdge = new EdgeGraph(line);
        
        GeomPlanarGraphDirectedEdge de0 = new GeomPlanarGraphDirectedEdge(newU, newV, coords[1], true);
        GeomPlanarGraphDirectedEdge de1 = new GeomPlanarGraphDirectedEdge(newV, newU, coords[coords.length - 2], false);
        newEdge.setDirectedEdges(de0, de1);
        newEdge.setAttributes(edge.attributes);
        newEdge.setNodes(newU, newV);
        subGraphNodesMap.add(newU, u);
        subGraphNodesMap.add(newV, v);
        subGraphEdgesMap.add(newEdge, edge);
        newU.primalEdge = u.primalEdge;
        newV.primalEdge = v.primalEdge;
        add(newEdge);
        this.edgesGraph.add(newEdge);
    }
    
    public class SubGraphNodesMap
    {
    	public HashMap<NodeGraph, NodeGraph> map = new HashMap<NodeGraph, NodeGraph>();
	    public void add(NodeGraph node, NodeGraph parentNode)
	    {
	    	map.put(node, parentNode);
	    }

	    public NodeGraph findParent(NodeGraph nodeSubGraph) {return map.get(nodeSubGraph);}
	    public NodeGraph findChild(NodeGraph nodeGraph) {return Utilities.getKeyFromValue(map, nodeGraph);}
	}
    
    public class SubGraphEdgesMap
    {
    	private HashMap<EdgeGraph, EdgeGraph> map = new HashMap<EdgeGraph, EdgeGraph>();
	    public void add(EdgeGraph edge, EdgeGraph parentEdge)
	    {
	    	map.put(edge, parentEdge);
	    }

	    public EdgeGraph findParent(EdgeGraph edgeSubGraph) {return map.get(edgeSubGraph);}
	    public EdgeGraph findChild(EdgeGraph edgeSubGraph) {return Utilities.getKeyFromValue(map, edgeSubGraph);}
	}
    
    public NodeGraph getParentNode(NodeGraph nodeSubGraph)
    {
    	return subGraphNodesMap.findParent(nodeSubGraph);
    }
    
    public ArrayList<NodeGraph> getParentNodes(ArrayList<NodeGraph> childNodes)
    {
    	
    	ArrayList<NodeGraph> parentNodes = new  ArrayList<NodeGraph>();
    	for (NodeGraph child: childNodes) 
    		{
    			NodeGraph parent = this.subGraphNodesMap.findParent(child);
    			if (parent != null) parentNodes.add(parent);
    		}
    	return parentNodes;
    }
    
    public ArrayList<NodeGraph> getChildNodes(ArrayList<NodeGraph> parentNodes)
    {
    	
    	ArrayList<NodeGraph> childNodes = new  ArrayList<NodeGraph>();
    	for (NodeGraph parent: parentNodes) 
    		{
    			NodeGraph child = this.subGraphNodesMap.findChild(parent);
    			if (child != null) childNodes.add(child);
    		}
    	return childNodes;
    }

    
    public EdgeGraph getParentEdge(EdgeGraph edgeSubGraph)
    {
    	return subGraphEdgesMap.findParent(edgeSubGraph);
    }
    
    public ArrayList<EdgeGraph> getParentEdges(ArrayList<EdgeGraph> childEdges)
    {
    	ArrayList<EdgeGraph> parentEdges = new  ArrayList<EdgeGraph>();
    	for (EdgeGraph child: childEdges) 
    		{
    			EdgeGraph parent = this.subGraphEdgesMap.findParent(child);
    			if (parent != null) parentEdges.add(parent);
    		}
    	return parentEdges;
    }
    
    public void setBarriersGraph()
    {
    	ArrayList<Integer> graphBarriers = new ArrayList<Integer>();
    	for (EdgeGraph e : this.edgesGraph) 
		{
			 List<Integer> barriers = (ArrayList<Integer>) this.getParentEdge(e).barriers;
			 if (barriers == null) continue;
			 graphBarriers.addAll(barriers);
		}
    	Set<Integer> setBarriers = new HashSet<Integer>(graphBarriers);
    	this.graphBarriers = new ArrayList<Integer>(setBarriers);
    }
    
    
    public ArrayList<Integer> getBarriersGraph()
    {
    	return this.graphBarriers;
    }    
}