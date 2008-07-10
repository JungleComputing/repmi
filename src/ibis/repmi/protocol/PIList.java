package ibis.repmi.protocol;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class PIList {

	HashMap pis;

	public PIList() {
		
		this.pis = new HashMap();
	}
	
	public synchronized void add(ProcessIdentifier pi) {
		pis.put(pi, null);
	}

	public synchronized void add(ProcessIdentifier pi, Object ob) {
		pis.put(pi, ob);
	}

	public synchronized boolean contains(ProcessIdentifier pi) {
		return pis.containsKey(pi);
	}

	public int size() {
		return pis.size();
	}

	public synchronized OpsQueue toDeleteOpsQ(){
		Set keys = pis.keySet();
		OpsQueue opq = new OpsQueue();
		
		Iterator piIt = keys.iterator();
		while(piIt.hasNext()) {
			ProcessIdentifier pi = (ProcessIdentifier)piIt.next();
			opq.enqueue(new Operation(pi, new Long(-1), Operation.LEAVE));
			piIt.remove();
		}
		return opq;
	}

	public synchronized void toDeleteOpsQ(OpsQueue opq){
		Set keys = pis.keySet();
				
		Iterator piIt = keys.iterator();
		while(piIt.hasNext()) {
			ProcessIdentifier pi = (ProcessIdentifier)piIt.next();
			if(opq.contains(pi) == false) {
				opq.enqueue(new Operation(pi, new Long(-1), Operation.LEAVE));
				piIt.remove();
			}			
		}
	}
	
	public void moveTo(PIList currentD) {
		Set keys = pis.keySet();
		Iterator piIt = keys.iterator();
		while(piIt.hasNext()) {
			ProcessIdentifier pi = (ProcessIdentifier)piIt.next();
			currentD.add(pi);
			piIt.remove();
		}
		
	}

	public void clear() {
		pis.clear();
	}

	public void moveTo(OpsQueue currentQueue) {
		Set keys = pis.keySet();
		Iterator piIt = keys.iterator();
		while(piIt.hasNext()) {
			currentQueue.merge((Object[])pis.get(piIt.next()));			
			piIt.remove();
		}
		
	}
}

