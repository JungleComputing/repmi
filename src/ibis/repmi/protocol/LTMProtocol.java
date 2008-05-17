package ibis.repmi.protocol;

import ibis.ipl.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ibis.repmi.comm.RepMIAckWelcomeMessage;
import ibis.repmi.comm.RepMILTMMessage;
import ibis.repmi.comm.RepMIMessage;
import ibis.repmi.comm.RepMIRPConnectUpcall;
import ibis.repmi.comm.RepMISOSMessage;
import ibis.repmi.comm.RepMISOSReplyMessage;
import ibis.repmi.comm.RepMIUpcall;
import ibis.repmi.comm.RepMIWelcomeMessage;
import ibis.util.Timer;

public class LTMProtocol {

    public static final long LATENCY = 300;

    private OpsQueue oq;

    private ProcessIdentifier localId;

    private LTVector localLTM;

    private SendPort sendPort;

    private Ibis ibis;

    private Replicateable ro = null;

    private boolean stop = false;

    private byte[] localWriteLock;

    private byte[] bcastLock;

    // private HashMap rpi;

    private RoundManager roundManager;

    private ExecutionThread executor;

    /* a HashMap of remote receivePortIdentifiers */
    private HashMap receivers;

    /* a HashMap of local receivePortIdentifiers */
    private HashMap myReceivers;

    private ReceivePort ibisRPExplicit;

    public static final PortType ptype = new PortType(new String[] {
            PortType.SERIALIZATION_OBJECT, PortType.CONNECTION_MANY_TO_MANY,
            PortType.COMMUNICATION_FIFO, PortType.COMMUNICATION_RELIABLE,
            PortType.RECEIVE_AUTO_UPCALLS, PortType.RECEIVE_EXPLICIT,
            PortType.CONNECTION_UPCALLS });

    public static final PortType explicitReceivePT = new PortType(new String[] {
            PortType.SERIALIZATION_OBJECT, PortType.CONNECTION_ONE_TO_MANY,
            PortType.COMMUNICATION_FIFO, PortType.COMMUNICATION_RELIABLE,
            PortType.RECEIVE_EXPLICIT });

    // MEAS
    private long readVal = 0;

    private long MAXVAL;

    private int NCPUS;

    private long elapsedTime;

    private long perOperationTime;

    private Timer timeInBcast = Timer.createTimer();

    private Timer timeWaitingEndRound = Timer.createTimer();

    private Timer timeWaitingStartRound = Timer.createTimer();

    public Timer timeInUpcalls = Timer.createTimer();

	public long recoveryTime = 0;

    // public Timer timeInJoinUpcalls = Timer.createTimer();
    // public Timer timeBetweenUpcalls = Timer.createTimer();
    // public static Timer critical = Timer.createTimer();

    public LTMProtocol(LTVector ltm, long timeout) {

        oq = new OpsQueue();
        // rpi = new HashMap();
        localLTM = ltm;
        localWriteLock = new byte[0];
        bcastLock = new byte[0];
        roundManager = new RoundManager(ltm, timeout);
        myReceivers = new HashMap();
        receivers = new HashMap();
    }

    public void setProcessIdentifier(ProcessIdentifier localProcessId) {
        localId = localProcessId;
        roundManager.setPid(localProcessId);
    }

    public void setSendPort(SendPort sp) {
        sendPort = sp;
    }

    public void setIbis(Ibis ibis) {
        // TODO Auto-generated method stub
        this.ibis = ibis;
    }

    public void setReplicatedObject(Replicateable ra) {

        if (ro == null)
            ro = ra;
    }

    public void setOpsQueue(OpsQueue oq2) {

        oq = oq2;

    }

    public OpsQueue getOpsQueue() {

        return oq;
    }

    // MEAS
    public void setMAXVAL(long maxval) {

        MAXVAL = maxval;
    }

    // MEAS
    public void setNCPUS(int ncpus) {

        NCPUS = ncpus;
    }

    // MEAS
    public boolean testReady() {

        // timeBetweenUpcalls.stop();

        elapsedTime = System.currentTimeMillis() - elapsedTime;
        // MEAS
        System.out.println("lops= " + executor.getLops() + "rops= "
                + executor.getRops() + "elapsedTime= " + elapsedTime + " "
                + "perOperationTime= " + perOperationTime + " "
                + "perOperationTotalTime= "
                + ((double) perOperationTime / executor.getLops()) + " \n"
                + "perBcastTime= " + timeInBcast.averageTime() + " "
                + "perStartRoundTime= " + timeWaitingStartRound.averageTime()
                + " " + "perEndRoundTime= " + timeWaitingEndRound.averageTime()
                + " " + "remoteOperationTotalTime= "
                + timeInUpcalls.totalTime() + " " + "perRemoteOperationTime= "
                + timeInUpcalls.averageTime() + " " +
                // "perJoinUpcallTime= " + timeInJoinUpcalls.averageTime() + " "
                // +
                // "totalCriticalTime= " + LTMProtocol.critical.totalTime() + "
                // " +
                // "perCriticalTime= " + LTMProtocol.critical.averageTime() + "
                // " +
                // "timeBetweenUpcallsAvg= " + timeBetweenUpcalls.averageTime()
                "");
        return true;

    }

    public void start() {

        /* needed to start the internal execution thread of the protocol */

        // timeBetweenUpcalls.start();
        // DEBUG
        System.out.println("OpsQueue initial size: "
                + roundManager.getCurrentQueue().size());

        executor = new ExecutionThread();
        roundManager.setExecutor(executor);
        roundManager.setNoConn(sendPort.connectedTo().length);
        roundManager.start();

        elapsedTime = System.currentTimeMillis();
    }

    public Object processLocalWrite(ReplicatedMethod write) {

        synchronized (localWriteLock) {
            long start = System.currentTimeMillis();
            Operation o;

            if (stop)
                return null;

            /*
             * //DEBUG System.out.println("Processing a local write");
             */

            o = new Operation(localId, null, write, Operation.LW);
            // oq.enqueue(o);

            // DEBUG MEAS
            timeWaitingStartRound.start();

            /* step a */
            roundManager.startNewRoundLW(o);

            // DEBUG MEAS
            timeWaitingStartRound.stop();

            // DEBUG MEAS
            timeInBcast.start();

            /* step b */
            broadcast(new RepMILTMMessage(localLTM, o));

            // DEBUG MEAS
            timeInBcast.stop();

            /* step c & d */
            Object result = null;

            // DEBUG MEAS
            timeWaitingEndRound.start();

            try {
                result = roundManager.waitForEndOfRound();
            } catch (RoundTimedOutException e) {
                // TODO Auto-generated catch block
                result = manageRecovery(e);
                recoveryTime += System.currentTimeMillis() - e.startTime; 
            }

            // DEBUG MEAS
            timeWaitingEndRound.stop();

            long end = System.currentTimeMillis();
            perOperationTime = perOperationTime + (end - start);
            return result;
        }
    }

    public Object processLocalRead(ReplicatedMethod read) {

        /*
         * synchronized(this) { if(stop) return null; }
         */
        /*
         * //DEBUG System.out.println("Processing a local read");
         */

        Object result = null;
        /*
         * //DEBUG System.out.println("read writeOp with TS: " + ts);
         */
        try {
            result = ro.getClass().getMethod(read.getName(),
                    read.getParamTypes()).invoke(ro, read.getArgs());
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return result;
    }

    public void processJoin(ProcessIdentifier fromId,
            ReceivePortIdentifier joinAckPort,
            ReceivePortIdentifier joiningIbis,
            ReceivePortIdentifier joinAckNormalNode) {

        synchronized (localWriteLock) {
            // timeInJoinUpcalls.start();
            Operation o;

            /*
             * if the join comes in while i am in leaving state, i let the
             * requesting node to contact another node after a timeout on the
             * connection
             */
            if (stop)
                return;

            // DEBUG
            System.out.println("Received a join request from "
                    + fromId.getUniqueId());

            o = new Operation(fromId, localId, null, Operation.JOIN,
                    joinAckPort, joiningIbis, joinAckNormalNode);

            roundManager.startNewRoundLW(o);

            broadcast(new RepMILTMMessage(localLTM, o));

            try {
                roundManager.waitForEndOfRound();
            } catch (RoundTimedOutException e) {
                manageRecovery(e);;
            }

            // timeInJoinUpcalls.stop();
        }
    }

    public void processLeave() {
        // DEBUG
        System.err.println("Processing leave request from (this ibis) "
                + ibis.identifier().name());

        synchronized (localWriteLock) {
            Operation o;
            if (stop == true) {
                System.out.println("U left already!");
                return;
            }
            stop = true;

            o = new Operation(localId, null, Operation.LEAVE);

            roundManager.startNewRoundLW(o);

            broadcast(new RepMILTMMessage(localLTM, o));

            try {
                roundManager.waitForEndOfRound();
            } catch (RoundTimedOutException e) {
                manageRecovery(e);
            }
        }
    }

    public void processRemoteOperation(Operation op, LTVector ltm,
            ProcessIdentifier fromId, ReadMessage m) {

        // DEBUG
        if (op.getType() == Operation.LEAVE)
            System.err.println("Receiving request from " + op.getPid()
                    + " to leave at (its) TS=" + op.getTS());

        // DEBUG MEAS
        Timer localRO = Timer.createTimer();
        localRO.start();

        Operation nop;
        if ((nop = roundManager.startNewRoundRW(op, m)) != null) {
            broadcast(new RepMILTMMessage(localLTM, nop));

            // DEBUG MEAS
            // System.err.println("sent a NOP to " + op.getPid().getUniqueId());
            // LTMProtocol.critical.stop();

            try {
                roundManager.waitForEndOfRound();
            } catch (RoundTimedOutException e) {
                manageRecovery(e);
                recoveryTime += System.currentTimeMillis() - e.startTime;
            }
        }

        // DEBUG MEAS
        localRO.stop();
        synchronized (this) {
            timeInUpcalls.add(localRO);
        }
    }

    public void broadcast(RepMIMessage mesg) {

        /*
         * //DEBUG System.out.println("starting bcast");
         */
        if (sendPort.connectedTo().length == 0) {
            /*
             * //DEBUG System.out.println("exiting bcast - no one else
             * listening");
             */
            return;
        }
        /*
         * //DEBUG System.out.println("bcasting to " +
         * sendPort.connectedTo().length + " nodes");
         */
        try {
            WriteMessage w = sendPort.newMessage();
            w.writeObject(mesg);
            w.finish();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        /*
         * //DEBUG System.out.println("finished bcast");
         */

    }

    public void executeLeave(Operation o) {

        synchronized (bcastLock) {

            /*
             * //DEBUG System.out.println("executing a LEAVE op" + "; TS: " +
             * o.getTS());
             */

            if (o.getPid().compareTo(localId) == 0) {

                synchronized (this) {
                    try {

                        // keepAlive.cancel();

                        // DEBUG
                        System.out.println(o.getPid().getUniqueId()
                                + " (this ibis) leaves");

                        ReceivePortIdentifier[] rpi = sendPort.connectedTo();

                        for (int i = 0; i < rpi.length; i++) {
                            try {
                                sendPort.disconnect(rpi[i]);
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                            }
                        }

                        sendPort.close();

                        executor.leave = true;

                        // DEBUG
                        System.err.println("Saying goodbye!");
                        ibis.end();

                        System.err.println("Ibis statistics:");
                        ibis.printManagementProperties(System.err);

                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } else {

                ReceivePortIdentifier goodbye = null;

                // DEBUG
                System.out.println(o.getPid().getUniqueId() + " leaves ");

                localLTM.deleteEntry(o.getPid());

                ReceivePortIdentifier[] rpi = sendPort.connectedTo();

                for (int i = 0; i < rpi.length; i++) {
                    if (rpi[i].ibisIdentifier().name().equals(
                            o.getPid().getUniqueId())) {
                        goodbye = rpi[i];
                        break;
                    }
                }

                // goodbye =
                // (ReceivePortIdentifier)rpi.get(o.getPid().getUniqueId());

                try {
                    if (goodbye != null)
                        sendPort.disconnect(goodbye);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                // DEBUG
                System.err.println("Disconnecting from "
                        + o.getPid().getUniqueId());
                roundManager.setNoConn(sendPort.connectedTo().length);
            }
        }
    }

    public void executeJoin(Operation o) {

        synchronized (bcastLock) {

            ReceivePortIdentifier newcomer;
            ReceivePort dedicatedRp = null;

            // DEBUG
            System.out.println("executing a JOIN op for "
                    + o.getPid().getUniqueId() + "; TS: " + o.getTS());

            if (o.getContact().compareTo(localId) == 0) {

                synchronized (this) {
                    localLTM.addEntry(o.getPid(), localLTM.getEntry(localId));
                    newcomer = o.getJoinPort();
                    try {
                        dedicatedRp = createNewRP(o.getPid().getUniqueId());
                        dedicatedRp.enableConnections();
                        dedicatedRp.enableMessageUpcalls();

                        SendPort joinAck = ibis
                                .createSendPort(LTMProtocol.ptype);
                        joinAck.connect(newcomer);
                        WriteMessage w = joinAck.newMessage();
                        w.writeObject(new RepMIWelcomeMessage(localLTM, ro,
                                roundManager.getRoundNo(), roundManager
                                        .getRestCurrentQueue(o.getPid()),
                                ibisRPExplicit.identifier(), dedicatedRp
                                        .identifier()));
                        w.finish();
                        joinAck.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                /*
                 * //DEBUG System.out.println("as contact node");
                 */

                try {
                    /*
                     * added temporarily until rpis can be found without
                     * nameserver in the loop
                     */
                    ReadMessage r = null;
                    r = ibisRPExplicit.receive();
                    RepMIAckWelcomeMessage raw = (RepMIAckWelcomeMessage) r
                            .readObject();
                    r.finish();

                    newcomer = raw.rpi;
                    /* end of added temp code */

                    addNewRpi(newcomer);
                    sendPort.connect(newcomer);
                    roundManager.setNoConn(sendPort.connectedTo().length);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                // to add sending a message on the join port of the newcomer
                // containing local operations
                // of all process in the system. the newcomer will know who to
                // wait for
                // using the matrix sent by the contact node.
                /*
                 * //DEBUG System.out.println("as simple node");
                 */

                localLTM.addEntry(o.getPid(), o.getTS());

                newcomer = o.getJoinNormalNodePort();
                try {
                    dedicatedRp = createNewRP(o.getPid().getUniqueId());
                    dedicatedRp.enableConnections();
                    dedicatedRp.enableMessageUpcalls();

                    SendPort joinAck = ibis.createSendPort(LTMProtocol.ptype);
                    joinAck.connect(newcomer);
                    WriteMessage w = joinAck.newMessage();
                    w.writeObject(new RepMIWelcomeMessage(localLTM, null, 0,
                            null, ibisRPExplicit.identifier(), dedicatedRp
                                    .identifier()));
                    w.finish();
                    joinAck.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                try {

                    /*
                     * added temporarily until rpis can be found without
                     * nameserver in the loop
                     */
                    ReadMessage r = null;
                    r = ibisRPExplicit.receive();
                    RepMIAckWelcomeMessage raw = (RepMIAckWelcomeMessage) r
                            .readObject();
                    r.finish();

                    newcomer = raw.rpi;
                    /* end of added temp code */

                    addNewRpi(newcomer);
                    sendPort.connect(newcomer);
                    roundManager.setNoConn(sendPort.connectedTo().length);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            bcastLock.notifyAll();
        }
    }

    class ExecutionThread {

        private Long internalLTS;

        private OpsQueue internalOQ;

        private long lops;

        private long rops;

        private boolean leave = false;

        ExecutionThread() {
            lops = 0;
            rops = 0;
        }

        public synchronized long getRops() {

            return rops;
        }

        public long getLops() {

            return lops;
        }

        public Object executeAllWrites(OpsQueue queue) {

            Operation o = null;
            Iterator it = queue.iterator();
            Object result = null;

            while (it.hasNext()) {

                o = (Operation) it.next();

                if (o.getType() == Operation.JOIN) {

                    executeJoin(o);
                } else if (o.getType() == Operation.LEAVE) {

                    executeLeave(o);
                } else if (o.getType() == Operation.NOPE) {

                } else {
                    /*
                     * //DEBUG System.out.println(o.getPid().getUniqueId() + ": " +
                     * o.getTS() + ": " + o.getType() + " (LR=0,LW=1,RW=2) " +
                     * o.getMethod().getName());
                     */
                    try {
                        // TODO decide whether a RW should also return this
                        // result
                        if (o.getType() == Operation.LW) {
                            result = ro.getClass().getMethod(
                                    o.getMethod().getName(),
                                    o.getMethod().getParamTypes()).invoke(ro,
                                    o.getMethod().getArgs());

                            // DEBUG
                            if (result != null)
                                System.out.println(result.toString());

                        } else {
                            ro.getClass().getMethod(o.getMethod().getName(),
                                    o.getMethod().getParamTypes()).invoke(ro,
                                    o.getMethod().getArgs());
                        }
                    } catch (IllegalArgumentException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (SecurityException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    /*
                     * if of type LW, wake up threads waiting for this operation
                     * to be executed
                     */

                    if (o.getType() == Operation.LW) {
                        synchronized (o) {
                            lops++;
                            o.notifyAll();
                        }
                    } else if (o.getType() == Operation.RW) {
                        synchronized (o) {
                            rops++;
                            o.notifyAll();
                        }
                    }

                }
            }
            return result;
        }
    }

    // MEAS
    public void waitForAllToJoin(int ncpus) {
        // TODO Auto-generated method stub
        synchronized (bcastLock) {
            while (sendPort.connectedTo().length < ncpus) {
                try {
                    bcastLock.wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        // DEBUG
        System.err.println("Everyone joined");
    }

    public void setRoundNo(long round) {
        // TODO Auto-generated method stub
        roundManager.setRoundNo(round);
    }

    public void setCurrentQueue(OpsQueue ops) {
        // TODO Auto-generated method stub
        roundManager.setCurrentQueue(ops);
    }

    public long getRops() {
        // TODO Auto-generated method stub
        return executor.getRops();
    }

    /* added temporarily until rpis can be found without nameserver in the loop */
    public void setIbisReceivePort(ReceivePort rpexpl) {
        // TODO Auto-generated method stub
        ibisRPExplicit = rpexpl;
    }

    public ReceivePort createNewRP(String sender) {
        // TODO Auto-generated method stub
        ReceivePort rp = null;
        try {
            rp = ibis.createReceivePort(ptype, "repmi-" + localId.getUniqueId()
                    + "-" + sender, new RepMIUpcall(this), new RepMIRPConnectUpcall(), null);
            rp.enableConnections();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (rp == null)
            return null;

        myReceivers.put(rp.identifier().name(), rp);
        return rp;
    }

    public void enableRPUpcalls() {
        // TODO Auto-generated method stub
        Iterator it = myReceivers.entrySet().iterator();
        while (it.hasNext()) {
            ((ReceivePort) ((Map.Entry) it.next()).getValue())
                    .enableMessageUpcalls();
        }
    }

    public void addNewRpi(ReceivePortIdentifier rpi) {
        // TODO Auto-generated method stub
        receivers.put(rpi.name(), rpi);
    }

    public void setIbisReceivePortIdentifier(ReceivePortIdentifier identifier) {
        // TODO Auto-generated method stub
        /*
         * when Many-to-Many is really used, a single receive port will be used
         * for comm with the rest of the "world". => ibisRPI = identifier
         */
    }

    public void processSOS(IbisIdentifier whoAsks, long ts, long recoveryRound) {

        // DEBUG
        System.err.println(ibis.identifier().name() + ": processing SOS from "
                + whoAsks.name());
        ProcessIdentifier inNeed = new ProcessIdentifier(whoAsks);
        roundManager.setPrevRoundInTrouble(ts, inNeed);
        // OpsQueue myOps = roundManager.getOpsQueue(ts);
        Object[] myOps = roundManager.getOpsList(inNeed,ts);
        RepMISOSReplyMessage sosreply = new RepMISOSReplyMessage(myOps, whoAsks
                .name(), recoveryRound, ts);
        synchronized (bcastLock) {
            broadcast(sosreply);
        }
    }

    public void processSOSReply(IbisIdentifier whoAnswered, String whoAsked,
            long ts, Object[] objects) {

        // DEBUG
        System.err.println(ibis.identifier().name()
                + ": processing SOS Reply from " + whoAnswered.name() + " for "
                + whoAsked);

        roundManager.setPrevRoundInTrouble(ts, new ProcessIdentifier(whoAsked));
        /* see if it is for me or not */
        roundManager.processReceivedQueue(objects, ts);

        if (whoAsked.compareTo(this.ibis.identifier().name()) == 0) {
            roundManager.receivedSOSReply(whoAnswered);
        }
    }

    protected Object manageRecovery(RoundTimedOutException e) {
        e.printStackTrace();
        RepMISOSMessage sos = roundManager.startNewRecoveryRound();
        broadcast(sos);
        while (roundManager.waitForEndOfRecoveryRound()) {
            sos = roundManager.startNewRecoveryRound();
            broadcast(sos);
        }
        Object result = roundManager.endRecoveredRound();
        return result;
    }
}
