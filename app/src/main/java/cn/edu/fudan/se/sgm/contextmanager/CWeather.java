/**
 * 
 */
package cn.edu.fudan.se.sgm.contextmanager;


import java.io.Serializable;

import cn.edu.fudan.se.servicestub.sync.webservice.ClientWeather;

/**
 * 天气上下文，也就是是晴天（SUNNY）还是下雨（RAINY）
 * 
 * @author whh
 * 
 */
public class CWeather implements IContext,Serializable {

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.fudan.se.contextmanager.IContext#getValue()
	 */
	@Override
	public Object getValue() {
		return ClientWeather.getWeather();
	}

}
