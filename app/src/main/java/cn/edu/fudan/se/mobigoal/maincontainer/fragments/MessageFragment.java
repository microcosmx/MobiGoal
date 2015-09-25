/**
 * 
 */
package cn.edu.fudan.se.mobigoal.maincontainer.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import cn.edu.fudan.se.R;
import cn.edu.fudan.se.agent.main.AideAgentInterface;
import cn.edu.fudan.se.agent.support.ACLMC_DelegateTask;
import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.support.TakePictureActivity;
import cn.edu.fudan.se.mobigoal.userMes.UserConfirmTask;
import cn.edu.fudan.se.mobigoal.userMes.UserConfirmWithRetTask;
import cn.edu.fudan.se.mobigoal.userMes.UserInputTextTask;
import cn.edu.fudan.se.mobigoal.userMes.UserShowContentTask;
import cn.edu.fudan.se.mobigoal.userMes.UserTakePictureTask;
import cn.edu.fudan.se.mobigoal.userMes.UserTask;
import cn.edu.fudan.se.sgm.goalmachine.message.MesBody_Mes2Manager;
import cn.edu.fudan.se.sgm.goalmachine.message.MesHeader_Mes2Manger;
import cn.edu.fudan.se.sgm.goalmachine.message.SGMMessage;
import cn.edu.fudan.se.sgm.goalmodel.EncodeDecodeRequestData;
import cn.edu.fudan.se.sgm.goalmodel.RequestData;
import jade.core.MicroRuntime;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

/**
 * message标签页
 * 
 * @author whh
 * 
 */
public class MessageFragment extends ListFragment {

	private SGMApplication application; // 获取应用程序，以得到里面的全局变量
	private UserTaskAdapter adapter;

	private AideAgentInterface aideAgentInterface; // agent interface

	private Handler handler;
	private Runnable runnable;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		application = (SGMApplication) getActivity().getApplication();

		try {
			aideAgentInterface = MicroRuntime.getAgent(
					application.getAgentNickname()).getO2AInterface(
					AideAgentInterface.class);
		} catch (StaleProxyException e) {
			Log.e("MessageFragment", "StaleProxyException");
			e.printStackTrace();
		} catch (ControllerException e) {
			Log.e("MessageFragment", "ControllerException");
			e.printStackTrace();
		}

		adapter = new UserTaskAdapter(getActivity(),
				R.layout.listview_usertask,
				application.getUserCurrentTaskList(), aideAgentInterface);

		setListAdapter(adapter);

		// 用于定时刷新
		handler = new Handler();
		runnable = new Runnable() {
			@Override
			public void run() {
				adapter.notifyDataSetChanged();
				handler.postDelayed(this, 500);
			}
		};
		handler.postDelayed(runnable, 500); // 0.5s刷新一次

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		setUserVisibleHint(true);
		super.onActivityCreated(savedInstanceState);

		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
	}

	@Override
	public void onDestroy() {
		handler.removeCallbacks(runnable);
		super.onDestroy();
	}


}

class UserTaskAdapter extends ArrayAdapter<UserTask> {

	private int mResource;
	private Context mContext;
	private LayoutInflater mInflater;
	private List<UserTask> mObjects;
	private AideAgentInterface aideAgentInterface; // agent interface

	public UserTaskAdapter(Context context, int resource,
			List<UserTask> objects, AideAgentInterface aideAgentInterface) {
		super(context, resource, objects);
		init(context, resource, objects, aideAgentInterface);
	}

	private void init(Context context, int resource, List<UserTask> objects,
			AideAgentInterface aideAgentInterface) {
		this.mContext = context;
		this.mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.mResource = resource;
		this.mObjects = objects;
		this.aideAgentInterface = aideAgentInterface;
	}

	@Override
	public int getCount() {
		return this.mObjects.size();
	}

	@Override
	public UserTask getItem(int position) {
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
			// undone
			holder.ll_tasklayout_undone = (LinearLayout) convertView
					.findViewById(R.id.ll_tasklayout_undone);
			holder.time = (TextView) convertView.findViewById(R.id.tv_taskTime);
			holder.fromAgent = (TextView) convertView
					.findViewById(R.id.tv_taskFromName);
			holder.description = (TextView) convertView
					.findViewById(R.id.tv_taskDescription);
			holder.done = (Button) convertView.findViewById(R.id.bt_taskDone);
			holder.quit = (Button) convertView.findViewById(R.id.bt_taskQuit);
			holder.decline = (Button) convertView.findViewById(R.id.bt_taskDecline);

			// done
			holder.ll_tasklayout_done = (RelativeLayout) convertView
					.findViewById(R.id.ll_tasklayout_done);
			holder.time_done = (TextView) convertView
					.findViewById(R.id.tv_taskTime_done);
			holder.fromAgent_done = (TextView) convertView
					.findViewById(R.id.tv_taskFromName_done);
			holder.description_done = (TextView) convertView
					.findViewById(R.id.tv_taskDescription_done);
			holder.del_mes = (ImageView) convertView
					.findViewById(R.id.iv_task_delete);

			convertView.setTag(holder);

		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// 下面部分不可缺少，是设置每个item具体显示的地方！
		final UserTask usertask = getItem(position);
		if (usertask.isDone()) {// done
			holder.ll_tasklayout_done.setVisibility(View.VISIBLE);
			holder.ll_tasklayout_undone.setVisibility(View.GONE);

			holder.time_done.setText(usertask.getTime());
			holder.fromAgent_done.setText("From: "
					+ usertask.getFromAgentName());
			holder.description_done.setText(usertask.getDescription());
			holder.del_mes.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							mContext);
					builder.setTitle("Delete");
					builder.setIcon(android.R.drawable.ic_dialog_alert);
					builder.setMessage("Are you sure to delete this message?");
					builder.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									mObjects.remove(usertask);
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
		} else {
			holder.ll_tasklayout_undone.setVisibility(View.VISIBLE);
			holder.ll_tasklayout_done.setVisibility(View.GONE);

			holder.time.setText(usertask.getTime());
			holder.fromAgent.setText("From: " + usertask.getFromAgentName());
//			holder.decline.setText("Decline");

			holder.done.setOnClickListener(new UserTaskDoneListener(usertask));
			holder.quit.setOnClickListener(new UserTaskQuitListener(usertask));
            holder.decline.setOnClickListener(new UserTaskQuitListener(usertask));

			// if (usertask instanceof UserDelegateInTask) { // 如果是需要用户选择委托去向的任务
			// holder.done.setText("Accept");
			// } else
			if (usertask instanceof UserShowContentTask) {// 展示内容的user task
				holder.done.setText("Show");
//				holder.quit.setText("Fail");
                holder.quit.setVisibility(View.GONE);
                holder.decline.setText("Decline");
			} else if (usertask instanceof UserTakePictureTask) {// 让用户拍照的task
				holder.done.setText("Camera");
//				holder.quit.setText("Fail");
                holder.quit.setVisibility(View.GONE);
                holder.decline.setText("Decline");
			} else if (usertask instanceof UserInputTextTask) {// 让用户输入一段文本的task
				holder.done.setText("Input");
//				holder.quit.setText("Fail");
                holder.quit.setVisibility(View.GONE);
                holder.decline.setText("Decline");
			} else if (usertask instanceof UserConfirmTask
					|| usertask instanceof UserConfirmWithRetTask) {
				holder.done.setText("Yes");
				holder.quit.setText("No");
                holder.decline.setText("Decline");
			} else {// 普通的user task
				holder.done.setText("Done");
				holder.quit.setText("Fail");
                holder.decline.setText("Decline");
			}

			holder.description.setText(usertask.getDescription());
		}

		return convertView;
	}

	/**
	 * 用户点击done按钮时的监听器，根据不同的UserTask类型有不同的响应
	 * 
	 * @author whh
	 * 
	 */
	private class UserTaskDoneListener implements OnClickListener {
		private UserTask userTask;

		public UserTaskDoneListener(UserTask userTask) {
			this.userTask = userTask;
		}

		@Override
		public void onClick(View v) {
			// 让用户输入文本的task
			if (userTask instanceof UserInputTextTask) {
				showInputTextDialog(userTask);
			}
			// 展示内容的user task
			else if (userTask instanceof UserShowContentTask) {
				showContentDialog(userTask);
			}
			// 让用户拍照的task
			else if (userTask instanceof UserTakePictureTask) {
				userTask.setDone(true);
				// mObjects.remove(userTask);
				// doneTaskList.add(0, userTask);

				Intent intent = new Intent();
				intent.setClass(mContext, TakePictureActivity.class);
				intent.putExtra("fromAgentName", userTask.getFromAgentName());
				intent.putExtra("goalmodelname", userTask.getGoalModelName());
				intent.putExtra("elementname", userTask.getElementName());
				intent.putExtra("requestDataName",
						userTask.getRequestDataName());
				mContext.startActivity(intent);
			} else if (userTask instanceof UserConfirmWithRetTask) {

				RequestData needRequestData = ((UserConfirmWithRetTask) userTask)
						.getNeedRequestData();
				RequestData retRequestData = new RequestData(
						needRequestData.getName(), "BooleanText");
				String toReturn = EncodeDecodeRequestData
						.decodeToText(needRequestData.getContent())
						+ " at "
						+ userTask.getRequestDataName().split("%")[1];
				retRequestData.setContent(toReturn.getBytes());

				System.out
						.println("debug!!!!!!UnreadFragement---------toReturn:"
								+ toReturn);

				ACLMC_DelegateTask aclmc_DelegateTask = new ACLMC_DelegateTask(
						ACLMC_DelegateTask.DTHeader.DTBACK, null,
						userTask.getFromAgentName(),
						userTask.getGoalModelName(), userTask.getElementName());
				aclmc_DelegateTask.setRetRequestData(retRequestData);
				aclmc_DelegateTask.setDone(true);// 完成了
				aideAgentInterface.sendMesToExternalAgent(aclmc_DelegateTask);

				userTask.setDone(true);
				// mObjects.remove(userTask);
				// doneTaskList.add(0, userTask);
			}
			// 普通的user task 或者让用户确认结果的task
			else {
				ACLMC_DelegateTask aclmc_DelegateTask = new ACLMC_DelegateTask(
						ACLMC_DelegateTask.DTHeader.DTBACK, null,
						userTask.getFromAgentName(),
						userTask.getGoalModelName(), userTask.getElementName());

				aclmc_DelegateTask.setDone(true);// 完成了
				aideAgentInterface.sendMesToExternalAgent(aclmc_DelegateTask);

				userTask.setDone(true);
				// mObjects.remove(userTask);
				// doneTaskList.add(0, userTask);
			}
		}

	}

	/**
	 * 用户点击quit按钮时的监听器，只有委托出去的任务是结束一个goal machine，其余都是结束一个task machine
	 * 
	 * @author whh
	 * 
	 */
	private class UserTaskQuitListener implements OnClickListener {
		private UserTask userTask;

		public UserTaskQuitListener(UserTask userTask) {
			this.userTask = userTask;
		}

		@Override
		public void onClick(View v) {

			if (userTask instanceof UserShowContentTask) {
				aideAgentInterface.sendMesToManager(new SGMMessage(
						MesHeader_Mes2Manger.LOCAL_AGENT_MESSAGE, userTask
								.getGoalModelName(), null, userTask
								.getElementName(), new MesBody_Mes2Manager(
								"QuitTE")));
			} else {// 其余的都是委托任务
				ACLMC_DelegateTask aclmc_DelegateTask = new ACLMC_DelegateTask(
						ACLMC_DelegateTask.DTHeader.DTBACK, null,
						userTask.getFromAgentName(),
						userTask.getGoalModelName(), userTask.getElementName());

				aclmc_DelegateTask.setDone(false);// 没有完成
				aideAgentInterface.sendMesToExternalAgent(aclmc_DelegateTask);
			}

			userTask.setDone(true);
			// mObjects.remove(userTask);
			// doneTaskList.add(0, userTask);
		}

	}

	private class ViewHolder {
		LinearLayout ll_tasklayout_undone;
		TextView time;
		TextView fromAgent;
		TextView description;
		Button done;
		Button quit;
		Button decline;

		RelativeLayout ll_tasklayout_done;
		TextView time_done;
		TextView fromAgent_done;
		TextView description_done;
		ImageView del_mes;
	}

	/**
	 * 让用户输入文本的task
	 * 
	 * @param userTask
	 *            UserInputTextTask
	 */
	private void showInputTextDialog(final UserTask userTask) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle("Input:");
		builder.setIcon(android.R.drawable.ic_dialog_info);

		View view = LayoutInflater.from(mContext).inflate(
				R.layout.dialog_userinput, null);
		final EditText editText = (EditText) view
				.findViewById(R.id.et_userinput);

		builder.setView(view);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String userInput = editText.getText().toString();

				ACLMC_DelegateTask aclmc_DelegateTask = new ACLMC_DelegateTask(
						ACLMC_DelegateTask.DTHeader.DTBACK, null, userTask
								.getFromAgentName(), userTask
								.getGoalModelName(), userTask.getElementName());
				aclmc_DelegateTask.setDone(true);

				RequestData requestData = new RequestData(userTask
						.getRequestDataName(), "Text");
				requestData.setContent(userInput.getBytes());

				aclmc_DelegateTask.setRetRequestData(requestData);
				aideAgentInterface.sendMesToExternalAgent(aclmc_DelegateTask);

				userTask.setDone(true);
				// mObjects.remove(userTask);
				// doneTaskList.add(0, userTask);

				dialog.cancel();
			}

		});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	/**
	 * 创建一个显示RequestData的对话框，显示的可以是Text或者Image
	 * 
	 * @param userTask
	 *            要显示的requestData
	 */
	private void showContentDialog(final UserTask userTask) {

		RequestData requestData = ((UserShowContentTask) userTask)
				.getRequestData();

		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

		if (requestData.getContentType().equals("List")) {

			builder.setTitle("Select:");

			// seller:tom;price:20;addr:room10###seller:
			String listString = EncodeDecodeRequestData
					.decodeToText(requestData.getContent());
			final String[] sellerInfos = listString.split("###");

			final int[] selectIndex = new int[1];
			builder.setSingleChoiceItems(sellerInfos, 0,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							selectIndex[0] = which;

						}
					});

			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// System.out
							// .println("DEBUG!!!!----TaskFragment,dialog. you select:"
							// + sellerInfos[selectIndex[0]]);

							String select = sellerInfos[selectIndex[0]];
							// 将选中的结果发回去
							RequestData retRequestData = new RequestData(
									userTask.getRequestDataName(), "Text");
							retRequestData.setContent(select.getBytes());

							ACLMC_DelegateTask aclmc_DelegateTask = new ACLMC_DelegateTask(
									ACLMC_DelegateTask.DTHeader.DTBACK, null,
									userTask.getFromAgentName(), userTask
											.getGoalModelName(), userTask
											.getElementName());
							aclmc_DelegateTask.setDone(true);
							aclmc_DelegateTask
									.setRetRequestData(retRequestData);
							aideAgentInterface
									.sendMesToExternalAgent(aclmc_DelegateTask);

							userTask.setDone(true);
							// mObjects.remove(userTask);
							// doneTaskList.add(0, userTask);
							dialog.cancel();
						}
					});
			builder.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});

		} else {

			builder.setTitle("Content:");
			builder.setIcon(android.R.drawable.ic_dialog_info);

			if (requestData.getContentType().equals("Text")) {
				builder.setMessage(EncodeDecodeRequestData
						.decodeToText(requestData.getContent()));
			} else if (requestData.getContentType().equals("Image")) {
				View view = LayoutInflater.from(mContext).inflate(
						R.layout.dialog_showcontent, null);
				ImageView imageView = (ImageView) view
						.findViewById(R.id.iv_dialog_content);
				imageView.setImageBitmap(EncodeDecodeRequestData
						.decodeToBitmap(requestData.getContent()));
				builder.setView(view);
			}

			builder.setNeutralButton("OK",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							// 只要点击了show按钮就表示这个“展示任务”完成了
							aideAgentInterface.sendMesToManager(new SGMMessage(
									MesHeader_Mes2Manger.LOCAL_AGENT_MESSAGE,
									userTask.getGoalModelName(), null, userTask
											.getElementName(),
									new MesBody_Mes2Manager("EndTE")));
							userTask.setDone(true);
							// mObjects.remove(userTask);
							// doneTaskList.add(0, userTask);

							dialog.cancel();
						}
					});

		}

		AlertDialog dialog = builder.create();
		dialog.show();
	}

}
