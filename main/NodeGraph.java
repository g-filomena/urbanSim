package urbanmason.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.DirectedEdge;
import com.vividsolutions.jts.planargraph.Node;

import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.MasonGeometry;

/**
 * A node of a planar graph that extends the Node class (JTS), with further and more straightforward functions.
 * This is one of the main components, along with EdgeGraph, of graphs belonging to the class Graph.
 *
 */
public class NodeGraph extends Node
{

	public NodeGraph(Coordinate pt) {super(pt);}

	public int nodeID;
	public int region = 999999;
	public boolean gateway;

	public MasonGeometry masonGeometry;
	public EdgeGraph primalEdge;
	public double centrality, centrality_sc;

	public ArrayList<Building> visible2d = new ArrayList<Building>();
	public ArrayList<Building> localLandmarks = new ArrayList<Building>();
	public ArrayList<Building> distantLandmarks  = new ArrayList<Building>();
	public ArrayList<Building> anchors = new ArrayList<Building>();
	public List<Double> distances = new ArrayList<Double>();

	public List<Integer> adjacentRegions = new ArrayList<Integer>();
	public ArrayList<NodeGraph> adjacentEntries = new ArrayList<NodeGraph>();
	private ArrayList<NodeGraph> adjacentNodes = new ArrayList<NodeGraph>();
	private ArrayList<EdgeGraph> edges = new ArrayList<EdgeGraph>();
	private ArrayList<GeomPlanarGraphDirectedEdge> outEdges = new ArrayList<GeomPlanarGraphDirectedEdge>();

	/**
	 * It sets the ID of the node;
	 *
	 */
	public void setID(int nodeID) {
		this.nodeID = nodeID;
	}

	/**
	 * It returns the ID of the node;
	 *
	 */
	public Integer getID() {
		return this.nodeID;
	}

	/**
	 * It returns the list of the EdgeGraphs that depart from the node;
	 *
	 */
	public ArrayList<EdgeGraph> getEdges() {
		return this.edges;
	}

	/**
	 * It returns the list of the NodeGraphs that are reachable from this node;
	 *
	 */
	public ArrayList<NodeGraph> getAdjacentNodes() {
		return this.adjacentNodes;
	}

	/**
	 * It sets some list useful for identifying the neighbouring components of this node.
	 *
	 */
	public void setNeighbouringComponents() {
		setEdgesNode();
		setAdjacentNodes();
		List<Object> out = new ArrayList<Object>(this.getOutEdges().getEdges());
		for (DirectedEdge e: outEdges) this.outEdges.add((GeomPlanarGraphDirectedEdge) e);
	}

	/**
	 * It returns the list of directed edges that depart from the node (out-going)
	 *
	 */
	public ArrayList<GeomPlanarGraphDirectedEdge> getOutDirectedEdges() {
		return this.outEdges;
	}


	/**
	 * It identifies and sets the list of all the edges (non-directed) that depart from this node.
	 *
	 */
	private void setEdgesNode() {
		ArrayList<EdgeGraph> edges = new ArrayList<EdgeGraph>();
		List<Object> out = new ArrayList<Object>(this.getOutEdges().getEdges());

		for (Object o : out) {
			EdgeGraph edge = (EdgeGraph) ((GeomPlanarGraphDirectedEdge) o).getEdge();
			if (!edges.contains(edge)) edges.add(edge);
		}
		this.edges = edges;
	}

	/**
	 * It identifies a list of all the adjacent nodes to this node (i.e. sharing an edge with this node).
	 * @return
	 *
	 */
	private void setAdjacentNodes()
	{
		ArrayList<NodeGraph> adjacentNodes = new ArrayList<NodeGraph>();

		for (EdgeGraph e : this.edges) {
			NodeGraph opposite = (NodeGraph) e.getOppositeNode(this);
			adjacentNodes.add(opposite);
		}
		this.adjacentNodes = adjacentNodes;
	}

	/**
	 * It returns the EdgeGraph between this and another node.
	 *
	 * @param otherNode an other node;
	 */
	public EdgeGraph getEdgeWith(NodeGraph otherNode) {
		EdgeGraph edge = null;
		Collection connectingEdge = getEdgesBetween(this, otherNode);
		for (Object o : connectingEdge) edge = (EdgeGraph) o;
		return edge;
	}

	/**
	 * It returns the directed edge between this and another node.
	 *
	 * @param otherNode an other node;
	 */
	public GeomPlanarGraphDirectedEdge getDirectedEdgeWith(NodeGraph otherNode) {
		GeomPlanarGraphDirectedEdge edge = null;
		for (Object e: this.getOutEdges().getEdges()) if (((DirectedEdge) e).getToNode() == otherNode) edge = (GeomPlanarGraphDirectedEdge) e;
		return edge;
	}


	/**
	 * It returns a list of integer representing all the adjacent regions to this node, if any.
	 *
	 */
	public ArrayList<Integer> getAdjacentRegion()
	{
		if (!this.gateway) return null;

		ArrayList<NodeGraph> oppositeNodes = new ArrayList<NodeGraph>(this.adjacentNodes);
		ArrayList<Integer> adjacentRegions = new ArrayList<Integer>();

		for (NodeGraph opposite : oppositeNodes) {
			int region = opposite.region;
			if (region != this.region) {
				adjacentRegions.add(region);
				adjacentEntries.add(opposite);
			}
		}

		return adjacentRegions;
	}

	/**
	 * It returns a LineString between two given nodes.
	 *
	 * @param node a node;
	 * @param otherNode an other node;
	 */
	public static LineString LineStringBetweenNodes(NodeGraph node, NodeGraph otherNode) {
		Coordinate[] coords = {node.getCoordinate(), otherNode.getCoordinate()};
		LineString line = new GeometryFactory().createLineString(coords);
		return line;
	}

	/**
	 * It generates the smallest enclosing circle between two given nodes.
	 *
	 * @param node a node;
	 * @param otherNode an other node;
	 */
	public static Geometry nodesEnclosingCircle(NodeGraph node, NodeGraph otherNode) {
		LineString line = LineStringBetweenNodes(node, otherNode);
		Point centroid = line.getCentroid();
		Geometry smallestEnclosingCircle = centroid.buffer(line.getLength()/2);
		return smallestEnclosingCircle;
	}

	/**
	 * It returns the Euclidean distance between two nodes.
	 *
	 * @param node a node;
	 * @param otherNode an other node;
	 */
	public static double nodesDistance(NodeGraph node, NodeGraph otherNode) {
		Coordinate originCoord = node.getCoordinate();
		Coordinate destinationCoord = otherNode.getCoordinate();
		return 	Utilities.euclideanDistance(originCoord, destinationCoord);

	}

	/**
	 * It returns a node in the dual representation of the graph, which represents a segment departing from this node (a node in a dual graph represents
	 * an actual street segment).
	 * When the node for which the dual-node is desired the originNode of a desired route, the segment closest to the destination is chosen (~ towards it).
	 * When the node for which the dual-node is desired is the destinationNode, the segment closest to the origin is chosen.
	 *
	 * @param originNode the originNode of the desired route;
	 * @param destinationNode the originNode of the desired route;
	 * @param regionBasedNavigation if the agent is navigating through regions;
	 * @param previousJunction to avoid to chose dual nodes representing segments that should not be traversed;
	 */
	public NodeGraph getDualNode(NodeGraph originNode, NodeGraph destinationNode, boolean regionBasedNavigation, NodeGraph previousJunction)
	{
		double distance = Double.MAX_VALUE;
		NodeGraph best = null;
		NodeGraph dualNode = null;
		ArrayList<EdgeGraph> edges = new ArrayList<EdgeGraph>(this.edges);
		double cost;

		for (EdgeGraph edge : edges) {
			if (edge.region == 999999 && regionBasedNavigation) continue;
			dualNode = edge.getDual();
			if (dualNode == null) continue;

			if (this.equals(destinationNode)) {
				cost = nodesDistance(edge.getOtherNode(this), originNode);
				if (cost < distance) {
					distance = cost;
					best = dualNode;
				}
			}
			else {
				cost = nodesDistance(edge.getOtherNode(this), destinationNode);

				if ((previousJunction != null) && ((previousJunction == dualNode.primalEdge.u) || (previousJunction == dualNode.primalEdge.v))) continue;
				if (regionBasedNavigation) {
					ArrayList<EdgeGraph> nextEdges =  new ArrayList<EdgeGraph>(edge.getOtherNode(originNode).getEdges());
					nextEdges.remove(edge);
					boolean bridges = true;
					for (EdgeGraph next : nextEdges) if (next.region != 999999) bridges = false;
					if (bridges) continue;
				}
				if (cost < distance) {
					distance = cost;
					best = dualNode;
				}
			}
		}
		return best;
	}

	/**
	 * It returns a map of nodes in the dual representation of the graph, which represent segments departing from this node (a node in a dual graph represents
	 * an actual street segment).
	 * When the node for which the dual-nodes are desired the originNode of a desired route, the map is sorted on the basis of the distance from the dual nodes
	 * (segments' centroids) to the destination node (~ towards it).
	 * When the node for which the dual-nodes are desired is the destinationNode, the map is sorted on the basis of the distance from the dual nodes
	 * (segments' centroids) to the origin node.
	 *
	 * This function is preferable to the one above as it allows considering multiple segments as possible "departures" for route planning algorithms.
	 * When computing paths within subgraphs, some specific segments may be indeed unreachable.
	 *
	 * @param originNode the originNode of the desired route;
	 * @param destinationNode the originNode of the desired route;
	 * @param regionBasedNavigation if the agent is navigating through regions;
	 * @param previousJunction to avoid to chose dual nodes representing segments that should not be traversed;
	 */
	public Map<NodeGraph, Double> getDualNodes(NodeGraph originNode, NodeGraph destinationNode, boolean regionBasedNavigation, NodeGraph previousJunction)
	{
		HashMap<NodeGraph, Double> dualNodes = new HashMap<NodeGraph, Double>();
		NodeGraph dualNode = null;
		ArrayList<EdgeGraph> edges = new ArrayList<EdgeGraph>(this.edges);

		for (EdgeGraph edge : edges) {
			if (edge.region == 999999 && regionBasedNavigation) continue;
			dualNode = edge.getDual();
			if (dualNode == null) continue;

			if (this.equals(destinationNode)) {
				double cost = nodesDistance(edge.getOtherNode(this), originNode);
				dualNodes.put(dualNode, cost);
			}
			else {
				double cost = nodesDistance(edge.getOtherNode(this), destinationNode);

				if ((previousJunction != null) && ((previousJunction == dualNode.primalEdge.u) || (previousJunction == dualNode.primalEdge.v))) continue;
				if (regionBasedNavigation) {
					ArrayList<EdgeGraph> nextEdges =  new ArrayList<EdgeGraph>(edge.getOtherNode(originNode).getEdges());
					nextEdges.remove(edge);
					boolean bridges = true;
					for (EdgeGraph next : nextEdges) if (next.region != 999999) bridges = false;
					if (bridges) continue;
				}
				dualNodes.put(dualNode, cost);
			}
		}

		return Utilities.sortByValue(dualNodes, false);
	}
}

