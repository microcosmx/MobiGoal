/**
 * 
 */
package cn.edu.fudan.se.sgm.goalmodel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import cn.edu.fudan.se.mobigoal.support.BindingCustomItem;
import cn.edu.fudan.se.sgm.contextmanager.CAccountBalance;
import cn.edu.fudan.se.sgm.contextmanager.CAuthorization;
import cn.edu.fudan.se.sgm.contextmanager.CTemperature;
import cn.edu.fudan.se.sgm.contextmanager.CWeather;
import cn.edu.fudan.se.sgm.contextmanager.IContext;
import cn.edu.fudan.se.sgm.goalmachine.Condition;
import cn.edu.fudan.se.sgm.goalmachine.ElementMachine;
import cn.edu.fudan.se.sgm.goalmachine.GoalMachine;
import cn.edu.fudan.se.sgm.goalmachine.TaskMachine;

/**
 * 解析表示一个goal model的xml文件，返回一个<code>GoalModel</code>
 * 
 * @author whh
 * 
 */
public class GmXMLParser {

	Hashtable<String, IContext> contextHashtable = new Hashtable<>();

	public GmXMLParser() {
		// 初始化条件检查时可能用到的上下文
		this.contextHashtable.put("Temperature", new CTemperature());
		this.contextHashtable.put("Weather", new CWeather());
		this.contextHashtable.put("Authorization", new CAuthorization());
		this.contextHashtable.put("AccountBalance", new CAccountBalance());

	}

	public GoalModel newGoalModel(String filename) {
		GoalModel goalModel = new GoalModel();
		initialGoalModelMappingTable(goalModel
				.getDeviceEventMapToExternalEventTable());
		try {
			// 得到DOM解析器的工厂实例
			DocumentBuilderFactory domfac = DocumentBuilderFactory
					.newInstance();
			// 从DOM工厂获得DOM解析器
			DocumentBuilder dombuilder = domfac.newDocumentBuilder();
			// 把要解析的XML文档转化为输入流，以便DOM解析器解析它
			InputStream is = new FileInputStream(filename);
			// 解析XML文档的输入流，得到一个Document
			Document doc = dombuilder.parse(is);
			// 得到XML文档的根节点(books)
			Element root = doc.getDocumentElement();
			// 获得根节点的所有属性名和值
			if (root.getAttributes().getLength() > 0) {
				if (root.getAttributeNode("name") != null) {
					goalModel.setName(root.getAttributeNode("name").getValue());
				}
				if (root.getAttributeNode("description") != null) {
					goalModel.setDescription(root.getAttributeNode(
							"description").getValue());
				}
			}

			// 得到根节点的所有子节点，也就是ElementMachine节点和RequestData节点
			NodeList emNodeList = root.getChildNodes();
			for (int i = 0; i < emNodeList.getLength(); i++) {

				Node emNode = emNodeList.item(i);// 这个node是element machine节点
				// 判断是不是子节点
				if (emNode.getNodeType() == Node.ELEMENT_NODE) {

					// ElementMachine elementMachine;
					if (emNode.getNodeName().equals("ElementMachine")) {

						int id, level = 0;
						String type = "", name = "", description = "";
						// 获得子节点的属性的名和值，也就是id, type, name,level等属性
						if (emNode.getAttributes().getLength() > 0) {
							for (int j = 0; j < emNode.getAttributes()
									.getLength(); j++) {
								switch (emNode.getAttributes().item(j)
										.getNodeName()) {
								case "id":
									id = Integer.parseInt(emNode
											.getAttributes().item(j)
											.getNodeValue());
									break;
								case "type":
									type = emNode.getAttributes().item(j)
											.getNodeValue();
									break;

								case "name":
									name = emNode.getAttributes().item(j)
											.getNodeValue();
									break;

								case "level":
									level = Integer.parseInt(emNode
											.getAttributes().item(j)
											.getNodeValue());
									break;
								case "description":
									description = emNode.getAttributes()
											.item(j).getNodeValue();
									break;

								default:
									break;
								}
							}
						}

						// 获得子节点的所有子节点，也就是decomposition, schedulerMethod,
						// condition等节点
						NodeList propertyNodeList = emNode.getChildNodes();
						// System.err.println(propertyNodeList.getLength());
						String parentGoal = "";
						int decomposition = 0, schedulerMethod = 0, priorityLevel = 0, waitingTimeLimit = 0, retryTimes = 0;

						String executingRequestedServiceName = "", taskExecutingLocation = "";
						Condition preCondition = null, postCondition = null, invariantCondition = null, commitmentCondition = null, contextCondition = null;

						for (int j = 0; j < propertyNodeList.getLength(); j++) {
							Node propertyNode = propertyNodeList.item(j);

							String conditionType = "", conditionValueType = "", conditionLeftValue = "", conditionOperator = "", conditionRightValue = "";
							boolean canRepairing = false;
							String conditionDescription = "";

							if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
								// System.err.println(propertyNode.getNodeName());
								// System.err.println(propertyNode.getTextContent());
								switch (propertyNode.getNodeName()) {
								case "parentGoal":
									parentGoal = propertyNode.getTextContent();
									break;
								case "decomposition":
									decomposition = Integer
											.parseInt(propertyNode
													.getTextContent());
									break;
								case "schedulerMethod":
									schedulerMethod = Integer
											.parseInt(propertyNode
													.getTextContent());
									break;
								case "priorityLevel":
									priorityLevel = Integer
											.parseInt(propertyNode
													.getTextContent());
									break;
								case "retryTimes":
									retryTimes = Integer.parseInt(propertyNode
											.getTextContent());
									break;
								case "executingRequestedServiceName":
									executingRequestedServiceName = propertyNode
											.getTextContent();
									break;
								case "executingLocation":
									taskExecutingLocation = propertyNode
											.getTextContent();
									break;

								case "waitingTimeLimit":
									waitingTimeLimit = Integer
											.parseInt(propertyNode
													.getTextContent());
									break;

								case "Condition":
									// propertyNode又是一个有自己属性和子节点的节点
									// 先获得它的属性
									if (propertyNode.getAttributes()
											.getLength() > 0) {
										for (int x = 0; x < propertyNode
												.getAttributes().getLength(); x++) {
											switch (propertyNode
													.getAttributes().item(x)
													.getNodeName()) {
											case "type":
												conditionType = propertyNode
														.getAttributes()
														.item(x).getNodeValue();
												break;
											case "valueType":
												conditionValueType = propertyNode
														.getAttributes()
														.item(x).getNodeValue();
												break;
											case "leftValueDes":
												conditionLeftValue = propertyNode
														.getAttributes()
														.item(x).getNodeValue();
												break;
											case "operator":
												conditionOperator = propertyNode
														.getAttributes()
														.item(x).getNodeValue();
												break;
											case "rightValue":
												conditionRightValue = propertyNode
														.getAttributes()
														.item(x).getNodeValue();
												break;

											}

										}
									}

									// 获得context节点的子节点
									NodeList conditionNodeList = propertyNode
											.getChildNodes();
									for (int y = 0; y < conditionNodeList
											.getLength(); y++) {
										Node conditionNode = conditionNodeList
												.item(y);
										if (conditionNode.getNodeType() == Node.ELEMENT_NODE) {

											if (conditionNode.getNodeName()
													.equals("canRepairing")) {
												canRepairing = Boolean
														.parseBoolean(conditionNode
																.getTextContent());
											}
											if (conditionNode.getNodeName()
													.equals("description")) {
												conditionDescription = conditionNode
														.getTextContent();
											}

										}
									}

									// new不同的condition
									switch (conditionType) {
									case "CONTEXT":
										contextCondition = new Condition(
												conditionType,
												conditionValueType,
												conditionLeftValue,
												conditionOperator,
												conditionRightValue);
										contextCondition
												.setContextHashtable(contextHashtable);
										contextCondition
												.setDescription(conditionDescription);
										break;
									case "PRE":
										preCondition = new Condition(
												conditionType,
												conditionValueType,
												conditionLeftValue,
												conditionOperator,
												conditionRightValue,
												canRepairing);
										preCondition
												.setContextHashtable(contextHashtable);
										preCondition
												.setDescription(conditionDescription);
										break;
									case "POST":
										postCondition = new Condition(
												conditionType,
												conditionValueType,
												conditionLeftValue,
												conditionOperator,
												conditionRightValue);
										postCondition
												.setContextHashtable(contextHashtable);
										postCondition
												.setDescription(conditionDescription);
										break;
									case "COMMITMENT":
										commitmentCondition = new Condition(
												conditionType,
												conditionValueType,
												conditionLeftValue,
												conditionOperator,
												conditionRightValue);
										commitmentCondition
												.setContextHashtable(contextHashtable);
										commitmentCondition
												.setDescription(conditionDescription);
										break;
									case "INVARIANT":
										invariantCondition = new Condition(
												conditionType,
												conditionValueType,
												conditionLeftValue,
												conditionOperator,
												conditionRightValue);
										invariantCondition
												.setContextHashtable(contextHashtable);
										invariantCondition
												.setDescription(conditionDescription);
										break;

									}

									break; // end case "Condition":

								default:
									break;
								}
							}
						}

						// 实例化一个element machine，根据不同的type实例不同的goal machine或者task
						// machine
						ElementMachine elementMachine = null;
						// 先找到对应的parent element machine，root goal的parent可以是null
						GoalMachine parentElementMachine = (GoalMachine) getElementMachineByName(
								goalModel.getElementMachines(), parentGoal);
						if (type.equals("GoalMachine")) {
							elementMachine = new GoalMachine(name,
									decomposition, schedulerMethod,
									parentElementMachine, level);

						} else if (type.equals("TaskMachine")) {
							elementMachine = new TaskMachine(name,
									parentElementMachine, level,
									executingRequestedServiceName);
							// // 不需要人的参与，而是需要调用服务的
							// if (!needPeopleInteraction
							// && executingRequestedServiceName != "") {
							// ((TaskMachine) elementMachine)
							// .setExecutingRequestedServiceName(executingRequestedServiceName);
							// }
							if (!taskExecutingLocation.equals("")) {
								((TaskMachine) elementMachine)
										.setExecutingLocation(taskExecutingLocation);
							}
						}

						elementMachine.setDescription(description);
						if (waitingTimeLimit != 0) {
							elementMachine
									.setWaitingTimeLimit(waitingTimeLimit);
						}
						// if (retryTimes != 0) {
						// elementMachine.setRetryTimes(retryTimes);
						// }

						// 设置各种condition
						if (preCondition != null) {
							elementMachine.setPreCondition(preCondition);
						}
						if (postCondition != null) {
							elementMachine.setPostCondition(postCondition);
						}
						if (invariantCondition != null) {
							elementMachine
									.setInvariantCondition(invariantCondition);
						}
						if (commitmentCondition != null) {
							elementMachine
									.setCommitmentCondition(commitmentCondition);
						}
						if (contextCondition != null) {
							elementMachine
									.setContextCondition(contextCondition);
						}

						if (parentElementMachine != null) {
							parentElementMachine.addSubElement(elementMachine,
									priorityLevel);
						}
						goalModel.addElementMachine(elementMachine);
					}

					// request data节点
					if (emNode.getNodeName().equals("RequestData")) {
						// 获得子节点的属性的名和值，也就是name, from, to,contentType等属性
						String name = "", from = "", to = "", contentType = "";
						if (emNode.getAttributes().getLength() > 0) {
							for (int j = 0; j < emNode.getAttributes()
									.getLength(); j++) {
								switch (emNode.getAttributes().item(j)
										.getNodeName()) {
								case "name":
									name = emNode.getAttributes().item(j)
											.getNodeValue();
									break;
								case "from":
									from = emNode.getAttributes().item(j)
											.getNodeValue();
									break;

								case "to":
									to = emNode.getAttributes().item(j)
											.getNodeValue();
									break;

								case "contentType":
									contentType = emNode.getAttributes()
											.item(j).getNodeValue();

								}
							}
						}
						RequestData requestData = new RequestData(name,
								contentType);
						goalModel.getAssignmentHashtable().put(from,
								requestData);
						goalModel.getParameterMapHashtable().put(to, from);
					}

					// EventBinding节点
					if (emNode.getNodeName().equals("EventBinding")) {
						android.util.Log.i("MY_LOG",
								"-------GmXMLParser--EventBingding!!");
						// 获得子节点的属性的名和值，也就是name, from, to,contentType等属性
						String device = "", external = "", element = "";
						if (emNode.getAttributes().getLength() > 0) {
							for (int j = 0; j < emNode.getAttributes()
									.getLength(); j++) {
								switch (emNode.getAttributes().item(j)
										.getNodeName()) {
								case "device":
									device = emNode.getAttributes().item(j)
											.getNodeValue();
									break;
								case "external":
									external = emNode.getAttributes().item(j)
											.getNodeValue();
									break;
								case "element":
									element = emNode.getAttributes().item(j)
											.getNodeValue();
									break;

								}
							}
						}
						// 将对应的绑定注册添加到goal model相关的table中
						EventBindingItem eventBindingItem = null;
						// if (element.equals("")) {
						// eventBindingItem = new EventBindingItem(
						// ExternalEvent.getExternalEvent(external),
						// null);
						// } else {
						eventBindingItem = new EventBindingItem(
								ExternalEvent.getExternalEvent(external),
								element);
						// }
						goalModel.getDeviceEventMapToExternalEventTable().put(
								device, eventBindingItem);

					}

				}
			}

		} catch (Exception e) {
			System.err.println("GmXMLParser new goal model error!!!!");
			e.printStackTrace();
		}

		// 设置root goal
		if (goalModel.getElementMachines().size() > 0) {
			goalModel.setRootGoal((GoalMachine) goalModel.getElementMachines()
					.get(0));
		}

		return goalModel;
	}

	/**
	 * 根据name在一个element machine列表中找到对应的元素
	 * 
	 * @param elementMachines
	 *            列表
	 * @param name
	 *            名字
	 * @return element machine
	 */
	private ElementMachine getElementMachineByName(
			ArrayList<ElementMachine> elementMachines, String name) {

		ElementMachine em = null;
		if ((name != null && name != "")
				&& (elementMachines != null && elementMachines.size() != 0)) {
			for (ElementMachine elementMachine : elementMachines) {
				if (elementMachine.getName().equals(name)) {
					em = elementMachine;
					break;
				}
			}
		}

		return em;
	}

	/**
	 * 
	 * @param table
	 */
	private void initialGoalModelMappingTable(
			Hashtable<String, EventBindingItem> table) {
		table.put("StartGM", new EventBindingItem(ExternalEvent.startGM, null));
		table.put("StopGM", new EventBindingItem(ExternalEvent.stopGM, null));
		table.put("SuspendGM", new EventBindingItem(ExternalEvent.suspendGM,
				null));
		table.put("ResumeGM",
				new EventBindingItem(ExternalEvent.resumeGM, null));
		table.put("ResetGM", new EventBindingItem(ExternalEvent.resetGM, null));
		table.put("EndTE", new EventBindingItem(ExternalEvent.endTE, null));
		table.put("QuitTE", new EventBindingItem(ExternalEvent.quitTE, null));
		table.put("ServiceExecutingDone", new EventBindingItem(
				ExternalEvent.serviceExecutingDone, null));
		table.put("ServiceExecutingFailed", new EventBindingItem(
				ExternalEvent.serviceExecutingFailed, null));
	}

	public static void editGoalModel(String filePath,
			HashMap<String, Integer> toCustom) {
		try {
			// 得到DOM解析器的工厂实例
			DocumentBuilderFactory domfac = DocumentBuilderFactory
					.newInstance();
			// 从DOM工厂获得DOM解析器
			DocumentBuilder dombuilder = domfac.newDocumentBuilder();
			// 把要解析的XML文档转化为输入流，以便DOM解析器解析它
			InputStream is = new FileInputStream(filePath);
			// 解析XML文档的输入流，得到一个Document
			Document doc = dombuilder.parse(is);
			// 得到XML文档的根节点(books)
			Element root = doc.getDocumentElement();
			// 获得根节点的所有属性名和值

			// 得到根节点的所有子节点，也就是ElementMachine节点和RequestData节点
			NodeList emNodeList = root.getChildNodes();
			for (int i = 0; i < emNodeList.getLength(); i++) {

				Node emNode = emNodeList.item(i);// 这个node是element machine节点
				// 判断是不是子节点
				if (emNode.getNodeType() == Node.ELEMENT_NODE) {

					// ElementMachine elementMachine;
					if (emNode.getNodeName().equals("ElementMachine")) {
						String name = emNode.getAttributes()
								.getNamedItem("name").getNodeValue();

						if (toCustom.get(name) != null) {
							// 获得子节点的所有子节点，也就是decomposition, schedulerMethod,
							// condition等节点
							NodeList propertyNodeList = emNode.getChildNodes();
							for (int j = 0; j < propertyNodeList.getLength(); j++) {
								Node propertyNode = propertyNodeList.item(j);
								if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
									if (propertyNode.getNodeName().equals(
											"priorityLevel")) {
										propertyNode.setTextContent(String
												.valueOf(toCustom.get(name)));
										propertyNode.setNodeValue(String
												.valueOf(toCustom.get(name)));
										// Log.i("MY_LOG", "EditXML: name: " +
										// name + ", priority: " +
										// propertyNode.getTextContent());
									}
								}
							}
						}

					}
				}

			}

			// 保存修改后的文件
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();

			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(filePath));
			transformer.transform(source, result);

		} catch (Exception e) {
			System.err.println("GmXMLParser edit goal model error!!!!");
			e.printStackTrace();
		}
	}

	public static void editGoalModelBinding(String filePath,
			ArrayList<BindingCustomItem> bindingCustomList) {
		try {
			// 得到DOM解析器的工厂实例
			DocumentBuilderFactory domfac = DocumentBuilderFactory
					.newInstance();
			// 从DOM工厂获得DOM解析器
			DocumentBuilder dombuilder = domfac.newDocumentBuilder();
			// 把要解析的XML文档转化为输入流，以便DOM解析器解析它
			InputStream is = new FileInputStream(filePath);
			// 解析XML文档的输入流，得到一个Document
			Document doc = dombuilder.parse(is);
			// 得到XML文档的根节点(books)
			Element root = doc.getDocumentElement();
			// 获得根节点的所有属性名和值

			// 得到根节点的所有子节点，也就是ElementMachine节点和RequestData节点
			NodeList emNodeList = root.getChildNodes();
			for (int i = 0; i < emNodeList.getLength(); i++) {

				Node emNode = emNodeList.item(i);// 这个node是element machine节点
				// 判断是不是子节点
				if (emNode.getNodeType() == Node.ELEMENT_NODE) {

					// ElementMachine elementMachine;
					if (emNode.getNodeName().equals("EventBinding")) {
						String device = emNode.getAttributes()
								.getNamedItem("device").getNodeValue();
						String external = emNode.getAttributes()
								.getNamedItem("external").getNodeValue();
						String element = "";
						if (emNode.getAttributes().getNamedItem("element") != null) {
							element = emNode.getAttributes()
									.getNamedItem("element").getNodeValue();
						}
						if (device.contains("Time")) {
							for (BindingCustomItem item : bindingCustomList) {
								if (item.getExternal().equals(external)
										&& item.getElement().equals(element)) {
									emNode.getAttributes()
											.getNamedItem("device")
											.setNodeValue(
													"Time" + item.getTime());
								}
							}
						} else if (device.contains("Phone")) {
							for (BindingCustomItem item : bindingCustomList) {
								if (item.getExternal().equals(external)
										&& item.getElement().equals(element)) {
									emNode.getAttributes()
											.getNamedItem("device")
											.setNodeValue(
													"Phone" + item.getPhone());
								}
							}
						}
						// if (toCustom.get(name) != null) {
						// if (emNode.getAttributes().getNamedItem("device")
						// .getNodeValue().contains("Time")) {
						// emNode.getAttributes()
						// .getNamedItem("device")
						// .setNodeValue(
						// "Time" + toCustom.get(name));
						// }
						// }

					}
				}

			}

			// 保存修改后的文件
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();

			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(filePath));
			transformer.transform(source, result);

		} catch (Exception e) {
			System.err.println("GmXMLParser edit goal model error!!!!");
			e.printStackTrace();
		}
	}
}
