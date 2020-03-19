package io.jenkins.plugins.sample;

import java.io.IOException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray; 
import org.json.simple.JSONObject; 
import org.json.simple.parser.JSONParser;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import io.jenkins.plugins.sample.Split;
import io.jenkins.plugins.sample.Treatments;


public class SplitAPI {
	
    static Logger myLogger = Logger.getLogger(SplitAPI.class.getName());
	String apiKey="";
	
	SplitAPI(String adminApiKey) {
		this.apiKey=adminApiKey;
	}
    
    public int ExecHTTPRequest(String httpRequest, String URL, String body) {
        int retryLimit = 10;
        int retries = 0;
        int sleepInterval = 5000;
        int statusCode = 0;
        while (retries<retryLimit) {
            retries+=1;
            statusCode = 500;
            try {
                myLogger.info("Executing "+httpRequest);
                myLogger.debug("URL="+URL);
                myLogger.debug("Body="+body);
                switch(httpRequest) {
                    case "DeleteHTTP":
                        statusCode = this.DeleteHTTP(URL);
                        break;
                    case "PostHTTP":
                        statusCode = this.PostHTTP(URL, body);
                        break;
                    case "PatchHTTP":
                        statusCode = this.PatchHTTP(URL, body);
                        break;
                    case "PutHTTP":
                        statusCode = this.PutHTTP(URL, body);
                        break;
                    default:
                        statusCode = 500;
                }
                myLogger.info("Status Code returned="+Integer.toString(statusCode));
                if (statusCode==429) {  // Rate Limit Reached
                    myLogger.warn("Rate Limit Detected, waiting for few seconds..");
                    Thread.sleep(sleepInterval);
                    continue;
                }
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return statusCode;
    }
	
	public String GetHTTP(String URL) throws ClientProtocolException, IOException, Exception {
        int retryLimit = 10;
        int retries = 0;
        int sleepInterval = 5000;
        String mresp="";
        CloseableHttpClient httpclient = null;
        try {
            while (retries<retryLimit) {
                httpclient = HttpClients.createDefault();
                int statusCode = 500;
                myLogger.debug("GetHTTP:URL: "+URL);
                HttpGet httpGet = new HttpGet(URL);
                httpGet.setHeader("Content-type", "application/json");
                httpGet.setHeader("Authorization", "Bearer "+this.apiKey);
                CloseableHttpResponse  response = httpclient.execute(httpGet);
                HttpEntity entity = response.getEntity();
                mresp =  EntityUtils.toString(entity);
                myLogger.debug("Response: ");
                myLogger.debug(mresp);
                statusCode = response.getStatusLine().getStatusCode();
                if (statusCode!=200 && statusCode!=302 && statusCode!=202 && statusCode!=429) {
                    throw new Exception(mresp);
                }
                retries+=1;
                if (statusCode==429) {  // Rate Limit Reached
                    myLogger.warn("Rate Limit Detected, waiting for few seconds..");
                    Thread.sleep(sleepInterval);
                    continue;
                }
                break;
            }
            return mresp;
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            httpclient.close();
        }
		return "";
	}

	public int PostHTTP(String URL, String body) throws ClientProtocolException, IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
	    	HttpPost httpPost = new HttpPost(URL);
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", "Bearer "+this.apiKey);
            httpPost.setEntity(new StringEntity(body));
	    	CloseableHttpResponse  response = httpclient.execute(httpPost);
	    	HttpEntity entity = response.getEntity();
            String mresp =  EntityUtils.toString(entity);
            myLogger.debug("Response: ");
            myLogger.debug(mresp);
	    	int statusCode = response.getStatusLine().getStatusCode();
            return statusCode;
		} catch (Exception e) {
            e.printStackTrace();

        } finally {
            httpclient.close();
        }
		return 500;
	}
    
    public int PatchHTTP(String URL, String body) throws ClientProtocolException, IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPatch httpPatch = new HttpPatch(URL);
            httpPatch.setHeader("Content-type", "application/json");
            httpPatch.setHeader("Authorization", "Bearer "+this.apiKey);
            httpPatch.setEntity(new StringEntity(body));
            CloseableHttpResponse  response = httpclient.execute(httpPatch);
            HttpEntity entity = response.getEntity();
            String mresp =  EntityUtils.toString(entity);
            myLogger.debug("Response: ");
            myLogger.debug(mresp);
            int statusCode = response.getStatusLine().getStatusCode();
            return statusCode;
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            httpclient.close();
        }
        return 500;
    }
    
    
    public int PutHTTP(String URL, String body) throws ClientProtocolException, IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPut httpPut = new HttpPut(URL);
            httpPut.setHeader("Content-type", "application/json");
            httpPut.setHeader("Authorization", "Bearer "+this.apiKey);
            httpPut.setEntity(new StringEntity(body));
            CloseableHttpResponse  response = httpclient.execute(httpPut);
            HttpEntity entity = response.getEntity();
            String mresp =  EntityUtils.toString(entity);
            myLogger.debug("Response: ");
            myLogger.debug(mresp);
            int statusCode = response.getStatusLine().getStatusCode();
            return statusCode;
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            httpclient.close();
        }
        return 500;
    }
    
    public int DeleteHTTP(String URL) throws ClientProtocolException, IOException, Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpDelete httpDelete = new HttpDelete(URL);
            httpDelete.setHeader("Content-type", "application/json");
            httpDelete.setHeader("Authorization", "Bearer "+this.apiKey);
            CloseableHttpResponse  response = httpclient.execute(httpDelete);
            HttpEntity entity = response.getEntity();
            String mresp =  EntityUtils.toString(entity);
            myLogger.debug("Response: ");
            myLogger.debug(mresp);
            int statusCode = response.getStatusLine().getStatusCode();
            return statusCode;
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            httpclient.close();
        }
        return 500;
    }

    public String GetWorkspaceId(String workspaceName) {
        try {
            String resp = this.GetHTTP("https://api.split.io/internal/api/v2/workspaces");
        	JSONParser parser = new JSONParser();
        	Object obj = parser.parse(resp);
        	JSONObject jsTemp = (JSONObject) obj;
            JSONArray jsArray = (JSONArray) jsTemp.get("objects");
            String wsId="";
            for (int ws=0; ws < jsArray.size(); ws++) {
            	JSONObject jsItem = (JSONObject) jsArray.get(ws);
            	String temp = jsItem.get("name").toString();
                if  (temp.equals(workspaceName)) {
                    wsId=jsItem.get("id").toString();                	
                }
        	}
            return wsId;
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return "";
    }
    
    public int CreateSplit(String workspaceId, String trafficTypeName, String splitName, String description) {
        try {
            String URL = "https://api.split.io/internal/api/v2/splits/ws/"+workspaceId+"/trafficTypes/"+trafficTypeName;
            String data="{\"name\":\""+splitName+"\", \"description\": \""+description+"\"}";
            return this.ExecHTTPRequest("PostHTTP", URL, data);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return 500;
    }
    
    public int AddSplitToEnvironment(String workspaceId, String environmentName, String splitName, String definition) {
        try {
            String URL = "https://api.split.io/internal/api/v2/splits/ws/"+workspaceId+"/"+splitName+"/environments/"+environmentName;
            return this.ExecHTTPRequest("PostHTTP", URL, definition);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return 500;
    }
    
    public int KillSplit(String workspaceId, String environmentName, String splitName) {
        try {
            String URL = "https://api.split.io/internal/api/v2/splits/ws/"+workspaceId+"/"+splitName+"/environments/"+environmentName+"/kill";
            return this.ExecHTTPRequest("PutHTTP", URL, "{ \"comment\": \"Killed By Jenkins Split Admin API\"}");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 500;
    }
    
    public int DeleteSplit(String workspaceId, String splitName) {
        try {
            if (CheckSplitExists(workspaceId, splitName)) {
                String URL = "https://api.split.io/internal/api/v2/splits/ws/"+workspaceId+"/"+splitName;
                return this.ExecHTTPRequest("DeleteHTTP", URL, null);
            } else {
                myLogger.info("Split does not exist, skipping");
                return 200;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 500;
    }

    
    public int AddWhiteListToSplit(String workspaceId, String environmentName, String splitName, String treatmentName, String whitelistKey) {
        try {
            String patchData="";
            Integer treatmentOrder = this.GetTreatmentOrder(workspaceId, environmentName, splitName, treatmentName);
            if (this.CheckWhitelistSectionExist(workspaceId, environmentName, splitName, treatmentName))
                patchData="[{\"op\": \"add\", \"path\": \"/treatments/"+treatmentOrder.toString()+"/keys/-\", \"value\": \""+whitelistKey+"\"}]";
            else
                patchData="[{\"op\": \"add\", \"path\": \"/treatments/"+treatmentOrder.toString()+"/keys\", \"value\": [\""+whitelistKey+"\"]}]";
            String URL = "https://api.split.io/internal/api/v2/splits/ws/"+workspaceId+"/"+splitName+"/environments/"+environmentName;
            return this.ExecHTTPRequest("PatchHTTP", URL, patchData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 500;
    }

    private Integer GetTreatmentOrder(String workspaceId, String environmentName, String splitName, String treatmentName) {
        try {
            int cnt=0;
            String resp = this.GetSplitDefinition(workspaceId, splitName, environmentName);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            JSONObject js = (JSONObject) obj;
            JSONArray jsArray = (JSONArray)js.get("treatments");
            for (int ws=0; ws < jsArray.size(); ws++) {
                JSONObject jsItem = (JSONObject) jsArray.get(ws);
                if (treatmentName.equals(jsItem.get("name").toString())) {
                    return cnt;
                }
                cnt=cnt+1;
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    private boolean CheckWhitelistSectionExist(String workspaceId, String environmentName, String splitName, String treatmentName) {
        try {
            String resp = this.GetSplitDefinition(workspaceId, splitName, environmentName);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            JSONObject js = (JSONObject) obj;
            JSONArray jsArray = (JSONArray)js.get("treatments");
            for (int ws=0; ws < jsArray.size(); ws++) {
                JSONObject jsItem = (JSONObject) jsArray.get(ws);
                if (treatmentName.equals(jsItem.get("name").toString())) {
                    if (jsItem.containsKey("keys"))
                        return true;
                    else
                        return false;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public String GetSplitDefinition(String workspaceId, String splitName, String environmentName) {
        try {
            return this.GetHTTP("https://api.split.io/internal/api/v2/splits/ws/"+workspaceId+"/"+splitName+"/environments/"+environmentName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    
    public int CreateSplitFromYAML(String workspaceId, String environmentName, String trafficTypeName, String YAMLFile) {
        try {
            Split[] split = readSplitsFromYAML(YAMLFile);
            for (Integer i=0; i<split.length; i++) {
                String splitName = split[i].getSplitName();
                myLogger.info("Working on Split name ("+splitName+")");
                
                if (splitName==null) {
                    throw new Exception("\nSplit Name not found in YAML file in iteration "+i.toString());
                }
                if (!this.CheckSplitExists(workspaceId, splitName)) {
                    this.CreateSplit(workspaceId, trafficTypeName, splitName, "Created form Jenkins plugin");
                }
                else myLogger.info("Split name ("+splitName+") already exists in workspace! Skipping.");
                
                if (!this.CheckSplitDefinitionExists(workspaceId, environmentName, splitName)) {
                    String splitDefinitions = ConstructSplitDefinitions(split[i]);
                    myLogger.debug(splitDefinitions);
                    this.AddSplitToEnvironment(workspaceId, environmentName, splitName, splitDefinitions);
                } else myLogger.info("Split definitions for ("+splitName+") already exists in environment! Skipping.");
                                        
            }
            return 200;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 500;
    }
    
    private Split[] readSplitsFromYAML(String YAMLFile) {
        List<Split> splits = new ArrayList<Split>();
        try {
            Yaml yaml = new Yaml();
            List<Map<String, HashMap<String, Object>>> yamlSplits = yaml.load(new InputStreamReader(new FileInputStream(YAMLFile), "UTF-8"));
            for(Map<String, HashMap<String, Object>> aSplit : yamlSplits) {
                // The outter map is a map with one key, the split name
                boolean existFlag=false;
                Split split = new Split();
                Treatments treatment = new Treatments();
                for (Map.Entry<String, HashMap<String, Object>> entry : aSplit.entrySet())  {
                    myLogger.debug("Reading YMAL, Split: "+entry.getKey());
                    if (CheckSplitExistInArray(splits, entry.getKey())) {
                        existFlag=true;
                        split = GetSplit(splits, entry.getKey());
                    } else
                        split.setSplitName(entry.getKey());
                    if (entry.getValue()==null) continue;
                    for (Map.Entry<String, Object> valueEntry : entry.getValue().entrySet()) {
                        myLogger.debug("Reading YMAL, element "+valueEntry.getKey());
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
                    for (int i=0; i<splits.size(); i++) {
                        if (splits.get(i).getSplitName().equals(split.getSplitName())) {
                            splits.set(i, split);
                        }
                    }
                } else {
                    splits.add(split);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return splits.toArray(new Split[0]);
    }
    
    private boolean CheckSplitExistInArray(List<Split> splits, String name) {
        for (int i=0; i<splits.size(); i++) {
            if (splits.get(i).getSplitName().equals(name)) return true;
        }
        return false;
    }
    
    private Split GetSplit(List<Split> splits, String name) {
        for (int i=0; i<splits.size(); i++) {
            if (splits.get(i).getSplitName().equals(name)) return splits.get(i);
        }
        return null;
    }

    private String ConstructSplitDefinitions(Split split) throws Exception {
        StringBuilder JSONStructure = new StringBuilder( "" );
        if (split.getTreatments()==null) {
// Return on/off treatments with default rule set to off
            return "{\"treatments\":[{\"name\": \"on\", \"description\": \"\"}, {\"name\": \"off\", \"description\": \"\"}],\"defaultTreatment\":\"off\",\"rules\":[],\"defaultRule\":[{\"treatment\": \"off\", \"size\": 100}]}";
        }
// Adding Treatments
        JSONStructure.append(this.AddTreatmentsFromYAML(split));
    JSONStructure.append(",\"defaultTreatment\":\""+split.getTreatments()[0].getTreatment()+"\"");
// Add default Rule
        JSONStructure.append(this.AddDefaultRuleFromYAML(split));
        JSONStructure.append("}");
        return JSONStructure.toString();
    }
    
    private String AddTreatmentsFromYAML(Split split) {
        StringBuilder JSONStructure = new StringBuilder( "" );
        try {
            Treatments[] treatments = split.getTreatments();
            int treatmentsCount=0;
            JSONStructure.append("{\"treatments\":[");
            for (int j=0; j<treatments.length; j++) {
                String treatmentName = treatments[j].getTreatment();
                if (treatmentName==null) {
                    throw new Exception("\nTreatment Name not found in YAML file for Split ("+split.getSplitName()+")");
                }
                if (treatmentsCount!=0) JSONStructure.append(",");
                JSONStructure.append("{\"name\":\""+treatmentName+"\", \"description\": \"\"");
                String dynamicConfig = treatments[j].getConfig();
                if (dynamicConfig!=null) {
                    JSONStructure.append(",\"configurations\":\""+dynamicConfig+"\"");
                }
                String keys[] = treatments[j].getKeys();
                if (keys!=null) {
                    JSONStructure.append(",\"keys\":[");
                    boolean firstFlag=true;
                    for (int k=0; k<keys.length; k++) {
                        if (!firstFlag) JSONStructure.append(",");
                        JSONStructure.append("\""+keys[k]+"\"");
                        firstFlag=false;
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
            e.printStackTrace();
        }
        return JSONStructure.toString();
    }
        
    private String AddDefaultRuleFromYAML(Split split) {
        StringBuilder JSONStructure = new StringBuilder( "" );
        try {
            JSONStructure.append(",\"defaultRule\":[");
            int treatmentCount=0;
            int totPercentage=0;
            Treatments[] treatments = split.getTreatments();
            for (int j=0; j<treatments.length; j++) {
                if (treatments[j].getPercentage()!=0) {
                    String treatmentName  = treatments[j].getTreatment();
                    int percentage = treatments[j].getPercentage();
                    if (j!=0) JSONStructure.append(",");
                    JSONStructure.append("{\"treatment\":\""+treatmentName+"\", \"size\":"+ String.valueOf(percentage)+"}");
                    totPercentage+=percentage;
                    treatmentCount++;
                }
            }
            if (treatmentCount==0) {
                // return with default rule set to off if off treatment exist
                boolean offFlag=false;
                for (int i=0; i<split.getTreatments().length; i++) {
                    if (split.getTreatments()[i].getTreatment().equals("off")) offFlag=true;
                }
                if (offFlag) {
                    JSONStructure.append("{\"treatment\": \"off\", \"size\": 100}]");
                } else {
                    JSONStructure.append("{\"treatment\": \""+split.getTreatments()[0].getTreatment()+"\", \"size\": 100}]");
                }
            } else {
                if (totPercentage<100) {
                    if (totPercentage<100 && treatmentCount==split.getTreatments().length) {
                        throw new Exception("\nTotal Default Rule Percentages are less than 100 for Split ("+split.getSplitName()+")");
                    }
                    String findTreatmentNotUsedInRule="";
                    int treatmentFound=0;
                    for (int i=0; i<split.getTreatments().length; i++) {
                        if (split.getTreatments()[i].getPercentage()==0) {
                                treatmentFound=i;
                        }
                    }
                    if (treatmentFound!=0) {
                        findTreatmentNotUsedInRule=split.getTreatments()[treatmentFound].getTreatment();
                    }
                    Integer remainingPercentage=100-totPercentage;
                    JSONStructure.append(", {\"treatment\": \""+findTreatmentNotUsedInRule+"\", \"size\": "+remainingPercentage.toString()+"}");
                }
                JSONStructure.append("]");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return JSONStructure.toString();
    }
    
    public String GetSplitsList(String workspaceId) {
        try {
            return this.GetHTTP("https://api.split.io/internal/api/v2/splits/ws/"+workspaceId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public String GetSplitsInEnvironmentList(String workspaceId, String environmentName) {
        try {
            return this.GetHTTP("https://api.split.io/internal/api/v2/splits/ws/"+workspaceId+"/environments/"+environmentName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private boolean CheckSplitExists(String workspaceId, String splitName) {
        try {
            String resp= this.GetSplitsList(workspaceId);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            JSONObject js = (JSONObject) obj;
            JSONArray jsArray = (JSONArray)js.get("objects");
            String wsId="";
            for (int ws=0; ws < jsArray.size(); ws++) {
                JSONObject jsItem = (JSONObject) jsArray.get(ws);
                String temp = jsItem.get("name").toString();
                if  (temp.equals(splitName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean CheckSplitDefinitionExists(String workspaceId, String environmentName, String splitName) {
        try {
            String resp= this.GetSplitsInEnvironmentList(workspaceId, environmentName);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            JSONObject js = (JSONObject) obj;
            JSONArray jsArray = (JSONArray)js.get("objects");
            String wsId="";
            for (int ws=0; ws < jsArray.size(); ws++) {
                JSONObject jsItem = (JSONObject) jsArray.get(ws);
                String temp = jsItem.get("name").toString();
                if  (temp.equals(splitName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void CheckStatus(int statusCode) {
        if (statusCode!=200 && statusCode!=302 && statusCode!=202) {
            throw new AssertionError("Admin API Call Failed");
        }
    }

    public int DeleteSplitDefinition(String workspaceId, String environmentName, String splitName) {
        try {
            if (CheckSplitDefinitionExists(workspaceId, environmentName, splitName)) {
                String URL = "https://api.split.io/internal/api/v2/splits/ws/"+workspaceId+"/"+splitName+"/environments/"+environmentName;
                return this.ExecHTTPRequest("DeleteHTTP", URL, null);
            } else {
                myLogger.info("Split Definition does not exist, skipping");
                return 200;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 500;
    }
    
}
