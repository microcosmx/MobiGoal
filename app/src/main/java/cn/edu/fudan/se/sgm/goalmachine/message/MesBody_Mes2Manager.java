package cn.edu.fudan.se.sgm.goalmachine.message;

import java.io.Serializable;


/**
 * agent与manager之间消息的Body部分
 * 
 * @author whh
 * 
 */
public class MesBody_Mes2Manager implements SGMMessage.MesBody, Serializable {

	private static final long serialVersionUID = -2315912253031070781L;
	
	private String body;
	public MesBody_Mes2Manager(String body){
		this.body = body;
	}
	public String getBody() {
		return body;
	}
	
	public String toString(){
		return body;
	}

}
