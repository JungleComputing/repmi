package ibis.repmi.comm;

import ibis.repmi.protocol.LTVector;
import ibis.repmi.protocol.Operation;

import java.io.Serializable;

public class RepMILTMMessage implements Serializable, RepMIMessage {

    public LTVector localLTM;

    public Operation arg;

    public RepMILTMMessage(LTVector ltm, Operation arg) {

        localLTM = ltm;
        this.arg = arg;
    }
}
