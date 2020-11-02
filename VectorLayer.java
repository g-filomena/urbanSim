package sim.app.geo.urbanSim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

import sim.field.geo.GeomVectorField;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;

public class VectorLayer extends GeomVectorField {

	private static final long serialVersionUID = 1L;

	/** Helper factory for computing the union or convex hull */

	public VectorLayer()
	{
		super();
	}

	public final Bag intersectingFeatures(Geometry inputGeometry) {
		Bag intersectingObjects = new Bag();
		Envelope e = inputGeometry.getEnvelopeInternal();
		e.expandBy(java.lang.Math.max(e.getHeight(),e.getWidth()) * 0.01 );
		List<?> gList = spatialIndex.query(e);

		for (Object o: gList) {
			MasonGeometry mg = (MasonGeometry) o;
			Geometry otherGeo = mg.geometry;
			if (inputGeometry.intersects(otherGeo)) intersectingObjects.add(mg);
		}
		return intersectingObjects;
	}

	public final Bag containedFeatures(Geometry inputGeometry) {
		Bag containedObjects = new Bag();
		Envelope e = inputGeometry.getEnvelopeInternal();
		e.expandBy(java.lang.Math.max(e.getHeight(),e.getWidth()) * 0.01 );
		List<?> gList = spatialIndex.query(e);

		for (Object o: gList) {
			MasonGeometry mg = (MasonGeometry) o;
			Geometry otherGeo = mg.geometry;
			if (inputGeometry.contains(otherGeo)) containedObjects.add(mg);
		}
		return containedObjects;
	}

	public Bag featuresBetweenLimits(Geometry inputGeometry, double l_lim, double u_lim) {
		Bag Objects = new Bag();
		Envelope e = inputGeometry.getEnvelopeInternal();
		e.expandBy(u_lim);
		List<?> gList = spatialIndex.query(e);

		for (Object o: gList) {
			MasonGeometry mg = (MasonGeometry) o;
			if ((inputGeometry.distance(mg.getGeometry()) >= l_lim) & (inputGeometry.distance(mg.getGeometry()) <= u_lim))
				Objects.add(mg);
			else continue;
		}
		return Objects;
	}

	public Bag featuresWithinDistance(Geometry inputGeometry, double radius)
	{
		Bag nearbyObjects = new Bag();
		Envelope e = inputGeometry.getEnvelopeInternal();
		e.expandBy(radius);

		List<?> gList = spatialIndex.query(e);

		for (Object o: gList) {
			MasonGeometry mg = (MasonGeometry) o;

			if (inputGeometry.isWithinDistance(mg.getGeometry(), radius)) nearbyObjects.add(mg);
		}

		return nearbyObjects;
	}


	public Bag filterFeatures(String attributeName, int attributeValue, boolean equal) {

		Bag Objects = new Bag();

		for (Object o : geometries) {
			MasonGeometry mg = (MasonGeometry) o;
			Integer attribute = mg.getIntegerAttribute(attributeName);
			if (!equal && !attribute.equals(attributeValue)) Objects.add(mg);
			else if (attribute.equals(attributeValue)) Objects.add(mg);
		}
		return Objects;
	}

	public Bag filterFeatures(String attributeName, String attributeValue, boolean equal) {
		Bag Objects = new Bag();

		for (Object o : geometries) {
			MasonGeometry mg = (MasonGeometry) o;
			String attribute = mg.getStringAttribute(attributeName);
			if (!equal && !attribute.equals(attributeValue)) Objects.add(mg);
			else if (attribute.equals(attributeValue)) Objects.add(mg);
		}
		return Objects;
	}


	public Bag filterFeatures(String attributeName, List<String> listValues, boolean equal) {
		Bag Objects = new Bag();

		for (Object o : geometries) {
			MasonGeometry mg = (MasonGeometry) o;
			String attribute = mg.getStringAttribute(attributeName);
			if (!equal && !listValues.contains(attribute)) Objects.add(mg);
			else if (listValues.contains(attribute)) Objects.add(mg);
		}
		return null;
	}

	public List<Integer> getIntColumn(String attributeName) {
		List<Integer> values = new ArrayList<Integer>();

		for (Object o : geometries) {
			MasonGeometry mg = (MasonGeometry) o;
			Integer attribute = mg.getIntegerAttribute(attributeName);
			values.add(attribute);
		}
		return values;
	}


	public VectorLayer selectFeatures(String attributeName, List<Integer> listValues, String method) {
		Bag Objects = new Bag();

		for (Object o : geometries) {
			MasonGeometry mg = (MasonGeometry) o;
			Integer attribute = mg.getIntegerAttribute(attributeName);
			if (method.equals("different")) {
				if (!listValues.contains(attribute)) Objects.add(mg);
			}
			else if (method.equals("equal")) {
				if (listValues.contains(attribute)) Objects.add(mg);
			}
		}
		VectorLayer newLayer = new VectorLayer();

		for (Object o : Objects) {
			MasonGeometry f = (MasonGeometry) o;
			newLayer.addGeometry(f);
		}
		return newLayer;
	}



	public Geometry layerConvexHull() {


		ArrayList<Coordinate> pts = new ArrayList<Coordinate>();

		for (Object o : geometries) {
			Geometry g = ((MasonGeometry) o).geometry;
			Coordinate c[] = g.getCoordinates();
			pts.addAll(Arrays.asList(c));
		}

		Coordinate[] coords = pts.toArray(new Coordinate[pts.size()]);
		ConvexHull convexHull = new ConvexHull(coords, this.geomFactory);
		return convexHull.getConvexHull();
	}

	public void setID(String attributeName) {

		for (Object o : geometries) {
			MasonGeometry mg = ((MasonGeometry) o);
			mg.setUserData(mg.getIntegerAttribute(attributeName));
		}
	}



}
