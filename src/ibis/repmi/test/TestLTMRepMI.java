package ibis.repmi.test;

import java.io.IOException;




import ibis.ipl.Ibis;
import ibis.ipl.IbisException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.StaticProperties;
import ibis.repmi.comm.RepMIUpcall;
import ibis.repmi.protocol.LTMProtocol;
import ibis.repmi.protocol.LTVector;
import ibis.repmi.protocol.MyResizeHandler;
import ibis.repmi.protocol.ProcessIdentifier;
import ibis.repmi.protocol.ReplicatedMethod;

public class TestLTMRepMI {

	Registry rgstry;
	
	PortType ptype;
	
	LTVector localLTM;
	
	LTMProtocol proto;	
	
	private long NOPS;

    boolean failure = false;

	private int NCPUS;
	
    public TestLTMRepMI(long nops, int plwa, int plwm, int ncpus) {
    	
    	NOPS = nops;
    	NCPUS = ncpus;
 
    }
    
    public void run() {
    
    	StaticProperties props = new StaticProperties();
        props.add("communication",
                "OneToOne, OneToMany, ManyToOne, FifoOrdered, Reliable, AutoUpcalls, ExplicitReceipt");
        props.add("serialization", "object");
        props.add("worldmodel", "open");

        localLTM = new LTVector();
        proto = new LTMProtocol(localLTM);
        
        // Create an Ibis
        final Ibis ibis;        
        final MyResizeHandler mrh = new MyResizeHandler(localLTM);
        try {        	
            ibis = Ibis.createIbis(props, mrh);       
            
        } catch (IbisException e) {
            System.err.println("Could not create Ibis: " + e);
            failure = true;
            return;
        }
        System.out.println("created ibis");
        
        rgstry = ibis.registry();
        mrh.setRegistry(rgstry);       
        mrh.setMyself(ibis.identifier());
        
        proto.setIbis(ibis);
        
        // Install shutdown hook that terminates ibis
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
               if(proto != null) {
            	   proto.processLeave();
                }
               synchronized(ibis) {
            	   while(proto.isStopped() == false) {
            		   try {            			   
            			   ibis.wait();
            		   } catch (InterruptedException e) {
            			   // TODO Auto-generated catch block
            			   e.printStackTrace();
            		   }
            	   }
               }               
            }
        });

        // Create properties for the port type
        StaticProperties portprops = new StaticProperties();
        portprops.add("communication",
                "OneToMany, FifoOrdered, Reliable, AutoUpcalls, ExplicitReceipt");
        portprops.add("serialization", "object");

        // Create the port type
        try {
            ptype = ibis.createPortType("RepMI port", portprops);
        } catch (Exception e) {
            System.err.println("Could not create port type: " + e);
            failure = true;
            return;
        }

        SendPort serverSender = null;
        SendPort joinAckSP = null;
        try {
        	serverSender = ptype.createSendPort();
        	joinAckSP = ptype.createSendPort();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			failure = true;
			return;
		}
        
		
		RepMIUpcall repmiUpcall = null;
	    ReceivePort serverReceiver = null;
//	      Create an upcall handler
	        repmiUpcall = new RepMIUpcall(proto);
	        try {
				serverReceiver = ptype.createReceivePort("ibis" + ibis.identifier().name(),
				        repmiUpcall);
				serverReceiver.enableConnections();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
	    mrh.setIbisRPI(serverReceiver.identifier());
		mrh.setServerSender(serverSender);
		mrh.setPType(ptype);		
			
        proto.setProcessIdentifier(new ProcessIdentifier(ibis.identifier()));
        proto.setSendPort(serverSender);
        proto.setJoinAckSendPort(joinAckSP);
        proto.setRegistry(rgstry);
        
        mrh.setProtocol(proto);
        
        ibis.enableResizeUpcalls();
        
        localLTM.waitForInit();
        
        /*
        //DEBUG
    	System.out.println("LTM has been init: " + 
    						localLTM.getEntry(new ProcessIdentifier(ibis.identifier()), 
    										  new ProcessIdentifier(ibis.identifier())) + 
    						" ... moving on");
        */
        
        try{
        
        serverReceiver.enableUpcalls();
        } catch(Exception e) {
        	System.err.println("Could not create upcall handler: " + e);
            failure = true;
            return;
        }
        
        /*
        //DEBUG
        System.out.println("created ports");
        */
        
        proto.setIbisReceivePortIdentifier(serverReceiver.identifier());
        
        /*
        
        ReplicatedAccount ra = new ReplicatedAccount();
        proto.setReplicatedObject(ra);
        
        */
        
        ReplicatedAsp ra = new ReplicatedAsp(NCPUS+1, 42);
        proto.setReplicatedObject(ra);
        
        //MEAS
        proto.setMAXVAL(10*NOPS);
        proto.setNCPUS(NCPUS);
         
        
        /*needed to start the internal execution thread of the protocol*/
        proto.start();
        
        proto.processLocalRead(new ReplicatedMethod(
				"printTable", null, null));
        
        Integer myRow = null;        
                   			
		myRow = (Integer)proto.processLocalWrite(new ReplicatedMethod(
					"myRow", null, null));
		
		int theRow [] = (int[])proto.processLocalRead(new ReplicatedMethod(
				"getMyRow", 
				new Class[] {Integer.class},
				new Object[] {myRow}));
		
		System.out.println("MyRow : " + myRow);
		
        for(int k=0; k<=NCPUS; ) {
        	
        	System.out.println("Round : " + k);
        	
        	if(k != myRow.intValue()) {
        		
        		while(((Integer)proto.processLocalRead(new ReplicatedMethod(
        				"getRound", null, null))).intValue() < k) {
        			try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
        		
        		theRow = (int[])proto.processLocalRead(new ReplicatedMethod("recomputeMyRow", 
        													new Class[] {Integer.class},
        													new Object[] {myRow}));
        		System.out.println("finished Round : " + k + "; MyRow: " + myRow + 
        							"; recomputed my row: " + theRow.toString());
        		k ++;
        	} else {        		
        		try {           		
        			Object[] args = new Object[2];
        			args[0] = myRow;
        			args[1] = theRow;
        			proto.processLocalWrite(new ReplicatedMethod(
        					"sendMyRow", new Class[] {Integer.class, theRow.getClass()}, args));
        		} catch (SecurityException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
        		
        		System.out.println("finished Round : " + k + "; MyRow: " + myRow + 
						"; sent my row: " + theRow.toString());
        		k ++;
        	}
        }
        
        proto.processLocalRead(new ReplicatedMethod(
				"printTable", null, null));
        
        proto.testReady();
        
        /*
        Random rand = new Random(0);
        int val;
        Object[] args = new Object[1];
        long execOps = NOPS;              
        
       while(!proto.testReady()) {
        	val = rand.nextInt(100);
        	args[0] = new Integer(10);
        	
          	if(val < pLWA) {          		
           		try {           			
					proto.processLocalWrite(new ReplicatedMethod(
							"writeAddition", new Class[] {Integer.class}, args));
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				execOps --;
				proto.setMAXVAL(10*execOps);				
           	} else if(val < pLWM) {
           		try {
					proto.processLocalWrite(new ReplicatedMethod(
							"writeMultiplication", new Class[] {Integer.class}, args));
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
           	} else {
           		try {
					Thread.sleep(1);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}				
           		try {           			
					proto.processLocalRead(new ReplicatedMethod(
							"readVal", (Class[]) null, null));
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 			
           	}
        }
       */
    }
    
	/**
	 * @param args
	 */
	/*
    public static void main(String[] args) {
		// TODO Auto-generated method stub
		TestLTMRepMI test = new TestLTMRepMI(Long.parseLong(args[0]));
        test.run();
        if (test.failure) {
            System.exit(1);
        }
        System.out.println("Test succeeded!");
        System.exit(0); // let shutdown hook terminate ibis
	}
*/
}
