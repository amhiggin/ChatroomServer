package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatroomServer {

	private static final int UNDEFINED_JOIN_ID = -1;
	private static final String JOIN_CHATROOM_IDENTIFIER = "JOIN_CHATROOM: ";
	private static final String CHAT_IDENTIFIER = "CHAT: ";
	private static final String LEAVE_CHATROOM_IDENTIFIER = "LEAVE_CHATROOM: ";
	private static final String JOIN_ID_IDENTIFIER = "JOIN_ID: ";
	private static final String CLIENT_NAME_IDENTIFIER = "CLIENT_NAME: ";

	public static AtomicInteger clientId;
	private static ServerSocket serverSocket;
	private static AtomicBoolean terminateServer;
	private static ConcurrentSkipListSet<Chatroom> activeChatRooms;
	private static ConcurrentSkipListSet<ClientNode> connectedClients;
	private static int serverPort;

	/*
	 * Server port is passed as arg[0]
	 */
	public static void main(String[] args) {
		try {
			initialiseServer(args[0]);
			while (!terminateServer.equals(Boolean.TRUE)) {
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

	public static void initialiseServer(String portSpecified) throws Exception {
		serverPort = Integer.parseInt(portSpecified);
		serverSocket = new ServerSocket(serverPort);
		initialiseServerManagementVariables();
		System.out.println(String.format("%s>> Server started on port %s...", getCurrentDateTime(), portSpecified));
	}

	private static String getCurrentDateTime() {
		DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyy HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		return format.format(now);
	}

	private static void initialiseServerManagementVariables() {
		connectedClients = new ConcurrentSkipListSet<ClientNode>();
		activeChatRooms = new ConcurrentSkipListSet<Chatroom>();
		terminateServer = new AtomicBoolean(Boolean.FALSE);
		clientId = new AtomicInteger(0);
	}

	private static synchronized void handleIncomingConnection() throws Exception {
		Socket clientSocket = serverSocket.accept();
		System.out.println(String.format("%s>> Connection received from %s...", getCurrentDateTime(),
				clientSocket.getInetAddress().toString()));

		ClientRequest clientRequest = requestedAction(clientSocket);
		ClientNode client = extractClientInfo(clientSocket, clientRequest);
		List<String> message = getFullMessageFromClient(clientSocket);
		ClientThread newClientConnectionThread = new ClientThread(client, clientRequest, message);

		newClientConnectionThread.start();
	}

	private static List<String> getFullMessageFromClient(Socket clientSocket) throws IOException {
		BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		List<String> lines = new LinkedList<String>(); // create a new list
		String line = inFromClient.readLine();
		while (line != null) {
			lines.add(line);
			line = inFromClient.readLine();
		}
		return lines;
	}

	public static ClientNode extractClientInfo(Socket clientSocket, ClientRequest requestType) throws IOException {
		List<String> fullMessage = getFullMessageFromClient(clientSocket);
		switch (requestType) {
		case JOIN_CHATROOM:
			return new ClientNode(clientSocket, fullMessage.get(3).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					fullMessage.get(0).split(JOIN_CHATROOM_IDENTIFIER, 0)[1], clientId.getAndIncrement());
		case CHAT:
			return new ClientNode(clientSocket, fullMessage.get(2).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					fullMessage.get(0).split(CHAT_IDENTIFIER, 0)[1],
					Integer.parseInt(fullMessage.get(1).split(JOIN_ID_IDENTIFIER, 0)[1]));
		case LEAVE_CHATROOM:
			return new ClientNode(clientSocket, fullMessage.get(2).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					fullMessage.get(0).split(LEAVE_CHATROOM_IDENTIFIER, 0)[1],
					Integer.parseInt(fullMessage.get(1).split(JOIN_ID_IDENTIFIER, 0)[1]));
		case DISCONNECT:
			return new ClientNode(clientSocket, fullMessage.get(2).split(CLIENT_NAME_IDENTIFIER, 0)[1], null,
					UNDEFINED_JOIN_ID);
		case HELO:
			return new ClientNode(clientSocket, null, null, UNDEFINED_JOIN_ID);
		case KILL_SERVICE:
			return new ClientNode(null, null, null, UNDEFINED_JOIN_ID);
		default:
			return null;
		}
	}

	public static void shutdown() {
		try {
			System.out.println(String.format("%s>> Server shutting down...", getCurrentDateTime()));
			for (ClientNode node : getAllConnectedClients()) {
				node.getConnection().close();
			}
			getActiveChatRooms().clear();
			serverSocket.close();
			System.out.println(String.format("%s>> Server shut down successfully. Goodbye.", getCurrentDateTime()));
		} catch (Exception e) {
			System.out.println(String.format("%s>> Error occurred when trying to shut down the server: %s",
					getCurrentDateTime(), e));
		}
	}

	static synchronized void recordClientChangeWithServer(ClientRequest requestedAction, ClientNode clientNode)
			throws Exception {
		if (clientNode != null) {
			if (requestedAction.equals(ClientRequest.JOIN_CHATROOM) && !getAllConnectedClients().contains(clientNode)
					&& (retrieveRequestedChatroomIfExists(clientNode.getChatroomId()) != null)) {
				addClientRecordToServer(clientNode);
			} else if (requestedAction.equals(ClientRequest.DISCONNECT)
					&& getAllConnectedClients().contains(clientNode)) {
				removeClientRecordFromServer(clientNode, retrieveRequestedChatroomIfExists(clientNode.getChatroomId()));
			}
		}
		// If we have left the chatroom, we want to keep the record that we were
		// in that chatroom (for repeated LEAVE requests)
	}

	public static ClientRequest requestedAction(Socket clientSocket) throws IOException {
		String requestType = parseClientRequestType(clientSocket);
		try {
			return ClientRequest.valueOf(requestType);
		} catch (Exception e) {
			return null;
		}
	}

	private static String parseClientRequestType(Socket clientSocket) throws IOException {
		BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		String clientSentence = inFromClient.readLine();
		String[] requestType = clientSentence.split(": ", 0);
		return requestType[0];
	}

	public static void addClientRecordToServer(ClientNode clientNode) {
		if (!getAllConnectedClients().contains(clientNode)) {
			getAllConnectedClients().add(clientNode);
		}
	}

	private static void removeClientRecordFromServer(ClientNode clientNode, Chatroom requestedChatroom)
			throws IOException {
		for (Chatroom chatroom : getActiveChatRooms()) {
			if (chatroom == requestedChatroom) {
				chatroom.getSetOfConnectedClients().remove(clientNode);
				break;
			}
		}
		// Remove record from server too
		connectedClients.remove(clientNode);
		clientNode.getConnection().close();
		return;
	}

	public static ConcurrentSkipListSet<Chatroom> getActiveChatRooms() {
		return activeChatRooms;
	}

	public static Chatroom retrieveRequestedChatroomIfExists(String requestedChatroomToJoin) {
		for (Chatroom chatroom : activeChatRooms) {
			if (chatroom.getChatroomId() == requestedChatroomToJoin) {
				return chatroom;
			}
		}
		return null;
	}

	public static int getServerPort() {
		return serverPort;
	}

	public static synchronized ConcurrentSkipListSet<ClientNode> getAllConnectedClients() {
		return connectedClients;
	}

	public static void setTerminateServer(AtomicBoolean value) {
		terminateServer = value;
	}

	public static void outputRequestErrorMessageToConsole(String errorResponse, ClientNode clientNode) {
		String output = String.format("%s>> ERROR processing request (client %s): %s", getCurrentDateTime(),
				clientNode.getName(), errorResponse);
		System.out.println(output);
	}

	public static ServerSocket getServerSocket() {
		return serverSocket;
	}

}
