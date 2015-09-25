/**
 * 
 */
package cn.edu.fudan.se.sgm.contextmanager;


import java.io.Serializable;

import cn.edu.fudan.se.servicestub.sync.webservice.ClientAccountBalance;

/**
 * @author whh
 * 
 */
public class CAccountBalance implements IContext,Serializable {

	@Override
	public Object getValue() {
		return ClientAccountBalance.getAccountBalance();
	}

}
