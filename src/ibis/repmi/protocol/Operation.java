package ibis.repmi.protocol;


import ibis.ipl.ReceivePortIdentifier;

import java.io.Serializable;

//TODO!!!!!!!!!!! to be broken into a superclass Operation and the following subclasses
//JoinOperation, RemoteOperation, RMIOperation

public class Operation implements Serializable {

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
	
	/*constructor for effective method call*/
	public Operation(ProcessIdentifier id, Long ts, ReplicatedMethod meth, int opType) {
		
		pid = id;
		timestamp = ts;
		method = meth;
		type = opType;
	}
		
	/*constructor for join request operation on contact side*/
	public Operation(ProcessIdentifier id, ProcessIdentifier contactId, Long ts, int opType, 
			ReceivePortIdentifier joinAckPortId, 
			ReceivePortIdentifier joiningIbisPortId, 
			ReceivePortIdentifier joinAckNormalNode2) {
				
		pid = id;
		contact = contactId; 
		timestamp = ts;
		type = opType;
		method = null;
		joinAck = joinAckPortId;
		ibisRPI = joiningIbisPortId;
		joinAckNormalNode = joinAckNormalNode2;
	}
	
	/*constructor for join request operation on simple side*/
	public Operation(ProcessIdentifier id, ProcessIdentifier contactId, Long ts, int opType) {
				
		pid = id;
		contact = contactId; 
		timestamp = ts;
		type = opType;
		method = null;		
	}
	
	/*constructor for leave/nope operation*/
	public Operation(ProcessIdentifier id, Long ts, int opType) {
		
		pid = id;		
		timestamp = ts;
		type = opType;
		method = null;
	}
	
	public Operation(Operation arg) {
		// TODO Auto-generated constructor stub
		this.contact = arg.contact;
		this.ibisRPI = arg.ibisRPI;
		this.joinAck = arg.joinAck;
		this.joinAckNormalNode = arg.joinAckNormalNode;
		this.method = arg.method;
		this.pid = arg.pid;
		this.timestamp = arg.timestamp;
		this.type = arg.type;
	}

	public Long getTS() {
		
		return timestamp;
	}
	
	public void setTS(long ts) {
		
		timestamp = new Long(ts);
	}
	
	public ProcessIdentifier getPid() {
		
		return pid;
	}
	
	public ReplicatedMethod getMethod() {
		
		return method;
	}
	
	public int getType() {
		
		return type;
	}
	
	public void setType(int t) {
		
		type = t;
	}

	public ProcessIdentifier getContact() {
		
		return contact;
	}
	
	public ReceivePortIdentifier getJoinPort() {
		
		return joinAck;
	}
	
	public ReceivePortIdentifier getIbisPortId() {
		
		return ibisRPI;
	}
	
	public ReceivePortIdentifier getJoinNormalNodePort() {
		
		return this.joinAckNormalNode;
	}
	
}
