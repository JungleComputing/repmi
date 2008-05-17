package ibis.repmi.protocol;

public class RoundTimedOutException extends Exception {
	
	public long startTime;

	public RoundTimedOutException(long start) {
		// TODO Auto-generated constructor stub
		startTime = start;
	}

}
