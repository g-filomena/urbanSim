package urbanmason.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.planargraph.DirectedEdge;

import sim.field.network.Network;
import sim.util.Bag;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.MasonGeometry;



/**
 * A planar graph that extends the GeomPlanarGraph (GeoMason) and PlanarGraph (JTS) classes.
 * Its basic components are NodeGraph and EdgeGraph.
 *
 */
public class Graph extends GeomPlanarGraph
{
	public ArrayList<EdgeGraph> edgesGraph = new ArrayList<EdgeGraph>();
	LinkedHashMap<NodeGraph, Double> centralityMap = new LinkedHashMap<NodeGraph, Double>();
	public HashMap<Integer, NodeGraph> nodesMap = new HashMap<Integer, NodeGraph>();
	VectorLayer junctions = new VectorLayer();

	public Graph()
	{
		super();
	}

	/**
	 * It populate network with lines from a GeomVectorField (GeoMason) or VectorLayer
	 *
	 * @param streetSegments the street segments layer;
	 */
	public void fromGeomField(VectorLayer streetSegments) {
		Bag geometries = streetSegments.getGeometries();
		for (Object o : geometries) {
			if (((MasonGeometry) o).geometry instanceof LineString) this.addLineString((MasonGeometry) o);
		}
	}

	/**
	 * It populate network with lines from a GeomVectorField (GeoMason) or VectorLayer.
	 * It also stores the geometries of the junctions, for convenience.
	 *
	 * @param streetJunctins the street junctions layer;
	 * @param streetSegments the street segments layer;
	 */
	public void fromGeomField(VectorLayer streetJunctions, VectorLayer streetSegments) {
		Bag geometries = streetSegments.getGeometries();
		for (Object o : geometries) {
			if (((MasonGeometry) o).geometry instanceof LineString) this.addLineString((MasonGeometry) o);
		}
		this.junctions = streetJunctions;
	}

	/**
	 * It adds an Edge Graph and its nodes to the graph.
	 * It also stores the geometries of the junctions, for convenience.
	 *
	 * @param wrappedLine the MasonGeometry corresponding to a street segment;
	 */
	private void addLineString(MasonGeometry wrappedLine) {
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


	/**
	 * It returns the NodeGraph corresponding to the given coordinate
	 *
	 * @param pt the coordinates;
	 * @note Override as the original methods returns a Node.
	 */
	public NodeGraph getNode(Coordinate pt) {

		NodeGraph node = findNode(pt);
		if (node == null) {
			node = new NodeGraph(pt);
			// ensure node is only added once to graph
			add(node);
		}
		return node;
	}

	/**
	 * It returns this graph's network;
	 *
	 */
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

	/**
	 * It returns the NodeGraph corresponding to the coordinate
	 *
	 * @param pt the coordinates;
	 * @note Override as the original methods returns a Node.
	 */
	@Override
	public NodeGraph findNode(Coordinate pt) {
		return (NodeGraph) nodeMap.find(pt);
	}

	/**
	 * It generates the nodes map of this graph.
	 *
	 */
	private void generateNodesMap() {
		Collection<NodeGraph> nodes = this.getNodes();
		for (NodeGraph node : nodes) {
			nodesMap.put(node.getID(), node);
		}
	}

	/**
	 * It generates the nodes' centrality map of this graph.
	 *
	 */
	public void generateCentralityMap() {

		this.generateNodesMap();
		LinkedHashMap<NodeGraph, Double> centralityMap = new LinkedHashMap<NodeGraph, Double>();
		Collection<NodeGraph> nodes = nodesMap.values();

		for (NodeGraph node : nodes) centralityMap.put(node, node.centrality);
		this.centralityMap = (LinkedHashMap<NodeGraph, Double>) Utilities.sortByValue(centralityMap, false);

		// rescale
		for (NodeGraph node : nodes) {
			double rescaled = (node.centrality - Collections.min(centralityMap.values())) /
					(Collections.max(centralityMap.values()) - Collections.min(centralityMap.values()));
			node.centrality_sc = rescaled;
		}
	}


	/**
	 * It returns a Map of salient nodes, on the basis of centrality values.
	 * The returned Map is in the format <NodeGraph, Double>, where the values represent centrality values.
	 * The percentile determines the threshold used to identify salient nodes. For example, if 0.75 is provided,
	 * only the nodes whose centrality value is higher than the value at the 75th percentile are returned.
	 * This is computed within the space (smallest enclosing circle) between two given nodes;
	 * The keys represent NodeGraph in the SubGraph (child nodes).
	 *
	 * @param node a node;
	 * @param otherNode an other node;
	 * @param percentile the percentile use to identify salient nodes;
	 */
	public Map<NodeGraph, Double> salientNodesWithinSpace(NodeGraph node, NodeGraph otherNode, double percentile) {
		ArrayList<NodeGraph> containedNodes = new ArrayList<NodeGraph>();
		Geometry smallestEnclosingCircle = NodeGraph.nodesEnclosingCircle(node, otherNode);
		containedNodes = this.getContainedNodes(smallestEnclosingCircle);

		if (containedNodes.size() == 0 ) return null;
		LinkedHashMap<NodeGraph, Double> spatialfilteredMap = new LinkedHashMap<NodeGraph, Double>();
		spatialfilteredMap = filterCentralityMap(centralityMap, containedNodes);
		if (spatialfilteredMap.size() == 0 || spatialfilteredMap == null ) return null;

		int position = (int) (spatialfilteredMap.size()*percentile);
		double boundary = (new ArrayList<Double>(spatialfilteredMap.values())).get(position);
		Map<NodeGraph, Double> valueFilteredMap = spatialfilteredMap.entrySet().stream()
				.filter(entry -> entry.getValue() >= boundary)
				.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

		if (valueFilteredMap.size() == 0 || valueFilteredMap == null) return null;
		else return valueFilteredMap;
	}


	/**
	 * It returns a Map of salient nodes, on the basis of centrality values.
	 * The returned Map is in the format <NodeGraph, Double>, where the values represent centrality values.
	 * The percentile determines the threshold used to identify salient nodes. For example, if 0.75 is provided,
	 * only the nodes whose centrality value is higher than the value at the 75th percentile are returned.
	 * This is computed within the entire graph..
	 *
	 * @param percentile the percentile use to identify salient nodes;
	 */
	public Map<NodeGraph, Double> salientNodesNetwork(double percentile) {
		int position;
		position = (int) (centralityMap.size()*percentile);
		double boundary = (new ArrayList<Double>(centralityMap.values())).get(position);
		Map<NodeGraph, Double> valueFilteredMap = centralityMap.entrySet().stream()
				.filter(entry -> entry.getValue() >= boundary)
				.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
		if (valueFilteredMap.size() == 0 || valueFilteredMap == null) return null;
		else return valueFilteredMap;
	}

	/**
	 * It returns a list of nodes contained within a given geometry.
	 *
	 * @param g the given geometry;
	 */
	public ArrayList<NodeGraph> getContainedNodes(Geometry g) {
		ArrayList<NodeGraph> containedNodes = new ArrayList<NodeGraph>();
		Collection<NodeGraph> nodes = nodesMap.values();

		for (NodeGraph node : nodes ) {
			Geometry geoNode = node.masonGeometry.geometry;
			if (g.contains(geoNode)) containedNodes.add(node);
		}
		return containedNodes;
	}

	/**
	 * It returns a list of edges contained within a given geometry.
	 *
	 * @param g the given geometry;
	 */
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

	/**
	 * It returns the list of edges contained in the graph.
	 *
	 * @param g the given geometry;
	 */
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

		Double radius = NodeGraph.nodesDistance(originNode,  destinationNode)*1.50;
		if (radius < 500) radius = 500.0;
		Geometry bufferOrigin = originNode.masonGeometry.geometry.buffer(radius);
		Geometry bufferDestination = destinationNode.masonGeometry.geometry.buffer(radius);
		Geometry convexHull = bufferOrigin.union(bufferDestination).convexHull();

		ArrayList<EdgeGraph> containedEdges = this.getContainedEdges(convexHull);
		return containedEdges;
	}

	public ArrayList<NodeGraph> getNodesBetweenLimits(NodeGraph originNode, double lowerLimit, double upperLimit) {

		ArrayList<NodeGraph> containedNodes = new ArrayList<NodeGraph>();
		MasonGeometry originGeometry = originNode.masonGeometry;
		Bag containedGeometries = this.junctions.featuresBetweenLimits(originGeometry.geometry, lowerLimit, upperLimit);
		for (Object o : containedGeometries) containedNodes.add(this.findNode(((MasonGeometry) o).geometry.getCoordinate()));
		return containedNodes;
	}

	public ArrayList<NodeGraph> getNodesBetweenLimitsOtherRegion(NodeGraph originNode, double lowerLimit, double upperLimit) {
		ArrayList<NodeGraph> containedNodes = new ArrayList<NodeGraph>();
		containedNodes = getNodesBetweenLimits(originNode, lowerLimit, upperLimit);
		return this.filterOutRegion(containedNodes, originNode.region);
	}


	public ArrayList<NodeGraph> filterOutRegion(ArrayList<NodeGraph> nodes, int region) {
		ArrayList<NodeGraph>  newNodes = new ArrayList<NodeGraph>(nodes);
		for(NodeGraph node : nodes) if (node.region == region) newNodes.remove(node);
		return newNodes;

	}

	public void setLocalLandmarkness(VectorLayer localLandmarks, HashMap<Integer, Building> buildingsMap,
			double radius) {

		Collection<NodeGraph> nodes = nodesMap.values();

		nodes.forEach((node) -> {

			Bag containedLandmarks = localLandmarks.featuresWithinDistance(node.masonGeometry.geometry, radius);
			for (Object l : containedLandmarks ) {
				MasonGeometry building = (MasonGeometry) l;
				node.localLandmarks.add(buildingsMap.get((int) building.getUserData()));
			}
		});
	}

	public void setGlobalLandmarkness(VectorLayer globalLandmarks, HashMap<Integer, Building> buildingsMap,
			double radiusAnchors, VectorLayer sightLines, int nrAnchors) {

		Collection<NodeGraph> nodes = nodesMap.values();

		for (NodeGraph node : nodes) {
			MasonGeometry nodeGeometry = node.masonGeometry;

			Bag containedLandmarks = globalLandmarks.featuresWithinDistance(node.masonGeometry.geometry, radiusAnchors);
			List<Double> gScores = new ArrayList<Double>();

			if (nrAnchors != 999999) {
				for (Object l : containedLandmarks ) {
					MasonGeometry building = (MasonGeometry) l;
					gScores.add(building.getDoubleAttribute("gScore_sc"));

				}
				Collections.sort(gScores);
				Collections.reverse(gScores);
			}

			for (Object l : containedLandmarks ) {
				MasonGeometry building = (MasonGeometry) l;
				if (nrAnchors != 999999 & building.getDoubleAttribute("gScore_sc") < gScores.get(nrAnchors-1)) continue;
				int buildingID = (int) building.getUserData();
				node.anchors.add(buildingsMap.get(buildingID));
				node.distances.add(building.geometry.distance(nodeGeometry.geometry));
			}
		}

		ArrayList<MasonGeometry> sightLinesGeometries = sightLines.geometriesList;
		for (MasonGeometry sl : sightLinesGeometries) {
			Building building = buildingsMap.get(sl.getIntegerAttribute("buildingID"));
			NodeGraph node = nodesMap.get(sl.getIntegerAttribute("nodeID"));
			node.distantLandmarks.add(building);
		}
	}
}














