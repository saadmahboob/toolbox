package eu.amidst.core.header.statics;

import java.util.*;

/**
 * Created by afa on 02/07/14.
 */
public final class StaticDataHeader {
    private List<Variable> vars;

    public List<Variable> getObservedVariables() {
        return vars;
    }

    public void setObservedVariables(List<Variable> vars) {
        this.vars = vars;
    }

    public int getNumberOfObservableVariables() {
        return vars.size();
    }

    public Variable addObservedVariable(int dataPosition, String name, int numberOfStates) {
        VariableImplementation var = new VariableImplementation(name);
        var.setNumberOfStates(numberOfStates);
        var.setObservable(true);
        var.setVarID(dataPosition);
        vars.add(dataPosition,var);
        return var;
    }
    private final class VariableImplementation implements Variable {
        private String name;
        private int varID;
        private boolean observable;
        private int numberOfStates;
        private boolean isLeave = false;


        public VariableImplementation(String name) {
            this.name = new String(name);
        }

        public String getName() {
            return this.name;
        }

        public int getVarID() {
            return varID;
        }

        private void setVarID(int id) {
            this.varID = id;
        }

        public void setObservable(boolean observable) { this.observable=observable; }

        public boolean isObservable() {
            return false;
        }

        public int getNumberOfStates() {
            return numberOfStates;
        }

        public void setNumberOfStates(int numberOfStates) {
            this.numberOfStates = numberOfStates;
        }

        public boolean isLeave() {
            return this.isLeave;
        }

        public void setLeave(boolean isLeave) {
            this.isLeave = isLeave;
        }

        public boolean isContinuous(){
            return this.numberOfStates==0;
        }

    }

}