package sim.app.geo.urbanSim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedPolygon;

import sim.field.geo.GeomVectorField;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;

public class VectorLayer extends GeomVectorField {

	private static final long serialVersionUID = 1L;
	public PreparedPolygon convexHull;
	/** Helper factory for computing the union or convex hull */
	private GeometryFactory geomFactory;

	public final Bag getIntersecting(Geometry inputGeometry)
	{
		Bag intersectingObjects = new Bag();
		Envelope e = inputGeometry.getEnvelopeInternal();
		e.expandBy(java.lang.Math.max(e.getHeight(),e.getWidth()) * 0.01 );
		List<?> gList = spatialIndex.query(e);

		for (int i = 0; i < gList.size(); i++)
		{
			MasonGeometry gm = (MasonGeometry) gList.get(i);
			Geometry otherGeo = gm.getGeometry();
			if (inputGeometry.intersects(otherGeo)) intersectingObjects.add(gm);
		}
		return intersectingObjects;
	}

	public final Bag getContainedObjects(Geometry inputGeometry)
	{
		Bag containedObjects = new Bag();
		for (int i = 0; i < geometries.size(); i++)
		{
			MasonGeometry gm = (MasonGeometry) geometries.get(i);
			Geometry g1 = gm.getGeometry();
			if (inputGeometry.contains(g1)) containedObjects.add(gm);
		}
		return containedObjects;
	}

	public Bag getWithinObjects(final Geometry g, final double l_lim, final double u_lim)
	{
		Bag Objects = new Bag();
		Envelope e = g.getEnvelopeInternal();
		e.expandBy(u_lim);
		List<?> gList = spatialIndex.query(e);

		for (int i = 0; i < gList.size(); i++)
		{
			MasonGeometry tempGeometry = (MasonGeometry) gList.get(i);
			if ((g.distance(tempGeometry.getGeometry()) >= l_lim) & (g.distance(tempGeometry.getGeometry()) <= u_lim)) Objects.add(tempGeometry);
			else continue;
		}
		return Objects;
	}

	public Bag filter(String attributeName, Integer attributeValue, String method)
	{
		Bag Objects = new Bag();

		for (int i = 0; i < geometries.size(); i++)
		{
			MasonGeometry mg = (MasonGeometry) geometries.get(i);
			Integer attribute = mg.getIntegerAttribute(attributeName);
			if (method == "different")
			{
				if (attribute.equals(attributeValue) == false) Objects.add(mg);
			}
			else if (method == "equal")
			{
				if (attribute == attributeValue) Objects.add(mg);
			}
			else continue;
		}
		return Objects;
	}

	public Bag getWithinObjects(final MasonGeometry mg, final double l_lim, final double u_lim)
	{
		return getWithinObjects(mg.getGeometry(), l_lim, u_lim);
	}

	public PreparedPolygon getConvexHull()
	{
		ArrayList<Coordinate> pts = new ArrayList<Coordinate>();
		//        List<?> gList = spatialIndex.queryAll();


		// Accumulate all the Coordinates in all the geometry into 'pts'
		for (int i = 0; i < geometries.size(); i++)
		{
			Geometry g = ((MasonGeometry) geometries.get(i)).getGeometry();
			Coordinate c[] = g.getCoordinates();
			pts.addAll(Arrays.asList(c));
		}

		// ConvexHull expects a vector of Coordinates, so now convert
		// the array list of Coordinates into a vector
		Coordinate[] coords = pts.toArray(new Coordinate[pts.size()]);

		ConvexHull hull = new ConvexHull(coords, this.geomFactory);
		this.convexHull = new PreparedPolygon((Polygon) hull.getConvexHull());
		return convexHull;
	}

}
