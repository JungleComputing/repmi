package joinc;

import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.ArrayBlockingQueue;

import org.gridlab.gat.*;
import org.gridlab.gat.io.*;
import org.gridlab.gat.resources.*;
import org.gridlab.gat.monitoring.*;

/**
  * TaskExecutor represents a thread of execution for a task.  One TaskExecutor
  * is created for every task that need to be ran.  Task that are restarted are 
  * run through a separate TaskExecutor.
  */
public class TaskExecutor implements Runnable, MetricListener {

    private Task task;
    private TaskStats taskStats;
    private ArrayBlockingQueue<TaskStats> tasksDone;
    private GATContext context;
    private Job job;
    private boolean exit;
    private long joinc_time;
    private long init_time;
    private long queue_time;
    private long run_time;
    private long stop_time;

    /** 
      * Constructor for TaskExecutor
      *
      * @param ts TaskStats to run task from
      * @param td Blocking Array for communicating completed tasks with Master
      * @param c GATContext from Master
      */
    public TaskExecutor(TaskStats ts, ArrayBlockingQueue td, GATContext c) {
	taskStats = ts;
	task = taskStats.task;
	tasksDone = td;
	context = c;
	exit = false;
    }
    /**
      * Method for forming a JobDescription from a Task and GATContext.
      *
      * @param task - tha Task that describes the job.
      * @param context - the GATContext to be used by the JobDescription.
      */
    public JobDescription createJD(Task task, GATContext context) {
	JobDescription job_descr = null;
	try {
		SoftwareDescription soft_descr = new SoftwareDescription();
		soft_descr.setLocation(new URI("any:////usr/local/package/jdk1.5/bin/java"));
		File stdin = GAT.createFile(context, new URI("any:///"+task.stdinFile));
		File stdout = GAT.createFile(context, new URI("any:///"+task.stdoutFile));
		File stderr = GAT.createFile(context, new URI("any:///"+task.stderrFile));
		soft_descr.setStdin(stdin);
		soft_descr.setStdout(stdout);
		soft_descr.setStdout(stderr);
		for(int i = 0;  i < task.inputFiles.length; i++) {
			if(task.inputFiles[i] != null) {
				File input = GAT.createFile(context, new URI("any:///"+task.inputFiles[i]));
				if(input.exists()) soft_descr.addPreStagedFile(input);
				else System.err.println("Error File " + task.inputFiles[i] + " does not exist!");
			}
		}
		for(int i = 0;  i < task.jars.length; i++) {
			if(task.jars[i] != null) {
				File jar = GAT.createFile(context, new URI("any:///"+task.jars[i]));
				if(jar.exists()) soft_descr.addPreStagedFile(jar);
				else System.err.println("Error File " + task.jars[i] + " does not exist!");
			}
		}
		for(int i = 0;  i < task.outputFiles.length; i++) {
			if(task.outputFiles[i] != null) {
				File output = GAT.createFile(context, new URI("any:///"+task.outputFiles[i]));
				soft_descr.addPostStagedFile(output);
				soft_descr.setStdout(output);
			}
		}
	
		String arguments[] = new String[task.jars.length + task.parameters.length + 2];
		arguments[0] = "-cp";
		for(int i = 1; i < (task.jars.length + 1); i++) {
			arguments[i] = task.jars[i - 1];
		}
		arguments[task.jars.length + 1] = task.className;
		for(int i = task.jars.length + 2; i < (task.jars.length + task.parameters.length + 2); i++) {
			arguments[i] = task.parameters[i - task.jars.length - 2];
		}
		soft_descr.setArguments(arguments);
		//setup resource descitiption
		Hashtable ht = new Hashtable();
		ht.put("machine.node", "fs0.das3.cs.vu.nl");
		ResourceDescription resource_descr = new HardwareResourceDescription(ht);

		//set job description
		job_descr = new JobDescription(soft_descr, resource_descr);
	} catch (Exception e) {  //catch all exceptions and set job to be restarted if any recieved
		System.err.println("Failed Creating Job Description: "+e);
		e.printStackTrace();
		taskStats.restart = true;
		exit = true;
	}
	return job_descr;
    }

    /**
      * processMetricEvent handles monitoring state changes of job status and
      * notifying the thread of the exit condition.  It is also responsible for
      * setting the time monitoring metrics. The exit conditions are if Job Stopped,
      * Submission error, or if the job state is unknown for 3 checks at 1s intervals.
      *
      * @param val - the MetricValue to process
      */
    public synchronized void processMetricEvent(MetricValue val) {
	String state = (String)val.getValue();

	if(state.equals("SCHEDULED")) {
		queue_time = val.getEventTime();
	}
	else if(state.equals("RUNNING")) {
		run_time = val.getEventTime();
	}
	else if(state.equals("STOPPED") || state.equals("SUBMISSION_ERROR")) {
		stop_time = val.getEventTime();
		exit = true;
		notifyAll();
	}
	else if(state.equals("UNKNOWN")) {
		for(int i = 0; i < 3 && state.equals("UNKNOWN"); i++) {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				//ignore
			}
			state = job.getStateString(job.getState());
		}
		if(state.equals("UNKNOWN")) {
			stop_time = val.getEventTime();
			exit = true;
			notifyAll();
		}
	}
    }
 
    /**
      * run is the method that is called when a thread executes the class.
      */
    public void run() { 
	joinc_time = (new Date()).getTime();  //set start time of thread
	try {
		//setup software description
		JobDescription job_descr = createJD(task, context);

		Preferences prefs = new Preferences();
		prefs.put("ResourceBroker.adaptor.name","globus");

		ResourceBroker broker = GAT.createResourceBroker(context, prefs);

		//Start job
		init_time = (new Date()).getTime();
		job = broker.submitJob(job_descr);

		//Add listener for job status changes
		MetricDefinition md = job.getMetricDefinitionByName("job.status");
		Metric m = md.createMetric();
		job.addMetricListener(this,m);

		synchronized(this) {
			while (!exit) wait();
		}
		
		int state = job.getState();
		if(state == Job.SUBMISSION_ERROR || state == Job.UNKNOWN) {
			taskStats.restart = true;
		}
		if (run_time == 0) run_time = stop_time;  //if job failure make sure run_time has a value
		taskStats.joincTime += (init_time - joinc_time)/1000;
		taskStats.initTime += (queue_time - init_time)/1000;
		taskStats.queueTime += (run_time - queue_time)/1000;
		taskStats.runTime += (stop_time - run_time)/1000;
		tasksDone.put(taskStats);

	} catch (Exception e) {  //catch all exceptions and set job to be restarted if any recieved
		System.err.println("Failed Job Creation: "+e);
		e.printStackTrace();
		taskStats.restart = true;
	}
    }
}
