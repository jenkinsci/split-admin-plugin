package io.split.jenkins.plugins;

import java.util.ArrayList;
import java.util.List;

/**
 * Class provide feature flag structure when importing data from YAML file
 *
 * @author Bilal Al-Shshany
 *
 */
public class FeatureFlag {
    private Treatments[] treatments;
    private String featureFlagName;

    public FeatureFlag() {
        this.treatments = null;
        this.featureFlagName = null;
    }

    public Treatments[] getTreatments() {
        return this.treatments == null ? null : (Treatments[]) this.treatments.clone();
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "I know what I'm doing")
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

    public String getFeatureFlagName() {
        return this.featureFlagName;
    }

    public void setFeatureFlagName(String featureFlagName) {
        this.featureFlagName = featureFlagName;
    }

}
