/**
 * 
 */
package cn.edu.fudan.se.mobigoal.support;

/**
 * @author whh
 *
 */
public class BindingCustomItem {
	private String device;
	private String external;
	private String element;

	private String time;
	private String phone;

	public BindingCustomItem(String device, String external, String element,
			String originalTime, String originalPhone) {
		this.device = device;
		this.external = external;
		this.element = element;
		this.time = originalTime;
		this.phone = originalPhone;
	}

	public String getDevice() {
		return device;
	}

	public String getExternal() {
		return external;
	}

	public String getElement() {
		return element;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}
}
