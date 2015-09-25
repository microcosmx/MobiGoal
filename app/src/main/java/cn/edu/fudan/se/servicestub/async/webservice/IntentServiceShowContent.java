/**
 * 
 */
package cn.edu.fudan.se.servicestub.async.webservice;

import android.app.IntentService;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Date;

import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.userMes.UserShowContentTask;
import cn.edu.fudan.se.servicestub.sync.webservice.ClientAuthorization;
import cn.edu.fudan.se.sgm.goalmodel.RequestData;

/**
 * 创建一个<code>UserShowContentTask</code>，展示某些内容
 * 
 * @author whh
 * 
 */
public class IntentServiceShowContent extends IntentService {

	private String goalModelName, elementName;

	public IntentServiceShowContent() {
		super("IntentServiceShowContent");
	}

	public IntentServiceShowContent(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		RequestData needRequestData = (RequestData) intent
				.getSerializableExtra("NEED_REQUEST_DATA_CONTENT");

		RequestData retRequestData = (RequestData) intent
				.getSerializableExtra("RET_REQUEST_DATA_CONTENT");

		goalModelName = intent.getExtras().getString("GOAL_MODEL_NAME");
		elementName = intent.getExtras().getString("ELEMENT_NAME");

		createUserShowContentTask(needRequestData, retRequestData);
	}

	@Override
	public void onDestroy() {
		System.out.println("IntentServiceShowContent onDestroy");
		super.onDestroy();
	}

	/**
	 * 创建一个<code>UserShowContentTask</code>，展示某些内容
	 * 
	 * @param requestData
	 *            要展示的内容
	 */
	private void createUserShowContentTask(RequestData requestData,
			RequestData retRequestData) {

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String delegateOutTaskTime = df.format(new Date());
		UserShowContentTask userShowContentTask = new UserShowContentTask(
				delegateOutTaskTime, ClientAuthorization.agentNickName,
				goalModelName, elementName);
		userShowContentTask.setRequestDataName(retRequestData.getName());
		// UserShowContentTask userShowContentTask = new UserShowContentTask(
		// delegateOutTaskTime, goalModelName, elementName, false);
		// 有需要展示的数据
		if (requestData != null) {
			userShowContentTask.setRequestData(requestData);
		}
		String description = "You have received ";

		if (requestData.getContentType().equals("Text")) {
			description += "a span of text.";
		} else if (requestData.getContentType().equals("Image")) {
			description += "an image.";
		} else if (requestData.getContentType().equals("List")) {
			description += "a list of " + requestData.getName()
					+ ". Please select one.";
		}

		userShowContentTask.setDescription(description);

		((SGMApplication) getApplication()).getUserCurrentTaskList().add(0,
				userShowContentTask);

		// 发送 弹窗广播，在MainActivity会监听这个广播然后弹出通知窗口
		Intent broadcast_ndt = new Intent();
		broadcast_ndt.setAction("jade.task.NOTIFICATION");
		broadcast_ndt.putExtra("Content", description);
		getApplicationContext().sendBroadcast(broadcast_ndt);
	}

}
