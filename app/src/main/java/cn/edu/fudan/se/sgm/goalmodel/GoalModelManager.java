package cn.edu.fudan.se.sgm.goalmodel;


import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cn.edu.fudan.se.agent.main.AideAgentInterface;
import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.utils.staticGoal.StaticEM;
import cn.edu.fudan.se.mobigoal.utils.staticGoal.StaticGM;
import cn.edu.fudan.se.mobigoal.utils.staticGoal.StaticGoalModel;
import cn.edu.fudan.se.sgm.goalmachine.ElementMachine;
import cn.edu.fudan.se.sgm.goalmachine.TaskMachine;
import cn.edu.fudan.se.sgm.goalmachine.message.MesBody_Mes2Machine;
import cn.edu.fudan.se.sgm.goalmachine.message.MesBody_Mes2Manager;
import cn.edu.fudan.se.sgm.goalmachine.message.MesHeader_Mes2Manger;
import cn.edu.fudan.se.sgm.goalmachine.message.SGMMessage;
import cn.edu.fudan.se.sgm.log.Log;
import jade.core.MicroRuntime;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

public class GoalModelManager implements Runnable,Serializable {

	private BlockingQueue<SGMMessage> msgPool; // 消息池

	/**
	 * 所有目标模型列表，key是goal model name
	 */
	private Hashtable<String, GoalModel> goalModelList;
	private String agentNickname;

	private SGMApplication application;

	public GoalModelManager(SGMApplication application) {
		this.msgPool = new LinkedBlockingQueue<SGMMessage>();
		this.goalModelList = new Hashtable<>();
		this.application = application;
	}

	@Override
	public void run() {

		while (true) {
			// 处理消息
			SGMMessage msg = this.getMsgPool().peek(); // peek是拿出来看看，但是没有从消息队列中remove

			if (msg != null) {

				msg = this.getMsgPool().poll(); // poll是remove

				switch ((MesHeader_Mes2Manger) msg.getHeader()) {

				case LOCAL_AGENT_MESSAGE: // 本地agent发来的消息，也就是通过UI操作发给agent然后agent又转发的消息
					handleLocalAgentMessage(msg);
					break;

				// case EXTERNAL_AGENT_MESSAGE: //
				// 外部agent发来的消息，也是就外部agent发来消息然后本地agent转发的消息
				// handleExternalAgentMessage(msg);
				// break;

				case ELEMENT_MESSAGE: // 本地element machine发来的消息
					handleElementMessage(msg);
					break;

				default:
					break;
				}

			}

			try {
				Thread.sleep(2 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * 本地agent发来的消息，也就是通过UI操作发给agent然后agent又转发的消息
	 * 
	 * @param msg
	 *            消息内容
	 */
	private void handleLocalAgentMessage(SGMMessage msg) {
		Log.logGMMDebug("handleLocalAgentMessage()", "init, msg body is: "
                + msg.getBody().toString());

		// Messager receiver = msg.getReceiver();
		// String targetTaskName = receiver.getElementName();
		// String targetGoalModelName = receiver.getGoalModelName();

		String targetGoalModelName = msg.getGoalModelName();
		String targetElementName = msg.getToElementName();

		/* 先找出注册了MesBody_Mes2Manager的goal model */
		ArrayList<GoalModel> relateGoalModels = new ArrayList<>();
		for (GoalModel goalModel : goalModelList.values()) {
			if (goalModel.getDeviceEventMapToExternalEventTable().containsKey(
					msg.getBody().toString())) {
				relateGoalModels.add(goalModel);
				android.util.Log.i("MY_LOG",
						"---handleLocalAgentMessage() relateGoalModel: "
								+ goalModel.getName() + "; msgBody: "
								+ msg.getBody().toString());
			}
		}

		/*
		 * 然后再找出target goal model list，如果targetGoalModelName不为空，那么这个列表里只有一个goal
		 * model
		 */
		ArrayList<GoalModel> targetGoalModels = new ArrayList<>();
		if (targetGoalModelName != null) {
			targetGoalModels.add(goalModelList.get(targetGoalModelName));
		} else {
			targetGoalModels.addAll(relateGoalModels);
		}

		/* 对所有targetGoalModels执行他们自己对应的externalEvent的对应的操作 */
		SGMMessage originalSgmMessage = copyMessage(msg);

		for (GoalModel targetGoalModel : targetGoalModels) {

			ElementMachine targetElementMachine = getEMFromGoalModelByName(
					targetGoalModel, targetElementName);
			android.util.Log.i("MY_LOG",
					"---handleLocalAgentMessage() targetElementMachine is null? "
							+ (targetElementMachine == null));
			if (targetElementMachine == null) {
				targetElementMachine = getEMFromGoalModelByName(
						targetGoalModel,
						targetGoalModel.getDeviceEventMapToExternalEventTable()
								.get(originalSgmMessage.getBody().toString())
								.getElementName());
				android.util.Log.i("MY_LOG",
						"---handleLocalAgentMessage() targetElementMachine is null? 222:"
								+ (targetElementMachine == null));
			}

			android.util.Log.i(
					"MY_LOG",
					"---handleLocalAgentMessage() ExternalEvent:"
							+ targetGoalModel
									.getDeviceEventMapToExternalEventTable()
									.get(originalSgmMessage.getBody()
											.toString()).getExternalEvent());
			switch (targetGoalModel.getDeviceEventMapToExternalEventTable()
					.get(originalSgmMessage.getBody().toString())
					.getExternalEvent()) {
			case startGM:
				start(targetGoalModel, originalSgmMessage);
				break;
			case stopGM:
				stop(targetGoalModel, originalSgmMessage);
				break;
			case suspendGM:
				suspend(targetGoalModel, originalSgmMessage);
				break;
			case resumeGM:
				resume(targetGoalModel, originalSgmMessage);
				break;
			case resetGM:
				reset(targetGoalModel);
				break;
			case quitTE:
			case serviceExecutingFailed:
				quitTaskMachine((TaskMachine) targetElementMachine,
						originalSgmMessage);
				break;

			case endTE:
			case serviceExecutingDone:
				// 看返回的信息中是否需要给某些数据赋值
				if (msg.getRetContent() != null) {
					System.out
							.println("handleLocalAgentMessage--ServiceExecutingDone--assignmentHashtable: "
									+ targetElementName);

					// 赋值表，key可能是两个element name的组合字符串
					Hashtable<String, RequestData> temp = targetGoalModel
							.getAssignmentHashtable();

					// 因为有可能是组合字符串，所以需要先找到要赋值的Key
					String keyString = targetElementName;
					outer: for (String key : temp.keySet()) {
						if (key.contains("-")) {// key是组合字符串
							String[] keyStrings = key.split("-");
							for (int i = 0; i < keyStrings.length; i++) {
								if (keyStrings[i].equals(targetElementName)) {
									keyString = key;
									break outer;
								}
							}
						} else {// key不是组合字符串
							if (key.equals(targetElementName)) {
								keyString = key;
								break outer;
							}
						}
						// if (key.contains(targetElementName)) {
						// keyString = key;
						// break;
						// }
					}
					// 如果确实有需要赋值的
					if (temp.get(keyString) != null) {
						temp.get(keyString)
								.setContent(
										originalSgmMessage.getRetContent()
												.getContent());
					}

					// 查看是否有CONSTANTVALUE并确认是否已经被赋值，如果有，就给GoalModel的相应变量赋值
					String requestElementName = targetGoalModel
							.getParameterMapHashtable().get("CONSTANTVALUE");
					outer: for (String key : temp.keySet()) {
						if (key.contains("-")) {// key是组合字符串
							String[] keyStrings = key.split("-");
							for (int i = 0; i < keyStrings.length; i++) {
								if (keyStrings[i].equals(targetElementName)) {
									keyString = key;
									break outer;
								}
							}
						} else {// key不是组合字符串
							if (key.equals(targetElementName)) {
								keyString = key;
								break outer;
							}
						}
					}
					RequestData data = targetGoalModel.getAssignmentHashtable()
							.get(requestElementName);
					if (data != null && data.getContentType().equals("Text")) {
						String constantValue = EncodeDecodeRequestData
								.decodeToText(data.getContent());
						targetGoalModel.setConstantValue(constantValue);
					}

				}

				endTaskMachine((TaskMachine) targetElementMachine,
						originalSgmMessage);
				break;
			default:
				break;
			}
		}

	}

	/**
	 * 处理委托、endTask、本地服务调用，是element machine通过maneger发送给agent的消息
	 * 
	 * @param msg
	 */
	private void handleElementMessage(SGMMessage msg) {

		if (msg != null) {
			Log.logGMMDebug("handleElementMessage()", "init, msg body is: "
					+ msg.getBody());

			switch (((MesBody_Mes2Manager) msg.getBody()).getBody()) {
			// 都是把消息直接转发给agent，由agent根据消息body部分进行处理
			// case RequestPersonIA: // 需要用户反馈是否完成task的消息
			case "NoDelegatedAchieved": // 告诉主人自己完成了任务
			case "NoDelegatedFailed": // 告诉主人自己没有完成任务
				getAideAgentInterface().handleMesFromManager(msg);

				finishGoalModel(msg.getGoalModelName());
				break;

			case "RequestService": // 需要调用服务，要查询参数表
				// 先从数据传输路径表中看这个element是否需要别的element赋值过的数据
				String requestElementName = goalModelList
						.get(msg.getGoalModelName()).getParameterMapHashtable()
						.get(msg.getFromElementName());

				// 赋值表，key可能是两个element name的组合字符串
				Hashtable<String, RequestData> temp = goalModelList.get(
						msg.getGoalModelName()).getAssignmentHashtable();

				if (requestElementName != null) {
					// 如果需要，就从数据赋值表里拿出这个它需要的数据
					RequestData needRequestData = temp.get(requestElementName);
					msg.setNeedContent(needRequestData);

					// 拦截地址
					if (msg.getTaskLocation() != null
							&& msg.getTaskLocation().equals("needLocation")
							&& needRequestData.getContentType()
									.contains("Text")) {
						String dataContent = EncodeDecodeRequestData
								.decodeToText(needRequestData.getContent());

						if (dataContent.contains("Addr")) {
							String locationString = dataContent.split(";")[2]
									.split(":")[1];
							msg.setTaskLocation(locationString);
						}
						if (dataContent.contains(" at ")) {
							String locationString = dataContent.split(" at ")[1];
							msg.setTaskLocation(locationString);
						}
					}

				}

				// 再看一下这个element完成后是不是会返回某些数据，如果是，那么这个任务委托给人执行的时候，直接让人输入这些数据
				outer: for (String key : temp.keySet()) {
					if (key.contains("-")) {// key是组合字符串
						String[] keyStrings = key.split("-");
						for (int i = 0; i < keyStrings.length; i++) {
							if (keyStrings[i].equals(msg.getFromElementName())) {
								msg.setRetContent(temp.get(key));
								break outer;
							}
						}
					} else {// key不是组合字符串
						if (key.equals(msg.getFromElementName())) {
							msg.setRetContent(temp.get(key));
							break outer;
						}
					}
					//
					// if (key.contains(msg.getFromElementName())) {
					// msg.setRetContent(temp.get(key));
					// break;
					// }
				}

				getAideAgentInterface().handleMesFromManager(msg);
				break;

			default:
				break;
			}
		}
	}

	/**
	 * start这个goal model里面的所有element machines
	 * 
	 * @param goalModel
	 *            要start的goal model
	 */

	private void start(GoalModel goalModel, SGMMessage msg) {
		Log.logAdaption(goalModel.getName(), "null", "goal model started!");
		Log.logGMMDebug("start()", "goal model: " + goalModel.getName()
				+ " start!");
		if (goalModel.getElementMachines() != null
				&& goalModel.getElementMachines().size() != 0) {
			for (ElementMachine elementMachine : goalModel.getElementMachines()) {
				Thread thread = new Thread(elementMachine);
				thread.start();
			}
			SGMMessage newMessage = copyMessage(msg);
			// 然后给root goal发送激活消息
			newMessage.setBody(MesBody_Mes2Machine.ACTIVATE);
			sendMesToRoot(goalModel, newMessage);
		} else {
			Log.logError("GoalModelManager:" + goalModel.getName(), "start()",
					"elementMachines is null or its size is 0!");
		}

		// 开始的时候execution界面多一个正在执行的goal model
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String startTime = df.format(new Date());
		goalModel.setStartTime(startTime);
		application.getExecutionList().add(0, goalModel);
	}

	/**
	 * stop这个goal model，只需要给这个goal model里面的root goal发送STOP消息即可
	 * 
	 * @param goalModel
	 *            要stop的goal model
	 */
	private void stop(GoalModel goalModel, SGMMessage msg) {
		Log.logAdaption(goalModel.getName(), "null", "goal model stopped!");
		Log.logGMMDebug("stop()", "goal model: " + goalModel.getName()
				+ " stop!");
		SGMMessage newMessage = copyMessage(msg);
		newMessage.setBody(MesBody_Mes2Machine.STOP);
		sendMesToRoot(goalModel, newMessage);
	}

	/**
	 * suspend这个goal model，只需要给这个goal model里面的root goal发送SUSPEND消息即可
	 * 
	 * @param goalModel
	 *            要suspend的goal model
	 */
	private void suspend(GoalModel goalModel, SGMMessage msg) {
		Log.logAdaption(goalModel.getName(), "null", "goal model suspended!");
		Log.logGMMDebug("suspend()", "goal model: " + goalModel.getName()
				+ " suspend!");
		SGMMessage newMessage = copyMessage(msg);
		newMessage.setBody(MesBody_Mes2Machine.SUSPEND);
		sendMesToRoot(goalModel, newMessage);
	}

	/**
	 * resume这个goal model，只需要给这个goal model里面的root goal发送RESUME消息即可
	 * 
	 * @param goalModel
	 *            要resume的goal model
	 */
	private void resume(GoalModel goalModel, SGMMessage msg) {
		Log.logAdaption(goalModel.getName(), "null", "goal model resumed!");
		Log.logGMMDebug("resume()", "goal model: " + goalModel.getName()
				+ " resume!");
		SGMMessage newMessage = copyMessage(msg);
		newMessage.setBody(MesBody_Mes2Machine.RESUME);
		sendMesToRoot(goalModel, newMessage);
	}

	/**
	 * 重新把所有ElementMachine的状态设置为initial
	 * 
	 * @param goalModel
	 *            要reset的goal model
	 */
	private void reset(GoalModel goalModel) {
		Log.logAdaption(goalModel.getName(), "null", "goal model resetted!");
		Log.logGMMDebug("reset()", "goal model: " + goalModel.getName()
				+ " reset!");
		if (goalModel.getElementMachines() != null
				&& goalModel.getElementMachines().size() != 0) {
			for (ElementMachine elementMachine : goalModel.getElementMachines()) {
				elementMachine.resetMachine();
			}
		} else {
			Log.logError("GoalModelManager:" + goalModel.getName(), "reset()",
					"elementMachines is null or its size is 0!");
		}

	}

	private void quitTaskMachine(TaskMachine taskMachine, SGMMessage msg) {
		Log.logGMMDebug("quitTaskMachine()",
				"task machine: " + taskMachine.getName() + " quit!");
		// SGMMessage msg = new SGMMessage("TOTASK", "UI",
		// taskMachine.getName(),
		// mes);

		SGMMessage newMessage = copyMessage(msg);
		newMessage.setBody(MesBody_Mes2Machine.TASK_FAILED);

		if (taskMachine.getMsgPool().offer(newMessage)) {
			Log.logMessage(newMessage, true);
			Log.logEMDebug("GoalModelManager:" + taskMachine.getName(),
					"endTaskMachine()",
					"UI thread send a " + newMessage.getBody() + " msg to "
							+ taskMachine.getName() + " succeed!");
		} else {
			Log.logMessage(newMessage, false);
			Log.logError("GoalModelManager:" + taskMachine.getName(),
					"endTaskMachine()",
					"UI thread send a " + newMessage.getBody() + " msg to "
							+ taskMachine.getName() + " error!");
		}
	}

	/**
	 * 给一个task
	 * machine发送END或者QUIT消息，这个是在用户完成了某个需要他参与的任务后，在UI上点击这个task后面的end按钮时触发的操作
	 * 
	 * @param taskMachine
	 *            用户完成的task
	 * @param msg
	 *            发送的消息内容，END为完成了，QUIT为没有完成
	 */
	private void endTaskMachine(TaskMachine taskMachine, SGMMessage msg) {
		Log.logGMMDebug("endTaskMachine()",
				"task machine: " + taskMachine.getName() + " end!");
		// SGMMessage msg = new SGMMessage("TOTASK", "UI",
		// taskMachine.getName(),
		// mes);

		SGMMessage newMessage = copyMessage(msg);
		newMessage.setBody(MesBody_Mes2Machine.TASK_DONE);

		if (taskMachine.getMsgPool().offer(newMessage)) {
			Log.logMessage(newMessage, true);
			Log.logEMDebug("GoalModelManager:" + taskMachine.getName(),
					"endTaskMachine()",
					"UI thread send a " + newMessage.getBody() + " msg to "
							+ taskMachine.getName() + " succeed!");
		} else {
			Log.logMessage(newMessage, false);
			Log.logError("GoalModelManager:" + taskMachine.getName(),
					"endTaskMachine()",
					"UI thread send a " + newMessage.getBody() + " msg to "
							+ taskMachine.getName() + " error!");
		}

	}

	/**
	 * 发送一条消息给goal model中的root goal
	 * 
	 * @param goalModel
	 *            消息接收方的goal model
	 * @param msg
	 *            要发送的消息
	 */
	private void sendMesToRoot(GoalModel goalModel, SGMMessage msg) {
		if (goalModel.getRootGoal() != null) {

			SGMMessage newMessage = copyMessage(msg);
			// SGMMessage msg = new SGMMessage("TOROOT", "UI", goalModel
			// .getRootGoal().getName(), mes);
			if (goalModel.getRootGoal().getMsgPool().offer(newMessage)) {
				Log.logMessage(newMessage, true);
				Log.logEMDebug("GoalModelManager:" + goalModel.getName(),
						"sendMesToRoot()",
						"UI thread send a " + newMessage.getBody() + " msg to "
								+ goalModel.getRootGoal().getName()
								+ " succeed!");
			} else {
				Log.logMessage(newMessage, false);
				Log.logError("GoalModelManager:" + goalModel.getName(),
						"sendMesToRoot()",
						"UI thread send a " + newMessage.getBody() + " msg to "
								+ goalModel.getRootGoal().getName() + " error!");
			}

		} else {
			Log.logError("GoalModelManager:" + goalModel.getName(),
					"sendMesToRoot()", "rootGoal is null!");
		}
	}

	/**
	 * 拿到agent的引用
	 * 
	 * @return agent
	 */
	private AideAgentInterface getAideAgentInterface() {
		AideAgentInterface aideAgentInterface = null; // agent interface
		try {
			aideAgentInterface = MicroRuntime.getAgent(agentNickname)
					.getO2AInterface(AideAgentInterface.class);
		} catch (StaleProxyException e) {
			e.printStackTrace();
		} catch (ControllerException e) {
			e.printStackTrace();
		}
		return aideAgentInterface;
	}

	/**
	 * 根据指定的element name，从goal model中取出对应的element machine
	 * 
	 * @param goalModel
	 *            goal model
	 * @param elementName
	 *            指定的element name
	 * @return 对应的element machine，如果没有返回null
	 */
	private ElementMachine getEMFromGoalModelByName(GoalModel goalModel,
			String elementName) {
		ElementMachine ret = null;
		for (ElementMachine em : goalModel.getElementMachines()) {
			if (em.getName().equals(elementName)) {
				ret = em;
			}
		}
		return ret;
	}

	/**
	 * copy一个msg，因为原msg会改变，而我们需要多次使用原始的msg
	 * 
	 * @param msg
	 *            原始msg
	 * @return
	 */
	private SGMMessage copyMessage(SGMMessage msg) {
		SGMMessage newMessage = new SGMMessage(msg.getHeader(),
				msg.getGoalModelName(), msg.getFromElementName(),
				msg.getToElementName(), msg.getBody());

		newMessage.setTaskDescription(msg.getTaskDescription());
		newMessage.setAbstractServiceName(msg.getAbstractServiceName());
		newMessage.setNeedContent(msg.getNeedContent());
		newMessage.setRetContent(msg.getRetContent());
		return newMessage;
	}

	/**
	 * 结束一个goal model运行实例时，把goal model转换成一个static goal model
	 * 
	 * @param goalModelName
	 */
	private void finishGoalModel(String goalModelName) {
		// reset的时候移除正在执行的goal model，添加一个static goal model
		GoalModel goalModel = this.goalModelList.get(goalModelName);

		StaticGM rootGoal = new StaticGM(goalModel.getRootGoal().getName(),
				goalModel.getRootGoal().getLevel(), null, goalModel
						.getRootGoal().getCurrentState(), goalModel
						.getRootGoal().getDecomposition());
//		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		String finishTime = df.format(new Date());

		StaticGoalModel staticGoalModel = new StaticGoalModel(
				goalModel.getName(), rootGoal, goalModel.getStartTime());

		for (ElementMachine em : goalModel.getElementMachines()) {
			StaticEM staticEM = StaticEM.getStaticEMFromEM(em);
			staticGoalModel.addStaticEM(staticEM);
		}
		staticGoalModel.setUserLogList(goalModel.getUserLogList());
		staticGoalModel.setConstantValue(goalModel.getConstantValue());
		goalModel.setConstantValue(null);

		application.getExecutionList().add(0, staticGoalModel);
		application.getExecutionList().remove(goalModel);
	}

	public void addGoalModel(GoalModel gm) {
		this.goalModelList.put(gm.getName(), gm);
		gm.setGoalModelManager(this);
	}

	public Hashtable<String, GoalModel> getGoalModelList() {
		return goalModelList;
	}

	public BlockingQueue<SGMMessage> getMsgPool() {
		return msgPool;
	}

	public void setMsgPool(BlockingQueue<SGMMessage> msgPool) {
		this.msgPool = msgPool;
	}

	public String getAgentNickname() {
		return agentNickname;
	}

	public void setAgentNickname(String agentNickname) {
		this.agentNickname = agentNickname;
	}

}
