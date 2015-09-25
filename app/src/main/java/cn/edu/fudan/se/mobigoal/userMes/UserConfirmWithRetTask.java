/**
 * 
 */
package cn.edu.fudan.se.mobigoal.userMes;


import cn.edu.fudan.se.sgm.goalmodel.RequestData;

/**
 * @author whh
 *
 */
public class UserConfirmWithRetTask  extends UserTask {
	
	private RequestData needRequestData;

	public UserConfirmWithRetTask(String time, String fromAgentName,
			String goalModelName, String elementName) {
		super(time, fromAgentName, goalModelName, elementName);
	}

	public RequestData getNeedRequestData() {
		return needRequestData;
	}

	public void setNeedRequestData(RequestData needRequestData) {
		this.needRequestData = needRequestData;
	}


}
