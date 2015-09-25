/**
 * 
 */
package cn.edu.fudan.se.sgm.contextmanager;


/**
 * 上下文管理，使用策略模式来实现
 * 
 * @author whh
 * 
 */
public class ContextManager {
	
//	public static Context applicationContext;
	
	private IContext context;

	public ContextManager(IContext context) {
		this.context = context;
	}

	public Object getValue() {
		return this.context.getValue();
	}
}
