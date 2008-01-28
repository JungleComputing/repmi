package ibis.repmi.test;


import ibis.mpj.*;
import ibis.repmi.protocol.ReplicatedMethod;

public class OneWriteToMany extends VoidTest {

	public OneWriteToMany(long nops, int plwa, int plwm, int ncpus,
			String wC) {

		super(nops,plwa,plwm,ncpus,wC);
	}

	public void run() {

		super.run();

		Object[] args = new Object[1];
		args[0] = new Integer(10);	                      

		try {

			System.setProperty("ibis.name_server.key","mpj_barrier");
			System.setProperty("ibis.pool.total_hosts", NCPUS + 1 + "");
			MPJ.init(new String[] {"mpj.localcopyIbis"});
			MPJ.COMM_WORLD.barrier();

			if((pLWA == 0) && 
					(ibis.identifier().cluster().compareTo(this.writerCluster) == 0)) {

				long start = System.currentTimeMillis();

				for(int i=0; i<NOPS; i++) {
					try {           			
						proto.processLocalWrite(new ReplicatedMethod(
								"writeAddition", new Class[] {Integer.class}, args));
					} catch (SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				MPJ.COMM_WORLD.barrier();
				long end = System.currentTimeMillis();
				System.out.println("Time per operation (lops): " + (double)(end-start)/NOPS);

				proto.testReady();
			} else {
				long start = System.currentTimeMillis();
				while(proto.getRops() < NOPS) {;}
				MPJ.COMM_WORLD.barrier();
				long end = System.currentTimeMillis();
				System.out.println("Time per operation: (rops)" + (double)(end-start)/NOPS);
			}
			MPJ.finish();
			proto.processLeave();
		} catch(MPJException mpje) {
			mpje.printStackTrace();
		}
	}
}


