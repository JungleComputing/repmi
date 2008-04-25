package ibis.repmi.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class LTVector implements Serializable {

    private HashMap ltm;

    private boolean isInit;

    public LTVector() {

        ltm = new HashMap();
        isInit = false;
    }

    /*
     * method for open world public void createEntry(ProcessIdentifier pkId,
     * HashMap entry) {
     * 
     * ltm.put(pkId, entry); }
     */

    public synchronized void init(ProcessIdentifier pi) {

        ltm.put(pi, new Long(0));

        // DEBUG
        System.out.println("Init LTVector for first ibis(" + pi.getUniqueId()
                + "): " + getEntry(pi));

        isInit = true;
        notifyAll();
    }

    public synchronized void init() {

        // DEBUG
        System.out.println("Init LTVector for simple node");

        isInit = true;
        notifyAll();
    }

    public synchronized void setLTM(LTVector localLTM) {
        // TODO Auto-generated method stub

        ltm = localLTM.ltm;
    }

    public synchronized void waitForInit() {
        // TODO Auto-generated method stub
        while (isInit == false) {
            try {
                // DEBUG
                System.out.println("waiting for LTM to become init ...");

                wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public synchronized void addEntry(ProcessIdentifier pnewId, Long initialTS) {

        Iterator it = ltm.entrySet().iterator();
        /*
         * set known ts of the newcomer as being the timestamp of the join
         * request operation
         */

        ltm.put(pnewId, new Long(initialTS.longValue()));
    }

    public synchronized void deleteEntry(ProcessIdentifier goodbye) {

        // delete its entry in the hashmap
        ltm.remove(goodbye);

    }

    public synchronized Long getEntry(ProcessIdentifier pkId) {

        Long pkTS = (Long) ltm.get(pkId);
        /*
         * process k has not been seen by this process yet so i add it
         */
        if (pkTS == null) {

            // DEBUG
            System.out.println("Can't find TS for " + pkId.getUniqueId());

            return new Long(0);
        }
        /*
         * Long ts = (Long)pkVT.get(plId);
         */
        /* process l has not been seen by this process yet */
        /*
         * if(ts == null) return null; return ts;
         */

        return pkTS;
    }

    private void setTS(ProcessIdentifier fromId, Long entry) {
        // TODO Auto-generated method stub
        ltm.put(fromId, entry);
    }

    public synchronized Set entrySet() {

        return ltm.entrySet();
    }

    public synchronized Set keys() {

        Set ret = new TreeSet();
        ret.addAll(ltm.keySet());
        return ret;
    }

    public synchronized void printDebug() {

        Iterator itpi = ltm.entrySet().iterator();
        Map.Entry mei;

        while (itpi.hasNext()) {
            mei = (Map.Entry) itpi.next();
            System.out.println("pid "
                    + ((ProcessIdentifier) mei.getKey()).hashCode() + " "
                    + (Long) mei.getValue());
        }
    }

    public void updateTS(ProcessIdentifier pid, long l) {
        // TODO Auto-generated method stub
        // if(((Long)(ltm.get(pid))).longValue() < l) ??? why?
        ltm.put(pid, new Long(l));
    }

    public void update(ProcessIdentifier identifier, LTVector remoteVT) {
        // TODO Auto-generated method stub
        Set remotePids = remoteVT.keys();

    }

}
