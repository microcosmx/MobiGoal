/**
 * 
 */
package cn.edu.fudan.se.mobigoal.maincontainer.fragments.subfragments.download;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import cn.edu.fudan.se.R;

/**
 * @author whh
 * 
 */
public class DownloadActivity extends FragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download);

		if (savedInstanceState == null) {
			FragmentTransaction transaction = getSupportFragmentManager()
					.beginTransaction();

			transaction.add(R.id.container, new DownloadFragment()).commit();
		}

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
//			// 跳到消息界面
//			Intent startMain = new Intent(this, MainActivity.class);
//			startMain.putExtra("INITIALINDEX", 0);
//			startActivity(startMain);
			finish();
			return true;
		}
		return true;
	}
}
