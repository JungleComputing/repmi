package applications.util;

/**
 *  
 * Read the DIMACS format 
 *  
 * @author Jason Maassen
 * @version 1.0 May 20, 2005
 * @since 1.0
 * 
 */


public class Dimacs {
    
    private int cities;   
    
    private int [][] distanceMatrix;        
    private double [] coordinates;
    
    private boolean euclidean; 
    private boolean integral;
    
    private Input in;
    
    private String name; 
    private String edgeWeightType;
    private String edgeWeightFormat;    
    private String next;    
    private String suffix;
          
    private DistanceTable table;
    
    public Dimacs(String file) throws Exception {         
        in = new Input(file);
        readFile();        
    }
    
    public DistanceTable getTable() { 
        
        if (table == null) { 
            if (distanceMatrix != null) { 
                table = new MatrixDistanceTable(name, cities, distanceMatrix);            
            } else if (euclidean && integral) {                
                int [] temp = new int[coordinates.length];
                
                for (int i=0;i<coordinates.length;i++) { 
                    temp[i] = (int) coordinates[i];                        
                }
                
                table = new IntegralEuclideanDistanceTable(name, cities, temp);
                
            } else if (euclidean && !integral) {
                table = new EucledeanDistanceTable(name, cities, coordinates);
            } else {
                table = new GeoDistanceTable(name, cities, coordinates);
            }
        }
        return table;
    }
         
    private boolean match(String match) { 
        
        if (next.startsWith(match)) { 
                
            suffix = next.substring(match.length()).trim();
        
            if (suffix.startsWith(":")) { 
                suffix = suffix.substring(1).trim();
            }
        
            return true;
        } else { 
            return false;
        }
    }
    
    private void readEdgeWeightSection() throws Exception { 
    
        if (edgeWeightType.equals("EXPLICIT") && 
                edgeWeightFormat.equals("UPPER_DIAG_ROW")) {
         
            euclidean = false;
            integral = true;
            
            distanceMatrix = new int[cities+1][cities+1];
            
            for (int i=1; i<=cities; ++i) { 
                for (int j=i; j<=cities; ++j) {
                    distanceMatrix[i][j] = distanceMatrix[j][i] = in.readInt();
                } 
            } 
            
        } else {
            throw new Exception("Edge weights with type " + edgeWeightType + 
                    " and format " + edgeWeightFormat + " not supported!");
        }
    }
    
    private void readNodeCoordSection() throws Exception { 
        
        if (edgeWeightType.equals("EUC_2D")) { 
            euclidean = true;
        } else if (edgeWeightType.equals("CEIL_2D")) {
            euclidean = true;
        } else if (edgeWeightType.equals("GEOM") || edgeWeightType.equals("GEO")) { 
            euclidean = false;
            integral= false;
        } else { 
            throw new Exception("Node coordinates with type " + edgeWeightType + 
                     " not supported!");
        }
         
        integral = true;           
        coordinates = new double[cities*2+2];
            
        for (int i=1;i<=cities;i++) {                
            int n = in.readInt();
            
            coordinates[2*n] = in.readDouble();
            coordinates[2*n+1] = in.readDouble();
            
            // Test if the coordinates are actually integers...
            if (integral) {
                if (coordinates[2*n] != Math.rint(coordinates[2*n]) || 
                        coordinates[2*n+1] != Math.rint(coordinates[2*n+1])) {                        
                    integral = false;                        
                }
            }                
        }       
    }

    private void readFile() throws Exception {
        
        do {             
            next = in.readLine();
            
            if (in.eof()) { 
                return;
            }
                        
            if (match("NAME")) {
                name = suffix;
            } else if (match("TYPE")) {
            } else if (match("COMMENT")) {
                // ignore
            } else if (match("DIMENSION")) {
                cities = Integer.parseInt(suffix);
            } else if (match("EDGE_WEIGHT_TYPE")) {
                edgeWeightType = suffix;
            } else if (match("EDGE_WEIGHT_FORMAT")) {
                edgeWeightFormat = suffix;
            } else if (match("EDGE_DATA_FORMAT")) {
            } else if (match("NODE_COORD_TYPE")) {
            } else if (match("EOF")) {
                return;
            } else if (match("EDGE_WEIGHT_SECTION")) {
                readEdgeWeightSection();
            } else if (match("NODE_COORD_SECTION")) {
                readNodeCoordSection();   
            } else if (match("DISPLAY_DATA_TYPE")) {
                // ignore
            } else { 
                throw new Exception("Parse error while reading file");
            }
                    
        } while (true);    
    }



}
