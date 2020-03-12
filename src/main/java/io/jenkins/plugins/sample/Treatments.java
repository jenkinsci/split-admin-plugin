package io.jenkins.plugins.sample;

public class Treatments {
    public String treatmentName;
    public String[] whitelistKeys;
    public String dynamicConfig;

    public String getTreatmentName() {
    	return this.treatmentName;
    }
    
    public void setTreatmentName(String treatmentName) {
    	this.treatmentName = treatmentName;
    }

    public String getDynamicConfig() {
    	return this.dynamicConfig;
    }
    
    public void setDynamicConfig(String dynamicConfig) {
    	this.dynamicConfig = dynamicConfig;
    }

    public String[] getWhitelistKeys() {
        return whitelistKeys;
    }
    public void setWhitelistKeys(String[] whitelistKeys) {
        this.whitelistKeys = whitelistKeys;
    }
}
