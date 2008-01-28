package ibis.repmi.commTest;
import ibis.repmi.comm.RepMIMessage;
import ibis.repmi.protocol.LTVector;
import ibis.repmi.protocol.Operation;

import java.io.Serializable;



public class DummyMessage implements RepMIMessage, Serializable {

	public DummyVector localLTM;
	public DummyOperation arg;
	
	public DummyMessage(DummyVector ltm, DummyOperation arg) {
		
		localLTM = ltm;
		this.arg = arg;
	}
}
