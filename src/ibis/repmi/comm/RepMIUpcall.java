package ibis.repmi.comm;

import java.io.IOException;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.repmi.protocol.LTMProtocol;
import ibis.repmi.protocol.LTVector;
import ibis.repmi.protocol.Operation;
import ibis.repmi.protocol.ProcessIdentifier;

public class RepMIUpcall implements MessageUpcall {

    LTMProtocol proto;

    Object mesg;

    public RepMIUpcall(LTMProtocol proto) {

        this.proto = proto;
    }

    public void upcall(ReadMessage m) {

        try {
            mesg = m.readObject();
        } catch (ClassNotFoundException e) {
            mesg = null;
        } catch (IOException e) {
            mesg = null;
            e.printStackTrace();
        }

        IbisIdentifier ii = m.origin().ibisIdentifier();

        if (mesg != null) {
            ProcessIdentifier pi = new ProcessIdentifier(ii);
            if (mesg instanceof RepMILTMMessage) {

                if (((RepMILTMMessage) mesg).arg.getType() == Operation.LW)
                    ((RepMILTMMessage) mesg).arg.setType(Operation.RW);

                /* i might not do communication, so i delay the call to m.finish */
                proto.processRemoteOperation(((RepMILTMMessage) mesg).arg,
                        ((RepMILTMMessage) mesg).localLTM, pi, m);
                return;

            } else if (mesg instanceof RepMIJoinMessage) {

                /*
                 * needs to be called to release the thread before entering the
                 * processJoin call which will broadcast the join request to all
                 * other processes in the system
                 */
                ReceivePortIdentifier joinAckPort = ((RepMIJoinMessage) mesg).recvPortId;
                ReceivePortIdentifier joiningIbisPort = ((RepMIJoinMessage) mesg).ibisRPI;
                ReceivePortIdentifier joinAckNormalNode = ((RepMIJoinMessage) mesg).joinAckNormalNode;

                try {
                    m.finish();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                proto.processJoin(pi, joinAckPort, joiningIbisPort,
                        joinAckNormalNode);

                return;

            }
        }
    }
}
