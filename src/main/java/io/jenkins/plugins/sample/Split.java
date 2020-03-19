package io.jenkins.plugins.sample;

import java.util.ArrayList;
import java.util.List;

public class Split {
    private Treatments[] treatments;
    private String splitName;

    public Split() {
        this.treatments = null;
        this.splitName = null;
    }

    public Treatments[] getTreatments() {
        return this.treatments == null ? null : (Treatments[]) this.treatments.clone();
    }
    
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value="EI_EXPOSE_REP2",
    justification="I know what I'm doing")
    public void setTreatments(Treatments[] treatments) {
    	this.treatments = treatments;
    }

    public void addTreatment(Treatments newTreatment) {
    	List<Treatments> tempTreatments = new ArrayList<Treatments>();
    	if (this.treatments!=null) {
        	for (Treatments t : this.treatments) { 
        		tempTreatments.add(t); 
            }    		
    	}
    	tempTreatments.add(newTreatment);
    	this.treatments = tempTreatments.toArray(new Treatments[0]);
    }

    public String getSplitName() {
    	return this.splitName;
    }

    public void setSplitName(String splitName) {
    	this.splitName = splitName;
    }

}
