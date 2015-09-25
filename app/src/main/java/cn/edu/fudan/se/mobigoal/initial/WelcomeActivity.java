package cn.edu.fudan.se.mobigoal.initial;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import cn.edu.fudan.se.R;
import cn.edu.fudan.se.agent.activity.SettingAgentActivity;
import cn.edu.fudan.se.agent.activity.StartAgentActivity;


/**
 * 欢迎界面，也就是那个设置agent的界面
 * @author whh
 *
 */
public class WelcomeActivity extends Activity{

    private Button bt_setAgent;
	private Button bt_startAgent;

	static final int SETTINGS_REQUEST = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_welcome);
		if (savedInstanceState == null) {
			bt_setAgent = (Button) findViewById(R.id.bt_setAgent);
			bt_startAgent = (Button) findViewById(R.id.bt_startAgent);

			bt_setAgent.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// 跳转到zjh写的设置界面
					Intent showSettings = new Intent(WelcomeActivity.this, SettingAgentActivity.class);
					startActivityForResult(showSettings, SETTINGS_REQUEST);
				}
			});

			bt_startAgent.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// 跳转到zjh写的开始agent界面，也就是他代码中的MainActivity，在这里更名为了StartAgentActivity
					Intent startAgent = new Intent(WelcomeActivity.this,
							StartAgentActivity.class);
					startActivity(startAgent);

				}
			});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

//	@Override
//	public void onBackPressed() {
//		// 点击返回键后不会退出程序，也就是再次进来的时候还是原来的运行状态
//		this.moveTaskToBack(true);
//		return;
//	}
}
