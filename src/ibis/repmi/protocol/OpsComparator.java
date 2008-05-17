package ibis.repmi.protocol;

import java.io.Serializable;
import java.util.Comparator;

/*if arg0 has a lower TS than arg1 then arg0 is smaller than arg1*/
public class OpsComparator implements Comparator, Serializable {

    public int compare(Object arg0, Object arg1) {

        Operation o0 = (Operation) arg0;
        Operation o1 = (Operation) arg1;
        
/* no longer needed since each queue has the same TS
        if (o0.getTS().compareTo(o1.getTS()) < 0)
            return -1;
        if (o0.getTS().compareTo(o1.getTS()) > 0)
            return 1;
  */
        return o0.getPid().compareTo(o1.getPid());
    }

}
