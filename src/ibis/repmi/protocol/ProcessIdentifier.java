package ibis.repmi.protocol;

import java.io.Serializable;

import ibis.ipl.IbisIdentifier;

public class ProcessIdentifier implements Comparable, Serializable {

	private IbisIdentifier ibisId;
	
	public ProcessIdentifier(IbisIdentifier ii) {
		
		ibisId = ii;
	}
	
	public int compareTo(Object o) {
		
		ProcessIdentifier other = (ProcessIdentifier)o;
		return this.getUniqueId().compareTo(other.getUniqueId());		 
	}
	
	public String toString() {
		/*might need to call ibisId.name() in the future*/
		return ibisId.toString();
	}
	
	public String getUniqueId() {
		
		return ibisId.name();
	}
	
	public String cluster() {
		
		return ibisId.location().getLevel(0);
	}
	
	public int hashCode() {
		
		return getUniqueId().hashCode();
	}
	
	public boolean equals(Object o) {
		
		if(o instanceof ProcessIdentifier) {			
			return getUniqueId().equals(((ProcessIdentifier) o).getUniqueId());
		} else { 
			return false; 
		}
	}
}
