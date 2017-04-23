package ee360p;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@ServerEndpoint("/chat")
public class SocketServer {
	//set to store blackjack deck and players
	private static final List<Card> deck = Collections.synchronizedList(new ArrayList<Card>());
	private static final Set<String> players = Collections.synchronizedSet(new HashSet<String>());
	private static final HashMap<String, ArrayList<Card>> hands = new HashMap<String, ArrayList<Card>>();
	//service to time blackjack these handle turns
	private static final ScheduledExecutorService exS = Executors.newSingleThreadScheduledExecutor();
	private static ArrayList<String> curPlayers = new ArrayList<String>();
	private static int tokenHolder = -1;
	// set to store all the live sessions
	private static final Set<Session> sessions = Collections
			.synchronizedSet(new HashSet<Session>());
	private static ScheduledFuture<?> nextTurn;
	// Mapping between session and person name
	private static final HashMap<String, String> nameSessionPair = new HashMap<String, String>();
	private JSONUtils jsonUtils = new JSONUtils();
	private static boolean entryOpen = false;
	
	
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

	/**
	 * Called when a socket connection opened
	 * */
	@OnOpen
	public void onOpen(Session session) {

		//System.out.println(session.getId() + " has opened a connection");

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
			nameSessionPair.put(session.getId(), name);
		}

		// Adding session to session list
		sessions.add(session);

		try {
			// Sending session id to the client that just connected
			session.getBasicRemote().sendText(
					jsonUtils.getClientDetailsJson(session.getId(),
							"Your session details"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Notifying all the clients about new person joined
		sendMessageToAll(session.getId(), name, " joined conversation!", true,
				false);

	}

	/**
	 * method called when new message received from any client
	 * 
	 * @param message
	 *            JSON message from client
	 * */
	@OnMessage
	public void onMessage(String message, Session session) {

		//System.out.println("Message from " + session.getId() + ": " + message);

		String msg = null;
		String sId = null;
		// Parsing the json and getting message
		try {
			JSONObject jObj = new JSONObject(message);
			msg = jObj.getString("message");
			sId = jObj.getString("sessionId");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (msg.charAt(0) == '/'){
			String cmd = msg.substring(1).toLowerCase();
			if (cmd.equals("blackjack")){
				blackjack();
				return;
			} else if (cmd.equals("join") && entryOpen) {
				String name = nameSessionPair.get(sId);
				if (players.add(sId)) {
					sendMessageToAll(session.getId(), "Server", name+" has joined Blackjack!", false, false);
					Card cpriv = getCard();
					String card_private = jsonUtils.getCardMessage(session.getId(), cpriv.name, cpriv.suit, nameSessionPair.get(session.getId()));
					try {
						session.getBasicRemote().sendText(card_private);
					} catch (IOException e) {
						e.printStackTrace();
					}
					Card cpub = getCard();
					String card_pub = jsonUtils.getCardMessage(session.getId(), cpub.name, cpub.suit, nameSessionPair.get(session.getId()));
					sendCardToAll(card_pub);
					ArrayList<Card> hand = new ArrayList<Card>();
					hand.add(cpub);
					hand.add(cpriv);
					hands.put(session.getId(), new ArrayList<Card>(hand));
				}
				return;
				
			} else if (cmd.equals("hand") && players.contains(sId)) {
				sendHand(session);
				return;
				
			}else if (cmd.equals("hit") && curPlayers.get(tokenHolder).equals(sId)){
				hit(session);
				return;
			} else {return;}
		}
		// Sending the message to all clients
		sendMessageToAll(session.getId(), nameSessionPair.get(session.getId()),
				msg, false, false);
	}
	private void sendCardToAll(String json) {
		for (Session s: sessions) {
			try {
				s.getBasicRemote().sendText(json);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void sendHand(Session s) {
		ArrayList<Card> hand = hands.get(s.getId());
		for (Card c:hand) {
			String card_private = jsonUtils.getCardMessage(s.getId(), c.name, c.suit, nameSessionPair.get(s.getId()));
			try {
				s.getBasicRemote().sendText(card_private);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void hit(Session s) {
		Card next = getCard();
		ArrayList<Card> hand = hands.get(s.getId());
		hand.add(next);
		int total = 0;
		for (Card c: hand) {
			total += c.val;
		}
		sendMessageToAll("-1", "Server", nameSessionPair.get(s.getId())+" hit and got:", false, false);
		String card = jsonUtils.getCardMessage(s.getId(), next.name, next.suit, nameSessionPair.get(s.getId()));
		sendCardToAll(card);
		sendMessageToAll("-1", "Server", nameSessionPair.get(s.getId())+" has "+total, false, false);
	}
	
	private Card getCard() {
		int random = (int) (Math.random()*deck.size());
		Card toreturn = deck.get(random);
		deck.remove(random);
		return toreturn;
	}
	/**
	 * Method called when a connection is closed
	 * */
	@OnClose
	public void onClose(Session session) {

		//System.out.println("Session " + session.getId() + " has ended");

		// Getting the client name that exited
		String name = nameSessionPair.get(session.getId());

		// removing the session from sessions list
		sessions.remove(session);

		// Notifying all the clients about person exit
		sendMessageToAll(session.getId(), name, " left conversation!", false,
				true);

	}

	/**
	 * Method to send message to all clients
	 * 
	 * @param sessionId
	 * @param message
	 *            message to be sent to clients
	 * @param isNewClient
	 *            flag to identify that message is about new person joined
	 * @param isExit
	 *            flag to identify that a person left the conversation
	 * */
	private void sendMessageToAll(String sessionId, String name,
			String message, boolean isNewClient, boolean isExit) {

		// Looping through all the sessions and sending the message individually
		for (Session s : sessions) {
			String json = null;

			// Checking if the message is about new client joined
			if (isNewClient) {
				json = jsonUtils.getNewClientJson(sessionId, name, message,
						sessions.size());

			} else if (isExit) {
				// Checking if the person left the conversation
				json = jsonUtils.getClientExitJson(sessionId, name, message,
						sessions.size());
			} else {
				// Normal chat conversation message
				json = jsonUtils
						.getSendAllMessageJson(sessionId, name, message);
			}

			try {
				//System.out.println("Sending Message To: " + sessionId + ", "
				//		+ json);

				s.getBasicRemote().sendText(json);
			} catch (IOException e) {
				System.out.println("error in sending. " + s.getId() + ", "
						+ e.getMessage());
				e.printStackTrace();
			}
		}
	}
	private void createDeck(List<Card> s) {
		s.clear();
		String[] names = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
		String[] suits = {"Spades", "Clubs", "Hearts", "Diamonds"};
		for (int i = 0; i < suits.length; i++){
			for (int j = 0; j < names.length; j++) {
				s.add(new Card(names[j], suits[i]));
			}
		}
	}
	
	public void blackjack() {
		System.out.println("Starting blackjack");
		players.clear();
		createDeck(deck);
		entryOpen = true;
		sendMessageToAll("-1", "Server", "Starting Blackjack!\nType /join to enter.", false, false);
		exS.schedule(new Runnable() {
			@Override
			public void run() {
				endEntry();
			}
		}, 15, TimeUnit.SECONDS);
	}
	
	public void passToken() {
		if (tokenHolder == curPlayers.size()-1){
			tokenHolder = -1;
			System.out.println("Game ending");
			gameEnd();
		} else {
			tokenHolder++;
			System.out.println("token passed to player# " + tokenHolder);
			sendMessageToAll("-1", "Server", nameSessionPair.get(curPlayers.get(tokenHolder))+"'s turn!", false, false);
			nextTurn = exS.schedule(new Runnable() {
				@Override
				public void run() {
					passToken();
				}
			}, 15, TimeUnit.SECONDS);
			System.out.println("next pass scheduled");
		}
	}
	
	public void gameEnd(){
		//nextTurn.cancel(true);
		sendMessageToAll("-1", "Server", "Game Over!", false, false);
		int best = 0;
		String winner = "Nobody";
		for (int i = 0; i < curPlayers.size(); i++) {
			String sid = curPlayers.get(i);
			String name = nameSessionPair.get(sid);
			ArrayList<Card> hand = hands.get(sid);
			int total = 0;
			for (Card c:hand) {
				total+=c.val;
			}
			if (total > best && total <= 21){
				best = total;
				winner = name;
			}
		}
		curPlayers.clear();
		sendMessageToAll("-1", "Server", "Winner is: "+winner+" with total: "+best+".", false, false);
	}
	
	public void endEntry() {
		entryOpen = false;
		sendMessageToAll("-1", "Server", "Entry closed", false, false);
		curPlayers.clear();
		curPlayers.addAll(players);
		String allPlayers = "";
		for (String s: curPlayers){
			allPlayers = allPlayers + nameSessionPair.get(s) + " ";
		}
		sendMessageToAll("-1", "Server", "Blackjack Players: " + allPlayers, false, false);
		passToken();
	}
	
	private class Card {
		int val;
		String name;
		String suit;
		
		Card(String name, String suit) {
			try {
				val = Integer.parseInt(name);
			} catch(NumberFormatException e) {
				val = 10;
			}
			this.name = name;
			this.suit = suit;
		}
		@Override
		public int hashCode() {
			int suit_val = 0;
			switch(suit) {
			case "Spades": suit_val = 1;
			case "Clubs": suit_val = 2;
			case "Hearts": suit_val = 3;
			case "Diamonds": suit_val = 4;
			}
			return val + (suit_val*100);
		}
	}
}