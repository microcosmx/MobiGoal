/**
 *
 */
package cn.edu.fudan.se.mobigoal.utils;

import java.io.Serializable;

/**
 * @author whh
 */
public class UserLog implements Serializable {

    private String time;
    private String log;

    public UserLog(String time, String log) {
        this.time = time;
        this.log = log;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }
}
