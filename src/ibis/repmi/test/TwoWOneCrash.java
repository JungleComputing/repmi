package ibis.repmi.test;

import java.io.IOException;

import ibis.repmi.protocol.Replicateable;
import ibis.repmi.protocol.ReplicatedMethod;

public class TwoWOneCrash extends VoidTest {

    protected String writerCluster;

    public TwoWOneCrash(long nops, int plwa, int plwm, int ncpus,
            Replicateable ro) {

        super(nops, plwa, plwm, ncpus, ro);
    }

    public void run() {

        super.run();

        Object[] args = new Object[1];
        args[0] = new Integer(10);

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

            // MPJ.COMM_WORLD.barrier();
            long end = System.currentTimeMillis();
            System.out.println("Time per operation (lops): "
                    + (double) (end - start - proto.recoveryTime) / NOPS);

            // Wait for what???

            proto.testReady();
        } else {
            if (pLWA == 1) {
                long start = System.currentTimeMillis();
                while (proto.getRops() < NOPS) {
                    ;
                }
                // MPJ.COMM_WORLD.barrier();
                long end = System.currentTimeMillis();
                System.err.println("Time per operation: (rops)"
                        + (double) (end - start - proto.recoveryTime) / NOPS);
            } else {

                long start = System.currentTimeMillis();
                while (proto.getRops() < NOPS / 2) {
                    ;
                }
                // MPJ.COMM_WORLD.barrier();
                long end = System.currentTimeMillis();
                System.err.println("Time per operation: (rops)" + (double) 2
                        * (end - start) / NOPS);
                try {
                    Long result = (Long) proto
                            .processLocalRead(new ReplicatedMethod("readVal",
                                    (Class[]) null, null));
                    System.err.println("Final result = " + result);
                } catch (SecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                try {
                    ibis.end();
                    /* it should work with the next statement */
                    System.exit(0);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        // MPJ.finish();

        try {
            Long result = (Long) proto.processLocalRead(new ReplicatedMethod(
                    "readVal", (Class[]) null, null));
            System.err.println("Final result = " + result);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // System.setProperty("ibis.name_server.key",originalIbisNameServerKey);
        proto.processLeave();
    }

}
