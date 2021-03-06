package ibis.repmi.test;

import ibis.repmi.protocol.Replicateable;
import ibis.repmi.protocol.ReplicatedMethod;

public class TwoWritersToMany extends VoidTest {

    public TwoWritersToMany(long nops, int plwa, int plwm, int ncpus,
            Replicateable ro) {

        super(nops, plwa, plwm, ncpus, ro);
    }

    public void run() {

        super.run();

        System.setProperty("ibis.name_server.key", "mpj_barrier");

        if ((pLWA == 1) || (pLWA == 0)) {

            Object[] args = new Object[1];
            args[0] = new Integer(2);

            // DEBUG
            System.out.println("My rank is: " + pLWA);

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
        } /*
             * else if(pLWA == 0) { Object[] args = new Object[1]; args[0] = new
             * Integer(2);
             * 
             * 
             * //DEBUG System.out.println("My rank is: " + pLWA);
             * 
             * long start = System.currentTimeMillis();
             * 
             * for(int i=0; i<NOPS; i++) { try { proto.processLocalWrite(new
             * ReplicatedMethod( "writeMultiplication", new Class[]
             * {Integer.class}, args)); } catch (SecurityException e) { // TODO
             * Auto-generated catch block e.printStackTrace(); } }
             * 
             * while(proto.getRops() < NOPS) {;}
             * 
             * MPJ.COMM_WORLD.barrier(); long end = System.currentTimeMillis();
             * System.out.println("Time per operation (lops + rops): " +
             * (double)(end-start)/NOPS);
             * 
             * proto.testReady(); }
             */
        else {
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
