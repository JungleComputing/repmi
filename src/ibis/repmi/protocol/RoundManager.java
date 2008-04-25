package ibis.repmi.protocol;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.repmi.comm.RepMISOSMessage;
import ibis.repmi.protocol.LTMProtocol.ExecutionThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class RoundManager {

    private long TIMEOUT;

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

    private boolean prevRoundInTrouble = false;

    private List replied;

    private int alive;

    public RoundManager(LTVector ltv, long timeout) {

        TS = 0;
        TIMEOUT = timeout;
        localVT = ltv;
        currentQueue = new OpsQueue();
        nextQueue = new OpsQueue();
        cacheQueue = new OpsQueue();
        endRLock = new byte[0];
        recoveryLock = new byte[0];
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
            op.setTS(TS);
            localVT.updateTS(localId, op.getTS().longValue());
            currentQueue.enqueue(op);
        } else {
            /*
             * // DEBUG System.out.println("waiting to start new round LW,
             * current round is:" + currentRound);
             */
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
        /*
         * // DEBUG System.out.println("new round LW started: " + currentRound);
         */
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
                currentQueue.enqueue(op);
                synchronized (endRLock) {
                    if (currentQueue.size() == expectedNo)
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
            while (currentQueue.size() != expectedNo) {
                try {
                    endRLock.wait(TIMEOUT);
                    /* woke up by timeout */

                    if (currentQueue.size() != expectedNo) {
                        faultRecovery();
                    }

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        synchronized (this) {
            Object result;
            result = executor.executeAllWrites(currentQueue);

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

    public void faultRecovery() throws RoundTimedOutException {
        /* check again i miss ops */
        if (currentQueue.size() == expectedNo)
            return;
        if (isPrevRoundInTrouble()) {
            try {
                /* wait for the prev round to recover put a better decision
                 * making condition */
                endRLock.wait(2 * expectedNo * expectedNo * TIMEOUT);
                resetPrevRoundInTrouble();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            /* i am in trouble */
            replied = Collections.synchronizedList(new ArrayList());
            alive = currentQueue.size();

            // DEBUG
            System.err.println("Round timing out after answered " + alive);

            throw new RoundTimedOutException();
        }
    }

    public boolean isPrevRoundInTrouble() {
        synchronized (recoveryLock) {
            return prevRoundInTrouble;
        }
    }

    /* for now i ignore a node in a next round would wake up due to timeout */
    public void setPrevRoundInTrouble(long ts) {
        synchronized (recoveryLock) {
            if (TS != ts)
                prevRoundInTrouble = true;
        }
    }

    public void resetPrevRoundInTrouble() {
        synchronized (recoveryLock) {
            prevRoundInTrouble = false;
        }
    }

    public RepMISOSMessage startNewRecoveryRound() {

        synchronized (recoveryLock) {

            recoveryRound++;

            // DEBUG
            System.err.println("new recovery round started after answered "
                    + replied.size() + " out of " + alive);
            
            return new RepMISOSMessage(recoveryRound, TS);

        }
    }

    public boolean waitForEndOfRecoveryRound() {
        // TODO Auto-generated method stub
        synchronized (recoveryLock) {
            try {
                recoveryLock.wait(expectedNo * expectedNo * TIMEOUT);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // DEBUG
            System.err.println("finished recovery round: " + recoveryRound
                    + "; replied = " + replied.size());

            if ((currentQueue.size() != expectedNo)
                    && (replied.size() != alive)) {
                alive = replied.size();
                replied.clear();
                return true;
            } else
                return false;
        }
    }

    public synchronized OpsQueue getOpsQueue(long ts) {

        if (TS == ts)
            return currentQueue.copy();
        else
            return cacheQueue.copy();
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

    public synchronized void processReceivedQueue(Object[] objects, long ts) {
        if (TS == ts) {
            currentQueue.merge(objects);
        } else
            return;
    }

    public void receivedSOSReply(IbisIdentifier whoAnswered) {
        synchronized (recoveryLock) {

            // DEBUG
            System.err.println("Received SOSReply from " + whoAnswered.name());
            System.err.println("Queue size is now " + currentQueue.size());

            replied.add(whoAnswered);
            if (replied.size() == alive) {
                System.err.println("Notify all on recoveryLock");
                recoveryLock.notifyAll();                
            }
                
        }
    }

    public synchronized Object endRecoveredRound() {
        Object result;
        result = executor.executeAllWrites(currentQueue);

        cacheQueue.clear();
        currentQueue.move(cacheQueue);
        processNextQueue();
        TS = 1 - TS;

        // DEBUG
        System.err.println("finished recovered round: " + roundNo
                + "; alive = " + alive);

        recoveryRound = 0;
        expectedNo = alive + 1;

        roundNo++;

        notifyAll();
        return result;

    }
}
