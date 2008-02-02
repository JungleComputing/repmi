package ibis.repmi.comm;

public class RepMISOSMessage implements RepMIMessage {

    public Long recoveryRound;

    public Long TS;

    public RepMISOSMessage(Long cR, Long ts) {

        recoveryRound = cR;
        TS = ts;
    }

}
