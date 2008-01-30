package ibis.repmi.test;

public class Test {
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        OneWriteToMany test = new OneWriteToMany(Long.parseLong(args[0]),
                Integer.parseInt(args[1]), 0, Integer.parseInt(args[2]),
                args[3]);
        test.run();
        System.out.println("Test succeeded!");
        System.exit(0); // let shutdown hook terminate ibis
    }
}
