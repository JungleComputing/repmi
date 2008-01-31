package ibis.repmi.test;

public class Test {
    static final int ONE_WRITER = 0;

    static final int TWO_WRITERS_1_CLUSTER = 1;

    static final int TWO_WRITERS_2_CLUSTERS = 2;

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        VoidTest test = null;
        long nops = Long.parseLong(args[1]);
        int cpuId = Integer.parseInt(args[2]);
        int ncpus = Integer.parseInt(args[3]);
        String writerCluster = args[4];
        
        switch (Integer.parseInt(args[0])) {
        case ONE_WRITER:
            test = new OneWriteToMany(nops, cpuId, 0, ncpus, writerCluster);
            break;
        case TWO_WRITERS_1_CLUSTER:
            test = new TwoWritersToMany(nops, cpuId, 0, ncpus);
            break;
        case TWO_WRITERS_2_CLUSTERS:
            test = new TwoWritersToManyOn2Clusters(nops, cpuId, 0, ncpus);
            break;
        default:
            System.out.println("No test selected");
            System.out.println("Usage: Test test_type params");
            System.out.println("\t test_type = 0 => ONE_WRITER");
            System.out.println("\t test_type = 1 => TWO_WRITERS_1_CLUSTER");
            System.out.println("\t test_type = 2 => TWO_WRITERS_2_CLUSTERS");    
            System.out.println("\t params = No_operations cpuId No_cpus [writer's cluster]");            
            System.exit(0);
        }

        test.run();
        System.out.println("Test succeeded!");
        System.exit(0); // let shutdown hook terminate ibis
    }
}
