package io.jenkins.plugins.sample;

import java.io.IOException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray; 
import org.json.simple.JSONObject; 
import org.json.simple.parser.JSONParser;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import io.jenkins.plugins.sample.Split;
import io.jenkins.plugins.sample.Treatments;

class HTTPResponse
{
    public int statusCode; 
    public String response; // To store division
    HTTPResponse(int statusCode, String response)
    {
        this.statusCode = statusCode;
        this.response = response;
    }
}

public class SplitAPI {
	
    private static Logger LOGGER = Logger.getLogger(SplitAPI.class.getName());
	private String _apiKey = "";
	private final int _httpError = -1;

	SplitAPI(String adminApiKey) {
		_apiKey = adminApiKey;
	}
    
    public HTTPResponse ExecHTTPRequest(String httpRequest, String URL, String body) {
        int retryLimit = 10;
        int retries = 0;
        int sleepInterval = 5000;
        HTTPResponse httpResponse = new HTTPResponse(_httpError, "");
        while (retries<retryLimit) {
            retries += 1;
            try {
            	httpResponse.statusCode = _httpError;
                LOGGER.info("Executing " + httpRequest);
                if (LOGGER.isDebugEnabled()) {
	                LOGGER.debug("URL=" + URL);
	                LOGGER.debug("Body=" + body);
                }
                switch(httpRequest) {
                	case "GetHTTP":
                		httpResponse = this.ExecuteHTTP(httpRequest, URL, null);
                		break;
                    case "DeleteHTTP":
                    	httpResponse = this.ExecuteHTTP(httpRequest, URL, null);
                        break;
                    case "PostHTTP":
                    	httpResponse = this.ExecuteHTTP(httpRequest, URL, body);
                        break;
                    case "PatchHTTP":
                    	httpResponse = this.ExecuteHTTP(httpRequest, URL, body);
                        break;
                    case "PutHTTP":
                    	httpResponse = this.ExecuteHTTP(httpRequest, URL, body);
                        break;
                    default:
                    	httpResponse.statusCode = _httpError;
                }
                LOGGER.info("Status Code returned=" + Integer.toString(httpResponse.statusCode));
                if (httpResponse.statusCode == 429) {  // Rate Limit Reached
                    LOGGER.warn("Rate Limit Detected, waiting for few seconds..");
                    Thread.sleep(sleepInterval);
                    continue;
                }
                break;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("Unexpected Error running " + httpRequest + "operation ", e);
            }
        }
        this.CheckStatus(httpResponse.statusCode);
        return httpResponse;
    }
    
    private Header[] GetHeaders() {
    	Header[] headers = {
    			new BasicHeader("Content-type" , "application/json"), 
    			new BasicHeader("Authorization", "Bearer " + _apiKey)
    	};
    	return headers;
    }
    
	public HTTPResponse ExecuteHTTP(String httpVerb, String URL, String body) throws ClientProtocolException, IOException {
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
            ((AbstractHttpMessage) httpAction).setHeaders(this.GetHeaders());
            if (httpVerb.equals("PostHTTP") || httpVerb.equals("PatchHTTP") || httpVerb.equals("PutHTTP"))
            	((HttpEntityEnclosingRequestBase) httpAction).setEntity(new StringEntity(body));
	    	CloseableHttpResponse  response = httpclient.execute((HttpUriRequest) httpAction);
	    	HttpEntity entity = response.getEntity();
            mresp = EntityUtils.toString(entity);
            if (LOGGER.isDebugEnabled()) {
	            LOGGER.debug("Response: ");
	            LOGGER.debug(mresp);
            }
	    	statusCode = response.getStatusLine().getStatusCode();
        } catch (RuntimeException e) {
            throw e;
		} catch (Exception e) {
            LOGGER.error("Unexpected Error running "+httpVerb+" operation ", e);
        } finally {
            httpclient.close();
        }
        return new HTTPResponse(statusCode, mresp);
	}
	
    public String GetWorkspaceId(String workspaceName) {
        String wsId = "";
        try {
            String resp = this.ExecHTTPRequest("GetHTTP","https://api.split.io/internal/api/v2/workspaces", null).response;
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
            LOGGER.error("Unexpected Error getting Workspace Id ", e);
        }
        return wsId;
    }
    
    public int CreateSplit(String workspaceId, String trafficTypeName, String splitName, String description) {
    	int statusCode = _httpError;
        try {
        	if (!CheckSplitExists(workspaceId, splitName)) {
                String URL = "https://api.split.io/internal/api/v2/splits/ws/" + workspaceId + "/trafficTypes/" + trafficTypeName;
                String data = "{\"name\":\"" + splitName + "\", \"description\": \"" + description + "\"}";
                statusCode = this.ExecHTTPRequest("PostHTTP", URL, data).statusCode;        		
        	} else {
        		LOGGER.info("Split already exist, skipping");
                statusCode = 200;
        	}           
        } catch (Exception e) {
            LOGGER.error("Unexpected Error creating Split ", e);
        }
        return statusCode;
    }
    
    public int AddSplitToEnvironment(String workspaceId, String environmentName, String splitName, String definition) {
    	int statusCode = _httpError;
        try {
            if (!CheckSplitDefinitionExists(workspaceId, environmentName, splitName)) {
                String URL = "https://api.split.io/internal/api/v2/splits/ws/" + workspaceId + "/" + splitName + "/environments/" + environmentName;
                statusCode = this.ExecHTTPRequest("PostHTTP", URL, definition).statusCode;
            } else {
                LOGGER.info("Split definition already exist, skipping");
                statusCode = 200;
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected Error adding Split to Environment ", e);
        }
        return statusCode;
    }
    
    public int KillSplit(String workspaceId, String environmentName, String splitName) {
    	int statusCode = _httpError;    	
        try {
            String URL = "https://api.split.io/internal/api/v2/splits/ws/" + workspaceId + "/" + splitName + "/environments/" + environmentName + "/kill";
            statusCode = this.ExecHTTPRequest("PutHTTP", URL, "{ \"comment\": \"Killed By Jenkins Split Admin API\"}").statusCode;
        } catch (Exception e) {
            LOGGER.error("Unexpected Error killing Split ", e);
        }
        return statusCode;
    }
    
    public int DeleteSplit(String workspaceId, String splitName) {
    	int statusCode = _httpError;    	
        try {
            if (CheckSplitExists(workspaceId, splitName)) {
                String URL = "https://api.split.io/internal/api/v2/splits/ws/" + workspaceId + "/" + splitName;
                statusCode = this.ExecHTTPRequest("DeleteHTTP", URL, null).statusCode;
            } else {
                LOGGER.info("Split does not exist, skipping");
                return 200;
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected Error deleting Split ", e);
        }
        return statusCode;
    }

    
    public int AddWhiteListToSplit(String workspaceId, String environmentName, String splitName, String treatmentName, String whitelistKey) {
    	int statusCode = _httpError;    	
        try {
            String patchData = "";
            int treatmentOrder = this.GetTreatmentOrder(workspaceId, environmentName, splitName, treatmentName);
            if (this.CheckWhitelistSectionExist(workspaceId, environmentName, splitName, treatmentName))
                patchData = "[{\"op\": \"add\", \"path\": \"/treatments/" + Integer.toString(treatmentOrder) + "/keys/-\", \"value\": \"" + whitelistKey + "\"}]";
            else
                patchData="[{\"op\": \"add\", \"path\": \"/treatments/" + Integer.toString(treatmentOrder) + "/keys\", \"value\": [\"" + whitelistKey + "\"]}]";
            String URL = "https://api.split.io/internal/api/v2/splits/ws/" + workspaceId + "/" + splitName + "/environments/" + environmentName;
            statusCode =  this.ExecHTTPRequest("PatchHTTP", URL, patchData).statusCode;
        } catch (Exception e) {
            LOGGER.error("Unexpected Error adding Whitelist to Split ", e);
        }
        return statusCode;
    }

    private int GetTreatmentOrder(String workspaceId, String environmentName, String splitName, String treatmentName) {
        int treatmentOrder = 0;
        try {
            String resp = this.GetSplitDefinition(workspaceId, splitName, environmentName);
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
                treatmentOrder += 1;
            }
            if (!treatmentFound) treatmentOrder = 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Error getting Treatment Order ", e);
        }
        return treatmentOrder;
    }
    
    private boolean CheckWhitelistSectionExist(String workspaceId, String environmentName, String splitName, String treatmentName) {
    	boolean whiteListSectionExist = false;
        try {
            String resp = this.GetSplitDefinition(workspaceId, splitName, environmentName);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            JSONObject js = (JSONObject) obj;
            JSONArray jsArray = (JSONArray)js.get("treatments");
            for (int ws = 0; ws < jsArray.size(); ws++) {
                JSONObject jsItem = (JSONObject) jsArray.get(ws);
                if (treatmentName.equals(jsItem.get("name").toString())) {
                    if (jsItem.containsKey("keys"))
                    	whiteListSectionExist = true;
                    else
                    	whiteListSectionExist = false;
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected Error checking whitelist section exists ", e);
        }
        return whiteListSectionExist;
    }
    public String GetSplitDefinition(String workspaceId, String splitName, String environmentName) {
    	String splitDefinition = "";
        try {
        	splitDefinition = this.ExecHTTPRequest("GetHTTP","https://api.split.io/internal/api/v2/splits/ws/" + workspaceId + "/" + splitName + "/environments/" + environmentName, "").response;
        } catch (Exception e) {
            LOGGER.error("Unexpected Error getting Split definition ", e);
        }
        return splitDefinition;
    }
    
    public int CreateSplitFromYAML(String workspaceId, String environmentName, String trafficTypeName, String YAMLFile) {
    	int statusCode = _httpError;    	
        try {
            Split[] split = readSplitsFromYAML(YAMLFile);
            for (Integer i = 0; i < split.length; i++) {
                String splitName = split[i].getSplitName();
                LOGGER.info("Working on Split name (" + splitName + ")");         
                if (splitName==null) {
                    throw new Exception("\nSplit Name not found in YAML file in iteration " + i.toString());
                }
                if (!this.CheckSplitExists(workspaceId, splitName)) {
                    this.CreateSplit(workspaceId, trafficTypeName, splitName, "Created form Jenkins plugin");
                }
                else LOGGER.info("Split name (" + splitName + ") already exists in workspace! Skipping.");
                if (!this.CheckSplitDefinitionExists(workspaceId, environmentName, splitName)) {
                    String splitDefinitions = ConstructSplitDefinitions(split[i]);
                    if (LOGGER.isDebugEnabled()) {
                    	LOGGER.debug(splitDefinitions);
                    }
                    this.AddSplitToEnvironment(workspaceId, environmentName, splitName, splitDefinitions);
                } else LOGGER.info("Split definitions for (" + splitName + ") already exists in environment! Skipping.");
                                        
            }
            statusCode = 200;
        } catch (Exception e) {
            LOGGER.error("Unexpected Error creating Split from Yaml ", e);
        }
        return statusCode;
    }
    
    private Split[] readSplitsFromYAML(String YAMLFile) {
        List<Split> splits = new ArrayList<Split>();
        try {
            Yaml yaml = new Yaml();
            List<Map<String, HashMap<String, Object>>> yamlSplits = yaml.load(new InputStreamReader(new FileInputStream(YAMLFile), "UTF-8"));
            for(Map<String, HashMap<String, Object>> aSplit : yamlSplits) {
                // The outter map is a map with one key, the split name
                boolean existFlag = false;
                Split split = new Split();
                Treatments treatment = new Treatments();
                for (Map.Entry<String, HashMap<String, Object>> entry : aSplit.entrySet())  {
                    if (LOGGER.isDebugEnabled()) {
                    	LOGGER.debug("Reading YMAL, Split: "+ entry.getKey());
                    }
                    if (CheckSplitExistInArray(splits, entry.getKey())) {
                        existFlag = true;
                        split = GetSplit(splits, entry.getKey());
                    } else
                        split.setSplitName(entry.getKey());
                    if (entry.getValue()==null) continue;
                    for (Map.Entry<String, Object> valueEntry : entry.getValue().entrySet()) {
                        if (LOGGER.isDebugEnabled()) {
                        	LOGGER.debug("Reading YMAL, element " + valueEntry.getKey());
                        }
                        if (valueEntry.getKey().equals("treatment")) {
                            treatment.setTreatment((String) valueEntry.getValue());
                        }
                        if (valueEntry.getKey().equals("config")) {
                            treatment.setConfig((String) valueEntry.getValue());
                        }
                        if (valueEntry.getKey().equals("keys")) {
                            List<String> keys = (List<String>) valueEntry.getValue();
                            treatment.setKeys(keys.toArray(new String[0]));
                        }
                        if (valueEntry.getKey().equals("percentage")) {
                            treatment.setPercentage(Integer.parseInt((String) valueEntry.getValue()));
                        }
                    }
                   }
                if (treatment.getTreatment()!=null)  split.addTreatment(treatment);
                if (existFlag) {
                    for (int i = 0; i < splits.size(); i++) {
                        if (splits.get(i).getSplitName().equals(split.getSplitName())) {
                            splits.set(i, split);
                        }
                    }
                } else {
                    splits.add(split);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unexpected Error reading Splits from Yaml ", e);
        }

        return splits.toArray(new Split[0]);
    }
    
    private boolean CheckSplitExistInArray(List<Split> splits, String name) {
    	boolean splitExist = false; 
        for (int i = 0; i < splits.size(); i++) {
            if (splits.get(i).getSplitName().equals(name)) {
            	splitExist = true;
            	break;
            }
        }
        return splitExist;
    }
    
    private Split GetSplit(List<Split> splits, String name) {
    	Split split = null;
        for (int i = 0; i < splits.size(); i++) {
            if (splits.get(i).getSplitName().equals(name)) {
            	split = splits.get(i);
            	break;
            }
        }
        return split;
    }

    private String ConstructSplitDefinitions(Split split) throws Exception {
        StringBuilder JSONStructure = new StringBuilder( "" );
        if (split.getTreatments()==null) {
// Return on/off treatments with default rule set to off
            return "{\"treatments\":[{\"name\": \"on\", \"description\": \"\"}, {\"name\": \"off\", \"description\": \"\"}],\"defaultTreatment\":\"off\",\"rules\":[],\"defaultRule\":[{\"treatment\": \"off\", \"size\": 100}]}";
        }
// Adding Treatments
        JSONStructure.append(this.AddTreatmentsFromYAML(split));
    JSONStructure.append(",\"defaultTreatment\":\"" + split.getTreatments()[0].getTreatment() + "\"");
// Add default Rule
        JSONStructure.append(this.AddDefaultRuleFromYAML(split));
        JSONStructure.append("}");
        return JSONStructure.toString();
    }
    
    private String AddTreatmentsFromYAML(Split split) {
        StringBuilder JSONStructure = new StringBuilder( "" );
        try {
            Treatments[] treatments = split.getTreatments();
            int treatmentsCount = 0;
            JSONStructure.append("{\"treatments\":[");
            for (int j = 0; j < treatments.length; j++) {
                String treatmentName = treatments[j].getTreatment();
                if (treatmentName==null) {
                    throw new Exception("\nTreatment Name not found in YAML file for Split (" + split.getSplitName() + ")");
                }
                if (treatmentsCount != 0) JSONStructure.append(",");
                JSONStructure.append("{\"name\":\"" + treatmentName + "\", \"description\": \"\"");
                String dynamicConfig = treatments[j].getConfig();
                if (dynamicConfig != null) {
                    JSONStructure.append(",\"configurations\":\"" + dynamicConfig + "\"");
                }
                String keys[] = treatments[j].getKeys();
                if (keys!=null) {
                    JSONStructure.append(",\"keys\":[");
                    boolean firstFlag = true;
                    for (int k = 0; k < keys.length; k++) {
                        if (!firstFlag) JSONStructure.append(",");
                        JSONStructure.append("\"" + keys[k] + "\"");
                        firstFlag = false;
                    }
                    JSONStructure.append("]");
                }
                treatmentsCount++;
                JSONStructure.append("}");
            }
            if (treatmentsCount==1) {
    // Add a second treatment
                JSONStructure.append(",{\"name\": \"SecondTreatment\", \"description\": \"\"}");
            }
            JSONStructure.append("]");
        } catch (Exception e) {
            LOGGER.error("Unexpected Error adding Treatments from Yaml ", e);
        }
        return JSONStructure.toString();
    }
        
    private String AddDefaultRuleFromYAML(Split split) {
        StringBuilder JSONStructure = new StringBuilder( "" );
        try {
            JSONStructure.append(",\"defaultRule\":[");
            int treatmentCount = 0;
            int totPercentage = 0;
            Treatments[] treatments = split.getTreatments();
            for (int j=0; j<treatments.length; j++) {
                if (treatments[j].getPercentage()!=0) {
                    String treatmentName  = treatments[j].getTreatment();
                    int percentage = treatments[j].getPercentage();
                    if (j!=0) JSONStructure.append(",");
                    JSONStructure.append("{\"treatment\":\"" + treatmentName + "\", \"size\":" + String.valueOf(percentage) + "}");
                    totPercentage += percentage;
                    treatmentCount++;
                }
            }
            if (treatmentCount==0) {
// return with default rule set to off if off treatment exist
                boolean offFlag=false;
                for (int i = 0; i < split.getTreatments().length; i++) {
                    if (split.getTreatments()[i].getTreatment().equals("off")) offFlag = true;
                }
                if (offFlag) {
                    JSONStructure.append("{\"treatment\": \"off\", \"size\": 100}]");
                } else {
                    JSONStructure.append("{\"treatment\": \"" + split.getTreatments()[0].getTreatment() + "\", \"size\": 100}]");
                }
            } else {
                if (totPercentage<100) {
                    if (totPercentage<100 && treatmentCount==split.getTreatments().length) {
                        throw new Exception("\nTotal Default Rule Percentages are less than 100 for Split (" + split.getSplitName() + ")");
                    }
                    String findTreatmentNotUsedInRule = "";
                    int treatmentFound = 0;
                    for (int i = 0; i < split.getTreatments().length; i++) {
                        if (split.getTreatments()[i].getPercentage()==0) {
                                treatmentFound = i;
                        }
                    }
                    if (treatmentFound!=0) {
                    findTreatmentNotUsedInRule = split.getTreatments()[treatmentFound].getTreatment();
                    }
                    Integer remainingPercentage = 100-totPercentage;
                    JSONStructure.append(", {\"treatment\": \"" + findTreatmentNotUsedInRule + "\", \"size\": " + remainingPercentage.toString() + "}");
                }
                JSONStructure.append("]");
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected Error adding Default Rule from Yaml ", e);
        }
        return JSONStructure.toString();
    }
    
    public String GetSplitsList(String workspaceId) {
    	String splitList = "";
        try {
        	splitList = this.ExecHTTPRequest("GetHTTP", "https://api.split.io/internal/api/v2/splits/ws/" + workspaceId, null).response;
        } catch (Exception e) {
            LOGGER.error("Unexpected Error getting Split list ", e);
        }
        return splitList;
    }

    public String GetSplitsInEnvironmentList(String workspaceId, String environmentName) {
    	String splitsInEnvironment = ""; 
        try {
        	splitsInEnvironment = this.ExecHTTPRequest("GetHTTP", "https://api.split.io/internal/api/v2/splits/ws/" + workspaceId + "/environments/" + environmentName, null).response;
        } catch (Exception e) {
            LOGGER.error("Unexpected Error getting Splits in Environment list ", e);
        }
        return splitsInEnvironment;
    }

    private boolean CheckSplitExists(String workspaceId, String splitName) {
    	boolean splitExists = false;
        try {
            String resp = this.GetSplitsList(workspaceId);
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
            LOGGER.error("Unexpected Error checking Split exists in workspace ", e);
        }
        return splitExists;
    }

    private boolean CheckSplitDefinitionExists(String workspaceId, String environmentName, String splitName) {
    	boolean splitDefinitionExist = false;
        try {
            String resp = this.GetSplitsInEnvironmentList(workspaceId, environmentName);
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
            LOGGER.error("Unexpected Error checking Split definition exist in Environment ", e);
        }
        return splitDefinitionExist;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "UC_USELESS_CONDITION",
    justification = "statusCode can still be -1")
    private void CheckStatus(int statusCode) {
        if ((statusCode!=200 && statusCode!=302 && statusCode!=202) || statusCode==_httpError) {
            throw new AssertionError("Admin API Call Failed with code " + Integer.toString(statusCode));
        }
    }

    public int DeleteSplitDefinition(String workspaceId, String environmentName, String splitName) {
    	int statusCode = _httpError;    	
        try {
            if (CheckSplitDefinitionExists(workspaceId, environmentName, splitName)) {
                String URL = "https://api.split.io/internal/api/v2/splits/ws/" + workspaceId + "/" + splitName + "/environments/" + environmentName;
                statusCode = this.ExecHTTPRequest("DeleteHTTP", URL, null).statusCode;
            } else {
                LOGGER.info("Split Definition does not exist, skipping");
                statusCode = 200;
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected Error deleting Split definition ", e);
        }
        return statusCode;
    }
    
}
