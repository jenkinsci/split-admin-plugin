package io.jenkins.plugins.sample;

public class Treatments {
    private String treatment;
    private String[] keys;
    private String config;
    private int percentage;

    public Treatments() {
        this.treatment = null;
        this.keys = null;
        this.config = null;
        this.percentage = 0;
    }

    
    public String getTreatment() {
    	return this.treatment;
    }
    
    public void setTreatment(String treatment) {
    	this.treatment = treatment;
    }

    public String getConfig() {
    	return this.config;
    }
    
    public void setConfig(String config) {
    	this.config = config;
    }

    public String[] getKeys() {
        return this.keys == null ? null : (String[]) this.keys.clone();
    }
    
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value="EI_EXPOSE_REP2",
    justification="I know what I'm doing")
    public void setKeys(String[] keys) {
        this.keys = keys;
    }
    
    public int getPercentage() {
    	return this.percentage;
    }
    
    public void setPercentage(int percentage) {
    	this.percentage = percentage;
    }

}
