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
import io.split.jenkins.plugins.Split;
import io.split.jenkins.plugins.Treatments;
import io.split.jenkins.plugins.YAMLParser;

import java.io.IOException;

/**
* Class wrapper for Split Admin API
*
* @author Bilal Al-Shshany
*
*/
public class SplitAPI {

    private static final String SPLIT_API_BASE_URL = "https://api.split.io/internal/api/v2";
    private static Logger _log = Logger.getLogger(SplitAPI.class);

    private String _apiKey = "";
    private final int _httpError = -1;

    SplitAPI(String adminApiKey) {
        _apiKey = adminApiKey;
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
        checkStatus(httpResponse.statusCode);
        return httpResponse;
    }
    
    private Header[] getHeaders() {
        Header[] headers = {
                new BasicHeader("Content-type" , "application/json"),
                new BasicHeader("Authorization", "Bearer " + _apiKey)
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

            if (httpVerb.equals("PostHTTP") || httpVerb.equals("PatchHTTP") || httpVerb.equals("PutHTTP"))
                ((HttpEntityEnclosingRequestBase) httpAction).setEntity(new StringEntity(body));

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
        try {
            String resp = execHTTPRequest("GetHTTP",SPLIT_API_BASE_URL + "/workspaces", null).response;
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            JSONObject jsTemp = (JSONObject) obj;
            JSONArray jsArray = (JSONArray) jsTemp.get("objects");
            for (int ws = 0; ws < jsArray.size(); ws++) {
                JSONObject jsItem = (JSONObject) jsArray.get(ws);
                String temp = jsItem.get("name").toString();
                if  (temp.equals(workspaceName)) {
                    wsId = jsItem.get("id").toString();
                }
            }
        } catch (Exception e) {
            _log.error("Unexpected Error getting Workspace Id ", e);
        }
        return wsId;
    }
    
    public int createSplit(String workspaceId, String trafficTypeName, String splitName, String description) {
        int statusCode = _httpError;
        try {
            if (!checkSplitExists(workspaceId, splitName)) {
                String URL = SPLIT_API_BASE_URL + "/splits/ws/" + workspaceId + "/trafficTypes/" + trafficTypeName;
                String data = "{\"name\":\"" + splitName + "\", \"description\": \"" + description + "\"}";
                statusCode = execHTTPRequest("PostHTTP", URL, data).statusCode;
            } else {
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
            if (!checkSplitDefinitionExists(workspaceId, environmentName, splitName)) {
                String URL = SPLIT_API_BASE_URL + "/splits/ws/" + workspaceId + "/" + splitName + "/environments/" + environmentName;
                statusCode = execHTTPRequest("PostHTTP", URL, definition).statusCode;
            } else {
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
            String URL = SPLIT_API_BASE_URL + "/splits/ws/" + workspaceId + "/" + splitName + "/environments/" + environmentName + "/kill";
            statusCode = execHTTPRequest("PutHTTP", URL, "{ \"comment\": \"Killed By Jenkins Split Admin API\"}").statusCode;
        } catch (Exception e) {
            _log.error("Unexpected Error killing Split name:" + splitName, e);
        }
        return statusCode;
    }
    
    public int deleteSplit(String workspaceId, String splitName) {
        int statusCode = _httpError;
        try {
            if (checkSplitExists(workspaceId, splitName)) {
                String URL = SPLIT_API_BASE_URL + "/splits/ws/" + workspaceId + "/" + splitName;
                statusCode = execHTTPRequest("DeleteHTTP", URL, null).statusCode;
            } else {
                _log.info("Split name:" + splitName + " does not exist, skipping");
                return 200;
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
            String URL = SPLIT_API_BASE_URL + "/splits/ws/" + workspaceId + "/" + splitName + "/environments/" + environmentName;
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
            splitDefinition = execHTTPRequest("GetHTTP",SPLIT_API_BASE_URL + "/splits/ws/" + workspaceId + "/" + splitName + "/environments/" + environmentName, "").response;
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
                    throw new Exception("Split Name not found in YAML file in iteration " + i);
                }

                if (!checkSplitExists(workspaceId, splitName)) {
                    createSplit(workspaceId, trafficTypeName, splitName, "Created form Jenkins plugin");
                } else {
                    _log.info("Split name:" + splitName + " already exists in workspace! Skipping.");
                }

                if (!checkSplitDefinitionExists(workspaceId, environmentName, splitName)) {

                    String splitDefinitions = YAMLParser.constructSplitDefinitions(split[i]);

                    if (_log.isDebugEnabled()) {
                        _log.debug(splitDefinitions);
                    }
                    addSplitToEnvironment(workspaceId, environmentName, splitName, splitDefinitions);
                } else _log.info("Split definitions for split name:" + splitName + " already exists in environment! Skipping.");

            }
            statusCode = 200;
        } catch (Exception e) {
            _log.error("Unexpected Error while parsing YAML file when creating Split name:" + splitName, e);
        }
        return statusCode;
    }

    public String getSplitsList(String workspaceId) {
        String splitList = "";
        try {
            splitList = execHTTPRequest("GetHTTP", SPLIT_API_BASE_URL + "/splits/ws/" + workspaceId, null).response;
        } catch (Exception e) {
            _log.error("Unexpected Error getting Split list ", e);
        }
        return splitList;
    }

    public String getSplitsInEnvironmentList(String workspaceId, String environmentName) {
        String splitsInEnvironment = "";
        try {
            splitsInEnvironment = execHTTPRequest("GetHTTP", SPLIT_API_BASE_URL + "/splits/ws/" + workspaceId + "/environments/" + environmentName, null).response;
        } catch (Exception e) {
            _log.error("Unexpected Error getting Splits in Environment list ", e);
        }
        return splitsInEnvironment;
    }

    private boolean checkSplitExists(String workspaceId, String splitName) {
        boolean splitExists = false;
        try {
            String resp = getSplitsList(workspaceId);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            JSONObject js = (JSONObject) obj;
            JSONArray jsArray = (JSONArray)js.get("objects");
            for (int ws = 0; ws < jsArray.size(); ws++) {
                JSONObject jsItem = (JSONObject) jsArray.get(ws);
                String temp = jsItem.get("name").toString();
                if  (temp.equals(splitName)) {
                    splitExists = true;
                    break;
                }
            }
        } catch (Exception e) {
            _log.error("Unexpected Error checking Split exists in workspace ", e);
            // We prefer to error out on the side of caution and assume a Split exists in case
            // of error to avoid overwriting any existing split at destination.
            splitExists = true;
        }
        return splitExists;
    }

    private boolean checkSplitDefinitionExists(String workspaceId, String environmentName, String splitName) {
        boolean splitDefinitionExist = false;
        try {
            String resp = getSplitsInEnvironmentList(workspaceId, environmentName);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            JSONObject js = (JSONObject) obj;
            JSONArray jsArray = (JSONArray)js.get("objects");
            for (int ws = 0; ws < jsArray.size(); ws++) {
                JSONObject jsItem = (JSONObject) jsArray.get(ws);
                String temp = jsItem.get("name").toString();
                if  (temp.equals(splitName)) {
                    splitDefinitionExist = true;
                    break;
                }
            }
        } catch (Exception e) {
            _log.error("Unexpected Error checking Split definition exist in Environment ", e);
        }
        return splitDefinitionExist;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "UC_USELESS_CONDITION",
    justification = "statusCode can still be -1")
    private void checkStatus(int statusCode) {
        if ((statusCode!=200 && statusCode!=302 && statusCode!=202) || statusCode==_httpError) {
            throw new AssertionError("Admin API Call Failed with code " + Integer.toString(statusCode));
        }
    }

    public int deleteSplitDefinition(String workspaceId, String environmentName, String splitName) {
        int statusCode = _httpError;
        try {
            if (checkSplitDefinitionExists(workspaceId, environmentName, splitName)) {
                String URL = SPLIT_API_BASE_URL + "/splits/ws/" + workspaceId + "/" + splitName + "/environments/" + environmentName;
                statusCode = execHTTPRequest("DeleteHTTP", URL, null).statusCode;
            } else {
                _log.info("Split Definition name:" + splitName + " does not exist, skipping");
                statusCode = 200;
            }
        } catch (Exception e) {
            _log.error("Unexpected Error deleting definitions for split name:" + splitName, e);
        }
        return statusCode;
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
