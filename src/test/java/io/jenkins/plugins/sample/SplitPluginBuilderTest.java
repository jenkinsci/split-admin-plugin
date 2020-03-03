package io.jenkins.plugins.sample;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class SplitPluginBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String splitAdminApi = "9enig1pcv629481bjme33o4dn0kg26n7m6d9";
    final String splitName = "SplitPlugin_Jenkins_Test";
    final String workspaceName = "Default";
    final String environmentName = "Production";
    final String trafficTypeName = "user";
    final String splitDefinitions = "{\"treatments\":[{\"name\": \"premium\", \"description\": \"\"}, { \"name\": \"standard\", \"description\": \"\"}, { \"name\": \"current\", \"description\": \"\"}],\"defaultTreatment\":\"current\",\"rules\":[], \"defaultRule\":[{\"treatment\": \"current\", \"size\": 100}]}";
    final String treatmentName = "premium";
    final String whitelistKey = "bob";


    @Test
    public void testSplitPlugin() throws Exception {
        testCreateSplit("createSplit");
        testAddToEnvironment("addToEnvironment");
        testAddToWhitelist("addToWhitelist");
        testKillSplit("killSplit");
        testDeleteSplit("deleteSplit");
    }
    
    private void testCreateSplit(String splitTask) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, "", workspaceName, trafficTypeName, "","","");
        builder.setApiKey(splitAdminApi);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Split ("+splitName+") is created!", build);
    }

    private void testAddToEnvironment(String splitTask) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, environmentName, workspaceName, "", splitDefinitions,"","");
        builder.setApiKey(splitAdminApi);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Split ("+splitName+") is added to ("+environmentName+") Environment!", build);
    }

    private void testAddToWhitelist(String splitTask) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, environmentName, workspaceName, "", "", whitelistKey, treatmentName);
        builder.setApiKey(splitAdminApi);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Key  ("+whitelistKey+") is added to ("+treatmentName+") Whitelist in Split ("+splitName+")", build);
    }

    private void testKillSplit(String splitTask) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, environmentName, workspaceName, "", "","","");
        builder.setApiKey(splitAdminApi);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Split ("+splitName+") is killed!", build);
    }
    
    private void testDeleteSplit(String splitTask) throws Exception {
         FreeStyleProject project = jenkins.createFreeStyleProject();
         SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, "", workspaceName, "", "","","");
         builder.setApiKey(splitAdminApi);
         project.getBuildersList().add(builder);
         FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
         jenkins.assertLogContains("Split ("+splitName+") is deleted!", build);
    }

}
