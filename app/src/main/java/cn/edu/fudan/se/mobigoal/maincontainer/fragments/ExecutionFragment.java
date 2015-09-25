/**
 * 
 */
package cn.edu.fudan.se.mobigoal.maincontainer.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import cn.edu.fudan.se.R;
import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.maincontainer.fragments.subfragments.execution.ProcedureActivity;
import cn.edu.fudan.se.mobigoal.utils.staticGoal.StaticGoalModel;
import cn.edu.fudan.se.sgm.goalmodel.GoalModel;

/**
 * @author whh
 * 
 */
public class ExecutionFragment extends ListFragment {
	private SGMApplication application; // 获取应用程序，以得到里面的全局变量

	private ExecutionAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		application = (SGMApplication) getActivity().getApplication();

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		adapter = new ExecutionAdapter(getActivity(),
				R.layout.listview_execution, application.getExecutionList());
		setListAdapter(adapter);

		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Intent intent = new Intent();
		intent.setClass(getActivity(), ProcedureActivity.class);
		intent.putExtra("position", position);
		startActivity(intent);
	}
}

class ExecutionAdapter extends ArrayAdapter<Object> {

	private List<Object> mObjects;
	private int mResource;
	private Context mContext;
	private LayoutInflater mInflater;

	public ExecutionAdapter(Context context, int resource, List<Object> objects) {
		super(context, resource, objects);
		init(context, resource, objects);
	}

	private void init(Context context, int resource, List<Object> objects) {
		this.mContext = context;
		this.mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.mResource = resource;
		this.mObjects = objects;
	}

	@Override
	public int getCount() {
		return this.mObjects.size();
	}

	@Override
	public Object getItem(int position) {
		return this.mObjects.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return createViewFromResource(position, convertView, parent, mResource);
	}

	private View createViewFromResource(int position, View convertView,
			ViewGroup parent, int resource) {
		ViewHolder holder;

		if (convertView == null) {
			holder = new ViewHolder();
			convertView = mInflater.inflate(resource, parent, false);

			holder.finishTime = (TextView) convertView
					.findViewById(R.id.tv_execution_finishtime);
			holder.state = (TextView) convertView
					.findViewById(R.id.tv_execution_state);
			holder.goalName = (TextView) convertView
					.findViewById(R.id.tv_execution_goalname);

			convertView.setTag(holder);

		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// 下面部分不可缺少，是设置每个item具体显示的地方！
		Object object = getItem(position);
		if (object instanceof GoalModel) {// 正在执行的goal model
			holder.finishTime.setText(((GoalModel) object).getStartTime());
			// holder.finishTime.setVisibility(View.INVISIBLE);
			holder.state.setText("State: Executing");
			holder.state.setTextColor(mContext.getResources().getColor(
					R.color.state_executing_green));
			if (((GoalModel) object).getConstantValue() != null
					&& !((GoalModel) object).getConstantValue().trim()
							.equals("")) {
				holder.goalName.setText(((GoalModel) object).getName() + " : "
						+ ((GoalModel) object).getConstantValue());
			} else {
				holder.goalName.setText(((GoalModel) object).getName());
			}
		} else if (object instanceof StaticGoalModel) {// 已经完成的goal model
			holder.finishTime.setText(((StaticGoalModel) object)
					.getStartTime());
			holder.state.setText("State: Achieved");
			holder.state.setTextColor(mContext.getResources().getColor(
					R.color.state_finished_blue));
			if (((StaticGoalModel) object).getConstantValue() != null
					&& !((StaticGoalModel) object).getConstantValue().trim()
							.equals("")) {
				holder.goalName
						.setText(((StaticGoalModel) object).getName() + " : "
								+ ((StaticGoalModel) object).getConstantValue());
			} else {
				holder.goalName.setText(((StaticGoalModel) object).getName());
			}
		}

		return convertView;
	}

	class ViewHolder {
		TextView finishTime;
		TextView state;
		TextView goalName;
	}
}
