package cn.edu.fudan.se.mobigoal.utils.staticGoal;


import cn.edu.fudan.se.sgm.goalmachine.State;

public class StaticTM extends StaticEM{

	public StaticTM(String name, int level, StaticEM parentGoal,
			State currentState) {
		super(name, level, parentGoal, currentState);
		// TODO Auto-generated constructor stub
	}
	
	public StaticTM(String name, int level, 
			State currentState) {
		super(name, level,currentState);
		// TODO Auto-generated constructor stub
	}

}
