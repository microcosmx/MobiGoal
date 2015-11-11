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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuth;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.demo.AccessTokenKeeper;
import com.sina.weibo.sdk.demo.Constants;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.openapi.legacy.FriendshipsAPI;

import java.util.logging.Level;

import cn.edu.fudan.se.R;
import cn.edu.fudan.se.agent.main.AideAgent;
import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.maincontainer.MainActivity;
import cn.edu.fudan.se.servicestub.sync.webservice.ClientAuthorization;
import cn.edu.fudan.se.weibo.FriendsListRequest;
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
 */
public class StartAgentActivity extends Activity {
    private WeiboAuth mWeiboAuth;
    private Oauth2AccessToken mAccessToken;

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
        // TODO here we intend to invoke weibo api. Mark....

        public void onClick(View v) {
            // add by jiahuan zheng
            System.out.println("buttonChatListener has been clicked.");
            mWeiboAuth = new WeiboAuth(StartAgentActivity.this, Constants.APP_KEY, Constants.REDIRECT_URL, Constants.SCOPE);
            mWeiboAuth.anthorize(new AuthListener());
            System.out.println("mWeiboAuth variable has been used.");
        }
    };

    private void startAgentByNickname(String nickname) {
        if (!checkName(nickname)) {
            logger.log(Level.INFO, "Invalid nickname!");
        } else {
            try {
                ((SGMApplication) getApplication()).setAgentNickname(nickname);
                ((SGMApplication) getApplication()).getGoalModelManager().setAgentNickname(nickname);
                ClientAuthorization.agentNickName = nickname;
                Log.i("start agent", "agent nickname: " + ((SGMApplication) getApplication()).getAgentNickname());
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

    /**
     * 微博认证授权回调类。
     * 1. SSO 授权时，需要在 {@link #onActivityResult} 中调用 {@link SsoHandler#authorizeCallBack} 后，
     * 该回调才会被执行。
     * 2. 非 SSO 授权时，当授权结束后，该回调就会被执行。
     * 当授权成功后，请保存该 access_token、expires_in、uid 等信息到 SharedPreferences 中。
     */

    class AuthListener implements WeiboAuthListener {

        @Override
        public void onComplete(Bundle values) {
            // 从 Bundle 中解析 Token
            System.out.println("fuck ! be invoked.");
            mAccessToken = Oauth2AccessToken.parseAccessToken(values);
            if (mAccessToken.isSessionValid()) {
                // 显示 Token
//				updateTokenView(false);

                // 保存 Token 到 SharedPreferences
                AccessTokenKeeper.writeAccessToken(StartAgentActivity.this, mAccessToken);
                Toast.makeText(StartAgentActivity.this,
                        "authorize success", Toast.LENGTH_SHORT).show();
                String uidNickname = mAccessToken.getUid();
                // Here we don't execute startAgentByNickname function.
               // startAgentByNickname(uidNickname);

                Long uid = Long.parseLong(uidNickname);

                System.out.println("weibo interface has been invoked correctly and the user id is : "+uid);

                FriendshipsAPI friendshipsAPI = new FriendshipsAPI(mAccessToken);
                friendshipsAPI.bilateralIds(uid, 50, 1, new FriendsListRequest());
            } else {
                // 当您注册的应用程序签名不正确时，就会收到 Code，请确保签名正确
                String code = values.getString("code");
                String message = "authorize fail";
                if (!TextUtils.isEmpty(code)) {
                    message = message + "\nObtained the code: " + code;
                }
                Toast.makeText(StartAgentActivity.this, message, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onCancel() {
            //TODO show the operation has cancelled.
        }

        @Override
        public void onWeiboException(WeiboException e) {
            Toast.makeText(StartAgentActivity.this,
                    "Auth exception : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

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
                }

                ;

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
                AideAgent.class.getName(), new Object[]{
                        getApplicationContext(),
                        (SGMApplication) getApplication()}, rc);
    }
}
