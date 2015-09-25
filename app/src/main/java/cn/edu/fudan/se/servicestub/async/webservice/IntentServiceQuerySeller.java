/**
 * 
 */
package cn.edu.fudan.se.servicestub.async.webservice;

import android.app.IntentService;
import android.content.Intent;

import java.util.ArrayList;
import java.util.HashMap;

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
public class IntentServiceQuerySeller extends IntentService {

	private String bookName = "";

	private String sellerInfos = "";

	private String goalModelName, elementName;

	/**
	 * 必须有一个空的构造函数，不然会报错
	 */
	public IntentServiceQuerySeller() {
		super("IntentServiceQuerySeller");
	}

	/**
	 * @param name
	 */
	public IntentServiceQuerySeller(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		RequestData requestData = (RequestData) intent
				.getSerializableExtra("NEED_REQUEST_DATA_CONTENT");

		bookName = EncodeDecodeRequestData.decodeToText(requestData
                .getContent());

		goalModelName = intent.getExtras().getString("GOAL_MODEL_NAME");
		elementName = intent.getExtras().getString("ELEMENT_NAME");

		if (querySellerName(bookName)) {

			// 服务执行成功，也就是图书馆有这本书
			SGMMessage msg = new SGMMessage(
					MesHeader_Mes2Manger.LOCAL_AGENT_MESSAGE, goalModelName,
					null, elementName, new MesBody_Mes2Manager("ServiceExecutingDone"));

			RequestData retRequestData = new RequestData("seller infos", "List");
			retRequestData.setContent(sellerInfos.getBytes());
			msg.setRetContent(retRequestData);

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
		System.out.println("IntentServiceQuerySeller onDestroy");
		super.onDestroy();
	}

	private boolean querySellerName(String bookname) {

		HashMap<String, HashMap<String,String>> seller = new HashMap<>();

		HashMap<String,String> bookList1 = new HashMap<String, String>();
		bookList1.put("Java Web Service","40");
		bookList1.put("OSGi in Action","20");
		bookList1.put("Java Script","30");

		HashMap<String,String> bookList2 = new HashMap<String, String>();
		bookList2.put("Java Web Service","20");
		bookList2.put("OSGi in Action","40");
		bookList2.put("Data Mining","30");

		seller.put("YuHan", bookList1);
		seller.put("ChaiNing", bookList2);

		HashMap<String, String> addrs = new HashMap<>();
		addrs.put("YuHan", "TeachingBuilding");
		addrs.put("ChaiNing", "MEBuilding");

		boolean ret = false;

		ArrayList<String> sellerInfoList = new ArrayList<>();

		for (String selleName : seller.keySet()) {
			if (seller.get(selleName).keySet().contains(bookname)) {
				String listItem = "Seller:" + selleName + ";Price:";
				String price = seller.get(selleName).get(bookname);
				listItem += price + ";Addr:" + addrs.get(selleName) +";Book:"+bookname;
				sellerInfoList.add(listItem);
			}
		}

		if (sellerInfoList.isEmpty()) {
			ret = false;
		} else {
			ret = true;
			for (String item : sellerInfoList) {
				sellerInfos += item + "###";
			}
		}

		return ret;
	}

}
