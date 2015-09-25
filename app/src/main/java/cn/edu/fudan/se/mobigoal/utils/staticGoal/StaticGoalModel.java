/**
 * 
 */
package cn.edu.fudan.se.mobigoal.utils.staticGoal;

import java.util.ArrayList;

import cn.edu.fudan.se.mobigoal.utils.UserLog;


/**
 * @author whh
 *
 */
public class StaticGoalModel {
	
	private String name; // goal model的名字
	
	/**
	 * goal model里面所有的ElementMachine
	 */
	private ArrayList<StaticEM> staticEMs;

	/**
	 * goal model的root goal，在初始化goal model的时候要设置，并且也要把它加到elementMachines中去。
	 */
	private StaticGM rootGoal;
	
	private String startTime;
	private String constantValue = null;
	
	private ArrayList<UserLog> userLogList;
	
	public StaticGoalModel(String name, StaticGM rootGoal, String startTime){
		this.name = name;
		this.rootGoal = rootGoal;
		this.startTime = startTime;
		this.staticEMs = new ArrayList<>();
		this.userLogList = new ArrayList<>();
	}
	
	public void addStaticEM(StaticEM staticEM){
		this.staticEMs.add(staticEM);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public StaticGM getRootGoal() {
		return rootGoal;
	}

	public void setRootGoal(StaticGM rootGoal) {
		this.rootGoal = rootGoal;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public ArrayList<StaticEM> getStaticEMs() {
		return staticEMs;
	}

	public ArrayList<UserLog> getUserLogList() {
		return userLogList;
	}

	public void setUserLogList(ArrayList<UserLog> userLogList) {
		for (UserLog userLog: userLogList) {
			this.userLogList.add(userLog);
		}
//		this.userLogList = userLogList;
	}

	public String getConstantValue() {
		return constantValue;
	}

	public void setConstantValue(String constantValue) {
		this.constantValue = constantValue;
	}

}
