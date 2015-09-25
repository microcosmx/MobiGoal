/**
 * 
 */
package cn.edu.fudan.se.mobigoal.support;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.text.SimpleDateFormat;
import java.util.Date;

import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.sgm.goalmachine.message.MesBody_Mes2Manager;
import cn.edu.fudan.se.sgm.goalmachine.message.MesHeader_Mes2Manger;
import cn.edu.fudan.se.sgm.goalmachine.message.SGMMessage;

/**
 * 监听各种系统广播的服务，在应用启动时启动服务
 * 
 * @author whh
 * 
 */
public class SystemBroadcastListenerService extends Service {

	private BroadcastReceiver mReceiver;
	private PhoneReceiver phoneReceiver;

	public SystemBroadcastListenerService() {
		mReceiver = new My_BroadcastReceiver();
		phoneReceiver = new PhoneReceiver();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		android.util.Log.i("MY_LOG",
				"-------SystemBroadcastListenerService onCreate()-------");
		IntentFilter mFilter = new IntentFilter();
		// mFilter.addAction("android.provider.Telephony.SMS_RECEIVED");// 新短信
		mFilter.addAction(Intent.ACTION_HEADSET_PLUG);// 耳机的插入和拔出
		mFilter.addAction(Intent.ACTION_TIME_TICK); // 时间流逝
		registerReceiver(mReceiver, mFilter);

		// 电话
		IntentFilter phoneFilter = new IntentFilter();
		phoneFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
		phoneFilter.addAction("android.intent.action.PHONE_STATE");
		registerReceiver(phoneReceiver, phoneFilter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
		unregisterReceiver(phoneReceiver);
	}

	private class My_BroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			SGMMessage msgToMessage = null;
			switch (action) {
			case Intent.ACTION_SCREEN_OFF: // 屏幕被关闭
				android.util.Log
						.i("MY_LOG",
								"-------SystemBroadcastListenerService screen off!!!-------");
				msgToMessage = new SGMMessage(
						MesHeader_Mes2Manger.LOCAL_AGENT_MESSAGE, null, null,
						null, new MesBody_Mes2Manager("NewSMS"));
				break;

			case Intent.ACTION_HEADSET_PLUG: // 耳机的插入和拔出
				if (intent.hasExtra("state")) {
					if (intent.getIntExtra("state", 0) == 0) {// 0代表拔出，1代表插入
						android.util.Log
								.i("MY_LOG",
										"-------SystemBroadcastListenerService headset not connected!!!-------");
					} else if (intent.getIntExtra("state", 0) == 1) {
						android.util.Log
								.i("MY_LOG",
										"-------SystemBroadcastListenerService headset connected!!!-------");
						msgToMessage = new SGMMessage(
								MesHeader_Mes2Manger.LOCAL_AGENT_MESSAGE, null,
								null, null, new MesBody_Mes2Manager("NewSMS"));

					}
				}
				break;

			case Intent.ACTION_TIME_TICK: // 时间流逝，1分钟触发一次
				SimpleDateFormat formatter = new SimpleDateFormat("hh:mm");
				String nowTime = formatter.format(new Date(System
						.currentTimeMillis()));
				msgToMessage = new SGMMessage(
						MesHeader_Mes2Manger.LOCAL_AGENT_MESSAGE, null, null,
						null, new MesBody_Mes2Manager("Time" + nowTime));

				android.util.Log.i("MY_LOG",
						"-------ACTION_TIME_TICK time!!!! nowTime: " + nowTime
								+ " -------");

				break;

			default:
				break;
			}

			if (msgToMessage != null) {
				GetAgent.getAideAgentInterface(
						(SGMApplication) getApplication()).sendMesToManager(
						msgToMessage);
			}

		}
	}

	private class PhoneReceiver extends BroadcastReceiver {
		

		@Override
		public void onReceive(Context context, Intent intent) {

			android.util.Log.i("MY_LOG",
					"-------SystemBroadcastListenerService PhoneReceiver intent action:"
							+ intent.getAction());

			if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
				// 如果是去电（拨出）
				android.util.Log
						.i("MY_LOG",
								"-------SystemBroadcastListenerService PhoneReceiver out phone");

			} else {
				// 查了下android文档，貌似没有专门用于接收来电的action,所以，非去电即来电
				android.util.Log
						.i("MY_LOG",
								"-------SystemBroadcastListenerService PhoneReceiver in phone");
				TelephonyManager tm = (TelephonyManager) context
						.getSystemService(Service.TELEPHONY_SERVICE);
				// 设置一个监听器
				tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
			}
		}

		PhoneStateListener listener = new PhoneStateListener() {

			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				// TODO Auto-generated method stub
				// state 当前状态 incomingNumber,貌似没有去电的API
				super.onCallStateChanged(state, incomingNumber);
				switch (state) {
				case TelephonyManager.CALL_STATE_IDLE:
					android.util.Log
							.i("MY_LOG",
									"-------SystemBroadcastListenerService PhoneReceiver phone idle");
					break;
				case TelephonyManager.CALL_STATE_OFFHOOK:
					android.util.Log
							.i("MY_LOG",
									"-------SystemBroadcastListenerService PhoneReceiver phone offhook");
					break;
				case TelephonyManager.CALL_STATE_RINGING:
					// 输出来电号码
					android.util.Log
							.i("MY_LOG",
									"-------SystemBroadcastListenerService PhoneReceiver phone ringing, incomingNumber:"
											+ incomingNumber);
					//发送消息
					SGMMessage msgToMessage = new SGMMessage(
							MesHeader_Mes2Manger.LOCAL_AGENT_MESSAGE, null, null,
							null, new MesBody_Mes2Manager("Phone"+incomingNumber));
					GetAgent.getAideAgentInterface(
							(SGMApplication) getApplication()).sendMesToManager(
							msgToMessage);
					break;
				}
			}
		};

	}
}
