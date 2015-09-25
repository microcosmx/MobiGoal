/**
 * 
 */
package cn.edu.fudan.se.agent.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.fudan.se.agent.data.UserInformation;

/**
 * 这个类里面的静态方法都是在AideAgent中用到的
 * 
 * @author whh
 * 
 */
public class AideAgentSupport {

	/**
	 * 根据抽象服务名称，从所有服务列表中查找出可用的具体服务名称
	 * 
	 * @param abstractServiceName
	 *            抽象服务名称
	 * @return 与抽象服务名称对应的所有可用的具体服务名称列表
	 */
	public static ArrayList<String> getServiceNameListBasedAbstractServiceName(
			String abstractServiceName,
			ArrayList<String> allIntentServiceNameArrayList) {
		ArrayList<String> ret = new ArrayList<>();

		for (String intentService : allIntentServiceNameArrayList) {
			if (intentService.contains(abstractServiceName)) {
				ret.add(intentService);
			}
		}

		return ret;
	}

	/**
	 * 当用户点击reset按钮后要把和那个goal model相关的taskExecutingAdaptionUtilList中储存的数据清空
	 * 
	 * @param goalModelName
	 *            reset的goal model
	 * @param taskExecutingAdaptionUtilList
	 *            相关的taskExecutingAdaptionUtilList
	 */
	public static void resetAdaptationUtilList(String goalModelName,
			Hashtable<String, AdaptationUtil> taskExecutingAdaptionUtilList) {

		ArrayList<String> toRemoveKeys = new ArrayList<>();
		for (String key : taskExecutingAdaptionUtilList.keySet()) {
			if (key.contains(goalModelName)) {
				toRemoveKeys.add(key);
			}
		}

		for (String key : toRemoveKeys) {
			taskExecutingAdaptionUtilList.remove(key);
		}

	}

	/**
	 * 初始化所有可能用到的intent service
	 */
	public static void initAllIntentService(
			ArrayList<String> allIntentServiceNameArrayList) {
		allIntentServiceNameArrayList
				.add("service.intentservice.weatherCandidate");
		allIntentServiceNameArrayList.add("service.intentservice.weather");
		allIntentServiceNameArrayList.add("service.intentservice.showcontent");
		allIntentServiceNameArrayList.add("service.intentservice.inputText");
		allIntentServiceNameArrayList.add("service.intentservice.takePicture");
		allIntentServiceNameArrayList.add("service.intentservice.userConfirm");

		allIntentServiceNameArrayList
				.add("service.intentservice.queryBookFromLibrary");
		allIntentServiceNameArrayList
				.add("service.intentservice.queryBookFromShop");
		allIntentServiceNameArrayList.add("service.intentservice.pay");
		allIntentServiceNameArrayList.add("service.intentservice.querySeller");
	}

	/**
	 * 返回两个人的亲密度
	 * 
	 * @param self
	 *            我自己
	 * @param friend
	 *            我朋友
	 * @return 朋友与自己的亲密度
	 */
	private static int getIntimacy(String self, String friend) {
		return 1;
	}

	/**
	 * 获得可委托对象的agent nick name list
	 * 
	 * @param userInformations
	 *            所有可委托对象的userInformation
	 * @param selfLocation
	 *            自己的位置
	 * @param selfAgentNickName
	 *            自己的agent nick name
	 * @return 可委托对象的agent nick name list，第一个为最佳的
	 */
	public static ArrayList<String> getDelegateToListBasedRanking(
			ArrayList<UserInformation> userInformations, String taskLocation,
			String selfAgentNickName) {

		ArrayList<String> ret = new ArrayList<>();

		if (taskLocation.equals("selfLocation")) {
			ret.add(selfAgentNickName + "#" + taskLocation);
		} else {

			// 获取与每个人的亲密度
			for (UserInformation userInformation : userInformations) {
				userInformation.setIntimacy(getIntimacy(selfAgentNickName,
						userInformation.getUserAgentNickname()));
			}

			Map<String, Double> distance = new HashMap<String, Double>();

			// 获取与所有好友距离的最大距离和最小距离
			double maxDis = 0, minDis = 0;

			if (!taskLocation.equals("null")) {// 执行任务时需要位置信息
				boolean isFirst = true;
				for (UserInformation userInformation : userInformations) {
					double dis = getShortDistance(
							getSpecificLocation(taskLocation),
							userInformation.getLocation());
					if (isFirst) {
						maxDis = dis;
						minDis = dis;
						isFirst = false;
					} else {
						if (maxDis < dis) {
							maxDis = dis;
						}
						if (minDis > dis) {
							minDis = dis;
						}
					}
				}
			}

			// 对位置距离进行归一化，然后算与所有好友的“距离”
			for (UserInformation userInformation : userInformations) {
				double locationDis = 0;
				if (!taskLocation.equals("null")) {// 执行任务时需要位置信息
					if (maxDis != minDis) {
						locationDis = (getShortDistance(
								getSpecificLocation(taskLocation),
								userInformation.getLocation()) / (maxDis - minDis));
					}
				}

				double dis = (locationDis + userInformation.getReputation() + userInformation
						.getIntimacy()) / 3;
				distance.put(userInformation.getUserAgentNickname(), dis);
			}

			List<Entry<String, Double>> sortList = new ArrayList<Entry<String, Double>>(
					distance.entrySet());
			Collections.sort(sortList,
					new Comparator<Entry<String, Double>>() {
						/**
						 * 按照距离从小到大排序
						 * 
						 * @param lhs
						 * @param rhs
						 * @return
						 */
						@Override
						public int compare(Entry<String, Double> lhs,
								Entry<String, Double> rhs) {
							if (lhs.getValue() == rhs.getValue()) {
								return 0;
							} else if (lhs.getValue() > rhs.getValue()) {
								return 1;
							} else {
								return -1;
							}
						}

					});

			for (Entry<String, Double> item : sortList) {
				ret.add(item.getKey() + "#" + taskLocation);
			}
		}
		// 返回排在第一个的
		return ret;
	}

	/**
	 * 根据位置中的经纬度计算出距离
	 * 
	 * @param location1
	 *            位置1，格式为Latitude:31.197595;Longitude:121.606087
	 * @param location2
	 *            位置2，格式为Latitude:31.197595;Longitude:121.606087
	 * @return 两个位置之间的距离
	 */
	private static double getShortDistance(String location1, String location2) {

		double lat1 = Double.parseDouble(location1.split(";")[0].split(":")[1]);
		double lon1 = Double.parseDouble(location1.split(";")[1].split(":")[1]);

		double lat2 = Double.parseDouble(location2.split(";")[0].split(":")[1]);
		double lon2 = Double.parseDouble(location2.split(";")[1].split(":")[1]);

		double a, b, R;
		R = 6378137; // 地球半径
		lat1 = lat1 * Math.PI / 180.0;
		lat2 = lat2 * Math.PI / 180.0;
		a = lat1 - lat2;
		b = (lon1 - lon2) * Math.PI / 180.0;
		double d;
		double sa2, sb2;
		sa2 = Math.sin(a / 2.0);
		sb2 = Math.sin(b / 2.0);
		d = 2
				* R
				* Math.asin(Math.sqrt(sa2 * sa2 + Math.cos(lat1)
						* Math.cos(lat2) * sb2 * sb2));
		return d;
	}

	/**
	 * 根据抽象地点（e.g. "图书馆"）返回具体的经纬度坐标位置
	 * 
	 * @param abstractlocation
	 *            抽象地点
	 * @return 具体的经纬度坐标位置
	 */
	private static String getSpecificLocation(String abstractlocation) {
		HashMap<String, String> locationMap = new HashMap<>();

		locationMap.put("Science Library",
				"Latitude:31.197595;Longitude:121.606087");// lib1在软件楼401
		locationMap.put("Computer Library",
				"Latitude:31.197006;Longitude:121.605141"); // lib2在二教自习室
		locationMap.put("Bookstore", "Latitude:31.197402;Longitude:121.606208");// bookstore在软件楼403
		locationMap.put("TeachingBuilding",
				"Latitude:31.197091;Longitude:121.604791");// seller1(YuHan)的位置，在二教走廊
		locationMap
				.put("MEBuilding", "Latitude:31.196523;Longitude:121.606115");// seller2(ChaiNing)的位置，在微电楼

		return locationMap.get(abstractlocation);
	}
}
