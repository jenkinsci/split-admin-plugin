package io.split.jenkins.plugins;

import io.split.jenkins.plugins.SplitAPI;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import javax.servlet.ServletException;
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
    private final String[] splitName;
    private final String[] environmentName;
    private final String[] workspaceName;
    private final String[] trafficTypeName;
    private final String splitDefinitions;
    private final String treatmentName;
    private final String targetlistKey;
    private final String splitYAMLFile;
    private Secret apiKey;
    private String adminBaseURL;
    private static Logger _log = Logger.getLogger(SplitAPI.class);
    private String splitYAMLFileFullPath = "";

    @DataBoundConstructor
    public SplitPluginBuilder(String splitTask, String[] splitName, String[] environmentName, String[] workspaceName, String[] trafficTypeName, String splitDefinitions, String targetlistKey, String treatmentName, String splitYAMLFile) {
        
        this.splitName = splitName.clone();
        this.environmentName = environmentName.clone();
        this.workspaceName = workspaceName.clone();
        this.trafficTypeName = trafficTypeName.clone();
        this.splitDefinitions = splitDefinitions;
        this.targetlistKey = targetlistKey;
        this.treatmentName = treatmentName;
        this.splitTask = splitTask;
        this.splitYAMLFile = splitYAMLFile;
        this.apiKey = DescriptorImpl.getSplitAdminApiKey();
        this.adminBaseURL = DescriptorImpl.getAdminBaseURL();
    }

    private int getSplitNameIndex(String splitTask) {
        int index = 0;
        if (splitTask.equals("createSplit")) index = 0;
        if (splitTask.equals("addToEnvironment")) index = 1;
        if (splitTask.equals("addKeyToTargetlist")) index = 2;
        if (splitTask.equals("killSplit")) index = 3;
        if (splitTask.equals("deleteSplitDefinition")) index = 4;
        if (splitTask.equals("deleteSplit")) index = 5;
        return index;
    }
    
    private int getWorkspaceIndex(String splitTask) {
        int index = 0;
        if (splitTask.equals("createSplitFromYAML")) index = 0;
        if (splitTask.equals("createSplit")) index = 1;
        if (splitTask.equals("addToEnvironment")) index = 2;
        if (splitTask.equals("addKeyToTargetlist")) index = 3;
        if (splitTask.equals("killSplit")) index = 4;
        if (splitTask.equals("deleteSplitDefinition")) index = 5;
        if (splitTask.equals("deleteSplit")) index = 6;
        return index;
    }

    private int getEnvironmentIndex(String splitTask) {
        int index = 0;
        if (splitTask.equals("createSplitFromYAML")) index = 0;
        if (splitTask.equals("addToEnvironment")) index = 1;
        if (splitTask.equals("addKeyToTargetlist")) index = 2;
        if (splitTask.equals("killSplit")) index = 3;
        if (splitTask.equals("deleteSplitDefinition")) index = 4;
        return index;
    }

    private int getTrafficTypeIndex(String splitTask) {
        int index = 0;
        if (splitTask.equals("createSplitFromYAML")) index = 0;
        if (splitTask.equals("createSplit")) index = 1;
        return index;
    }

    
    public String getTrafficTypeName() {
        return trafficTypeName[getTrafficTypeIndex(splitTask)];
    }

    public String getSplitYAMLFile() {
        return splitYAMLFile;
    }
    
    public String getTreatmentName() {
        return treatmentName;
    }
        
    public String getTargetlistKey() {
        return targetlistKey;
    }
    
    public String getSplitDefinitions() {
        return splitDefinitions;
    }

    public String getSplitName() {
        return splitName[getSplitNameIndex(splitTask)];
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

    public void setSplitYAMLFileFullPath(String filePath) {
        this.splitYAMLFileFullPath = filePath;
    }
    
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        if (splitYAMLFileFullPath.equals("")) {
            splitYAMLFileFullPath = workspace.getRemote() + "/" + splitYAMLFile;
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
        if (splitTask.equals("createSplit")) {
            statusCode=spAdmin.createSplit(workspaceId, trafficTypeName[getTrafficTypeIndex(splitTask)], splitName[getSplitNameIndex(splitTask)], "Created From Jenkins Split Admin API");
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Split (" + splitName[getSplitNameIndex(splitTask)] + ") is created!");
        }
        if (splitTask.equals("createSplitFromYAML")) {
            statusCode=spAdmin.createSplitFromYAML(workspaceId, environmentName[getEnvironmentIndex(splitTask)], trafficTypeName[getTrafficTypeIndex(splitTask)], splitYAMLFileFullPath);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Splits created successfully from (" + splitYAMLFileFullPath + ")!");
        }
        if (splitTask.equals("addToEnvironment")) {
            statusCode=spAdmin.addSplitToEnvironment(workspaceId, environmentName[getEnvironmentIndex(splitTask)], splitName[getSplitNameIndex(splitTask)],  splitDefinitions);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Split (" + splitName[getSplitNameIndex(splitTask)] + ") is added to (" + environmentName[getEnvironmentIndex(splitTask)] + ") Environment!");
        }
        if (splitTask.equals("killSplit")) {
            statusCode=spAdmin.killSplit(workspaceId, environmentName[getEnvironmentIndex(splitTask)], splitName[getSplitNameIndex(splitTask)]);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Split (" + splitName[getSplitNameIndex(splitTask)] + ") is killed!");
        }
        if (splitTask.equals("addKeyToTargetlist")) {
            statusCode=spAdmin.addTargetListToSplit(workspaceId, environmentName[getEnvironmentIndex(splitTask)], splitName[getSplitNameIndex(splitTask)],  treatmentName, targetlistKey);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Key (" + targetlistKey + ") is added to ("+treatmentName+") Targetlist in Split (" + splitName[getSplitNameIndex(splitTask)] + ")");
        }
        if (splitTask.equals("deleteSplit")) {
            statusCode=spAdmin.deleteSplit(workspaceId, splitName[getSplitNameIndex(splitTask)]);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Split (" + splitName[getSplitNameIndex(splitTask)] + ") is deleted!");
        }
        if (splitTask.equals("deleteSplitDefinition")) {
            statusCode=spAdmin.deleteSplitDefinition(workspaceId, environmentName[getEnvironmentIndex(splitTask)], splitName[getSplitNameIndex(splitTask)]);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Split (" + splitName[getSplitNameIndex(splitTask)] + ") definition is deleted!");
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
