package ibis.repmi.test;

import ibis.ipl.*;
import ibis.repmi.comm.RepMIUpcall;
import ibis.repmi.protocol.LTMProtocol;
import ibis.repmi.protocol.LTVector;
import ibis.repmi.protocol.MyResizeHandler;
import ibis.repmi.protocol.ProcessIdentifier;

import java.io.IOException;

public class VoidTest {
	Registry rgstry;

	PortType ptype;

	LTVector localLTM;

	LTMProtocol proto;	

	protected long NOPS;
	protected int pLWA;
	protected int pLWM;

	boolean failure = false;

	protected int NCPUS;

	protected String writerCluster;

	Ibis ibis;        
	MyResizeHandler mrh;

	public VoidTest(long nops, int plwa, int plwm, int ncpus) {

		NOPS = nops;
		pLWA = plwa;
		pLWM = plwm;
		NCPUS = ncpus;
	}

	public VoidTest(long nops, int plwa, int plwm, int ncpus,
			String wC) {

		NOPS = nops;
		NCPUS = ncpus;
		pLWA = plwa;
		pLWM = plwm;
		writerCluster = wC;
	}       

	public void run() {
		
		/*
		StaticProperties props = new StaticProperties();
		props.add("communication",
				"OneToOne, OneToMany, ManyToOne, " +
				"FifoOrdered, Reliable, AutoUpcalls, ExplicitReceipt");
		props.add("serialization", "object");
		props.add("worldmodel", "open");
		*/	
		
		localLTM = new LTVector();
		proto = new LTMProtocol(localLTM);

		// Create an Ibis		
		// define capabilities
		IbisCapabilities capabilities =
            new IbisCapabilities(IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED,
            		IbisCapabilities.CLOSED_WORLD);
		
		// define resize handler
		mrh = new MyResizeHandler(localLTM);
		
		// define port types		
		ptype = new PortType(new String[] 
		      { PortType.SERIALIZATION_OBJECT,
                PortType.CONNECTION_MANY_TO_MANY,
                PortType.COMMUNICATION_FIFO,
                PortType.COMMUNICATION_RELIABLE,
                PortType.RECEIVE_AUTO_UPCALLS,
                PortType.RECEIVE_EXPLICIT});
		
		PortType explicitReceivePT = new PortType(new String[]
		      { PortType.SERIALIZATION_OBJECT,
				PortType.CONNECTION_ONE_TO_MANY,
				PortType.COMMUNICATION_FIFO,
                PortType.COMMUNICATION_RELIABLE,
                PortType.RECEIVE_EXPLICIT});	
		
		try {        	
			ibis = IbisFactory.createIbis(capabilities, mrh, 
					new PortType[] {ptype, explicitReceivePT});
		} catch (IbisCreationFailedException icfe) {
			System.err.println("Could not create Ibis: " + icfe);
			failure = true;
			return;
		}
		System.out.println("created ibis " + ibis.identifier());

		rgstry = ibis.registry();
		mrh.setRegistry(rgstry);       
		mrh.setMyself(ibis);

		proto.setIbis(ibis);		

		SendPort serverSender = null;
		SendPort joinAckSP = null;
		SendPort explicitSP = null;
		try {
			serverSender = ibis.createSendPort(ptype);
			joinAckSP = ibis.createSendPort(ptype);
			explicitSP = ibis.createSendPort(explicitReceivePT);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			failure = true;
			return;
		}


		RepMIUpcall repmiUpcall = null;
		ReceivePort serverReceiver = null;
		ReceivePort explicitReceiver = null;
//		Create an upcall handler
		repmiUpcall = new RepMIUpcall(proto);
		try {
			serverReceiver = ibis.createReceivePort(ptype,
					"repmi-contact-" + ibis.identifier().name(),
					repmiUpcall);
			serverReceiver.enableConnections();
			explicitReceiver = ibis.createReceivePort(explicitReceivePT,
					"repmi-adm-" + ibis.identifier().name());
			explicitReceiver.enableConnections();
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		mrh.setIbisRPI(serverReceiver.identifier());
		mrh.setServerSender(serverSender);
		mrh.setExplicitSP(explicitSP);
		mrh.setPType(ptype);		

		proto.setProcessIdentifier(new ProcessIdentifier(ibis.identifier()));
		proto.setSendPort(serverSender);
		proto.setJoinAckSendPort(joinAckSP);
		proto.setRegistry(rgstry);
		proto.setPtype(ptype);

		mrh.setProtocol(proto);

		rgstry.enableEvents();

		localLTM.waitForInit();

		/*
        //DEBUG
    	System.out.println("LTM has been init: " + 
    						localLTM.getEntry(new ProcessIdentifier(ibis.identifier()), 
    										  new ProcessIdentifier(ibis.identifier())) + 
    						" ... moving on");
		 */
	
		proto.setIbisReceivePortIdentifier(serverReceiver.identifier());
		
		/*added temporarily, until rpis can be found without nameserver in the loop*/
		proto.setIbisReceivePort(explicitReceiver);
		
		serverReceiver.enableMessageUpcalls();
		proto.enableRPUpcalls();

		/*
        //DEBUG
        System.out.println("created ports");
		 */

		//MEAS
		proto.setMAXVAL(NOPS);
		proto.setNCPUS(NCPUS);		
		ReplicatedAccount ra = new ReplicatedAccount();
		proto.setReplicatedObject(ra);
		
		proto.waitForAllToJoin(NCPUS-1);
		
		int size = ibis.registry().getPoolSize();

        System.err.println("pool size = " + size);
        
        ibis.registry().waitUntilPoolClosed();

        System.err.println("pool closed");
	}
}
