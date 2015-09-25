/**
 * 
 */
package cn.edu.fudan.se.servicestub.async.webservice;

import android.app.IntentService;
import android.content.Intent;

import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.support.GetAgent;
import cn.edu.fudan.se.sgm.goalmachine.message.MesBody_Mes2Manager;
import cn.edu.fudan.se.sgm.goalmachine.message.MesHeader_Mes2Manger;
import cn.edu.fudan.se.sgm.goalmachine.message.SGMMessage;
import cn.edu.fudan.se.sgm.goalmodel.EncodeDecodeRequestData;
import cn.edu.fudan.se.sgm.goalmodel.RequestData;

/**
 * @author whh
 * 
 */
public class IntentServiceBorrowBookFromLibrary extends IntentService {

	private String bookName = "";

	private String goalModelName, elementName;

	/**
	 * 必须有一个空的构造函数，不然会报错
	 */
	public IntentServiceBorrowBookFromLibrary() {
		super("IntentServiceBorrowBookFromLibrary");
	}

	/**
	 * @param name
	 */
	public IntentServiceBorrowBookFromLibrary(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		RequestData needRequestData = (RequestData) intent
				.getSerializableExtra("NEED_REQUEST_DATA_CONTENT");

		goalModelName = intent.getExtras().getString("GOAL_MODEL_NAME");
		elementName = intent.getExtras().getString("ELEMENT_NAME");

		bookName = EncodeDecodeRequestData.decodeToText(needRequestData
                .getContent());


		if (borrowBookFromLibrary(bookName)) {

			// 服务执行成功，也就是图书馆有这本书
			SGMMessage msg = new SGMMessage(
					MesHeader_Mes2Manger.LOCAL_AGENT_MESSAGE, goalModelName,
					null, elementName, new MesBody_Mes2Manager("ServiceExecutingDone"));

			GetAgent.getAideAgentInterface((SGMApplication) getApplication())
					.handleMesFromService(msg);

		} else {
			// 服务执行失败
			SGMMessage msg = new SGMMessage(
					MesHeader_Mes2Manger.LOCAL_AGENT_MESSAGE, goalModelName,
					null, elementName,
					new MesBody_Mes2Manager("ServiceExecutingFailed"));

			GetAgent.getAideAgentInterface((SGMApplication) getApplication())
					.handleMesFromService(msg);

		}

	}

	@Override
	public void onDestroy() {
		System.out.println("IntentServiceBorrowBookFromLibrary onDestroy");
		super.onDestroy();
	}

	private boolean borrowBookFromLibrary(String bookname) {

		int random = (int) (Math.random() * 10); // [0,10)
		if (random > 100) {	//100%的概率
			// 借书成功
			return true;
		} else {
			// 借书失败
			return false;
		}
	}

}
