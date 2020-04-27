package sim.app.geo.urbanSim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

import sim.util.geo.AttributeValue;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;



/** A planar graph edge that wraps a LineString
*
* XXX Should consider making this an internal class to GeomPlanarGraph?
*/
public class EdgeGraph extends GeomPlanarGraphEdge
{
	public int district;
	int roadType;
	int edgeID;
	double distanceScaled;
	public MasonGeometry masonGeometry;
	
	public int roadDistance, angularChange, roadDistanceLandmarks, angularChangeLandmarks, topological;
	public int localLandmarks, globalLandmarks;
	public int roadDistanceRegions, angularChangeRegions, roadDistanceBarriers, angularChangeBarriers, 
		roadDistanceRegionsBarriers, angularChangeRegionsBarriers;

	public NodeGraph u, v;
	public NodeGraph dualNode;
	
	public List<Integer> positiveBarriers = new ArrayList<Integer>();
	public List<Integer> negativeBarriers = new ArrayList<Integer>();
	List<Integer> rivers = new ArrayList<Integer>();
	List<Integer> parks = new ArrayList<Integer>();
//	boolean pedestrian;
	
	public Map<String, AttributeValue> attributes;
	
    public void setAttributes(final Map<String, AttributeValue> attributes)
    {
        this.attributes = attributes;
    }
	
	public EdgeGraph(LineString line)
	{
		super(line);
	}

	public void setNodes(final NodeGraph u, final NodeGraph v)
	{
		this.u = u;
		this.v = v;
	}
	
    public void setID(int edgeID)
    {
        this.edgeID = edgeID;
    }
    
    public Integer getID()
    {
        return this.edgeID;
    }
    
    public NodeGraph getDual()
    {
        return this.dualNode;
    }
    
    public double getLength()
    {
        return this.getLine().getLength();
    }
    
    public double getDeflectionAngle() //only for dual edges
    {
        return this.getDoubleAttribute("deg");
    }
    
    public Coordinate getCoordsCentroid() //only for dual edges
    {
    	return this.getLine().getCentroid().getCoordinate();
    }
    
    
    public void setBarriers()
    {
		String pBarriersString = this.getStringAttribute("p_barr");
		String nBarriersString = this.getStringAttribute("n_barr");
		String riversString = this.getStringAttribute("a_rivers");
		String parksString = this.getStringAttribute("aw_parks");
	
		///// ASSIGNING TO EACH NODE LANDMARKS AT THE JUNCTION
		if (!pBarriersString.equals("[]"))
		{
			String p = pBarriersString.replaceAll("[^-?0-9]+", " ");
	    	for(String t : (Arrays.asList(p.trim().split(" ")))) this.positiveBarriers.add(Integer.valueOf(t));
		}
		else this.positiveBarriers = null;
		if (!nBarriersString.equals("[]"))
		{
			String n = nBarriersString.replaceAll("[^-?0-9]+", " ");
			for(String t : (Arrays.asList(n.trim().split(" ")))) this.negativeBarriers.add(Integer.valueOf(t));
		}
		else this.negativeBarriers = null;
		
	   	if (!riversString.equals("[]"))
		{
			String r = riversString.replaceAll("[^-?0-9]+", " ");
			for(String t : (Arrays.asList(r.trim().split(" ")))) this.rivers.add(Integer.valueOf(t));
		}
		else this.rivers = null;
	   	
	   	if (!parksString.equals("[]"))
		{
			String p = parksString.replaceAll("[^-?0-9]+", " ");
			for(String t : (Arrays.asList(p.trim().split(" ")))) this.parks.add(Integer.valueOf(t));
		}
    }
    
    public Integer getIntegerAttribute(final String name)
    {
        return this.attributes.get(name).getInteger();
    }

    public Double getDoubleAttribute(final String name)
    {
        return this.attributes.get(name).getDouble();
    }
    
    public String getStringAttribute(final String name)
    {
        return this.attributes.get(name).getString();
    }
    
    
    public NodeGraph getOtherNode(NodeGraph node)
    {
    	if (this.u == node) return this.v;
    	else if (this.v == node) return this.u;
    	else return null;
    }
    

}