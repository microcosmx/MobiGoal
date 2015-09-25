package cn.edu.fudan.se.mobigoal.utils.staticGoal;


import cn.edu.fudan.se.sgm.goalmachine.ElementMachine;
import cn.edu.fudan.se.sgm.goalmachine.GoalMachine;
import cn.edu.fudan.se.sgm.goalmachine.State;
import cn.edu.fudan.se.sgm.goalmachine.TaskMachine;

public class StaticEM {
	
	private int level; // 这个主要是在安卓界面显示目标树的时候用的，指这个element处在第几层，root goal为0层
	private String name; // element的名字
	private StaticEM parentGoal; // 当前element的父目标，除root
										// goal外每个element都有parentGoal
	private State currentState; // element目前所处的状态
	
	public StaticEM(String name, int level, StaticEM parentGoal, State currentState){
		this.name = name;
		this.level = level;
		this.parentGoal = parentGoal;
		this.currentState = currentState;
	}
	
	public StaticEM(String name, int level,  State currentState){
		this.name = name;
		this.level = level;
		this.currentState = currentState;
	}
	
	public static StaticEM getStaticEMFromEM(ElementMachine em){
		StaticEM staticEM = null;
		if (em != null) {
			if (em instanceof GoalMachine) {
				staticEM = new StaticGM(em.getName(), em.getLevel(), em.getCurrentState(), ((GoalMachine) em).getDecomposition());
				staticEM.setParentGoal(getStaticEMFromEM(em.getParentGoal()));
			}else if (em instanceof TaskMachine) {
				staticEM = new StaticTM(em.getName(), em.getLevel(), em.getCurrentState());
				staticEM.setParentGoal(getStaticEMFromEM(em.getParentGoal()));
			}
		}
		
		return staticEM;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public StaticEM getParentGoal() {
		return parentGoal;
	}

	public void setParentGoal(StaticEM parentGoal) {
		this.parentGoal = parentGoal;
	}

	public State getCurrentState() {
		return currentState;
	}

	public void setCurrentState(State currentState) {
		this.currentState = currentState;
	}
}
