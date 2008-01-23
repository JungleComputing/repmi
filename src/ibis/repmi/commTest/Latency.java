package ibis.repmi.commTest;

/* $Id: Latency.java,v 1.15 2004/11/24 10:52:16 ceriel Exp $ */

import ibis.ipl.*;

import java.io.IOException;

import ftrepmi.comm.RepMILTMMessage;
import ftrepmi.protocol.LTVector;
import ftrepmi.protocol.Operation;
import ftrepmi.protocol.ProcessIdentifier;
import ftrepmi.protocol.ReplicatedMethod;

class Latency {

    static Ibis ibis;

    static Registry registry;

    public static void main(String[] args) {
        /* Parse commandline. */

        boolean upcall = false;
        int NOPS = 1000;

        if (args.length > 1) {
            upcall = args[1].equals("-u");
        }

        try {
            // int rank = info.rank();
            // int size = info.size();
            // int remoteRank = (rank == 0 ? 1 : 0);

            IbisCapabilities capabilities =
                new IbisCapabilities(IbisCapabilities.CLOSED_WORLD,
                        IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED);

            PortType portType =
                new PortType(new String[] { PortType.SERIALIZATION_OBJECT,
                        PortType.CONNECTION_ONE_TO_MANY,
                        PortType.COMMUNICATION_RELIABLE ,PortType.RECEIVE_EXPLICIT, PortType.RECEIVE_POLL});

            System.err.println("creating ibis");
            
            ibis = IbisFactory.createIbis(capabilities, null, portType);
            
            int size = ibis.registry().getPoolSize();

            System.err.println("pool size = " + size);
            
            ibis.registry().waitUntilPoolClosed();

            System.err.println("pool closed");
            
            //list of all ibisses. should have the same size and order everywhere
            IbisIdentifier[] ibisses = ibis.registry().joinedIbises();

            int rank = -1;
            for (int i = 0; i < ibisses.length; i++) {
                if (ibisses[i].equals(ibis.identifier())) {
                    //he! this is me :)
                    rank = i;
                    break;
                }
            }
            if (rank == -1) {
                System.err.println("could not determine my rank");
                ibis.end();
                System.exit(1);
            }
            
            System.err.println("rank = " + rank);


            //what's this? -Niels
            int remoteRank = (rank == 0 ? 1 : 0);

            ReceivePort rports[] = new ReceivePort[size - 1];
            SendPort sport = ibis.createSendPort(portType);
            
            int tags[] = new int[size - 1];

            LTVector ltv = new LTVector();
            Operation op =
                new Operation(new ProcessIdentifier(ibis.identifier()),
                        new Long(0), new ReplicatedMethod("bla",
                                new Class[] { Integer.class },
                                new Object[] { new Integer(10) }), remoteRank);

            for (int i = 0, j = 0; i < size - 1; i++, j++) {

                if (j == rank)
                    j++;
                rports[i] =
                    ibis.createReceivePort(portType, "receive port for " + j);
                rports[i].enableConnections();
                tags[i] = 0;
            }
            


            long noBytes = 0;

            System.setProperty("ibis.name_server.key", "mpj_barrier");
//            MPJ.init(new String[] { "mpj.localcopyIbis" });
//            MPJ.COMM_WORLD.barrier();

            long start;

            if (rank == 0) {

                System.err.println(rank + "******* connecting to everyone");

                for (int j = 0; j < size; j++) {

                    if (j == rank)
                        j++;

                    System.err.println(rank + "*******  connect to " + ibisses[j]);

                    ReceivePortIdentifier id = sport.connect(ibisses[j], "receive port for " + rank);
                    
                    ltv.addEntry(new ProcessIdentifier(id.ibisIdentifier()), new Long(0));
                }

                System.err.println(rank + "*******  connect done ");

//                MPJ.COMM_WORLD.barrier();

                WriteMessage w;
                ReadMessage r;

                start = System.currentTimeMillis();
                for (int j = 0; j < NOPS; j++) {
                    w = sport.newMessage();
                    w.writeObject(new RepMILTMMessage(ltv, op));
                    noBytes += w.finish() * (size - 1);

                    // System.err.println(rank + "******* receive acks");

                    int counter = 0;

                    while (counter < size - 1) {

                        for (int k = 0; k < size - 1; k++) {
                            if (tags[k] == j) {
                                r = rports[k].receive();
                                if (r != null) {
                                    counter++;
                                    tags[k]++;
                                    RepMILTMMessage res =
                                        (RepMILTMMessage) r.readObject();
                                    noBytes += r.finish();
                                }
                            }
                        }
                    }
                }

                long end = System.currentTimeMillis();

                System.err.println("Time per round: " + (double) (end - start)
                        / NOPS);
                System.err.println("Average noB/message: " + noBytes / NOPS);

                sport.close();

            } else {
                System.err.println(rank + "******* connecting to everyone");

                for (int j = 0; j < size; j++) {

                    System.err.println(rank + "*******  connect to " + ibisses[j]);
                    
                    ReceivePortIdentifier id = sport.connect(ibisses[j], "receive port for " + rank);
                    
                    ltv.addEntry(new ProcessIdentifier(id.ibisIdentifier()), new Long(0));

                    if (j + 1 == rank)
                        j++;
                }

                System.err.println(rank + "*******  connect done ");

//                MPJ.COMM_WORLD.barrier();

                ReadMessage r;
                WriteMessage w;

                start = System.currentTimeMillis();
                for (int j = 0; j < NOPS; j++) {

                    // System.err.println(rank + "******* receive op");

                    r = rports[0].receive();
                    RepMILTMMessage res = (RepMILTMMessage) r.readObject();
                    tags[0]++;
                    noBytes += r.finish();

                    w = sport.newMessage();
                    w.writeObject(new RepMILTMMessage(ltv, op));
                    noBytes += w.finish() * (size - 1);

                    // System.err.println(rank + "******* receive acks");

                    int counter = 0;

                    while (counter < size - 2) {

                        for (int k = 0; k < size - 1; k++) {
                            if (tags[k] == j) {
                                r = rports[k].receive();
                                if (r != null) {
                                    counter++;
                                    tags[k]++;
                                    res = (RepMILTMMessage) r.readObject();
                                    noBytes += r.finish();
                                }
                            }
                        }
                    }
                }

                long end = System.currentTimeMillis();

                System.err.println("Time per round: " + (double) (end - start)
                        / NOPS);
                System.err.println("Average noB/message: " + noBytes / NOPS);

                sport.close();
            }

//            MPJ.finish();

            for (int i = 0; i < size - 1; i++)
                rports[i].close();

            ibis.end();

        } catch (IOException e) {
            System.out.println("Got exception " + e);
            System.out.println("StackTrace:");
            e.printStackTrace();
        } catch (IbisCreationFailedException e) {
            System.out.println("Got exception " + e);
            System.out.println("StackTrace:");
            e.printStackTrace();
//        } catch (MPJException mpje) {
//            System.out.println("Got exception " + mpje);
//            System.out.println("StackTrace:");
//            mpje.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Got exception " + e);
            System.out.println("StackTrace:");
            e.printStackTrace();
        }
    }
}
