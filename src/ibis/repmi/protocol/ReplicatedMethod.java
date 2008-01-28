package ibis.repmi.protocol;

import java.io.Serializable;


public class ReplicatedMethod implements Serializable {

	String name;
	Class[] paramTypes;
	Object[] args;
	
	public ReplicatedMethod(String name, Class[] paramTypes, Object[] arguments) {
		
		this.name = name;
		this.paramTypes = paramTypes;
		args = arguments;
	}
	
	String getName() {
		
		return name;
	}

	Class[] getParamTypes() {
		
		return paramTypes;
	}
	
	Object[] getArgs() {
		
		return args;
	}
}
