/**
 * 
 */
package cn.edu.fudan.se.agent.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import cn.edu.fudan.se.agent.support.ACLMC_DelegateTask;
import cn.edu.fudan.se.agent.support.AdaptationUtil;
import cn.edu.fudan.se.agent.support.AideAgentSupport;
import cn.edu.fudan.se.agent.support.TaskExecutingUtil;
import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.userMes.UserConfirmTask;
import cn.edu.fudan.se.mobigoal.userMes.UserConfirmWithRetTask;
import cn.edu.fudan.se.mobigoal.userMes.UserInputTextTask;
import cn.edu.fudan.se.mobigoal.userMes.UserTakePictureTask;
import cn.edu.fudan.se.mobigoal.userMes.UserTask;
import cn.edu.fudan.se.mobigoal.utils.UserLog;
import cn.edu.fudan.se.sgm.goalmachine.message.MesBody_Mes2Manager;
import cn.edu.fudan.se.sgm.goalmachine.message.MesHeader_Mes2Manger;
import cn.edu.fudan.se.sgm.goalmachine.message.SGMMessage;
import cn.edu.fudan.se.sgm.goalmodel.EncodeDecodeRequestData;
import cn.edu.fudan.se.sgm.goalmodel.GoalModelManager;
import cn.edu.fudan.se.sgm.goalmodel.RequestData;
import cn.edu.fudan.se.sgm.log.Log;
import edu.fudan.se.agent.data.RequestUserInformation;
import edu.fudan.se.agent.data.UserInformation;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

/**
 * @author qwy, whh
 * 
 */
public class AideAgent extends Agent implements AideAgentInterface {
	private static final long serialVersionUID = -540261740171554489L;

	private SGMApplication sgmApplication;
	private GoalModelManager goalModelManager;

	private ArrayList<UserTask> userCurrentTaskList;
//	private ArrayList<UserLog> userLogList;

	private Context context;

	int canTriedTimes = 2;// agent收到调用服务失败消息后重新查找替换服务的次数
	ArrayList<String> allIntentServiceNameArrayList = new ArrayList<String>();

	// 用来储存goalModelName#elementName在做自适应时尝试过的各种服务，包括自动服务，或者尝试过的friend
	Hashtable<String, AdaptationUtil> taskExecutingAdaptionUtilList = new Hashtable<String, AdaptationUtil>();

	// 用来存储执行goalModelName#elementName需要的各种相关数据，key是goalModelName#elementName
	Hashtable<String, TaskExecutingUtil> taskExecutingUtilList = new Hashtable<String, TaskExecutingUtil>();

	@Override
	protected void setup() {
		super.setup();
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			if (args[0] instanceof Context) {
				context = (Context) args[0];
			}
			if (args[1] instanceof SGMApplication) {
				sgmApplication = (SGMApplication) args[1];
			}

			goalModelManager = sgmApplication.getGoalModelManager();
			userCurrentTaskList = sgmApplication.getUserCurrentTaskList();
//			userLogList = sgmApplication.getUserLogList();
		}

		registerO2AInterface(AideAgentInterface.class, this);

		/* 初始化所有可能用到的intent service */
		AideAgentSupport.initAllIntentService(allIntentServiceNameArrayList);

		/* 在黄页服务中注册所有的goal model服务 */
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
//		for (GoalModel gm : goalModelManager.getGoalModelList().values()) {
//			ServiceDescription sd = new ServiceDescription();
//			sd.setType("GOALMODEL");
//			sd.setName(gm.getName());
//			dfd.addServices(sd);
//		}

		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}

		// 循环接收并处理来自外部agent的消息
		addBehaviour(new HandleMesFromExternalAgent());

	}

	@Override
	protected void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	@Override
	public void sendMesToManager(SGMMessage msg) {
		this.addBehaviour(new SendMesToManager(this, msg));
	}

	@Override
	public void handleMesFromManager(SGMMessage msg) {
		this.addBehaviour(new HandleMesFromManager(this, msg));
	}

	@Override
	public void sendMesToExternalAgent(ACLMC_DelegateTask aclmc_DelegateTask) {
		this.addBehaviour(new SendMesToExternalAgent(this, aclmc_DelegateTask));
	}

	@Override
	public void handleMesFromService(SGMMessage msg) {
		this.addBehaviour(new HandleMesFromService(this, msg));
	}

	@Override
	public void sendLocationToServerAgent(String userLocation) {
		this.addBehaviour(new SendLocationToServerAgent(this, userLocation));
	}

//	@Override
//	public void registerGoalModelService(GoalModel goalModel) {
//		this.addBehaviour(new RegisterGoalModelService(this, goalModel));
//	}

	/**
	 * 发送消息给本地manager
	 * 
	 * @author whh
	 * 
	 */
	private class SendMesToManager extends OneShotBehaviour {

		private static final long serialVersionUID = 4474626789362069528L;
		private SGMMessage msg;

		public SendMesToManager(Agent a, SGMMessage msg) {
			super(a);
			this.msg = msg;
		}

		@Override
		public void action() {
			Log.logAADebug("AideAgent", "SendMesToManager()", "init.");
			android.util.Log.i("MY_LOG", "Send mes to manager...");
			// msg.getSender().setAgentName(a.getName());

			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String logTime = df.format(new Date());

			switch (((MesBody_Mes2Manager) msg.getBody()).getBody()) {

			case "StartGM":
				UserLog userLog1 = new UserLog(logTime, msg.getGoalModelName()
						+ " started!");
				goalModelManager.getGoalModelList().get(msg.getGoalModelName()).addUserLog(userLog1);
//				userLogList.add(0, userLog1);
				break;
			case "StopGM":
				UserLog userLog2 = new UserLog(logTime, msg.getGoalModelName()
						+ " stopped!");
				goalModelManager.getGoalModelList().get(msg.getGoalModelName()).addUserLog(userLog2);
//				userLogList.add(0, userLog2);
				break;
			case "SuspendGM":
				UserLog userLog3 = new UserLog(logTime, msg.getGoalModelName()
						+ " suspended!");
				goalModelManager.getGoalModelList().get(msg.getGoalModelName()).addUserLog(userLog3);
//				userLogList.add(0, userLog3);
				break;
			case "ResumeGM":
				UserLog userLog4 = new UserLog(logTime, msg.getGoalModelName()
						+ " resumed!");
				goalModelManager.getGoalModelList().get(msg.getGoalModelName()).addUserLog(userLog4);
//				userLogList.add(0, userLog4);
				break;
			case "ResetGM":
				// 当用户点击reset按钮后要把和那个goal model相关的两个adaptationUtilList中储存的数据清空
				AideAgentSupport.resetAdaptationUtilList(
						msg.getGoalModelName(), taskExecutingAdaptionUtilList);

				goalModelManager.getGoalModelList().get(msg.getGoalModelName()).clearUserLog();
//				userLogList.clear();

				UserLog userLog5 = new UserLog(logTime, msg.getGoalModelName()
						+ " reset!");
				goalModelManager.getGoalModelList().get(msg.getGoalModelName()).addUserLog(userLog5);
//				userLogList.add(0, userLog5);
				break;

			default:
				break;
			}

			if (goalModelManager.getMsgPool().offer(msg)) {
				Log.logMessage(msg, true);
			} else {
				Log.logMessage(msg, false);
			}
		}

	}

	/**
	 * 处理来自本地manager的消息
	 * 
	 * @author whh
	 * 
	 */
	private class HandleMesFromManager extends OneShotBehaviour {

		private static final long serialVersionUID = -628259960649979121L;
		private SGMMessage msg;
		private Agent agent;

		public HandleMesFromManager(Agent a, SGMMessage msg) {
			super(a);
			this.agent = a;
			this.msg = msg;
		}

		@Override
		public void action() {
			Log.logAADebug("AideAgent", "HandleMesFromManager()",
					"init. msg body is: " + msg.getBody());
			android.util.Log.i("MY_LOG", "Handle mes from manager...");

			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			String goalModelName = msg.getGoalModelName();
			switch (((MesBody_Mes2Manager) msg.getBody()).getBody()) {

			case "RequestService":

				String elementName = msg.getFromElementName();
				String abstractServiceName = msg.getAbstractServiceName();
				String taskDescription = msg.getTaskDescription();

				// 这是第一次调用，需要初始化一些相关变量
				TaskExecutingUtil taskExecutingUtil = new TaskExecutingUtil(
						abstractServiceName, taskDescription,
						msg.getNeedContent(), msg.getRetContent());
				if (msg.getTaskLocation() != null) {
					taskExecutingUtil.setTaskLocation(msg.getTaskLocation());
				}

				taskExecutingUtilList.put(goalModelName + "#" + elementName,
						taskExecutingUtil);

				AdaptationUtil adaptationUtil = new AdaptationUtil();
				taskExecutingAdaptionUtilList.put(goalModelName + "#"
						+ elementName, adaptationUtil);

				Log.logAdaption(goalModelName, elementName,
						"Initial. AideAgent received RequestService Mes.");

				// 开始调用服务自动执行
				invokeServiceExecuting(goalModelName, elementName);

				break;

			case "NoDelegatedAchieved":
				String mesTime = df.format(new Date());
				UserLog userLog1 = new UserLog(mesTime, goalModelName
						+ " achieved!");
				goalModelManager.getGoalModelList().get(goalModelName).addUserLog(userLog1);
//				userLogList.add(0, userLog1);
				break;

			case "NoDelegatedFailed":

				String mesTime2 = df.format(new Date());
				UserLog userLog2 = new UserLog(mesTime2, goalModelName
						+ " failed!");
				goalModelManager.getGoalModelList().get(goalModelName).addUserLog(userLog2);
//				userLogList.add(0, userLog2);
				break;

			default:
				break;
			}
		}

	}

	/**
	 * 发送消息给外部agent
	 * 
	 * @author whh
	 * 
	 */
	private class SendMesToExternalAgent extends OneShotBehaviour {

		private static final long serialVersionUID = 226868601699462162L;
		private ACLMC_DelegateTask aclmc_DelegateTask;
		private Agent a;

		public SendMesToExternalAgent(Agent a,
				ACLMC_DelegateTask aclmc_DelegateTask) {
			super(a);
			this.a = a;
			this.aclmc_DelegateTask = aclmc_DelegateTask;
		}

		@Override
		public void action() {
			Log.logAADebug("AideAgent", "SendMesToExternalAgent()", "init.");

			String toAgentName = aclmc_DelegateTask.getToAgentName();
			aclmc_DelegateTask.setFromAgentName(this.a.getLocalName());

			ACLMessage aclmsg = new ACLMessage(ACLMessage.INFORM);
			aclmsg.addReceiver(new AID(toAgentName, AID.ISLOCALNAME));
			// aclmsg.setContent(content);
			try {
				aclmsg.setContentObject(aclmc_DelegateTask);
			} catch (IOException e) {
				android.util.Log
						.i("MY_LOG",
								"Send mes to external agent...aclmsg.setContentObject(aclmc_DelegateTask) error!!!");
				e.printStackTrace();
			}
			send(aclmsg);

		}

	}

	/**
	 * 处理来自外部agent的消息
	 * 
	 * @author whh
	 * 
	 */
	private class HandleMesFromExternalAgent extends CyclicBehaviour {

		private static final long serialVersionUID = -4159835298981404030L;

		@Override
		public void action() {
			ACLMessage msg = receive();
			if (msg != null) {
				Log.logAADebug("AideAgent", "HandleMesFromExternalAgent()",
						"init.");
				android.util.Log.i("MY_LOG",
						"Handle mes from external agent...");

				// 来自server agent的消息
				if (msg.getPerformative() == ACLMessage.PROPOSE) {
					try {
						RequestUserInformation requestUserInformation = (RequestUserInformation) msg
								.getContentObject();

						String goalModelName = requestUserInformation
								.getGoalModelName();
						String elementName = requestUserInformation
								.getElementName();

						ArrayList<UserInformation> userInformationList = requestUserInformation
								.getUserInformations();

						// 开始委托执行
						delegateToPeopleExecuting(goalModelName, elementName,
								userInformationList, this.getAgent()
										.getLocalName());

					} catch (UnreadableException e) {
						e.printStackTrace();
					}
				}

				// 来自其他aide agent的ACLMessage.INFORM消息
				if (msg.getPerformative() == ACLMessage.INFORM) {
					try {
						ACLMC_DelegateTask aclmc_DelegateTask = (ACLMC_DelegateTask) msg
								.getContentObject();
						// 新来的委托任务
						if (aclmc_DelegateTask.getDtHeader().equals(
								ACLMC_DelegateTask.DTHeader.NEWDT)) {
							handleNewDelegateTask(aclmc_DelegateTask);
						}
						// 委托任务的返回
						else if (aclmc_DelegateTask.getDtHeader().equals(
								ACLMC_DelegateTask.DTHeader.DTBACK)) {
							handleDelegateTaskBack(aclmc_DelegateTask);
						}

					} catch (UnreadableException e) {
						e.printStackTrace();
					}

				}
			}
		}
	}

	private class HandleMesFromService extends OneShotBehaviour {

		private static final long serialVersionUID = 4286751790137940309L;
		private SGMMessage msg;
		private Agent a;

		public HandleMesFromService(Agent a, SGMMessage msg) {
			super(a);
			this.a = a;
			this.msg = msg;
		}

		@Override
		public void action() {
			Log.logAADebug("AideAgent", "HandleMesFromService()",
					"init. msg body is: " + msg.getBody());

			String goalModelName = msg.getGoalModelName();
			String elementName = msg.getToElementName();

			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String logTime = df.format(new Date());
			String logContent = "";

			// 服务调用成功，直接把msg发送给manager，同时要把serviceInvocationUtilList里面储存的信息清空
//			if (msg.getBody().equals(MesBody_Mes2Manager.ServiceExecutingDone)) {
			if (((MesBody_Mes2Manager)msg.getBody()).getBody().equals("ServiceExecutingDone")) {

				// 清空serviceInvocationUtilList里面储存的相关信息清空
				clearTaskExecuting(goalModelName, elementName);

				// 发送消息给manager
				android.util.Log.i("MY_LOG", "Send mes to manager...");
				if (goalModelManager.getMsgPool().offer(msg)) {
					Log.logMessage(msg, true);
				} else {
					Log.logMessage(msg, false);
				}

				Log.logAdaption(goalModelName, elementName,
						"Invoking service Done. Result: Succeed!");
				logContent += "The task <" + goalModelName + "-" + elementName
						+ "> ends.";
			}
			// 服务调用失败
//			else if (msg.getBody().equals(
//					MesBody_Mes2Manager.ServiceExecutingFailed)) {
			
			else if (((MesBody_Mes2Manager)msg.getBody()).getBody().equals(
					"ServiceExecutingFailed")) {

				// 继续调用服务，如果没有可选的，会自动跳入到委托执行
				invokeServiceExecuting(goalModelName, elementName);

				Log.logAdaption(goalModelName, elementName,
						"Invoking service Done. Result: Failed! Continue adaption.");

				logContent += "The task <" + goalModelName + "-" + elementName
						+ "> failed. Try again...";
			}

			UserLog userLog = new UserLog(logTime, logContent);

			goalModelManager.getGoalModelList().get(goalModelName).addUserLog(userLog);
//			userLogList.add(0, userLog);

		}
	}

	/**
	 * 发送位置信息给ServerAgent
	 * 
	 * @author whh
	 * 
	 */
	private class SendLocationToServerAgent extends OneShotBehaviour {

		private static final long serialVersionUID = -8674488872785177053L;
		private Agent a;
		private String userLocation;

		public SendLocationToServerAgent(Agent a, String userLocation) {
			super(a);
			this.a = a;
			this.userLocation = userLocation;
		}

		@Override
		public void action() {
			String content = a.getLocalName() + "---" + userLocation;

			android.util.Log.i("SendLocationToServerAge",
					"Send user location to server agent...content is: "
							+ content);

			ACLMessage aclmsg = new ACLMessage(ACLMessage.INFORM);
			aclmsg.addReceiver(new AID("ServerAgent", AID.ISLOCALNAME));
			aclmsg.setContent(content);
			send(aclmsg);
		}

	}

	/**
	 * 向server agent发送请求所有用户信息的ACLMessage，发送后会进入等待server agent回复过程中
	 * 
	 * @param goalModelName
	 *            请求用户信息的goal model name
	 * @param elementName
	 *            请求用户信息的element name
	 */
	private void obtainFriendsInfoFromServerAgent(String goalModelName,
			String elementName) {
		Log.logAADebug("AideAgent", "obtainFriendsInfoFromServerAgent()",
				goalModelName + "#" + elementName);

		String content = goalModelName + "#" + elementName;
		android.util.Log.i("MY_LOG",
				"Send OBTAINFRIENDSINFO aclMessage to server agent...content is: "
						+ content);

		ACLMessage aclmsg = new ACLMessage(ACLMessage.CFP);
		aclmsg.addReceiver(new AID("ServerAgent", AID.ISLOCALNAME));
		aclmsg.setContent(content);
		send(aclmsg);
		// 然后进去等待server agent回复过程中
	}

	/**
	 * 调用自动执行的服务来完成task，如果没有可选服务，会跳入到委托执行过程中
	 * 
	 * @param goalModelName
	 *            要执行的task所属的goal model name
	 * @param elementName
	 *            要执行的task的element name
	 */
	private void invokeServiceExecuting(String goalModelName, String elementName) {

		TaskExecutingUtil taskExecutingUtil = taskExecutingUtilList
				.get(goalModelName + "#" + elementName);

		// 执行goalModelName#elementName时需要查找的抽象服务名称
		String abstractServiceName = taskExecutingUtil.getAbstractServiceName();

		// 先查询注册的服务列表中是否有匹配服务
		ArrayList<String> serviceInvocationChoice = AideAgentSupport
				.getServiceNameListBasedAbstractServiceName(
						abstractServiceName, allIntentServiceNameArrayList);

		// 然后查看是调用服务的记录
		AdaptationUtil serviceInvocationUtil = taskExecutingAdaptionUtilList
				.get(goalModelName + "#" + elementName);

		// 把已经调用过的服务列表从可选服务列表中移除
		serviceInvocationChoice.removeAll(serviceInvocationUtil
				.getAlreadyTriedList());

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String logTime = df.format(new Date());
		String logContent = "";

		// 查看移除之后是否还有可选的服务列表
		if (serviceInvocationChoice.isEmpty()) {// 没有，委托执行
			// 不能够委托的必须本地执行的task
			if (taskExecutingUtil.getTaskLocation() != null
					&& taskExecutingUtil.getTaskLocation().equals(
							"selfLocation")) {
				// 结束任务
				taskFail(goalModelName, elementName);
			} else {
				// 向server agent请求朋友列表
				obtainFriendsInfoFromServerAgent(goalModelName, elementName);
				// 然后会一直等待server agent回复信息，在handleMesFromExternalAgent会对信息进行处理

				Log.logAdaption(goalModelName, elementName,
						"No service to invoke. Delegate the task to people.");

				logContent += "The task <"
						+ goalModelName
						+ "-"
						+ elementName
						+ "> could not be accomplished automatically. Try to delegate it...";
				UserLog userLog = new UserLog(logTime, logContent);

				goalModelManager.getGoalModelList().get(goalModelName).addUserLog(userLog);
//				userLogList.add(0, userLog);
			}

		} else {// 有，自动调用服务执行。只要有可选服务，就会调用，没有超过次数之说
			// 将调用信息添加到记录中
			serviceInvocationUtil.getAlreadyTriedList().add(
					serviceInvocationChoice.get(0));

			// 开始调用服务
			Intent serviceIntent = new Intent(serviceInvocationChoice.get(0));
			Bundle bundle = new Bundle();
			bundle.putString("GOAL_MODEL_NAME", goalModelName);
			bundle.putString("ELEMENT_NAME", elementName);

			RequestData needRequestData = taskExecutingUtil
					.getNeedRequestData();
			if (needRequestData != null) {
				bundle.putSerializable("NEED_REQUEST_DATA_CONTENT",
						needRequestData);
			}

			RequestData retRequestData = taskExecutingUtil.getRetRequestData();
			if (retRequestData != null) {
				bundle.putSerializable("RET_REQUEST_DATA_CONTENT",
						retRequestData);
			}

			serviceIntent.putExtras(bundle);
			context.startService(serviceIntent);
			// 然后进入等待服务调用结果返回中

			Log.logAdaption(goalModelName, elementName,
					"Invoking service. Service name: "
							+ serviceInvocationChoice.get(0));

			logContent += "The task <" + goalModelName + "-" + elementName
					+ "> begin executing automatically...";
			UserLog userLog = new UserLog(logTime, logContent);
			goalModelManager.getGoalModelList().get(goalModelName).addUserLog(userLog);
//			userLogList.add(0, userLog);

		}

	}

	/**
	 * 将goalModelName#elementName委托给人执行，包括自己
	 * 
	 * @param goalModelName
	 *            goal model name
	 * @param elementName
	 *            element name
	 * @param userInformationList
	 *            所有可委托对象的信息，包括名称、距离、声望、亲密度等，用来做排序然后返回排好序的委托对象列表
	 * @param selfAgentName
	 *            本机agent的nick name
	 */
	private void delegateToPeopleExecuting(String goalModelName,
			String elementName, ArrayList<UserInformation> userInformationList,
			String selfAgentName) {
		if (userInformationList == null || userInformationList.isEmpty()) {// 没有可委托对象,失败

			delegateFail(goalModelName, elementName);

			Log.logAdaption(goalModelName, elementName,
					"No delegateTo user. Task Failed!");

		} else {
			// 先获得委托任务在执行时需要的各种数据
			TaskExecutingUtil taskExecutingUtil = taskExecutingUtilList
					.get(goalModelName + "#" + elementName);

			// 执行任务需要的地点，可能为空，可能是selfLocation，可能是一个确定地点，也可能是多个确定地点
			String taskLocation = "";
			if (taskExecutingUtil.getTaskLocation() == null) {
				taskLocation = "null";
			} else {
				taskLocation = taskExecutingUtil.getTaskLocation();
			}
			// 获取location，至少是一个（为空的时候被赋值成了"null"），可能是多个
			String[] locationArray = taskLocation.split("#");
			int locationNum = locationArray.length;

			AdaptationUtil delegateAUtil = taskExecutingAdaptionUtilList
					.get(goalModelName + "#" + elementName);// 不可能为空
			int triedLocationNums = delegateAUtil.getTriedLocationNums();

			if (triedLocationNums < locationNum) {
				// 把位置信息传进去
				ArrayList<String> delegaToChoice = AideAgentSupport
						.getDelegateToListBasedRanking(userInformationList,
								locationArray[triedLocationNums], selfAgentName);

				// 先移除已经针对这个位置委托过的人
				delegaToChoice.removeAll(delegateAUtil.getAlreadyTriedList());
				int alreadyTriedTimes = delegateAUtil.getTriedTimes();
				// 还可以委托
				if ((!delegaToChoice.isEmpty())
						&& (alreadyTriedTimes < canTriedTimes)) {
					String delegateTo = delegaToChoice.get(0);
					delegateAUtil.setTriedTimes(alreadyTriedTimes + 1);
					delegateAUtil.getAlreadyTriedList().add(delegateTo);

					android.util.Log.i("MY_LOG", "DelegateTo user is: "
							+ delegateTo + ", alreadyTriedTimes: "
							+ alreadyTriedTimes);
					// 发送消息给委托对象agent
					// 委托出去的任务，必须要人来做，无需给输入数据，只需要知道让用户返回什么数据即可
					String toAgentName = delegateTo.split("#")[0];
					ACLMC_DelegateTask aclmc_DelegateTask = new ACLMC_DelegateTask(
							ACLMC_DelegateTask.DTHeader.NEWDT, selfAgentName,
							toAgentName, goalModelName, elementName);
					//替换地址信息
					String taskDescription=taskExecutingUtil
							.getTaskDescription();
					if (taskDescription.contains("?location?")) {
						taskDescription = taskDescription.replace("?location?", locationArray[triedLocationNums]);
					}
					System.out.println("debug!!!!!!!!!!!!!!!!+taskDescription: " + taskDescription);
					aclmc_DelegateTask.setTaskDescription(taskDescription);

					RequestData needRequestData = taskExecutingUtil
							.getNeedRequestData();
					if (needRequestData != null) {
						aclmc_DelegateTask.setNeedRequestData(needRequestData);
					}

					RequestData retRequestData = taskExecutingUtil
							.getRetRequestData();
					if (retRequestData != null) {
						RequestData newRetRequestData = new RequestData(retRequestData.getName(), retRequestData.getContentType());
						newRetRequestData.setContent(retRequestData.getContent());
						if (retRequestData.getName().contains("?location?")) {
							newRetRequestData.setName(newRetRequestData.getName()
									.replace("?location?",
											locationArray[triedLocationNums]));
						}
						android.util.Log.i("MY_LOG",
								"delegateToPeopleExecuting---retRequestData name2: "
										+ newRetRequestData.getName());
						aclmc_DelegateTask.setRetRequestData(newRetRequestData);
					}
					sendMesToExternalAgent(aclmc_DelegateTask);// 这里有可能是发回给自己了

					SimpleDateFormat df = new SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss");
					String logTime = df.format(new Date());
					String logContent = "The task <" + goalModelName + "-"
							+ elementName + "> is delegated to [" + delegateTo
							+ "].";
					UserLog userLog = new UserLog(logTime, logContent);
					goalModelManager.getGoalModelList().get(goalModelName).addUserLog(userLog);
//					userLogList.add(0, userLog);

					Log.logAdaption(goalModelName, elementName,
							"Delegate to people. DelegateToName: " + delegateTo
									+ ", AlreadyTriedTimes: "
									+ alreadyTriedTimes);

				} else {// 针对当前这个位置没有可委托对象或者超过了重试次数
					// 把尝试过的位置次数+1
					delegateAUtil.setTriedLocationNums(++triedLocationNums);
					// 把针对当前位置试过的次数重新设置为0
					delegateAUtil.setTriedTimes(0);

					if (delegateAUtil.getTriedLocationNums() >= locationNum) {// 所有地方都试过了
						delegateFail(goalModelName, elementName);

						Log.logAdaption(goalModelName, elementName,
								"No delegateTo user. Task Failed!");
					} else {// 重新发起一次请求，开始针对下个位置尝试
						obtainFriendsInfoFromServerAgent(goalModelName,
								elementName);
						// 然后会一直等待server
						// agent回复信息，在handleMesFromExternalAgent会对信息进行处理
					}
				}

			}
		}
	}

	/**
	 * 针对goalModelName#elementName委托执行时，没有可委托对象或者超过重试次数了
	 * 
	 * @param goalModelName
	 *            goal model name
	 * @param elementName
	 *            element name
	 */
	private void delegateFail(String goalModelName, String elementName) {

		String taskDescription = taskExecutingUtilList.get(
				goalModelName + "#" + elementName).getTaskDescription();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String logTime = df.format(new Date());
		String logContent = "Can not find someone to delegate the task <"
				+ goalModelName + "-" + elementName + ">. Task failed!";
		UserLog userLog = new UserLog(logTime, logContent);
		goalModelManager.getGoalModelList().get(goalModelName).addUserLog(userLog);
//		userLogList.add(0, userLog);

		android.util.Log.i("MY_LOG",
				"no delegateTo user or alreadyTriedTimes >= canTriedTimes!!");

		// 清空执行时储存的数据
		clearTaskExecuting(goalModelName, elementName);

		SGMMessage sgmMessage = new SGMMessage(
				MesHeader_Mes2Manger.LOCAL_AGENT_MESSAGE, goalModelName, null,
				elementName, new MesBody_Mes2Manager("QuitTE"));

		android.util.Log.i("MY_LOG", "Send mes to manager...");
		if (goalModelManager.getMsgPool().offer(sgmMessage)) {
			Log.logMessage(sgmMessage, true);
		} else {
			Log.logMessage(sgmMessage, false);
		}
	}

	private void taskFail(String goalModelName, String elementName) {

		// 清空执行时储存的数据
		clearTaskExecuting(goalModelName, elementName);

		SGMMessage sgmMessage = new SGMMessage(
				MesHeader_Mes2Manger.LOCAL_AGENT_MESSAGE, goalModelName, null,
				elementName, new MesBody_Mes2Manager("QuitTE"));

		android.util.Log.i("MY_LOG", "Send mes to manager...");
		if (goalModelManager.getMsgPool().offer(sgmMessage)) {
			Log.logMessage(sgmMessage, true);
		} else {
			Log.logMessage(sgmMessage, false);
		}
	}

	/**
	 * 清空在执行goalModelName#elementName时储存的各种数据
	 * 
	 * @param goalModelName
	 *            goal model name
	 * @param elementName
	 *            element name
	 */
	private void clearTaskExecuting(String goalModelName, String elementName) {
		taskExecutingAdaptionUtilList.remove(goalModelName + "#" + elementName);
		taskExecutingUtilList.remove(goalModelName + "#" + elementName);
	}

	/**
	 * 处理新的委托任务
	 * 
	 * @param aclmc_DelegateTask
	 */
	private void handleNewDelegateTask(ACLMC_DelegateTask aclmc_DelegateTask) {

		String fromAgentName = aclmc_DelegateTask.getFromAgentName();
		String goalModelName = aclmc_DelegateTask.getGoalModelName();
		String elementName = aclmc_DelegateTask.getElementName();

		String userTaskTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
				.format(new Date());
		String userTaskDescription = "";

		UserTask userTask = null;
		// 不需要人返回某些数据，是普通的userTask
		if (aclmc_DelegateTask.getRetRequestData() == null) {
			RequestData needRequestData = aclmc_DelegateTask
					.getNeedRequestData();
			userTask = new UserTask(userTaskTime, fromAgentName, goalModelName,
					elementName);
			userTaskDescription = "You need to do the task:\n"
					+ aclmc_DelegateTask.getTaskDescription();
			if (needRequestData != null
					&& needRequestData.getContentType().contains("Text")) {
				userTaskDescription += " "
						+ EncodeDecodeRequestData.decodeToText(needRequestData
                        .getContent());
			}
		} else {
			RequestData retRequestData = aclmc_DelegateTask.getRetRequestData();
			RequestData needRequestData = aclmc_DelegateTask
					.getNeedRequestData();
			// 让用户输入文本的任务
			if (retRequestData.getContentType().equals("Text")) {
				userTask = new UserInputTextTask(userTaskTime, fromAgentName,
						goalModelName, elementName);
				userTaskDescription = "Please input:\n"
						+ retRequestData.getName();
				if (needRequestData != null
						&& needRequestData.getContentType().equals("Text")) {
					userTaskDescription += " of "
							+ EncodeDecodeRequestData
									.decodeToText(needRequestData.getContent());
				}
			}
			// 让用户拍照的任务
			else if (retRequestData.getContentType().equals("Image")) {
				userTask = new UserTakePictureTask(userTaskTime, fromAgentName,
						goalModelName, elementName);
				userTaskDescription = "Please take a picture about:\n"
						+ retRequestData.getName();
			}
			// 让用户确认结果的任务
			else if (retRequestData.getContentType().equals("Boolean")) {
				userTask = new UserConfirmTask(userTaskTime, fromAgentName,
						goalModelName, elementName);
				userTaskDescription = "Please confirm: \n"
						+ retRequestData.getName();
				if (needRequestData != null
						&& needRequestData.getContentType().equals("Text")) {
					userTaskDescription += EncodeDecodeRequestData
							.decodeToText(needRequestData.getContent());
				}
			}
			else if (retRequestData.getContentType().equals("BooleanText")) {
				userTask = new UserConfirmWithRetTask(userTaskTime, fromAgentName, goalModelName, elementName);
				
				System.out.println("debug!!!!!!!!!AideAgent handleNewDelegateTask--retRequestData.getName():"+retRequestData.getName());
				
				userTaskDescription = "Please confirm: \n"
						+ retRequestData.getName().split("%")[0];
				if (needRequestData != null
						&& needRequestData.getContentType().equals("Text")) {
					userTaskDescription += EncodeDecodeRequestData
							.decodeToText(needRequestData.getContent());
					((UserConfirmWithRetTask)userTask).setNeedRequestData(needRequestData);
				}
			}

			userTask.setRequestDataName(retRequestData.getName());
		}
		userTask.setDescription(userTaskDescription);
		userCurrentTaskList.add(0, userTask);
		// userTaskList.add(userTask);

		// 新任务广播
		Intent broadcast_nda = new Intent();
		broadcast_nda.setAction("jade.task.NOTIFICATION");
		broadcast_nda.putExtra("Content", userTask.getDescription());
		context.sendBroadcast(broadcast_nda);

	}

	/**
	 * 处理委托任务的返回
	 * 
	 * @param aclmc_DelegateTask
	 */
	private void handleDelegateTaskBack(ACLMC_DelegateTask aclmc_DelegateTask) {
		String fromAgentName = aclmc_DelegateTask.getFromAgentName();
		String goalModelName = aclmc_DelegateTask.getGoalModelName();
		String elementName = aclmc_DelegateTask.getElementName();

		TaskExecutingUtil taskExecutingUtil = taskExecutingUtilList
				.get(goalModelName + "#" + elementName);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String logTime = df.format(new Date());
		String logContent = "";

		if (aclmc_DelegateTask.isDone() == true) { // 被委托对象完成了任务

			logContent += "[" + fromAgentName + "] completed the task <"
					+ goalModelName + "-" + elementName + ">. ";

			SGMMessage sgmMessage = new SGMMessage(
					MesHeader_Mes2Manger.LOCAL_AGENT_MESSAGE, goalModelName,
					null, elementName, new MesBody_Mes2Manager("EndTE"));
			if (aclmc_DelegateTask.getRetRequestData() != null) {
				sgmMessage
						.setRetContent(aclmc_DelegateTask.getRetRequestData());
			}

			// 清空serviceInvocationUtilList里面储存的相关信息清空
			clearTaskExecuting(goalModelName, elementName);

			// 发送消息给manager
			android.util.Log.i("MY_LOG", "Send mes to manager...");
			if (goalModelManager.getMsgPool().offer(sgmMessage)) {
				Log.logMessage(sgmMessage, true);
			} else {
				Log.logMessage(sgmMessage, false);
			}

			Log.logAdaption(goalModelName, elementName,
					"Delegate to people Done. Result: Succeed! From: "
							+ fromAgentName);

		} else {// 被委托对象没有完成任务，继续委托给其他人，也就是向server agent发出请求朋友的信息

			// 必须自己完成的任务
			if (taskExecutingUtil.getTaskLocation().equals("selfLocation")) {
				logContent += "[" + fromAgentName
						+ "] didn't complete the task <" + goalModelName + "-"
						+ elementName + ">. Task failed!";

				taskFail(goalModelName, elementName);
				// 然后会一直等待server agent回复信息，在handleMesFromExternalAgent会对信息进行处理

				Log.logAdaption(goalModelName, elementName, "[" + fromAgentName
						+ "] didn't complete the task <" + goalModelName + "-"
						+ elementName + ">. Task failed!");
			} else {

				logContent += "[" + fromAgentName
						+ "] didn't complete the task <" + goalModelName + "-"
						+ elementName + ">. Continue...";

				obtainFriendsInfoFromServerAgent(goalModelName, elementName);
				// 然后会一直等待server agent回复信息，在handleMesFromExternalAgent会对信息进行处理

				Log.logAdaption(goalModelName, elementName,
						"Delegate to people Done. Result: Failed! From: "
								+ fromAgentName + ". Continue to adaption.");

			}

		}

		UserLog userLog = new UserLog(logTime, logContent);
		goalModelManager.getGoalModelList().get(goalModelName).addUserLog(userLog);
//		userLogList.add(0, userLog);
	}
}
