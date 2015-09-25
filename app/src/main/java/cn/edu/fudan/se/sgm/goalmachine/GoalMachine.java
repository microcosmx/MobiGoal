/**
 * 
 */
package cn.edu.fudan.se.sgm.goalmachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import cn.edu.fudan.se.sgm.goalmachine.message.MesBody_Mes2Machine;
import cn.edu.fudan.se.sgm.goalmachine.message.MesBody_Mes2Manager;
import cn.edu.fudan.se.sgm.goalmachine.message.MesHeader_Mes2Machine;
import cn.edu.fudan.se.sgm.goalmachine.message.MesHeader_Mes2Manger;
import cn.edu.fudan.se.sgm.goalmachine.message.SGMMessage;
import cn.edu.fudan.se.sgm.goalmachine.support.CauseToFailed;
import cn.edu.fudan.se.sgm.goalmachine.support.CauseToRepairing;
import cn.edu.fudan.se.sgm.goalmachine.support.RecordedState;
import cn.edu.fudan.se.sgm.log.Log;


/**
 * Goal Machine， 继承自<code>ElementMachine</code><br>
 * 它与<code>TaskMachine</code>的区别是， <code>TaskMachine</code>不能够再有subElement
 * 
 * @author whh
 * 
 */
public class GoalMachine extends ElementMachine {

	private ArrayList<ElementMachine> subElements = new ArrayList<ElementMachine>(); // subElements

	private int decomposition; // 分解，0表示AND分解，1表示OR分解
	private int schedulerMethod; // AND分解情况下的子目标执行顺序，0表示并行处理，1表示串行

	// private boolean isDelegated = false; // 任务是否是被委托的，也就是是否是别人委托给本机的，只有是root
	// // goal的时候需要设置
	// private String agentFrom; // 如果是别人委托过来的，需要设置委托来源
	// private String delegateGoalModelFrom; // 委托来源的goal
	// // model的名字，把结果发回去的时候需要找到原来的goal
	// // model

	// private boolean needDelegate; //
	// 任务是否要委托出去，也就是委托给别人做，如果是，则它应该是没有subElements的

	/**
	 * 目标状态机
	 * 
	 * @param name
	 *            目标状态机的名称
	 * @param decomposition
	 *            当前目标的分解方式，0表示AND分解，1表示OR分解
	 * @param schedulerMethod
	 *            AND分解情况下的子目标执行顺序，0表示并行处理，1表示串行，如果decomposition是OR分解，这个值无意义，
	 *            可以设置成-1
	 * @param parentGoal
	 *            当前目标的父目标，如果当前目标是root goal，这个值可以设置为null
	 */
	public GoalMachine(String name, int decomposition, int schedulerMethod,
			ElementMachine parentGoal, int level) {
		super(name, parentGoal, level);
		this.decomposition = decomposition;
		this.schedulerMethod = schedulerMethod;
		// this.needDelegate = needDelegate;
	}

	// ***********************************************
	// 下面的方法都是在各个状态下entry和do部分做的action
	// ***********************************************

	/**
	 * activated状态中entry所做的action：给所有subElement发ACTIVATE消息，让其激活
	 */
	@Override
	public void activatedEntry() {
		Log.logEMDebug(this.getName(), "activatedEntry()", "init.");

		// if (this.isNeedDelegate()) { // 是需要委托出去的，不需要给子目标发送激活消息，直接告诉父目标自己激活了
		//
		// if (this.sendMessageToParent(MesBody_Mes2Machine.ACTIVATEDDONE)) {
		// Log.logDebug(this.getName(), "activatedEntry()",
		// "send ACTIVATEDDONE msg to "
		// + this.getParentGoal().getName() + " succeed!");
		// } else {
		// Log.logError(this.getName(), "activatedEntry()",
		// "send ACTIVATEDDONE msg to "
		// + this.getParentGoal().getName() + " error!");
		// }
		//
		// } else { // 不是需要委托出去的

		if (this.getDecomposition() == 0) { // AND分解
			// 给所有子目标发送激活消息
			activatedEntry_sendMesToAllSub_AND();

		} else {// OR分解
				// 给下一个可以激活的子目标发送激活消息
			activatedEntryDo_sendMesToOneSub_OR();
		}
		// }

	}

	boolean isActivatedDo_waitingSubReplyDone = false;

	boolean isSendActivateMesToOneSubDone = false;
	boolean canStartAfterRepaired = false;

	/**
	 * 重写了<code>ElementMachine</code>中的activateDo()方法。<br>
	 * 做的action为：等待subElements反馈ACTIVATEDDONE消息中，如果都反馈已激活，进入等待父目标的START指令中。
	 * 如果是root goal，则在activatedDo_waitingSubReply()方法中已发生状态跳转
	 */
	@Override
	public void activateDo(SGMMessage msg) {
//		Log.logDebug(this.getName(), "activateDo()", "init.");
		// if (this.isNeedDelegate()) {// 是需要委托出去的，不需要等待子目标的反馈，直接等待父目标的start消息
		// if (msg != null) {
		// Log.logDebug(this.getName(), "activateDo()",
		// "get a message from " + msg.getSender().toString()
		// + "; body is: " + msg.getBody());
		//
		// // 消息内容是START，表示父目标让当前目标开始状态转换
		// if (msg.getBody().equals(MesBody_Mes2Machine.START)) {
		// this.getMsgPool().poll();
		// this.setCurrentState(this.transition(State.Activated,
		// this.getPreCondition()));
		// }
		// }
		// } else {
		if (isActivatedDo_waitingSubReplyDone) { // subElements都反馈已激活，进入等待父目标的START指令中。如果是root
													// goal，则在activatedDo_waitingSubReply()方法中已发生状态跳转

			// 如果是通过修复重新进入activate状态的，不用等待START命令，直接进入执行
			if (canStartAfterRepaired) {
				this.setCurrentState(this.transition(State.Activated,
						this.getPreCondition()));
			} else {
				if (msg != null) {
					Log.logEMDebug(this.getName(), "activateDo()",
							"get a message from " + msg.getGoalModelName()
									+ "#" + msg.getFromElementName()
									+ "; body is: " + msg.getBody());

					// 消息内容是START，表示父目标让当前目标开始状态转换
					if (msg.getBody().equals(MesBody_Mes2Machine.START)) {
						this.getMsgPool().poll();
						this.setCurrentState(this.transition(State.Activated,
								this.getPreCondition()));
					}

				}
			}

		} else { // 等待subElements反馈中
			if (this.getDecomposition() == 0) { // AND分解
				activatedDo_waitingSubReply_AND(msg);
			} else { // OR分解
				if (isSendActivateMesToOneSubDone) {
					activatedDo_waitingSubReply_OR(msg);
				} else {
					activatedEntryDo_sendMesToOneSub_OR();
				}
			}
		}
		// }
	}

	boolean isSendMesToOneSubDone = false;

	/**
	 * 重写了<code>ElementMachine</code>中的executingEntry()方法。<br>
	 * 做的action为：按照是否是AND分解以及是否是并行来决定给哪些subElements发START消息<br>
	 * 如果是AND、并行：给所有subElements发START消息；<br>
	 * 如果是AND、串行： 给一个不是achieved状态的subElement发START消息；<br>
	 * 如果是OR：按照优先级给其中已激活的subElements发送START消息
	 */
	@Override
	public void executingEntry() {
		Log.logEMDebug(this.getName(), "executingEntry()", "init.");

		// if (this.isNeedDelegate()) { // 这个goal是需要委托给别人做的
		// Log.logDebug(this.getName(), "executingEntry()",
		// "needDelegate is true!");
		//
		// // 发送消息给manager
		// // 怎么知道委托给谁做，也就是receiver的设置
		// SGMMessage msgToManager = new SGMMessage(
		// MesHeader_Mes2Manger.ELEMENT_MESSAGE, null, this
		// .getGoalModel().getName(), this.getName(), null,
		// this.getName(), this.getName(),
		// MesBody_Mes2Manager.DelegateOut);
		// sendMesToManager(msgToManager);
		//
		// } else {

		if (this.getDecomposition() == 0) { // AND分解
			if (this.getSchedulerMethod() == 0) { // 并行
				executingEntry_sendMesToAllSub_AND_PARALLERL();

			} else { // 串行
				executingEntryDo_sendMesToOneSub_AND_SERIAL();
			}
		} else { // OR分解
			executingEntryDo_sendMesToOneSub_OR();
		}
		// }
	}

	/**
	 * 重写了<code>ElementMachine</code>中的executingDo()方法。<br>
	 * 做的action为：按照是否是AND分解以及是否是并行来决定等待哪些subElements的反馈<br>
	 * 如果是AND、并行：等待所有subElements反馈消息ACHIEVED，必须是所有的子目标都反馈完成，
	 * 如果所有子目标都完成了，自己可以尝试发生跳转到achieved；<br>
	 * 如果是AND、串行：等待subElement反馈完成的消息，得到消息后，把它标记为achieved，
	 * 然后重新进入SendMesToOneSub_AND_SERIAL，给下个未完成状态的subElement发消息
	 * ，如此循环，直到最后一个subElement完成，可以尝试发生跳转；<br>
	 * 如果是OR：
	 */
	@Override
	public void executingDo(SGMMessage msg) {
//		Log.logDebug(this.getName(), "executingDo()", "init.");

		// if (this.isNeedDelegate()) { //
		// 这个goal是需要委托给别人做的，进入等待manager发送过来的任务是否完成的消息
		// Log.logDebug(this.getName(), "executingDo()",
		// "needDelegate is true!");
		// executingDo_waitingManagerReply(msg);
		//
		// } else {
		if (this.getDecomposition() == 0) { // AND分解
			if (this.getSchedulerMethod() == 0) { // 并行
				executingDo_waitingSubReply(msg);
			} else { // 串行
				if (isSendMesToOneSubDone) { // 已经给其中一个sub发过消息了，要进入等待反馈状态中
					executingDo_waitingSubReply(msg);
				} else { // 给下一个sub发消息
					executingEntryDo_sendMesToOneSub_AND_SERIAL();
				}
			}
		} else { // OR分解
			if (isSendMesToOneSubDone) { // 已经给其中一个已激活状态的sub发过消息了，进入等待反馈中
				executingDo_waitingSubReply(msg);
			} else {
				executingEntryDo_sendMesToOneSub_OR();
			}
		}
		// }

	}

	/**
	 * progressChecking状态中do所做的action：只要收到subElement发来的ACHIEVEDDONE消息就进入这个状态，
	 * 然后检查是不是符合自身进入achieved状态的条件，如果符合，跳转到achieved，如果不符合，跳回到executing状态。<br>
	 * AND分解检查条件：所有的subElements都achieved<br>
	 * OR分解检查条件：只要有一个subElement进入achieved
	 */
	@Override
	public void progressCheckingDo() {
		Log.logEMDebug(this.getName(), "progressCheckingDo()", "init.");

		if (this.getDecomposition() == 0) { // AND分解
			// 检查是否全部已完成
			int count = 0;
			for (ElementMachine element : this.getSubElements()) {
				if (element.getRecordedState() == RecordedState.Achieved) { // achieved
					count++;
				}
			}

			if (this.getSchedulerMethod() == 0) { // 并行
				if (count == this.getSubElements().size()) { // 全部激活,自己可以尝试发生跳转到achieved
					this.setCurrentState(this.transition(
							State.ProgressChecking, this.getPostCondition()));
				} else {
					this.setCurrentState(State.Executing); // 没有全部激活，继续跳回到executing
				}

			} else { // 串行
				if (count == this.getSubElements().size()) { // 全部激活,自己可以尝试发生跳转到achieved
					this.setCurrentState(this.transition(
							State.ProgressChecking, this.getPostCondition()));
				} else {
					isSendMesToOneSubDone = false; // 这样下次循环的时候就会再次去执行executingEntry_AND_SERIAL()
					this.setCurrentState(State.Executing); // 没有全部激活，继续跳回到executing
				}
			}

		} else { // OR分解，因为是收到了ACHIEVEDDONE消息才会进入progressChecking，所以肯定有一个subElement状态是achieved了
			this.setCurrentState(this.transition(State.ProgressChecking,
					this.getPostCondition()));
		}
	}

	/**
	 * suspended状态中entry所做的action：给所有subElements发送SUSPEND消息
	 */
	@Override
	public void suspendedEntry() {
		Log.logEMDebug(this.getName(), "suspendedEntry()", "init.");
		if (this.getSubElements() != null) {
			for (ElementMachine element : this.getSubElements()) {

				if (this.sendMessageToSub(element, MesBody_Mes2Machine.SUSPEND)) {
					Log.logEMDebug(this.getName(), "suspendedEntry()",
							"send SUSPEND msg to " + element.getName()
									+ " succeed!");
				} else {
					Log.logError(this.getName(), "suspendedEntry()",
							"send SUSPEND msg to " + element.getName()
									+ " error!");
				}
			}
		}
	}

	/**
	 * suspended状态中do所做的action：目标处于挂起状态时，只需要不断检查是否有RESUME到来即可，如果收到了，
	 * 给所有subElements发送RESUME消息，然后把自己状态转换为executing状态
	 */
	@Override
	public void suspendedDo(SGMMessage msg) {
//		Log.logDebug(this.getName(), "suspendedDo()", "init.");
		// SGMMessage msg = this.getMsgPool().poll(); // 每次拿出一条消息
		if (msg != null) {
			Log.logEMDebug(this.getName(), "suspendedDo()", "get a message from "
					+ msg.getGoalModelName() + "#" + msg.getFromElementName()
					+ "; body is: " + msg.getBody());
			if (msg.getBody().equals(MesBody_Mes2Machine.RESUME)) {
				this.getMsgPool().poll();
				// 给所有subElements发送RESUME消息
				if (this.getSubElements() != null) {
					for (ElementMachine element : this.getSubElements()) {

						if (this.sendMessageToSub(element,
								MesBody_Mes2Machine.RESUME)) {
							Log.logEMDebug(this.getName(), "suspendedEntry()",
									"send RESUME msg to " + element.getName()
											+ " succeed!");
						} else {
							Log.logError(this.getName(), "suspendedEntry()",
									"send RESUME msg to " + element.getName()
											+ " error!");
						}
					}
				}
				// 把自己状态设置为executing，同时resetSuspendEntry
				this.setCurrentState(State.Executing);
				resetSuspendEntry();
			}
		}
	}

	/**
	 * achieved状态中do所做的action：如果是root goal并且是别人委托进来的，就告诉委托方目标达成
	 */
	public void achievedDo() {
//		Log.logDebug(this.getName(), "achievedDo()", "init.");
		this.stopMachine(); // 本身已完成
		Log.logEMDebug(this.getName(), "achievedDo()",
				"It has achieved its goal and stopped its machine!");

		if (this.getParentGoal() == null) { // 是root goal
			// if (this.isDelegated()) { // 而且是被委托的
			// SGMMessage msgToManager = new SGMMessage(
			// MesHeader_Mes2Manger.ELEMENT_MESSAGE, null, this
			// .getGoalModel().getName(), this.getName(),
			// this.getAgentFrom(), this.getDelegateGoalModelFrom(),
			// this.getName(), MesBody_Mes2Manager.DelegatedAchieved);
			// msgToManager.setDescription("GoalModel-"
			// + this.getGoalModel().getName() + " is achieved!");
			// sendMesToManager(msgToManager);
			// } else { // 不是被委托的

			SGMMessage msgToManager = new SGMMessage(
					MesHeader_Mes2Manger.ELEMENT_MESSAGE, this.getGoalModel()
							.getName(), this.getName(), null,
					new MesBody_Mes2Manager("NoDelegatedAchieved"));
			// SGMMessage msgToManager = new SGMMessage(
			// MesHeader_Mes2Manger.ELEMENT_MESSAGE, null, this
			// .getGoalModel().getName(), this.getName(),
			// null, null, null,
			// MesBody_Mes2Manager.NoDelegatedAchieved);
//			msgToManager.setTaskDescription("GoalModel-"
//					+ this.getGoalModel().getName() + " is achieved!");
			sendMesToManager(msgToManager);
			// }
		}

	}

	/**
	 * failed状态中do所做的action：如果是root goal并且是别人委托进来的，就告诉委托方目标失败，
	 */
	public void failedDo() {
//		Log.logDebug(this.getName(), "failedDo()", "init.");
		this.stopMachine();
		Log.logEMDebug(this.getName(), "failedDo()",
				"It failed to achieved its goal and stopped its machine!");

		if (this.getParentGoal() == null) { // 是root goal
		// if (this.isDelegated()) { // 而且是被委托的
		// SGMMessage msgToManager = new SGMMessage(
		// MesHeader_Mes2Manger.ELEMENT_MESSAGE, null, this
		// .getGoalModel().getName(), this.getName(),
		// this.getAgentFrom(), this.getDelegateGoalModelFrom(),
		// this.getName(), MesBody_Mes2Manager.DelegatedFailed);
		// msgToManager.setDescription("GoalModel-"
		// + this.getGoalModel().getName() + " is failed!");
		// sendMesToManager(msgToManager);
		// } else { // 不是被委托的

			SGMMessage msgToManager = new SGMMessage(
					MesHeader_Mes2Manger.ELEMENT_MESSAGE, this.getGoalModel()
							.getName(), this.getName(), null,
					new MesBody_Mes2Manager("NoDelegatedFailed"));
			// SGMMessage msgToManager = new SGMMessage(
			// MesHeader_Mes2Manger.ELEMENT_MESSAGE, null, this
			// .getGoalModel().getName(), this.getName(),
			// null, null, null, MesBody_Mes2Manager.NoDelegatedFailed);
//			msgToManager.setTaskDescription("GoalModel-"
//					+ this.getGoalModel().getName() + " is failed!");
			sendMesToManager(msgToManager);
			// }
		}

	}

	/**
	 * AND分解</br> activated状态entry所做的action:检查子目标的上下文条件，如果满足，就给给子目标发送激活消息；
	 * 只要有一个子目标的上下文条件不满足，就直接stop状态机，
	 */
	private void activatedEntry_sendMesToAllSub_AND() {
		Log.logEMDebug(this.getName(), "activatedEntry_sendMesToAllSub_AND()",
				"init.");
		if (this.getSubElements() != null) {

			for (ElementMachine element : this.getSubElements()) {
				// 给所有子目标发送激活消息
				if (sendMessageToSub(element, MesBody_Mes2Machine.ACTIVATE)) { // 发送成功
					Log.logEMDebug(this.getName(),
							"activatedEntry_sendMesToAllSub_AND()",
							"send \"ACTIVATE\" msg to " + element.getName()
									+ " succeed!");
				} else {
					Log.logError(this.getName(),
							"activatedEntry_sendMesToAllSub_AND()",
							"send ACTIVATE msg to " + element.getName()
									+ " error!");
				}

			}
		} else {
			Log.logError(this.getName(),
					"activatedEntry_sendMesToAllSub_AND()",
					"getSubElements() == null");
		}
	}

	/**
	 * OR分解</br> activated状态entry所做的action:给所有子目标发送激活消息，然后开始等待子目标的反馈
	 */
	private void activatedEntryDo_sendMesToOneSub_OR() {
		Log.logEMDebug(this.getName(), "activatedEntryDo_sendMesToOneSub_OR()",
				"init.");

		if (this.getSubElements() != null) {
			int failedCount = 0;

			for (ElementMachine element : this.getSubElements()) {
				// 找到下一个处于initial状态的subElement，给其发送activate消息，然后break，跳出循环
				if (element.getRecordedState() == RecordedState.Initial) {

					if (sendMessageToSub(element, MesBody_Mes2Machine.ACTIVATE)) {
						Log.logEMDebug(this.getName(),
								"activatedEntryDo_sendMesToOneSub_OR()",
								"send ACTIVATE msg to " + element.getName()
										+ " succeed!");
						isSendActivateMesToOneSubDone = true;
						break;
					} else {
						Log.logError(this.getName(),
								"activatedEntryDo_sendMesToOneSub_OR()",
								"send ACTIVATE msg to " + element.getName()
										+ " error!");
					}
				} else {
					failedCount++;
				}

			}

			// 检查是不是所有的都激活失败了，如果是，自己直接进入failed状态
			if (failedCount == this.getSubElements().size()) {
				this.setCurrentState(State.Failed);
				this.setCauseToFailed(CauseToFailed.ActivatedFail);
			}
		} else {
			Log.logError(this.getName(),
					"activatedEntryDo_sendMesToOneSub_OR()",
					"getSubElements() == null.");
		}
	}

	/**
	 * AND分解</br>
	 * activated状态中do所做的action：检查消息队列，看subElement是否已激活，如果subElement已激活，
	 * 表示自己的激活全部完成，可告诉parent开始下一步行动了；这时，目标仍处于activated状态，等待父目标发送START指令。（root
	 * goal除外，root goal要直接发生状态跳转）
	 * 
	 * @param msg
	 */
	private void activatedDo_waitingSubReply_AND(SGMMessage msg) {
//		Log.logDebug(this.getName(), "activatedDo_waitingSubReply_AND()",
//				"init.");
		if (msg != null) {
			Log.logEMDebug(
					this.getName(),
					"activatedDo_waitingSubReply_AND()",
					"get a message from " + msg.getGoalModelName() + "#"
							+ msg.getFromElementName() + "; body is: "
							+ msg.getBody());
			// 消息内容是ACTIVATEDDONE，表示发送这条消息的子目标已激活
			if (msg.getBody().equals(MesBody_Mes2Machine.ACTIVATEDDONE)) {
				this.getMsgPool().poll();
				setSubElementRecordedState(msg.getFromElementName(),
						(MesBody_Mes2Machine) msg.getBody());
			} else if (msg.getBody()
					.equals(MesBody_Mes2Machine.ACTIVATEDFAILED)) { // 子目标反馈的是激活失败ACTIVATEDFAILED
				this.getMsgPool().poll();
				setSubElementRecordedState(msg.getFromElementName(),
						(MesBody_Mes2Machine) msg.getBody());
				// 只要收到了激活失败消息，自己就激活失败了
				this.setCurrentState(State.Repairing);
				this.setCauseToRepairing(CauseToRepairing.SubActivatedFail);
				return;
			}

			// 所有子目标都激活，可以进入下一步骤
			int count = 0;
			for (ElementMachine element : this.getSubElements()) {
				if (element.getRecordedState() == RecordedState.Activated) { // 激活
					count++;
				}
			}
			if (count == this.getSubElements().size()) {// 所有子目标都激活了
				if (this.getParentGoal() != null) { // 不是root goal
					if (this.sendMessageToParent(MesBody_Mes2Machine.ACTIVATEDDONE)) {
						Log.logEMDebug(this.getName(),
								"activatedDo_waitingSubReply_AND()",
								"send ACTIVATEDDONE msg to parent.");
						isActivatedDo_waitingSubReplyDone = true;
					} else {
						Log.logError(this.getName(),
								"activatedDo_waitingSubReply_AND()",
								"send ACTIVATEDDONE msg to parent error!");
					}
				} else {// 自己本身是root goal，无需等待父目标发送START指令，直接发生跳转
					this.setCurrentState(this.transition(State.Activated,
							this.getPreCondition()));
				}
			}
		}
	}

	/**
	 * OR分解</br> activated状态中do所做的action：只要收到激活成功的消息，就告诉父目标激活成功，进入等待START命令中；
	 * 如果收到了激活失败的消息， 重新发送激活消息给下一个目标。如果全部失败，告诉父目标失败
	 * 
	 * @param msg
	 */
	private void activatedDo_waitingSubReply_OR(SGMMessage msg) {
//		Log.logDebug(this.getName(), "activatedDo_waitingSubReply_OR()",
//				"init.");

		if (msg != null) {
			Log.logEMDebug(
					this.getName(),
					"activatedDo_waitingSubReply_OR()",
					"get a message from " + msg.getGoalModelName() + "#"
							+ msg.getFromElementName() + "; body is: "
							+ msg.getBody());

			// 消息内容是ACTIVATEDDONE，表示发送这条消息的子目标已激活
			if (msg.getBody().equals(MesBody_Mes2Machine.ACTIVATEDDONE)) {
				this.getMsgPool().poll();
				setSubElementRecordedState(msg.getFromElementName(),
						(MesBody_Mes2Machine) msg.getBody());

				// 告诉父目标激活成功
				if (this.getParentGoal() != null) { // 不是root goal
					if (this.sendMessageToParent(MesBody_Mes2Machine.ACTIVATEDDONE)) {
						Log.logEMDebug(this.getName(),
								"activatedDo_waitingSubReply_OR()",
								"send ACTIVATEDDONE msg to parent.");
					} else {
						Log.logError(this.getName(),
								"activatedDo_waitingSubReply_OR()",
								"send ACTIVATEDDONE msg to parent error!");
					}
				} else {// 自己本身是root goal，无需等待父目标发送START指令，直接发生跳转
					this.setCurrentState(this.transition(State.Activated,
							this.getPreCondition()));
				}

				isActivatedDo_waitingSubReplyDone = true;

			} else if (msg.getBody()
					.equals(MesBody_Mes2Machine.ACTIVATEDFAILED)) { // 子目标反馈的是激活失败ACTIVATEDFAILED
				this.getMsgPool().poll();
				setSubElementRecordedState(msg.getFromElementName(),
						(MesBody_Mes2Machine) msg.getBody());

				this.setCurrentState(State.Repairing);
				this.setCauseToRepairing(CauseToRepairing.SubActivatedFail);

				// 两个都设置成false，下次循环时会再次执行activatedEntryDo_sendMesToOneSub_OR();
				// isActivatedDo_waitingSubReplyDone = false;
				// isSendActivateMesToOneSubDone = false;
			}
		}
	}

	/**
	 * AND分解而且是并行<br>
	 * executing状态中entry所做的action：给所有subElement发START消息，让其开始进入执行Executing状态
	 */
	private void executingEntry_sendMesToAllSub_AND_PARALLERL() {
		Log.logEMDebug(this.getName(),
				"executingEntry_sendMesToAllSub_AND_PARALLERL()", "init.");

		if (getSubElements() != null) {
			for (ElementMachine element : getSubElements()) {

				if (sendMessageToSub(element, MesBody_Mes2Machine.START)) {
					Log.logEMDebug(this.getName(),
							"executingEntry_sendMesToAllSub_AND_PARALLERL()",
							"send START msg to " + element.getName()
									+ " succeed!");
				} else {
					Log.logError(this.getName(),
							"executingEntry_sendMesToAllSub_AND_PARALLERL()",
							"send START msg to " + element.getName()
									+ " error!");
				}

			}
		} else {
			Log.logError(this.getName(),
					"executingEntry_sendMesToAllSub_AND_PARALLERL()",
					"getSubElements() == null.");

		}
	}

	/**
	 * AND分解而且是串行<br>
	 * executing状态中entry所做的action：给subElements中一个状态不是已完成的element发消息
	 */
	private void executingEntryDo_sendMesToOneSub_AND_SERIAL() {
		Log.logEMDebug(this.getName(),
				"executingEntryDo_sendMesToOneSub_AND_SERIAL()", "init.");

		if (getSubElements() != null) {

			for (ElementMachine element : getSubElements()) {
				// 找到下一个还不是已完成状态的subElement，给其发送start消息，然后break，跳出循环
				if (element.getRecordedState() != RecordedState.Achieved) { // 5表示是完成状态

					if (sendMessageToSub(element, MesBody_Mes2Machine.START)) {
						Log.logEMDebug(
								this.getName(),
								"executingEntryDo_sendMesToOneSub_AND_SERIAL()",
								"send START msg to " + element.getName()
										+ " succeed!");
						break;
					} else {
						Log.logError(
								this.getName(),
								"executingEntryDo_sendMesToOneSub_AND_SERIAL()",
								"send START msg to " + element.getName()
										+ " error!");
					}

				}
			}
		} else {
			Log.logError(this.getName(),
					"executingEntryDo_sendMesToOneSub_AND_SERIAL()",
					"getSubElements() == null.");
		}

		isSendMesToOneSubDone = true;
	}

	/**
	 * OR分解<br>
	 * executing状态中entry所做的action：按照优先级给其中已激活的subElements发送START消息
	 */
	private void executingEntryDo_sendMesToOneSub_OR() {
		Log.logEMDebug(this.getName(), "executingEntryDo_sendMesToOneSub_OR()",
				"init.");
		if (getSubElements() != null) {
			for (ElementMachine element : getSubElements()) {
				// 找到下一个已激活状态的subElement，给其发送start消息，然后break，跳出循环
				if (element.getRecordedState() == RecordedState.Activated) {

					if (sendMessageToSub(element, MesBody_Mes2Machine.START)) {
						Log.logEMDebug(this.getName(),
								"executingEntryDo_sendMesToOneSub_OR()",
								"send START msg to " + element.getName()
										+ " succeed!");
						break;
					} else {
						Log.logError(this.getName(),
								"executingEntryDo_sendMesToOneSub_OR()",
								"send START msg to " + element.getName()
										+ " error!");
					}
				}
			}
		} else {
			Log.logError(this.getName(),
					"executingEntryDo_sendMesToOneSub_OR()",
					"getSubElements() == null.");
		}
		isSendMesToOneSubDone = true;
	}

	// /**
	// * 任务是被委托出去的，在executingDo做的就是等待manager发送过来任务是否完成的消息
	// *
	// * @param msg
	// */
	// private void executingDo_waitingManagerReply(SGMMessage msg) {
	// Log.logDebug(this.getName(), "executingDo_waitingManagerReply()",
	// "init.");
	// if (msg != null) {
	// Log.logDebug(this.getName(), "executingDo_waitingManagerReply()",
	// "get a message from " + msg.getSender() + "; body is: "
	// + msg.getBody());
	// // 如果manager发送过来的是ACHIEVEDDONE
	// if (msg.getBody().equals(MesBody_Mes2Machine.ACHIEVEDDONE)) {
	// this.getMsgPool().poll();
	// this.setCurrentState(State.Achieved);
	// } else if (msg.getBody().equals(MesBody_Mes2Machine.FAILED)) {
	// this.getMsgPool().poll();
	// // this.setCurrentState(State.Failed);
	// this.setCurrentState(State.Repairing);
	// this.setCauseToRepairing(CauseToRepairing.SubExecutingFail);
	// }
	//
	// }
	// }

	/**
	 * 包括所有情形：AND分解并行、AND分解串行、OR分解<br>
	 * executing状态中do所做的action：等待所有subElements反馈消息ACHIEVED，必须是所有的子目标都反馈完成，
	 * 如果所有子目标都完成了，自己可以尝试发生跳转到achieved；<br>
	 * 如果得到的反馈消息是FAILED，直接进入failed状态
	 */
	private void executingDo_waitingSubReply(SGMMessage msg) {
//		Log.logDebug(this.getName(),
//				"executingDo_waitingSubReply()", "init.");

		// SGMMessage msg = this.getMsgPool().poll(); // 拿出一条消息
		if (msg != null) {
			Log.logEMDebug(
					this.getName(),
					"executingDo_waitingSubReply()",
					"get a message from " + msg.getGoalModelName() + "#"
							+ msg.getFromElementName() + "; body is: "
							+ msg.getBody());
			// 如果子目标反馈的是ACHIEVED
			if (msg.getBody().equals(MesBody_Mes2Machine.ACHIEVEDDONE)) {
				this.getMsgPool().poll();
				setSubElementRecordedState(msg.getFromElementName(),
						(MesBody_Mes2Machine) msg.getBody());
				// 检查是否全部已完成
				this.setCurrentState(State.ProgressChecking);

			} else if (msg.getBody().equals(MesBody_Mes2Machine.FAILED)) {
				this.getMsgPool().poll();
				setSubElementRecordedState(msg.getFromElementName(),
						(MesBody_Mes2Machine) msg.getBody());
				// 进入修复状态，并且设置导致进入修复状态的原因
				this.setCurrentState(State.Repairing);
				this.setCauseToRepairing(CauseToRepairing.SubExecutingFail);
			} else if (msg.getBody()
					.equals(MesBody_Mes2Machine.ACTIVATEDFAILED)) {
				this.getMsgPool().poll();
				setSubElementRecordedState(msg.getFromElementName(),
						(MesBody_Mes2Machine) msg.getBody());
				// 进入修复状态，并且设置导致进入修复状态的原因
				this.setCurrentState(State.Repairing);
				this.setCauseToRepairing(CauseToRepairing.SubActivatedFail);
			}

		}

	}

	/**
	 * 停止运行当前machine：另外，要给所有subElements里面不是Failed或者Achieved状态的子目标发送STOP消息
	 */
	@Override
	public void stopMachine() {
		this.setFinish(true);

		if (this.getSubElements() != null) {
			// 给所有subElements里面不是Failed,ActivatedFailed状态或者Achieved状态，或者是没有记录RecordedState的目标发送STOP消息
			for (ElementMachine element : this.getSubElements()) {
				if ((element.getRecordedState() != RecordedState.Failed
						&& element.getRecordedState() != RecordedState.Achieved && element
						.getRecordedState() != RecordedState.ActivatedFailed)
						|| element.getRecordedState() == null) {
					if (sendMessageToSub(element, MesBody_Mes2Machine.STOP)) {
						Log.logEMDebug(this.getName(), "stopMachine()",
								"send STOP msg to " + element.getName()
										+ " succeed!");
					} else {
						Log.logError(this.getName(), "stopMachine()",
								"send STOP msg to " + element.getName()
										+ " error!");
					}
				}
			}
		} else {
			Log.logError(this.getName(), "stopMachine()",
					"getSubElements() == null!");
		}
	}

	/**
	 * 让GoalMachine重写，用来初始化里面的两个变量
	 */
	public void resetElementMachine() {
		isActivatedDo_waitingSubReplyDone = false;
		isSendMesToOneSubDone = false;
		isSendActivateMesToOneSubDone = false;
		canStartAfterRepaired = false;
		// isDelegated = false;
	}

	public void resetElementMachineExecuting() {

		isSendMesToOneSubDone = false;
	}

	/**
	 * 对SubActivatedFail和SubExecutingFail情况进行修复，需要根据是AND分解还是OR分解分别判断。<br>
	 * AND分解：直接进入failed<br>
	 * OR分解：如果subElements没有一个处于激活状态（全部是failed或者initial），说明没有可选的了，进入failed；否则，
	 * 进入executing状态，给下一个处于activated状态的subElement发Start消息
	 * 
	 * @return 返回修复后的状态，默认返回repairing状态
	 */
	@Override
	public State subFailRepairing(CauseToRepairing causeToRepairing) {
		Log.logEMDebug(this.getName(), "subFailRepairing()", "init.");
		
		//只要有一次是通过执行失败进入修复的，就不用再等待start命令的；如果全部是通过激活失败进入修复的，还需要等待start命令
		if (causeToRepairing.equals(CauseToRepairing.SubExecutingFail)) {
			canStartAfterRepaired = true;
		}

		if (this.getDecomposition() == 0) { // AND分解
			if (causeToRepairing.equals(CauseToRepairing.SubActivatedFail)) {
				this.setCauseToFailed(CauseToFailed.ActivatedFail);
			}
			return State.Failed;
		} else { // OR分解
			// 收到failed消息后，先检查是否所有的subElements都是failed或者ActivatedFailed，如果是说明没有可选的了，进入failed
			int count = 0;
			for (ElementMachine element : this.getSubElements()) {
				if (element.getRecordedState() == RecordedState.Failed
						|| element.getRecordedState() == RecordedState.ActivatedFailed) {
					count++;
				}
			}
			if (count == this.getSubElements().size()) { // 全部是全部是failed或者activatedFailed
				isSendMesToOneSubDone = true;
				if (causeToRepairing.equals(CauseToRepairing.SubActivatedFail)) {
					this.setCauseToFailed(CauseToFailed.ActivatedFail);
				}
				return State.Failed; // 进入failed状态
			} else {
				// 重新进入Activated，给下个处于初始化状态的subElement发送激活消息
				isSendMesToOneSubDone = false;
				isActivatedDo_waitingSubReplyDone = false;
				isSendActivateMesToOneSubDone = false;
//				isRepaired = true;

				return State.Activated;
			}
		}
		// return State.Repairing;
	}

	// ***********************************************
	// 结束各个状态下entry和do部分做的action声明
	// ***********************************************

	// *************一些辅助方法***************************

	/**
	 * 发送消息给subElement
	 * 
	 * @param sub
	 *            subElement
	 * @param body
	 *            消息body部分
	 * @return true 发送成功, false 发送失败
	 */
	private boolean sendMessageToSub(ElementMachine sub, SGMMessage.MesBody body) {
		SGMMessage msg = new SGMMessage(MesHeader_Mes2Machine.ToSub, this
				.getGoalModel().getName(), this.getName(), sub.getName(), body);

		// SGMMessage msg = new SGMMessage(MesHeader_Mes2Machine.ToSub, null,
		// null, this.getName(), null, null, sub.getName(), body);
		if (sub.getMsgPool().offer(msg)) {
			Log.logMessage(msg, true);
			return true;
		} else {
			Log.logMessage(msg, false);
			return false;
		}
	}

	/**
	 * 根据收到的来自subElement的消息内容来设定父目标所记录的子目标的状态
	 * 
	 * @param subElementName
	 *            subElement的名称，即sender部分
	 * @param message
	 *            来自subElement的消息的内容，即body部分
	 */
	private void setSubElementRecordedState(String subElementName,
			MesBody_Mes2Machine message) {
		for (ElementMachine sub : this.getSubElements()) {
			if (sub.getName().equals(subElementName)) {
				switch (message) {
				case ACTIVATEDDONE:
					sub.setRecordedState(RecordedState.Activated); // 子目标已激活
					break;
				case ACHIEVEDDONE:
					sub.setRecordedState(RecordedState.Achieved); // 子目标已完成
					break;
				case FAILED:
					sub.setRecordedState(RecordedState.Failed); // 子目标失败
					break;
				case STARTEXECUTING:
					sub.setRecordedState(RecordedState.Executing); // 子目标开始执行
					break;
				case ACTIVATEDFAILED:
					sub.setRecordedState(RecordedState.ActivatedFailed); // 子目标激活失败，也就是上下文不满足
					break;
				default:
					break;
				}
			}
		}

	}

	// *************结束一些辅助方法************************

	public int getDecomposition() {
		return decomposition;
	}

	public int getSchedulerMethod() {
		return schedulerMethod;
	}

	/**
	 * 为当前目标添加一个subElement
	 * 
	 * @param element
	 *            要添加的subElement
	 * @param priorityLevel
	 *            这个subElement在所有subElements中的优先级，数值越大优先级越高，优先级主要在OR分解中用到
	 */
	public void addSubElement(ElementMachine element, int priorityLevel) {

		element.setPriorityLevel(priorityLevel);
		this.subElements.add(element);
		// 根据优先级对subElements排序，按照优先级从大到小排序
		Collections.sort(this.subElements, new Comparator<ElementMachine>() {

			@Override
			public int compare(ElementMachine e1, ElementMachine e2) {
				if (e1.getPriorityLevel() < e2.getPriorityLevel()) {
					return 1;
				} else if (e1.getPriorityLevel() > e2.getPriorityLevel()) {
					return -1;
				} else {
					return 0;
				}
			}
		});
	}

	public ArrayList<ElementMachine> getSubElements() {
		return this.subElements;
	}



}
