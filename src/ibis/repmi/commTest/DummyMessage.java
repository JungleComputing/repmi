package ibis.repmi.commTest;
import java.io.Serializable;

import ftrepmi.comm.RepMIMessage;
import ftrepmi.protocol.LTVector;
import ftrepmi.protocol.Operation;


public class DummyMessage implements RepMIMessage, Serializable {

	public DummyVector localLTM;
	public DummyOperation arg;
	
	public DummyMessage(DummyVector ltm, DummyOperation arg) {
		
		localLTM = ltm;
		this.arg = arg;
	}
}
