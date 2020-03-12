package io.jenkins.plugins.sample;

public class DefaultRule {
    public String ruleTreatment;
    public int percentage;

    public String getRuleTreatment() {
    	return this.ruleTreatment;
    }
    
    public void setRuleTreatment(String ruleTreatment) {
    	this.ruleTreatment = ruleTreatment;
    }

    public int getPercentage() {
    	return this.percentage;
    }
    
    public void setPercentage(int percentage) {
    	this.percentage = percentage;
    }

}
