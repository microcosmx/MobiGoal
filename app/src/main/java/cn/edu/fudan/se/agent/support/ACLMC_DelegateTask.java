/**
 * 
 */
package cn.edu.fudan.se.agent.support;

import java.io.Serializable;

import cn.edu.fudan.se.sgm.goalmodel.RequestData;


/**
 * ACLMessage content delegateTask，在agent之间发送委托任务的时候，作为ACMMessage content的部分
 * 
 * @author whh
 * 
 */
public class ACLMC_DelegateTask implements Serializable {

	private static final long serialVersionUID = 3996209050583315117L;

	private DTHeader dtHeader;
	
	private String fromAgentName;
	private String toAgentName;
	private String goalModelName;
	private String elementName;
	
	private String taskDescription;
	private boolean isDone;
	private RequestData retRequestData;
	private RequestData needRequestData;

	public ACLMC_DelegateTask(DTHeader dtHeader, String fromAgentName,
			String toAgentName, String goalModelName, String elementName) {
		this.dtHeader = dtHeader;
		this.fromAgentName = fromAgentName;
		this.toAgentName = toAgentName;
		this.goalModelName = goalModelName;
		this.elementName = elementName;
	}

	public boolean isDone() {
		return isDone;
	}

	public void setDone(boolean isDone) {
		this.isDone = isDone;
	}

	public RequestData getRetRequestData() {
		return retRequestData;
	}

	public void setRetRequestData(RequestData retRequestData) {
		this.retRequestData = retRequestData;
	}

	public DTHeader getDtHeader() {
		return dtHeader;
	}

	public void setFromAgentName(String fromAgentName){
		this.fromAgentName = fromAgentName;
	}
	
	public String getFromAgentName() {
		return fromAgentName;
	}

	public String getToAgentName() {
		return toAgentName;
	}

	public String getGoalModelName() {
		return goalModelName;
	}

	public String getElementName() {
		return elementName;
	}

	public String getTaskDescription() {
		return taskDescription;
	}

	public void setTaskDescription(String taskDescription) {
		this.taskDescription = taskDescription;
	}

	public RequestData getNeedRequestData() {
		return needRequestData;
	}

	public void setNeedRequestData(RequestData needRequestData) {
		this.needRequestData = needRequestData;
	}

	public static enum DTHeader {
		NEWDT, DTBACK;
	}

}


