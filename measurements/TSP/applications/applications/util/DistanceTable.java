package applications.util;

import java.util.Arrays;

/**
 *  
 * Base class for the distance tables.  
 *  
 * @author Jason Maassen
 * @version 1.0 May 23, 2005
 * @since 1.0
 * 
 */
public abstract class DistanceTable {
        
    private static final int MATRIX_THRESHOLD = 40;
    
    public final String name;     
    public final int cities;
    public final boolean euclidean;
    public final boolean integral;
    
    protected int [][] distances;    
    protected int [][] closest;
        
    protected DistanceTable(String name, int cities, boolean euclidean, 
            boolean integral) {
        this.name = name;
        this.cities = cities;        
        this.euclidean = euclidean;
        this.integral = integral;
        
        // See if the problem is small enough to cache te results
        if (cities < MATRIX_THRESHOLD) {         
            this.distances = new int[cities][cities];
                    
            for (int i=0;i<cities;i++) { 
                Arrays.fill(distances[i], -1);
            }
        }
    }
    
    private void sort(int [] cit, int [] dist) {

        for (int i = 0; i < cit.length - 1; i++) {
            for (int j = 0; j < cit.length - 1 - i; j++) {
                if (dist[j + 1] < dist[j]) {
                    int tmp = dist[j + 1];
                    dist[j + 1] = dist[j];
                    dist[j] = tmp;

                    int ta = cit[j + 1];
                    cit[j + 1] = cit[j];
                    cit[j] = ta;
                }
            }
        }
    }

    private void fillClosest() { 
        // Not very efficient ?        
        int [] distance = new int[cities-1];
        
        for (int i=1;i<=cities;i++) {
            int next = 0;
            int [] res = new int[cities-1];
            
         //   System.out.print("Handling " + i + " -> [");
                        
            for (int j=1;j<=cities;j++) { 
                if (j != i) { 
                    res[next] = j;
                    distance[next] = distance(i, j);
                    
           //         System.out.print("" + j + " (" + distance[next] + ") ");
                    
                    next++;                                        
                }
            }
            
//            System.out.println("]");
            
            sort(res, distance);
            closest[i-1] = res;                        
        }    
       /*
        for (int i=1;i<=cities;i++) { 
            System.out.print("Closest to " + i + " -> [");
        
            int [] tmp = closest[i-1];
            
            for (int j=0;j<tmp.length;j++) { 
                
                int city = tmp[j];
                int dist = distance(i, city);
                
                System.out.print("" + city + " (" + dist + ") ");            
            }
            System.out.println("]");
        }
        */
    }
    
    public int distance(int from, int to) { 
                
        if (from <= 0 || to <= 0 || from > cities || to > cities) { 
            throw new Error("Illegal city used in distance table!");
        }
                
        if (cities < MATRIX_THRESHOLD) {         
            int result = distances[from-1][to-1];
        
            if (result != -1) { 
                return result;
            } 
        
            result = calculateDistance(from, to);        
            distances[from-1][to-1] = result;
            return result;
        } 
        
        return calculateDistance(from, to);
    }
               
    public int [] closest(int from) { 
        
        // TODO: Also use MATRIX_THRESHOLD here ?
        if (closest == null) {
            closest = new int[cities][];                
            fillClosest();            
        }
        
        return closest[from-1];
    } 
    
    
    protected abstract int calculateDistance(int from, int to);            
}
