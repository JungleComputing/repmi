package applications.tsp;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import applications.util.Dimacs;
import applications.util.DistanceTable;
import applications.util.MST;

/**
 * JOINC worker application that solves part of a larger Travelling Salesman 
 * Problem (TSP). 
 * 
 * This worker expects the following command line parameters (note that the 
 * exact number depends on the problem size): 
 *
 *   - file name of input file containing info on the cities
 *   - best solution found so far (by previous jobs) 
 *   - partial path (consisting of <count> <city1> <city2> ... <cityN>) 
 *  
 * @author Jason Maassen
 * @version 2.0 Feb 8, 2006
 * @since 1.0
 * 
 */
public final class TSPWorker {
    
    // Writer for the output file
    private static PrintWriter out;
    
    // Distance table containing info on the cities
    private final DistanceTable table; 
    
    // Best path found so far
    private final short [] best;
    
    // Current path 
    private final short [] path;
    
    // Cities used in current path
    private final boolean [] present;
    
    // Start city of our search  
    private final int start;
    
    // End city of our search  
    private final int end;
    
    // Number of cities in the initial path of our search  
    private final int initialCities;
    
    // Length of the initial path of our search      
    private final int startLength;
        
    // The best solution provided by the master           
    private final int startMinimum;
    
    // The best solution that this worker has found               
    private int minimum;   
       
    // Number of states examined by this worker              
    private long states = 0;

    // Start time of this worker              
    private long startTime;

    // End time of this worker              
    private long endTime;

    // Spanning tree used in distance estimations              
    private MST spanningTree;
    
    private TSPWorker(DistanceTable table, int minimum, short [] path, 
            int startCities) {  
            
        this.table = table;
        this.startMinimum = minimum;
        this.minimum = minimum;
                        
        this.path = path;
        this.initialCities = startCities;        
        this.start = path[startCities-1];
        this.end = 1;
        
        this.best = new short[table.cities];        
        
        this.present = new boolean[table.cities+1];
        Arrays.fill(present, false);
        
        present[0] = true;
        
        int len = 0;
        
        out.print("Initial path: ");
        for (int i=0;i<startCities;i++) { 
            present[path[i]] = true;        
            
            if (i >= 1) { 
                len += table.distance(path[i-1], path[i]);
            }
            
            out.print(path[i] + " ");
        }
                
        out.println();

        this.startLength = len;               
        out.println("Initial path length: " + len);
        
        this.spanningTree = new MST(table);
        
        out.println("Upper bound: " + minimum);
        out.println("Lower bound: " + 
                (len + spanningTree.getLength(present, 1)));           
    }
    
    /**
     * Save the current solution, since it is better than the previous ones. 
     * 
     * @param length length of solution. 
     */
    private final void setMinimum(int length) {
        minimum = length;
        System.arraycopy(path, 0, best, 0, path.length);
                    
        double sec = ((System.currentTimeMillis() - startTime)/1000.0);
            
        out.print("Found shorter path ");
        for (int i = 0; i < path.length; i++) {
            out.print(path[i]);
            out.print(" ");
        }
        out.println("with length " + length + " after " + sec 
                + " seconds.");
        
        out.flush();
    }
    
    /**
     * Search a TSP subtree that starts with initial route "path"
     * If partial route is longer than current best full route
     * then forget about this partial route.
     *
     * @param startCity start city of path
     * @param hops number of cities in path 
     * @param length length of the path
     */
    private void tsp(int startCity, int hops, int length) {

        states++;
        
        // Last city of path
        if (hops == table.cities) {
            
            // Close the tour
            length += table.distance(startCity, end);            

            // Check the mimimum.
            if (length < minimum) { 
                setMinimum(length);
            }            
        } 
        
        /* Try all cities that are not on the path yet */        
        int [] others = table.closest(startCity); 
            
        for (int i=0;i<others.length; i++) {
            
            int nextCandidate = others[i];
            
            if (!present[nextCandidate] && nextCandidate != end) {                                    
                // calculate the length if this city is added 
                int newLength = length + table.distance(startCity, nextCandidate);
                
                // Estimate the lower bound for the final result 
                int tourEst = newLength + spanningTree.getLength(present, end);        
                
                // If the lower bound is not longer that the minimum, 
                // recursively continue searching....  
                if (tourEst < minimum) {
                    present[nextCandidate] = true;
                    
                    path[hops] = (short) nextCandidate;                              
                    tsp(nextCandidate, hops + 1, newLength);                        
                    present[nextCandidate] = false;
                } 
            }
        }
    }
    
    /**
     * Starts the calcutation
     */
    private void start() { 
                  
        startTime = System.currentTimeMillis();
        
        tsp(start, initialCities, startLength);  
        
        endTime = System.currentTimeMillis();
               
        if (minimum < startMinimum) { 
            // Better solution was found!
            out.print("Minimum tour: ");
            
            for (int i = 0; i < best.length; i++) {
                out.print(best[i]);            
                out.print(" ");                        
            }        
            out.println("1");
            
            out.println("Minimum tour lenght: " + minimum);        
        } else {         
            // No solution was found!
            out.print("Minimum tour: ");
            
            for (int i = 0; i < best.length; i++) {
                out.print("0 ");            
            }        
            out.println("0");
            
            out.println("Minimum tour lenght: " + Integer.MAX_VALUE);        
        } 
          
        out.println("Total computation time: " + ((endTime-startTime)/1000));
        out.println("Total states examined: " + states);        
        out.println();
        
        spanningTree.printStatistics(out);
    }
        
    /**
     * Check if the worker is being run on a 'restricted' machine. 
     * 
     * To prevent users from accidently running all compute jobs on the frontend
     * machine (causing enormous load), we check what machine we are on, and 
     * refuse to start if it is one of the DAS-2 frontends.  
     */
    private static void checkMachine() { 
        
        // List of restricted machines
        String [] restrictedHosts = new String[] { "fs0.das2.cs.vu.nl", "fs0",         
                "fs1.das2.liacs.nl", "fs1", "fs2.das2.nikhef.nl", "fs2", 
                "fs3.das2.ewi.tudelft.nl", "fs3", "fs4.das2.phys.uu.nl", "fs4"};
        
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            
            for (int i=0;i<restrictedHosts.length;i++) { 
                if (hostname.equals(restrictedHosts[i])) { 
                    System.err.println("You are running a worker on a frontend "
                            + "machine (" + restrictedHosts[i] + "). This is "
                            + "not allowed!");
                    System.exit(1);
                }
            }                        
        } catch (UnknownHostException e) {
            // ignore and hope for the best...
        }
    }
    
    public static void main(String[] args) {
        
        checkMachine();
        
        try {                        
            // First read the input file
            DistanceTable table = new Dimacs(args[0]).getTable();             
            
            // Open the output file            
            out = new PrintWriter(new FileOutputStream(args[1]));
                        
            // Then the current minimum 
            int minimum = Integer.parseInt(args[2]);
            
            // Then read the partial path that we have to extend.
            int length = Integer.parseInt(args[3]);
            
            short [] path = new short[table.cities];           

            for (int i=0;i<length;i++) { 
                path[i] = Short.parseShort(args[4+i]);
            }
            
            new TSPWorker(table, minimum, path, length).start();
            
            out.flush();
            out.close();            
        } catch (Exception e) {
            System.err.println("Oops: ");
            e.printStackTrace(System.err);
        }
    }        
}
