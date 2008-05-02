package ibis.repmi.comm;

import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPortIdentifier;

public class RepMIRPConnectUpcall implements ReceivePortConnectUpcall {

    public RepMIRPConnectUpcall() {        
        // TODO Auto-generated constructor stub
    }

    public boolean gotConnection(ReceivePort arg0, SendPortIdentifier arg1) {       
        return true;
    }

    public void lostConnection(ReceivePort arg0, SendPortIdentifier arg1,
            Throwable arg2) {
        if (arg2 == null) return;
        System.err.println("lost connection with " + arg1.ibisIdentifier().name() + 
                " because " + arg2.getLocalizedMessage());
        arg2.printStackTrace();
    }

}
