package ibis.repmi.commTest;
import java.io.Serializable;

import ibis.ipl.ReceivePortIdentifier;
import ftrepmi.protocol.ProcessIdentifier;
import ftrepmi.protocol.ReplicatedMethod;


public class DummyOperation implements Serializable{

	public static final int LR = 0;
	public static final int LW = 1;
	public static final int RW = 2;
	public static final int JOIN = 3;
	public static final int LEAVE = 4;
	public static final int NOPE = 5;
	
	private ProcessIdentifier pid;
	private ProcessIdentifier contact;
	private Long timestamp;
	private ReplicatedMethod method;
	private int type;
	private ReceivePortIdentifier joinAck;
	private ReceivePortIdentifier ibisRPI;
	private ReceivePortIdentifier joinAckNormalNode;
	
}
