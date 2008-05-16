package applications.util;

/**
 *  
 * Euclidean distance table for non-integral coordinates.  
 *  
 * @author Jason Maassen
 * @version 1.0 May 23, 2005
 * @since 1.0
 * 
 */
public class EucledeanDistanceTable extends DistanceTable {

    private double [] coordinates;
    
    protected EucledeanDistanceTable(String name, int cities, double [] coordinates) {
        super(name, cities, true, false);
        this.coordinates = coordinates;        
    }
    
    protected int calculateDistance(int from, int to) {
        
        if (from <= 0 || to <= 0 || from > cities || to > cities) { 
            throw new Error("Illegal city used in distance table!");
        }
        
        double x1 = coordinates[2*from];
        double y1 = coordinates[2*from+1];
        
        double x2 = coordinates[2*to];
        double y2 = coordinates[2*to+1];
        
        double dx = x1-x2;
        double dy = y1-y2;
                             
        return (int) Math.ceil(Math.sqrt(dx*dx + dy*dy));
    }
}
