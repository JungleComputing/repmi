package joinc;
 
import org.gridlab.gat.*;
import org.gridlab.gat.io.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Date;

/**
 * This is the main class of the JOINC library.
 * 
 * It mainly consists of abstract methods, which will be implemented by 
 * application. Since these methods are the interface to the outside world, 
 * they may NOT be changed! For an example of how they are used, see the 
 * applications.      
 * 
 * @author Jason Maassen
 * @version 2.0 Feb 13, 2006
 * @since 1.0
 * 
 */
public abstract class Master {
        
    /**
     * Returns a task that can be run on the grid.
     * (This method will be implemented by the application).
     * 
     * @return a task to run on the grid.
     */
    public abstract Task getTask(); 
    
    /**
     * This method will be implemented by the application. 
     * It must be called by your code after a task is done 
     * to notify the application of this fact.
     *
     * @param task The task that has finished.
     */
    public abstract void taskDone(Task task); 
        
    /**
     * Returns the total number of tasks that need to be run.
     * (This method will be implemented by the application).
     *  
     * @return number of tasks that will be produced.
     */
    public abstract int totalTasks();
    
    /**
     * Returns the maximum number of workers that may be used.
     * (This method will be implemented by the application). 
     *      
     * @return the maximum number of workers you may use.
     */
    public abstract int maximumWorkers(); 
        
    /**
     * If there is nothing else to do, you may call this method 
     * to allow the application to do some work.
     * (This method will be implemented by the application).
     */
    public abstract void idle();  

    /*
     * MAX_RESTARTS controls how many times a task can be restarted
     * before the program will stop trying.
     */
    private int MAX_RESTARTS = 10;
   
    /**
     * This is the method you have to implement. By using the
     * abstract methods above you can get information and jobs
     * from the application. Do whatever you need to do here
     * to run these jobs on 'the grid'. Don't forget to notify
     * the application every time a job has finished.
     * 
     * Keep in mind that machines or jobs may fail, so you may
     * need to restart them sometimes. Also, you should not use
     * more workers than what 'maximumWorkers()' returns.
     * 
     * Enjoy! 
     */
    public void start() { 
        // this is your playground        
	// initialization
	long startGAT = (new Date()).getTime(); //Start Master Time
	int max_workers = maximumWorkers();
	int total_tasks = totalTasks();
	int tasks_done = 0;
	ArrayBlockingQueue<TaskStats> tasksCompleted = new ArrayBlockingQueue<TaskStats>(total_tasks);
	TaskStats tasksDone[] = new TaskStats[total_tasks];
	ExecutorService taskPool = Executors.newFixedThreadPool(max_workers);
	ExecutorService taskCheckPool = Executors.newCachedThreadPool();
	GATContext context = new GATContext();
	try {
		for(int i = 0; i < total_tasks; i++) {  //Add all tasks to the the taskpool at once.
			TaskStats taskStats = new TaskStats(getTask());
			taskPool.execute(new TaskExecutor(taskStats, tasksCompleted, context));
		}
			
	} catch (Exception e) {
		System.err.println("Failed: "+e);
		e.printStackTrace();
	}
	while (tasks_done < total_tasks) { //While not all tasks have finished continue checking for completed tasks.
		if(!tasksCompleted.isEmpty()) {
			try {
				TaskStats taskStats = tasksCompleted.take();
				for(int i = 0;  i < taskStats.task.outputFiles.length; i++) {
					File output = GAT.createFile(context, new URI("any:///"+taskStats.task.outputFiles[i]));
					boolean exists = output.exists();
					long length = output.length();
					if(!output.exists() || output.length() == 0) {  //Check for valid output files from task
						System.err.println("Task " + (taskStats.task).taskNumber + ": File Error " + 
							(output.toGATURI()).toString() + " Exists=" + exists + " Length=" + length);
						taskStats.restart = true;
					}
				}
				if(taskStats.restart) { //if a task needs to be restarted add it back to the taskpool
					taskStats.restart = false;
					taskStats.restarts++;
					if(taskStats.restarts < MAX_RESTARTS) { //task is not restarted if it fails more than MAX_RESTARTS
						taskPool.execute( new TaskExecutor(taskStats, tasksCompleted, context));
					}
					else {
						System.err.println("Task : " + taskStats.task.taskNumber + "Exceeding max restarts and won't be restarted again");
						taskDone(taskStats.task);
						tasksDone[taskStats.id] = taskStats;
						tasks_done++;
					}
				}
				else {
					taskDone(taskStats.task);
					tasksDone[taskStats.id] = taskStats;
					tasks_done++;
				}
			} catch (Exception e) {
				System.err.println("Failed: "+e);
				e.printStackTrace();
			}
		}
		else idle();
	}
	taskPool.shutdown();
	GAT.end();
	long endGAT = (new Date()).getTime();  //End Master Time

	//Print out some thread statistics
	long joincTotal = 0, joincMin = -1, joincMax = 0;
	long initTotal = 0, initMin = -1, initMax = 0;
	long queueTotal = 0, queueMin = -1, queueMax = 0;
	long runTotal = 0, runMin = -1, runMax = 0;
	System.out.println("Statistics:");
	System.out.println("ID\tJOINC\tINIT\tQUEUE\tRUN\tRESTARTS");
	for(int i = 0; i < total_tasks; i++) {
		System.out.println(tasksDone[i].id + "\t" + tasksDone[i].joincTime + "\t" +
			tasksDone[i].initTime + "\t" + tasksDone[i].queueTime + "\t" +  tasksDone[i].runTime + "\t" + tasksDone[i].restarts);
		joincTotal += tasksDone[i].joincTime;
		if(tasksDone[i].joincTime < joincMin || joincMin < 0) joincMin = tasksDone[i].joincTime;
		if(tasksDone[i].joincTime > joincMax) joincMax = tasksDone[i].joincTime;
		initTotal += tasksDone[i].initTime;
		if(tasksDone[i].initTime < initMin || initMin < 0) initMin = tasksDone[i].initTime;
		if(tasksDone[i].initTime > initMax) initMax = tasksDone[i].initTime;
		queueTotal += tasksDone[i].queueTime;
		if(tasksDone[i].queueTime < queueMin || queueMin < 0) queueMin = tasksDone[i].queueTime;
		if(tasksDone[i].queueTime > queueMax) queueMax = tasksDone[i].queueTime;
		runTotal += tasksDone[i].runTime;
		if(tasksDone[i].runTime < runMin || runMin < 0) runMin = tasksDone[i].runTime;
		if(tasksDone[i].runTime > runMax) runMax = tasksDone[i].runTime;
	}
	System.out.println("JOINC: Total: " + joincTotal + " Avg: " + (joincTotal/total_tasks) + " Min: " + joincMin + " Max: " + joincMax);
	System.out.println("INIT: Total: " + initTotal + " Avg: " + (initTotal/total_tasks) + " Min: " + initMin + " Max: " + initMax);
	System.out.println("QUEUE: Total: " + queueTotal + " Avg: " + (queueTotal/total_tasks) + " Min: " + queueMin + " Max: " + queueMax);
	System.out.println("RUN: Total: " + runTotal + " Avg: " + (runTotal/total_tasks) + " Min: " + runMin + " Max: " + runMax);
	System.out.println("Master Run Time: " + (endGAT - startGAT)/1000);
    }
}
