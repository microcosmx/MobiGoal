/**
 * 
 */
package cn.edu.fudan.se.mobigoal.maincontainer.fragments.subfragments.execution;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import cn.edu.fudan.se.R;
import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.utils.staticGoal.StaticEM;
import cn.edu.fudan.se.mobigoal.utils.staticGoal.StaticGM;
import cn.edu.fudan.se.mobigoal.utils.staticGoal.StaticGoalModel;
import cn.edu.fudan.se.sgm.goalmachine.ElementMachine;
import cn.edu.fudan.se.sgm.goalmachine.GoalMachine;
import cn.edu.fudan.se.sgm.goalmachine.State;
import cn.edu.fudan.se.sgm.goalmodel.GoalModel;

/**
 * @author whh
 *
 */
public class ProgressFragment extends ListFragment {
	
	private int position;
	
	public ProgressFragment(){
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        position = getArguments().getInt("Position");
		// application = (SGMApplication) getActivity().getApplication();

		Object executionObject = ((SGMApplication) getActivity()
				.getApplication()).getExecutionList().get(position);

		if (executionObject instanceof GoalModel) {
			GoalTreeAdapter adapter = new GoalTreeAdapter(getActivity(),
					R.layout.listview_goalmodeldetails,
					((GoalModel) executionObject).getElementMachines());
			setListAdapter(adapter);
			
		} else if (executionObject instanceof StaticGoalModel) {
			StaticGoalTreeAdapter adapter = new StaticGoalTreeAdapter(getActivity(),
					R.layout.listview_goalmodeldetails,
					((StaticGoalModel) executionObject).getStaticEMs());
			setListAdapter(adapter);
		}
		

//		// 自定义action bar
//		ActionBar actionBar = getActivity().getActionBar();
//		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
//		actionBar.setCustomView(R.layout.actionbar_goaltree);

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	}
}


/**
 * 内部类，adapter
 * 
 * @author whh
 * 
 */
class GoalTreeAdapter extends ArrayAdapter<ElementMachine> {

	private Context mContext;
	private int mResource;
	private LayoutInflater mInflater;
	private List<ElementMachine> treeElements;
	private Bitmap iconAND; // and分解
	private Bitmap iconOR; // or分解

	public GoalTreeAdapter(Context context, int resource,
			List<ElementMachine> treeElements) {
		super(context, resource, treeElements);
		init(context, resource, treeElements);
	}

	private void init(Context context, int resource,
			List<ElementMachine> treeElements) {
		this.mContext = context;
		this.mResource = resource;
		this.mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.treeElements = treeElements;
		this.iconAND = BitmapFactory.decodeResource(context.getResources(),
				R.mipmap.tree_view_icon_and);
		this.iconOR = BitmapFactory.decodeResource(context.getResources(),
				R.mipmap.tree_view_icon_or);
	}

	@Override
	public int getCount() {
		return this.treeElements.size();
	}

	@Override
	public ElementMachine getItem(int position) {
		return this.treeElements.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		return createViewFromResource(position, convertView, parent, mResource);
	}

	private View createViewFromResource(int position, View convertView,
			ViewGroup parent, int resource) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(resource, parent, false);
			holder = new ViewHolder();
			holder.layout = (RelativeLayout) convertView
					.findViewById(R.id.tree_rl_goal);
			holder.icon = (ImageView) convertView
					.findViewById(R.id.tree_iv_goal_icon);
			holder.name = (TextView) convertView
					.findViewById(R.id.tree_tv_goal_name);
			holder.state = (ImageView) convertView
					.findViewById(R.id.tree_iv_goal_state);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		int level = treeElements.get(position).getLevel();
		holder.icon.setPadding(30 * level, holder.icon.getPaddingTop(), 0,
				holder.icon.getPaddingBottom());
		// 设置不同层级的背景颜色，目前只支持四级
		switch (level) {
		case 0:
			holder.layout.setBackgroundColor(mContext.getResources().getColor(
					R.color.gl_0));
			break;
		case 1:
			holder.layout.setBackgroundColor(mContext.getResources().getColor(
					R.color.gl_1));
			break;
		case 2:
			holder.layout.setBackgroundColor(mContext.getResources().getColor(
					R.color.gl_2));
			break;
		case 3:
			holder.layout.setBackgroundColor(mContext.getResources().getColor(
					R.color.gl_3));
			break;
		case 4:
			holder.layout.setBackgroundColor(mContext.getResources().getColor(
					R.color.gl_4));
			break;
		case 5:
			holder.layout.setBackgroundColor(mContext.getResources().getColor(
					R.color.gl_5));
			break;
		default:
			holder.layout.setBackgroundColor(mContext.getResources().getColor(
					R.color.gl_5));
			break;
		}

		holder.name.setText(treeElements.get(position).getName());

		State currentState = treeElements.get(position).getCurrentState();
		switch (currentState) {
		case Initial:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_initial));
			break;
		case Activated:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_process));
			break;
		case Executing:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_process));
			break;
		case Waiting:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_process));
			break;
		case Suspended:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_suspended));
			break;
		case Repairing:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_process));
			break;
		case ProgressChecking:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_process));
			break;
		case Achieved:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_achieved));
			break;
		case Stop:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_stop));
			break;

		case Failed:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_failed));
			break;
		}

		// 如果是root goal，不用显示图标
		if (treeElements.get(position).getParentGoal() == null) {
			holder.icon.setVisibility(View.INVISIBLE);
		} else {
			// 如果父目标是AND分解，当前目标显示AND图标
			if (((GoalMachine) (treeElements.get(position).getParentGoal()))
					.getDecomposition() == 0) { // AND分解
				holder.icon.setImageBitmap(iconAND);
			}
			// 如果父目标是OR分解，当前目标显示OR图标
			else if (((GoalMachine) (treeElements.get(position).getParentGoal()))
					.getDecomposition() == 1) { // OR分解
				holder.icon.setImageBitmap(iconOR);
			}
			holder.icon.setVisibility(View.VISIBLE);
		}

		return convertView;
	}

	class ViewHolder {
		RelativeLayout layout;
		ImageView icon;
		TextView name;
		ImageView state;
	}

}


/**
 * 内部类，adapter
 * 
 * @author whh
 * 
 */
class StaticGoalTreeAdapter extends ArrayAdapter<StaticEM> {

	private Context mContext;
	private int mResource;
	private LayoutInflater mInflater;
	private List<StaticEM> treeElements;
	private Bitmap iconAND; // and分解
	private Bitmap iconOR; // or分解

	public StaticGoalTreeAdapter(Context context, int resource,
			List<StaticEM> treeElements) {
		super(context, resource, treeElements);
		init(context, resource, treeElements);
	}

	private void init(Context context, int resource,
			List<StaticEM> treeElements) {
		this.mContext = context;
		this.mResource = resource;
		this.mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.treeElements = treeElements;
		this.iconAND = BitmapFactory.decodeResource(context.getResources(),
				R.mipmap.tree_view_icon_and);
		this.iconOR = BitmapFactory.decodeResource(context.getResources(),
				R.mipmap.tree_view_icon_or);
	}

	@Override
	public int getCount() {
		return this.treeElements.size();
	}

	@Override
	public StaticEM getItem(int position) {
		return this.treeElements.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		return createViewFromResource(position, convertView, parent, mResource);
	}

	private View createViewFromResource(int position, View convertView,
			ViewGroup parent, int resource) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(resource, parent, false);
			holder = new ViewHolder();
			holder.layout = (RelativeLayout) convertView
					.findViewById(R.id.tree_rl_goal);
			holder.icon = (ImageView) convertView
					.findViewById(R.id.tree_iv_goal_icon);
			holder.name = (TextView) convertView
					.findViewById(R.id.tree_tv_goal_name);
			holder.state = (ImageView) convertView
					.findViewById(R.id.tree_iv_goal_state);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		int level = treeElements.get(position).getLevel();
		holder.icon.setPadding(30 * level, holder.icon.getPaddingTop(), 0,
				holder.icon.getPaddingBottom());
		// 设置不同层级的背景颜色，目前只支持四级
		switch (level) {
		case 0:
			holder.layout.setBackgroundColor(mContext.getResources().getColor(
					R.color.gl_0));
			break;
		case 1:
			holder.layout.setBackgroundColor(mContext.getResources().getColor(
					R.color.gl_1));
			break;
		case 2:
			holder.layout.setBackgroundColor(mContext.getResources().getColor(
					R.color.gl_2));
			break;
		case 3:
			holder.layout.setBackgroundColor(mContext.getResources().getColor(
					R.color.gl_3));
			break;
		case 4:
			holder.layout.setBackgroundColor(mContext.getResources().getColor(
					R.color.gl_4));
			break;
		case 5:
			holder.layout.setBackgroundColor(mContext.getResources().getColor(
					R.color.gl_5));
			break;
		default:
			holder.layout.setBackgroundColor(mContext.getResources().getColor(
					R.color.gl_5));
			break;
		}

		holder.name.setText(treeElements.get(position).getName());

		State currentState = treeElements.get(position).getCurrentState();
		switch (currentState) {
		case Initial:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_initial));
			break;
		case Activated:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_process));
			break;
		case Executing:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_process));
			break;
		case Waiting:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_process));
			break;
		case Suspended:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_suspended));
			break;
		case Repairing:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_process));
			break;
		case ProgressChecking:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_process));
			break;
		case Achieved:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_achieved));
			break;
		case Stop:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_stop));
			break;

		case Failed:
			holder.state.setImageDrawable(mContext.getResources().getDrawable(
					R.mipmap.state_failed));
			break;
		}

		// 如果是root goal，不用显示图标
		if (treeElements.get(position).getParentGoal() == null) {
			holder.icon.setVisibility(View.INVISIBLE);
		} else {
			// 如果父目标是AND分解，当前目标显示AND图标
			if (((StaticGM) (treeElements.get(position).getParentGoal()))
					.getDecomposition() == 0) { // AND分解
				holder.icon.setImageBitmap(iconAND);
			}
			// 如果父目标是OR分解，当前目标显示OR图标
			else if (((StaticGM) (treeElements.get(position).getParentGoal()))
					.getDecomposition() == 1) { // OR分解
				holder.icon.setImageBitmap(iconOR);
			}
			holder.icon.setVisibility(View.VISIBLE);
		}

		return convertView;
	}

	class ViewHolder {
		RelativeLayout layout;
		ImageView icon;
		TextView name;
		ImageView state;
	}

}
