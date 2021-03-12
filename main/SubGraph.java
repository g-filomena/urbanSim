package urbanmason.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.LineString;

import sim.util.geo.GeomPlanarGraphDirectedEdge;

/**
 * This class represents sub graphs derived from a graph. It establish links between the graph component and the corresponding child components, allowing
 * faster operations and the creation of "regional" or "district" graphs.
 *
 * Navigation throughout a SubGraph is straightforward and can be easily retraced to the parent graph.
 */

public class SubGraph extends Graph
{
	private SubGraphNodesMap subGraphNodesMap = new SubGraphNodesMap();
	private SubGraphEdgesMap subGraphEdgesMap = new SubGraphEdgesMap();
	private ArrayList<Integer> graphBarriers = new ArrayList<Integer>();
	LinkedHashMap<NodeGraph, Double> centralityMap = new LinkedHashMap<NodeGraph, Double>();
	Graph parentGraph = new Graph();

	/**
	 * The SubGraph constructor, when passing the parent graph and the list of EdgeGraphs in the parent graph that should be included in the
	 * SubGraph.
	 *
	 * @param parentGraph the original parent graph.
	 * @param edges the list of edges to include.
	 */
	public SubGraph(Graph parentGraph, ArrayList<EdgeGraph> edges) {
		//		this.parentGraph = parentGraph;
		for (EdgeGraph edge: edges) addFromOtherGraph(parentGraph, edge);
		for (NodeGraph node : this.getNodesList()) {
			node.setNeighbouringComponents();
		}
	}

	public SubGraph() {
	}

	/**
	 * It adds an EdgeGraph and the corresponding nodes to the SubGraph, with all the necessary attributes.
	 *
	 * @param parentGraph the parent graph;
	 * @param parentEdge the parent edge that it's being added;
	 */
	public void addFromOtherGraph(Graph parentGraph, EdgeGraph parentEdge) {

		NodeGraph u = parentEdge.u;
		NodeGraph v = parentEdge.v;
		Coordinate uCoord = u.getCoordinate();
		Coordinate vCoord = v.getCoordinate();

		NodeGraph childU = this.getNode(uCoord);
		NodeGraph childV = this.getNode(vCoord);
		LineString line = parentEdge.getLine();
		Coordinate[] coords = CoordinateArrays.removeRepeatedPoints(line.getCoordinates());
		EdgeGraph childEdge = new EdgeGraph(line);

		GeomPlanarGraphDirectedEdge de0 = new GeomPlanarGraphDirectedEdge(childU, childV, coords[1], true);
		GeomPlanarGraphDirectedEdge de1 = new GeomPlanarGraphDirectedEdge(childV, childU, coords[coords.length - 2], false);
		childEdge.setDirectedEdges(de0, de1);
		this.setAttributesChildEdge(childEdge, parentEdge);
		childEdge.setNodes(childU, childV);

		subGraphNodesMap.add(childU, u);
		subGraphNodesMap.add(childV, v);
		subGraphEdgesMap.add(childEdge, parentEdge);
		childU.primalEdge = u.primalEdge;
		childV.primalEdge = v.primalEdge;
		add(childEdge);
		this.edgesGraph.add(childEdge);
	}

	/**
	 * It maps the nodes of the SubGraph with its parent graph's nodes.
	 *
	 */
	private class SubGraphNodesMap 		{
		public HashMap<NodeGraph, NodeGraph> map = new HashMap<NodeGraph, NodeGraph>();
		private void add(NodeGraph node, NodeGraph parentNode) 	{
			map.put(node, parentNode);
		}
		private NodeGraph findParent(NodeGraph nodeSubGraph) {return map.get(nodeSubGraph);}
		private NodeGraph findChild(NodeGraph nodeGraph) {return Utilities.getKeyFromValue(map, nodeGraph);}
	}

	/**
	 * It maps the edges of the SubGraph with its parent graph's edges.
	 *
	 */
	private class SubGraphEdgesMap {

		private HashMap<EdgeGraph, EdgeGraph> map = new HashMap<EdgeGraph, EdgeGraph>();
		public void add(EdgeGraph edge, EdgeGraph parentEdge) {
			map.put(edge, parentEdge);
		}

		private EdgeGraph findParent(EdgeGraph edgeSubGraph) {return map.get(edgeSubGraph);}
		private EdgeGraph findChild(EdgeGraph edgeSubGraph) {return Utilities.getKeyFromValue(map, edgeSubGraph);}
	}

	/**
	 * It returns the parent node of a child node;
	 *
	 * @param childNode a child node in the SubGraph;
	 */
	public NodeGraph getParentNode(NodeGraph childNode)	{
		return subGraphNodesMap.findParent(childNode);
	}

	/**
	 * It returns the parent nodes of all the child nodes in the SubGraph;
	 *
	 */
	public ArrayList<NodeGraph> getParentNodes() {
		ArrayList<NodeGraph> parentNodes = new ArrayList<NodeGraph>();
		parentNodes.addAll(this.subGraphNodesMap.map.values());
		return parentNodes;
	}

	/**
	 * It returns all the parent nodes associated with the child nodes contained in the list passed;
	 *
	 * @param childNodes a list of child nodes contained in the SubGraph;
	 */
	public ArrayList<NodeGraph> getParentNodes(ArrayList<NodeGraph> childNodes) {

		ArrayList<NodeGraph> parentNodes = new  ArrayList<NodeGraph>();
		for (NodeGraph child: childNodes) {
			NodeGraph parent = this.subGraphNodesMap.findParent(child);
			if (parent != null) parentNodes.add(parent);
		}
		return parentNodes;
	}

	/**
	 * It returns all the child nodes associated with the parent nodes contained in the list passed;
	 *
	 * @param parentNodes a list of parent nodes, who may be associated with child nodes in the SubGraph;
	 */
	public ArrayList<NodeGraph> getChildNodes(ArrayList<NodeGraph> parentNodes) {
		ArrayList<NodeGraph> childNodes = new  ArrayList<NodeGraph>();
		for (NodeGraph parent: parentNodes) {
			NodeGraph child = this.subGraphNodesMap.findChild(parent);
			if (child != null) childNodes.add(child);
		}
		return childNodes;
	}

	/**
	 * It returns all the child edges associated with the parent edges contained in the list passed;
	 *
	 * @param parentEdges a list of parent edges, who may be associated with child edges in the SubGraph;
	 */
	public ArrayList<EdgeGraph> getChildEdges(ArrayList<EdgeGraph> parentEdges)	{
		ArrayList<EdgeGraph> childEdges = new  ArrayList<EdgeGraph>();
		for (EdgeGraph parent: parentEdges) {
			EdgeGraph child = this.subGraphEdgesMap.findChild(parent);
			if (child != null) childEdges.add(child);
		}
		return childEdges;
	}

	/**
	 * It returns the parent edge of a child edge;
	 *
	 * @param childEdge a child edge in the SubGraph;
	 */
	public EdgeGraph getParentEdge(EdgeGraph childEdge) {
		return subGraphEdgesMap.findParent(childEdge);
	}

	/**
	 * It returns all the parent edges associated with the child edges contained in the list passed;
	 *
	 * @param childEdges a list of child edges contained in the SubGraph;
	 */
	public ArrayList<EdgeGraph> getParentEdges(ArrayList<EdgeGraph> childEdges) {
		ArrayList<EdgeGraph> parentEdges = new  ArrayList<EdgeGraph>();
		for (EdgeGraph child: childEdges) {
			EdgeGraph parent = this.subGraphEdgesMap.findParent(child);
			if (parent != null) parentEdges.add(parent);
		}
		return parentEdges;
	}

	/**
	 * It returns the ArrayList of NodeGraphs contained in this SubGraph.
	 */
	public ArrayList<NodeGraph> getNodesList() {
		ArrayList<NodeGraph> nodesList = new ArrayList<NodeGraph>();
		nodesList.addAll(this.subGraphNodesMap.map.keySet());
		return nodesList;
	}


	/**
	 * It sets the attribute of a child edge, passing the corresponding parent edge.
	 *
	 * @param childEdge the child edge;
	 * @param parentEdge the parent edge;
	 */
	public void setAttributesChildEdge(EdgeGraph childEdge, EdgeGraph parentEdge) {
		childEdge.setID(parentEdge.getID());
		childEdge.dualNode = parentEdge.getDual();
		// for dual edges:
		childEdge.deflectionDegrees = parentEdge.deflectionDegrees;
	}

	/**
	 * It stores information about the barriers within this SubGraph.
	 *
	 */
	public void setSubGraphBarriers() {

		ArrayList<Integer> graphBarriers = new ArrayList<Integer>();
		for (EdgeGraph childEdge : this.edgesGraph) {
			childEdge.barriers = this.getParentEdge(childEdge).barriers;
			childEdge.positiveBarriers = this.getParentEdge(childEdge).positiveBarriers;
			childEdge.negativeBarriers = this.getParentEdge(childEdge).negativeBarriers;
			childEdge.waterBodies = this.getParentEdge(childEdge).waterBodies;
			childEdge.parks = this.getParentEdge(childEdge).parks;
			graphBarriers.addAll(childEdge.barriers);
		}
		Set<Integer> setBarriers = new HashSet<Integer>(graphBarriers);
		this.graphBarriers = new ArrayList<Integer>(setBarriers);
	}

	/**
	 * It stores information about landmark at nodes, within the SubGraph.
	 *
	 */
	public void setSubGraphLandmarks() 	{
		ArrayList<NodeGraph> childNodes = this.getNodesList();

		for (NodeGraph node : childNodes) {
			NodeGraph parentNode = this.getParentNode(node);
			node.visible2d = parentNode.visible2d;
			node.localLandmarks = parentNode.localLandmarks;
			node.distantLandmarks = parentNode.distantLandmarks;
			node.anchors = parentNode.anchors;
			node.distances = parentNode.distances;
		}
	}

	/**
	 * It gets information about the barriers from the parent graph.
	 *
	 */
	public ArrayList<Integer> getSubGraphBarriers() {
		return this.graphBarriers;
	}

	/**
	 * It generates the centrality map of the SubGraph.
	 *
	 */
	public void generateSubGraphCentralityMap() {
		LinkedHashMap<NodeGraph, Double> centralityMap = new LinkedHashMap<NodeGraph, Double>();
		Collection<NodeGraph> nodes = this.subGraphNodesMap.map.keySet();
		for (NodeGraph n: nodes) 	{
			NodeGraph parentNode = this.getParentNode(n);
			centralityMap.put(n, parentNode.centrality);
		}
		this.centralityMap = (LinkedHashMap<NodeGraph, Double>) Utilities.sortByValue(centralityMap, false);
	}

	/**
	 * It returns a Map of salient nodes, on the basis of centrality values in the parent graph.
	 * The returned Map is in the format <NodeGraph, Double>, where the values represent centrality values.
	 * The percentile determines the threshold used to identify salient nodes. For example, if 0.75 is provided,
	 * only the nodes whose centrality value is higher than the value at the 75th percentile are returned.
	 *
	 * The keys represent NodeGraph in the parent Graph (parent nodes).
	 * @param percentile the percentile use to identify salient nodes;
	 */
	public ArrayList<NodeGraph> globalSalientNodesInSubGraph(double percentile) {
		Map<NodeGraph, Double> salientParentGraph = this.parentGraph.salientNodesNetwork(percentile);
		ArrayList<NodeGraph> salientParentNodes = new ArrayList<NodeGraph>(salientParentGraph.keySet());
		salientParentNodes.retainAll(this.getParentNodes());
		return salientParentNodes;
	}

	@Override
	/**
	 * It returns a Map of salient nodes, on the basis of centrality values.
	 * The returned Map is in the format <NodeGraph, Double>, where the values represent centrality values.
	 * The percentile determines the threshold used to identify salient nodes. For example, if 0.75 is provided,
	 * only the nodes whose centrality value is higher than the value at the 75th percentile are returned.
	 * This is computed within the SubGraph.
	 * The keys represent NodeGraph in the SubGraph (child nodes).
	 *
	 * @param percentile the percentile use to identify salient nodes;
	 */
	public Map<NodeGraph, Double> salientNodesNetwork(double percentile) {
		int position;
		double min_value = 0.0;

		position = (int) (this.centralityMap.size()*percentile);
		min_value = (new ArrayList<Double>(this.centralityMap.values())).get(position);

		double boundary = min_value;
		Map<NodeGraph, Double> filteredMap = this.centralityMap.entrySet().stream()
				.filter(entry -> entry.getValue() >= boundary)
				.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

		if ((filteredMap.size() == 0) || (filteredMap == null)) return null;
		Map<NodeGraph, Double> parentMap = new HashMap<NodeGraph, Double>();

		for (Map.Entry<NodeGraph, Double> entry : filteredMap.entrySet()) {
			NodeGraph parentNode = this.getParentNode(entry.getKey());
			parentMap.put(parentNode, entry.getValue());
		}
		return parentMap;
	}
}