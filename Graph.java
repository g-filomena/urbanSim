package sim.app.geo.urbanSim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
	public ArrayList<EdgeGraph> edgesGraph = new ArrayList<EdgeGraph>();
	LinkedHashMap<NodeGraph, Double> centralityMap = new LinkedHashMap<NodeGraph, Double>();
	public HashMap<Integer, NodeGraph> nodesMap = new HashMap<Integer, NodeGraph>();

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

		for (Object o : geometries)
		{
			if (((MasonGeometry) o).geometry instanceof LineString) this.addLineString((MasonGeometry) o);
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

		nodesMap.put(u.getID(), u);
		nodesMap.put(v.getID(), v);

		EdgeGraph edge = new EdgeGraph(line);
		GeomPlanarGraphDirectedEdge de0 = new GeomPlanarGraphDirectedEdge(u, v, coords[1], true);
		GeomPlanarGraphDirectedEdge de1 = new GeomPlanarGraphDirectedEdge(v, u, coords[coords.length - 2], false);

		edge.setDirectedEdges(de0, de1);
		edge.setAttributes(wrappedLine.getAttributes());
		edge.setNodes(u, v);
		edge.masonGeometry = wrappedLine;
		add(edge);
		edgesGraph.add(edge);
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
	@Override
	public NodeGraph getNode(Coordinate pt) {

		NodeGraph node = findNode(pt);
		if (node == null) {
			node = new NodeGraph(pt);
			// ensure node is only added once to graph
			add(node);
		}
		return node;
	}

	@Override
	public Network getNetwork() {

		Network network = new Network(false); // false == not directed
		Collection edges = getEdges();
		for (Object object : edges ) {
			DirectedEdge edge = (DirectedEdge) object;
			network.addEdge(edge.getFromNode(), edge.getToNode(), edge);
		}
		return network;
	}

	@Override
	public NodeGraph findNode(Coordinate pt) {
		return (NodeGraph) nodeMap.find(pt);
	}

	private void generateNodesMap() {
		Collection<NodeGraph> nodes = this.getNodes();
		for (NodeGraph node : nodes) {
			nodesMap.put(node.getID(), node);
		}
	}

	public void generateCentralityMap() {

		this.generateNodesMap();

		LinkedHashMap<NodeGraph, Double> centralityMap = new LinkedHashMap<NodeGraph, Double>();
		Collection<NodeGraph> nodes = nodesMap.values();

		for (NodeGraph node : nodes) centralityMap.put(node, node.centrality);
		this.centralityMap = (LinkedHashMap<NodeGraph, Double>) Utilities.sortByValue(centralityMap, "ascending");

		// rescale
		for (NodeGraph node : nodes) {
			double rescaled = (node.centrality - Collections.min(centralityMap.values())) /
					(Collections.max(centralityMap.values()) - Collections.min(centralityMap.values()));
			node.centrality_sc = rescaled;
		}
	}


	public ArrayList<NodeGraph> salientNodesWithinSpace(NodeGraph originNode, NodeGraph destinationNode, double lowL, double uppL,
			double percentile, String typeLandmarkness) {

		Geometry smallestEnclosingCircle;
		ArrayList<NodeGraph> containedNodes = new ArrayList<NodeGraph>();

		if (destinationNode != null && lowL == 0) {
			smallestEnclosingCircle = Utilities.smallestEnclosingCircle(originNode, destinationNode);
			containedNodes = this.getContainedNodes(smallestEnclosingCircle);
		}
		else if (destinationNode == null && lowL == 0) {
			smallestEnclosingCircle = originNode.masonGeometry.geometry.buffer(uppL);
			containedNodes = this.getContainedNodes(smallestEnclosingCircle);
		}
		else if (lowL != 0) containedNodes = this.getNodesBetweenLimits(originNode, lowL, uppL);

		if (containedNodes.size() == 0 || containedNodes == null) return null;
		LinkedHashMap<NodeGraph, Double> SpatialfilteredMap = filterCentralityMap(centralityMap, containedNodes);
		if (SpatialfilteredMap.size() == 0 || SpatialfilteredMap == null) return null;

		int position;
		double min_value = 0.0;

		// global quantile
		if (typeLandmarkness == "global") {
			position = (int) (centralityMap.size()*percentile);
			min_value = (new ArrayList<Double>(centralityMap.values())).get(position);
		}
		else {
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

	public ArrayList<NodeGraph> salientNodesNetwork(double percentile) {
		int position;
		double min_value = 0.0;

		position = (int) (centralityMap.size()*percentile);
		min_value = (new ArrayList<Double>(centralityMap.values())).get(position);

		double boundary = min_value;
		Map<NodeGraph, Double> valueFilteredMap = centralityMap.entrySet().stream()
				.filter(entry -> entry.getValue() >= boundary)
				.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

		if ((valueFilteredMap.size() == 0) || (valueFilteredMap == null)) return null;
		ArrayList<NodeGraph> result = new ArrayList<>(valueFilteredMap.keySet());
		return result;
	}


	public ArrayList<NodeGraph> getContainedNodes(Geometry g) {
		ArrayList<NodeGraph> containedNodes = new ArrayList<NodeGraph>();
		Collection<NodeGraph> nodes = nodesMap.values();

		for (NodeGraph node : nodes ) {
			Geometry geoNode = node.masonGeometry.getGeometry();
			if (g.contains(geoNode)) containedNodes.add(node);
		}
		return containedNodes;
	}

	public ArrayList<EdgeGraph> getContainedEdges(Geometry g)
	{
		ArrayList<EdgeGraph> containedEdges = new ArrayList<EdgeGraph>();
		ArrayList<EdgeGraph> edges = this.edgesGraph;

		for (EdgeGraph edge: edges) {
			Geometry geoEdge = edge.masonGeometry.geometry;
			if (g.contains(geoEdge)) containedEdges.add(edge);
		}
		return containedEdges;
	}

	@Override
	public ArrayList<EdgeGraph> getEdges() {
		return this.edgesGraph;
	}

	public static LinkedHashMap<NodeGraph, Double> filterCentralityMap(LinkedHashMap<NodeGraph, Double> map, ArrayList<NodeGraph> filter) {

		LinkedHashMap<NodeGraph, Double> mapFiltered = new LinkedHashMap<NodeGraph, Double> (map);
		ArrayList<NodeGraph> result = new ArrayList<NodeGraph>();
		for(NodeGraph key : mapFiltered.keySet()) {if(filter.contains(key)) result.add(key);}
		mapFiltered.keySet().retainAll(result);
		return mapFiltered;
	}


	public ArrayList<EdgeGraph> edgesWithinSpace(NodeGraph originNode, NodeGraph destinationNode) {
		Double radius = Utilities.nodesDistance(originNode,  destinationNode)*1.50;
		if (radius < 500) radius = 500.0;
		Geometry bufferOrigin = originNode.masonGeometry.geometry.buffer(radius);
		Geometry bufferDestination = destinationNode.masonGeometry.geometry.buffer(radius);
		Geometry convexHull = bufferOrigin.union(bufferDestination).convexHull();

		ArrayList<EdgeGraph> containedEdges = this.getContainedEdges(convexHull);
		return containedEdges;
	}

	public ArrayList<NodeGraph> getNodesBetweenLimits(NodeGraph originNode, double lowL, double uppL) {

		Geometry bufferS = originNode.masonGeometry.geometry.buffer(lowL);
		Geometry bufferB = originNode.masonGeometry.geometry.buffer(uppL);
		Geometry space = bufferB.difference(bufferS);
		return this.getContainedNodes(space);
	}

	public ArrayList<NodeGraph> getNodesBetweenLimitsOtherRegion(NodeGraph originNode, double lowL, double uppL) {
		Geometry bufferL = originNode.masonGeometry.geometry.buffer(lowL);
		Geometry bufferU = originNode.masonGeometry.geometry.buffer(uppL);
		Geometry space = bufferU.difference(bufferL);
		return this.filterOutRegion(this.getContainedNodes(space), originNode.region);
	}


	public ArrayList<NodeGraph> filterOutRegion(ArrayList<NodeGraph> nodes, int region) {
		ArrayList<NodeGraph>  newNodes = new ArrayList<NodeGraph>(nodes);
		for(NodeGraph node : nodes) if (node.region == region) newNodes.remove(node);
		return newNodes;

	}

	public void setLocalLandmarkness(VectorLayer localLandmarks, HashMap<Integer, Building> buildingsMap,
			double radius, String[][] visibilityMatrix) {

		Collection<NodeGraph> nodes = nodesMap.values();

		for (NodeGraph node : nodes) {

			Bag containedLandmarks = localLandmarks.featuresWithinDistance(node.masonGeometry.geometry, radius);
			if (containedLandmarks.size() == 0) {
				node.localLandmarks = null;
			}
			else {
				for (Object l : containedLandmarks ) {
					MasonGeometry building = (MasonGeometry) l;
					node.localLandmarks.add(buildingsMap.get((int) building.getUserData()));
				}
			}
		}

		// landmarks visible from the junction
		if (visibilityMatrix != null) {
			for (int column = 1; column < visibilityMatrix[0].length; column++) {
				for (int row = 1; row < visibilityMatrix.length; row++) {
					int visibility = Integer.valueOf(visibilityMatrix[row][column]);

					if (visibility == 1) {
						int buildingID = Integer.valueOf(visibilityMatrix[row][0]);
						nodesMap.get(row).visible2d.add(buildingsMap.get(buildingID));
					}
				}
			}
		}
		else {
			for (NodeGraph node : nodes) node.visible2d = null;
		}
	}

	public void setGlobalLandmarkness(VectorLayer globalLandmarks, HashMap<Integer, Building> buildingsMap,
			double radius, VectorLayer sightLines) {

		Collection<NodeGraph> nodes = nodesMap.values();

		nodes.forEach((node) -> {
			Bag sightLinesFromNode = new Bag();
			MasonGeometry nodeGeometry = node.masonGeometry;
			Bag containedLandmarks = globalLandmarks.featuresWithinDistance(node.masonGeometry.geometry, radius);

			if (containedLandmarks.size() == 0) {
				node.anchors = null;
				node.distances = null;
			}
			else {
				for (Object l : containedLandmarks ) {
					MasonGeometry building = (MasonGeometry) l;
					int buildingID = (int) building.getUserData();
					node.anchors.add(buildingsMap.get(buildingID));
					node.distances.add(building.geometry.distance(nodeGeometry.geometry));
				}
			}

			sightLinesFromNode = sightLines.filterFeatures("nodeID", node.getID(), true);
			if (sightLinesFromNode.size() == 0) node.distantLandmarks = null;
			else {
				for (Object sL : sightLinesFromNode) {
					MasonGeometry sightLine = (MasonGeometry) sL;
					int buildingID = sightLine.getIntegerAttribute("buildingID");
					node.distantLandmarks.add(buildingsMap.get(buildingID));
				}
			}
		});
	}
}













