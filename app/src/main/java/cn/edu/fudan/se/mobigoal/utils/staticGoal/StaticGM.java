package cn.edu.fudan.se.mobigoal.utils.staticGoal;


import cn.edu.fudan.se.sgm.goalmachine.State;

public class StaticGM extends StaticEM{
	
	private int decomposition; // 分解，0表示AND分解，1表示OR分解
//	private int schedulerMethod; // AND分解情况下的子目标执行顺序，0表示并行处理，1表示串行
	
	public StaticGM(String name, int level, StaticEM parentGoal,
			State currentState,int decomposition) {
		super(name, level, parentGoal, currentState);
		this.setDecomposition(decomposition);
//		this.schedulerMethod = schedulerMethod;
	}
	
	public StaticGM(String name, int level,
			State currentState,int decomposition) {
		super(name, level, currentState);
		this.setDecomposition(decomposition);
//		this.schedulerMethod = schedulerMethod;
	}

	public int getDecomposition() {
		return decomposition;
	}

	public void setDecomposition(int decomposition) {
		this.decomposition = decomposition;
	}
}
