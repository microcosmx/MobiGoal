/**
 *
 */
package cn.edu.fudan.se.sgm.goalmachine;


import java.io.Serializable;
import java.util.Hashtable;

import cn.edu.fudan.se.sgm.contextmanager.ContextManager;
import cn.edu.fudan.se.sgm.contextmanager.IContext;

/**
 * 状态机中需要检查的条件Condition,具体类型有：CONTEXT,PRE,POST,COMMITMENT,INVARIANT
 *
 * @author whh
 */
public class Condition implements Serializable {

    private String type; // 条件类型，具体有CONTEXT,PRE,POST,COMMITMENT,INVARIANT
    private boolean satisfied = true; // 条件是否被满足，true为被满足
    private boolean waitable; // 这个条件是否可以通过等待，重新改变是否满足的状态，只有pre condition需要设置

    private String valueType;

    private String leftValueDes;
    private String operator;
    private String rightValue;

    private String description;

    private Hashtable<String, IContext> contextHashtable;

    public Condition(String type, String valueType, String leftValueDes,
                     String operator, String rightValue) {
        this.type = type;
        this.valueType = valueType;
        this.leftValueDes = leftValueDes;
        this.operator = operator;
        this.rightValue = rightValue;
    }

    public Condition(String type, String valueType, String leftValueDes,
                     String operator, String rightValue, boolean waitable) {
        this.type = type;
        this.valueType = valueType;
        this.leftValueDes = leftValueDes;
        this.operator = operator;
        this.rightValue = rightValue;
        this.waitable = waitable;
    }

    public String getType() {
        return type;
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    public void setSatisfied(boolean satisfied) {
        this.satisfied = satisfied;
    }

    public boolean isWaitable() {
        return waitable;
    }

    public Hashtable<String, IContext> getContextHashtable() {
        return contextHashtable;
    }

    public void setContextHashtable(Hashtable<String, IContext> contextHashtable) {
        this.contextHashtable = contextHashtable;
    }

    public String getRightValue() {
        return rightValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void check() {
        System.out.println("---------check condition start!-------type: "
                + this.type + ", valueType: " + this.valueType
                + ", leftValueDes: " + this.leftValueDes);

        ContextManager contextManager = new ContextManager(
                this.contextHashtable.get(this.leftValueDes));

        switch (this.valueType) {
            case "Int":
                int leftValueInt = (int) contextManager.getValue();
                switch (this.operator) {
                    case "BIGGERTHAN":
                        this.satisfied = (leftValueInt > Integer
                                .parseInt(this.rightValue));
                        break;
                    case "EQUAL":
                        this.satisfied = (leftValueInt == Integer
                                .parseInt(this.rightValue));
                        break;
                    case "SMALLERTHAN":
                        this.satisfied = (leftValueInt < Integer
                                .parseInt(this.rightValue));
                        break;
                }
                break;

            case "Double":
                double leftValuedouble = (double) contextManager.getValue();
                switch (this.operator) {
                    case "BIGGERTHAN":
                        this.satisfied = (leftValuedouble > Double
                                .parseDouble(this.rightValue));
                        break;
                    case "EQUAL":
                        this.satisfied = (leftValuedouble == Double
                                .parseDouble(this.rightValue));
                        break;
                    case "SMALLERTHAN":
                        this.satisfied = (leftValuedouble < Double
                                .parseDouble(this.rightValue));
                        break;
                }
                break;

            case "Boolean":
                boolean leftValueBoolean = (boolean) contextManager.getValue();
                switch (this.operator) {
                    case "EQUAL":
                        this.satisfied = (leftValueBoolean == Boolean
                                .parseBoolean(this.rightValue));
                        break;
                }
                break;

            case "String":
                String leftValueString = (String) contextManager.getValue();
                switch (this.operator) {
                    case "EQUAL":
                        this.satisfied = (leftValueString.equals(this.rightValue));
                        break;
                    case "NOTEQUAL":
                        this.satisfied = (!leftValueString.equals(this.rightValue));
                        break;
                }
                break;
        }

        System.out.println("---------check condition done!-------isSatisfied: "
                + this.isSatisfied());
    }


}
