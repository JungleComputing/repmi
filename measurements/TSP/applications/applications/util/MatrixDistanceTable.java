package applications.util;

/**
 *  
 * Distance table based on distance matrix. Only works for small problems.  
 *  
 * @author Jason Maassen
 * @version 1.0 May 23, 2005
 * @since 1.0
 * 
 */
public final class MatrixDistanceTable extends DistanceTable {

    protected MatrixDistanceTable(String name, int cities, int [][] distances) {
        super(name, cities, true, true);
        
        // Just set the distances matrix of the parent class
        this.distances = distances;
    }
    
    protected int calculateDistance(int from, int to) {                      
        return distances[from][to];
    }
}
