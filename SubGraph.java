package sim.app.geo.urbanSim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.LineString;

import sim.util.geo.GeomPlanarGraphDirectedEdge;



public class SubGraph extends Graph
{
	private SubGraphNodesMap subGraphNodesMap = new SubGraphNodesMap();
	private SubGraphEdgesMap subGraphEdgesMap = new SubGraphEdgesMap();
	private ArrayList<Integer> graphBarriers = new ArrayList<Integer>();
	LinkedHashMap<NodeGraph, Double> centralityMap = new LinkedHashMap<NodeGraph, Double>();
	Graph parentGraph;

	public SubGraph(Graph parentGraph, ArrayList<EdgeGraph> edges) {
		this.parentGraph = parentGraph;
		for (EdgeGraph edge: edges)
		{
			NodeGraph u = edge.u;
			NodeGraph v = edge.v;
			addFromOtherGraph(parentGraph, edge, u,v);
		}
	}

	public SubGraph() {
	}

	public void addFromOtherGraph(Graph parentGraph, EdgeGraph edge, NodeGraph u, NodeGraph v) {
		Coordinate uCoord = u.getCoordinate();
		Coordinate vCoord = v.getCoordinate();

		NodeGraph childU = this.getNode(uCoord);
		NodeGraph childV = this.getNode(vCoord);

		LineString line = edge.getLine();
		Coordinate[] coords = CoordinateArrays.removeRepeatedPoints(line.getCoordinates());
		EdgeGraph childEdge = new EdgeGraph(line);

		GeomPlanarGraphDirectedEdge de0 = new GeomPlanarGraphDirectedEdge(childU, childV, coords[1], true);
		GeomPlanarGraphDirectedEdge de1 = new GeomPlanarGraphDirectedEdge(childV, childU, coords[coords.length - 2], false);
		childEdge.setDirectedEdges(de0, de1);
		childEdge.setAttributes(edge.attributes);
		childEdge.setNodes(childU, childV);

		subGraphNodesMap.add(childU, u);
		subGraphNodesMap.add(childU, v);
		subGraphEdgesMap.add(childEdge, edge);
		childU.primalEdge = u.primalEdge;
		childV.primalEdge = v.primalEdge;
		add(childEdge);
		this.edgesGraph.add(childEdge);
	}

	public class SubGraphNodesMap 		{
		public HashMap<NodeGraph, NodeGraph> map = new HashMap<NodeGraph, NodeGraph>();
		public void add(NodeGraph node, NodeGraph parentNode) 		{
			map.put(node, parentNode);
		}

		public NodeGraph findParent(NodeGraph nodeSubGraph) {return map.get(nodeSubGraph);}
		public NodeGraph findChild(NodeGraph nodeGraph) {return Utilities.getKeyFromValue(map, nodeGraph);}
	}

	public class SubGraphEdgesMap {

		private HashMap<EdgeGraph, EdgeGraph> map = new HashMap<EdgeGraph, EdgeGraph>();
		public void add(EdgeGraph edge, EdgeGraph parentEdge) {
			map.put(edge, parentEdge);
		}

		public EdgeGraph findParent(EdgeGraph edgeSubGraph) {return map.get(edgeSubGraph);}
		public EdgeGraph findChild(EdgeGraph edgeSubGraph) {return Utilities.getKeyFromValue(map, edgeSubGraph);}
	}

	public NodeGraph getParentNode(NodeGraph nodeSubGraph)	{
		return subGraphNodesMap.findParent(nodeSubGraph);
	}

	public ArrayList<NodeGraph> getParentNodes() {
		ArrayList<NodeGraph> parentNodes = new ArrayList<NodeGraph>();
		parentNodes.addAll(this.subGraphNodesMap.map.values());
		return parentNodes;
	}

	public ArrayList<NodeGraph> getParentNodes(ArrayList<NodeGraph> childNodes) {

		ArrayList<NodeGraph> parentNodes = new  ArrayList<NodeGraph>();
		for (NodeGraph child: childNodes) {
			NodeGraph parent = this.subGraphNodesMap.findParent(child);
			if (parent != null) parentNodes.add(parent);
		}
		return parentNodes;
	}

	public ArrayList<NodeGraph> getChildNodes(ArrayList<NodeGraph> parentNodes) {
		ArrayList<NodeGraph> childNodes = new  ArrayList<NodeGraph>();
		for (NodeGraph parent: parentNodes) {
			NodeGraph child = this.subGraphNodesMap.findChild(parent);
			if (child != null) childNodes.add(child);
		}
		return childNodes;
	}

	public ArrayList<EdgeGraph> getChildEdges(ArrayList<EdgeGraph> parentEdges)	{
		ArrayList<EdgeGraph> childEdges = new  ArrayList<EdgeGraph>();
		for (EdgeGraph parent: parentEdges) {
			EdgeGraph child = this.subGraphEdgesMap.findChild(parent);
			if (child != null) childEdges.add(child);
		}
		return childEdges;
	}


	public EdgeGraph getParentEdge(EdgeGraph edgeSubGraph) {
		return subGraphEdgesMap.findParent(edgeSubGraph);
	}

	public ArrayList<EdgeGraph> getParentEdges(ArrayList<EdgeGraph> childEdges) {
		ArrayList<EdgeGraph> parentEdges = new  ArrayList<EdgeGraph>();
		for (EdgeGraph child: childEdges) {
			EdgeGraph parent = this.subGraphEdgesMap.findParent(child);
			if (parent != null) parentEdges.add(parent);
		}
		return parentEdges;
	}

	public void setSubGraphBarriers() {

		ArrayList<Integer> graphBarriers = new ArrayList<Integer>();
		for (EdgeGraph e : this.edgesGraph) {
			List<Integer> barriers = this.getParentEdge(e).barriers;
			if (barriers == null) continue;
			graphBarriers.addAll(barriers);
		}
		Set<Integer> setBarriers = new HashSet<Integer>(graphBarriers);
		this.graphBarriers = new ArrayList<Integer>(setBarriers);
	}

	public void setLandmarksSubGraph() 	{
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

	public ArrayList<Integer> getSubGraphBarriers() {
		return this.graphBarriers;
	}

	public void generateSubGraphCentralityMap() {
		LinkedHashMap<NodeGraph, Double> centralityMap = new LinkedHashMap<NodeGraph, Double>();
		Collection<NodeGraph> nodes = this.subGraphNodesMap.map.values();
		for (NodeGraph n: nodes) 	{
			NodeGraph parentNode = this.getParentNode(n);
			centralityMap.put(n, parentNode.centrality);
		}
		this.centralityMap = (LinkedHashMap<NodeGraph, Double>) Utilities.sortByValue(centralityMap, "ascending");
	}


	public ArrayList<NodeGraph> salientNodesInSubGraph(double percentile) {
		ArrayList<NodeGraph> salientParentNodes = this.parentGraph.salientNodesNetwork(percentile);
		salientParentNodes.retainAll(this.getParentNodes());
		return salientParentNodes;
	}


	public ArrayList<NodeGraph> localSalientNodes(double percentile) {
		int position;
		double min_value = 0.0;

		position = (int) (this.centralityMap.size()*percentile);
		min_value = (new ArrayList<Double>(this.centralityMap.values())).get(position);

		double boundary = min_value;
		Map<NodeGraph, Double> valueFilteredMap = this.centralityMap.entrySet().stream()
				.filter(entry -> entry.getValue() >= boundary)
				.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

		if ((valueFilteredMap.size() == 0) || (valueFilteredMap == null)) return null;
		ArrayList<NodeGraph> result = new ArrayList<>(valueFilteredMap.keySet());
		return result;
	}

	public ArrayList<NodeGraph> getNodesList() {
		ArrayList<NodeGraph> nodesList = new ArrayList<NodeGraph>();
		nodesList.addAll(this.subGraphNodesMap.map.keySet());
		return nodesList;
	}
}