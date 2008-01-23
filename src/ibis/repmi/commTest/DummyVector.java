package ibis.repmi.commTest;
import java.io.Serializable;
import java.util.HashMap;


public class DummyVector implements Serializable {
	
	private HashMap ltm;
	private boolean isInit; 
	
	public DummyVector() {
		
		ltm = new HashMap();
		isInit = false;
	}

}
