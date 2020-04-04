package io.jenkins.plugins.sample;

import io.jenkins.plugins.sample.SplitAPI;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
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
import org.kohsuke.stapler.DataBoundSetter;
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
    private final String whitelistKey;
    private final String splitYAMLFile;
    private String apiKey;

    @DataBoundConstructor
    public SplitPluginBuilder(String splitTask, String[] splitName, String[] environmentName, String[] workspaceName, String[] trafficTypeName, String splitDefinitions, String whitelistKey, String treatmentName, String splitYAMLFile) {
        
        this.splitName = splitName.clone();
        this.environmentName = environmentName.clone();
        this.workspaceName = workspaceName.clone();
        this.trafficTypeName = trafficTypeName.clone();
        this.splitDefinitions = splitDefinitions;
        this.whitelistKey = whitelistKey;
        this.treatmentName = treatmentName;
        this.splitTask = splitTask;
        this.splitYAMLFile = splitYAMLFile;
        this.apiKey = DescriptorImpl.getSplitAdminApiKey();
    }

    private int GetWorkspaceIndex(String splitTask) {
        int index = 0;
        if (splitTask.equals("createSplit")) index = 0;
        if (splitTask.equals("addToEnvironment")) index = 1;
        if (splitTask.equals("addKeyToWhitelist")) index = 2;
        if (splitTask.equals("killSplit")) index = 3;
        if (splitTask.equals("deleteSplitDefinition")) index = 4;
        if (splitTask.equals("deleteSplit")) index = 5;
        if (splitTask.equals("createSplitFromYAML")) index = 6;
        return index;
    }

    private int GetEnvironmentIndex(String splitTask) {
        int index = 0;
        if (splitTask.equals("addToEnvironment")) index = 0;
        if (splitTask.equals("addKeyToWhitelist")) index = 1;
        if (splitTask.equals("killSplit")) index = 2;
        if (splitTask.equals("deleteSplitDefinition")) index = 3;
        if (splitTask.equals("createSplitFromYAML")) index = 4;
        return index;
    }

    private int GetTrafficTypeIndex(String splitTask) {
        int index = 0;
        if (splitTask.equals("createSplit")) index = 0;
        if (splitTask.equals("createSplitFromYAML")) index = 1;
        return index;
    }

    
    public String getTrafficTypeName() {
        return trafficTypeName[GetTrafficTypeIndex(splitTask)];
    }

    public String getSplitYAMLFile() {
        return splitYAMLFile;
    }
    
    public String getTreatmentName() {
        return treatmentName;
    }
    
    public String getWhitelistKey() {
        return whitelistKey;
    }
    
    public String getSplitDefinitions() {
        return splitDefinitions;
    }

    public String getSplitName() {
        return splitName[GetWorkspaceIndex(splitTask)];
    }

    public String getEnvironmentName() {
        return environmentName[GetEnvironmentIndex(splitTask)];
    }

    public String getWorkspaceName() {
        return workspaceName[GetWorkspaceIndex(splitTask)];
    }
    
    public String getSplitTask() {
        return splitTask;
    }

    public void setApiKey(String splitApiKey) {
        this.apiKey = splitApiKey;
    }
    
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        String splitYAMLFileFullPath = "";
        splitYAMLFileFullPath = workspace.getRemote() + "/" + splitYAMLFile;
        listener.getLogger().println("Select Task: "+splitTask);
        if (this.apiKey.equals("")) this.apiKey = DescriptorImpl.getSplitAdminApiKey();
        SplitAPI spAdmin = new SplitAPI(this.apiKey);
        String workspaceId = spAdmin.GetWorkspaceId(workspaceName[GetWorkspaceIndex(splitTask)]);
        listener.getLogger().println("WorkspaceId: " + workspaceId);
        Integer statusCode = 0;
        if (splitTask.equals("createSplit")) {
            statusCode=spAdmin.CreateSplit(workspaceId, trafficTypeName[0], splitName[0], "Created From Jenkins Split Admin API");
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Split (" + splitName[0] + ") is created!");
        }
        if (splitTask.equals("createSplitFromYAML")) {
            statusCode=spAdmin.CreateSplitFromYAML(workspaceId, environmentName[GetEnvironmentIndex(splitTask)], trafficTypeName[GetTrafficTypeIndex(splitTask)], splitYAMLFileFullPath);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Splits created successfuly from (" + splitYAMLFileFullPath + ")!");
        }
        if (splitTask.equals("addToEnvironment")) {
            statusCode=spAdmin.AddSplitToEnvironment(workspaceId, environmentName[0], splitName[0],  splitDefinitions);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Split (" + splitName[0] + ") is added to (" + environmentName[0] + ") Environment!");
        }
        if (splitTask.equals("killSplit")) {
            statusCode=spAdmin.KillSplit(workspaceId, environmentName[0], splitName[0]);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Split (" + splitName[0] + ") is killed!");
        }
        if (splitTask.equals("addKeyToWhitelist")) {
            statusCode=spAdmin.AddWhiteListToSplit(workspaceId, environmentName[0], splitName[0],  treatmentName, whitelistKey);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Key (" + whitelistKey + ") is added to ("+treatmentName+") Whitelist in Split (" + splitName[0] + ")");
        }
        if (splitTask.equals("deleteSplit")) {
            statusCode=spAdmin.DeleteSplit(workspaceId, splitName[0]);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Split (" + splitName[0] + ") is deleted!");
        }
        if (splitTask.equals("deleteSplitDefinition")) {
            statusCode=spAdmin.DeleteSplitDefinition(workspaceId, environmentName[0], splitName[0]);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Split (" + splitName[0] + ") definition is deleted!");
        }
    }
    
    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        static String splitAdminApiKey = "";

        public static String getSplitAdminApiKey() {
            return splitAdminApiKey;
        }
        
        public static void setSplitAdminApiKey(String splitApiKey) {
            splitAdminApiKey = splitApiKey;
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData)
                throws FormException {
            setSplitAdminApiKey(req.getParameter("ext_split_admin_api_key"));
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
