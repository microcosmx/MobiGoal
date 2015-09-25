/**
 * 
 */
package cn.edu.fudan.se.sgm.goalmachine;


import cn.edu.fudan.se.sgm.goalmachine.message.MesBody_Mes2Machine;
import cn.edu.fudan.se.sgm.goalmachine.message.MesBody_Mes2Manager;
import cn.edu.fudan.se.sgm.goalmachine.message.MesHeader_Mes2Manger;
import cn.edu.fudan.se.sgm.goalmachine.message.SGMMessage;
import cn.edu.fudan.se.sgm.log.Log;

/**
 * Task Machine，继承自<code>ElementMachine</code>
 * 
 * @author whh
 * 
 */
public class TaskMachine extends ElementMachine {

	// private boolean needPeopleInteraction; // 是否需要人的交互
	private String executingAbstractServiceName; // 执行这个task时具体需要调用的服务名称，如果需要人的交互，这个就为空了
	private String executingLocation;	//执行任务时的地点
	

	// private boolean needDelegate; //
	// 任务是否要委托出去，也就是委托给别人做，如果是，则它应该是没有subElements的

	/**
	 * 构造方法
	 * 
	 * @param name
	 *            task machine名字
	 * @param parentGoal
	 *            父目标
	 * @param level
	 *            显示层级
	 * @param executingAbstractServiceName
	 *            执行时需要查找的服务
	 */
	public TaskMachine(String name, ElementMachine parentGoal, int level,
			String executingAbstractServiceName) {
		super(name, parentGoal, level);
		this.executingAbstractServiceName = executingAbstractServiceName;
		this.executingLocation = null;
	}

	/**
	 * activated状态中entry所做的action：在initialDo中已经尝试把自己状态转换为Activated了，进入这个状态后，
	 * 说明已经被激活了，于是向父目标发送ACTIVATEDDONE消息，然后进入activatedDo()方法中，等待父进程的START消息
	 */
	@Override
	public void activatedEntry() {
		Log.logEMDebug(this.getName(), "activatedEntry()", "init.");

		if (this.sendMessageToParent(MesBody_Mes2Machine.ACTIVATEDDONE)) {
			Log.logEMDebug(this.getName(), "activatedEntry()",
					"send ACTIVATEDDONE msg to "
							+ this.getParentGoal().getName() + " succeed!");
		} else {
			Log.logError(this.getName(), "activatedEntry()",
					"send ACTIVATEDDONE msg to "
							+ this.getParentGoal().getName() + " error!");
		}

	}

	/**
	 * activated状态中do所做的action：自身不是root goal，所以要一直等待父目标的START指令，收到后才可以发生状态转换<br>
	 * <code>GoalMachine中需要重写</code>
	 */
	@Override
	public void activateDo(SGMMessage msg) {
//		Log.logDebug(this.getName(), "activateDo()", "init.");

		// SGMMessage msg = this.getMsgPool().poll(); // 每次拿出一条消息
		if (msg != null) {
			Log.logEMDebug(this.getName(), "activateDo()", "get a message from "
					+ msg.getGoalModelName() + "#" + msg.getFromElementName()
					+ "; body is: " + msg.getBody());

			// 消息内容是START，表示父目标让当前目标开始状态转换
			if (msg.getBody().equals(MesBody_Mes2Machine.START)) {
				this.getMsgPool().poll();
				this.setCurrentState(this.transition(State.Activated,
						this.getPreCondition()));
			}

		}
	}

	/**
	 * executing状态中entry所做的action：给manager发送消息，消息内容是需要人的参与或者是调用服务
	 */
	public void executingEntry() {
		Log.logEMDebug(this.getName(), "executingEntry()", "init.");

		SGMMessage msgToManager = new SGMMessage(
				MesHeader_Mes2Manger.ELEMENT_MESSAGE, this.getGoalModel()
						.getName(), this.getName(), null,
				new MesBody_Mes2Manager("RequestService"));
		msgToManager.setAbstractServiceName(this.getExecutingAbstractServiceName());
		if (this.getExecutingLocation()!=null) {
			msgToManager.setTaskLocation(this.getExecutingLocation());
		}
		msgToManager.setTaskDescription(this.getDescription());

		sendMesToManager(msgToManager);

	}

	/**
	 * executing状态中do所做的action：等待TASK_END的消息到达
	 */
	@Override
	public void executingDo(SGMMessage msg) {
//		Log.logDebug(this.getName(), "executingDo()", "init.");

		if (msg != null) {
			Log.logEMDebug(
					this.getName(),
					"executingDo_waitingEnd()",
					"get a message from " + msg.getGoalModelName() + "#"
							+ msg.getFromElementName() + "; body is: "
							+ msg.getBody());

			if (msg.getBody().equals(MesBody_Mes2Machine.TASK_DONE)) { // 收到外部UI的END消息
				this.getMsgPool().poll();
				this.setCurrentState(this.transition(State.Executing,
						this.getPostCondition()));
			} else if (msg.getBody().equals(MesBody_Mes2Machine.SUSPEND)) { // 收到父目标的SUSPEND消息
				this.getMsgPool().poll();
				this.setCurrentState(State.Suspended);
			} else if (msg.getBody().equals(MesBody_Mes2Machine.TASK_FAILED)) { // 用户没有完成这个任务，放弃了
				this.getMsgPool().poll();
				this.setCurrentState(State.Failed);
			}
		}

	}

	/**
	 * suspended状态中do所做的action：目标处于挂起状态时，只需要不断检查是否有RESUME到来即可，如果收到了，
	 * 把目标状态转换为executing状态
	 */
	@Override
	public void suspendedDo(SGMMessage msg) {
		Log.logEMDebug(this.getName(), "suspendedDo()", "init.");
		// SGMMessage msg = this.getMsgPool().poll(); // 每次拿出一条消息
		if (msg != null) {
			Log.logEMDebug(this.getName(), "suspendedDo()", "get a message from "
					+ msg.getGoalModelName() + "#" + msg.getFromElementName()
					+ "; body is: " + msg.getBody());
			if (msg.getBody().equals(MesBody_Mes2Machine.RESUME)) {
				this.getMsgPool().poll();
				// 把自己状态设置为executing,同时resetSuspendEntry
				this.setCurrentState(State.Executing);
				resetSuspendEntry();
			}
		}
	}

	/**
	 * 让TaskMachine重写，用来初始化里面的一个变量
	 */
	public void resetElementMachine() {
	}

	// public boolean isNeedPeopleInteraction() {
	// return needPeopleInteraction;
	// }
	//
	// public void setNeedPeopleInteraction(boolean needPeopleInteraction) {
	// this.needPeopleInteraction = needPeopleInteraction;
	// }

	public String getExecutingAbstractServiceName() {
		return executingAbstractServiceName;
	}

	public void setExecutingAbstractServiceName(
			String executingAbstractServiceName) {
		this.executingAbstractServiceName = executingAbstractServiceName;
	}

	public String getExecutingLocation() {
		return executingLocation;
	}

	public void setExecutingLocation(String executingLocation) {
		this.executingLocation = executingLocation;
	}

}
