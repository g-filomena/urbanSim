package sim.app.geo.urbanSim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.stream.Collectors;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.planargraph.DirectedEdge;


import sim.field.geo.GeomVectorField;
import sim.field.network.Network;
import sim.util.Bag;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.MasonGeometry;



/** A JTS PlanarGraph
 *
 * Planar graph useful for exploiting network topology.
 *
 * @see sim.app.geo.networkworld and sim.app.geo.campusworld
 *
 */
public class Graph extends GeomPlanarGraph
{
	public ArrayList<EdgeGraph> edges = new ArrayList<EdgeGraph>();
    static LinkedHashMap<NodeGraph, Double> centralityMap = new LinkedHashMap<NodeGraph, Double>(); 
	public Graph()
    {
        super();
    }

	

    /** populate network with lines from a GeomVectorField
     *
     * @param field containing line segments
     *
     * Assumes that 'field' contains co-planar linear objects
     *
     */
    public void fromGeomField(GeomVectorField field)
    {
        Bag geometries = field.getGeometries();

        for (int i = 0; i < geometries.numObjs; i++)
        {
            if (((MasonGeometry) geometries.get(i)).geometry instanceof LineString)
            {
                this.addLineString((MasonGeometry)geometries.get(i));
            }
        }
        
        

    }  
    
    private void addLineString(MasonGeometry wrappedLine)
    {
        LineString line = (LineString) wrappedLine.geometry;
        if (line.isEmpty()) return;

        Coordinate[] coords = CoordinateArrays.removeRepeatedPoints(line.getCoordinates());
        if (coords.length < 2) return;

        Coordinate uCoord = coords[0];
        Coordinate vCoord = coords[coords.length - 1];
        NodeGraph u = getNode(uCoord);
        NodeGraph v = getNode(vCoord);

        EdgeGraph edge = new EdgeGraph(line);
        GeomPlanarGraphDirectedEdge de0 = new GeomPlanarGraphDirectedEdge(u, v, coords[1], true);
        GeomPlanarGraphDirectedEdge de1 = new GeomPlanarGraphDirectedEdge(v, u, coords[coords.length - 2], false);

        edge.setDirectedEdges(de0, de1);
        edge.setAttributes(wrappedLine.getAttributes());
        edge.setNodes(u, v);
        edge.masonGeometry = wrappedLine;
        add(edge);
        edges.add(edge);
    }
    

    
    /** get the node corresponding to the coordinate
     *
     * @param startPt
     * @return graph node associated with point
     *
     * Will create a new Node if one does not exist.
     *
     * @note Some code copied from JTS PolygonizeGraph.getNode() and hacked to fit
     */
    public NodeGraph getNode(Coordinate pt)
    {
        NodeGraph node = (NodeGraph) findNode(pt);
        if (node == null)
        {
            node = new NodeGraph(pt);
            // ensure node is only added once to graph
            add(node);
        }
        return node;
    }

    public Network getNetwork()
    {
        Network network = new Network(false); // false == not directed

        for ( Object object : getEdges() )
        {
            DirectedEdge edge = (DirectedEdge) object;
            network.addEdge(edge.getFromNode(), edge.getToNode(), edge);
        }
        return network;
    }
    
    public NodeGraph findNode(Coordinate pt)
    {
      return (NodeGraph) nodeMap.find(pt);
    }
    

    public void generateCentralityMap()
    {
    	LinkedHashMap<NodeGraph, Double> centralityMap = new LinkedHashMap<NodeGraph, Double>(); 
    	for (Object o: this.nodeMap.values())
    		{
    			NodeGraph node = (NodeGraph) o;
    			centralityMap.put(node, node.centrality);
    		}
    	Graph.centralityMap = (LinkedHashMap<NodeGraph, Double>) utilities.sortByValue(centralityMap);
    }
 
    
    public ArrayList<NodeGraph> salientNodes(NodeGraph originNode, NodeGraph destinationNode, double distance, double percentile, String mode)
	{
    	
    	Geometry buffer;
	   	if (destinationNode != null) buffer = utilities.smallestEnclosingCircle(originNode, destinationNode);
    	else buffer = originNode.masonGeometry.geometry.buffer(distance);
	    
	   	ArrayList<NodeGraph> containedNodes = new ArrayList<NodeGraph>();
	    containedNodes = this.getContainedNodes(buffer); 	    
	    LinkedHashMap<NodeGraph, Double> SpatialfilteredMap =  filterCentralityMap(centralityMap, containedNodes);
	    if ((SpatialfilteredMap.size() == 0) || (SpatialfilteredMap == null)) return null;

	    int position;
	    double min_value = 0.0;
	    
	    // global quantile
	    if (mode == "global")
    	{
	    	position = (int) (centralityMap.size()*percentile);
	    	min_value = (new ArrayList<Double>(centralityMap.values())).get(position);
    	}
	    else
    	{
	    	position = (int) (SpatialfilteredMap.size()*percentile);
	    	min_value = (new ArrayList<Double>(SpatialfilteredMap.values())).get(position);
    	}
	    double boundary = min_value;
	    Map<NodeGraph, Double> valueFilteredMap = SpatialfilteredMap.entrySet().stream()
	    	      .filter(entry -> entry.getValue() >= boundary)
	    	      .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
	    
	    if ((valueFilteredMap.size() == 0) || (valueFilteredMap == null)) return null;
	    ArrayList<NodeGraph> result = new ArrayList<>(valueFilteredMap.keySet());
	    return result;	
	}
    

    public ArrayList<NodeGraph> getContainedNodes(Geometry g)
    {
    	ArrayList<NodeGraph> containedNodes = new ArrayList<NodeGraph>(); 
        for (Object o: this.nodeMap.values())
		{
        	Geometry geoNode = ((NodeGraph) o).masonGeometry.getGeometry();
			if (g.contains(geoNode)) containedNodes.add((NodeGraph) o);
		}
        return containedNodes;
	}
    
    public ArrayList<EdgeGraph> getContainedEdges(Geometry g)
    {
    	ArrayList<EdgeGraph> containedEdges = new ArrayList<EdgeGraph>(); 
        for (Object o: this.edges)
		{
        	Geometry geoEdge = ((EdgeGraph) o).masonGeometry.geometry;
			if (g.contains(geoEdge)) containedEdges.add((EdgeGraph) o);
		}
        return containedEdges;
	}
    
    public ArrayList<EdgeGraph> getEdges()
    {
        return this.edges;
	}

	public static LinkedHashMap<NodeGraph, Double> filterCentralityMap(LinkedHashMap<NodeGraph, Double> map, ArrayList<NodeGraph> filter)
	{	
		LinkedHashMap<NodeGraph, Double> mapFiltered = new LinkedHashMap<NodeGraph, Double> (map);	  	
	  	ArrayList<NodeGraph> result = new ArrayList<NodeGraph>();
	  	for(NodeGraph key : mapFiltered.keySet()) {if(filter.contains(key)) result.add(key);}
	  	mapFiltered.keySet().retainAll(result);
		return mapFiltered;
	}
	
	
	public ArrayList<EdgeGraph> edgesWithinSpace(NodeGraph originNode, NodeGraph destinationNode)
	{	
		Double radius = utilities.nodesDistance(originNode,  destinationNode)*0.25;
		if (radius < 500) radius = 500.0;
		Geometry bufferOrigin = originNode.masonGeometry.geometry.buffer(radius);
		Geometry bufferDestination = destinationNode.masonGeometry.geometry.buffer(500);
		Geometry convexHull = bufferOrigin.union(bufferDestination).convexHull();
		
		ArrayList<EdgeGraph> containedEdges = this.getContainedEdges(convexHull);
		return containedEdges;
	}
					
}












