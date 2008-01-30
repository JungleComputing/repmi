package ibis.repmi.protocol;

import java.io.IOException;
import java.util.Set;

import ibis.ipl.Ibis;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.Registry;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.MessageUpcall;
import ibis.ipl.WriteMessage;
import ibis.repmi.comm.RepMIAckWelcomeMessage;
import ibis.repmi.comm.RepMIJoinMessage;
import ibis.repmi.comm.RepMIWelcomeMessage;

public class MyResizeHandler implements RegistryEventHandler {

    LTVector ltm;

    LTMProtocol proto;

    Registry registry;

    SendPort serverSender;

    ReceivePort receiver;

    Ibis myself;

    IbisIdentifier contact;

    Set p2ContactMe;

    int rank = 0;

    private ReceivePortIdentifier ibisRPI;

    public MyResizeHandler(LTVector ltm) {

        this.ltm = ltm;
    }

    public void setServerSender(SendPort srvSnd) {

        serverSender = srvSnd;
    }

    public void setRegistry(Registry rgstry) {

        registry = rgstry;
    }

    public void setMyself(Ibis ib) {

        myself = ib;
    }

    public void setProtocol(LTMProtocol p) {

        proto = p;
    }

    public void setIbisRPI(ReceivePortIdentifier identifier) {
        ibisRPI = identifier;

    }

    public void died(IbisIdentifier ii) {

    }

    public void left(IbisIdentifier ii) {

    }

    public void mustLeave(IbisIdentifier[] iis) {

    }

    public void joined(IbisIdentifier ii) {

        rank++;

        if (ii.equals(myself.identifier())) {

            /* it's my own turn */

            if (rank == 1) {

                // DEBUG
                System.out.println("First in system");

                contact = myself.identifier();
                proto.start();
                ltm.init(new ProcessIdentifier(ii));

            } else {
                /* i know i am not alone :) */
            }
        } else {
            /* another ibis has joined */
            /* contact the first one, could be changed to another algo */
            if (contact == null) {
                contact = ii;
                ReceivePortIdentifier client;
                try {

                    // DEBUG
                    System.out.println("Detected ibis " + ii.name()
                            + " already in system "
                            + "and chose it as contact node");

                    ReceivePort receiverContact = myself.createReceivePort(
                            LTMProtocol.ptype, "join-contact"
                                    + myself.identifier().name());
                    receiverContact.enableConnections();

                    receiver = myself.createReceivePort(LTMProtocol.ptype,
                            "join" + myself.identifier().name(),
                            new JoinUpcall());
                    receiver.enableConnections();

                    if (serverSender != null) {
                        client = serverSender.connect(contact, "repmi-contact-"
                                + contact.name());
                        WriteMessage w = serverSender.newMessage();
                        w.writeObject(new RepMIJoinMessage(receiverContact
                                .identifier(), ibisRPI, receiver.identifier()));
                        w.finish();

                        serverSender.disconnect(client);

                        // DEBUG
                        System.out.println("Join request sent");

                        /* waiting for acceptance in the system */

                        ReadMessage r = null;
                        try {
                            r = receiverContact.receive();
                        } catch (ReceiveTimedOutException rtoe) {
                            contact = null;
                            receiverContact.close();
                            receiver.close();
                            return;
                        }
                        RepMIWelcomeMessage welcome = (RepMIWelcomeMessage) r
                                .readObject();
                        r.finish();

                        p2ContactMe = welcome.localLTM.keys();

                        // DEBUG
                        System.out.println("ibisses to contact me: "
                                + p2ContactMe.size());

                        ltm.setLTM(welcome.localLTM);
                        proto.setReplicatedObject(welcome.ro);
                        proto.setRoundNo(welcome.round);
                        proto.setCurrentQueue(welcome.ops);

                        ReceivePortIdentifier dedicatedRpi = proto.createNewRP(
                                contact, LTMProtocol.ptype).identifier();
                        SendPort explicitSP = myself
                                .createSendPort(LTMProtocol.explicitReceivePT);

                        explicitSP.connect(welcome.rpi);

                        /*
                         * added temporarily until rpis can be found without
                         * nameserver in the loop
                         */
                        WriteMessage wRpi = explicitSP.newMessage();
                        wRpi.writeObject(new RepMIAckWelcomeMessage(
                                dedicatedRpi));
                        wRpi.finish();

                        // explicitSP.disconnect(welcome.rpi);
                        explicitSP.close();

                        p2ContactMe.remove(new ProcessIdentifier(contact));

                        // DEBUG
                        System.out.println("ibisses to accept me: "
                                + p2ContactMe.size());

                        proto.addNewRpi(welcome.dedicatedRpi);

                        serverSender.connect(welcome.dedicatedRpi);
                        /*
                         * //DEBUG System.out.println("Join request accepted");
                         * System.out.println("size of sent queue " +
                         * welcome.oq.size());
                         */
                        if (p2ContactMe.size() > 1) {
                            /* more nodes than just me and contact node */
                            receiver.enableMessageUpcalls();
                        } else {
                            System.out.println("I am second in system");
                            proto.start();
                            ltm.init();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {

            }
        }

    }

    class JoinUpcall implements MessageUpcall {

        public void upcall(ReadMessage m) throws IOException {

            SendPort explicitSP = myself
                    .createSendPort(LTMProtocol.explicitReceivePT);
            IbisIdentifier lastAck = null;
            RepMIWelcomeMessage welcome;

            try {
                welcome = (RepMIWelcomeMessage) m.readObject();
            } catch (ClassNotFoundException e) {
                welcome = null;
            }

            IbisIdentifier ii = m.origin().ibisIdentifier();

            ProcessIdentifier pi = new ProcessIdentifier(ii);

            try {
                m.finish();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (serverSender != null) {
                explicitSP.connect(welcome.rpi);

                ReceivePortIdentifier dedicatedRpi = proto.createNewRP(ii,
                        LTMProtocol.ptype).identifier();

                /*
                 * added temporarily until rpis can be found without nameserver
                 * in the loop
                 */

                WriteMessage wRpi = explicitSP.newMessage();
                wRpi.writeObject(new RepMIAckWelcomeMessage(dedicatedRpi));
                wRpi.finish();

                // explicitSP.disconnect(welcome.rpi);
                explicitSP.close();

                proto.addNewRpi(welcome.dedicatedRpi);
                serverSender.connect(welcome.dedicatedRpi);
            }

            // DEBUG
            System.out.println("Join request accepted by ibis " + ii.name());

            synchronized (JoinUpcall.class) {
                /* i need to read queues from other processes already in system */
                p2ContactMe.remove(new ProcessIdentifier(ii));

                // DEBUG
                System.out
                        .println("Detected ibis " + ii.name()
                                + " already in system "
                                + "and waiting for information");
                System.out.println("ibisses to detect: " + p2ContactMe.size());

                // proto.getOpsQueue().merge(welcome.oq);
                // ltm.updateVT(new ProcessIdentifier(myself),
                // welcome.localLTM.getVT(new ProcessIdentifier(ii)));
                // ltm.update(new ProcessIdentifier(ii), welcome.localLTM);

                if (p2ContactMe.size() == 1) {
                    lastAck = ii;
                }
            }
            if (lastAck != null) {
                /*
                 * //DEBUG System.out.println("Last process to ack my presence,
                 * starting ...");
                 */
                proto.start();
                ltm.init();
            }

        }
    }

    /*
     * public void setExplicitSPT(PortType explicitSP) { // TODO Auto-generated
     * method stub //this.explicitSP = explicitSP; }
     */
    public void electionResult(String arg0, IbisIdentifier arg1) {
        // TODO Auto-generated method stub

    }

    public void gotSignal(String arg0) {
        // TODO Auto-generated method stub

    }

}
