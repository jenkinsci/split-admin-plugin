package io.split.jenkins.plugins;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.util.Secret;
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
    final String adminBaseURL = "https://api.split.io/internal/api/v2";
    String splitAdminApi = "";
    final String[] splitName = new String[] {"SplitPlugin_Jenkins_Test",  "SplitPlugin_Jenkins_Test", "SplitPlugin_Jenkins_Test",  "SplitPlugin_Jenkins_Test", "SplitPlugin_Jenkins_Test",  "SplitPlugin_Jenkins_Test"};
    final String[] workspaceName = new String[] {"Default", "Default", "Default", "Default", "Default", "Default", "Default"};
    final String[] environmentName = new String[] {"Production", "Production", "Production", "Production", "Production"};
    final String[] trafficTypeName = new String[] {"user", "user"};
    final String splitDefinitions = "{\"treatments\":[{\"name\": \"premium\", \"description\": \"\"}, { \"name\": \"standard\", \"description\": \"\"}, { \"name\": \"current\", \"description\": \"\"}],\"defaultTreatment\":\"current\",\"rules\":[], \"defaultRule\":[{\"treatment\": \"current\", \"size\": 100}]}";
    final String treatmentName = "premium";
    final String targetlistKey = "bob";
    final String splitYAMLFile = "/Users/bilalal-shahwany/Desktop/Projects/Java/SplitJenkins/split-plugin/src/test/java/io/split/jenkins/plugins/splits.yaml";

    private boolean checkAdminAPI() {
        boolean adminAPIValid = true;
        if (splitAdminApi == null || splitAdminApi.equals("")) {
            System.out.println("Admin API not set, skipping tests");
            adminAPIValid = false;
        }
        return adminAPIValid;
    }
    
    @Test
    public void testCreateSplitFromYAML() throws Exception {
        if (checkAdminAPI()) {
            FreeStyleProject project = jenkins.createFreeStyleProject();
            SplitPluginBuilder builder = prepareBuilder("createSplitFromYAML", new String[] {""}, environmentName, workspaceName, trafficTypeName, "", "", "", splitYAMLFile);
            builder.setSplitYAMLFileFullPath(splitYAMLFile);
            project.getBuildersList().add(builder);
            FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
            jenkins.assertLogContains("Splits created successfully from (", build);
        }
    }

    @Test
    public void testCreateAndDeleteSplit() throws Exception {
        if (checkAdminAPI()) {
            testCreateSplit();
            testAddToEnvironment();
            testAddToTargetlist();
            testKillSplit();
            testDeleteSplitDefinition();
            testDeleteSplit();
        }
    }
    
    private void testCreateSplit() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = prepareBuilder("createSplit", splitName, new String[] {""}, workspaceName, trafficTypeName, "", "", "", "");
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Split (" + splitName[0] + ") is created!", build);
    }

    private void testAddToEnvironment() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = prepareBuilder("addToEnvironment", splitName, environmentName, workspaceName, new String[] {""}, splitDefinitions, "", "", "");
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Split (" + splitName[0] + ") is added to ("+environmentName[0]+") Environment!", build);
    }
    
    private void testAddToTargetlist() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = prepareBuilder("addKeyToTargetlist", splitName, environmentName, workspaceName, new String[] {""}, "", targetlistKey, treatmentName, "");
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Key ("+targetlistKey+") is added to ("+treatmentName+") Targetlist in Split (" + splitName[0] + ")", build);
    }
        
    private void testKillSplit() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = prepareBuilder("killSplit", splitName, environmentName, workspaceName, new String[] {""}, "", "", "", "");
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Split (" + splitName[0] + ") is killed!", build);
    }
        
    private void testDeleteSplitDefinition() throws Exception {
         FreeStyleProject project = jenkins.createFreeStyleProject();
         SplitPluginBuilder builder = prepareBuilder("deleteSplitDefinition", splitName, environmentName, workspaceName, new String[] {""}, "", "", "", "");
         project.getBuildersList().add(builder);
         FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
         jenkins.assertLogContains("Split (" + splitName[0] + ") definition is deleted", build);
    }
        
    private void testDeleteSplit() throws Exception {
         FreeStyleProject project = jenkins.createFreeStyleProject();
         SplitPluginBuilder builder = prepareBuilder("deleteSplit", splitName, new String[] {""}, workspaceName, new String[] {""}, "","","","");
         project.getBuildersList().add(builder);
         FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
         jenkins.assertLogContains("Split (" + splitName[0] + ") is deleted!", build);
    }
            
    private SplitPluginBuilder prepareBuilder(String splitTask, String[] splitName, String[] environmentName, String[] workspaceName, String[] trafficTypeName, String splitDefinitions, String targetlistKey, String treatmentName, String splitYAMLFile) {
        
        SplitPluginBuilder tempBuilder = new SplitPluginBuilder(splitTask, splitName,  environmentName, workspaceName, trafficTypeName, splitDefinitions,  targetlistKey, treatmentName, splitYAMLFile);
        tempBuilder.setApiKey(Secret.fromString(splitAdminApi));
        tempBuilder.setAdminBaseURL(adminBaseURL);
        return tempBuilder;
    }
}
