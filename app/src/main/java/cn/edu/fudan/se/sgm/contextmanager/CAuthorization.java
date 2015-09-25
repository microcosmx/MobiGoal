/**
 * 
 */
package cn.edu.fudan.se.sgm.contextmanager;


import java.io.Serializable;

import cn.edu.fudan.se.servicestub.sync.webservice.ClientAuthorization;

/**
 * 授权检查
 * @author whh
 *
 */
public class CAuthorization implements IContext,Serializable {

	@Override
	public Object getValue() {
		return ClientAuthorization.isAuthorized();
	}

}
