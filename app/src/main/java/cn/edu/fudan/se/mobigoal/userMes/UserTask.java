/**
 * 
 */
package cn.edu.fudan.se.mobigoal.userMes;

import java.io.Serializable;

/**
 * 用户要做的任务
 * 
 * @author whh
 * 
 */
public class UserTask implements Serializable {

	private String time;

	private String fromAgentName;
	private String goalModelName;
	private String elementName;

	private String description;
	private boolean isDone; // 用户是否做过了

	private String requestDataName;

	public UserTask(String time, String fromAgentName, String goalModelName,
			String elementName) {
		this.time = time;
		this.fromAgentName = fromAgentName;
		this.goalModelName = goalModelName;
		this.elementName = elementName;
		this.isDone = false;
	}

	public String getTime() {
		return time;
	}

	public String getFromAgentName() {
		return fromAgentName;
	}

	public boolean isDone() {
		return isDone;
	}

	public void setDone(boolean isDone) {
		this.isDone = isDone;
	}

	public String getGoalModelName() {
		return goalModelName;
	}

	public String getElementName() {
		return elementName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRequestDataName() {
		return requestDataName;
	}

	public void setRequestDataName(String requestDataName) {
		this.requestDataName = requestDataName;
	}

}
