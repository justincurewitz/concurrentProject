package ee360p;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
 
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
 
import org.json.JSONException;
import org.json.JSONObject;
 
import com.google.common.collect.Maps;

public class ChatServer {
	private JSONUtils jsu = new JSONUtils();
	
	private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<Session>());
	private static final HashMap<String, String> nameSessPair = new HashMap<String, String>();
	
	// Getting query params
    public static Map<String, String> getQueryMap(String query) {
        Map<String, String> map = Maps.newHashMap();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] nameval = param.split("=");
                map.put(nameval[0], nameval[1]);
            }
        }
        return map;
    }
    
    public void onOpen(Session session) {
        System.out.println(session.getId() + " has opened a connection");
        Map<String, String> queryParams = getQueryMap(session.getQueryString()); 
        String name = "";
 
        if (queryParams.containsKey("name")) {
            // Getting client name via query param
            name = queryParams.get("name");
            try {
                name = URLDecoder.decode(name, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            // Mapping client name and session id
            nameSessPair.put(session.getId(), name);
        }
        
        // Adding session to session list
        sessions.add(session);
        try {
            // Sending session id to the client that just connected
            session.getBasicRemote().sendText(
                    jsu.getClientDetailsJson(session.getId(),
                            "Your session details"));
        } catch (IOException e) {
            e.printStackTrace();
        }
 
        // Notifying all the clients about new person joined
        sendMessageToAll(session.getId(), name, " joined conversation!", true, false);
    }
    
    public void onMessage(String message, Session session) {
        System.out.println("Message from " + session.getId() + ": " + message);
        String msg = null;
 
        // Parsing the json and getting message
        try {
            JSONObject jObj = new JSONObject(message);
            msg = jObj.getString("message");
        } catch (JSONException e) {
            e.printStackTrace();
        }
 
        // Sending the message to all clients
        sendMessageToAll(session.getId(), nameSessPair.get(session.getId()), msg, false, false);
    }
    
    public void onClose(Session session) {    	 
        System.out.println("Session " + session.getId() + " has ended");
        // Getting the client name that exited
        String name = nameSessPair.get(session.getId());
 
        // removing the session from sessions list
        sessions.remove(session);
 
        // Notifying all the clients about person exit
        sendMessageToAll(session.getId(), name, " left conversation!", false, true);
    }
    
    private void sendMessageToAll(String sessionId, String name,
            String message, boolean isNewClient, boolean isExit) {
 
        // Looping through all the sessions and sending the message individually
        for (Session s : sessions) {
            String json = null;
 
            // Checking if the message is about new client joined
            if (isNewClient) {
                json = jsu.getNewClientJson(sessionId, name, message,
                        sessions.size());
 
            } else if (isExit) {
                // Checking if the person left the conversation
                json = jsu.getClientExitJson(sessionId, name, message,
                        sessions.size());
            } else {
                // Normal chat conversation message
                json = jsu.getSendAllMessageJson(sessionId, name, message);
            }
 
            try {
                System.out.println("Sending Message To: " + sessionId + ", "
                        + json);
 
                s.getBasicRemote().sendText(json);
            } catch (IOException e) {
                System.out.println("error in sending. " + s.getId() + ", "
                        + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
