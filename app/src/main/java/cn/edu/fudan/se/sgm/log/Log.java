/**
 * 
 */
package cn.edu.fudan.se.sgm.log;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.edu.fudan.se.sgm.goalmachine.message.SGMMessage;


/**
 * 把程序运行中的各种日志写入到日志文件，包括Debug log和Error log等
 * 
 * @author whh
 * 
 */
public class Log {

	/**
	 * 记录定制化的日志
	 * 
	 * @param content
	 */
	public static void logCustomization(String goalModelName, String content) {
		String customFile = "customization.txt";
		try {
			String con = "[" + goalModelName + "], " + content;
			writeAndroidLog(customFile, con);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 记录自适应过程
	 * 
	 * @param goalModelName
	 *            发生自适应的goal model
	 * @param elementName
	 *            发生自适应的element
	 * @param content
	 *            记录的内容
	 */
	public static void logAdaption(String goalModelName, String elementName,
			String content) {
		String adaptionFile = "adaption.txt";
		try {

			String con = "[" + goalModelName + "-" + elementName + "], "
					+ content;
			writeAndroidLog(adaptionFile, con);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 记录正常运行时的GoalModelManager的debug日志
	 * 
	 * @param methodName
	 *            GoalModelManager中的方法名字
	 * @param content
	 *            记录的内容
	 */
	public static void logGMMDebug(String methodName, String content) {
		String debugFile = "gmmdebug.txt";

		try {
			String con = "[GoalModelManager] " + methodName + ", " + content;
			writeAndroidLog(debugFile, con);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 记录正常运行的AideAgent的debug日志
	 * 
	 * @param agentName
	 *            goal name
	 * @param methodName
	 *            记录日志时调用的方法名字
	 * @param content
	 *            日志内容
	 */
	public static void logAADebug(String agentName, String methodName,
			String content) {

		String debugFile = "aadebug.txt";
		try {
			String con = "[" + agentName + "] " + methodName + ", LogContent: "
					+ content;
			writeAndroidLog(debugFile, con);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 记录正常运行的element machine的debug日志
	 * 
	 * @param goalName
	 *            goal name
	 * @param methodName
	 *            记录日志时调用的方法名字
	 * @param content
	 *            日志内容
	 */
	public static void logEMDebug(String goalName, String methodName,
			String content) {

		String debugFile = "emdebug.txt";
		try {
			String con = "[" + goalName + "] " + methodName + ", LogContent: "
					+ content;
			writeAndroidLog(debugFile, con);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 记录错误日志
	 * 
	 * @param goalName
	 *            goal name
	 * @param methodName
	 *            记录日志时调用的方法名字
	 * @param content
	 *            日志内容
	 */
	public static void logError(String goalName, String methodName,
			String content) {

		String errorFile = "error.txt";
		try {
			String con = "[" + goalName + "] " + methodName + ", LogContent: "
					+ content;
			writeAndroidLog(errorFile, con);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 记录消息发送日志
	 * 
	 * @param msg
	 *            记录的发送的消息
	 * @param success
	 *            消息是否发送成功
	 */
	public static void logMessage(SGMMessage msg, boolean success) {
		String messageFile = "message.txt";

		String result = "";
		if (success) {
			result = "succeed!";
		} else {
			result = "failed!";
		}

		String content = msg.getHeader() + ", [" + msg.getFromElementName()
				+ "] send to [" + msg.getToElementName() + "], body is: ["
				+ msg.getBody() + "]. " + result;
		try {
			writeAndroidLog(messageFile, content);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 把日志内容写入文件
	 * 
	 * @param filePath
	 *            文件路径
	 * @param content
	 *            日志内容
	 * @throws java.io.IOException
	 *             IO异常
	 */
	public static void write(String filePath, String content)
			throws IOException {

		File file = new File(filePath);

		if (!file.exists()) {
			file.createNewFile();
			System.out.println("Create new log file: " + filePath);

		}

		FileWriter writer = new FileWriter(file, true);

		Date nowDate = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		String time = dateFormat.format(nowDate);

		writer.write(time + " " + content + ".\n");
		writer.close();

	}

	/**
	 * 往安卓的sd卡上写日志文件
	 * 
	 * @param fileName
	 *            日志名字
	 * @param content
	 *            日志内容
	 * @throws java.io.IOException
	 *             异常
	 */
	private static void writeAndroidLog(String fileName, String content)
			throws IOException {

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			// 创建一个文件夹对象，赋值为外部存储器的目录
			File sdCardDir = Environment.getExternalStorageDirectory();
			// 得到一个路径，内容是sdcard的文件夹路径和APP自身名字
			String appDir = sdCardDir.getPath() + "/sgm/log/";
			File appPathFile = new File(appDir);
			// 如果路径不存在，创建一个目录
			if (!appPathFile.exists()) {
				appPathFile.mkdirs();
			}

			// 这个是在app目录下创建日志文件，文件名字为fileName
			File saveFile = new File(appDir, fileName);

			FileOutputStream fileOutputStream = new FileOutputStream(saveFile,
					true);

			Date nowDate = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss");
			String time = dateFormat.format(nowDate);

			String writeString = time + " " + content + "\n";

			fileOutputStream.write(writeString.getBytes());
			fileOutputStream.close();
		} else {
			android.util.Log.e("Log ERROR", "sdCard error!!");
		}

	}

	/**
	 * 清空日志
	 * 
	 * @param filePath
	 *            要清空的文件
	 * @throws java.io.IOException
	 *             IO异常
	 */
	public static void clearLog(String filePath) throws IOException {
		File file = new File(filePath);
		if (!file.exists()) {
			file.createNewFile();
		}

		FileWriter writer = new FileWriter(filePath, false);// 从开始写，即清空

		writer.write("");
		writer.close();
	}

}
