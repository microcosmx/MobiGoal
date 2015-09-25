/**
 * 
 */
package cn.edu.fudan.se.sgm.goalmodel;

import java.io.Serializable;

/**
 * 目标模型执行过程中用到的数据。</br> 在service与agent之间传递的是content，是字节流
 * 
 * @author whh
 * 
 */
public class RequestData implements Serializable {

	private static final long serialVersionUID = -4140392933528054478L;

	private String name;
	private String contentType; // 数据的类型，比如Image,Text,Voice,ArrayList<String>(type是List)等，在真正用数据时解码用
	private byte[] content; // 储存具体内容的字节流
	

	/**
	 * 构造方法
	 * 
	 * @param name
	 *            这个数据的名字，只是用来标识，没什么具体用处
	 * @param contentType
	 *            数据的类型，比如Image,Text,Voice等，在真正用数据时解码用
	 */
	public RequestData(String name, String contentType) {
		this.name = name;
		this.contentType = contentType;
		this.content = new byte[512];
	}

	public String getName() {
		return name;
	}

	public String getContentType() {
		return contentType;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public void setName(String name) {
		this.name = name;
	}

}
