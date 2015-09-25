/**
 * 
 */
package cn.edu.fudan.se.mobigoal.maincontainer.fragments.subfragments.goalmodel;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import cn.edu.fudan.se.R;
import cn.edu.fudan.se.sgm.goalmodel.GoalModel;

/**
 * @author whh
 * 
 */
public class GoalModelAbstractFragment extends Fragment {

	private GoalModel goalModel; // 要显示目标树的goal model
	private TextView tv_gm_description;

	public GoalModelAbstractFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
        goalModel = (GoalModel)getArguments().getSerializable("GoalModel");
		View view = inflater.inflate(R.layout.fragment_goalmodelabstract, null);

		tv_gm_description = (TextView) view
				.findViewById(R.id.tv_gm_description);
		tv_gm_description.setText(goalModel.getDescription());
		
		return view;
	}
}