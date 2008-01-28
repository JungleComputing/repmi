package ibis.repmi.comm;

import java.util.List;

public class RepMISOSReplyMessage implements RepMIMessage{

	public List foundOp;
	
	public RepMISOSReplyMessage(List fOp) {
		
		foundOp = fOp;
	}

}
