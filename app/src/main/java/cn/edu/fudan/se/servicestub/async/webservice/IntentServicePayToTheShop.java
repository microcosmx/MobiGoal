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
public class IntentServicePayToTheShop extends IntentService {

	private String bookPrice = "";

	private String goalModelName, elementName;

	/**
	 * 必须有一个空的构造函数，不然会报错
	 */
	public IntentServicePayToTheShop() {
		super("IntentServicePayToTheShop");
	}

	/**
	 * @param name
	 */
	public IntentServicePayToTheShop(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		RequestData requestData = (RequestData) intent
				.getSerializableExtra("NEED_REQUEST_DATA_CONTENT");
		
		System.out
		.println("IntentServicePayToTheShop, requestData content is null?:"
				+ (requestData.getContent() == null) + ", type:"
				+ requestData.getContentType());


		bookPrice = EncodeDecodeRequestData.decodeToText(requestData
                .getContent());

		goalModelName = intent.getExtras().getString("GOAL_MODEL_NAME");
		elementName = intent.getExtras().getString("ELEMENT_NAME");

		if (payToTheShop(bookPrice)) {

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
		System.out.println("IntentServicePayToTheShop onDestroy");
		super.onDestroy();
	}

	private boolean payToTheShop(String bookPrice) {

		int random = (int) (Math.random() * 10); // [0,10)
		if (random > 1) { // 80%的概率
			// 支付成功
			System.out.println("IntentServicePayToTheShop --------- pay succeed!");
			return true;
		} else {
			// 支付失败
			System.out.println("IntentServicePayToTheShop --------- pay failed!");
			return false;
		}
	}

}
