package ibis.repmi.comm;

import ibis.ipl.ReceivePortIdentifier;
import ibis.repmi.protocol.LTVector;
import ibis.repmi.protocol.OpsQueue;
import ibis.repmi.protocol.Replicateable;

import java.io.Serializable;

public class RepMIWelcomeMessage implements Serializable, RepMIMessage {

    public LTVector localLTM;

    public Replicateable ro;

    public long round;

    public OpsQueue ops;

    public ReceivePortIdentifier rpi;

    public ReceivePortIdentifier dedicatedRpi;

    public RepMIWelcomeMessage(LTVector ltm, Replicateable rob, long roundNo,
            OpsQueue ops, ReceivePortIdentifier recvPI,
            ReceivePortIdentifier dedicatedRpi) {

        localLTM = ltm;
        ro = rob;
        round = roundNo;
        rpi = recvPI;
        this.ops = ops;
        this.dedicatedRpi = dedicatedRpi;
    }
}
