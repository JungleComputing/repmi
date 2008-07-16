package ibis.repmi.test.acp;

import ibis.repmi.test.VoidTest;

class ACP extends VoidTest {

	public final int MAX_RELATIONS = 50;

	int numVariables;

	int numValues;

	int numConnections;

	int numRelations;

	int numRelationPairs;

	int seed;

	ReplicatedMatrix matrix;

	Relation[] relations;

	Constraints constraints;

	int removed;

	long checks;

	int revise = 0;

	int change_ops = 0;

	int nr_modif = 0;

	int[] list_change;

	int poz_change;

	public ACP(int numVariables, int numValues, int numConnections,
			int numRelations, int numRelationPairs, int seed, int cpu,
			int ncpus, ReplicatedMatrix matrix) {

		super(cpu, ncpus, matrix);
		this.numVariables = numVariables;
		this.numValues = numValues;
		this.numConnections = numConnections;
		this.numRelations = numRelations;
		this.numRelationPairs = numRelationPairs;
		this.seed = seed;
		this.matrix = matrix;

		poz_change = removed = 0;
		checks = 0;

		list_change = new int[numValues];

		// GenerateRelations.
		relations = new Relation[numRelations];

		OrcaRandom random = new OrcaRandom(seed);

		for (int h = 0; h < numRelations; h++) {
			relations[h] = new Relation(numValues, h, random, numRelationPairs,
					numValues);
		}

		// GenerateConstraints
		constraints = new Constraints(numVariables, numConnections,
				numRelations, relations, new OrcaRandom(seed));
	}

	boolean sprevise(int i, int j, boolean[][] local, Relation rel)
			throws Exception {

		/*
		 * Test if there is a relation between var_i and var_j :
		 * 
		 * for every legal value of var_i : for every legal value of var_j :
		 * test if var_j supports var_i
		 * 
		 * if var_i is not supported remove it something is modified else there
		 * is a solution
		 * 
		 * if !solution found panic else return if something was modified
		 * 
		 */

		boolean modified, solution, support;

		revise++;

		modified = solution = false;

		for (int k = 0; k < numValues; k++) {

			if (local[i][k]) {

				support = false;

				for (int l = 0; l < numValues; l++) {

					if (local[j][l]) {

						checks++;

						if (i < j) {
							if (rel.test(k, l)) {
								support = true;
								break;
							}
						} else {
							if (rel.test(l, k)) {
								support = true;
								break;
							}
						}
					}
				}

				if (!support) {
					removed++;
					local[i][k] = false;
					list_change[poz_change++] = k;
					modified = true;
				} else {
					solution = true;
				}

			}

		}

		if (!solution) {
			System.out.println("No solution for " + i + " " + j);
			throw new NoSolutionException();
		}

		/*
		 * if (modified) { System.out.println("Removed " + removed);
		 * 
		 * for ( i = 0;i<numVariables;i++) { for ( j= 0;j<numValues;j++) {
		 * System.out.print((local[i][j] ? "1" : "0")); } System.out.println(); } }
		 */

		return modified;
	}

	void spac_loop() throws Exception {

		boolean local_change, i_change, temp;
		boolean[] work_list;
		boolean[][] local;

		while (work.workFor(cpu)) {

			local_change = true;

			while (local_change) {

				local_change = false;
				work_list = work.getWork(cpu);
				local = matrix.getValue();

				for (int i = 0; i < numVariables; i++) {

					if (work_list[i]) {

						work.vote(i, false); // HACK : i might have become
						// true again....
						i_change = false;
						poz_change = 0;

						for (int j = 0; j < numVariables; j++) {
							if (i != j) {
								if (constraints.relation(i, j)) {
									temp = sprevise(i, j, local, constraints
											.get(i, j));
									i_change = i_change || temp;
									// be careful with lazy evaluation here !
								}
							}
						}

						if (i_change) {
							local_change = true;
							matrix.change(i, list_change, poz_change);
							// System.out.println(cpu + " change " +
							// poz_change);
							change_ops++;
							work.announce(i);
						}
					}
				}
			}

			work.ready(cpu);
			nr_modif++;
		}

		// System.out.println(cpu + " Revise : " + revise);
		// System.out.println(cpu + " Checks : " + checks);
		// System.out.println(cpu + " Modif : " + nr_modif);

	}

	void start() throws Exception {

		long start, end;

		super.run();

		if (super.pLWA == 0) {
			System.out.println("" + this);
		}

		// RuntimeSystem.barrier();

		start = System.currentTimeMillis();

		try {
			spac_loop();
		} catch (NoSolutionException e) {
			System.out.println("No solution found!");
		}

		end = System.currentTimeMillis();

		data.put(cpu, removed, checks, (end - start), nr_modif, change_ops,
				revise);

		if (super.pLWA == 0) {
			System.out.println(data.result());
		}

		System.exit(0);
	}

	public String toString() {

		String temp = "ACP\n";

		temp += "Number of variables            " + numVariables + "\n";
		temp += "Number of values               " + numValues + "\n";
		temp += "Number of connections/variable " + numConnections + "\n";
		temp += "Number of relations            " + numRelations + "\n";
		temp += "Number of relation pairs       " + numRelationPairs + "\n";

		return temp;
	}

	public static void main(String args[]) {

		try {
			int numVariables;
			int numValues;
			int numConnections;
			int numRelations;
			int numRelationPairs;
			int seed;
			int ncpus;

			Input in;

			if (args.length != 2) {
				System.out.println("Usage : ACP <inputfile> <ncpus>");
				System.exit(1);
			}

			in = new Input(args[0]);

			numVariables = in.readInt();
			in.readln();

			numValues = in.readInt();
			in.readln();

			numConnections = in.readInt();
			in.readln();

			numRelations = in.readInt();
			in.readln();

			numRelationPairs = in.readInt();
			in.readln();

			seed = in.readInt();

			ncpus = Integer.parseInt(args[1]);

			ReplicatedMatrix matrix = new ReplicatedMatrix(numVariables,
					numValues, true);

			new ACP(numVariables, numValues, numConnections, numRelations,
					numRelationPairs, seed, ncpus, matrix).start();

		} catch (Exception e) {
			System.out.println("OOPS: main got exception " + e);
			e.printStackTrace();
		}

	}
}
