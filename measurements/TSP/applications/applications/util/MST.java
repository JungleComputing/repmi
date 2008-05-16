package applications.util;

import java.io.PrintWriter;
import java.util.Arrays;


/**
 * Simple test that determines the Minimal Spanning Tree (MST). 
 * Uses an N^3 algorithm, so only works for small problems. 
 *  
 * @author Jason Maassen
 * @version 1.0 May 23, 2005
 * @since 1.0
 * 
 */
public class MST {

    private final DistanceTable table;
    
    private int [] resultCache;         
    private int [] unusedCities;
    
    // Statistics
    private long hits = 0;
    private long lookups = 0;
    private long totalTime = 0;
    private int entries = 0;    
    
    public MST(DistanceTable table) { 
            this.table = table;            
            this.unusedCities = new int[table.cities];
            
            int size = (1 << table.cities);
            this.resultCache = new int[size];
            Arrays.fill(resultCache, -1);
    }
    
    private int getUnusedCities(boolean [] usedCities, int endCity) { 

        // Note: the currentCity is automatically included, but the end city 
        // may not be.
        int num = 0;
        
        for (int i=1;i<=table.cities;i++) {            
            if (!usedCities[i] || (i == endCity)) {
                num++;
            }
        }
        
        int next = 0;
        
        for (int i=1;i<=table.cities;i++) {            
            if (!usedCities[i] || (i == endCity)) {
                unusedCities[next++] = i;
            }
        }
        
        return num;        
    }

    private int getIndex(boolean [] usedCities, int end) { 
        
        int index = (1 << (end-1));
        
        for (int i=1;i<=table.cities;i++) {            
            if (!usedCities[i]) {
                index |= (1 << (i-1));
            }
        }
        
        return index;
    }
                
    public int getLength(boolean [] usedCities, int end) { 
        
        // try a cache lookup first!
        int index = getIndex(usedCities, end);

        lookups++;
        
        if (resultCache[index] >= 0) {
            hits++;
            return resultCache[index];
        }
        
        long time = System.currentTimeMillis();
        
        final int numUnusedCities = getUnusedCities(usedCities, end);
        
        int length = 0;
        int num = 1;   
        
        while (num < numUnusedCities) {         
            int dst = num;
            int shortest = Integer.MAX_VALUE;
            
            for (int i=0;i<num;i++) {                              
             
                for (int j=num;j<numUnusedCities;j++) { 
                    
                    int tmp = table.distance(unusedCities[i], unusedCities[j]);
                           
                    if (tmp < shortest) { 
                        shortest = tmp;
                        dst = j;
                    } else if (tmp == shortest) {                        
                        // If its a tie, we prefer the destination city with 
                        // the lowest number. This way, the outcome does not 
                        // depend on sorting order
                        
                        if (unusedCities[j] < unusedCities[dst]) { 
                            dst = j;
                        }
                    }
                }
            }
            
            length += shortest;
            
            // Add the used city
            if (dst != num) {
                int tmp = unusedCities[num]; 
                unusedCities[num] = unusedCities[dst];
                unusedCities[dst] = tmp;                
            }
            
            num++;            
        }
        
        entries++;
        resultCache[index] = length;
        
        time = System.currentTimeMillis() - time;
        
        totalTime += time;
     
        return length;
    }    
    
    public void printStatistics(PrintWriter out) { 
        out.println("Spanning tree statistics:");
        out.println(" - cache size   : " + resultCache.length);        
        out.println(" - cache entries: " + entries);        
        out.println(" - cache lookups: " + lookups);
        out.println(" - cache hits   : " + hits);
        out.println(" - total time   : " + (totalTime/1000));
    }
}
