package ibis.repmi.protocol;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.repmi.comm.RepMISOSMessage;
import ibis.repmi.protocol.LTMProtocol.ExecutionThread;
import ibis.util.Timer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class RoundManager {

    private long TS;

    private ProcessIdentifier localId;

    private LTVector localVT;

    private OpsQueue currentQueue; /* ordered by Pid */

    private OpsQueue nextQueue; /* ordered by Pid */

    private OpsQueue cacheQueue; /* ordered by Pid */

    /* internals */

    private int expectedNo = 0;

    private byte[] endRLock;

    private byte[] recoveryLock;

    private ExecutionThread executor;

    private boolean lwtookover = false;

    // MEAS & DEBUG
    private int roundNo = 1; // first round -> roundNo = 1

    private long recoveryRound = 0; // by default i am in no recovery round

    private boolean inRecovery = false;

    private PIList crashed, crashedNextRound, crashedInRecovery, currentD,
            nextD, helpQ;

    private int alive;

    public RoundManager(LTVector ltv, long timeout) {

        TS = 0;
        localVT = ltv;
        currentQueue = new OpsQueue();
        nextQueue = new OpsQueue();
        cacheQueue = new OpsQueue();
        endRLock = new byte[0];
        recoveryLock = new byte[0];
        crashed = new PIList();
        crashedNextRound = new PIList();
        crashedInRecovery = new PIList();
        currentD = new PIList();
        nextD = new PIList();
        helpQ = new PIList();
    }

    public void setPid(ProcessIdentifier lid) {

        localId = lid;
    }

    public void setExecutor(ExecutionThread exe) {

        executor = exe;
    }

    public synchronized void startNewRoundLW(Operation op) {

        // DEBUG
        if (op.getType() == Operation.LEAVE)
            System.err.println("This Ibis " + op.getPid()
                    + " wants to leave at TS=" + TS + " current round = "
                    + roundNo);

        if (currentQueue.size() == 0) {

            // DEBUG
            System.err.println("starting new round LW, current round is:"
                    + roundNo);

            op.setTS(TS);
            localVT.updateTS(localId, op.getTS().longValue());
            currentQueue.enqueue(op);
        } else {

            // DEBUG
            System.err
                    .println("waiting to start new round LW, current round is:"
                            + roundNo);

            if (nextQueue.size() == 0) {
                op.setTS(1 - TS);
                nextQueue.enqueue(op);
                while (op.getTS().longValue() != TS) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                localVT.updateTS(localId, op.getTS().longValue());
            } else {
                op.setTS(1 - TS);
                nextQueue.enqueue(op);
                lwtookover = true;
                while (op.getTS().longValue() != TS) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                localVT.updateTS(localId, op.getTS().longValue());
            }
        }

        // DEBUG
        System.out.println("new round LW started: " + roundNo);

        return;
    }

    public synchronized Operation startNewRoundRW(Operation op, ReadMessage m) {

        // DEBUG
        if (op.getType() == Operation.LEAVE)
            System.err.println("Ibis " + op.getPid() + " wants to leave at TS="
                    + TS + " current round = " + roundNo);

        if (currentQueue.size() == 0) {
            if (op.getTS().longValue() != TS) {
                System.err.println("OUCH!!!!! round = " + roundNo);
                return null;
            }
            try {
                m.finish();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Operation ol = new Operation(localId, null, Operation.NOPE);
            ol.setTS(TS);
            localVT.updateTS(localId, TS);

            currentQueue.enqueue(op);
            currentQueue.enqueue(ol);
            return ol;
        } else {
            if (op.getTS().longValue() == TS) {
                if (op.getType() != Operation.JOIN) {
                    localVT.updateTS(op.getPid(), op.getTS().longValue());
                } else {
                    localVT.updateTS(op.getContact(), op.getTS().longValue());
                }
                synchronized (endRLock) {
                    currentQueue.enqueue(op);
                    if ((currentQueue.size() + crashed.size()) == expectedNo)
                        endRLock.notifyAll();
                }
                return null;
            } else {
                if (nextQueue.size() != 0) {
                    nextQueue.enqueue(op);
                    return null;
                } else {
                    try {
                        m.finish();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    nextQueue.enqueue(op);
                    while ((op.getTS().longValue() != TS)
                            && (lwtookover != true)) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    if (lwtookover) {
                        lwtookover = false;
                        return null;
                    } else {
                        Operation ol = new Operation(localId, null,
                                Operation.NOPE);
                        ol.setTS(TS);
                        localVT.updateTS(localId, TS);

                        currentQueue.enqueue(ol);
                        return ol;
                    }
                }
            }
        }
    }

    public void processNextQueue() {

        if (nextQueue.size() == 0) {
            return;
        }

        Operation op;

        while ((op = nextQueue.dequeue()) != null) {

            if (op.getType() != Operation.JOIN) {
                localVT.updateTS(op.getPid(), op.getTS().longValue());
            } else {
                localVT.updateTS(op.getContact(), op.getTS().longValue());
            }
            currentQueue.enqueue(op);
        }
    }

    public Object waitForEndOfRound() throws RoundTimedOutException {

        // DEBUG
        // System.out.println("waiting to finish round: " + roundNo);

        synchronized (endRLock) {
            /*
             * condition when assuming no faults while (currentQueue.size() !=
             * expectedNo) {
             */
            while ((currentQueue.size() + crashed.size()) != expectedNo) {
                try {
                    endRLock.wait();
                    /* woke up by timeout */

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (crashed.size() != 0) {
                throw new RoundTimedOutException();
            }
        }

        Object result = endRound();
        return result;
    }

    public void crashed(ProcessIdentifier c) {

        // DEBUG
        System.err.println("crashed node " + c);
        synchronized (this) {
            if (currentQueue.contains(c)) {
                if (nextQueue.contains(c)) {
                    nextD.add(c);
                } else {
                    crashedNextRound.add(c);
                }
            } else {
                synchronized (endRLock) {
                    crashed.add(c);
                    if ((currentQueue.size() + crashed.size()) == expectedNo)
                        endRLock.notifyAll();
                }
            }
        }
        synchronized (recoveryLock) {
            if (inRecovery) {
                if (!helpQ.contains(c)) {
                    crashedInRecovery.add(c);
                    if ((helpQ.size() + crashedInRecovery.size()) == alive)
                        recoveryLock.notifyAll();
                }
            }
        }
    }

    private synchronized Object endRound() {

        Object result;
        result = executor.executeAllWrites(currentQueue);

        executor.executeAllWrites(currentD.toDeleteOpsQ());
        nextD.moveTo(currentD);
        crashedNextRound.moveTo(crashed);

        cacheQueue.clear();
        currentQueue.move(cacheQueue);
        processNextQueue();
        TS = 1 - TS;

        // DEBUG
        // System.out.println("finished round: " + roundNo);

        roundNo++;

        notifyAll();
        return result;
    }

    public void setNoConn(int expNo) {

        synchronized (endRLock) {
            expectedNo = expNo + 1;
            endRLock.notifyAll();
        }
    }

    public synchronized void setRoundNo(long roundSn) {

        TS = roundSn;

    }

    public synchronized long getRoundNo() {

        return TS;
    }

    public OpsQueue getRestCurrentQueue(ProcessIdentifier pid) {

        OpsQueue res = new OpsQueue();

        Iterator it = currentQueue.iterator();
        Operation op;

        while (it.hasNext()) {
            op = (Operation) it.next();
            if (op.getPid().compareTo(pid) > 0) {
                res.enqueue(op);
            }
        }

        return res;
    }

    public synchronized void setCurrentQueue(OpsQueue ops) {

        currentQueue = ops;
    }

    public synchronized void start() {

        executor.executeAllWrites(currentQueue);

        currentQueue.move(cacheQueue);
        processNextQueue();
        TS = 1 - TS;

        // DEBUG
        System.out.println("finished round: " + roundNo);
        roundNo++;
    }

    public synchronized OpsQueue getCurrentQueue() {

        return currentQueue;
    }

    public RepMISOSMessage startNewRecoveryRound(SendPort sp) {

        synchronized (recoveryLock) {
            inRecovery = true;
            /* alive = sp.connectedTo().length; */
            alive = currentQueue.size()
                    - (nextD.size() + crashedNextRound.size()) - 1;
            recoveryRound++;

            // DEBUG
            System.err.println("new recovery round started with " + alive
                    + " alive nodes");

            return new RepMISOSMessage(recoveryRound, TS);
        }
    }

    public boolean waitForEndOfRecoveryRound() {
        // TODO Auto-generated method stub

        synchronized (recoveryLock) {

            while ((helpQ.size() + crashedInRecovery.size()) != alive) {
                try {
                    recoveryLock.wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            // DEBUG
            System.err
                    .println("finished recovery round: " + recoveryRound
                            + " ; alive: " + alive + "  ; \n helpQ: (" + helpQ
                            + ") ; \n crashed: (" + crashed + ") ; \n nextD: ("
                            + nextD + ") ; \n currentD: (" + currentD
                            + ") ; \n crashed in recovery: ("
                            + crashedInRecovery + ")");

            processHelpQueue();

            if ((currentQueue.size() != expectedNo)
                    && (crashedInRecovery.size() != 0)) {
                // helpQ.clear(); will be already cleared by process helpQueue
                crashedInRecovery.clear();
                return true;
            } else
                return false;
        }
    }

    private void processHelpQueue() {

        helpQ.moveTo(currentQueue);
    }

    public synchronized Object[] getOpsList(ProcessIdentifier whoAsks, long ts) {

        if (TS == ts)
            return currentQueue.toList();
        else {
            if (currentQueue.contains(whoAsks)) {
                return nextQueue.toList();
            }
            return cacheQueue.toList();
        }
    }

    public void receivedSOSReply(IbisIdentifier whoAnswered, Object[] objects) {
        synchronized (recoveryLock) {

            helpQ.add(new ProcessIdentifier(whoAnswered), objects);

            // DEBUG
            System.err.println("Received SOSReply from " + whoAnswered.name());
            System.err.println("Help queue size is now " + helpQ.size());
            System.err.println("crashed in recovery queue size is now "
                    + crashedInRecovery.size());

            if ((helpQ.size() + crashedInRecovery.size()) == alive) {

                // DEBUG
                System.err.println("Notify all on recoveryLock");

                recoveryLock.notifyAll();
            }
        }
    }

    public Object endRecoveredRound() {

        synchronized (recoveryLock) {

//          DEBUG
            System.err.println("finished recovered round: " + roundNo 
                    + "  ; \n currentQ: (" + currentQueue 
                    + ") ; \n helpQ: (" + helpQ + ") ; \n crashed: (" + crashed
                    + ") ; \n nextD: (" + nextD + ") ; \n currentD: ("
                    + currentD + ") ; \n crashed in recovery: ("
                    + crashedInRecovery + ")");
            
            if (currentQueue.size() < expectedNo) {
                crashed.toDeleteOpsQ(currentQueue);
            }
            inRecovery = false;
            recoveryRound = 0;
        }

        Object result = endRound();

        // DEBUG
        System.err.println("expectedNo: " + expectedNo);

        return result;
    }

    public void decNoConn() {
        // TODO Auto-generated method stub
        expectedNo--;
    }

    public void incNoConn() {
        // TODO Auto-generated method stub
        expectedNo++;
    }
}
