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
    private final String splitName;
    private final String environmentName;
    private final String workspaceName;
    private final String trafficTypeName;
    private final String splitDefinitions;
    private final String treatmentName;
    private final String whitelistKey;
    private final String splitYAMLFile;
    private String apiKey;

    @DataBoundConstructor
    public SplitPluginBuilder(String splitTask, String splitName, String environmentName, String workspaceName, String trafficTypeName, String splitDefinitions, String whitelistKey, String treatmentName, String splitYAMLFile) {
        this.splitName = splitName;
        this.environmentName = environmentName;
        this.workspaceName = workspaceName;
        this.trafficTypeName = trafficTypeName;
        this.splitDefinitions = splitDefinitions;
        this.whitelistKey = whitelistKey;
        this.treatmentName = treatmentName;
        this.splitTask = splitTask;
        this.splitYAMLFile = splitYAMLFile;
        this.apiKey = DescriptorImpl.getSplitAdminApiKey();
    }

    public String getTrafficTypeName() {
        return trafficTypeName;
    }

    public String getSplitYAMLFile() {
        return splitYAMLFile;
    }
    
    public String getSplitDefinitions() {
        return splitDefinitions;
    }

    public String getSplitName() {
        return splitName;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public String getWorkspaceName() {
        return workspaceName;
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
        String workspaceId = spAdmin.GetWorkspaceId(workspaceName);
        listener.getLogger().println("WorkspaceId: " + workspaceId);
        Integer statusCode = 0;
        if (splitTask.equals("createSplit")) {
            statusCode=spAdmin.CreateSplit(workspaceId, trafficTypeName, splitName, "Created From Jenkins Split Admin API");
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Split (" + splitName + ") is created!");
        }
        if (splitTask.equals("createSplitFromYAML")) {
            statusCode=spAdmin.CreateSplitFromYAML(workspaceId, environmentName, trafficTypeName, splitYAMLFileFullPath);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Splits created successfuly from (" + splitYAMLFileFullPath + ")!");
        }
        if (splitTask.equals("addToEnvironment")) {
            statusCode=spAdmin.AddSplitToEnvironment(workspaceId, environmentName, splitName,  splitDefinitions);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Split (" + splitName + ") is added to (" + environmentName + ") Environment!");
        }
        if (splitTask.equals("killSplit")) {
            statusCode=spAdmin.KillSplit(workspaceId, environmentName, splitName);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Split (" + splitName + ") is killed!");
        }
        if (splitTask.equals("addToWhitelist")) {
            statusCode=spAdmin.AddWhiteListToSplit(workspaceId, environmentName, splitName,  treatmentName, whitelistKey);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Key  (" + whitelistKey + ") is added to ("+treatmentName+") Whitelist in Split ("+splitName+")");
        }
        if (splitTask.equals("deleteSplit")) {
            statusCode=spAdmin.DeleteSplit(workspaceId, splitName);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Split (" + splitName + ") is deleted!");
        }
        if (splitTask.equals("deleteSplitDefinition")) {
            statusCode=spAdmin.DeleteSplitDefinition(workspaceId, environmentName, splitName);
            listener.getLogger().println("Returned Status Code: " + statusCode.toString());
            listener.getLogger().println("Split (" + splitName + ") definition is deleted!");
        }
    }
    
    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        static String splitAdminApiKey = "";
        public FormValidation doCheckWorkspaceName(@QueryParameter String workspaceName)
                throws IOException, ServletException {
            if (workspaceName.length() == 0)
                return FormValidation.error("Please Set the Workspace Name");
            return FormValidation.ok();
        }

        public FormValidation doCheckEnvironmentName(@QueryParameter String environmentName)
                throws IOException, ServletException {
            if (environmentName.length() == 0)
                return FormValidation.error("Please Set the Environment Name");
            return FormValidation.ok();
        }

        public FormValidation doCheckSplitName(@QueryParameter String splitName)
                throws IOException, ServletException {
            if (splitName.length() == 0)
                return FormValidation.error("Please Set the Split Name");
            return FormValidation.ok();
        }

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
