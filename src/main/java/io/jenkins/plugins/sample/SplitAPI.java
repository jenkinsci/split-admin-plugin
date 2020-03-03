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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONArray; 
import org.json.simple.JSONObject; 
import org.json.simple.parser.JSONParser;


public class SplitAPI {
	
	Logger myLogger = LoggerFactory.getLogger(SplitAPI.class);
	String apiKey="";
	
	SplitAPI(String adminApiKey) {
		this.apiKey=adminApiKey;
	}
	
	public String GetHTTP(String URL) throws ClientProtocolException, IOException, Exception {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
	    	HttpGet httpGet = new HttpGet(URL);
            httpGet.setHeader("Content-type", "application/json");
            httpGet.setHeader("Authorization", "Bearer "+this.apiKey);
	    	CloseableHttpResponse  response = httpclient.execute(httpGet);
	    	HttpEntity entity = response.getEntity();
            String mresp =  EntityUtils.toString(entity);
            System.out.println("Response: ");            
            System.out.println(mresp);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode!=200 && statusCode!=302 && statusCode!=202) {
                throw new Exception(mresp);
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
            System.out.println("Response: ");            
            System.out.println(mresp);
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
            System.out.println("Response: ");
            System.out.println(mresp);
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
            System.out.println("Response: ");
            System.out.println(mresp);
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
            System.out.println("Response: ");
            System.out.println(mresp);
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
            return this.PostHTTP(URL, data);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return 500;
    }
    
    public int AddSplitToEnvironment(String workspaceId, String environmentName, String splitName, String definition) {
        try {
            String URL = "https://api.split.io/internal/api/v2/splits/ws/"+workspaceId+"/"+splitName+"/environments/"+environmentName;
            return this.PostHTTP(URL, definition);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return 500;
    }
    
    public int KillSplit(String workspaceId, String environmentName, String splitName) {
        try {
            String URL = "https://api.split.io/internal/api/v2/splits/ws/"+workspaceId+"/"+splitName+"/environments/"+environmentName+"/kill";
            return this.PutHTTP(URL, "{ \"comment\": \"Killed By Jenkins Split Admin API\"}");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 500;
    }
    
    public int DeleteSplit(String workspaceId, String splitName) {
        try {
            String URL = "https://api.split.io/internal/api/v2/splits/ws/"+workspaceId+"/"+splitName;
            return this.DeleteHTTP(URL);
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
            return this.PatchHTTP(URL, patchData);
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
}
