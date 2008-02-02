package ibis.repmi.comm;

import ibis.repmi.protocol.OpsQueue;

import java.io.Serializable;

public class RepMISOSReplyMessage implements Serializable, RepMIMessage {

    public OpsQueue myOps;

    public String whomIHelp;

    public long recoveryRound;

    public long TS;

    public RepMISOSReplyMessage(OpsQueue myOps2, String whomIHelp,
            long recoveryRound, long TS) {

        this.myOps = myOps2;
        this.whomIHelp = whomIHelp;
        this.recoveryRound = recoveryRound;
        this.TS = TS;
    }

}
