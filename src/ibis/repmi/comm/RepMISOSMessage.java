package ibis.repmi.comm;

import java.io.Serializable;

public class RepMISOSMessage implements Serializable, RepMIMessage {

    public long recoveryRound;

    public long TS;

    public RepMISOSMessage(long cR, long ts) {

        recoveryRound = cR;
        TS = ts;
    }

}
