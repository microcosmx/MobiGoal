/**
 * 
 */
package cn.edu.fudan.se.mobigoal.maincontainer.fragments.subfragments.download;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cn.edu.fudan.se.R;
import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.support.DownloadTask;
import cn.edu.fudan.se.sgm.goalmodel.GmXMLParser;
import cn.edu.fudan.se.sgm.goalmodel.GoalModel;
import cn.edu.fudan.se.sgm.goalmodel.GoalModelManager;
import cn.edu.fudan.se.sgm.log.Log;

/**
 * 从服务器上下载goal model xml文件的fragment
 * 
 * @author whh
 * 
 */
public class DownloadFragment extends ListFragment {

	private SGMApplication application; // 获取应用程序，以得到里面的全局变量

	private DownloadListAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		application = (SGMApplication) getActivity().getApplication();

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		adapter = new DownloadListAdapter(getActivity(),
				R.layout.listview_download, application.getDownloadTaskList(),
				application.getGoalModelManager(), application);
		setListAdapter(adapter);

		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

	}

}

/**
 * 用于DownloadFragment的list适配器<br/>
 * 
 * @author whh
 * 
 * @param
 */
class DownloadListAdapter extends ArrayAdapter<DownloadTask> {

	private List<DownloadTask> mObjects;
	private int mResource;
	private Context mContext;
	private LayoutInflater mInflater;
	private GoalModelManager goalModelManager;
	private SGMApplication application;

	public DownloadListAdapter(Context context, int resource,
			List<DownloadTask> objects, GoalModelManager goalModelManager,
			SGMApplication application) {
		super(context, resource, objects);
		init(context, resource, objects, goalModelManager, application);
	}

	private void init(Context context, int resource,
			List<DownloadTask> objects, GoalModelManager goalModelManager,
			SGMApplication application) {
		mContext = context;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mResource = resource;
		mObjects = objects;
		this.goalModelManager = goalModelManager;
		this.application = application;
	}

	@Override
	public int getCount() {
		return this.mObjects.size();
	}

	@Override
	public DownloadTask getItem(int position) {
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

			holder.text = (TextView) convertView
					.findViewById(R.id.tv_download_filename);
			holder.icon = (ImageView) convertView
					.findViewById(R.id.iv_download);

			convertView.setTag(holder);

		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// 下面部分不可缺少，是设置每个item具体显示的地方！
		final DownloadTask downloadTask = getItem(position);
		holder.text.setText(downloadTask.getName());
		if (downloadTask.isAlreadyDownload() == true) {
			holder.icon.setImageResource(R.mipmap.goal_alreadydownload_image);
			holder.icon.setClickable(false);
		} else {
			holder.icon.setImageResource(R.mipmap.goal_todownload_image);
			// 还没有下载的要添加下载按钮的监听器，点击后下载
			holder.icon.setClickable(true);
			holder.icon.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					ExecutorService executorService = Executors
							.newCachedThreadPool();
					DownLoadGoalModelTask task = new DownLoadGoalModelTask(
							downloadTask);
					Future<Boolean> result = executorService.submit(task);
					try {
						
						if (result.get()==true) {
							System.out.println("下载成功： " + result.get());

							Toast.makeText(getContext(), "Download Succeed!", Toast.LENGTH_LONG);
							// 把downloadTask标记为已下载
							downloadTask.setAlreadyDownload(true);
							notifyDataSetChanged();
						}else {
							Toast.makeText(getContext(), "Download Error! Please Retry!", Toast.LENGTH_LONG);
						}
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
					

				}
			});
		}

		return convertView;
	}

	class DownLoadGoalModelTask implements Callable<Boolean> {

		private DownloadTask downloadTask;

		private DownLoadGoalModelTask(DownloadTask downloadTask) {
			this.downloadTask = downloadTask;
		}

		@Override
		public Boolean call() throws Exception {
			return downloadGoalModel(downloadTask);
		}

	}

	/**
	 * 从服务器上下载一个goal model xml文件保存到本地，并解析它，将解析出来的goal model添加到goalModelManager中
	 * 
	 * @param downloadTask
	 *            要下载的goal model的downloadTask
	 */
	private Boolean downloadGoalModel(DownloadTask downloadTask) {
		Boolean ret = false;
		String sdCardDir = Environment.getExternalStorageDirectory().getPath()
				+ "/sgm/fxml/";
		try {
			Log.logCustomization(downloadTask.getName(), "downloading started!");
			System.out.println("DownloadFragment--downloadGoalModel()--开始下载");
			URL url = new URL(downloadTask.getUrl());
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000);// 设置超时时间为5s
			if (connection.getResponseCode() == 200) {
				InputStream inputStream = connection.getInputStream(); // 获得输入流
				File file = new File(sdCardDir + downloadTask.getName());
				FileOutputStream fileOutputStream = new FileOutputStream(file); // 对应文件建立输出流
				byte[] buffer = new byte[1024];// 新建缓存, 用来存储从网络读取数据 再写入文件
				int len = 0;
				while ((len = inputStream.read(buffer)) != -1) {// 当没有读到最后的时候
					fileOutputStream.write(buffer, 0, len);
				}
				fileOutputStream.flush();
				fileOutputStream.close();
				inputStream.close();

				// 文件已经保存到sbCard里了，解析它 //TODO
				GmXMLParser gmXMLParser = new GmXMLParser();
				GoalModel goalModel = gmXMLParser.newGoalModel(sdCardDir
						+ downloadTask.getName());
				goalModelManager.addGoalModel(goalModel);

//				// 把下载的服务注册到agent platform上
//				GetAgent.getAideAgentInterface(application)
//						.registerGoalModelService(goalModel);
				ret = true;
				Log.logCustomization(downloadTask.getName(),
						"downloading finished. parse finished.");
			} else {
				System.err
						.println("DownloadFragment--downloadGoalModel()--网络连接失败, Error Code: "
								+ connection.getResponseCode());

				Log.logCustomization(downloadTask.getName(),
						"downloading failed.");
			}
		} catch (IOException e) {
			System.err
					.println("DownloadFragment--downloadGoalModel()--IOException");

			Log.logCustomization(downloadTask.getName(),
					"downloading IOException.");
			e.printStackTrace();
		}
		return ret;
	}

	class ViewHolder {
		TextView text;
		ImageView icon;
	}

}