package ibis.repmi.test;

import java.io.Serializable;


public class ReplicatedAccount implements ReplicatedAccountInterface, Serializable {

	
	long val = 0;
	
	public void writeMultiplication(Integer times) {
		
		val *= times.intValue(); 
	}
	
	public void writeAddition(Integer quantity) {
		
		val += quantity.intValue();
	}
	
	public Long readVal() {
		
		System.out.println("readVal: " + val);
		return new Long(val);
	}
	
}
