package ibis.repmi.protocol;

import java.io.Serializable;

import ibis.ipl.IbisIdentifier;

public class ProcessIdentifier implements Comparable, Serializable {

    //private IbisIdentifier ibisId;

    private String ibisId;
    private String cluster;    
    
    public ProcessIdentifier(IbisIdentifier ii) {

        ibisId = ii.name();
        cluster = ii.location().getLevel(0);
    }

    public int compareTo(Object o) {

        ProcessIdentifier other = (ProcessIdentifier) o;
        return this.getUniqueId().compareTo(other.getUniqueId());
    }

    public String toString() {
        
        return ibisId;
    }

    public String getUniqueId() {

        return ibisId;
    }

    public String getCluster() {

        return cluster;
    }

    public int hashCode() {

        return getUniqueId().hashCode();
    }

    public boolean equals(Object o) {

        if (o instanceof ProcessIdentifier) {
            return getUniqueId().equals(((ProcessIdentifier) o).getUniqueId());
        } else {
            return false;
        }
    }
}
