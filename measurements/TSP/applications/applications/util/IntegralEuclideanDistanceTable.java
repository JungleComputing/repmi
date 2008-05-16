package applications.util;

/**
 * Euclidean distance table for integral coordinates.  
 *  
 * @author Jason Maassen
 * @version 1.0 May 23, 2005
 * @since 1.0
 * 
 */
public class IntegralEuclideanDistanceTable extends DistanceTable {

    private int [] coordinates;
    
    protected IntegralEuclideanDistanceTable(String name, int cities, 
            int [] coordinates) { 
        
        super(name, cities, true, true);
        this.coordinates = coordinates;        
    }
    
    protected int calculateDistance(int from, int to) {

        if (from <= 0 || to <= 0 || from > cities || to > cities) { 
            throw new Error("Illegal city used in distance table! " + from + 
                    " " + to);
        }
        
        int x1 = coordinates[2*from];
        int y1 = coordinates[2*from+1];
        
        int x2 = coordinates[2*to];
        int y2 = coordinates[2*to+1];
        
        int dx = x1-x2;
        int dy = y1-y2;
                             
        int res = (int) Math.ceil(Math.sqrt(dx*dx + dy*dy));
        
        System.out.println(from + " -> " + to + " == " + res);
        return res;
    }
}
