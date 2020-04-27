package sim.app.geo.urbanSim;

import com.vividsolutions.jts.geom.Coordinate;

public class Angle {
	
	public static double dot (double [] vectorA, double [] vectorB) 
	{
	  return vectorA[0]*vectorB[0]+vectorA[1]*vectorB[1];
	}
	
	
	public static double angle(NodeGraph originNode, NodeGraph destinationNode) 
	{
		Coordinate origin = originNode.getCoordinate();
		Coordinate destination = destinationNode.getCoordinate();
	    double [] vectorA = {(origin.x-origin.x), (origin.y-(origin.y + 2000))};
	    double [] vectorB = {(origin.x-destination.x), (origin.y-destination.y)};
	    double dot_prod = dot(vectorA, vectorB);
	    double magA = Math.pow(dot(vectorA, vectorA), 0.5);
	    double magB = Math.pow(dot(vectorB, vectorB), 0.5);
	   
	    double anglRad = Math.acos(dot_prod/magB/magA);
	    double angleDeg = Math.toDegrees(anglRad)%360;
	    if (destination.x < origin.x) angleDeg = 180+(180-angleDeg);
		return angleDeg;
	
	}
	public static double angle(Coordinate origin, Coordinate destination) 
	{
	    double [] vectorA = {(origin.x-origin.x), (origin.y-(origin.y + 2000))};
	    double [] vectorB = {(origin.x-destination.x), (origin.y-destination.y)};
	    double dot_prod = dot(vectorA, vectorB);
	    double magA = Math.pow(dot(vectorA, vectorA), 0.5);
	    double magB = Math.pow(dot(vectorB, vectorB), 0.5);
	   
	    double anglRad = Math.acos(dot_prod/magB/magA);
	    double angleDeg = Math.toDegrees(anglRad)%360;
	    if (destination.x < origin.x) angleDeg = 180+(180-angleDeg);
		return angleDeg;
	
	}
	
	public static double differenceAngles(Double angleA, Double angleB) 
	{
		double difference = 0;
		//check if same quadrant
		if ((angleA <=180 & angleB <=180) || (angleA > 180 & angleB > 180)) difference = Math.abs(angleA-angleB);
		
		else if (angleA > 180 & angleB <= 180)
		{
			double tmpA = Math.abs(angleB-angleA);
		    double tmpB = Math.abs(angleA-(angleB+360));
			difference = Math.min(tmpA, tmpB);
		}
//		 (angleB > 180 & angleA <= 180)
		else
		{
			double tmpA = Math.abs(angleB-angleA);
		    double tmpB = Math.abs(angleB-(angleA+360));
			difference = Math.min(tmpA, tmpB);
		}
		return difference;
	
	}
	
	
    public static boolean isInDirection(double angleOD, double angleON, double cone)
    {	
    	    	
        double limitL = angleOD-(cone/2+1);
        double limitR = angleOD+(cone/2+1);
        if (limitL < 0) limitL = 360.0 + limitL;
        if (limitR > 360) limitR = limitR - 360.0;
	    
        if (((limitR > 180) & (angleON < limitL) & (angleON > 0) & (angleON < 180)) ||
        	((limitL > 180) & (limitR <= 180) & (angleON > limitR) & (angleON > limitL)) ||
        	((limitL > 180) & (limitR <= 180) & (angleON < limitR) & (angleON < limitL) & (angleON < 180)) ||
        	((limitL > 180) & (limitR > 180) & (angleON > 180) & (angleON > limitL) & (angleON < limitR)) ||
        	((limitL <= 180) & (limitR <= 180) & (angleON <= 180) & (angleON > limitL) & (angleON < limitR)) ||
        	((limitL <= 180) & (limitR > 180) & (angleON > limitL) & (angleON < limitR))) return true;
        else return false;
    }
	

}
