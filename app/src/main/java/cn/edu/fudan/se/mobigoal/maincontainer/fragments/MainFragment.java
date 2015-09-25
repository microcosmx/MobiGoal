/**
 * 
 */
package cn.edu.fudan.se.mobigoal.maincontainer.fragments;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import cn.edu.fudan.se.R;
import cn.edu.fudan.se.agent.main.AideAgentInterface;
import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.maincontainer.fragments.subfragments.download.DownloadActivity;
import cn.edu.fudan.se.mobigoal.support.GetAgent;
import cn.edu.fudan.se.mobigoal.userMes.UserTask;
import cn.edu.fudan.se.sgm.goalmachine.State;
import cn.edu.fudan.se.sgm.goalmachine.message.MesBody_Mes2Manager;
import cn.edu.fudan.se.sgm.goalmachine.message.MesHeader_Mes2Manger;
import cn.edu.fudan.se.sgm.goalmachine.message.SGMMessage;
import cn.edu.fudan.se.sgm.goalmodel.GoalModel;

/**
 * 在主体container中的fragment，它有四个view pager，实现了左右标签页的滑动
 * 
 * @author whh
 * 
 */
public class MainFragment extends Fragment {

	private ViewPager mPager;
	private ArrayList<Fragment> fragmentsList;
	private LinearLayout ll_tab_message, ll_tab_mygoal, ll_tab_execution;
	private ImageView iv_tab_message, iv_tab_mygoal, iv_tab_execution;

	private TextView tv_tab_mygoal, tv_tab_message, tv_tab_execution;

	private Fragment messageFragment, mygoalFragment, excutionFragment;

	private int initialIndex;

	private ActionBar actionBar;

	public MainFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        initialIndex = getArguments().getInt("InitialIndex");

		/* 自定义action bar */
        actionBar = getActivity().getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.actionbar_mygoal);
		ImageView ab_iv_add_goal = (ImageView) actionBar.getCustomView()
				.findViewById(R.id.ab_iv_add_goal);
		ab_iv_add_goal.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(getActivity(), DownloadActivity.class);
				startActivity(intent);
			}
		});

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container,
				false);

		// initTabLayout
		ll_tab_mygoal = (LinearLayout) rootView
				.findViewById(R.id.ll_tab_mygoal);
		ll_tab_message = (LinearLayout) rootView
				.findViewById(R.id.ll_tab_message);
		ll_tab_execution = (LinearLayout) rootView
				.findViewById(R.id.ll_tab_execution);

		ll_tab_mygoal.setOnClickListener(new MyOnClickListener(0));
		ll_tab_message.setOnClickListener(new MyOnClickListener(1));
		ll_tab_execution.setOnClickListener(new MyOnClickListener(2));

		iv_tab_message = (ImageView) rootView.findViewById(R.id.iv_tab_message);
		iv_tab_mygoal = (ImageView) rootView.findViewById(R.id.iv_tab_mygoal);
		iv_tab_execution = (ImageView) rootView
				.findViewById(R.id.iv_tab_execution);

		tv_tab_mygoal = (TextView) rootView.findViewById(R.id.tv_tab_mygoal);
		tv_tab_message = (TextView) rootView.findViewById(R.id.tv_tab_message);
		tv_tab_execution = (TextView) rootView
				.findViewById(R.id.tv_tab_execution);

		// initViewpager
		initViewPager(rootView);

		return rootView;
	}

	/**
	 * 加载viewPager
	 * 
	 * @param parentView
	 */
	private void initViewPager(View parentView) {
		mPager = (ViewPager) parentView.findViewById(R.id.vp_main);
		fragmentsList = new ArrayList<Fragment>();

		mygoalFragment = new MyGoalFragment();
		messageFragment = new MessageFragment();
		excutionFragment = new ExecutionFragment();

		fragmentsList.add(mygoalFragment);
		fragmentsList.add(messageFragment);
		fragmentsList.add(excutionFragment);

		mPager.setAdapter(new MyFragmentPagerAdapter(getChildFragmentManager(),
				fragmentsList));
		mPager.setOnPageChangeListener(new MyOnPageChangeListener());
		mPager.setCurrentItem(initialIndex);

	}

	/**
	 * 内部类，用来响应tab的点击
	 * 
	 * @author whh
	 * 
	 */
	class MyOnClickListener implements OnClickListener {
		private int index = 0;

		public MyOnClickListener(int i) {
			index = i;
		}

		@Override
		public void onClick(View v) {
			mPager.setCurrentItem(index);
		}
	}

	/**
	 * 内部类
	 * 
	 * @author whh
	 * 
	 */
	class MyOnPageChangeListener implements OnPageChangeListener {

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {

		}

		@Override
		public void onPageScrollStateChanged(int arg0) {

		}

		@Override
		public void onPageSelected(int arg0) {
			// 设置按钮的显示
			switch (arg0) {
			case 0:
				iv_tab_mygoal.setBackgroundResource(R.mipmap.tab_mygoal_sel);
				iv_tab_message
						.setBackgroundResource(R.mipmap.tab_message_nor);
				iv_tab_execution
						.setBackgroundResource(R.mipmap.tab_execution_nor);

				tv_tab_mygoal.setTextColor(getResources().getColor(
						R.color.tab_blue));
				tv_tab_message.setTextColor(getResources().getColor(
						R.color.tab_grey));
				tv_tab_execution.setTextColor(getResources().getColor(
						R.color.tab_grey));

				// 自定义action bar
				actionBar.setCustomView(R.layout.actionbar_mygoal);
				ImageView ab_iv_add_goal = (ImageView) actionBar
						.getCustomView().findViewById(R.id.ab_iv_add_goal);
				ab_iv_add_goal.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent intent = new Intent();
						intent.setClass(getActivity(), DownloadActivity.class);
						startActivity(intent);
					}
				});
				break;

			case 1:
				iv_tab_mygoal.setBackgroundResource(R.mipmap.tab_mygoal_nor);
				iv_tab_message
						.setBackgroundResource(R.mipmap.tab_message_sel);
				iv_tab_execution
						.setBackgroundResource(R.mipmap.tab_execution_nor);

				tv_tab_mygoal.setTextColor(getResources().getColor(
						R.color.tab_grey));
				tv_tab_message.setTextColor(getResources().getColor(
						R.color.tab_blue));
				tv_tab_execution.setTextColor(getResources().getColor(
						R.color.tab_grey));

				// 自定义action bar，清空消息
				actionBar.setCustomView(R.layout.actionbar_message);
				ImageView ab_iv_clear_task = (ImageView) actionBar
						.getCustomView().findViewById(R.id.ab_iv_clear_task);
				ab_iv_clear_task.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						AlertDialog.Builder builder = new AlertDialog.Builder(
								getActivity());
						builder.setTitle("Delete");
						builder.setIcon(android.R.drawable.ic_dialog_alert);
						builder.setMessage("Are you sure to delete all messages?");
						builder.setPositiveButton("Yes",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										ArrayList<UserTask> currentTasks = ((SGMApplication) getActivity()
												.getApplication())
												.getUserCurrentTaskList();
										ArrayList<UserTask> toRemove = new ArrayList<UserTask>();
										for (UserTask userTask : currentTasks) {
											if (userTask.isDone()) {
												toRemove.add(userTask);
											}
										}
										currentTasks.removeAll(toRemove);
										dialog.dismiss();
									}
								});
						builder.setNegativeButton("NO",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
									}
								});

						AlertDialog dialog = builder.create();
						dialog.setCanceledOnTouchOutside(false);// 使除了dialog以外的地方不能被点击
						dialog.show();
					}
				});
				break;
			case 2:

				iv_tab_mygoal.setBackgroundResource(R.mipmap.tab_mygoal_nor);
				iv_tab_message
						.setBackgroundResource(R.mipmap.tab_message_nor);
				iv_tab_execution
						.setBackgroundResource(R.mipmap.tab_execution_sel);

				tv_tab_mygoal.setTextColor(getResources().getColor(
						R.color.tab_grey));
				tv_tab_message.setTextColor(getResources().getColor(
						R.color.tab_grey));
				tv_tab_execution.setTextColor(getResources().getColor(
						R.color.tab_blue));

				actionBar.setCustomView(R.layout.actionbar_execution);
				ImageView ab_iv_add_instance = (ImageView) actionBar
						.getCustomView().findViewById(R.id.ab_iv_add_instance);
				ab_iv_add_instance.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// 先获取可以新增实例的goal model name
						ArrayList<GoalModel> goalModels = new ArrayList<GoalModel>(
								((SGMApplication) getActivity()
										.getApplication())
										.getGoalModelManager()
										.getGoalModelList().values());
						ArrayList<String> noExecutinGoalModels = new ArrayList<String>();
						for (GoalModel goalModel : goalModels) {
							if (goalModel.getRootGoal().getCurrentState()
									.equals(State.Initial)) {
								noExecutinGoalModels.add(goalModel.getName());
							}
						}

						AlertDialog.Builder builder = new AlertDialog.Builder(
								getActivity());
						builder.setTitle("New Instance");
						builder.setIcon(android.R.drawable.ic_dialog_info);

						// 所有goal model都正在运行
						if (noExecutinGoalModels.size() == 0) {
							builder.setMessage("All goal models are executing.");
							builder.setNeutralButton("OK",
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											dialog.dismiss();
										}
									});
						}
						// 可以新增实例，弹出列表框
						else {
							final String[] selectString = new String[1];
							final String[] goalModelNames = (String[]) noExecutinGoalModels
									.toArray(new String[noExecutinGoalModels
											.size()]);
							builder.setSingleChoiceItems(goalModelNames, 0,
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											selectString[0] = goalModelNames[which];
										}

									});
							builder.setPositiveButton("Start",
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {

											AideAgentInterface aideAgentInterface = GetAgent
													.getAideAgentInterface((SGMApplication) getActivity()
                                                            .getApplication());
											aideAgentInterface
													.sendMesToManager(new SGMMessage(
															MesHeader_Mes2Manger.LOCAL_AGENT_MESSAGE,
															selectString[0],
															null,
															null,
															new MesBody_Mes2Manager(
																	"StartGM")));

											// 跳到消息界面
											mPager.setCurrentItem(2);
										}
									});

							builder.setNegativeButton("Cancel",
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											dialog.dismiss();
										}
									});
						}

						AlertDialog dialog = builder.create();
						dialog.show();
					}
				});
				break;

			default:
				break;
			}

		}
	}

	/**
	 * 内部类，MyFragmentPagerAdapter
	 * 
	 * @author whh
	 * 
	 */
	class MyFragmentPagerAdapter extends FragmentPagerAdapter {

		private ArrayList<Fragment> fragmentsList;

		/**
		 * @param fm
		 *            FragmentManager
		 */
		public MyFragmentPagerAdapter(FragmentManager fm,
				ArrayList<Fragment> fragments) {
			super(fm);
			this.fragmentsList = fragments;
		}

		@Override
		public Fragment getItem(int arg0) {
			return fragmentsList.get(arg0);
		}

		@Override
		public int getCount() {
			return fragmentsList.size();
		}

		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE; // To make notifyDataSetChanged() do something
		}

	}

}
