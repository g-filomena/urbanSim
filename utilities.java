package sim.app.geo.urbanSim;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.vividsolutions.jts.geom.*;

import sim.util.Bag;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.MasonGeometry;
import sim.field.geo.GeomVectorField;

public class utilities {	
	

	// sort map
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) 
	{
	    return map.entrySet()
	              .stream()
	              .sorted(Map.Entry.comparingByValue(/*Collections.reverseOrder()*/))
	              .collect(Collectors.toMap(
	                Map.Entry::getKey, 
	                Map.Entry::getValue, 
	                (e1, e2) -> e1, 
	                LinkedHashMap::new
	              ));
	}

	public static double euclideanDistance(Coordinate originCoord, Coordinate destinationCoord)
	{
	    return Math.sqrt(Math.pow(originCoord.x - destinationCoord.x, 2)
	            + Math.pow(originCoord.y - destinationCoord.y, 2));
	}
    		
	public static double nodesDistance(NodeGraph originNode, NodeGraph destinationNode)
    {
        Coordinate originCoord = originNode.getCoordinate();
        Coordinate destinationCoord = destinationNode.getCoordinate();
        return 	euclideanDistance(originCoord, destinationCoord);

    }
    
    public static double finalLengthRoute(ArrayList<GeomPlanarGraphDirectedEdge> route)
    {
    	double distance = 0;
    
    	for (int i = 0; i < route.size(); i++)
    	{
    		GeomPlanarGraphDirectedEdge edge = route.get(i);
    		EdgeGraph d = (EdgeGraph) edge.getEdge();
    		distance += d.getLine().getLength();
    	}
		return distance;
    }
    
      
	public static LineString LineStringBetweenNodes(NodeGraph nodeA, NodeGraph nodeB)
	{
		Coordinate[] coords = {nodeA.getCoordinate(), nodeB.getCoordinate()};
		LineString line = new GeometryFactory().createLineString(coords);
		return line;
	}
    
    
	public static Geometry smallestEnclosingCircle(NodeGraph nodeA, NodeGraph nodeB)
	{			
		LineString line = LineStringBetweenNodes(nodeA, nodeB);
		Point centroid = line.getCentroid();
		Geometry buffer = centroid.buffer(line.getLength()/2);
		return buffer;
	}
    
	public static HashMap<MasonGeometry, Double> filterMap(HashMap<MasonGeometry, Double> map, Bag filter)
	{	
	
	  	HashMap<MasonGeometry, Double> mapFiltered = new HashMap<MasonGeometry, Double> (map);	  	
	  	ArrayList<MasonGeometry> result = new ArrayList<MasonGeometry>();
	  	for(MasonGeometry key : mapFiltered.keySet()) {if(filter.contains(key)) result.add(key);}
	  	mapFiltered.keySet().retainAll(result);
		return mapFiltered;
	}
	

	
	public static NodeGraph commonPrimalJunction(NodeGraph cen, NodeGraph otherCen)
	{
	    EdgeGraph edge = cen.primalEdge;
	    EdgeGraph otherEdge = otherCen.primalEdge;
	    	    
	    if ((edge.u == otherEdge.u) | (edge.u == otherEdge.v)) return edge.u;
	    else return edge.v;
	}
	
	public static class Path {
		public ArrayList<GeomPlanarGraphDirectedEdge> edges;
		public HashMap<NodeGraph, NodeWrapper> mapWrappers;
	}
	
	
    public static GeomVectorField filterGeomVectorField(GeomVectorField gvf, String attributeName, Integer attributeValue, 
    		String method)
    {
    	Bag objects = new Bag();
    	Bag geometries = gvf.getGeometries();

	    for (int i = 0; i < geometries.size(); i++)
	    {
	    	MasonGeometry mg = (MasonGeometry) geometries.get(i);
	        Integer attribute = mg.getIntegerAttribute(attributeName);
	        if ((method == "different") && (!attribute.equals(attributeValue))) objects.add(mg);
	        else if ((method == "equal") && (attribute == attributeValue)) objects.add(mg);
	        else continue;
	     }
	    
    	GeomVectorField newGeomVectorField = new GeomVectorField();	 
 	    for (Object o : objects)
 	    {
 	    	MasonGeometry mg = (MasonGeometry) o;
 	    	newGeomVectorField.addGeometry(mg);
 	    }
		return newGeomVectorField;
    }
	
    public static GeomVectorField filterGeomVectorField(GeomVectorField gvf, String attributeName, String attributeValue, 
    		String method)
    {
    	Bag objects = new Bag();
    	Bag geometries = gvf.getGeometries();

	    for (int i = 0; i < geometries.size(); i++)
	    {
	    	MasonGeometry mg = (MasonGeometry) geometries.get(i);
	        String attribute = mg.getStringAttribute(attributeName);
	        if ((method == "different") && (!attribute.equals(attributeValue))) objects.add(mg);
	        else if ((method == "equal") && (attribute.equals(attributeValue))) objects.add(mg);
	        else continue;
	     }
	    
    	GeomVectorField newGeomVectorField = new GeomVectorField();	 
 	    for (Object o : objects)
 	    {
 	    	MasonGeometry mg = (MasonGeometry) o;
 	    	newGeomVectorField.addGeometry(mg);
 	    }
		return newGeomVectorField;
    }
	
	public static Bag bagsIntersection(Bag setA, Bag setB) 
	
	{
	    Bag tmp = new Bag();
	    for (Object x : setA) if (setB.contains(x)) tmp.add(x);
	    return tmp;
	}
	
	public static NodeGraph previousJunction(ArrayList<GeomPlanarGraphDirectedEdge> path)
	{
		// from global graph
		NodeGraph lastCen = ((EdgeGraph) path.get(path.size()-1).getEdge()).getDual(); 
		NodeGraph otherCen = ((EdgeGraph) path.get(path.size()-2).getEdge()).getDual();
		return commonPrimalJunction(lastCen, otherCen);
	}
	
	public static ArrayList<NodeGraph> centroidsFromPath(ArrayList<GeomPlanarGraphDirectedEdge> path)
	{
		ArrayList<NodeGraph> centroids = new ArrayList<NodeGraph> ();
		for (GeomPlanarGraphDirectedEdge e: path) centroids.add(((EdgeGraph) e.getEdge()).getDual());
		return centroids;
	}
	

	public static <K, V> K getKeyFromValue(Map<K, V> HashMap, V value) {
	    for (Entry<K, V> entry : HashMap.entrySet()) 
	    {
	        if (Objects.equals(value, entry.getValue())) return entry.getKey();
	    }
	    return null;
	}
	
	public static double fromNormalDistribution(double mean, double sd, String direction )
    {
		Random random = new Random();
		double result = random.nextGaussian()*sd+mean;
		if (direction != null)
		{
			if ((direction.equals("left")) && (result > mean)) result = mean;
			if ((direction.equals("right")) && (result < mean)) result = mean;	
		}
		if (result <= 0.00) result = mean;
		return result;
    }
	
	public static Geometry viewField(NodeGraph originNode, NodeGraph destinationNode)
	{	
		Coordinate coordA = utilities.angleViewField(originNode, destinationNode, 30);
		Coordinate coordB = utilities.angleViewField(originNode, destinationNode, -30);
		GeometryFactory geometryFactory = new GeometryFactory();
		MultiPoint points = geometryFactory.createMultiPoint(new Coordinate[]{originNode.getCoordinate(), coordA, coordB});
		Geometry viewField = points.convexHull();
		return viewField;
	}
	
	
	public static Coordinate angleViewField(NodeGraph originNode, NodeGraph destinationNode, double desired)
	{
		double angle = Angle.angle(originNode, destinationNode);
		if (angle > 360) angle = 360-angle;
		Coordinate coord = getCoordAngle(originNode, nodesDistance(originNode, destinationNode), angle+desired);
		return coord;
	}
	
	
	public static Coordinate getCoordAngle(NodeGraph originNode, double distance, double angle)
	{
		double x = distance * Math.sin(Math.toRadians(angle)) + originNode.getCoordinate().x;
		double y = distance * Math.cos(Math.toRadians(angle)) + originNode.getCoordinate().y;
	    Coordinate coord = new Coordinate(x,y);
	    return coord;
	}

} 
    
    
        

    