/**
 * 
 */
package cn.edu.fudan.se.sgm.goalmachine;

import android.os.Looper;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cn.edu.fudan.se.sgm.goalmachine.message.MesBody_Mes2Machine;
import cn.edu.fudan.se.sgm.goalmachine.message.MesHeader_Mes2Machine;
import cn.edu.fudan.se.sgm.goalmachine.message.SGMMessage;
import cn.edu.fudan.se.sgm.goalmachine.support.CauseToFailed;
import cn.edu.fudan.se.sgm.goalmachine.support.CauseToRepairing;
import cn.edu.fudan.se.sgm.goalmachine.support.RecordedState;
import cn.edu.fudan.se.sgm.goalmodel.GoalModel;
import cn.edu.fudan.se.sgm.log.Log;


/**
 * 元素状态机，GoalMachine和TaskMachine继承此抽象类
 * 
 * @author whh
 * 
 */
public class ElementMachine implements Runnable,Serializable {

	private GoalModel goalModel; // 这个element machine所属于的goal model

	private int level; // 这个主要是在安卓界面显示目标树的时候用的，指这个element处在第几层，root goal为0层

	private String name; // element的名字
	private ElementMachine parentGoal; // 当前element的父目标，除root
										// goal外每个element都有parentGoal
	private String description; // 任务的描述，如果这个任务需要人来完成，它需要有一段自然语言的描述展示给人看

	private State currentState = State.Initial; // element目前所处的状态

	private BlockingQueue<SGMMessage> msgPool; // 消息队列，即消息池，可以直观理解为当前element的个人信箱

	private RecordedState recordedState = RecordedState.Initial; // 让父目标用来记录当前element的状态
	private int priorityLevel; // 让父目标用来记录当前element的优先级

	private Date startTime; // 当前element machine开始执行时的时间

	private Date startWaitingTime; // 开始等待的时间
	private int waitingTimeLimit; // 等待时间限制
	// private int retryTimes = 1;// 可重试次数，也就是一共可以执行的次数

	private boolean finish; // 标识当前machine是否运行结束，结束后run()里面的while循环将停止

	// element machine相关的各种条件，目前设定是都可以为null
	private Condition contextCondition;
	private Condition preCondition; // 可以为null
	private Condition postCondition; // 可以为null
	private Condition commitmentCondition; // 整个shouldDo符合状态里都有可能不满足的，要一直检查
	private Condition invariantCondition; // 整个shouldDo符合状态里都有可能不满足的，要一直检查

	private CauseToRepairing causeToRepairing;
	private CauseToFailed causeToFailed;

	/* 标记各种状态的entry动作是否完成 */
	boolean isInitialEntryDone = false;
	boolean isActivatedEntryDone = false;
	boolean isExecutingEntryDone = false;
	boolean isFailedEntryDone = false;
	boolean isAchievedEntryDone = false;
	boolean isWaitingEntryDone = false;
	boolean isSuspendedEntryDone = false;

	// int currentExecutingIndex = 0; // 当前进入执行的次数，

	/**
	 * 构造方法
	 * 
	 * @param name
	 *            当前element的名字
	 * @param parentGoal
	 *            当前element的父目标，如果当前目标是root goal，这个值可以设置为null
	 */
	public ElementMachine(String name, ElementMachine parentGoal, int level) {
		this.name = name;
		this.parentGoal = parentGoal;
		this.level = level;
		this.msgPool = new LinkedBlockingQueue<SGMMessage>();
	}

	@Override
	public void run() {
		Looper.prepare();
		this.setCurrentState(State.Initial); // 刚开始是目标状态是initial状态
		// this.setStartTime(new Date()); // 设置目标状态机开始运行时间为当前时间

		Log.logEMDebug(this.name, "run()", "ElementMachine start!");

		this.setFinish(false);
		while (!this.isFinish()) {
			// 如果contextCondition为空，表示没有设置上下文条件，这个goal是有意义的，可以检出消息，执行行为
			doMainRunningBehaviour();
			try {
				Thread.sleep(500);//0.5s
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Log.logEMDebug(this.getName(), "run()",
				"ElementMachine stop while cycling!");

	}

	/**
	 * 主体持续运行的行为，包括从消息池里检出消息，以及在对应的状态执行相应的行为
	 */
	private void doMainRunningBehaviour() {

		// 每次循环拿出一条消息，然后后面有不同的方法来处理这个消息，如果消息被处理了，再从消息队列中把这条消息移除，这样可以保证每条消息都经过了处理，不会漏掉
		SGMMessage msg = this.getMsgPool().peek();

		if (filterMessage(msg)) {
			// 过滤之后再拿出新的消息
			msg = this.getMsgPool().peek();
		}

		// 每次开始前都先检查是否有STOP消息到来
		if (checkIfStop(msg)) { // 有STOP消息
			this.stopMachine();
		} else {

			switch (this.getCurrentState()) {
			case Initial: // initial
				if (isInitialEntryDone) { // 如果完成了entry动作，循环执行do动作
					initialDo(msg);
				} else {
					initialEntry(); // entry动作只会执行一次
					isInitialEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
				}

				break;
			case Activated: // activated
				if (doCCandInvCChecking()) { // 检测到违反
					break;
				} else {

					if (isActivatedEntryDone) { // 自己激活后就等待父目标的Start指令
						activateDo(msg);
					} else { // 刚进入激活状态，尝试把自己状态转换为激活
						activatedEntry();
						isActivatedEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
						this.setStartTime(new Date());
					}
				}

				break;
			case Waiting:// waiting
				if (doCCandInvCChecking()) { // 检测到违反
					break;
				} else {

					if (isWaitingEntryDone) {
						waitingDo();
					} else {
						waitingEntry();
						isWaitingEntryDone = true;
					}
				}
				break;
			case Executing: // executing
				if (doCCandInvCChecking()) { // 检测到违反
					break;
				} else {

					if (isExecutingEntryDone) {
						// 先检查一下是否有SUSPEND消息到达
						if (checkIfSuspend(msg)) { // 需要挂起
							break;
						} else { // 不需要挂起
							executingDo(msg);
						}

					} else {
						executingEntry();
						// currentExecutingIndex++; //
						// 每次进入executing，代表了执行了一次，执行次数加一
						isExecutingEntryDone = true;
					}
				}

				break;
			case Suspended: // suspened
				if (doCCandInvCChecking()) { // 检测到违反
					break;
				} else {

					if (isSuspendedEntryDone) {
						suspendedDo(msg);
					} else {
						suspendedEntry();
						isSuspendedEntryDone = true;
					}
				}
				break;
			case Repairing: // repairing
				repairingDo();
				break;
			case ProgressChecking: // progressChecking
				progressCheckingDo();
				break;

			case Failed:// failed
				if (isFailedEntryDone) {
					failedDo();
				} else {
					failedEntry();
					isFailedEntryDone = true;
				}
				break;
			case Achieved:// achieved
				if (isAchievedEntryDone) {
					achievedDo();
				} else {
					achievedEntry();
					isAchievedEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
				}

				break;
			default:
				break;

			}
		}
	}

	// ***********************************************
	// 下面的方法都是在各个状态下entry和do部分做的action
	// ***********************************************

	/**
	 * initial状态中entry所做的action：<br>
	 * 只会执行一次
	 */
	public void initialEntry() {
		Log.logEMDebug(this.getName(), "initialEntry()", "init.");
		// this.setStartTime(new Date()); // setTimer
	}

	/**
	 * initial状态的do所做的action：监听消息池，看是否有ACTIVATE消息到达
	 */
	public void initialDo(SGMMessage msg) {
		// Log.logDebug(this.getName(), "initialDo()", "init.");
		// SGMMessage msg = this.getMsgPool().poll(); // 每次拿出一条消息
		if (msg != null) {
			Log.logEMDebug(
					this.name,
					"initialDo()",
					"get a msg from " + msg.getGoalModelName() + "#"
							+ msg.getFromElementName() + ", body is: "
							+ msg.getBody());

			// 收到消息后的行为处理
			if (msg.getBody().equals(MesBody_Mes2Machine.ACTIVATE)) {
				this.getMsgPool().poll();
				// this.setCurrentState(State.Activated);
				this.setCurrentState(this.transition(State.Initial,
						this.getContextCondition()));
			}
		}
	}

	/**
	 * activated状态中entry所做的action，子类需要重写
	 */
	public void activatedEntry() {

	}

	/**
	 * activated状态中do所做的action：自身不是root goal，所以要一直等待父目标的START指令，收到后才可以发生状态转换<br>
	 * <code>GoalMachine中需要重写</code>
	 */
	public void activateDo(SGMMessage msg) {

	}

	/**
	 * waiting状态中entry所做的action：告诉父目标自己进入了Waiting状态
	 */
	public void waitingEntry() {
		Log.logEMDebug(this.getName(), "waitingEntry()", "init.");
		// 设置waiting开始的时间
		this.setStartWaitingTime(new Date());
	}

	/**
	 * waiting状态中do所做的action：等待父目标的EXITWAITING消息，并且一直做checkPreCondition
	 */
	public void waitingDo() {
		// Log.logDebug(this.getName(), "waitingDo()", "init.");

		// 先判断等待时间是否超时
		Date nowTime = new Date();
		long waitTime = nowTime.getTime()
				- this.getStartWaitingTime().getTime(); // 得到的差值单位是毫秒
		// 单位是分钟
		if (waitTime <= (this.getWaitingTimeLimit() * 60 * 1000)) { // 没有超时
			// 然后再做条件检查，判断是否能够跳出waiting状态
			checkPreCondition();
			if (this.getPreCondition().isSatisfied()) {
				this.setCurrentState(State.Executing);
			}
		} else { // 超时了
			Log.logEMDebug(this.getName(), "waitingDo()",
					"Waiting Timeout!!!!!!");
			this.setCurrentState(State.Failed);
		}

	}

	/**
	 * executing状态中entry所做的action：<code>GoalMachine</code>和
	 * <code>TaskMachine</code>需要重写
	 */
	public void executingEntry() {
		Log.logEMDebug(this.getName(), "executingEntry()", "init.");

	}

	/**
	 * executing状态中do所做的action：需要重写
	 */
	public void executingDo(SGMMessage msg) {

	}

	/**
	 * 在executingDo()方法前面先调用的方法，用来检测是否有收到SUSPEND消息，如果收到了，就把状态跳转到Suspended
	 * 
	 * @return true 表示需要挂起，直接进入挂起状态，不能再执行下面的代码；false 不需要挂起
	 */
	private boolean checkIfSuspend(SGMMessage msg) {
		// Log.logDebug(this.getName(), "checkIfSuspend()", "init.");

		// 这里用peek()方法取消息，Retrieves, but does not
		// remove，这样就不会影响接下来的executingDo()方法中取消息了
		// SGMMessage msg = this.getMsgPool().peek(); // 每次拿出一条消息
		if (msg != null) {

			// 消息内容是SUSPEND，表示父目标让当前目标进入挂起
			if (msg.getBody().equals(MesBody_Mes2Machine.SUSPEND)) {
				this.getMsgPool().poll(); // 如果消息真的是SUSPEND，那么就把它拿出来
				Log.logEMDebug(this.getName(), "checkIfSuspend()",
						"get a message from " + msg.getGoalModelName() + "#"
								+ msg.getFromElementName() + "; body is: "
								+ msg.getBody());
				this.setCurrentState(State.Suspended);
				return true;
			}

		}
		return false;
	}

	/**
	 * suspended状态中entry所做的action：需要重写
	 */
	public void suspendedEntry() {

	}

	/**
	 * suspended状态中do所做的action：需要重写
	 */
	public void suspendedDo(SGMMessage msg) {

	}

	/**
	 * repairing状态中do所做的action：尝试从Repairing状态跳转到修复后的状态，
	 * 修复后的状态在transiton中会通过调用doRepairing(condition)得到
	 */
	public void repairingDo() {
		Log.logEMDebug(this.getName(), "repairingDo()", "init.");
		this.setCurrentState(this.transition(State.Repairing, null));
	}

	/**
	 * progressChecking状态中do所做的action：<code>GoalMachine</code>需要重写，
	 * <code>TaskMachine</code>不需要进行进度检查，不会进入到此状态
	 */
	public void progressCheckingDo() {

	}

	/**
	 * failed状态中entry所做的action：告诉父目标自己FAILED
	 */
	public void failedEntry() {
		Log.logEMDebug(this.getName(), "failedEntry()", "init.");
		// 告诉父目标自己failed
		if (this.getParentGoal() != null) {
			if (this.getCauseToFailed() == null) {
				if (sendMessageToParent(MesBody_Mes2Machine.FAILED)) {
					Log.logEMDebug(this.getName(), "failedEntry()",
							"send FAILED msg to parent succeed!");
				} else {
					Log.logError(this.getName(), "failedEntry()",
							"send FAILED msg to parent error!");
				}
			} else {
				switch (this.getCauseToFailed()) {
				case ActivatedFail:
					if (sendMessageToParent(MesBody_Mes2Machine.ACTIVATEDFAILED)) {
						Log.logEMDebug(this.getName(), "failedEntry()",
								"send ACTIVATEDFAILED msg to parent succeed!");
					} else {
						Log.logError(this.getName(), "failedEntry()",
								"send ACTIVATEDFAILED msg to parent error!");
					}
					break;

				default:
					if (sendMessageToParent(MesBody_Mes2Machine.FAILED)) {
						Log.logEMDebug(this.getName(), "failedEntry()",
								"send FAILED msg to parent succeed!");
					} else {
						Log.logError(this.getName(), "failedEntry()",
								"send FAILED msg to parent error!");
					}
					break;
				}
			}
		}
	}

	/**
	 * failed状态中do所做的action：停止自己的状态机，<code>GoalMachine</code>需要重写
	 */
	public void failedDo() {
		// Log.logDebug(this.getName(), "failedDo()", "init.");
		this.stopMachine();
		Log.logEMDebug(this.getName(), "failedDo()",
				"It failed to achieved its goal and stopped its machine!");
	}

	/**
	 * achieved状态中entry所做的action：给父目标发送ACHIEVEDDONE消息，然后标记自己完成；如果本身是root
	 * goal，就不用发送了，直接标记整个goal model完成
	 */
	public void achievedEntry() {
		Log.logEMDebug(this.getName(), "achievedEntry()", "init.");

		if (this.getParentGoal() != null) { // 不是root goal
			if (this.sendMessageToParent(MesBody_Mes2Machine.ACHIEVEDDONE)) {
				Log.logEMDebug(this.getName(), "achievedEntry()",
						"send ACHIEVEDDONE msg to parent succeed!");
			} else {
				Log.logError(this.getName(), "achievedEntry()",
						"send ACHIEVEDDONE msg to parent error!");
			}
		}
	}

	/**
	 * achieved状态中do所做的action：在<code>GoalMachine</code>中需要重写
	 */
	public void achievedDo() {
		// Log.logDebug(this.getName(), "achievedDo()", "init.");
		this.stopMachine(); // 本身已完成
		Log.logEMDebug(this.getName(), "achievedDo()",
				"It has achieved its goal and stopped its machine!");

	}

	// ***********************************************
	// 结束各个状态下entry和do部分做的action声明
	// ***********************************************

	// *************一些辅助方法***************************

	/**
	 * 检查是否有STOP消息到来
	 * 
	 * @return true 有STOP消息到来；false 没有STOP消息到来
	 */
	private boolean checkIfStop(SGMMessage msg) {
		if (msg != null) {

			// 消息内容是STOP，表示父目标让当前目标stop
			if (msg.getBody().equals(MesBody_Mes2Machine.STOP)) {
				this.getMsgPool().poll(); // 如果消息真的是STOP，那么就把它拿出来
				Log.logEMDebug(this.getName(), "checkIfStop()",
						"get a message from " + msg.getGoalModelName() + "#"
								+ msg.getFromElementName() + "; body is: "
								+ msg.getBody());
				// 收到STOP消息后把自己状态设置为stop
				this.setCurrentState(State.Stop);
				return true;
			}
		}
		return false;
	}

	/**
	 * 状态转换
	 * 
	 * @param currentState
	 *            目前所处状态
	 * @param condition
	 *            转换时要检查的条件
	 * @return 转换后的状态
	 */
	public State transition(State currentState, Condition condition) {

		State ret = currentState;

		switch (currentState) {

		case Initial:
			// 检查context condition
			if (condition == null) {
				ret = State.Activated;
			} else {
				if (condition.getType().equals("CONTEXT")) {
					checkContextCondition();
					if (condition.isSatisfied()) {
						ret = State.Activated;
					} else {
						// context condition不满足，直接failed
//						SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//						String logTime = df.format(new Date());
//						String logContent= "Condition: " + condition.getDescription() +" is violated. " + this.getDescription() + " failed.";
//						UserLog userLog = new UserLog(logTime, logContent);
						
						ret = State.Failed;
						this.setCauseToFailed(CauseToFailed.ActivatedFail);
					}
				}
			}
			break;

		case Activated: // 1(activated)

			// 检查pre condition
			if (condition == null) { // pre condition为空
				ret = State.Executing; // 直接跳转到Executing
			} else {
				// 先判断条件是不是pre condition，是的话执行检查然后根据结果进行跳转；如果不是，无意义，返回-1
				if (condition.getType().equals("PRE")) {

					checkPreCondition();
					if (condition.isSatisfied()) {
						ret = State.Executing; // pre condition满足，跳转到Executing
					} else {
						if (condition.isWaitable()) {// 不满足但是可以等待
							ret = State.Waiting;
						} else { // 不满足，不可以等待
							ret = State.Failed;
						}
					}
				}
			}
			break;

		case Executing: // (executing)
		case ProgressChecking: // progressChecking

			if (condition == null) { // post condition为空
				ret = State.Achieved; // 直接跳转到Achieved
			} else {
				// 先判断条件是不是post condition，是的话执行检查然后根据结果进行跳转；如果不是，无意义，返回-1
				if (condition.getType().equals("POST")) {
					checkPostCondition();
					if (condition.isSatisfied()) {
						ret = State.Achieved; // post condition满足，跳转到Achieved
					} else {
						ret = State.Repairing; // post condition不满足，跳转到Repairing
						this.setCauseToRepairing(CauseToRepairing.PostCondition);
					}
				}
			}
			break;

		case Repairing:
			ret = doRepairing(this.getCauseToRepairing());
			break;

		default:
			break;

		}
		return ret;
	}

	/**
	 * 根据不同的策略进行修复，修复成功后仍然是根据不同的策略跳转到不同的状态<br>
	 * 要有一张修复策略表对应
	 * 
	 * @param cause
	 *            导致进入修复的原因
	 * @return 修复结束后要跳转到的状态
	 */
	private State doRepairing(CauseToRepairing cause) {
		State retState = State.Repairing; // 默认返回repairing状态，以防出现异常修复失败
		switch (cause) {

		case PostCondition:
			// 检查已经执行的次数是否小于设定的可重试次数，如果小于，可重新进入执行，否则表示已经重试完毕，进入failed。
			// if (currentExecutingIndex < this.getRetryTimes()) {
			// retState = State.Executing;
			// resetExecuting();
			// } else {
			retState = State.Failed;
			// }
			break;
		case SubActivatedFail:
			retState = subFailRepairing(CauseToRepairing.SubActivatedFail);
			break;
		case SubExecutingFail:
			retState = subFailRepairing(CauseToRepairing.SubExecutingFail);
			break;
		}

		return retState;
	}

	/**
	 * 对SubActivatedFail和SubExecutingFail情况进行修复，<code>GoalMachine</code>需要重写；
	 * <code>TaskMachine</code>
	 * 不需要，因为Task不会收到SubActivatedFail或者SubExecutingFail消息
	 * 
	 * @return 默认返回repairing状态
	 */
	public State subFailRepairing(CauseToRepairing causeToRepairing) {
		return State.Repairing;
	}

	/**
	 * 在ShouldDo复合状态中队commitment condition和invariant
	 * condition进行检查，如果违反，跳到failed状态
	 * 
	 * @return true 检测到违反；false 没有检测到违反
	 */
	private boolean doCCandInvCChecking() {

		// 先判断commitment condition是否为null
		if (this.getCommitmentCondition() != null) {
			checkCommitmentCondition();
			if (!this.getCommitmentCondition().isSatisfied()) { // 检测到违反
				Log.logEMDebug(this.getName(), "doCCandInvCChecking()",
						"commitment condition violation is detected!!!");
				this.setCurrentState(State.Failed);
				return true;
			}
		}

		// 先判断invariant condition是否为null
		if (this.getInvariantCondition() != null) {
			checkInvariantCondition();
			if (!this.getInvariantCondition().isSatisfied()) { // 检测到违反
				Log.logEMDebug(this.getName(), "doCCandInvCChecking()",
						"invariant condition violation is detected!!!");
				this.setCurrentState(State.Failed);
				return true;
			}
		}
		return false;
	}

	/**
	 * 发送消息给parentGoal
	 * 
	 * @param body
	 *            消息的body部分
	 * @return true 发送成功, false 发送失败
	 */
	public boolean sendMessageToParent(SGMMessage.MesBody body) {
		SGMMessage msg = new SGMMessage(MesHeader_Mes2Machine.ToParent, this
				.getGoalModel().getName(), this.getName(), this.getParentGoal()
				.getName(), body);
		// SGMMessage msg = new SGMMessage(MesHeader_Mes2Machine.ToParent, null,
		// null, this.getName(), null, null, this.getParentGoal()
		// .getName(), body);
		if (this.getParentGoal().getMsgPool().offer(msg)) {
			// 发送成功
			Log.logMessage(msg, true);
			return true;
		} else {
			Log.logMessage(msg, false);
			return false;
		}

	}

	/**
	 * 发送消息给manager
	 * 
	 * @param msg
	 *            要发送的消息
	 */
	public void sendMesToManager(SGMMessage msg) {
		Log.logEMDebug(this.getName(), "sendMesToManager()", "init.");

		if (this.getGoalModel().getGoalModelManager().getMsgPool().offer(msg)) {
			Log.logMessage(msg, true);
			Log.logEMDebug(this.getName(), "sendMesToManager()", "send a "
					+ msg.getBody() + " message to Manager succeed!");
		} else {
			Log.logMessage(msg, false);
			Log.logError(this.getName(), "sendMesToManager()",
					"send a " + msg.getBody() + " message to Manager error!");
		}
	}

	/**
	 * 停止运行当前machine：<code>GoalMachine需要重写</code>
	 */
	public void stopMachine() {
		this.setFinish(true);

		Log.logEMDebug(this.getName(), "stopMachine()",
				"It begins to stop its machine");
	}

	/**
	 * 重新设置当前machine，即把状态设置为初始化状态
	 */
	public void resetMachine() {

		this.setCurrentState(State.Initial);
		// reset之后要把这些变量再次全部初始化，不然thread开始的时候会直接跳过entry动作
		isInitialEntryDone = false;
		isActivatedEntryDone = false;
		isExecutingEntryDone = false;
		isFailedEntryDone = false;
		isAchievedEntryDone = false;
		isWaitingEntryDone = false;
		isSuspendedEntryDone = false;

		recordedState = RecordedState.Initial; // 让父目标用来记录当前element的状态

		// 把GoalMachine、TaskMachine里面的变量也设置为初始化
		resetElementMachine();
	}

	/**
	 * 让GoalMachine和TaskMachine重写，用来初始化里面的两个变量
	 */
	public void resetElementMachine() {

	}

	// public void resetExecuting() {
	// isExecutingEntryDone = false;
	// resetElementMachineExecuting();
	// }
	//
	// /**
	// * 让GoalMachine和TaskMachine重写，用来初始化里面的两个变量
	// */
	// public void resetElementMachineExecuting() {
	//
	// }

	/**
	 * 在suspended状态收到RESUME消息后要重新把isSuspendedEntryDone设置为false，以防再次进入suspended状态
	 */
	public void resetSuspendEntry() {
		isSuspendedEntryDone = false;
	}

	/**
	 * 消息过滤器，以防在非executing状态收到了suspend消息，这个时候要把这个消息从消息队列中丢弃，不然会影响接收后面的消息
	 * 
	 * @return true表示过滤掉了一条信息，需要重新从消息队列里拿出消息；false表示没有过滤信息
	 */
	private boolean filterMessage(SGMMessage msg) {
		if (msg != null) {
			if (msg.getBody().equals(MesBody_Mes2Machine.SUSPEND)
					&& (this.getCurrentState() != State.Executing)) {
				this.getMsgPool().poll(); // 把它拿出来
				Log.logEMDebug(this.getName(), "filterMessage()",
						"filter a SUSPEND msg!");
				return true;
			}
			if (msg.getBody().equals(MesBody_Mes2Machine.RESUME)
					&& (this.getCurrentState() != State.Suspended)) {
				this.getMsgPool().poll(); // 把它拿出来
				Log.logEMDebug(this.getName(), "filterMessage()",
						"filter a RESUME msg!");
				return true;
			}
			if (msg.getBody().equals(MesBody_Mes2Machine.ACTIVATEDDONE)
					&& (this.getCurrentState() != State.Activated)) {
				this.getMsgPool().poll(); // 把它拿出来
				Log.logEMDebug(this.getName(), "filterMessage()",
						"filter a ACTIVATEDDONE msg!");
				return true;
			}
		}
		return false;

	}

	// /**
	// * 检查default pre condition，<code>GoalMachine</code>需要重写，
	// * <code>TaskMachine</code>不需要重写
	// */
	// public void checkDefaultPreConditon() {
	//
	// }

	// *************结束一些辅助方法************************

	// *************checkCondition方法**************

	private void checkContextCondition() {
		this.getContextCondition().check();
	}

	private void checkPreCondition() {
		this.getPreCondition().check();
	}

	private void checkPostCondition() {
		this.getPostCondition().check();
	}

	private void checkCommitmentCondition() {
		// 第一次检查承诺条件的时候还没有设置startTime
		if (this.getStartTime() == null) {
			this.getCommitmentCondition().setSatisfied(true);
			return;
		}

		// 单位是分钟
		long timeLimit = Long.parseLong(this.getCommitmentCondition()
				.getRightValue());

		// 判断执行时间是否超时
		Date nowTime = new Date();
		long executingTime = nowTime.getTime() - this.getStartTime().getTime(); // 得到的差值单位是毫秒

		if (executingTime > (timeLimit * 60 * 1000)) { // 超时
			Log.logEMDebug(this.getName(), "checkCommitmentCondition()",
					"Waiting Timeout!!!!!!");
			this.getCommitmentCondition().setSatisfied(false);
		} else { // 没有超时
			this.getCommitmentCondition().setSatisfied(true);
		}
	}

	private void checkInvariantCondition() {
		this.getInvariantCondition().check();
	}

	// *************结束checkCondition方法**********

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public State getCurrentState() {
		return currentState;
	}

	public void setCurrentState(State currentState) {
		this.currentState = currentState;
	}

	public BlockingQueue<SGMMessage> getMsgPool() {
		return msgPool;
	}

	public void setMsgPool(BlockingQueue<SGMMessage> msgPool) {
		this.msgPool = msgPool;
	}

	public RecordedState getRecordedState() {
		return recordedState;
	}

	public void setRecordedState(RecordedState recordedState) {
		this.recordedState = recordedState;
	}

	public int getPriorityLevel() {
		return priorityLevel;
	}

	public void setPriorityLevel(int priorityLevel) {
		this.priorityLevel = priorityLevel;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getStartWaitingTime() {
		return startWaitingTime;
	}

	public void setStartWaitingTime(Date startWaitingTime) {
		this.startWaitingTime = startWaitingTime;
	}

	public int getWaitingTimeLimit() {
		return waitingTimeLimit;
	}

	public void setWaitingTimeLimit(int waitingTimeLimit) {
		this.waitingTimeLimit = waitingTimeLimit;
	}

	public boolean isFinish() {
		return finish;
	}

	public void setFinish(boolean finish) {
		this.finish = finish;
	}

	public ElementMachine getParentGoal() {
		return parentGoal;
	}

	public void setParentGoal(ElementMachine parentGoal) {
		this.parentGoal = parentGoal;
	}

	public Condition getContextCondition() {
		return contextCondition;
	}

	public void setContextCondition(Condition contextCondition) {
		this.contextCondition = contextCondition;
	}

	public Condition getPreCondition() {
		return preCondition;
	}

	public void setPreCondition(Condition preCondition) {
		this.preCondition = preCondition;
	}

	public Condition getPostCondition() {
		return postCondition;
	}

	public void setPostCondition(Condition postCondition) {
		this.postCondition = postCondition;
	}

	public Condition getCommitmentCondition() {
		return commitmentCondition;
	}

	public void setCommitmentCondition(Condition commitmentCondition) {
		this.commitmentCondition = commitmentCondition;
	}

	public Condition getInvariantCondition() {
		return invariantCondition;
	}

	public void setInvariantCondition(Condition invariantCondition) {
		this.invariantCondition = invariantCondition;
	}

	public CauseToRepairing getCauseToRepairing() {
		return causeToRepairing;
	}

	public void setCauseToRepairing(CauseToRepairing causeToRepairing) {
		this.causeToRepairing = causeToRepairing;
	}

	public CauseToFailed getCauseToFailed() {
		return causeToFailed;
	}

	public void setCauseToFailed(CauseToFailed causeToFailed) {
		this.causeToFailed = causeToFailed;
	}

	public GoalModel getGoalModel() {
		return goalModel;
	}

	public void setGoalModel(GoalModel goalModel) {
		this.goalModel = goalModel;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	// public int getRetryTimes() {
	// return retryTimes;
	// }
	//
	// public void setRetryTimes(int retryTimes) {
	// this.retryTimes = retryTimes;
	// }

}
