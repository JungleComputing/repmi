package ibis.repmi.test;

public class Test {
    static final int ONE_WRITER = 0;

    static final int TWO_WRITERS_1_CLUSTER = 1;

    static final int TWO_WRITERS_2_CLUSTERS = 2;

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        VoidTest test = null;
        switch (Integer.parseInt(args[0])) {
        case ONE_WRITER:
            test = new OneWriteToMany(Long.parseLong(args[1]), Integer
                    .parseInt(args[2]), 0, Integer.parseInt(args[3]), args[4]);
            break;
        case TWO_WRITERS_1_CLUSTER:
            test = new TwoWritersToMany(Long.parseLong(args[1]), Integer
                    .parseInt(args[2]), 0, Integer.parseInt(args[3]));
            break;
        case TWO_WRITERS_2_CLUSTERS:
            test = new TwoWritersToManyOn2Clusters(Long.parseLong(args[1]),
                    Integer.parseInt(args[2]), 0, Integer.parseInt(args[3]));
            break;
        default:
            System.out.println("No test selected");
            System.exit(0);
        }

        test.run();
        System.out.println("Test succeeded!");
        System.exit(0); // let shutdown hook terminate ibis
    }
}
