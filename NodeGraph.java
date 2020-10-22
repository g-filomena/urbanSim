package sim.app.geo.urbanSim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.planargraph.DirectedEdge;
import com.vividsolutions.jts.planargraph.Node;

import sim.util.geo.AttributeValue;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.MasonGeometry;

/**
 * A Node with further functions.
 *
 */
public class NodeGraph extends Node
{

	public NodeGraph(Coordinate pt) {super(pt);}

	public int nodeID, region;
	public boolean gateway;

	public MasonGeometry masonGeometry;
	public EdgeGraph primalEdge;
	public double centrality, centrality_sc;

	public List<Integer> visible2d = new ArrayList<Integer>();
	public List<Integer> localLandmarks = new ArrayList<Integer>();
	public List<Double> localScores = new ArrayList<Double>();
	public List<Integer> distantLandmarks  = new ArrayList<Integer>();
	public List<Double> distantScores = new ArrayList<Double>();
	public List<Integer> anchors = new ArrayList<Integer>();
	public List<Double> distances = new ArrayList<Double>();

	public ArrayList<Integer> adjacentRegions = new ArrayList<Integer>();
	public ArrayList<NodeGraph> adjacentEntries = new ArrayList<NodeGraph>();
	public ArrayList<NodeGraph> adjacentNodes = new ArrayList<NodeGraph>();
	public ArrayList<EdgeGraph> edges = new ArrayList<EdgeGraph>();
	public ArrayList<GeomPlanarGraphDirectedEdge> outEdges = new ArrayList<GeomPlanarGraphDirectedEdge>();
	private Map<String, AttributeValue> attributes;

	public void setID(int nodeID) {
		this.nodeID = nodeID;
	}

	public Integer getID() {
		return this.nodeID;
	}

	public Object getAttribute(final String name) {
		return this.attributes.get(name);
	}

	public Integer getIntegerAttribute(final String name) {
		return this.attributes.get(name).getInteger();
	}

	public Double getDoubleAttribute(final String name) {
		return this.attributes.get(name).getDouble();
	}

	public String getStringAttribute(final String name) {
		return this.attributes.get(name).getString();
	}

	/**
	 * It set the landmarkness values/attributes of this node, when the MasonGeometry it's passed.
	 *
	 * @param geometry the MasonGeometry of this node.
	 */

	public void setLandmarkness(MasonGeometry geometry) {

		String localString = geometry.getStringAttribute("loc_land");
		String lScoresString = geometry.getStringAttribute("loc_scor");
		String distantString = geometry.getStringAttribute("dist_land");
		String dScoresString = geometry.getStringAttribute("dist_scor");
		String anchorsString = geometry.getStringAttribute("anchors");
		String distancesString = geometry.getStringAttribute("distances");

		if (!localString.equals("[]")) {
			String l = localString.replaceAll("[^-?0-9]+", " ");
			String s = lScoresString.replaceAll("[^0-9.]+", " ");
			for(String t : (Arrays.asList(l.trim().split(" ")))) this.localLandmarks.add(Integer.valueOf(t));
			for(String t : (Arrays.asList(s.trim().split(" ")))) this.localScores.add(Double.valueOf(t));
		}
		else {
			this.localLandmarks = null;
			this.localScores = null;
		}

		if (!distantString.equals("[]")) {
			String l = distantString.replaceAll("[^-?0-9]+", " ");
			String s = dScoresString.replaceAll("[^0-9.]+", " ");
			for(String t : (Arrays.asList(l.trim().split(" ")))) this.distantLandmarks.add(Integer.valueOf(t));
			for(String t : (Arrays.asList(s.trim().split(" ")))) this.distantScores.add(Double.valueOf(t));
		}
		else {
			this.distantLandmarks = null;
			this.distantScores = null;
		}

		if (!anchorsString.equals("[]")) {
			String l = anchorsString.replaceAll("[^-?0-9]+", " ");
			String d = distancesString.replaceAll("[^0-9.]+", " ");
			for(String t : (Arrays.asList(l.trim().split(" ")))) this.anchors.add(Integer.valueOf(t));
			for(String t : (Arrays.asList(d.trim().split(" ")))) this.distances.add(Double.valueOf(t));
		}
		else this.anchors = null;
	}


	public EdgeGraph getEdgeBetween(NodeGraph v) {
		EdgeGraph edge = null;
		Collection connectingEdge = NodeGraph.getEdgesBetween(this, v);
		for (Object o : connectingEdge) edge = (EdgeGraph) o;
		return edge;
	}

	public ArrayList<EdgeGraph> getEdges() {
		return this.edges;
	}

	public ArrayList<GeomPlanarGraphDirectedEdge> getOutDirectedEdges() {
		return this.outEdges;
	}

	public void setNeighboouringComponents() {
		this.edges = getEdgesNode();
		this.adjacentNodes = getAdjacentNodes();
		List<DirectedEdge> outEdges  = this.getOutEdges().getEdges();
		for (DirectedEdge e: outEdges) this.outEdges.add((GeomPlanarGraphDirectedEdge) e);
	}

	public GeomPlanarGraphDirectedEdge getDirectedEdgeBetween(NodeGraph v) {
		GeomPlanarGraphDirectedEdge edge = null;
		for (Object e: this.getOutEdges().getEdges()) if (((DirectedEdge) e).getToNode() == v) edge = (GeomPlanarGraphDirectedEdge) e;
		return edge;
	}

	private ArrayList<EdgeGraph> getEdgesNode() {
		ArrayList<EdgeGraph> edges = new ArrayList<EdgeGraph>();
		List<Object> out = this.getOutEdges().getEdges();
		Set<Object> outEdges = new HashSet<Object>(out);

		for (Object o : outEdges) {
			EdgeGraph edge = (EdgeGraph) ((GeomPlanarGraphDirectedEdge) o).getEdge();
			edges.add(edge);
		}
		return edges;
	}


	public ArrayList<NodeGraph> getAdjacentNodes()
	{
		ArrayList<NodeGraph> adjacentNodes = new ArrayList<NodeGraph>();
		ArrayList<EdgeGraph> edges = getEdgesNode();

		for (EdgeGraph e : edges) {
			NodeGraph opposite = (NodeGraph) e.getOppositeNode(this);
			adjacentNodes.add(opposite);
		}
		return adjacentNodes;
	}

	public ArrayList<Integer> getAdjacentRegion()
	{
		if (!this.gateway) return null;

		ArrayList<NodeGraph> oppositeNodes = new ArrayList<NodeGraph>(this.getAdjacentNodes());
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


	public NodeGraph getDualNode(NodeGraph originNode, NodeGraph destinationNode, boolean regionBasedNavigation, NodeGraph previousJunction)
	{
		NodeGraph dualNode = null;
		double distance = Double.MAX_VALUE;
		NodeGraph best = null;

		for (EdgeGraph edge : this.getEdgesNode()) {

			if ((edge.region == 999999) && (regionBasedNavigation)) continue; // bridge between regions
			dualNode = edge.getDual();
			if (dualNode == null) continue;

			if (this != destinationNode) {
				double cost = Utilities.nodesDistance(dualNode, destinationNode);
				if ((previousJunction != null) && ((previousJunction == dualNode.primalEdge.u) ||
						(previousJunction == dualNode.primalEdge.v))) continue;

				if (cost < distance) {
					distance = cost;
					best = dualNode;
				}
			}
			else
			{
				double cost = Utilities.nodesDistance(dualNode, originNode);
				if (cost < distance) {
					distance = cost;
					best = dualNode;
				}
			}
		}
		return best;
	}
}

