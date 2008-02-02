package ibis.repmi.comm;

import java.util.List;

public class RepMISOSReplyMessage implements RepMIMessage {

    public List myOps;

    public String whomIHelp;

    public Long recoveryRound;

    public RepMISOSReplyMessage(List myOps, String whomIHelp, Long recoveryRound) {

        this.myOps = myOps;
        this.whomIHelp = whomIHelp;
        this.recoveryRound = recoveryRound;
    }

}
