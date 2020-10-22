package sim.app.geo.urbanSim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import sim.app.geo.pedSimCity.PedSimCity;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;

public class NodesLookup {
	
	
	// random node from whole set of nodes
    public static NodeGraph randomNode(Bag geometriesNodes, Graph network)
    {
		Random random = new Random();
		Integer c = random.nextInt(geometriesNodes.size());
    	MasonGeometry geoNode = (MasonGeometry) geometriesNodes.objs[c]; 
    	return network.findNode(geoNode.geometry.getCoordinate());
    }
    
    // look for a random node outside a given district, within a certain radius from a given node.
    static NodeGraph nodeOutsideDistrict(NodeGraph originNode, double radius, Graph network)
    {
    	
    	VectorLayer junctionsWithin = new VectorLayer();
    	MasonGeometry nodeGeometry = originNode.masonGeometry;
		Random random = new Random();
		
    	Bag filterSpatial = null;
    	Bag filterByDistrict = null;
    	NodeGraph n = null;
    	double expanding_radius = radius;
    	while (n == null)
    	{
    		if (expanding_radius >= radius * 2.00) return null;
    		filterSpatial = PedSimCity.junctions.getObjectsWithinDistance(nodeGeometry, expanding_radius);
    		if (filterSpatial.size() < 1) 
    		{
    			expanding_radius = expanding_radius *1.10;	
    			continue;
     	  	
    		}
    		for (Object o : filterSpatial)
     	    {
     	    	MasonGeometry geoNode = (MasonGeometry) o;
     	    	junctionsWithin.addGeometry(geoNode);
     	    }
     	  	
    		if (junctionsWithin.getGeometries().size() == 0)
			{
    			expanding_radius = expanding_radius *1.10;	
    			continue;
			}
     	  	
    		filterByDistrict = junctionsWithin.filter("region", originNode.region, "different");
    		if (filterByDistrict.size() == 0)
			{
    			expanding_radius = expanding_radius *1.10;	
    			continue;
			}

			Integer c = random.nextInt(filterByDistrict.size());
	 	  	MasonGeometry geoNode = (MasonGeometry) filterByDistrict.objs[c];
	 	  	n = network.findNode(geoNode.geometry.getCoordinate());	 	  	
//	 	  	if (!PedestrianSimulation.startingNodes.contains(n.masonGeometry)) n = null;
	 	  	expanding_radius = expanding_radius *1.10;
 	  	}
 	  	return n;
    }
    
    
    public static NodeGraph outsideDistrictByCentrality(NodeGraph originNode, double distance)
    {
    	double tolerance = 0.10;
    	NodeGraph destinationNode = null;
		Random random = new Random();
		
		while (destinationNode == null)
		{
			if (tolerance >= 0.50) return null;
			double lowL = distance - distance*tolerance;
	 	    double uppL = distance + distance*tolerance;
	 	    
			ArrayList<NodeGraph> possibleDestinations = PedSimCity.network.salientNodesBewteenSpace(originNode, null, lowL, uppL, 0.75,
					"global");
			
			if (possibleDestinations == null)
			{
				tolerance += 0.10;
				continue;
			}
			
			possibleDestinations = (ArrayList<NodeGraph>) possibleDestinations.stream().
					filter(node -> node.region != originNode.region).collect(Collectors.toList());
			if ((possibleDestinations == null) || (possibleDestinations.size() == 0))
			{
				tolerance += 0.10;
				continue;
			}
			Integer c = random.nextInt(possibleDestinations.size());
			destinationNode = possibleDestinations.get(c);
		}
		return destinationNode;
    }
		
			      
    
    public static NodeGraph nodeWithinFromDistances(NodeGraph originNode, VectorLayer nodeGeometries, List<Float> distances, Graph network)
    {
    	Random random = new Random();
    	int pD = random.nextInt(distances.size());
    	Float distance = distances.get(pD);
    	NodeGraph node = null;
    	ArrayList <NodeGraph> candidates = null;
    	double tolerance = 50;
    	
    	while(true)
    	{
	 	  	double lowL = distance - tolerance;
	 	    double uppL = distance + tolerance;
	 	    candidates = network.getWithinNodes(originNode, lowL, uppL);	 
	 	  	if (candidates.size() > 1) break;
	 	  	else tolerance += 50;
    	}
    	
    	while (node == null || (node.getID() == originNode.getID()))
    	{
			Integer c = random.nextInt(candidates.size());
			node = candidates.get(c);
	 	  	if (originNode.getEdgeBetween(node) != null) node = null;
    	}
 	  	return node;
    }
    
    public static NodeGraph nodeWithinDistance(NodeGraph originNode, double lowL, double uppL, Graph network)
    {	    
    	Random random = new Random();
    	ArrayList <NodeGraph> candidates = network.getWithinNodes(originNode, lowL, uppL);	
 	  	int c = random.nextInt(candidates.size());
 	  	NodeGraph node = candidates.get(c);
 	  	return node;
    }
    
    public static NodeGraph nodeWithinDistanceOtherDistricts(NodeGraph originNode, double lowL, double uppL, Graph network)
    {	    
    	Random random = new Random();
    	ArrayList <NodeGraph> candidates = network.getWithinNodesOtherRegions(originNode, lowL, uppL);	
 	  	int c = random.nextInt(candidates.size());
 	  	NodeGraph node = candidates.get(c);
 	  	return node;
    }
    
//    static NodeGraph nodeDistributionWithin(Geometry geo, VectorLayer vectorField,  
//    		HashMap<MasonGeometry, Double> nodesMap, double lowL, double uppL, Graph network)
//    {
//    	Random random = new Random();
//		double p = random.nextFloat();
//	  	MasonGeometry  randomNode = null;
//        HashMap<MasonGeometry, Double> nodesTmpMap  = new HashMap<MasonGeometry, Double> (nodesMap);	  	
//        ArrayList<MasonGeometry> result = new ArrayList<MasonGeometry>();
//        
//	  	Bag filter = vectorField.getWithinObjects(geo, lowL, uppL);	  
//        for(MasonGeometry key : nodesTmpMap.keySet()) {if(filter.contains(key)) {result.add(key);}}
//        
//        nodesTmpMap.keySet().retainAll(result);
//        double sumMetric =  nodesTmpMap.values().stream().mapToDouble(d->d).sum();
//        
//        // computing probabilities
//        for (MasonGeometry key : nodesTmpMap.keySet()) TODO use entry set
//        {
//        	double rc = nodesTmpMap.get(key);
//        	double probRC = rc/sumMetric;
//        	nodesTmpMap.put(key, probRC);
//        }
//       
//        Map orderedNodes = Utilities.sortByValue(nodesTmpMap, "ascending"); 	
// 	  	Iterator it = orderedNodes.entrySet().iterator();
// 	  	
// 	  	while (it.hasNext())  TODO remove iterator
// 	  	{
// 	  	   double cumulative = 0.00;
// 	       Map.Entry<MasonGeometry, Double> entry = (Map.Entry<MasonGeometry, Double>)it.next();
// 	       double probNode = entry.getValue();
// 	       cumulative += probNode;
// 	        if (cumulative < p) continue;
// 	        if (cumulative >= p) 
//        	{
//	        	randomNode = entry.getKey();
//	        	break;
//        	}
// 	    }
// 	  	return network.findNode(randomNode.geometry.getCoordinate());
//    }
    
   
//    NodeGraph nodeFromDistribution(PedestrianSimulation state, HashMap<MasonGeometry, Double> nodesMap, Graph network)
//    {
//    	Random random = new Random();
//    	double p = random.nextFloat();
//    	double cumulative = 0.00;
// 	  	MasonGeometry randomNode = null; 	
//        Map orderedNodes = utilities.sortByValue(nodesMap); 	
// 	  	Iterator it = orderedNodes.entrySet().iterator();
//
// 	  	while (it.hasNext()) 
// 	  	{
// 	  		Map.Entry<MasonGeometry, Double> entry = (Map.Entry<MasonGeometry, Double>) it.next();
// 	  		double probNode = entry.getValue();
// 	  		cumulative += probNode;
// 	        if (cumulative < p) continue;
// 	        if (cumulative >= p) 
// 	        {
// 	        	randomNode = entry.getKey();
// 	        	break;
// 	        }
// 	    }
// 	  	return network.findNode(randomNode.geometry.getCoordinate());
//    }

	
		
    
    
    
    
}
