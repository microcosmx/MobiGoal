/**
 *
 */
package cn.edu.fudan.se.mobigoal.maincontainer.fragments.subfragments.execution;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

import cn.edu.fudan.se.R;

/**
 * @author whh
 */
public class ProgressActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_procedure);

        // 获取传递过来的intent中的goal model position，然后从全局变量中得到对应的goal model
        Intent intent = getIntent();
        int position = intent.getIntExtra("position", 0);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager()
                    .beginTransaction();
            ProgressFragment progressFragment = new ProgressFragment();
            Bundle positionBundle = new Bundle();
            positionBundle.putInt("Position", position);
            progressFragment.setArguments(positionBundle);

            transaction.add(R.id.container, progressFragment)
                    .commit();
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.progress, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
