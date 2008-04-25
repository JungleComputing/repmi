package ibis.repmi.comm;

import ibis.repmi.protocol.OpsQueue;

import java.io.Serializable;
import java.util.List;

public class RepMISOSReplyMessage implements Serializable, RepMIMessage {

    //public OpsQueue myOps;
    public Object[] myOps;

    public String whomIHelp;

    public long recoveryRound;

    public long TS;

    public RepMISOSReplyMessage(Object[] myOps2, String whomIHelp, long recoveryRound, long ts) {
        this.myOps = myOps2;
        this.whomIHelp = whomIHelp;
        this.recoveryRound = recoveryRound;
        TS = ts;
    }

    /*
    public RepMISOSReplyMessage(OpsQueue myOps2, String whomIHelp,
            long recoveryRound, long TS) {

        this.myOps = myOps2;
        this.whomIHelp = whomIHelp;
        this.recoveryRound = recoveryRound;
        this.TS = TS;
    }
    */

}
