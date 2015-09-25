/**
 * 
 */
package cn.edu.fudan.se.mobigoal.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat.Builder;
import android.widget.RemoteViews;

import cn.edu.fudan.se.R;
import cn.edu.fudan.se.mobigoal.maincontainer.MainActivity;

/**
 * 以通知的形式提醒用户的处理工具类
 * 
 * @author whh
 * 
 */
public class NotificationUtil {

	private Context mContext;

	public NotificationUtil(Context mContext) {
		this.mContext = mContext;
	}

	/**
	 * 弹出一个通知
	 * 
	 * @param title
	 *            通知的title
	 * @param content
	 *            通知的内容
	 * @param ticker
	 *            通知的提示
	 * @param notificationIdentifier
	 *            通知标识符，不同的标识符在通知栏有不同的通知，同一个标识符后来的通知会覆盖前一个
	 */
	public void showNotification(String title, String content, String ticker,
			int notificationIdentifier) {

		RemoteViews notification_view = new RemoteViews(
				mContext.getPackageName(), R.layout.view_notification);
		notification_view.setImageViewResource(R.id.notification_icon,
				R.mipmap.app__launcher);
		notification_view.setTextViewText(R.id.tv_notification_title, title);
		notification_view
				.setTextViewText(R.id.tv_notification_content, content);

		NotificationManager mNotificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);

		Builder mBuilder = new Builder(mContext);
		mBuilder.setContent(notification_view).setTicker(ticker)
				.setWhen(System.currentTimeMillis())
				.setPriority(Notification.PRIORITY_DEFAULT).setOngoing(false)
				.setDefaults(Notification.DEFAULT_ALL)
				.setSmallIcon(R.mipmap.ic_launcher).setAutoCancel(true);

		// 点击的意图ACTION是跳转到Intent
		Intent resultIntent = new Intent(mContext, MainActivity.class);
		// resultIntent.putExtra("FLAG", "MESSAGE");
		resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0,
				resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(pendingIntent);

		Notification notification = mBuilder.build();
		notification.contentView = notification_view;
		mNotificationManager.notify(notificationIdentifier, notification);
	}

}
