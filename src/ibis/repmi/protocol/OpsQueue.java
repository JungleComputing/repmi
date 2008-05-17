package ibis.repmi.protocol;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class OpsQueue implements Serializable {

    private TreeSet queue;

    // private Operation lastLW = null;

    public OpsQueue() {

        queue = new TreeSet(new OpsComparator());
    }

    public synchronized void enqueue(Operation op) {

        queue.add(op);
        /*
         * if (op.getType() == Operation.LW) { // LET OP ! assumption that local
         * writes are implicitely ordered by // timestamp lastLW = op; }
         */
    }

    public synchronized Operation dequeue(Long maxTs) {

        /*
         * should return the operation with minimum TS, that is lower than maxTS
         * and also delete it from the queue;
         */

        if (queue.isEmpty())
            return null;

        Operation ret = (Operation) queue.first();
        /* there are no more operations with TS lower than maxTS */
        if (ret.getTS().compareTo(maxTs) > 0)
            return null;

        queue.remove(ret);
        return ret;

    }

    public synchronized Operation dequeue() {

        if (queue.isEmpty())
            return null;

        Operation ret = (Operation) queue.first();
        queue.remove(ret);
        return ret;
    }

    public synchronized int move(OpsQueue destQ, Long maxTs) {

        Operation op;

        if (queue.size() == 0)
            return 0;

        while ((op = dequeue(maxTs)) != null) {
            destQ.enqueue(op);
        }

        return destQ.queue.size();
    }

    public synchronized int move(OpsQueue destQ) {

        Operation op;

        if (queue.size() == 0)
            return 0;

        while ((op = dequeue()) != null) {
            destQ.enqueue(op);
        }

        return destQ.queue.size();
    }

    public synchronized void clear() {

        queue.clear();
    }

    /*
     * public synchronized boolean localWritesPending() {
     * 
     * if (lastLW != null) return true; return false; }
     * 
     * public synchronized Operation getLastLocalWrite() {
     * 
     * return lastLW; }
     */
    public synchronized boolean contains(Operation op) {

        if (op == null)
            return false;
        /*
         * //DEBUG System.out.println("Queue of size " + queue.size() +" is
         * checked for containing op with " + "timestamp " + op.getTS());
         */
        return queue.contains(op);
    }

    public synchronized boolean contains(ProcessIdentifier pi) {
        
        //return queue.contains(new Operation(pi, new Long(), 0));
    	Iterator iterator = queue.iterator();
    	while (iterator.hasNext()) {
    		if (((Operation)iterator.next()).getPid().equals(pi)) 
    			return true;
    	}
    	return false;
    }
    
    public synchronized Iterator iterator() {

        return queue.iterator();
    }

    public synchronized void merge(OpsQueue oq) {

        if (oq == null)
            return;
        Operation op;
        while ((op = oq.dequeue()) != null) {

            enqueue(op);
        }
    }

    public synchronized int size() {
        // TODO Auto-generated method stub
        return queue.size();
    }

    public Object[] toList() {
        return queue.toArray();
    }

    public synchronized OpsQueue copy() {
        // TODO Auto-generated method stub
        OpsQueue newOq = new OpsQueue();
        newOq.queue = new TreeSet(new OpsComparator());
        for (Object ob : queue) {
            newOq.enqueue(new Operation((Operation) ob));
        }
        return newOq;
    }

    public void merge(Object[] objects) {
        for (Object ob : queue) {
            enqueue((Operation) ob);
        }
    }
}
