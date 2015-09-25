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
public class ProcedureActivity extends FragmentActivity {
    private int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_procedure);

        // 获取传递过来的intent中的goal model position，然后从全局变量中得到对应的goal model
        Intent intent = getIntent();
        position = intent.getIntExtra("position", 0);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager()
                    .beginTransaction();
            ProcedureFragment procedureFragment = new ProcedureFragment();
            Bundle positionBundle = new Bundle();
            positionBundle.putInt("Position", position);
            procedureFragment.setArguments(positionBundle);

            transaction.add(R.id.container, procedureFragment)
                    .commit();
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.procedure, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_showdetails:
                // 跳转到progress activity
                Intent intent = new Intent();
                intent.setClass(this, ProgressActivity.class);
                intent.putExtra("position", position);
                startActivity(intent);
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
