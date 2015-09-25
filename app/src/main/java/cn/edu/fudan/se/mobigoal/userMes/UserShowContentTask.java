/**
 * 
 */
package cn.edu.fudan.se.mobigoal.userMes;


import cn.edu.fudan.se.sgm.goalmodel.RequestData;

/**
 * 用户收到这个任务后，点击上面的show按钮，会显示一个对话框，这个对话框里显示适当的内容；点击quit按钮表示失败
 * 
 * @author whh
 * 
 */
public class UserShowContentTask extends UserTask {
	
	private RequestData requestData;

	public UserShowContentTask(String time, String fromAgentName,
			String goalModelName, String elementName) {
		super(time, fromAgentName, goalModelName, elementName);
	}

	public RequestData getRequestData() {
		return requestData;
	}

	public void setRequestData(RequestData requestData) {
		this.requestData = requestData;
	}

}
