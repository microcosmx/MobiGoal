/**
 * 
 */
package cn.edu.fudan.se.mobigoal.maincontainer.fragments.subfragments.goalmodel;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import cn.edu.fudan.se.R;
import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.sgm.goalmodel.GoalModel;

/**
 * @author whh
 * 
 */
public class GoalModelActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_goalmodel);

		// 获取传递过来的intent中的goal model position，然后从全局变量中得到对应的goal model
		Intent intent = getIntent();
		String goalmodelname = intent.getStringExtra("goalmodelname");

		GoalModel goalModel = ((SGMApplication) getApplication())
				.getGoalModelManager().getGoalModelList().get(goalmodelname);

		if (savedInstanceState == null) {
			FragmentTransaction transaction = getSupportFragmentManager()
					.beginTransaction();
            GoalModelFragment goalModelFragment = new GoalModelFragment();
            Bundle goalModelBundle = new Bundle();
            goalModelBundle.putSerializable("GoalModel",goalModel);
            goalModelFragment.setArguments(goalModelBundle);

			transaction.add(R.id.container, goalModelFragment)
					.commit();
		}
	}

}
