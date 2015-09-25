/**
 * 
 */
package cn.edu.fudan.se.sgm.goalmachine.message;

import java.io.Serializable;

import cn.edu.fudan.se.sgm.goalmodel.RequestData;


/**
 * 实现线程间的通信的消息格式，是可序列化对象
 * 
 * @author whh
 * 
 */
public class SGMMessage implements Serializable {

	private static final long serialVersionUID = -1901699789736351108L;

	private MesHeader header; // 消息头部
	private String goalModelName;
	private String fromElementName;
	private String toElementName;
	private MesBody body; // 消息主体

	private String abstractServiceName;
	private String taskDescription; // 消息附加的描述
	private String taskLocation;
	
	private RequestData needContent; // 执行任务时需要传入的数据
	private RequestData retContent; // 这个任务执行完毕后会返回的数据

	public SGMMessage(MesHeader header, String goalModelName,
			String fromElementName, String toElementName, MesBody body) {
		this.header = header;
		this.goalModelName = goalModelName;
		this.fromElementName = fromElementName;
		this.toElementName = toElementName;
		this.body = body;
		this.needContent = null;
		this.retContent = null;
	}

	public MesBody getBody() {
		return body;
	}

	public void setBody(MesBody body) {
		this.body = body;
	}

	public MesHeader getHeader() {
		return header;
	}

	public String getGoalModelName() {
		return goalModelName;
	}

	public String getFromElementName() {
		return fromElementName;
	}

	public String getToElementName() {
		return toElementName;
	}

	public RequestData getNeedContent() {
		return needContent;
	}

	public void setNeedContent(RequestData needContent) {
		this.needContent = needContent;
	}

	public RequestData getRetContent() {
		return retContent;
	}

	public void setRetContent(RequestData retContent) {
		this.retContent = retContent;
	}

	public String getAbstractServiceName() {
		return abstractServiceName;
	}

	public void setAbstractServiceName(String abstractServiceName) {
		this.abstractServiceName = abstractServiceName;
	}

	public String getTaskDescription() {
		return taskDescription;
	}

	public void setTaskDescription(String taskDescription) {
		this.taskDescription = taskDescription;
	}

	public String getTaskLocation() {
		return taskLocation;
	}

	public void setTaskLocation(String taskLocation) {
		this.taskLocation = taskLocation;
	}

	/**
	 * 消息头部
	 * 
	 * @author whh
	 * 
	 */
	public interface MesHeader {

	}

	/**
	 * @author whh
	 * 
	 */
	public interface MesBody {
		
		public String toString();
	}

}
