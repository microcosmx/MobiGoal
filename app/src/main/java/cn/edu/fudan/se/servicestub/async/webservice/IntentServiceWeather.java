/**
 * 
 */
package cn.edu.fudan.se.servicestub.async.webservice;

import android.app.IntentService;
import android.content.Intent;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpResponseException;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.support.GetAgent;
import cn.edu.fudan.se.sgm.goalmachine.message.MesBody_Mes2Manager;
import cn.edu.fudan.se.sgm.goalmachine.message.MesHeader_Mes2Manger;
import cn.edu.fudan.se.sgm.goalmachine.message.SGMMessage;
import cn.edu.fudan.se.sgm.goalmodel.EncodeDecodeRequestData;
import cn.edu.fudan.se.sgm.goalmodel.RequestData;

/**
 * 调用天气服务的桩，用安卓的<code>IntentService</code>实现
 * 
 * @author whh
 * 
 */
public class IntentServiceWeather extends IntentService {

	private String cityName = "";

	private String weatherInfo = "";
	private String goalModelName, elementName;

	/**
	 * 必须有一个空的构造函数，不然会报错
	 */
	public IntentServiceWeather() {
		super("IntentServiceWeather");
	}

	/**
	 * @param name
	 */
	public IntentServiceWeather(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		RequestData requestData = (RequestData) intent
				.getSerializableExtra("NEED_REQUEST_DATA_CONTENT");
		System.out
				.println("IntentServiceWeather, requestData content is null?:"
						+ (requestData.getContent() == null) + ", type:"
						+ requestData.getContentType());

		cityName = EncodeDecodeRequestData.decodeToText(requestData
                .getContent());
		
		System.out
		.println("IntentServiceWeather, cityName: " + cityName);

		goalModelName = intent.getExtras().getString("GOAL_MODEL_NAME");
		elementName = intent.getExtras().getString("ELEMENT_NAME");
		/*
		 * 这里是ksoap的bug，调用web
		 * service的时候总是会报EOFException，但是短时间内重新连接就不会报错，所以这里catch到exception后立刻重新再获取一遍
		 */
		try {
			weatherInfo = getWeather(cityName);
		} catch (IOException | XmlPullParserException e) {
			System.out.println("IntentServiceWeather catch exection! retry!!");
			try {
				weatherInfo = getWeather(cityName);
			} catch (IOException | XmlPullParserException e1) {
				// 服务执行失败
				SGMMessage msg = new SGMMessage(
						MesHeader_Mes2Manger.LOCAL_AGENT_MESSAGE,
						goalModelName, null, elementName,
						new MesBody_Mes2Manager("ServiceExecutingFailed"));

				GetAgent.getAideAgentInterface(
                        (SGMApplication) getApplication())
						.handleMesFromService(msg);
				return;
			}
		}

//		// 测试时用，弹出一个通知，显示这个web service调用完毕要返回了
//		NotificationUtil notificationUtil = new NotificationUtil(this);
//		notificationUtil.showNotification("Web service Done",
//				"intent service weather done!\nweatherInfo: " + weatherInfo,
//				"Web Service Done", 100);

		// 服务执行成功
		SGMMessage msg = new SGMMessage(
				MesHeader_Mes2Manger.LOCAL_AGENT_MESSAGE,
				goalModelName, null, elementName,
				new MesBody_Mes2Manager("ServiceExecutingDone"));
		RequestData retRequestData = new RequestData("weatherInfo", "Text");
		retRequestData.setContent(weatherInfo.getBytes());
		msg.setRetContent(retRequestData);

		GetAgent.getAideAgentInterface((SGMApplication) getApplication())
				.handleMesFromService(msg);

	}

	@Override
	public void onDestroy() {
		System.out.println("IntentServiceWeather onDestroy");
		super.onDestroy();
	}

	/**
	 * 调用中国气象局提供的查询天气的web service，查询某个城市的天气
	 * 
	 * @param cityName
	 *            要查询天气的城市
	 * @return 这个城市的天气
	 * @throws org.xmlpull.v1.XmlPullParserException
	 * @throws java.io.IOException
	 * @throws HttpResponseException
	 */
	private String getWeather(String cityName) throws HttpResponseException,
			IOException, XmlPullParserException {

		String ret = "";

		// 指定 WebService 的命名空间和调用方法
		final String NAMESPACE = "http://webservice.se.fudan.edu/";
		String SERVICE_URL = "http://10.131.252.246:8080/WeatherService/WeatherPort";

		// 调用的方法，通过城市名称获取天气情况
		final String METHOD_NAME = "getWeatherByName";

		HttpTransportSE ht = new HttpTransportSE(SERVICE_URL, 30 * 1000);
		SoapObject soapObject = new SoapObject(NAMESPACE, METHOD_NAME);
		// 添加参数，name不重要，只要按照方法中的参数顺序添加即可
		soapObject.addProperty("arg0", cityName);

		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
				SoapEnvelope.VER11);
		// 设置与.Net提供的Web Serviceb保持较好的兼容性（true）
		envelope.dotNet = false;
		envelope.bodyOut = soapObject;

		/*
		 * 必须是null！！！！！否则web service服务端收不到传过去的参数！！！为了这个bug奋斗了快一下午了。。。
		 */
		ht.call(null, envelope);

		if (envelope.getResponse() != null) {
			SoapObject result = (SoapObject) envelope.bodyIn;
			System.out.println(result.getPropertyAsString(0)); // 打印出来的是:
			ret = result.getPropertyAsString(0);
		} else {
			ret = "error";
		}
		// 必须断开连接，不然再次调用这个intent service会报错
		ht.getServiceConnection().disconnect();

		return ret;
	}

}
