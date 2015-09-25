/**
 * 
 */
package cn.edu.fudan.se.mobigoal.maincontainer.fragments.subfragments.execution;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
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
import cn.edu.fudan.se.mobigoal.utils.UserLog;
import cn.edu.fudan.se.mobigoal.utils.staticGoal.StaticGoalModel;
import cn.edu.fudan.se.sgm.goalmodel.GoalModel;

/**
 * setting标签页
 * 
 * @author whh
 * 
 */
public class ProcedureFragment extends ListFragment {

	// private SGMApplication application; // 获取应用程序，以得到里面的全局变量
	private UserMessageAdapter adapter;

	private int position;

	private Handler handler;
	private Runnable runnable;

	public ProcedureFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position = getArguments().getInt("Position");

        // application = (SGMApplication) getActivity().getApplication();

        Object executionObject = ((SGMApplication) getActivity()
                .getApplication()).getExecutionList().get(position);

        if (executionObject instanceof GoalModel) {
            adapter = new UserMessageAdapter(getActivity(),
                    R.layout.listview_procedure,
                    ((GoalModel) executionObject).getUserLogList());
        } else if (executionObject instanceof StaticGoalModel) {
            adapter = new UserMessageAdapter(getActivity(),
                    R.layout.listview_procedure,
                    ((StaticGoalModel) executionObject).getUserLogList());
        }
        // adapter = new UserMessageAdapter(getActivity(),
        // R.layout.listview_procedure, application.getUserLogList());

        setListAdapter(adapter);

        // 用于定时刷新
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
                handler.postDelayed(this, 1 * 1000);
            }
        };
        handler.postDelayed(runnable, 1 * 1000); // 1s刷新一次
    }
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	}

	@Override
	public void onDestroy() {
		handler.removeCallbacks(runnable);
		super.onDestroy();
	}
}

class UserMessageAdapter extends ArrayAdapter<UserLog> {

	private List<UserLog> mObjects;
	private int mResource;
	private Context mContext;
	private LayoutInflater mInflater;

	public UserMessageAdapter(Context context, int resource,
			List<UserLog> objects) {
		super(context, resource, objects);
		init(context, resource, objects);
	}

	private void init(Context context, int resource, List<UserLog> objects) {
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
	public UserLog getItem(int position) {
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

			holder.time = (TextView) convertView.findViewById(R.id.tv_mes_time);
			holder.content = (TextView) convertView
					.findViewById(R.id.tv_mes_content);

			convertView.setTag(holder);

		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// 下面部分不可缺少，是设置每个item具体显示的地方！
		// final User usertask = getItem(position);
		final UserLog userLog = getItem(position);
		holder.time.setText(userLog.getTime());
		holder.content.setText(userLog.getLog());

		return convertView;
	}

	class ViewHolder {
		TextView time;
		TextView content;
	}
}
