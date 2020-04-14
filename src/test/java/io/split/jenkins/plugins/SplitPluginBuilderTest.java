package io.split.jenkins.plugins;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
* Unit Test class
*
* @author Bilal Al-Shshany
*
*/
public class SplitPluginBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String splitAdminApi = "ADMIN API KEY";
    final String[] splitName = new String[] {"SplitPlugin_Jenkins_Test",  "SplitPlugin_Jenkins_Test", "SplitPlugin_Jenkins_Test",  "SplitPlugin_Jenkins_Test", "SplitPlugin_Jenkins_Test",  "SplitPlugin_Jenkins_Test"};
    final String[] workspaceName = new String[] {"Default", "Default", "Default", "Default", "Default", "Default", "Default"};
    final String[] environmentName = new String[] {"Production", "Production", "Production", "Production", "Production"};
    final String[] trafficTypeName = new String[] {"user", "user"};
    final String splitDefinitions = "{\"treatments\":[{\"name\": \"premium\", \"description\": \"\"}, { \"name\": \"standard\", \"description\": \"\"}, { \"name\": \"current\", \"description\": \"\"}],\"defaultTreatment\":\"current\",\"rules\":[], \"defaultRule\":[{\"treatment\": \"current\", \"size\": 100}]}";
    final String treatmentName = "premium";
    final String whitelistKey = "bob";
    final String splitYAMLFile = "/Users/bilalal-shahwany/Desktop/Projects/Java/SplitJenkins/split-plugin/src/test/java/io/jenkins/plugins/sample/splits.yaml";


    @Test
    public void testSplitPlugin() throws Exception {
        if (splitAdminApi==null || splitAdminApi.equals("")) {
            System.out.println("Admin API not set, skipping tests");
        } else {
            testCreateSplit("createSplit");
            testAddToEnvironment("addToEnvironment");
            testAddToWhitelist("addKeyToWhitelist");
            testKillSplit("killSplit");
            testDeleteSplitDefinition("deleteSplitDefinition");
            testDeleteSplit("deleteSplit");
            testCreateSplitFromYAML("createSplitFromYAML");
        }
    }
    
    private void testCreateSplit(String splitTask) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, new String[] {""}, workspaceName, trafficTypeName, "", "", "", "");
        builder.setApiKey(splitAdminApi);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Split (" + splitName[0] + ") is created!", build);
    }

    private void testAddToEnvironment(String splitTask) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, environmentName, workspaceName, new String[] {""}, splitDefinitions, "", "", "");
        builder.setApiKey(splitAdminApi);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Split (" + splitName[0] + ") is added to ("+environmentName[0]+") Environment!", build);
    }

    private void testAddToWhitelist(String splitTask) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, environmentName, workspaceName, new String[] {""}, "", whitelistKey, treatmentName, "");
        builder.setApiKey(splitAdminApi);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Key ("+whitelistKey+") is added to ("+treatmentName+") Whitelist in Split (" + splitName[0] + ")", build);
    }

    private void testKillSplit(String splitTask) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, environmentName, workspaceName, new String[] {""}, "", "", "", "");
        builder.setApiKey(splitAdminApi);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Split (" + splitName[0] + ") is killed!", build);
    }

    private void testDeleteSplitDefinition(String splitTask) throws Exception {
         FreeStyleProject project = jenkins.createFreeStyleProject();
         SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, environmentName, workspaceName, new String[] {""}, "", "", "", "");
         builder.setApiKey(splitAdminApi);
         project.getBuildersList().add(builder);
         FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
         jenkins.assertLogContains("Split (" + splitName[0] + ") definition is deleted", build);
    }
    
    private void testDeleteSplit(String splitTask) throws Exception {
         FreeStyleProject project = jenkins.createFreeStyleProject();
         SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, new String[] {""}, workspaceName, new String[] {""}, "","","","");
         builder.setApiKey(splitAdminApi);
         project.getBuildersList().add(builder);
         FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
         jenkins.assertLogContains("Split (" + splitName[0] + ") is deleted!", build);
    }

    private void testCreateSplitFromYAML(String splitTask) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, new String[] {""}, environmentName, workspaceName, trafficTypeName, "", "", "", splitYAMLFile);
        builder.setApiKey(splitAdminApi);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Splits created successfully from (", build);
    }
}
