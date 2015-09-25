/**
 * 
 */
package cn.edu.fudan.se.mobigoal.maincontainer.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.edu.fudan.se.R;
import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.maincontainer.fragments.subfragments.goalmodel.GoalModelActivity;
import cn.edu.fudan.se.mobigoal.support.BindingCustomItem;
import cn.edu.fudan.se.sgm.goalmachine.ElementMachine;
import cn.edu.fudan.se.sgm.goalmachine.GoalMachine;
import cn.edu.fudan.se.sgm.goalmachine.State;
import cn.edu.fudan.se.sgm.goalmodel.GmXMLParser;
import cn.edu.fudan.se.sgm.goalmodel.GoalModel;
import cn.edu.fudan.se.sgm.goalmodel.GoalModelManager;
import cn.edu.fudan.se.sgm.log.Log;

/**
 * myGoal标签页
 * 
 * @author whh
 * 
 */
public class MyGoalFragment extends ListFragment {

	private SGMApplication application; // 获取应用程序，以得到里面的全局变量

	private MyGoalListAdapter adapter;

	private ArrayList<GoalModel> goalmodels;

	public MyGoalFragment() {

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		application = (SGMApplication) getActivity().getApplication();

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// 将adapter的初始化移到onActivityCreated中后，manager中goal
		// model发生变化时，这里会触发notifyDataChanged()
		goalmodels = new ArrayList<>(application.getGoalModelManager()
				.getGoalModelList().values());

		adapter = new MyGoalListAdapter(getActivity(),
				R.layout.listview_mygoal, goalmodels,
				application.getGoalModelManager());

		setListAdapter(adapter);

		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Intent intent = new Intent();
		intent.setClass(getActivity(), GoalModelActivity.class);
		intent.putExtra("goalmodelname", goalmodels.get(position).getName());
		startActivity(intent);
	}

}

/**
 * 用于MyGoalFragment的list适配器<br/>
 * 使用Adapter轻松完成注册Listener的过程。我们继承BaseAdapter，然后在getView中实现整个初始化的过程。
 * 
 * @author whh
 * 
 * @param
 */
class MyGoalListAdapter extends ArrayAdapter<GoalModel> {

	private int mResource;
	private Context mContext;
	private LayoutInflater mInflater;
	private List<GoalModel> mObjects;
	private GoalModelManager goalModelManager;

	public MyGoalListAdapter(Context context, int resource,
			List<GoalModel> objects, GoalModelManager goalModelManager) {
		super(context, resource, objects);
		init(context, resource, objects, goalModelManager);
	}

	private void init(Context context, int resource, List<GoalModel> objects,
			GoalModelManager goalModelManager) {
		this.mContext = context;
		this.mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.mResource = resource;
		this.mObjects = objects;
		this.goalModelManager = goalModelManager;
	}

	@Override
	public int getCount() {
		return this.mObjects.size();
	}

	@Override
	public GoalModel getItem(int position) {
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

			holder.text = (TextView) convertView.findViewById(R.id.goal_text);
			holder.icon = (ImageView) convertView.findViewById(R.id.goal_image);

			convertView.setTag(holder);

		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// 下面部分不可缺少，是设置每个item具体显示的地方！
		GoalModel item = getItem(position);
		holder.text.setText(item.getName());

		holder.icon.setOnClickListener(new GoalModelSettingListener(item));
		holder.icon.setFocusable(false);

		return convertView;
	}

	class GoalModelSettingListener implements OnClickListener {

		private GoalModel goalModel;

		public GoalModelSettingListener(GoalModel goalModel) {
			this.goalModel = goalModel;
		}

		@Override
		public void onClick(View v) {

			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

			// 如果goal model已经在运行中了，暂时不能做定制化
			if (goalModel.getRootGoal().getCurrentState() != State.Initial) {
				builder.setTitle("Warning");
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				builder.setMessage("The goal model is running. No setting allowed!");
				builder.setNeutralButton("OK",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
			} else {
				Log.logCustomization(goalModel.getName(),
                        "customization started!");
				// 先找出or分解的子目标，让用户设定优先级
				if (!goalModel.getElementMachines().isEmpty()) {

					ArrayList<CustomItem> customItemList = new ArrayList<>();

					for (ElementMachine elementMachine : goalModel
							.getElementMachines()) {
						// 是goal machine而且是or分解
						if ((elementMachine instanceof GoalMachine)
								&& (((GoalMachine) elementMachine)
										.getDecomposition() == 1)) {
							// 先将这个不需要定制的父目标加进来
							customItemList.add(new CustomItem(elementMachine
									.getName(), false));
							for (ElementMachine sub : ((GoalMachine) elementMachine)
									.getSubElements()) {
								// 将需要定制的子目标加进来
								customItemList.add(new CustomItem(
										sub.getName(), true));
							}
						}
					}

					ListView listView = new ListView(mContext);
					final PriorityCustomizationViewAdapter cvAdapter = new PriorityCustomizationViewAdapter(
							mContext, R.layout.listview_customization,
							customItemList);
					listView.setAdapter(cvAdapter);

					// 弹出对goal model做定制化的对话框
					builder.setTitle("Set Priority");
					builder.setIcon(android.R.drawable.btn_star);
					builder.setView(listView);
					builder.setPositiveButton("Save",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {

									HashMap<String, Integer> toCustom = new HashMap<>();

									for (int i = 0; i < cvAdapter.getCount(); i++) {
										CustomItem item = cvAdapter.getItem(i);
										if (item.isNeedCustom()) {
											toCustom.put(item.getName(),
													item.getPriority());
										}
									}

									// 将定制好的优先级写入文件
									String sdCardDir = Environment
											.getExternalStorageDirectory()
											.getPath()
											+ "/sgm/fxml/";
									String filePath = sdCardDir
											+ goalModel.getName().trim()
											+ ".xml";
									GmXMLParser.editGoalModel(filePath,
                                            toCustom);

									// 然后再重新解析文件，替换goal model manager中的goal
									// model
									GmXMLParser parser = new GmXMLParser();
									GoalModel newGoalModel = parser
											.newGoalModel(filePath);
									goalModelManager.getGoalModelList().remove(
											goalModel.getName());
									goalModelManager.addGoalModel(newGoalModel);
									notifyDataSetChanged();

									Log.logCustomization(goalModel.getName(),
											"customization finished! reparse finished!");
								}
							});
					builder.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();

									Log.logCustomization(goalModel.getName(),
											"customization canceled!");
								}
							});

					AlertDialog dialog = builder.create();
					dialog.setCanceledOnTouchOutside(false);// 使除了dialog以外的地方不能被点击
					dialog.show();
					dialog.getWindow().clearFlags(
							WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
				}
			}
			// 下面是绑定的设置
			if (!goalModel.getDeviceEventMapToExternalEventTable().isEmpty()) {

				final ArrayList<BindingCustomItem> bindingCustomList = new ArrayList<>();
				for (String key : goalModel
						.getDeviceEventMapToExternalEventTable().keySet()) {
					if (key.contains("Time")) {
						bindingCustomList
								.add(new BindingCustomItem(
										key,
										goalModel
												.getDeviceEventMapToExternalEventTable()
												.get(key).getExternalEvent()
												.toString(),
										goalModel
												.getDeviceEventMapToExternalEventTable()
												.get(key).getElementName(), key
												.substring(4), null));
					}
					if (key.contains("Phone")) {
						bindingCustomList
								.add(new BindingCustomItem(
										key,
										goalModel
												.getDeviceEventMapToExternalEventTable()
												.get(key).getExternalEvent()
												.toString(),
										goalModel
												.getDeviceEventMapToExternalEventTable()
												.get(key).getElementName(),
										null, key.substring(5)));
					}
				}

				if (!bindingCustomList.isEmpty()) {

					AlertDialog.Builder builder2 = new AlertDialog.Builder(
							mContext);
					ListView listView = new ListView(mContext);
					final BindingCustomizationViewAdapter cvAdapter = new BindingCustomizationViewAdapter(
							mContext, R.layout.listview_customization_binding,
							bindingCustomList);
					listView.setAdapter(cvAdapter);

					// 弹出对goal model做定制化的对话框
					builder2.setTitle("Binding Setting:");
					builder2.setIcon(android.R.drawable.btn_star);
					builder2.setView(listView);
					builder2.setPositiveButton("Save",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {

									// 将定制好的时间写入文件
									String sdCardDir = Environment
											.getExternalStorageDirectory()
											.getPath()
											+ "/sgm/fxml/";
									String filePath = sdCardDir
											+ goalModel.getName().trim()
											+ ".xml";
									GmXMLParser.editGoalModelBinding(filePath,
											bindingCustomList);

									// 然后再重新解析文件，替换goal model manager中的goal
									// model
									GmXMLParser parser = new GmXMLParser();
									GoalModel newGoalModel = parser
											.newGoalModel(filePath);
									goalModelManager.getGoalModelList().remove(
											goalModel.getName());
									goalModelManager.addGoalModel(newGoalModel);
									notifyDataSetChanged();

									Log.logCustomization(goalModel.getName(),
											"customization finished! reparse finished!");
								}
							});
					builder2.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();

									Log.logCustomization(goalModel.getName(),
											"customization canceled!");
								}
							});

					AlertDialog dialog2 = builder2.create();
					dialog2.setCanceledOnTouchOutside(false);// 使除了dialog以外的地方不能被点击
					dialog2.show();
					dialog2.getWindow().clearFlags(
							WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
				}
			}
		}
	}

	class ViewHolder {
		TextView text;
		ImageView icon;
	}

}

/**
 * 定制化优先级对话框中的ListView适配器
 * 
 * @author whh
 * 
 */
class PriorityCustomizationViewAdapter extends ArrayAdapter<CustomItem> {

	private Context mContext;
	private int mResource;
	private LayoutInflater mInflater;
	private ArrayList<CustomItem> customItemList;

	public PriorityCustomizationViewAdapter(Context context, int resource,
			ArrayList<CustomItem> customItemList) {
		super(context, resource, customItemList);
		init(context, resource, customItemList);
	}

	private void init(Context context, int resource,
			ArrayList<CustomItem> customItemList) {
		this.mContext = context;
		this.mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.mResource = resource;
		this.customItemList = customItemList;
	}

	@Override
	public int getCount() {
		return this.customItemList.size();
	}

	@Override
	public CustomItem getItem(int position) {
		return this.customItemList.get(position);
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
		final ViewHolder holder;

		if (convertView == null) {
			holder = new ViewHolder();
			convertView = mInflater.inflate(resource, parent, false);

			holder.layout = (RelativeLayout) convertView
					.findViewById(R.id.custom_rl_item);
			holder.name = (TextView) convertView
					.findViewById(R.id.tv_custom_name);
			holder.priority = (EditText) convertView
					.findViewById(R.id.et_custom_priority);

			convertView.setTag(holder);

		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// 下面部分不可缺少，是设置每个item具体显示的地方！
		final CustomItem item = getItem(position);
		holder.name.setText(item.getName());
		if (item.isNeedCustom()) {// 需要定制
			holder.name.setPadding(30, 0, 0, 0);
			holder.priority.setVisibility(View.VISIBLE);
			holder.priority.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					// 获取用户输入的数字
					String input = holder.priority.getText().toString();
					try {
						item.setPriority(Integer.parseInt(input));
					} catch (Exception e) {

					}

				}
			});
		} else {
			holder.name.setPadding(0, 0, 0, 0);
			holder.layout.setBackgroundColor(mContext.getResources().getColor(
					R.color.custom));
			holder.priority.setVisibility(View.INVISIBLE);
		}

		return convertView;
	}

	class ViewHolder {
		RelativeLayout layout;
		TextView name;
		EditText priority;
	}

}

/**
 * 定制化binding对话框中的ListView适配器
 * 
 * @author whh
 * 
 */
class BindingCustomizationViewAdapter extends ArrayAdapter<BindingCustomItem> {

	private Context mContext;
	private int mResource;
	private LayoutInflater mInflater;
	private ArrayList<BindingCustomItem> customItemList;

	public BindingCustomizationViewAdapter(Context context, int resource,
			ArrayList<BindingCustomItem> customItemList) {
		super(context, resource, customItemList);
		init(context, resource, customItemList);
	}

	private void init(Context context, int resource,
			ArrayList<BindingCustomItem> customItemList) {
		this.mContext = context;
		this.mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.mResource = resource;
		this.customItemList = customItemList;
	}

	@Override
	public int getCount() {
		return this.customItemList.size();
	}

	@Override
	public BindingCustomItem getItem(int position) {
		return this.customItemList.get(position);
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
		final ViewHolder holder;

		if (convertView == null) {
			holder = new ViewHolder();
			convertView = mInflater.inflate(resource, parent, false);

			holder.tv_external_value = (TextView) convertView
					.findViewById(R.id.tv_external_value);
			holder.rl_element = (RelativeLayout) convertView
					.findViewById(R.id.rl_element);
			holder.tv_element_value = (TextView) convertView
					.findViewById(R.id.tv_element_value);
			holder.rl_set_time = (RelativeLayout) convertView
					.findViewById(R.id.rl_set_time);
			holder.tp_set_time = (TimePicker) convertView
					.findViewById(R.id.tp_set_time);
			holder.rl_set_phone = (RelativeLayout) convertView
					.findViewById(R.id.rl_set_phone);
			holder.et_set_phone = (EditText) convertView
					.findViewById(R.id.et_set_phone);

			convertView.setTag(holder);

		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// 下面部分不可缺少，是设置每个item具体显示的地方！
		final BindingCustomItem item = getItem(position);
		holder.tv_external_value.setText(item.getExternal());
		if (item.getElement() == null || item.getElement().equals("")) {
			holder.rl_element.setVisibility(View.GONE);
		} else {
			holder.rl_element.setVisibility(View.VISIBLE);
			holder.tv_element_value.setText(item.getElement());
		}
		if (item.getDevice().contains("Time")) {// 设置时间的
			holder.rl_set_time.setVisibility(View.VISIBLE);
			holder.tp_set_time.setIs24HourView(true);
			holder.tp_set_time.setCurrentHour(Integer.parseInt(item.getTime()
					.split(":")[0]));
			holder.tp_set_time.setCurrentMinute(Integer.parseInt(item.getTime()
					.split(":")[1]));
			holder.tp_set_time
					.setOnTimeChangedListener(new OnTimeChangedListener() {

						@Override
						public void onTimeChanged(TimePicker view,
								int hourOfDay, int minute) {
							if (hourOfDay < 10) {
								if (minute < 10) {
									item.setTime("0" + hourOfDay + ":0"
											+ minute);
								} else {
									item.setTime("0" + hourOfDay + ":" + minute);
								}
							} else {
								if (minute < 10) {
									item.setTime(hourOfDay + ":0" + minute);
								} else {
									item.setTime(hourOfDay + ":" + minute);
								}
							}
						}
					});
			holder.rl_set_phone.setVisibility(View.GONE);// 这时设置电话的不可见

		} else if (item.getDevice().contains("Phone")) {// 设置电话的
			holder.rl_set_time.setVisibility(View.GONE);
			holder.rl_set_phone.setVisibility(View.VISIBLE);
			holder.et_set_phone.setText(item.getPhone());
			holder.et_set_phone.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					item.setPhone(holder.et_set_phone.getText().toString());
				}
			});
		}

		return convertView;
	}

	class ViewHolder {
		TextView tv_external_value;
		RelativeLayout rl_element;
		TextView tv_element_value;
		RelativeLayout rl_set_time;
		TimePicker tp_set_time;
		RelativeLayout rl_set_phone;
		EditText et_set_phone;
	}

}

/**
 * 定制化优先级
 * 
 * @author whh
 * 
 */
class CustomItem {

	private String name;
	private boolean needCustom; // 如果这个条目是不需要定制的父目标，这个为false
	private int priority;

	public CustomItem(String name, boolean needCustom) {
		this.name = name;
		this.needCustom = needCustom;
	}

	public String getName() {
		return this.name;
	}

	public boolean isNeedCustom() {
		return needCustom;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}
}
