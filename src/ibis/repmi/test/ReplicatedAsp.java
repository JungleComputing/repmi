package ibis.repmi.test;

import java.io.Serializable;

public class ReplicatedAsp implements ReplicatedAspInterface, Serializable {

	private int table[][];
	private int row = -1;
	private int round = -1;
	private int dim;	
	
	public ReplicatedAsp(int n, int maxDist) {
		
		dim = n;
		table = new int[n][n];		
		for(int i=0; i<n; i++) {			
			for(int j=0; j<n; j++) {
				table[i][j] = (i == j ? 0 :
                    1+(int)((double)maxDist*Math.random()));
			}
		}
		
	}
	
	public synchronized Integer myRow() {
		// TODO Auto-generated method stub
		row ++;		
		return new Integer(row);
	}

	public synchronized void sendMyRow(Integer myRow, int[] theRow) {
		// TODO Auto-generated method stub
		round = myRow.intValue();
		table[round] = theRow;
	}
	
	public synchronized Integer getRound() {
		
		return new Integer(round);
	}
	
	public synchronized int[] getMyRow(Integer myRow) {
		
		return table[myRow.intValue()];
	}
	
	public synchronized int[] recomputeMyRow(Integer myRow) {
	
		int tmp;
		int i = myRow.intValue();
		for(int j=0; j<dim; j++) {
			tmp = table[i][round] + table[round][j];
			if(tmp < table[i][j]) {
				table[i][j] = tmp;
			}				
		}
		return table[i];
	}

	public synchronized void printTable() {
		// TODO Auto-generated method stub
		for(int i=0; i<dim; i++) {
			for(int j=0; j<dim; j++) {
				System.out.print(table[i][j] + " ");
			}
			System.out.println();
		}
	}
	
}
