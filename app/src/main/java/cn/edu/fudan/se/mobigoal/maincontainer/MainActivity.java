package cn.edu.fudan.se.mobigoal.maincontainer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import cn.edu.fudan.se.R;
import cn.edu.fudan.se.agent.main.AideAgentInterface;
import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.maincontainer.fragments.MainFragment;
import cn.edu.fudan.se.mobigoal.utils.Constant;
import cn.edu.fudan.se.mobigoal.utils.NotificationUtil;
import jade.core.MicroRuntime;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

/**
 * 主体activity，加载了一个fragment
 * 
 * @author whh
 * 
 */
public class MainActivity extends FragmentActivity {

	private MyReceiver myReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			// 休眠5s是为了让agent能够启动起来，不然在MessageFragment里得不到agent的引用

			try {
				MicroRuntime.getAgent(
						((SGMApplication) getApplication()).getAgentNickname())
						.getO2AInterface(AideAgentInterface.class);
			} catch (StaleProxyException e) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} catch (ControllerException e) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

			int initialIndex = 0;
			Intent intent = getIntent();
			if (intent.getExtras() != null) {
				initialIndex = (int) intent.getExtras().get("INITIALINDEX");
			}

			FragmentTransaction transaction = getSupportFragmentManager()
					.beginTransaction();
            MainFragment mainFragment = new MainFragment();
            Bundle initialBundle = new Bundle();
            initialBundle.putInt("InitialIndex",initialIndex);
            mainFragment.setArguments(initialBundle);

			transaction.add(R.id.container, mainFragment)
					.commit();
		}

		// 处理agent弹窗相关
		myReceiver = new MyReceiver();
		IntentFilter refreshChatFilter = new IntentFilter();
		refreshChatFilter.addAction("jade.task.NOTIFICATION");
		refreshChatFilter.addAction("jade.mes.NOTIFICATION");
		registerReceiver(myReceiver, refreshChatFilter);

		// 初始化ContextManager中的applicationContext，以便在UI条件检查时弹出对话框
		// ContextManager.applicationContext = this;

	}
//
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
//	}

	@Override
	public void onBackPressed() {
		// 点击返回键后不会退出程序，也就是再次进来的时候还是原来的运行状态
		this.moveTaskToBack(true);
		return;
	}

	/**
	 * 用来监听agent发来的弹窗UI的消息
	 * 
	 * @author whh
	 * 
	 */
	private class MyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			NotificationUtil mNotificationUtil = new NotificationUtil(context);
			String action = intent.getAction();
			if (action.equalsIgnoreCase("jade.task.NOTIFICATION")) {
				mNotificationUtil.showNotification("New Task", intent
						.getExtras().getString("Content"),
						"New Task From SGM!", Constant.Notification_New_Task);
			}
			if (action.equalsIgnoreCase("jade.mes.NOTIFICATION")) {
				mNotificationUtil.showNotification("New Mes", intent
						.getExtras().getString("Content"), "New Mes From SGM!",
						Constant.Notification_New_Mes);
			}
		}
	}

}
