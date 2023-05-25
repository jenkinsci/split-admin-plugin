package io.split.jenkins.plugins;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.Secret;
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
    final String[] featureFlagName = new String[] {"SplitPlugin_Jenkins_Test",  "SplitPlugin_Jenkins_Test", "SplitPlugin_Jenkins_Test",  "SplitPlugin_Jenkins_Test", "SplitPlugin_Jenkins_Test",  "SplitPlugin_Jenkins_Test"};
    final String[] workspaceName = new String[] {"Default", "Default", "Default", "Default", "Default", "Default", "Default"};
    final String[] environmentName = new String[] {"Production", "Production", "Production", "Production", "Production"};
    final String[] trafficTypeName = new String[] {"user", "user"};
    final String featureFlagDefinitions = "{\"treatments\":[{\"name\": \"premium\", \"description\": \"\"}, { \"name\": \"standard\", \"description\": \"\"}, { \"name\": \"current\", \"description\": \"\"}],\"defaultTreatment\":\"current\",\"rules\":[], \"defaultRule\":[{\"treatment\": \"current\", \"size\": 100}]}";
    final String treatmentName = "premium";
    final String targetlistKey = "bob";
    final String featureFlagYAMLFile = "/Users/bilalal-shahwany/Desktop/Projects/Java/SplitJenkins/split-plugin/src/test/java/io/split/jenkins/plugins/featureFlags.yaml";

    private boolean checkAdminAPI() {
        boolean adminAPIValid = true;
        if (splitAdminApi == null || splitAdminApi.equals("")) {
            System.out.println("Admin API not set, skipping tests");
            adminAPIValid = false;
        }
        return adminAPIValid;
    }

    @Test
    public void testCreateFeatureFlagFromYAML() throws Exception {
        if (checkAdminAPI()) {
            FreeStyleProject project = jenkins.createFreeStyleProject();
            SplitPluginBuilder builder = prepareBuilder("createFeatureFlagFromYAML", new String[] {""}, environmentName, workspaceName, trafficTypeName, "", "", "", featureFlagYAMLFile);
            builder.setFeatureFlagYAMLFileFullPath(featureFlagYAMLFile);
            project.getBuildersList().add(builder);
            FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
            jenkins.assertLogContains("Feature flags created successfully from (", build);
        }
    }

    @Test
    public void testCreateAndDeleteFeatureFlag() throws Exception {
        if (checkAdminAPI()) {
            testCreateFeatureFlag();
            testAddToEnvironment();
            testAddToTargetlist();
            testKillFeatureFlag();
            testDeleteFeatureFlagDefinition();
            testDeleteFeatureFlag();
        }
    }

    private void testCreateFeatureFlag() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        System.out.print(featureFlagName);
        SplitPluginBuilder builder = prepareBuilder("createFeatureFlag", featureFlagName, new String[] {""}, workspaceName, trafficTypeName, "", "", "", "");
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Feature flag (" + featureFlagName[0] + ") is created!", build);
    }

    private void testAddToEnvironment() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = prepareBuilder("addToEnvironment", featureFlagName, environmentName, workspaceName, new String[] {""}, featureFlagDefinitions, "", "", "");
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Feature flag (" + featureFlagName[0] + ") is added to ("+environmentName[0]+") Environment!", build);
    }

    private void testAddToTargetlist() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = prepareBuilder("addKeyToTargetlist", featureFlagName, environmentName, workspaceName, new String[] {""}, "", targetlistKey, treatmentName, "");
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Key ("+targetlistKey+") is added to ("+treatmentName+") Targetlist in Feature flag (" + featureFlagName[0] + ")", build);
    }

    private void testKillFeatureFlag() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = prepareBuilder("killFeatureFlag", featureFlagName, environmentName, workspaceName, new String[] {""}, "", "", "", "");
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Feature flag (" + featureFlagName[0] + ") is killed!", build);
    }

    private void testDeleteFeatureFlagDefinition() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = prepareBuilder("deleteFeatureFlagDefinition", featureFlagName, environmentName, workspaceName, new String[] {""}, "", "", "", "");
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Feature flag (" + featureFlagName[0] + ") definition is deleted", build);
    }

    private void testDeleteFeatureFlag() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        SplitPluginBuilder builder = prepareBuilder("deleteFeatureFlag", featureFlagName, new String[] {""}, workspaceName, new String[] {""}, "","","","");
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Feature flag (" + featureFlagName[0] + ") is deleted!", build);
    }

    private SplitPluginBuilder prepareBuilder(String splitTask, String[] featureFlagName, String[] environmentName, String[] workspaceName, String[] trafficTypeName, String featureFlagDefinitions, String targetlistKey, String treatmentName, String featureFlagYAMLFile) {

        SplitPluginBuilder tempBuilder = new SplitPluginBuilder(splitTask, featureFlagName,  environmentName, workspaceName, trafficTypeName, featureFlagDefinitions,  targetlistKey, treatmentName, featureFlagYAMLFile);
        tempBuilder.setApiKey(Secret.fromString(splitAdminApi));
        tempBuilder.setAdminBaseURL(adminBaseURL);
        return tempBuilder;
    }
}
