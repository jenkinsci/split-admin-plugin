package io.split.jenkins.plugins;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
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
    final String splitName = "SplitPlugin_Jenkins_Test";
    final String workspaceName = "Default";
    final String environmentName = "Production";
    final String trafficTypeName = "user";
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
            testAddToWhitelist("addToWhitelist");
            testKillSplit("killSplit");
            testDeleteSplitDefinition("deleteSplitDefinition");
            testDeleteSplit("deleteSplit");
            testCreateSplitFromYAML("createSplitFromYAML");
        }
    }
    
    private void testCreateSplit(String splitTask) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, "", workspaceName, trafficTypeName, "","","","");
        builder.setApiKey(splitAdminApi);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Split ("+splitName+") is created!", build);
    }

    private void testAddToEnvironment(String splitTask) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, environmentName, workspaceName, "", splitDefinitions,"","","");
        builder.setApiKey(splitAdminApi);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Split ("+splitName+") is added to ("+environmentName+") Environment!", build);
    }

    private void testAddToWhitelist(String splitTask) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, environmentName, workspaceName, "", "", whitelistKey, treatmentName,"");
        builder.setApiKey(splitAdminApi);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Key  ("+whitelistKey+") is added to ("+treatmentName+") Whitelist in Split ("+splitName+")", build);
    }

    private void testKillSplit(String splitTask) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, environmentName, workspaceName, "", "","","","");
        builder.setApiKey(splitAdminApi);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Split ("+splitName+") is killed!", build);
    }

    private void testDeleteSplitDefinition(String splitTask) throws Exception {
         FreeStyleProject project = jenkins.createFreeStyleProject();
         SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, environmentName, workspaceName, "", "","","","");
         builder.setApiKey(splitAdminApi);
         project.getBuildersList().add(builder);
         FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
         jenkins.assertLogContains("Split ("+splitName+") definition is deleted", build);
    }
    
    private void testDeleteSplit(String splitTask) throws Exception {
         FreeStyleProject project = jenkins.createFreeStyleProject();
         SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, "", workspaceName, "", "","","","");
         builder.setApiKey(splitAdminApi);
         project.getBuildersList().add(builder);
         FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
         jenkins.assertLogContains("Split ("+splitName+") is deleted!", build);
    }

    private void testCreateSplitFromYAML(String splitTask) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = new SplitPluginBuilder(splitTask, splitName, environmentName, workspaceName, trafficTypeName, "","","",splitYAMLFile);
        builder.setApiKey(splitAdminApi);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Splits created successfuly from (", build);
    }
}
