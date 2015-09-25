package cn.edu.fudan.se.servicestub.async.webservice;

import android.app.IntentService;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Date;

import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.userMes.UserTakePictureTask;
import cn.edu.fudan.se.mobigoal.userMes.UserTask;
import cn.edu.fudan.se.servicestub.sync.webservice.ClientAuthorization;
import cn.edu.fudan.se.sgm.goalmodel.RequestData;

public class IntentServiceTakePicture extends IntentService {

	private String goalModelName, elementName;

	public IntentServiceTakePicture() {
		super("IntentServiceTakePicture");
	}

	public IntentServiceTakePicture(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		RequestData retRequestData = (RequestData) intent
				.getSerializableExtra("RET_REQUEST_DATA_CONTENT");

		RequestData needRequestData = (RequestData) intent
				.getSerializableExtra("NEED_REQUEST_DATA_CONTENT");

		goalModelName = intent.getExtras().getString("GOAL_MODEL_NAME");
		elementName = intent.getExtras().getString("ELEMENT_NAME");
		
		//创建让用户拍照的task

		String userTaskTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
				.format(new Date());
		
		UserTask userTask = new UserTakePictureTask(userTaskTime, ClientAuthorization.agentNickName,
				goalModelName, elementName);
		String userTaskDescription = "Please take a picture about:\n"
				+ retRequestData.getName();
		
		userTask.setRequestDataName(retRequestData.getName());
		userTask.setDescription(userTaskDescription);
		((SGMApplication) getApplication()).getUserCurrentTaskList().add(0, userTask);

		// 新任务广播
		Intent broadcast_nda = new Intent();
		broadcast_nda.setAction("jade.task.NOTIFICATION");
		broadcast_nda.putExtra("Content", userTask.getDescription());
		getApplicationContext().sendBroadcast(broadcast_nda);

	}

	@Override
	public void onDestroy() {
		System.out.println("IntentServiceTakePicture onDestroy");
		super.onDestroy();
	}


}
