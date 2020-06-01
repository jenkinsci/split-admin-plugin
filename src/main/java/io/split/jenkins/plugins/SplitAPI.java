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
import java.lang.reflect.Array;
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
    
    public int createSplit(String workspaceId, String trafficTypeName, String splitName, String description) {
        int statusCode = _httpError;
        try {
            String URL = _adminBaseURL + "/splits/ws/" + workspaceId + "/trafficTypes/" + trafficTypeName;
            String data = "{\"name\":\"" + splitName + "\", \"description\": \"" + description + "\"}";
            statusCode = execHTTPRequest("PostHTTP", URL, data).statusCode;
            if (statusCode == 409) {
                _log.info("Split name:" + splitName + " already exist, skipping");
                statusCode = 200;
            }
        } catch (Exception e) {
            _log.error("Unexpected Error creating Split name:" + splitName, e);
        }
        return statusCode;
    }
    
    public int addSplitToEnvironment(String workspaceId, String environmentName, String splitName, String definition) {
        int statusCode = _httpError;
        try {
            String URL = _adminBaseURL + "/splits/ws/" + workspaceId + "/" + splitName + "/environments/" + environmentName;
            statusCode = execHTTPRequest("PostHTTP", URL, definition).statusCode;
            if (statusCode == 409) {
                _log.info("Split definition already exist, skipping");
                statusCode = 200;
            }
        } catch (Exception e) {
            _log.error("Unexpected Error adding Split name:" + splitName + " to Environment name:" + environmentName, e);
        }
        return statusCode;
    }
    
    public int killSplit(String workspaceId, String environmentName, String splitName) {
        int statusCode = _httpError;
        try {
            String URL = _adminBaseURL + "/splits/ws/" + workspaceId + "/" + splitName + "/environments/" + environmentName + "/kill";
            statusCode = execHTTPRequest("PutHTTP", URL, "{ \"comment\": \"Killed By Jenkins Split Admin API\"}").statusCode;
        } catch (Exception e) {
            _log.error("Unexpected Error killing Split name:" + splitName, e);
        }
        return statusCode;
    }
    
    public int deleteSplit(String workspaceId, String splitName) {
        int statusCode = _httpError;
        try {
            String URL = _adminBaseURL + "/splits/ws/" + workspaceId + "/" + splitName;
            statusCode = execHTTPRequest("DeleteHTTP", URL, null).statusCode;
            if (statusCode == 404) {
                _log.info("Split name:" + splitName + " does not exist, skipping");
                statusCode = 200;
            }
        } catch (Exception e) {
            _log.error("Unexpected Error deleting Split name:" + splitName, e);
        }
        return statusCode;
    }

    
    public int addWhiteListToSplit(String workspaceId, String environmentName, String splitName, String treatmentName, String whitelistKey) {
        int statusCode = _httpError;
        try {
            String patchData = "";
            int treatmentOrder = getTreatmentOrder(workspaceId, environmentName, splitName, treatmentName);
            if (checkWhitelistSectionExist(workspaceId, environmentName, splitName, treatmentName))
                patchData = "[{\"op\": \"add\", \"path\": \"/treatments/" + treatmentOrder + "/keys/-\", \"value\": \"" + whitelistKey + "\"}]";
            else
                patchData="[{\"op\": \"add\", \"path\": \"/treatments/" + treatmentOrder + "/keys\", \"value\": [\"" + whitelistKey + "\"]}]";
            String URL = _adminBaseURL + "/splits/ws/" + workspaceId + "/" + splitName + "/environments/" + environmentName;
            statusCode =  execHTTPRequest("PatchHTTP", URL, patchData).statusCode;
        } catch (Exception e) {
            _log.error("Unexpected Error adding Whitelist to Split name:" + splitName, e);
        }
        return statusCode;
    }

    private int getTreatmentOrder(String workspaceId, String environmentName, String splitName, String treatmentName) {
        int treatmentOrder = 0;
        try {
            String resp = getSplitDefinition(workspaceId, splitName, environmentName);
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
    
    private boolean checkWhitelistSectionExist(String workspaceId, String environmentName, String splitName, String treatmentName) {
        boolean whiteListSectionExist = false;
        try {
            String resp = getSplitDefinition(workspaceId, splitName, environmentName);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            JSONObject js = (JSONObject) obj;
            JSONArray jsArray = (JSONArray)js.get("treatments");
            for (int ws = 0; ws < jsArray.size(); ws++) {
                JSONObject jsItem = (JSONObject) jsArray.get(ws);
                if (treatmentName.equals(jsItem.get("name").toString())) {
                    if (jsItem.containsKey("keys")) {
                        whiteListSectionExist = true;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            _log.error("Unexpected Error checking whitelist section exists ", e);
        }
        return whiteListSectionExist;
    }

    public String getSplitDefinition(String workspaceId, String splitName, String environmentName) {
        String splitDefinition = "";
        try {
            splitDefinition = execHTTPRequest("GetHTTP",_adminBaseURL + "/splits/ws/" + workspaceId + "/" + splitName + "/environments/" + environmentName, "").response;
        } catch (Exception e) {
            _log.error("Unexpected Error getting Split definition for split name:" + splitName, e);
        }
        return splitDefinition;
    }

    public int createSplitFromYAML(String workspaceId, String environmentName, String trafficTypeName, String YAMLFile) {
        int statusCode = _httpError;
        String splitName = "";
        try {
            Split[] split = YAMLParser.readSplitsFromYAML(YAMLFile);
            for (Integer i = 0; i < split.length; i++) {
                splitName = split[i].getSplitName();
                _log.info("Working on Split name:" + splitName);

                if (splitName == null) {
                    throw new AssertionError("Split Name not found in YAML file in iteration " + i);
                }

                createSplit(workspaceId, trafficTypeName, splitName, "Created form Jenkins plugin");
                
                String splitDefinitions = YAMLParser.constructSplitDefinitions(split[i]);
                if (_log.isDebugEnabled()) {
                    _log.debug(splitDefinitions);
                }
                addSplitToEnvironment(workspaceId, environmentName, splitName, splitDefinitions);
            }
            statusCode = 200;
        } catch (Exception e) {
            _log.error("Unexpected Error while parsing YAML file when creating Split name:" + splitName, e);
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

    public int deleteSplitDefinition(String workspaceId, String environmentName, String splitName) {
        int statusCode = _httpError;
        try {
            if (checkIfEnvironmentExist(workspaceId, environmentName)) {
                String URL = _adminBaseURL + "/splits/ws/" + workspaceId + "/" + splitName + "/environments/" + environmentName;
                statusCode = execHTTPRequest("DeleteHTTP", URL, null).statusCode;
            } else {
                _log.error("Environment:" + environmentName + " does not exist");
                throw new AssertionError("Environment:" + environmentName + " does not exist");
            }
            if (statusCode == 404) {
                _log.info("Split Definition name:" + splitName + " does not exist, skipping");
                statusCode = 200;
            }
        } catch (Exception e) {
            _log.error("Unexpected Error deleting definitions for split name:" + splitName, e);
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
