package ibis.repmi.comm;

import java.util.List;

public class RepMISOSMessage implements RepMIMessage {

    public Long currentRound;

    public List missingOp;

    public RepMISOSMessage(Long cR, List missing) {

        currentRound = cR;
        missingOp = missing;
    }

}
