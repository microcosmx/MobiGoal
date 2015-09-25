/**
 * 
 */
package cn.edu.fudan.se.sgm.goalmodel;

/**
 * @author whh
 * 
 */
public enum ExternalEvent {
	startGM, stopGM, suspendGM, resumeGM, resetGM, endTE, quitTE,
	serviceExecutingDone, serviceExecutingFailed;

	public static ExternalEvent getExternalEvent(String string) {
		switch (string) {
		case "startGM":
			return ExternalEvent.startGM;
		case "stopGM":
			return ExternalEvent.startGM;
		case "suspendGM":
			return ExternalEvent.suspendGM;
		case "resumeGM":
			return ExternalEvent.resumeGM;
		case "resetGM":
			return ExternalEvent.resetGM;
		case "endTE":
			return ExternalEvent.endTE;
		case "quitTE":
			return ExternalEvent.quitTE;
//		case "quitGM":
//			return ExternalEvent.quitGM;
		case "serviceExecutingDone":
			return ExternalEvent.serviceExecutingDone;
		case "serviceExecutingFailed":
			return ExternalEvent.serviceExecutingFailed;
//		case "delegatedAchieved":
//			return ExternalEvent.delegatedAchieved;
//		case "delegatedFailed":
//			return ExternalEvent.delegatedFailed;

		default:
			return null;
		}
	}
}
