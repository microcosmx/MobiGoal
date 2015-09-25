package cn.edu.fudan.se.agent.main;


import cn.edu.fudan.se.agent.support.ACLMC_DelegateTask;
import cn.edu.fudan.se.sgm.goalmachine.message.SGMMessage;

public interface AideAgentInterface {

	public void sendMesToManager(SGMMessage msg);

	public void handleMesFromManager(SGMMessage msg);

	public void sendMesToExternalAgent(ACLMC_DelegateTask aclmc_DelegateTask);
	
	public void handleMesFromService(SGMMessage msg);
	
	public void sendLocationToServerAgent(String userLocation);
	

}
