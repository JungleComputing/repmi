package ibis.repmi.comm;

import ibis.ipl.ReceivePortIdentifier;

import java.io.Serializable;

public class RepMIAckWelcomeMessage implements RepMIMessage, Serializable {

    public ReceivePortIdentifier rpi;

    public RepMIAckWelcomeMessage(ReceivePortIdentifier rpi) {
        this.rpi = rpi;
    }

}
