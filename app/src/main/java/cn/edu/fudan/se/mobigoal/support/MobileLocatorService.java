/**
 *
 */
package cn.edu.fudan.se.mobigoal.support;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.utils.Constant;
import cn.edu.fudan.se.mobigoal.utils.NotificationUtil;

/**
 * 类名：MobileLocatorService 功能描述：定位服务类。 启动方式：手动、开机自启动。
 * 关闭方式：用户在设置里强制停止应用、关闭手机。（用户使用其他软件杀死掉我们的服务，用户重新启动应用服务才会开启。）
 * 1、开机自启动服务，等1分钟后开始检测网络状态和GPS是否开启，并通过通知栏提醒用户。（未开启时，提醒三次，5分钟提醒一次）
 * 2、直接启动应用，立即开始检测网络状态和GPS是否开启，并通过弹Dialog提示用户。若用户不愿意开启网络，即网络不可用时，直接退出应用。
 * 3、用户在设置-->应用程序-->正在运行的服务里面手动停止掉服务后，服务自动重启。
 * 4、网络检测可用，开始检测GPS。用户不开启GPS时，使用基站定位（WLAN、3G/2G）。
 * 5、网络检测可用，启动百度地图定位服务，每隔五分钟确认一次当前我所在的位置，并将经纬度值上传服务器端。
 * 6、网络检测可用，但是在发送定位数据时，网络断开了，以Toast形式提醒用户。
 * 7、网络检测可用，但是在定位过程中，网络断开了，并且目前打开的不是我们的应用（也就是说服务在后台运行），以通知的形式提醒用户。
 * 8、服务运行过程中，意外停止了。当用户重启应用后，服务重新启动。
 * <p/>
 * 更改: 1、添加了开机自启动后，检测网络和通过通知栏提醒用户当前的网络、GPS状态。 2、服务运行过程中，网络检测返回的标识的处理。
 *
 * @author whh
 */
public class MobileLocatorService extends Service {

    // 间隔时间1分钟
    // private static final int DELAY_TIME = 1 * 60 * 1000;
    private static final int DELAY_TIME = 30 * 1000; // 30s

    // 百度定位SDK的核心类
    private LocationClient mLocationClient;

    // 定位结果处理器
    private MyLocationListener mLocationListener;

    // 通知工具类
    private NotificationUtil mNotificationUtil;

    /*
     * (non-Javadoc)
     *
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
//		Log.logEMDebug("MobileLocatorService", "onCreate()",
//                "-------MobileLocatorService onCreate()-------");
//		android.util.Log.i("MY_LOG",
//				"-------MobileLocatorService onCreate()-------");

        // 休眠5s是为了让agent能够启动起来，不然得不到agent的引用
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mNotificationUtil = new NotificationUtil(this);
        // 初始化定位服务，配置相应参数
        initLocationService();

        // 检查网络，在方法里，如果网络可用就开启了定位服务
        checkNetwork();
    }

    /**
     * 初始化定位服务，配置相应参数
     */
    private void initLocationService() {
        mLocationClient = new LocationClient(this.getApplicationContext());
        mLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(mLocationListener);

        LocationClientOption locationOption = new LocationClientOption();
        locationOption.setOpenGps(true);
        locationOption.setIsNeedAddress(true); // 返回的定位结果包含地址信息
        locationOption.setAddrType("all"); // 返回的定位结果包含地址信息
        locationOption.setCoorType("bd09ll");// 返回的定位结果是百度经纬度,默认值gcj02
        // locationOption.disableCache(true);// 禁止启用缓存定位
        // locationOption.setPriority(LocationClientOption.GpsFirst);
        locationOption.setScanSpan(DELAY_TIME);// 设置发起定位请求的间隔时间

        mLocationClient.setLocOption(locationOption);
    }

    /**
     * 检测网络是否可用
     *
     * @return 如果网络可用，就开启定位服务，返回true，否则返回false
     */
    private boolean checkNetwork() {
        // 如果网络不可用，开启GPS就没有意义
        if (isNetworkAvailable(this)) {
            if (!isGPSOPen(this)) {
                // 通知用户GPS未开启
                mNotificationUtil.showNotification("Notification:",
                        "GPS is not open!", "New Mes From SGM!",
                        Constant.Notification_GPS_State);
            }

//			Log.logEMDebug("MobileLocatorService", "checkNetwork()",
//					"MobileLocatorService start Location Service!");
//			android.util.Log.i("MY_LOG",
//					"MobileLocatorService start Location Service!");
            // 开启定位服务
            mLocationClient.start();
            return true;
        } else {
            // 通知用户网络不可用
            mNotificationUtil.showNotification("Notification:",
                    "Network Error!", "New Mes From SGM!",
                    Constant.Notification_Network_State);
            return false;
        }
    }

    /**
     * 检测当的网络（WLAN、3G/2G）状态
     *
     * @param context Context
     * @return true 表示网络可用
     */
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
     *
     * @param context
     * @return true 表示开启
     */
    private boolean isGPSOPen(final Context context) {
        LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }
        return false;

    }

    /**
     * 把位置信息发送给服务器
     *
     * @param location 位置信息
     */
    private void sendLocationToServer(String location) {

        // Log.logDebug("MobileLocatorService", "sendLocationToServer()",
        // "MobileLocatorService--sendLocationToServer()");
//		android.util.Log.i("MobileLocatorService",
//				"MobileLocatorService--sendLocationToServer()");
        GetAgent.getAideAgentInterface((SGMApplication) getApplication())
                .sendLocationToServerAgent(location);
    }

    /**
     * 更改本地全局变量中存储的位置信息
     *
     * @param location 新的位置信息
     */
    private void changeLocationInfo(String location) {

        // Log.logDebug("MobileLocatorService", "changeLocationInfo()",
        // "MobileLocatorService--changeLocationInfo()");
//		android.util.Log.i("MobileLocatorService",
//				"MobileLocatorService--changeLocationInfo()");
        SGMApplication sgmApplication = (SGMApplication) getApplication();
        sgmApplication.setLocation(location);
    }

    /**
     * 定位结果处理器
     *
     * @author whh
     */
    class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null) {
                return;
            }

            // Log.logDebug("MobileLocatorService", "onReceiveLocation()",
            // "BDLocationListener--onReceiveLocation()");
//            android.util.Log
//                    .i("MobileLocatorService",
//                            "MobileLocatorService--BDLocationListener--onReceiveLocation()");

			/*
             * location.getLocType()的返回值含义： 61:GPS定位结果。 62:扫描整合定位依据失败。此时定位结果无效。
			 * 63:网络异常，没有成功向服务器发起请求。此时定位结果无效。 65:定位缓存的结果。
			 * 66:离线定位结果。通过requestOfflineLocaiton调用时对应的返回结果。
			 * 67:离线定位失败。通过requestOfflineLocaiton调用时对应的返回结果。
			 * 68:网络连接失败时，查找本地离线定位时对应的返回结果。 161:表示网络定位结果。 162~167:服务端定位失败。
			 */
            int locType = location.getLocType();
            StringBuffer sb = new StringBuffer(256);
            sb.append("Time:");
            sb.append(location.getTime());
            sb.append("\nError code:");
            sb.append(locType);
            sb.append("\nLatitude:");
            sb.append(location.getLatitude());
            sb.append("\nLongitude:");
            sb.append(location.getLongitude());
            if (locType == BDLocation.TypeNetWorkLocation) { // 161
                sb.append("\nAddr:");
                sb.append(location.getAddrStr());
            }
            // Log.logDebug("MobileLocatorService", "onReceiveLocation()",
            // "sb: "
            // + sb.toString());
            // android.util.Log.i("MobileLocatorService",
            // "MobileLocatorService--sb: " + sb.toString());

            String locationToLog = "Latitude:" + location.getLatitude()
                    + ";Longitude:" + location.getLongitude();
//            android.util.Log.i("MobileLocatorService", "locType--"
//                    + locType);

            // 61或者161，定位成功
            if (locType == BDLocation.TypeGpsLocation
                    || locType == BDLocation.TypeNetWorkLocation) {
                // 将定位结果上传到agent服务器，并且更改本地存储的位置信息
                sendLocationToServer(locationToLog);
                changeLocationInfo(locationToLog);
            }
            // 63或者68
            else if (locType == BDLocation.TypeNetWorkException
                    || locType == BDLocation.TypeOffLineLocationNetworkFail) {
                // 通知用户网络不可用
                mNotificationUtil.showNotification("Notification:",
                        "Network Error!", "New Mes From SGM!",
                        Constant.Notification_Network_State);
            }
        }

    }

}
