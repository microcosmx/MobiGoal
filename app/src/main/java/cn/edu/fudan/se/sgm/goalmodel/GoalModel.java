/**
 * 
 */
package cn.edu.fudan.se.sgm.goalmodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;

import cn.edu.fudan.se.mobigoal.utils.UserLog;
import cn.edu.fudan.se.sgm.goalmachine.ElementMachine;
import cn.edu.fudan.se.sgm.goalmachine.GoalMachine;


/**
 * 表示一个完整的goal model，用来把里面的<code>ElementMachine</code>组织起来
 * 
 * @author whh
 * 
 */
public class GoalModel implements Serializable{

	private String name; // goal model的名字
	private String description; // goal model的描述
	private String startTime;
	private String constantValue = null;

	/**
	 * goal model里面所有的ElementMachine
	 */
	private ArrayList<ElementMachine> elementMachines;
	private ArrayList<UserLog> userLogList;

	/**
	 * goal model的root goal，在初始化goal model的时候要设置，并且也要把它加到elementMachines中去。
	 */
	private GoalMachine rootGoal;

	private GoalModelManager goalModelManager;

	/**
	 * 对requestDate进行赋值时查询的表，manager在收到service回复时查询这个表 。key是element name
	 */
	private Hashtable<String, RequestData> assignmentHashtable;

	/**
	 * 参数表，manager在收到element请求服务时查询的表，如果有，说明element调用的服务需要传入参数，那么就把byte[]
	 * 字节流附加在信息中传递过去。key是element name,value是assignmentHashtable中的key
	 */
	private Hashtable<String, String> parameterMapHashtable;
	
	/**
	 * 将设备事件（定时器、点击按钮等）映射到goal machine的“外部事件”, key是MesBody_Mes2Manager.toString
	 */
	private Hashtable<String, EventBindingItem> deviceEventMapToExternalEventTable;

	public GoalModel() {
		this.elementMachines = new ArrayList<>();
		this.userLogList = new ArrayList<>();
		this.assignmentHashtable = new Hashtable<>();
		this.parameterMapHashtable = new Hashtable<>();
		this.deviceEventMapToExternalEventTable = new Hashtable<>();
	}

	/**
	 * 构造方法
	 * 
	 * @param name
	 *            goal model的名字，唯一
	 */
	public GoalModel(String name) {
		this.name = name;
		this.elementMachines = new ArrayList<>();
		this.userLogList = new ArrayList<>();
		this.assignmentHashtable = new Hashtable<>();
		this.parameterMapHashtable = new Hashtable<>();
		this.deviceEventMapToExternalEventTable = new Hashtable<>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * 为goal model添加一个element machine
	 * 
	 * @param elementMachine
	 *            要添加的element machine
	 */
	public void addElementMachine(ElementMachine elementMachine) {
		this.elementMachines.add(elementMachine);
		elementMachine.setGoalModel(this);
	}

	public ArrayList<ElementMachine> getElementMachines() {
		return this.elementMachines;
	}

	public GoalMachine getRootGoal() {
		return rootGoal;
	}

	public void setRootGoal(GoalMachine rootGoal) {
		this.rootGoal = rootGoal;
	}

	public GoalModelManager getGoalModelManager() {
		return goalModelManager;
	}

	public void setGoalModelManager(GoalModelManager goalModelManager) {
		this.goalModelManager = goalModelManager;
	}

	public Hashtable<String, RequestData> getAssignmentHashtable() {
		return assignmentHashtable;
	}

	public Hashtable<String, String> getParameterMapHashtable() {
		return parameterMapHashtable;
	}

	public Hashtable<String, EventBindingItem> getDeviceEventMapToExternalEventTable() {
		return deviceEventMapToExternalEventTable;
	}

	public ArrayList<UserLog> getUserLogList() {
		return userLogList;
	}
	
	public void addUserLog(UserLog userLog){
		this.userLogList.add(0,userLog);
	}
	
	public void clearUserLog(){
		this.userLogList.clear();
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getConstantValue() {
		return constantValue;
	}

	public void setConstantValue(String constantValue) {
		this.constantValue = constantValue;
	}

}
