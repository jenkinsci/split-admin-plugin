package io.split.jenkins.plugins;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to parse feature flag information from YAML file
 *
 * @author Bilal Al-Shshany
 *
 */
public class YAMLParser {

    private static Logger _log = Logger.getLogger(YAMLParser.class);

    static FeatureFlag[] readFeatureFlagsFromYAML(String YAMLFile) throws Exception {
        List<FeatureFlag> featureFlags = new ArrayList<FeatureFlag>();
        try {
            Yaml yaml = new Yaml(new SafeConstructor());
            List<Map<String, HashMap<String, Object>>> yamlFeatureFlags = yaml.load(new InputStreamReader(new FileInputStream(YAMLFile), "UTF-8"));
            for (Map<String, HashMap<String, Object>> aFeatureFlag : yamlFeatureFlags) {
                // The outer map is a map with one key, the feature flag name
                boolean existFlag = false;
                FeatureFlag featureFlag = new FeatureFlag();
                Treatments treatment = new Treatments();
                for (Map.Entry<String, HashMap<String, Object>> entry : aFeatureFlag.entrySet()) {
                    if (_log.isDebugEnabled()) {
                        _log.debug("Reading YAML, feature flag: " + entry.getKey());
                    }
                    if (checkFeatureFlagExistInArray(featureFlags, entry.getKey())) {
                        existFlag = true;
                        featureFlag = getFeatureFlag(featureFlags, entry.getKey());
                    } else
                        featureFlag.setFeatureFlagName(entry.getKey());
                    if (entry.getValue() == null) continue;
                    for (Map.Entry<String, Object> valueEntry : entry.getValue().entrySet()) {
                        if (_log.isDebugEnabled()) {
                            _log.debug("Reading YAML, element " + valueEntry.getKey());
                        }
                        if (valueEntry.getKey().equals("treatment")) {
                            treatment.setTreatment((String) valueEntry.getValue());
                        }
                        if (valueEntry.getKey().equals("config")) {
                            treatment.setConfig((String) valueEntry.getValue());
                        }
                        if (valueEntry.getKey().equals("keys")) {
                            List<String> keys = (List<String>) valueEntry.getValue();
                            treatment.setKeys(keys.toArray(new String[0]));
                        }
                        if (valueEntry.getKey().equals("percentage")) {
                            treatment.setPercentage(Integer.parseInt((String) valueEntry.getValue()));
                        }
                    }
                }

                if (treatment.getTreatment() != null) {
                    featureFlag.addTreatment(treatment);
                }

                if (existFlag) {
                    for (int i = 0; i < featureFlags.size(); i++) {
                        if (featureFlags.get(i).getFeatureFlagName().equals(featureFlag.getFeatureFlagName())) {
                            featureFlags.set(i, featureFlag);
                        }
                    }
                } else {
                    featureFlags.add(featureFlag);
                }
            }
        } catch (RuntimeException e) {
            _log.error("Unexpected Error reading feature flags from Yaml ", e);
            throw new AssertionError("Exception reading YAML file:" + YAMLFile + ", " + e);
        } catch (Exception e) {
            _log.error("Unexpected Error Caught ", e);
            throw new AssertionError("Unexpected Error Caught " + e);
        }

        return featureFlags.toArray(new FeatureFlag[0]);
    }

    static boolean checkFeatureFlagExistInArray(List<FeatureFlag> featureFlags, String name) {
        boolean featureFlagExist = false;
        for (int i = 0; i < featureFlags.size(); i++) {
            if (featureFlags.get(i).getFeatureFlagName().equals(name)) {
                featureFlagExist = true;
                break;
            }
        }
        return featureFlagExist;
    }

    static FeatureFlag getFeatureFlag(List<FeatureFlag> featureFlags, String name) {
        FeatureFlag featureFlag = null;
        for (int i = 0; i < featureFlags.size(); i++) {
            if (featureFlags.get(i).getFeatureFlagName().equals(name)) {
                featureFlag = featureFlags.get(i);
                break;
            }
        }
        return featureFlag;
    }

    static String constructFeatureFlagDefinitions(FeatureFlag featureFlag) throws Exception {
        StringBuilder JSONStructure = new StringBuilder("");
        if (featureFlag.getTreatments() == null) {
            // Return on/off treatments with default rule set to off
            return "{\"treatments\":[{\"name\": \"on\", \"description\": \"\"}, {\"name\": \"off\", \"description\": \"\"}],\"defaultTreatment\":\"off\",\"rules\":[],\"defaultRule\":[{\"treatment\": \"off\", \"size\": 100}]}";
        }
        // Adding Treatments
        JSONStructure.append(addTreatmentsFromYAML(featureFlag));
        JSONStructure.append(",\"defaultTreatment\":\"" + featureFlag.getTreatments()[0].getTreatment() + "\"");
        // Add default Rule
        JSONStructure.append(addDefaultRuleFromYAML(featureFlag));
        JSONStructure.append("}");
        return JSONStructure.toString();
    }

    static String addTreatmentsFromYAML(FeatureFlag featureFlag) {
        StringBuilder JSONStructure = new StringBuilder("");
        try {
            Treatments[] treatments = featureFlag.getTreatments();
            int treatmentsCount = 0;
            JSONStructure.append("{\"treatments\":[");
            for (int j = 0; j < treatments.length; j++) {
                String treatmentName = treatments[j].getTreatment();
                if (treatmentName == null) {
                    throw new Exception("\nTreatment Name not found in YAML file for feature flag (" + featureFlag.getFeatureFlagName() + ")");
                }
                if (treatmentsCount != 0) JSONStructure.append(",");
                JSONStructure.append("{\"name\":\"" + treatmentName + "\", \"description\": \"\"");
                String dynamicConfig = treatments[j].getConfig();
                if (dynamicConfig != null) {
                    JSONStructure.append(",\"configurations\":\"" + dynamicConfig + "\"");
                }
                String keys[] = treatments[j].getKeys();
                if (keys != null) {
                    JSONStructure.append(",\"keys\":[");
                    boolean firstFlag = true;
                    for (int k = 0; k < keys.length; k++) {
                        if (!firstFlag) JSONStructure.append(",");
                        JSONStructure.append("\"" + keys[k] + "\"");
                        firstFlag = false;
                    }
                    JSONStructure.append("]");
                }
                treatmentsCount++;
                JSONStructure.append("}");
            }
            if (treatmentsCount == 1) {
                // Add a second treatment
                JSONStructure.append(",{\"name\": \"SecondTreatment\", \"description\": \"\"}");
            }
            JSONStructure.append("]");
        } catch (Exception e) {
            _log.error("Unexpected Error adding Treatments from Yaml ", e);
        }
        return JSONStructure.toString();
    }

    static String addDefaultRuleFromYAML(FeatureFlag featureFlag) {
        StringBuilder JSONStructure = new StringBuilder("");
        try {
            JSONStructure.append(",\"defaultRule\":[");
            int treatmentCount = 0;
            int totPercentage = 0;
            Treatments[] treatments = featureFlag.getTreatments();
            for (int j = 0; j < treatments.length; j++) {
                if (treatments[j].getPercentage() != 0) {
                    String treatmentName = treatments[j].getTreatment();
                    int percentage = treatments[j].getPercentage();
                    if (j != 0) JSONStructure.append(",");
                    JSONStructure.append("{\"treatment\":\"" + treatmentName + "\", \"size\":" + String.valueOf(percentage) + "}");
                    totPercentage += percentage;
                    treatmentCount++;
                }
            }
            if (treatmentCount == 0) {
                // return with default rule set to off if off treatment exist
                boolean offFlag = false;
                for (int i = 0; i < featureFlag.getTreatments().length; i++) {
                    if (featureFlag.getTreatments()[i].getTreatment().equals("off")) offFlag = true;
                }
                if (offFlag) {
                    JSONStructure.append("{\"treatment\": \"off\", \"size\": 100}]");
                } else {
                    JSONStructure.append("{\"treatment\": \"" + featureFlag.getTreatments()[0].getTreatment() + "\", \"size\": 100}]");
                }
            } else {
                if (totPercentage < 100) {
                    if (totPercentage < 100 && treatmentCount == featureFlag.getTreatments().length) {
                        throw new Exception("\nTotal Default Rule Percentages are less than 100 for feature flag (" + featureFlag.getFeatureFlagName() + ")");
                    }
                    String findTreatmentNotUsedInRule = "";
                    int treatmentFound = 0;
                    for (int i = 0; i < featureFlag.getTreatments().length; i++) {
                        if (featureFlag.getTreatments()[i].getPercentage() == 0) {
                            treatmentFound = i;
                        }
                    }
                    if (treatmentFound != 0) {
                        findTreatmentNotUsedInRule = featureFlag.getTreatments()[treatmentFound].getTreatment();
                    }
                    Integer remainingPercentage = 100 - totPercentage;
                    JSONStructure.append(", {\"treatment\": \"" + findTreatmentNotUsedInRule + "\", \"size\": " + remainingPercentage.toString() + "}");
                }
                JSONStructure.append("]");
            }
        } catch (Exception e) {
            _log.error("Unexpected Error adding Default Rule from Yaml ", e);
        }
        return JSONStructure.toString();
    }
}
