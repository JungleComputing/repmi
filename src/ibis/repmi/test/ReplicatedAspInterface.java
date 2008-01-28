package ibis.repmi.test;

import ibis.repmi.protocol.Replicateable;

public interface ReplicatedAspInterface extends Replicateable {

	public Integer myRow();
	
	public void sendMyRow(Integer myRow, int[] theRow);	
	 
}