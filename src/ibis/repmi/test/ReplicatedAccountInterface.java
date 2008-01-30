package ibis.repmi.test;

import ibis.repmi.protocol.Replicateable;

public interface ReplicatedAccountInterface extends Replicateable {

    public void writeMultiplication(Integer times);

    public void writeAddition(Integer quantity);
}
