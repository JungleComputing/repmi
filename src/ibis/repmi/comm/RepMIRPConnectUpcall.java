package ibis.repmi.comm;

import java.io.IOException;

import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPortIdentifier;
import ibis.repmi.protocol.LTMProtocol;

public class RepMIRPConnectUpcall implements ReceivePortConnectUpcall {

    LTMProtocol proto;
    
    public RepMIRPConnectUpcall(LTMProtocol proto) {        
        // TODO Auto-generated constructor stub
        this.proto = proto;
    }

    public boolean gotConnection(ReceivePort arg0, SendPortIdentifier arg1) {       
        return true;
    }

    public void lostConnection(ReceivePort arg0, SendPortIdentifier arg1,
            Throwable arg2) {
        if (arg2 == null) return;
        synchronized(this) {
        System.err.println("lost connection with " + arg1.ibisIdentifier().name() + 
                " because " + arg2.getLocalizedMessage());
        arg2.printStackTrace();
        try {
            proto.sendPort.disconnect(arg1.ibisIdentifier(), "repmi-"
                    + arg1.ibisIdentifier().name() + "-"
                    + proto.localId.getUniqueId());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.err.println(">> left connections " + proto.sendPort.connectedTo().length);
        }
        proto.processCrash(arg1.ibisIdentifier());
    }

}
