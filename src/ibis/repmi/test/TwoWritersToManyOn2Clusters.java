package ibis.repmi.test;

import ibis.repmi.protocol.Replicateable;
import ibis.repmi.protocol.ReplicatedMethod;

public class TwoWritersToManyOn2Clusters extends VoidTest {

    public TwoWritersToManyOn2Clusters(long nops, int plwa, int plwm,
            int ncpus, Replicateable ro) {

        super(nops, plwa, plwm, ncpus, ro);

    }

    public void run() {

        super.run();

        /*
         * Random rand = new Random(0); int val;
         */
        Object[] args = new Object[1];
        args[0] = new Integer(2);

        System.setProperty("ibis.name_server.key", "mpj_barrier");

        if (pLWA == 0) {

            long start = System.currentTimeMillis();
            for (int i = 0; i < NOPS; i++) {
                try {
                    proto.processLocalWrite(new ReplicatedMethod(
                            "writeAddition", new Class[] { Integer.class },
                            args));
                } catch (SecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            while (proto.getRops() < NOPS) {
                ;
            }

            long end = System.currentTimeMillis();
            System.out.println("Time per operation (lops + rops): "
                    + (double) (end - start) / (2 * NOPS));

            proto.testReady();
        } else {
            long start = System.currentTimeMillis();

            while (proto.getRops() < 2 * NOPS) {
                ;
            }

            long end = System.currentTimeMillis();
            System.out.println("Time per operation: (rops)"
                    + (double) (end - start) / (2 * NOPS));
        }
        try {
            Long result = (Long) proto.processLocalRead(new ReplicatedMethod(
                    "readVal", (Class[]) null, null));
            System.out.println("Final result = " + result);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // System.setProperty("ibis.name_server.key",originalIbisNameServerKey);
        proto.processLeave();
    }
}
