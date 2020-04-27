package sim.app.geo.urbanSim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import sim.app.geo.pedestrianSimulation.PedestrianSimulation;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;

public class nodesLookup {
	
	
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
    		filterSpatial = PedestrianSimulation.junctions.getObjectsWithinDistance(nodeGeometry, expanding_radius);
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
     	  	
    		filterByDistrict = junctionsWithin.filter("district", originNode.district, "different");
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
    
    
    public static NodeGraph outsideDistrictByCentrality(NodeGraph originNode, double radius)
    {
    	
    	NodeGraph destinationNode = null;
    	double expanding_radius = radius;
		Random random = new Random();
		while (destinationNode == null)
		{
			if (expanding_radius >= radius * 2.00) return null;
//			ArrayList<NodeGraph> possibleDestinations = PedestrianSimulation.network.salientNodes(originNode, null, expanding_radius, 0.99, "global");
//			if (possibleDestinations == null)
//			{
//
//				expanding_radius = expanding_radius *1.10;
//				continue;
//			}
			
//			ArrayList<NodeGraph> possibleDestinations = new ArrayList<>(PedestrianSimulation.network.centralityMap.keySet());
			ArrayList<NodeGraph> possibleDestinations = new ArrayList<>();
			int[] pb = {38817, 42331, 44569,33699, 24656};
			for (int i : pb) possibleDestinations.add(PedestrianSimulation.nodesMap.get(i));
//			possibleDestinations = new ArrayList<NodeGraph>(possibleDestinations.subList(possibleDestinations.size()-9, possibleDestinations.size()));
			possibleDestinations = (ArrayList<NodeGraph>) possibleDestinations.stream().
					filter(node -> node.district != originNode.district).collect(Collectors.toList());
			
			if ((possibleDestinations == null) || (possibleDestinations.size() == 0))
			{
				expanding_radius = expanding_radius *1.10;
				continue;
			}
			Integer c = random.nextInt(possibleDestinations.size());
			destinationNode = possibleDestinations.get(c);
		}
		return destinationNode;
    }
		
			      
    
    public static NodeGraph nodeWithin(NodeGraph originNode, VectorLayer vectorField, List<Float> distances, Graph network)
    {
		GeometryFactory fact = new GeometryFactory();
    	MasonGeometry nodeLocation = new MasonGeometry(fact.createPoint(new Coordinate(originNode.getCoordinate())));
    	Geometry geo = nodeLocation.geometry;
    	Random random = new Random();
    	int pD = random.nextInt(distances.size());
    	Float distance = distances.get(pD);
    	NodeGraph node = null;
    	Bag filter = null;
    	double range = 5;
    	
    	while(true)
    	{
	 	  	double lowL = distance - distance*range;
	 	    double uppL = distance + distance*range;
	 	  	filter = vectorField.getWithinObjects(geo, lowL, uppL);	 
	 	  	if (filter.size() > 1) break;
	 	  	else range = range + 5;
    	}
    	
    	MasonGeometry geoNode = null;
    	
    	while (geoNode == null || (node.getID() == originNode.getID()))
    	{
			Integer c = random.nextInt(filter.size());
	 	  	geoNode = (MasonGeometry) filter.objs[c];
	 	  	node = network.findNode(geoNode.geometry.getCoordinate());
    	}
 	  	return node;
    }
    
    static NodeGraph nodeWithin(Geometry geo, VectorLayer vectorField, double lowL, double uppL, Graph network)
    {	    
    	Random random = new Random();
 	  	Bag filter = vectorField.getWithinObjects(geo, lowL, uppL);	  	
 	  	int c = random.nextInt(filter.size());
 	  	MasonGeometry geoNode = (MasonGeometry) filter.objs[c];
 	  	return network.findNode(geoNode.geometry.getCoordinate());
    }
    
    static NodeGraph nodeDistributionWithin(Geometry geo, VectorLayer vectorField,  
    		HashMap<MasonGeometry, Double> nodesMap, double lowL, double uppL, Graph network)
    {
    	Random random = new Random();
		double p = random.nextFloat();
	  	MasonGeometry  randomNode = null;
        HashMap<MasonGeometry, Double> nodesTmpMap  = new HashMap<MasonGeometry, Double> (nodesMap);	  	
        ArrayList<MasonGeometry> result = new ArrayList<MasonGeometry>();
        
	  	Bag filter = vectorField.getWithinObjects(geo, lowL, uppL);	  
        for(MasonGeometry key : nodesTmpMap.keySet()) {if(filter.contains(key)) {result.add(key);}}
        
        nodesTmpMap.keySet().retainAll(result);
        double sumMetric =  nodesTmpMap.values().stream().mapToDouble(d->d).sum();
        
        // computing probabilities
        for (MasonGeometry key : nodesTmpMap.keySet())
        {
        	double rc = nodesTmpMap.get(key);
        	double probRC = rc/sumMetric;
        	nodesTmpMap.put(key, probRC);
        }
       
        Map orderedNodes = utilities.sortByValue(nodesTmpMap); 	
 	  	Iterator it = orderedNodes.entrySet().iterator();
 	  	
 	  	while (it.hasNext()) 
 	  	{
 	  	   double cumulative = 0.00;
 	       Map.Entry<MasonGeometry, Double> entry = (Map.Entry<MasonGeometry, Double>)it.next();
 	       double probNode = entry.getValue();
 	       cumulative += probNode;
 	        if (cumulative < p) continue;
 	        if (cumulative >= p) 
        	{
	        	randomNode = entry.getKey();
	        	break;
        	}
 	    }
 	  	return network.findNode(randomNode.geometry.getCoordinate());
    }
    
   
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
