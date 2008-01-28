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

		StaticProperties props = new StaticProperties();
		props.add("communication",
				"OneToOne, OneToMany, ManyToOne, " +
				"FifoOrdered, Reliable, AutoUpcalls, ExplicitReceipt");
		props.add("serialization", "object");
		props.add("worldmodel", "open");

		localLTM = new LTVector();
		proto = new LTMProtocol(localLTM);

		// Create an Ibis
		mrh = new MyResizeHandler(localLTM);
		try {        	
			ibis = Ibis.createIbis(props, mrh);       

		} catch (IbisException e) {
			System.err.println("Could not create Ibis: " + e);
			failure = true;
			return;
		}
		System.out.println("created ibis " + ibis.identifier());

		rgstry = ibis.registry();
		mrh.setRegistry(rgstry);       
		mrh.setMyself(ibis);

		proto.setIbis(ibis);

		// Create properties for the upcall port type
		StaticProperties portprops = new StaticProperties();
		portprops.add("communication",
		"OneToMany, FifoOrdered, Reliable, AutoUpcalls, ExplicitReceipt");
		portprops.add("serialization", "object");

		//create properties for explicit receipt port type
		StaticProperties explportprops = new StaticProperties();
		explportprops.add("communication",
		"OneToMany, FifoOrdered, Reliable, ExplicitReceipt");
		explportprops.add("serialization", "object");
		PortType explPType = null;
		
		// Create the port type
		try {
			ptype = ibis.createPortType("RepMI port", portprops);
			explPType = ibis.createPortType("RepMI explicit receipt", explportprops);
		} catch (Exception e) {
			System.err.println("Could not create port type: " + e);
			failure = true;
			return;
		}

		SendPort serverSender = null;
		SendPort joinAckSP = null;
		SendPort explicitSP = null;
		try {
			serverSender = ptype.createSendPort();
			joinAckSP = ptype.createSendPort();
			explicitSP = explPType.createSendPort();
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
			serverReceiver = ptype.createReceivePort("repmi-contact-" + ibis.identifier().name(),
					repmiUpcall);
			serverReceiver.enableConnections();
			explicitReceiver = explPType.createReceivePort("repmi-adm-" + ibis.identifier().name());
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

		ibis.enableResizeUpcalls();

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
		
		serverReceiver.enableUpcalls();
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
	}
}
