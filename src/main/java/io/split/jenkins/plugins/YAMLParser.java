package io.split.jenkins.plugins;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.FileNotFoundException;

/**
* Class to parse Split information from YAML file
*
* @author Bilal Al-Shshany
*
*/
public class YAMLParser {

    private static Logger _log = Logger.getLogger(YAMLParser.class);

    static Split[] readSplitsFromYAML(String YAMLFile) throws Exception {
        List<Split> splits = new ArrayList<Split>();
        try {
            Yaml yaml = new Yaml();
            List<Map<String, HashMap<String, Object>>> yamlSplits = yaml.load(new InputStreamReader(new FileInputStream(YAMLFile), "UTF-8"));
            for (Map<String, HashMap<String, Object>> aSplit : yamlSplits) {
                // The outter map is a map with one key, the split name
                boolean existFlag = false;
                Split split = new Split();
                Treatments treatment = new Treatments();
                for (Map.Entry<String, HashMap<String, Object>> entry : aSplit.entrySet()) {
                    if (_log.isDebugEnabled()) {
                        _log.debug("Reading YAML, Split: " + entry.getKey());
                    }
                    if (checkSplitExistInArray(splits, entry.getKey())) {
                        existFlag = true;
                        split = getSplit(splits, entry.getKey());
                    } else
                        split.setSplitName(entry.getKey());
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
                    split.addTreatment(treatment);
                }

                if (existFlag) {
                    for (int i = 0; i < splits.size(); i++) {
                        if (splits.get(i).getSplitName().equals(split.getSplitName())) {
                            splits.set(i, split);
                        }
                    }
                } else {
                    splits.add(split);
                }
            }
        } catch (RuntimeException e) {
            _log.error("Unexpected Error reading Splits from Yaml ", e);
            throw new AssertionError("Exception reading YAML file:" + YAMLFile + ", " + e);
        } catch (Exception e) {
            _log.error("Unexpected Error Caught ", e);
            throw new AssertionError("Unexpected Error Caught " + e);
        }

        return splits.toArray(new Split[0]);
    }

    static boolean checkSplitExistInArray(List<Split> splits, String name) {
        boolean splitExist = false;
        for (int i = 0; i < splits.size(); i++) {
            if (splits.get(i).getSplitName().equals(name)) {
                splitExist = true;
                break;
            }
        }
        return splitExist;
    }

    static Split getSplit(List<Split> splits, String name) {
        Split split = null;
        for (int i = 0; i < splits.size(); i++) {
            if (splits.get(i).getSplitName().equals(name)) {
                split = splits.get(i);
                break;
            }
        }
        return split;
    }

    static String constructSplitDefinitions(Split split) throws Exception {
        StringBuilder JSONStructure = new StringBuilder("");
        if (split.getTreatments() == null) {
            // Return on/off treatments with default rule set to off
            return "{\"treatments\":[{\"name\": \"on\", \"description\": \"\"}, {\"name\": \"off\", \"description\": \"\"}],\"defaultTreatment\":\"off\",\"rules\":[],\"defaultRule\":[{\"treatment\": \"off\", \"size\": 100}]}";
        }
        // Adding Treatments
        JSONStructure.append(addTreatmentsFromYAML(split));
        JSONStructure.append(",\"defaultTreatment\":\"" + split.getTreatments()[0].getTreatment() + "\"");
        // Add default Rule
        JSONStructure.append(addDefaultRuleFromYAML(split));
        JSONStructure.append("}");
        return JSONStructure.toString();
    }

    static String addTreatmentsFromYAML(Split split) {
        StringBuilder JSONStructure = new StringBuilder("");
        try {
            Treatments[] treatments = split.getTreatments();
            int treatmentsCount = 0;
            JSONStructure.append("{\"treatments\":[");
            for (int j = 0; j < treatments.length; j++) {
                String treatmentName = treatments[j].getTreatment();
                if (treatmentName == null) {
                    throw new Exception("\nTreatment Name not found in YAML file for Split (" + split.getSplitName() + ")");
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

    static String addDefaultRuleFromYAML(Split split) {
        StringBuilder JSONStructure = new StringBuilder("");
        try {
            JSONStructure.append(",\"defaultRule\":[");
            int treatmentCount = 0;
            int totPercentage = 0;
            Treatments[] treatments = split.getTreatments();
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
                for (int i = 0; i < split.getTreatments().length; i++) {
                    if (split.getTreatments()[i].getTreatment().equals("off")) offFlag = true;
                }
                if (offFlag) {
                    JSONStructure.append("{\"treatment\": \"off\", \"size\": 100}]");
                } else {
                    JSONStructure.append("{\"treatment\": \"" + split.getTreatments()[0].getTreatment() + "\", \"size\": 100}]");
                }
            } else {
                if (totPercentage < 100) {
                    if (totPercentage < 100 && treatmentCount == split.getTreatments().length) {
                        throw new Exception("\nTotal Default Rule Percentages are less than 100 for Split (" + split.getSplitName() + ")");
                    }
                    String findTreatmentNotUsedInRule = "";
                    int treatmentFound = 0;
                    for (int i = 0; i < split.getTreatments().length; i++) {
                        if (split.getTreatments()[i].getPercentage() == 0) {
                            treatmentFound = i;
                        }
                    }
                    if (treatmentFound != 0) {
                        findTreatmentNotUsedInRule = split.getTreatments()[treatmentFound].getTreatment();
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
