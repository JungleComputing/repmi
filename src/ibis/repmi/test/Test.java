package ibis.repmi.test;

import ibis.repmi.protocol.Replicateable;

public class Test {
    static final int ONE_WRITER = 0;

    static final int TWO_WRITERS_1_CLUSTER = 1;

    static final int TWO_WRITERS_2_CLUSTERS = 2;

    static final int ONE_WRITER_W_CRASH = 3;

    static final int NO_TEST = 4;

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        if (args.length < 5) {
            System.out.println("Too few parameters");
            System.out
                    .println("Usage: Test test_type No_operations cpuId No_cpus timeout [writer's cluster]");
            System.out.println("\t test_type = 0 => ONE_WRITER");
            System.out.println("\t test_type = 1 => TWO_WRITERS_1_CLUSTER");
            System.out.println("\t test_type = 2 => TWO_WRITERS_2_CLUSTERS");
            System.exit(0);
        }
        VoidTest test = null;
        long nops = Long.parseLong(args[1]);
        int cpuId = Integer.parseInt(args[2]);
        int ncpus = Integer.parseInt(args[3]);
        long timeout = Long.parseLong(args[4]);

        int testType;
        if (args.length == 0)
            testType = NO_TEST;
        else
            testType = Integer.parseInt(args[0]);

        Replicateable ra = new ReplicatedAccount();

        switch (testType) {
        case ONE_WRITER:
            test = new OneWriteToMany(nops, cpuId, 0, ncpus, args[5], ra);
            ((OneWriteToMany) test).run();
            break;
        case TWO_WRITERS_1_CLUSTER:
            test = new TwoWritersToMany(nops, cpuId, 0, ncpus, ra);
            ((TwoWritersToMany) test).run();
            break;
        case TWO_WRITERS_2_CLUSTERS:
            test = new TwoWritersToManyOn2Clusters(nops, cpuId, 0, ncpus, ra);
            ((TwoWritersToManyOn2Clusters) test).run();
            break;
        case ONE_WRITER_W_CRASH:
            test = new OneWCrash(nops, cpuId, 0, ncpus, args[5], ra);
            ((OneWCrash) test).run();
            break;
        default:
            System.out.println("No test selected");
            System.out
                    .println("Usage: Test test_type No_operations cpuId No_cpus timeout [writer's cluster]");
            System.out.println("\t test_type = 0 => ONE_WRITER");
            System.out.println("\t test_type = 1 => TWO_WRITERS_1_CLUSTER");
            System.out.println("\t test_type = 2 => TWO_WRITERS_2_CLUSTERS");
            System.exit(0);
        }

        System.out.println("Test succeeded!");
        System.exit(0); // let shutdown hook terminate ibis
    }
}
