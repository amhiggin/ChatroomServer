package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatroomServer {

	private static ServerSocket serverSocket;
	private static AtomicBoolean terminateServer;
	private static ConcurrentSkipListMap<Chatroom, ConcurrentSkipListSet<ClientNode>> activeChatRooms;
	private static int serverPort;

	/*
	 * Server port is passed as arg[0] - TODO handle "KILL_SERVICE\n" case
	 */
	public static void main(String[] args) {
		try {
			initialiseServer(args);
			while (true && !terminateServer.equals(Boolean.TRUE)) {
				try {
					handleIncomingConnection();
				} catch (Exception e) {
					System.out.println(String.format("Error handling connection: %s", e));
				}
			}
		} catch (Exception e) {
			System.out.println(String.format("Error while initialising server: %s", e));
		} finally {
			shutdown();
		}
	}

	private static void initialiseServer(String[] args) throws Exception {
		if (args.length != 1) {
			throw new Exception("Incorrect initialisation arguments supplied: expected port number.");
		}
		serverPort = Integer.parseInt(args[0]);
		serverSocket = new ServerSocket(getServerPort());
		initialiseServerManagementVariables();
		System.out.println(String.format("Server listening on port %s...", args[0]));
	}

	private static void initialiseServerManagementVariables() {
		activeChatRooms = new ConcurrentSkipListMap<Chatroom, ConcurrentSkipListSet<ClientNode>>();
		terminateServer = new AtomicBoolean(Boolean.FALSE);
	}

	private static synchronized void handleIncomingConnection() throws Exception {
		Socket clientSocket = serverSocket.accept();
		System.out.println(String.format("Connection received from %s...", clientSocket.getInetAddress().toString()));

		// Extract connection-specific info
		ClientNode client = extractClientInfo(clientSocket);
		ClientRequest clientRequest = requestedAction(clientSocket);
		String message = getFullMessageFromClient(clientSocket);
		ClientThread newClientConnectionThread = new ClientThread(client, clientRequest, message,
				serverSocket.getLocalPort());

		// Execute and update
		newClientConnectionThread.run();// or .start()??
		recordClientChangeWithServer(clientRequest, client);
	}

	private static String getFullMessageFromClient(Socket clientSocket) {
		// TODO Auto-generated method stub
		return null;
	}

	public static ClientNode extractClientInfo(Socket clientSocket) throws IOException {
		BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		List<String> lines = new LinkedList<String>(); // create a new list
		String line = inFromClient.readLine();
		while (line != null) { // loop till you have no more lines
			lines.add(line); // add the line to your list
			line = inFromClient.readLine(); // try to read another line
		}

		String clientName = (lines.get(3).split("CLIENT_NAME:", 0))[1];
		String chatroomId = (lines.get(0).split("JOIN_CHATROOM:", 0))[0];
		return new ClientNode(clientSocket, clientName, chatroomId);
	}

	private static void shutdown() {
		try {
			System.out.println("Initiating server shutdown...");
			for (Entry<Chatroom, ConcurrentSkipListSet<ClientNode>> entry : getActiveChatRooms().entrySet()) {
				for (ClientNode client : entry.getValue()) {
					client.getConnection().close();
				}
			}
			getActiveChatRooms().clear();
			serverSocket.close();
		} catch (Exception e) {
			System.out.println(String.format("Error occurred when trying to shut down the server: %s", e));
		}
	}

	static synchronized void recordClientChangeWithServer(ClientRequest requestedAction, ClientNode clientNode)
			throws Exception {
		if (clientNode != null) {
			if (requestedAction.equals(ClientRequest.JOIN_CHATROOM)
					&& !getActiveChatRooms().values().contains(clientNode)) {
				addClientRecordToServer(clientNode);
			} else if (requestedAction.equals(ClientRequest.LEAVE_CHATROOM)
					&& getActiveChatRooms().values().contains(clientNode)) {
				removeClientRecordFromServer(clientNode, retrieveRequestedChatroomIfExists(clientNode.getChatroomId()));
			}
		}
	}

	public static ClientRequest requestedAction(Socket clientSocket) throws IOException {
		String requestType = parseClientRequest(clientSocket);
		try {
			return ClientRequest.valueOf(requestType);
		} catch (Exception e) {
			return null;
		}
	}

	private static String parseClientRequest(Socket clientSocket) throws IOException {
		BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		String clientSentence = inFromClient.readLine();
		String[] requestType = clientSentence.split(":", 0);
		return requestType[0];
	}

	private static void addClientRecordToServer(ClientNode clientNode) {
		for (Entry<Chatroom, ConcurrentSkipListSet<ClientNode>> entry : getActiveChatRooms().entrySet()) {
			if (entry.getKey().getChatroomId() == clientNode.getChatroomId()) {
				if (!entry.getValue().contains(clientNode)) {
					entry.getValue().add(clientNode);
					return;
				}
			}
		}
	}

	private static void removeClientRecordFromServer(ClientNode clientNode, Chatroom chatroom) throws IOException {
		for (Entry<Chatroom, ConcurrentSkipListSet<ClientNode>> entry : getActiveChatRooms().entrySet()) {
			if (entry.getKey() == chatroom) {
				entry.getValue().remove(clientNode);
				clientNode.getConnection().close();
				return;
			}
		}
	}

	public static ConcurrentSkipListMap<Chatroom, ConcurrentSkipListSet<ClientNode>> getActiveChatRooms() {
		return activeChatRooms;
	}

	public static Chatroom retrieveRequestedChatroomIfExists(String requestedChatroomToJoin) {
		for (Entry<Chatroom, ConcurrentSkipListSet<ClientNode>> entry : activeChatRooms.entrySet()) {
			if (entry.getKey().getChatroomId() == requestedChatroomToJoin) {
				return entry.getKey();
			}
		}
		return null;
	}

	public static int getServerPort() {
		return serverPort;
	}

	public static synchronized ConcurrentSkipListSet<ClientNode> getAllConnectedClients() {
		ConcurrentSkipListSet<ClientNode> allClients = new ConcurrentSkipListSet<ClientNode>();
		for (ConcurrentSkipListSet<ClientNode> clients : activeChatRooms.values()) {
			for (ClientNode node : clients) {
				allClients.add(node);
			}
		}
		return allClients;
	}

	public static void handleError(String string) {
		// TODO Auto-generated method stub
		// TODO implement using the Error enum

	}

}
