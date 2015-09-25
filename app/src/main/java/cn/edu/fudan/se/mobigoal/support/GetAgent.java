/**
 * 
 */
package cn.edu.fudan.se.mobigoal.support;

import cn.edu.fudan.se.agent.main.AideAgentInterface;
import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import jade.core.MicroRuntime;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

/**
 * @author whh
 *
 */
public class GetAgent {
	/**
	 * 获得agent引用
	 * @param sgmApplication application上下文
	 * @return agent接口
	 */
	public static AideAgentInterface getAideAgentInterface(
			SGMApplication sgmApplication) {
		AideAgentInterface aideAgentInterface = null; // agent interface
		try {
			aideAgentInterface = MicroRuntime.getAgent(
					sgmApplication.getAgentNickname()).getO2AInterface(
					AideAgentInterface.class);
		} catch (StaleProxyException e) {
			e.printStackTrace();
		} catch (ControllerException e) {
			e.printStackTrace();
		}
		return aideAgentInterface;
	}
}
