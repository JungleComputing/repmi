package applications.util;

/**
 *  
 * Geometric distance table. Coordinates are londitude/lattitude on a sphere. 
 *  
 * @author Jason Maassen
 * @version 1.0 May 23, 2005
 * @since 1.0
 * 
 */
public class GeoDistanceTable extends DistanceTable {
    
    private static final double M_PI=3.14159265358979323846264;
    
    private final double [] coordinates;    
        
    protected GeoDistanceTable(String name, int cities, double [] coordinates) {
        super(name, cities, false, false);
        this.coordinates = coordinates;         
    }
/*    
    public int distance(int from, int to) { 
        
        double lati = M_PI * coordinates[2*from] / 180.0;
        double latj = M_PI * coordinates[2*to] / 180.0;
        
        double longi = M_PI * coordinates[2*from+1] / 180.0;
        double longj = M_PI * coordinates[2*to+1] / 180.0;
        
        double q1 = Math.cos(latj) * Math.sin(longi - longj);
        double q3 = Math.sin((longi - longj)/2.0);
        double q4 = Math.cos((longi - longj)/2.0);
        double q2 = Math.sin(lati + latj) * q3 * q3 - Math.sin(lati - latj) * q4 * q4;
        double q5 = Math.cos(lati - latj) * q4 * q4 - Math.cos(lati + latj) * q3 * q3;
        return (int) (6378388.0 * Math.atan2(Math.sqrt(q1*q1 + q2*q2), q5) + 1.0);    
    }
    */
    
    /*
     * Q: I get wrong distances for problems of type GEO.
     * 
     * A: There has been some confusion of how to compute the distances. I use 
     * the following code.
     * 
     * For converting coordinate input to longitude and latitude in radian:
     * 
     * PI = 3.141592;
     * 
     * deg = (int) x[i];
     * min = x[i]- deg;
     * rad = PI * (deg + 5.0 * min/ 3.0) / 180.0;
     * 
     * For computing the geographical distance:
     * 
     * RRR = 6378.388;
     * 
     * q1 = cos( longitude[i] - longitude[j] );
     * q2 = cos( latitude[i] - latitude[j] );
     * q3 = cos( latitude[i] + latitude[j] );
     * dij = (int) ( RRR * acos( 0.5*((1.0+q1)*q2 - (1.0-q1)*q3) ) + 1.0); 
     * 
     */
    
    protected static double toRad(double in) { 
        double deg = (int) in;
        double min = in - deg;
        return M_PI * (deg + 5.0 * min/ 3.0) / 180.0;       
    }
    
    protected int calculateDistance(int from, int to) { 
                       
        double lati = toRad(coordinates[2*from]);
        double latj = toRad(coordinates[2*to]);
        
        double longi = toRad(coordinates[2*from+1]);
        double longj = toRad(coordinates[2*to+1]);
        
        double q1 = Math.cos(longi - longj);
        double q2 = Math.cos(lati - latj);
        double q3 = Math.cos(lati + latj);
        
        return (int) (6378.388 * Math.acos(0.5*((1.0+q1)*q2-(1.0-q1)*q3))+1.0);        
    }
    
}
