/**
 * 
 */
package cn.edu.fudan.se.servicestub.sync.webservice;

/**
 * 授权检查
 * @author whh
 *
 */
public class ClientAuthorization {

	public static String agentNickName;

	public static boolean isAuthorized(){
		//调用web service进行检查
		System.out.println("ClientAuthorization--agentNickName: " + agentNickName);
		if (agentNickName!=null)
			return true;
		else
			return false;

	}
}
