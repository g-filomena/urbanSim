package urbanmason.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sim.util.Bag;
import sim.util.geo.MasonGeometry;


/**
 * A class containing functions to identify random nodes, given certain conditions.
 * Usually these are used to identify origin and destination nodes for possible trips.
 *
 */
public class NodesLookup {

	/**
	 * Given a graph, it returns a random node within it.
	 *
	 * @param network a graph;
	 */
	public static NodeGraph randomNode(Graph network) {
		Random random = new Random();
		Integer c = random.nextInt(network.nodesMap.values().size());
		ArrayList<NodeGraph> nodes = new ArrayList<NodeGraph>(network.nodesMap.values());
		return nodes.get(c);
	}

	/**
	 * Given a graph and a list of nodes' geometries, it returns a random node of the graph, that is also contained in the list.
	 *
	 * @param network a graph;
	 * @param nodesGeometries the list of nodes' geometries;
	 */
	public static NodeGraph randomNode(Graph network, ArrayList<MasonGeometry> nodesGeometries) {
		Random random = new Random();
		Integer c = random.nextInt(nodesGeometries.size());
		MasonGeometry geoNode = nodesGeometries.get(c);
		return network.findNode(geoNode.geometry.getCoordinate());
	}

	/**
	 * Given a graph, the function returns a random node, within a certain radius from a given origin node and outside the given node's region.
	 *
	 * @param network a graph;
	 * @param originNode a node;
	 * @param radius the distance from the originNode, within which the random node should be found;
	 */
	public static NodeGraph randomNodeOtherRegion(Graph network, NodeGraph originNode, double radius) {

		VectorLayer junctionsWithin = new VectorLayer();
		MasonGeometry nodeGeometry = originNode.masonGeometry;
		Random random = new Random();

		Bag filterSpatial = null;
		Bag filterByDistrict = null;
		NodeGraph n = null;
		double expanding_radius = radius;
		while (n == null) {
			if (expanding_radius >= radius * 2.00) return null;
			filterSpatial = network.junctions.featuresWithinDistance(nodeGeometry.geometry, expanding_radius);

			if (filterSpatial.size() < 1) {
				expanding_radius = expanding_radius *1.10;
				continue;
			}
			for (Object o : filterSpatial) junctionsWithin.addGeometry((MasonGeometry) o);

			if (junctionsWithin.getGeometries().size() == 0) {
				expanding_radius = expanding_radius *1.10;
				continue;
			}

			filterByDistrict = junctionsWithin.filterFeatures("district", originNode.region, false);
			if (filterByDistrict.size() == 0) {
				expanding_radius = expanding_radius *1.10;
				continue;
			}

			Integer c = random.nextInt(filterByDistrict.size());
			MasonGeometry geoNode = (MasonGeometry) filterByDistrict.objs[c];
			n = network.findNode(geoNode.geometry.getCoordinate());
			expanding_radius = expanding_radius *1.10;
		}
		return n;
	}

	/**
	 * Given a graph, the function returns a random node that is approximately as far away from a given origin node,
	 * as a distance extracted from a list of distances.
	 *
	 * @param network a graph;
	 * @param originNode a node;
	 * @param distances the list of possible distances used to identify the node;
	 */
	public static NodeGraph randomNodeFromDistancesSet(Graph network, NodeGraph originNode, List<Float> distances) {

		Random random = new Random();
		int pD = random.nextInt(distances.size());
		double distance = distances.get(pD);
		if (distance < 100) distance = 100;
		NodeGraph node = null;
		ArrayList<NodeGraph> candidates = new ArrayList<NodeGraph>();
		double tolerance = 50;

		while(true) {
			double lowerLimit = distance - tolerance;
			double upperLimit = distance + tolerance;
			candidates = network.getNodesBetweenLimits(originNode, lowerLimit, upperLimit);
			if (candidates.size() > 1) break;
			else tolerance += 50;
		}
		while (node == null || (node.getID() == originNode.getID())) {
			Integer c = random.nextInt(candidates.size());
			node = candidates.get(c);
			if (originNode.getEdgeWith(node) != null) node = null;
		}
		return node;
	}

	/**
	 * Given a graph, the function returns a random node whose distance from a passed origin node is within certain limits.
	 *
	 * @param network a graph;
	 * @param originNode a node;
	 * @param lowerLimit the minimum distance from the origin node;
	 * @param upperLimit the maximum distance from the origin node;
	 */
	public static NodeGraph randomNodeBetweenLimits(Graph network, NodeGraph originNode, double lowerLimit, double upperLimit) {
		Random random = new Random();
		ArrayList <NodeGraph> candidates = network.getNodesBetweenLimits(originNode, lowerLimit, upperLimit);
		int c = random.nextInt(candidates.size());
		NodeGraph node = candidates.get(c);
		return node;
	}

	/**
	 * Given a graph, the function returns a random node whose distance from a passed origin node is within certain limits.
	 * The returned node belongs to a region different from the origin node's region.
	 *
	 * @param network a graph;
	 * @param originNode a node;
	 * @param lowerLimit the minimum distance from the origin node;
	 * @param upperLimit the maximum distance from the origin node;
	 */
	public static NodeGraph randomNodeBetweenLimitsOtherRegion(Graph network, NodeGraph originNode, double lowerLimit, double upperLimit) {
		Random random = new Random();
		ArrayList <NodeGraph> candidates = network.getNodesBetweenLimitsOtherRegion(originNode, lowerLimit, upperLimit);
		int c = random.nextInt(candidates.size());
		NodeGraph node = candidates.get(c);
		return node;
	}
}
