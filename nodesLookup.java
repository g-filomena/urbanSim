package sim.app.geo.urbanSim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sim.app.geo.pedSimCity.PedSimCity;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;

public class NodesLookup {


	// random node from whole set of nodes
	public static NodeGraph randomNode(Bag geometriesNodes, Graph network) {
		Random random = new Random();
		Integer c = random.nextInt(geometriesNodes.size());
		MasonGeometry geoNode = (MasonGeometry) geometriesNodes.objs[c];
		return network.findNode(geoNode.geometry.getCoordinate());
	}


	// look for a random node outside a given district, within a certain radius from a given node.
	public static NodeGraph randomNodeOtherRegion(NodeGraph originNode, double radius, Graph network) {

		VectorLayer junctionsWithin = new VectorLayer();
		MasonGeometry nodeGeometry = originNode.masonGeometry;
		Random random = new Random();

		Bag filterSpatial = null;
		Bag filterByDistrict = null;
		NodeGraph n = null;
		double expanding_radius = radius;
		while (n == null) {
			if (expanding_radius >= radius * 2.00) return null;
			filterSpatial = PedSimCity.junctions.getObjectsWithinDistance(nodeGeometry, expanding_radius);
			if (filterSpatial.size() < 1) {
				expanding_radius = expanding_radius *1.10;
				continue;
			}
			for (Object o : filterSpatial) {
				MasonGeometry geoNode = (MasonGeometry) o;
				junctionsWithin.addGeometry(geoNode);
			}

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


	public static NodeGraph randomNodeFromDistancesSet(NodeGraph originNode, VectorLayer nodeGeometries, List<Float> distances,
			Graph network) {
		Random random = new Random();
		int pD = random.nextInt(distances.size());
		Float distance = distances.get(pD);
		NodeGraph node = null;
		ArrayList <NodeGraph> candidates = null;
		double tolerance = 50;

		while(true) {
			double lowL = distance - tolerance;
			double uppL = distance + tolerance;
			candidates = network.getNodesBetweenLimits(originNode, lowL, uppL);
			if (candidates.size() > 1) break;
			else tolerance += 50;
		}

		while (node == null || (node.getID() == originNode.getID())) {
			Integer c = random.nextInt(candidates.size());
			node = candidates.get(c);
			if (originNode.getEdgeBetween(node) != null) node = null;
		}
		return node;
	}

	public static NodeGraph randomNodeBetweenLimits(NodeGraph originNode, double lowL, double uppL, Graph network) {
		Random random = new Random();
		ArrayList <NodeGraph> candidates = network.getNodesBetweenLimits(originNode, lowL, uppL);
		int c = random.nextInt(candidates.size());
		NodeGraph node = candidates.get(c);
		return node;
	}

	public static NodeGraph randomNodeBetweenLimitsOtherRegion(NodeGraph originNode, double lowL, double uppL, Graph network) {
		Random random = new Random();
		ArrayList <NodeGraph> candidates = network.getNodesBetweenLimitsOtherRegion(originNode, lowL, uppL);
		int c = random.nextInt(candidates.size());
		NodeGraph node = candidates.get(c);
		return node;
	}
}
