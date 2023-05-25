package io.split.jenkins.plugins;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.Secret;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.apache.log4j.Logger;
import net.sf.json.JSONObject;

/**
 * Class implementing Jenkins plugin builder and descriptor
 *
 * @author Bilal Al-Shshany
 *
 */
public class SplitPluginBuilder extends Builder implements SimpleBuildStep {

    private final String splitTask;
    private final String[] featureFlagName;
    private final String[] environmentName;
    private final String[] workspaceName;
    private final String[] trafficTypeName;
    private final String featureFlagDefinitions;
    private final String treatmentName;
    private final String targetlistKey;
    private final String featureFlagYAMLFile;
    private Secret apiKey;
    private String adminBaseURL;
    private static Logger _log = Logger.getLogger(SplitAPI.class);
    private String featureFlagYAMLFileFullPath = "";

    @DataBoundConstructor
    public SplitPluginBuilder(String splitTask, String[] featureFlagName, String[] environmentName, String[] workspaceName, String[] trafficTypeName, String featureFlagDefinitions, String targetlistKey, String treatmentName, String featureFlagYAMLFile) {

        this.featureFlagName = featureFlagName.clone();
        this.environmentName = environmentName.clone();
        this.workspaceName = workspaceName.clone();
        this.trafficTypeName = trafficTypeName.clone();
        this.featureFlagDefinitions = featureFlagDefinitions;
        this.targetlistKey = targetlistKey;
        this.treatmentName = treatmentName;
        this.splitTask = splitTask;
        this.featureFlagYAMLFile = featureFlagYAMLFile;
        this.apiKey = DescriptorImpl.getSplitAdminApiKey();
        this.adminBaseURL = DescriptorImpl.getAdminBaseURL();
    }

    private int getFeatureFlagNameIndex(String splitTask) {
        int index = 0;
        if (splitTask.equals("createFeatureFlag")) index = 0;
        if (splitTask.equals("addToEnvironment")) index = 1;
        if (splitTask.equals("addKeyToTargetlist")) index = 2;
        if (splitTask.equals("killFeatureFlag")) index = 3;
        if (splitTask.equals("deleteFeatureFlagDefinition")) index = 4;
        if (splitTask.equals("deleteFeatureFlag")) index = 5;
        return index;
    }

    private int getWorkspaceIndex(String splitTask) {
        int index = 0;
        if (splitTask.equals("createFeatureFlagFromYAML")) index = 0;
        if (splitTask.equals("createFeatureFlag")) index = 1;
        if (splitTask.equals("addToEnvironment")) index = 2;
        if (splitTask.equals("addKeyToTargetlist")) index = 3;
        if (splitTask.equals("killFeatureFlag")) index = 4;
        if (splitTask.equals("deleteFeatureFlagDefinition")) index = 5;
        if (splitTask.equals("deleteFeatureFlag")) index = 6;
        return index;
    }

    private int getEnvironmentIndex(String splitTask) {
        int index = 0;
        if (splitTask.equals("createFeatureFlagFromYAML")) index = 0;
        if (splitTask.equals("addToEnvironment")) index = 1;
        if (splitTask.equals("addKeyToTargetlist")) index = 2;
        if (splitTask.equals("killFeatureFlag")) index = 3;
        if (splitTask.equals("deleteFeatureFlagDefinition")) index = 4;
        return index;
    }

    private int getTrafficTypeIndex(String splitTask) {
        int index = 0;
        if (splitTask.equals("createFeatureFlagFromYAML")) index = 0;
        if (splitTask.equals("createFeatureFlag")) index = 1;
        return index;
    }


    public String getTrafficTypeName() {
        return trafficTypeName[getTrafficTypeIndex(splitTask)];
    }

    public String getFeatureFlagYAMLFile() {
        return featureFlagYAMLFile;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public String getTargetlistKey() {
        return targetlistKey;
    }

    public String getFeatureFlagDefinitions() {
        return featureFlagDefinitions;
    }

    public String getFeatureFlagName() {
        return featureFlagName[getFeatureFlagNameIndex(splitTask)];
    }

    public String getEnvironmentName() {
        return environmentName[getEnvironmentIndex(splitTask)];
    }

    public String getWorkspaceName() {
        return workspaceName[getWorkspaceIndex(splitTask)];
    }

    public String getSplitTask() {
        return splitTask;
    }

    public void setApiKey(Secret splitApiKey) {
        this.apiKey = splitApiKey;
    }

    public void setAdminBaseURL(String adminBaseURL) {
        this.adminBaseURL = adminBaseURL;
    }

    public void setFeatureFlagYAMLFileFullPath(String filePath) {
        this.featureFlagYAMLFileFullPath = filePath;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        if (featureFlagYAMLFileFullPath.equals("")) {
            featureFlagYAMLFileFullPath = workspace.getRemote() + "/" + featureFlagYAMLFile;
        }
        _log.info("Selected Task: " + splitTask);
        listener.getLogger().println("Selected Task: " + splitTask);
        if (this.apiKey.equals(Secret.fromString(""))) {
            this.apiKey = DescriptorImpl.getSplitAdminApiKey();
        }
        if (this.adminBaseURL.equals("")) {
            this.adminBaseURL = DescriptorImpl.getAdminBaseURL();
        }

        SplitAPI spAdmin = new SplitAPI(this.apiKey, this.adminBaseURL);
        String workspaceId = spAdmin.getWorkspaceId(workspaceName[getWorkspaceIndex(splitTask)]);
        listener.getLogger().println("WorkspaceId: " + workspaceId);
        Integer statusCode = 0;
        if (splitTask.equals("createFeatureFlag")) {
            statusCode=spAdmin.createFeatureFlag(workspaceId, trafficTypeName[getTrafficTypeIndex(splitTask)], featureFlagName[getFeatureFlagNameIndex(splitTask)], "Created From Jenkins Split Admin API");
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Feature flag (" + featureFlagName[getFeatureFlagNameIndex(splitTask)] + ") is created!");
        }
        if (splitTask.equals("createFeatureFlagFromYAML")) {
            statusCode=spAdmin.createFeatureFlagFromYAML(workspaceId, environmentName[getEnvironmentIndex(splitTask)], trafficTypeName[getTrafficTypeIndex(splitTask)], featureFlagYAMLFileFullPath);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Feature flags created successfully from (" + featureFlagYAMLFileFullPath + ")!");
        }
        if (splitTask.equals("addToEnvironment")) {
            statusCode=spAdmin.addFeatureFlagToEnvironment(workspaceId, environmentName[getEnvironmentIndex(splitTask)], featureFlagName[getFeatureFlagNameIndex(splitTask)], featureFlagDefinitions);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Feature flag (" + featureFlagName[getFeatureFlagNameIndex(splitTask)] + ") is added to (" + environmentName[getEnvironmentIndex(splitTask)] + ") Environment!");
        }
        if (splitTask.equals("killFeatureFlag")) {
            statusCode=spAdmin.killFeatureFlag(workspaceId, environmentName[getEnvironmentIndex(splitTask)], featureFlagName[getFeatureFlagNameIndex(splitTask)]);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Feature flag (" + featureFlagName[getFeatureFlagNameIndex(splitTask)] + ") is killed!");
        }
        if (splitTask.equals("addKeyToTargetlist")) {
            statusCode=spAdmin.addTargetListToFeatureFlag(workspaceId, environmentName[getEnvironmentIndex(splitTask)], featureFlagName[getFeatureFlagNameIndex(splitTask)],  treatmentName, targetlistKey);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Key (" + targetlistKey + ") is added to ("+treatmentName+") Targetlist in Feature flag (" + featureFlagName[getFeatureFlagNameIndex(splitTask)] + ")");
        }
        if (splitTask.equals("deleteFeatureFlag")) {
            statusCode=spAdmin.deleteFeatureFlag(workspaceId, featureFlagName[getFeatureFlagNameIndex(splitTask)]);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Feature flag (" + featureFlagName[getFeatureFlagNameIndex(splitTask)] + ") is deleted!");
        }
        if (splitTask.equals("deleteFeatureFlagDefinition")) {
            statusCode=spAdmin.deleteFeatureFlagDefinition(workspaceId, environmentName[getEnvironmentIndex(splitTask)], featureFlagName[getFeatureFlagNameIndex(splitTask)]);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Feature flag (" + featureFlagName[getFeatureFlagNameIndex(splitTask)] + ") definition is deleted!");
        }
        _log.info("Task:" + splitTask + " is completed");

    }

    @Symbol("split")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        static Secret splitAdminApiKey = Secret.fromString("");
        static String splitAdminBaseURL = "https://api.split.io/internal/api/v2";

        public static Secret getSplitAdminApiKey() {
            return splitAdminApiKey;
        }

        public static void setSplitAdminApiKey(String splitApiKey) {
            splitAdminApiKey = Secret.fromString(splitApiKey);
        }

        public static String getAdminBaseURL() {
            return splitAdminBaseURL;
        }

        public static void setAdminBaseURL(String baseURL) {
            splitAdminBaseURL = baseURL;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData)
                throws FormException {
            setSplitAdminApiKey(req.getParameter("ext_split_admin_api_key"));
            setAdminBaseURL(req.getParameter("ext_admin_base_url"));
            save();
            return super.configure(req, formData);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.SplitPluginBuilder_DescriptorImpl_DisplayName();
        }
    }

}
