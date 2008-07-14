package ibis.repmi.test.acp;

import ibis.repmi.protocol.Replicateable;

public interface ReplicatedMatrixInterface extends Replicateable {

    public void change(int x, int[] list_change, int poz_change);
}
