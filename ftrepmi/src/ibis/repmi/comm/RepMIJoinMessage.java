package ibis.repmi.comm;

import ibis.ipl.ReceivePortIdentifier;

import java.io.Serializable;

public class RepMIJoinMessage implements Serializable, RepMIMessage {

    public ReceivePortIdentifier recvPortId;

    public ReceivePortIdentifier ibisRPI;

    public ReceivePortIdentifier joinAckNormalNode;

    public RepMIJoinMessage(ReceivePortIdentifier identifier,
            ReceivePortIdentifier ibisRPI, ReceivePortIdentifier identifier2) {
        recvPortId = identifier;
        this.ibisRPI = ibisRPI;
        joinAckNormalNode = identifier2;
    }

}
