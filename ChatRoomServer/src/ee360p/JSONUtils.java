package ee360p;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtils {
	private static final String FLAG_SELF="self", FLAG_NEW="new", FLAG_MESSAGE="message", FLAG_EXIT="exit";
	
	public JSONUtils() {
		
	}
	
	public String getClientDetailsJson(String sessionId, String message) {
		String json = null;
		
		try {
			JSONObject obj = new JSONObject();
			obj.put("flag", FLAG_SELF);
			obj.put("sessionId", sessionId);
			obj.put("message", message);
			
			json = obj.toString();
		} catch(JSONException e) {e.printStackTrace();}
		
		return json;
	}
	
	public String getNewClientJson(String sessionId, String name, String message, int onlineCount){
		String json = null;
		
		try{
			JSONObject obj = new JSONObject();
			obj.put("flag", FLAG_NEW);
			obj.put("name", name);
			obj.put("sessionId", sessionId);
			obj.put("message", message);
			obj.put("onlineCount", onlineCount);
			
			json = obj.toString();
		} catch (JSONException e) {e.printStackTrace();}
		
		return json;
	}
	
	public String getClientExitJson(String sessionId, String name, String message, int onlineCount) {
		String json = null;
		
		try {
			JSONObject Obj = new JSONObject();
            Obj.put("flag", FLAG_EXIT);
            Obj.put("name", name);
            Obj.put("sessionId", sessionId);
            Obj.put("message", message);
            Obj.put("onlineCount", onlineCount);
 
            json = Obj.toString();
		} catch (JSONException e) {e.printStackTrace();}
		
		return json;
	}
	
	public String getSendAllMessageJson(String sessionId, String fromName, String message) {
		String json = null;
		 
        try {
            JSONObject jObj = new JSONObject();
            jObj.put("flag", FLAG_MESSAGE);
            jObj.put("sessionId", sessionId);
            jObj.put("name", fromName);
            jObj.put("message", message);
 
            json = jObj.toString();
 
        } catch (JSONException e) {
            e.printStackTrace();
        }
 
        return json;
	}
	
}
