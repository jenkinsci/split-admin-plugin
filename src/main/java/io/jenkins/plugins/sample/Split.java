package io.jenkins.plugins.sample;

public class Split {
    public String splitName;
    public Treatments[] treatments;
    public DefaultRule[] defaultRule;
	 
	    // Constructors, Getters, Setters and toString	
    public String getSplitName() {
    	return this.splitName;
    }
    
    public void setSplitName(String splitName) {
    	this.splitName = splitName;
    }

    public Treatments[] getTreatments() {
        return treatments;
    }
    public void setTreatments(Treatments[] treatments) {
        this.treatments = treatments;
    }
    
    public DefaultRule[] getDefaultRule() {
    	return this.defaultRule;
    }
    
    public void setDefaultRule(DefaultRule[] defaultRule) {
    	this.defaultRule = defaultRule;
    }
    

}
