/**
 * 
 */
package cn.edu.fudan.se.sgm.contextmanager;

/**
 * 返回一个上下文的值
 * 
 * @author whh
 * 
 */
public interface IContext{

	/**
	 * 返回一个上下文的值
	 * 
	 * @return 不同种类的上下文的值
	 */
	public Object getValue();
}
