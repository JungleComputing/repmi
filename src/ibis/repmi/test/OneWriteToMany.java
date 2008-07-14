package ibis.repmi.test;

import java.io.IOException;

import ibis.repmi.protocol.Replicateable;
import ibis.repmi.protocol.ReplicatedMethod;

public class OneWriteToMany extends VoidTest {

    protected String writerCluster;

    public OneWriteToMany(long nops, int plwa, int plwm, int ncpus,
            String wC, Replicateable ro) {

        super(nops, plwa, plwm, ncpus, ro);
        writerCluster = wC;
    }

    public void run() {

//      DEBUG
        System.err.println("calling super");
        
        super.run();

//      DEBUG
//        System.err.println("I made it outside super");
        
        Object[] args = new Object[1];
        args[0] = new Integer(10);

        int clusterLevel = ibis.identifier().location().getLevels().length - 1; 
        
        if ((pLWA == 0)
                && ibis.identifier().location().getLevel(clusterLevel).contains(
                        this.writerCluster)) {

            //DEBUG
            System.err.println("I am writer");
            
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
                    + (double) (end - start) / NOPS);

            proto.testReady();
        } else {
            
//          DEBUG
            System.err.println("I am reader");
            
            long start = System.currentTimeMillis();
            while (proto.getRops() < NOPS) {
                ;
            }
            // MPJ.COMM_WORLD.barrier();
            long end = System.currentTimeMillis();
            System.err.println("Time per operation: (rops)"
                    + (double) (end - start) / NOPS);
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
