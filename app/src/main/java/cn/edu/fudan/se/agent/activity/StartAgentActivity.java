/**
 * 
 */
package cn.edu.fudan.se.agent.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import java.util.logging.Level;

import cn.edu.fudan.se.R;
import cn.edu.fudan.se.agent.main.AideAgent;
import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.maincontainer.MainActivity;
import cn.edu.fudan.se.servicestub.sync.webservice.ClientAuthorization;
import jade.android.AndroidHelper;
import jade.android.MicroRuntimeService;
import jade.android.MicroRuntimeServiceBinder;
import jade.android.RuntimeCallback;
import jade.core.MicroRuntime;
import jade.core.Profile;
import jade.util.Logger;
import jade.util.leap.Properties;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;

/**
 * 此处是郑家欢原来代码中的<code>MainActivity</code>，主要是开始了agent
 * 
 * @author zjh
 * 
 */
public class StartAgentActivity extends Activity {
	private Logger logger = Logger.getJADELogger(this.getClass().getName());

	private MicroRuntimeServiceBinder microRuntimeServiceBinder;
	private ServiceConnection serviceConnection;

	static final int CHAT_REQUEST = 0;

	private String nickname;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_startagent);
		Button button = (Button) findViewById(R.id.button_chat);
		button.setOnClickListener(buttonChatListener);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unbindService(serviceConnection);

		logger.log(Level.INFO, "Destroy activity!");
	}

	private static boolean checkName(String name) {
		if (name == null || name.trim().equals("")) {
			return false;
		}
		// FIXME: should also check that name is composed
		// of letters and digits only
		return true;
	}

	private OnClickListener buttonChatListener = new OnClickListener() {
		public void onClick(View v) {
			final EditText nameField = (EditText) findViewById(R.id.edit_nickname);
			nickname = nameField.getText().toString();
			if (!checkName(nickname)) {
				logger.log(Level.INFO, "Invalid nickname!");
			} else {
				try {
					((SGMApplication) getApplication())
							.setAgentNickname(nickname);
					((SGMApplication) getApplication()).getGoalModelManager()
							.setAgentNickname(nickname);

					ClientAuthorization.agentNickName = nickname;

					Log.i("start agent",
							"agent nickname: "
									+ ((SGMApplication) getApplication())
											.getAgentNickname());
					SharedPreferences settings = getSharedPreferences(
							"jadeChatPrefsFile", 0);
					String host = settings.getString("defaultHost", "");
					String port = settings.getString("defaultPort", "");
					startChat(nickname, host, port, agentStartupCallback);

					Intent intent = new Intent(StartAgentActivity.this,
							MainActivity.class);
					// intent.putExtra("agentname", nickname);
					startActivity(intent);

					// 开启定位服务
					Log.i("MY_LOG", "startLocationService......");
					Intent locationServiceIntent = new Intent(
							"service.appservice.locator");
					startService(locationServiceIntent);

					// 开启监听系统广播的服务
					Log.i("MY_LOG",
							"startSystemBroadcastListenerService......");
					Intent sblistenerIntent = new Intent(
							"service.appservice.sblistener");
					startService(sblistenerIntent);

				} catch (Exception ex) {
					System.out.println(ex.toString()
							+ "i want to konwekrwlkejr");
					logger.log(Level.SEVERE,
							"Unexpected exception creating chat agent!");
				}
			}
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CHAT_REQUEST) {
			if (resultCode == RESULT_CANCELED) {
				RuntimeCallback<Void> rc = new RuntimeCallback<Void>() {
					@Override
					public void onSuccess(Void thisIsNull) {
					}

					@Override
					public void onFailure(Throwable throwable) {
						logger.log(Level.SEVERE, "Failed to stop the "
								+ AideAgent.class.getName() + "...");
						agentStartupCallback.onFailure(throwable);
					}
				};
				logger.log(Level.INFO, "Stopping Jade...");
				microRuntimeServiceBinder.stopAgentContainer(rc);
			}
		}
	}

	private RuntimeCallback<AgentController> agentStartupCallback = new RuntimeCallback<AgentController>() {
		@Override
		public void onSuccess(AgentController agent) {
		}

		@Override
		public void onFailure(Throwable throwable) {
			logger.log(Level.INFO, "Nickname already in use!");
		}
	};

	public void startChat(final String nickname, final String host,
			final String port,
			final RuntimeCallback<AgentController> agentStartupCallback) {

		final Properties profile = new Properties();
		profile.setProperty(Profile.MAIN_HOST, host);
		profile.setProperty(Profile.MAIN_PORT, port);
		profile.setProperty(Profile.MAIN, Boolean.FALSE.toString());
		profile.setProperty(Profile.JVM, Profile.ANDROID);

		if (AndroidHelper.isEmulator()) {
			profile.setProperty(Profile.LOCAL_HOST, AndroidHelper.LOOPBACK);
		} else {
			profile.setProperty(Profile.LOCAL_HOST,
					AndroidHelper.getLocalIPAddress());
		}
		profile.setProperty(Profile.LOCAL_PORT, "2000");

		if (microRuntimeServiceBinder == null) {
			serviceConnection = new ServiceConnection() {
				public void onServiceConnected(ComponentName className,
						IBinder service) {
					microRuntimeServiceBinder = (MicroRuntimeServiceBinder) service;
					logger.log(Level.INFO,
							"Gateway successfully bound to MicroRuntimeService");
					startContainer(nickname, profile, agentStartupCallback);
				};

				public void onServiceDisconnected(ComponentName className) {
					microRuntimeServiceBinder = null;
					logger.log(Level.INFO,
							"Gateway unbound from MicroRuntimeService");
				}
			};
			logger.log(Level.INFO, "Binding Gateway to MicroRuntimeService...");

			bindService(new Intent(getApplicationContext(),
					MicroRuntimeService.class), serviceConnection,
					Context.BIND_AUTO_CREATE);

		} else {
			logger.log(Level.INFO,
					"MicroRumtimeGateway already binded to service");
			startContainer(nickname, profile, agentStartupCallback);
		}
	}

	private void startContainer(final String nickname, Properties profile,
			final RuntimeCallback<AgentController> agentStartupCallback) {
		if (!MicroRuntime.isRunning()) {

			RuntimeCallback<Void> rc = new RuntimeCallback<Void>() {
				@Override
				public void onSuccess(Void thisIsNull) {
					logger.log(Level.INFO,
							"Successfully start of the container...");
					startAgent(nickname, agentStartupCallback);
				}

				@Override
				public void onFailure(Throwable throwable) {
					logger.log(Level.SEVERE, "Failed to start the container...");
				}
			};

			microRuntimeServiceBinder.startAgentContainer(profile, rc);
		} else {
			startAgent(nickname, agentStartupCallback);
		}
	}

	private void startAgent(final String nickname,
			final RuntimeCallback<AgentController> agentStartupCallback) {

		RuntimeCallback<Void> rc = new RuntimeCallback<Void>() {
			@Override
			public void onSuccess(Void thisIsNull) {
				logger.log(Level.INFO, "Successfully start of the "
						+ AideAgent.class.getName() + "...");
				try {
					agentStartupCallback.onSuccess(MicroRuntime
							.getAgent(nickname));

				} catch (ControllerException e) {
					// Should never happen
					agentStartupCallback.onFailure(e);
				}
			}

			@Override
			public void onFailure(Throwable throwable) {
				logger.log(Level.SEVERE, "Failed to start the "
						+ AideAgent.class.getName() + "...");
				agentStartupCallback.onFailure(throwable);
			}
		};
		microRuntimeServiceBinder.startAgent(nickname,
				AideAgent.class.getName(), new Object[] {
						getApplicationContext(),
						(SGMApplication) getApplication() }, rc);
	}
}
