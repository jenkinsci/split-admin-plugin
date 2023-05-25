package io.split.jenkins.plugins;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import hudson.util.Secret;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;


/**
 * Class wrapper for Split Admin API
 *
 * @author Bilal Al-Shshany
 *
 */
public class SplitAPI {

    private static Logger _log = Logger.getLogger(SplitAPI.class);

    private Secret _apiKey = Secret.fromString("");
    private String _adminBaseURL = "";
    private final int _httpError = -1;
    private Map<String, String> _workspaces = new HashMap<>();
    private Map<String, List<String>> _environments = new HashMap<>();

    SplitAPI(Secret adminApiKey, String adminBaseURL) {
        _apiKey = adminApiKey;
        _adminBaseURL = adminBaseURL;
    }

    public HTTPResponse execHTTPRequest(String httpRequest, String URL, String body) {
        int retryLimit = 10;
        int retries = 0;
        int sleepInterval = 5000;
        HTTPResponse httpResponse = new HTTPResponse(_httpError, "");
        while (retries < retryLimit) {
            retries++;
            try {
                httpResponse.statusCode = _httpError;
                _log.info("Executing " + httpRequest);
                if (_log.isDebugEnabled()) {
                    _log.debug("URL=" + URL);
                    _log.debug("Body=" + body);
                }

                httpResponse = executeHTTP(httpRequest, URL, body);

                _log.info("Status Code returned=" + httpResponse.statusCode);
                if (httpResponse.statusCode == 429) {  // Rate Limit Reached
                    _log.warn("Rate Limit Detected, waiting for few seconds..");
                    Thread.sleep(sleepInterval);
                    continue;
                }
                break;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                _log.error("Unexpected Error running " + httpRequest + "operation ", e);
            }
        }
        if (!(httpRequest.equals("DeleteHTTP") && httpResponse.statusCode == 404)) {
            checkStatus(httpResponse.statusCode);
        }
        return httpResponse;
    }

    private Header[] getHeaders() {
        Header[] headers = {
                new BasicHeader("Content-type" , "application/json"),
                new BasicHeader("Authorization", "Bearer " + Secret.toString(_apiKey))
        };
        return headers;
    }

    public HTTPResponse executeHTTP(String httpVerb, String URL, String body) throws ClientProtocolException, IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        int statusCode = _httpError;
        String mresp = "";
        Object httpAction = "";
        try {
            switch(httpVerb) {
                case "GetHTTP":
                    httpAction = new HttpGet(URL);
                    break;
                case "DeleteHTTP":
                    httpAction = new HttpDelete(URL);
                    break;
                case "PostHTTP":
                    httpAction = new HttpPost(URL);
                    break;
                case "PatchHTTP":
                    httpAction = new HttpPatch(URL);
                    break;
                case "PutHTTP":
                    httpAction = new HttpPut(URL);
                    break;
                default:
                    statusCode = _httpError;
            }
            ((AbstractHttpMessage) httpAction).setHeaders(getHeaders());

            if (httpVerb.equals("PostHTTP") || httpVerb.equals("PatchHTTP") || httpVerb.equals("PutHTTP")) {
                ((HttpEntityEnclosingRequestBase) httpAction).setEntity(new StringEntity(body));
            }

            CloseableHttpResponse  response = httpclient.execute((HttpUriRequest) httpAction);
            HttpEntity entity = response.getEntity();
            mresp = EntityUtils.toString(entity);

            if (_log.isDebugEnabled()) {
                _log.debug("Response: ");
                _log.debug(mresp);
            }
            statusCode = response.getStatusLine().getStatusCode();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            _log.error("Unexpected Error running "+httpVerb+" operation ", e);
        } finally {
            httpclient.close();
        }
        return new HTTPResponse(statusCode, mresp);
    }

    public String getWorkspaceId(String workspaceName) {
        String wsId = "";
        if (_workspaces.containsKey(workspaceName)) {
            _log.debug("Workspace:" + workspaceName + " found in cached list");
            wsId = _workspaces.get(workspaceName);
        } else {
            try {
                String resp = execHTTPRequest("GetHTTP",_adminBaseURL + "/workspaces", null).response;
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(resp);
                JSONObject jsTemp = (JSONObject) obj;
                JSONArray jsArray = (JSONArray) jsTemp.get("objects");
                for (int ws = 0; ws < jsArray.size(); ws++) {
                    JSONObject jsItem = (JSONObject) jsArray.get(ws);
                    String temp = jsItem.get("name").toString();
                    addToWorkspacesMap(jsItem.get("name").toString(), jsItem.get("id").toString());
                    if  (temp.equals(workspaceName)) {
                        wsId = jsItem.get("id").toString();
                    }
                }
            } catch (Exception e) {
                _log.error("Unexpected Error getting Workspace Id ", e);
            }
        }
        return wsId;
    }

    public int createFeatureFlag(String workspaceId, String trafficTypeName, String featureFlagName, String description) {
        int statusCode = _httpError;
        try {
            String URL = _adminBaseURL + "/splits/ws/" + workspaceId + "/trafficTypes/" + trafficTypeName;
            String data = "{\"name\":\"" + featureFlagName + "\", \"description\": \"" + description + "\"}";
            statusCode = execHTTPRequest("PostHTTP", URL, data).statusCode;
            if (statusCode == 409) {
                _log.info("Feature flag name:" + featureFlagName + " already exist, skipping");
                statusCode = 200;
            }
        } catch (Exception e) {
            _log.error("Unexpected Error creating feature flag name:" + featureFlagName, e);
        }
        return statusCode;
    }

    public int addFeatureFlagToEnvironment(String workspaceId, String environmentName, String featureFlagName, String definition) {
        int statusCode = _httpError;
        try {
            String URL = _adminBaseURL + "/splits/ws/" + workspaceId + "/" + featureFlagName + "/environments/" + environmentName;
            statusCode = execHTTPRequest("PostHTTP", URL, definition).statusCode;
            if (statusCode == 409) {
                _log.info("Feature flag definition already exist, skipping");
                statusCode = 200;
            }
        } catch (Exception e) {
            _log.error("Unexpected Error adding feature flag name:" + featureFlagName + " to Environment name:" + environmentName, e);
        }
        return statusCode;
    }

    public int killFeatureFlag(String workspaceId, String environmentName, String featureFlagName) {
        int statusCode = _httpError;
        try {
            String URL = _adminBaseURL + "/splits/ws/" + workspaceId + "/" + featureFlagName + "/environments/" + environmentName + "/kill";
            statusCode = execHTTPRequest("PutHTTP", URL, "{ \"comment\": \"Killed By Jenkins Split Admin API\"}").statusCode;
        } catch (Exception e) {
            _log.error("Unexpected Error killing feature flag name:" + featureFlagName, e);
        }
        return statusCode;
    }

    public int deleteFeatureFlag(String workspaceId, String featureFlagName) {
        int statusCode = _httpError;
        try {
            String URL = _adminBaseURL + "/splits/ws/" + workspaceId + "/" + featureFlagName;
            statusCode = execHTTPRequest("DeleteHTTP", URL, null).statusCode;
            if (statusCode == 404) {
                _log.info("Feature flag name:" + featureFlagName + " does not exist, skipping");
                statusCode = 200;
            }
        } catch (Exception e) {
            _log.error("Unexpected Error deleting feature flag name:" + featureFlagName, e);
        }
        return statusCode;
    }

    public int addTargetListToFeatureFlag(String workspaceId, String environmentName, String featureFlagName, String treatmentName, String targetlistKey) {
        int statusCode = _httpError;
        try {
            String patchData = "";
            int treatmentOrder = getTreatmentOrder(workspaceId, environmentName, featureFlagName, treatmentName);
            if (checkTargetlistSectionExist(workspaceId, environmentName, featureFlagName, treatmentName))
                patchData = "[{\"op\": \"add\", \"path\": \"/treatments/" + treatmentOrder + "/keys/-\", \"value\": \"" + targetlistKey + "\"}]";
            else
                patchData="[{\"op\": \"add\", \"path\": \"/treatments/" + treatmentOrder + "/keys\", \"value\": [\"" + targetlistKey + "\"]}]";
            String URL = _adminBaseURL + "/splits/ws/" + workspaceId + "/" + featureFlagName + "/environments/" + environmentName;
            statusCode =  execHTTPRequest("PatchHTTP", URL, patchData).statusCode;
        } catch (Exception e) {
            _log.error("Unexpected Error adding Targetlist to feature flag name:" + featureFlagName, e);
        }
        return statusCode;
    }



    private int getTreatmentOrder(String workspaceId, String environmentName, String featureFlagName, String treatmentName) {
        int treatmentOrder = 0;
        try {
            String resp = getFeatureFlagDefinition(workspaceId, featureFlagName, environmentName);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            JSONObject js = (JSONObject) obj;
            JSONArray jsArray = (JSONArray)js.get("treatments");
            boolean treatmentFound = false;
            for (int ws = 0; ws < jsArray.size(); ws++) {
                JSONObject jsItem = (JSONObject) jsArray.get(ws);
                if (treatmentName.equals(jsItem.get("name").toString())) {
                    treatmentFound = true;
                    break;
                }
                treatmentOrder++;
            }
            if (!treatmentFound) treatmentOrder = 0;
        } catch (Exception e) {
            _log.error("Unexpected Error getting Treatment Order ", e);
        }
        return treatmentOrder;
    }

    private boolean checkTargetlistSectionExist(String workspaceId, String environmentName, String featureFlagName, String treatmentName) {
        boolean targetListSectionExist = false;
        try {
            String resp = getFeatureFlagDefinition(workspaceId, featureFlagName, environmentName);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            JSONObject js = (JSONObject) obj;
            JSONArray jsArray = (JSONArray)js.get("treatments");
            for (int ws = 0; ws < jsArray.size(); ws++) {
                JSONObject jsItem = (JSONObject) jsArray.get(ws);
                if (treatmentName.equals(jsItem.get("name").toString())) {
                    if (jsItem.containsKey("keys")) {
                        targetListSectionExist = true;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            _log.error("Unexpected Error checking targetlist section exists ", e);
        }
        return targetListSectionExist;
    }


    public String getFeatureFlagDefinition(String workspaceId, String featureFlagName, String environmentName) {
        String featureFlagDefinition = "";
        try {
            featureFlagDefinition = execHTTPRequest("GetHTTP",_adminBaseURL + "/splits/ws/" + workspaceId + "/" + featureFlagName + "/environments/" + environmentName, "").response;
        } catch (Exception e) {
            _log.error("Unexpected Error getting feature flag definition for feature flag name:" + featureFlagName, e);
        }
        return featureFlagDefinition;
    }

    public int createFeatureFlagFromYAML(String workspaceId, String environmentName, String trafficTypeName, String YAMLFile) {
        int statusCode = _httpError;
        String featureFlagName = "";
        try {
            FeatureFlag[] featureFlag = YAMLParser.readFeatureFlagsFromYAML(YAMLFile);
            for (Integer i = 0; i < featureFlag.length; i++) {
                featureFlagName = featureFlag[i].getFeatureFlagName();
                _log.info("Working on feature flag name:" + featureFlagName);

                if (featureFlagName == null) {
                    throw new AssertionError("Feature flag name not found in YAML file in iteration " + i);
                }

                createFeatureFlag(workspaceId, trafficTypeName, featureFlagName, "Created form Jenkins plugin");

                String featureFlagDefinitions = YAMLParser.constructFeatureFlagDefinitions(featureFlag[i]);
                if (_log.isDebugEnabled()) {
                    _log.debug(featureFlagDefinitions);
                }
                addFeatureFlagToEnvironment(workspaceId, environmentName, featureFlagName, featureFlagDefinitions);
            }
            statusCode = 200;
        } catch (Exception e) {
            _log.error("Unexpected Error while parsing YAML file when creating feature flag name:" + featureFlagName, e);
        }
        return statusCode;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "UC_USELESS_CONDITION",
            justification = "statusCode can still be -1")
    private void checkStatus(int statusCode) {
        if ((statusCode != 200 && statusCode != 302 && statusCode != 202 && statusCode != 409) || statusCode == _httpError) {
            throw new AssertionError("Admin API Call Failed with code " + Integer.toString(statusCode));
        }
    }

    public int deleteFeatureFlagDefinition(String workspaceId, String environmentName, String featureFlagName) {
        int statusCode = _httpError;
        try {
            if (checkIfEnvironmentExist(workspaceId, environmentName)) {
                String URL = _adminBaseURL + "/splits/ws/" + workspaceId + "/" + featureFlagName + "/environments/" + environmentName;
                statusCode = execHTTPRequest("DeleteHTTP", URL, null).statusCode;
            } else {
                _log.error("Environment:" + environmentName + " does not exist");
                throw new AssertionError("Environment:" + environmentName + " does not exist");
            }
            if (statusCode == 404) {
                _log.info("Feature flag Definition name:" + featureFlagName + " does not exist, skipping");
                statusCode = 200;
            }
        } catch (Exception e) {
            _log.error("Unexpected Error deleting definitions for feature flag name:" + featureFlagName, e);
        }
        return statusCode;
    }

    private void addToWorkspacesMap(String workspaceName, String workspaceId) {
        if (!_workspaces.containsKey(workspaceName)) {
            _log.debug("Workspace:" + workspaceName + " added to cached list");
            _workspaces.put(workspaceName, workspaceId);
        }
    }

    private void addEnvironmentsPerWorkspaceToMap(String workspaceId) {
        if (_environments.containsKey(workspaceId)) {
            _log.debug("Workspace:" + workspaceId + " found in cached list");
            return;
        }
        try {
            List<String> environments = new ArrayList<String>();
            String resp = execHTTPRequest("GetHTTP",_adminBaseURL + "/environments/ws/" + workspaceId, null).response;
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            JSONArray jsArray = (JSONArray) obj;
            for (int ws = 0; ws < jsArray.size(); ws++) {
                JSONObject jsItem = (JSONObject) jsArray.get(ws);
                environments.add(jsItem.get("name").toString());
                _log.debug("Adding environment:" + jsItem.get("name").toString() + " for Workspace:" + workspaceId + " to cached list");

            }
            addToEnvironmentsMap(workspaceId, environments);
        } catch (Exception e) {
            _log.error("Unexpected Error getting environment list for Workspace Id:"+workspaceId, e);
        }
    }

    private void addToEnvironmentsMap(String workspaceId, List<String> environments) {
        if (!_environments.containsKey(workspaceId)) {
            _environments.put(workspaceId, environments);
        }
    }

    public boolean checkIfEnvironmentExist(String workspaceId, String environmentName) {
        boolean environmentExist = false;
        if (_workspaces.containsValue(workspaceId)) {
            if (!_environments.containsKey(workspaceId)) {
                addEnvironmentsPerWorkspaceToMap(workspaceId);
            }
            List <String> workspaceEnvironments = _environments.get(workspaceId);
            for (int i = 0; i < workspaceEnvironments.size(); i++) {
                if (workspaceEnvironments.get(i).equals(environmentName)) {
                    environmentExist = true;
                    break;
                }
            }
        }
        return environmentExist;
    }

}

/**
 * Class for http response structure
 *
 * @author Bilal Al-Shshany
 *
 */
class HTTPResponse {

    public int statusCode;
    public String response;

    HTTPResponse(int statusCode, String response) {
        this.statusCode = statusCode;
        this.response = response;
    }
}
