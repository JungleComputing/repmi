package joinc;

/**
  * TaskStats creates an object that contains a Task and several variables
  * that are useful in tracking the performance of the Task and whether it
  * needs to be restarted.
  */

public class TaskStats {

    public Task task;
    public int id;
    public boolean restart;
    public int restarts;
    public long joincTime;
    public long initTime;
    public long queueTime;
    public long runTime;

    /**
      * Contructor for TaskStats to initialize all variables
      *
      * @param t the task to be added
      */
    public TaskStats(Task t) {
	task = t;
	id = task.taskNumber;
	restarts = 0;
	restart = false;
	joincTime = 0;
	initTime = 0;
	queueTime = 0;
	runTime = 0;
    }
}
