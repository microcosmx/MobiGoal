/**
 * 
 */
package cn.edu.fudan.se.mobigoal.initial;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

import cn.edu.fudan.se.mobigoal.support.DownloadTask;
import cn.edu.fudan.se.mobigoal.userMes.UserTask;
import cn.edu.fudan.se.sgm.goalmodel.GmXMLParser;
import cn.edu.fudan.se.sgm.goalmodel.GoalModel;
import cn.edu.fudan.se.sgm.goalmodel.GoalModelManager;
import jade.util.Logger;

/**
 * 重写Application，主要重写里面的onCreate方法，设置并初始化一些全局变量
 * 
 * @author whh
 * 
 */
public class SGMApplication extends Application implements Serializable {

	private static final long serialVersionUID = 1L;

	private Logger logger = Logger.getJADELogger(this.getClass().getName());

	private ArrayList<UserTask> userDoneTaskList;
	private ArrayList<UserTask> userCurrentTaskList;
	// private ArrayList<UserLog> userLogList;
	private ArrayList<DownloadTask> downloadTaskList;

	private ArrayList<Object> executionList;

	private String agentNickname;

	private GoalModelManager goalModelManager;

	private String location = ""; // 位置信息

	@Override
	public void onCreate() {
		super.onCreate();
		initialData();
		initialJadePreferences();
	}

	/**
	 * 把用户的goal model list数据加载进来，如果以后要从xml文件里读取，就是在这里设置
	 */
	private void initialData() {
		this.userDoneTaskList = new ArrayList<>();
		this.userCurrentTaskList = new ArrayList<>();
		this.executionList = new ArrayList<>();
		// this.userLogList = new ArrayList<>();
		this.downloadTaskList = new ArrayList<>();

		goalModelManager = new GoalModelManager(this);
		GmXMLParser gmXMLParser = new GmXMLParser();

		/* 先读取本地sdCard上sgm/fxml目录下的文件，这里存储的是已经下载的goal model xml文件，如果有，直接解析 */
		String sdCardDir = Environment.getExternalStorageDirectory().getPath()
				+ "/sgm/fxml/";
		File file = new File(sdCardDir);
		// 如果路径不存在，创建一个目录
		if (!file.exists()) {
			file.mkdirs();
		}

		HashMap<String, String> localFileList = new HashMap<String, String>();
		getLocalFileList(file, localFileList);

		if (!localFileList.isEmpty()) {
			for (String filePath : localFileList.values()) {
				GoalModel goalModel = gmXMLParser.newGoalModel(filePath);
				goalModelManager.addGoalModel(goalModel);
			}
		}

		/*
		 * 读取服务器上的goal model xml文件列表，添加DownloadTask
		 * 由于不能在主线程中直接进行网络连接，所以另外开一个线程连接网络
		 */
		ExecutorService executor = Executors.newCachedThreadPool();
		GetServerFileListTask getServerFileListTask = new GetServerFileListTask();
		Future<HashMap<String, String>> result = executor
				.submit(getServerFileListTask);

		// HashMap<String, String> serverFileList = getServerFileList();
		HashMap<String, String> serverFileList = new HashMap<String, String>();
		try {
			serverFileList = result.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		if (!serverFileList.isEmpty()) {
			for (String fileName : serverFileList.keySet()) {
				DownloadTask downloadTask = new DownloadTask(fileName,
						serverFileList.get(fileName));
				if (localFileList.containsKey(fileName)) { // 本地文件中有这个文件，说明这个是已经被下载的
					// System.out.println("SGMApplication--localFileList.containsKey():"
					// + fileName);
					downloadTask.setAlreadyDownload(true);
				} else {
					downloadTask.setAlreadyDownload(false);
				}
				this.downloadTaskList.add(downloadTask);
			}
		}

		Thread gmm = new Thread(goalModelManager);
		gmm.start();

	}

	/**
	 * 迭代函数，读取文件夹中的文件列表，把文件名和文件绝对路径储存在一个HashMap中
	 * 
	 * @param path
	 *            文件夹路径
	 * @param localFileList
	 *            储存着文件名和文件绝对路径的HashMap
	 */
	private void getLocalFileList(File path,
			HashMap<String, String> localFileList) {
		// 如果是文件夹
		if (path.isDirectory()) {
			// 返回文件夹中有的数据
			File[] files = path.listFiles();
			// 先判断下有没有权限，如果没有权限的话，就不执行了
			if (files == null) {
				return;
			}
			for (int i = 0; i < files.length; i++) {
				getLocalFileList(files[i], localFileList);
			}
		} else {// 如果是文件的话直接加入
			String filePath = path.getAbsolutePath();
			// 文件名
			String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
			localFileList.put(fileName, filePath);
		}
	}

	class GetServerFileListTask implements Callable<HashMap<String, String>> {

		@Override
		public HashMap<String, String> call() throws Exception {
			return getServerFileList();
		}

	}

	/**
	 * 从服务器端获取goal model xml列表
	 * 
	 * @return 储存着文件名和文件下载链接的HashMap
	 */
	private HashMap<String, String> getServerFileList() {
		HashMap<String, String> serverFileList = new HashMap<String, String>();
		try {
			URL url = new URL(
					"http://10.131.252.246:8080/sgmfiles/filelist.txt");
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000);// 超时时间为5s
			if (connection.getResponseCode() == 200) {
				InputStream inputStream = connection.getInputStream();
				byte[] buffer = new byte[1024];
				inputStream.read(buffer);
				String[] filenames = new String(buffer).split(",");
				for (int i = 0; i < filenames.length - 1; i++) {
					// System.out.println("SGMApplication---"+filenames[i] +
					// ".xml");
					serverFileList.put(filenames[i] + ".xml",
							"http://10.131.252.246:8080/sgmfiles/xml/"
									+ filenames[i] + ".xml");
				}
				inputStream.close();
			} else {
				System.err
						.println("SGMApplication--getServerFileList()--网络连接失败, Error Code: "
								+ connection.getResponseCode());
			}
		} catch (IOException e) {
			System.err
					.println("SGMApplication--getServerFileList()--IOException");
			e.printStackTrace();
		}
		return serverFileList;
	}

	/**
	 * zjh所写代码，把jade需要的相关属性初始化
	 */
	private void initialJadePreferences() {
		SharedPreferences settings = getSharedPreferences("jadeChatPrefsFile",
				0);

		String defaultHost = settings.getString("defaultHost", "");
		String defaultPort = settings.getString("defaultPort", "");
		if (defaultHost.isEmpty() || defaultPort.isEmpty()) {
			logger.log(Level.INFO, "Create default properties");
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("defaultHost", "10.131.253.133"); // 改成jade平台的ip
			editor.putString("defaultPort", "1099");
			editor.commit();
		}
	}

	public String getAgentNickname() {
		return this.agentNickname;
	}

	public GoalModelManager getGoalModelManager() {
		return this.goalModelManager;
	}

	public void setAgentNickname(String agentNickname) {
		this.agentNickname = agentNickname;
	}

	public ArrayList<UserTask> getUserDoneTaskList() {
		return userDoneTaskList;
	}

	public ArrayList<UserTask> getUserCurrentTaskList() {
		return userCurrentTaskList;
	}

	public void clearTasksOfGoalModel(GoalModel goalModel) {
		ArrayList<UserTask> toRemoveArrayList = new ArrayList<>();
		for (UserTask userTask : this.userDoneTaskList) {
			if (userTask.getGoalModelName().equals(goalModel.getName())) {
				toRemoveArrayList.add(userTask);
			}
		}
		this.userDoneTaskList.removeAll(toRemoveArrayList);

		ArrayList<UserTask> toRemoveArrayList2 = new ArrayList<>();
		for (UserTask userTask : this.userCurrentTaskList) {
			if (userTask.getGoalModelName().equals(goalModel.getName())) {
				toRemoveArrayList.add(userTask);
			}
		}
		this.userCurrentTaskList.removeAll(toRemoveArrayList2);
	}

	// public ArrayList<UserLog> getUserLogList() {
	// return userLogList;
	// }
	//
	// public void clearUserLogs() {
	// this.userLogList.clear();
	// }

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public ArrayList<DownloadTask> getDownloadTaskList() {
		return downloadTaskList;
	}

	public ArrayList<Object> getExecutionList() {
		return executionList;
	}

}
