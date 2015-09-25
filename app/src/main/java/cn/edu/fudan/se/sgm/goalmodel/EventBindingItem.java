/**
 * 
 */
package cn.edu.fudan.se.sgm.goalmodel;

import java.io.Serializable;

/**
 * @author whh
 * 
 */
public class EventBindingItem implements Serializable{

	private ExternalEvent externalEvent;
	private String elementName;

	public EventBindingItem(ExternalEvent externalEvent, String elementName) {
		this.externalEvent = externalEvent;
		this.elementName = elementName;
	}

	public ExternalEvent getExternalEvent() {
		return externalEvent;
	}

	public String getElementName() {
		return elementName;
	}

}
