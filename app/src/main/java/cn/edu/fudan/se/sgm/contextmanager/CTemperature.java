/**
 * 
 */
package cn.edu.fudan.se.sgm.contextmanager;

import java.io.Serializable;

import cn.edu.fudan.se.servicestub.sync.webservice.ClientTemperature;

/**
 * 温度上下文
 * 
 * @author whh
 * 
 */
public class CTemperature implements IContext,Serializable {

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.fudan.se.contextmanager.IContext#getValue()
	 */
	@Override
	public Object getValue() {
		return ClientTemperature.getTemperature();
	}

}
