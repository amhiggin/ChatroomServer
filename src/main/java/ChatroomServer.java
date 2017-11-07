package main.java;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.LocalDateTime;

public class ChatroomServer {

	private static final String SPLIT_CRITERIA = ": ";
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
					outputServiceErrorMessageToConsole(e.getMessage());
				}
			}
		} catch (Exception e) {
			outputServiceErrorMessageToConsole(e.getMessage());
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
		LocalDateTime now = new LocalDateTime();
		return now.toString();
	}

	private static void initialiseServerManagementVariables() {
		connectedClients = new ConcurrentSkipListSet<ClientNode>();
		activeChatRooms = new ConcurrentSkipListSet<Chatroom>();
		terminateServer = new AtomicBoolean(Boolean.FALSE);
		clientId = new AtomicInteger(0);
	}

	private static synchronized void handleIncomingConnection() throws Exception {
		Socket clientSocket = serverSocket.accept();
		clientSocket.setKeepAlive(true);
		System.out.println(String.format("%s>> Connection received from %s...", getCurrentDateTime(),
				clientSocket.getInetAddress().toString()));

		List<String> message = getFullMessageFromClient(clientSocket);
		ClientRequest clientRequest = requestedAction(message);
		ClientNode client = extractClientInfo(clientSocket, clientRequest, message);
		ClientThread newClientConnectionThread = new ClientThread(client, clientRequest, message);

		ChatroomServer.printMessageToConsole("Running thread...");
		newClientConnectionThread.run();
	}

	public static synchronized List<String> getFullMessageFromClient(Socket clientSocket) throws IOException {
		printMessageToConsole("retrieving message from the client (getFullMessageFromClient)"); // TODO
		BufferedInputStream inputStream = new BufferedInputStream(clientSocket.getInputStream());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		int result = inputStream.read();
		while ((result != -1) && (inputStream.available() > 0)) {
			outputStream.write((byte) result);
			printMessageToConsole("added new byte " + result);
			result = inputStream.read();
			printMessageToConsole("reading next byte .." + result);
		}
		// Assuming UTF-8 encoding
		String inFromClient = outputStream.toString("UTF-8");
		printMessageToConsole("inFromClient is: " + inFromClient);
		List<String> lines = getRequestStringAsArrayList(inFromClient);

		printMessageToConsole("Thats all the lines!");
		return lines;
	}

	private static List<String> getRequestStringAsArrayList(String inFromClient) {
		String[] linesArray = inFromClient.split("\n");
		List<String> lines = new ArrayList<String>();
		for (String line : linesArray) {
			lines.add(line);
		}
		return lines;
	}

	public static synchronized ClientNode extractClientInfo(Socket clientSocket, ClientRequest requestType,
			List<String> message) throws IOException {
		printMessageToConsole("in extractClientInfo method");
		switch (requestType) {
		case JOIN_CHATROOM:
			return new ClientNode(clientSocket, message.get(3).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					message.get(0).split(JOIN_CHATROOM_IDENTIFIER, 0)[1], clientId.getAndIncrement());
		case CHAT:
			return new ClientNode(clientSocket, message.get(2).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					message.get(0).split(CHAT_IDENTIFIER, 0)[1],
					Integer.parseInt(message.get(1).split(JOIN_ID_IDENTIFIER, 0)[1]));
		case LEAVE_CHATROOM:
			return new ClientNode(clientSocket, message.get(2).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					message.get(0).split(LEAVE_CHATROOM_IDENTIFIER, 0)[1],
					Integer.parseInt(message.get(1).split(JOIN_ID_IDENTIFIER, 0)[1]));
		case DISCONNECT:
			return new ClientNode(clientSocket, message.get(2).split(CLIENT_NAME_IDENTIFIER, 0)[1], null,
					UNDEFINED_JOIN_ID);
		case HELO:
			printMessageToConsole("Helo client node created");
			return new ClientNode(clientSocket, null, null, UNDEFINED_JOIN_ID);
		case KILL_SERVICE:
			return new ClientNode(null, null, null, UNDEFINED_JOIN_ID);
		default:
			printMessageToConsole("Null clientnode created");
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

	public static synchronized ClientRequest requestedAction(List<String> message) throws IOException {
		printMessageToConsole("In requestedAction method");
		String requestType = parseClientRequestType(message);
		try {
			ClientRequest clientRequest = ClientRequest.valueOf(requestType);
			printMessageToConsole("The parsed request type matched with " + clientRequest.getValue());
			return clientRequest;
		} catch (Exception e) {
			outputServiceErrorMessageToConsole("Error occurred trying to fetch the request type");
			return null;
		}
	}

	private static String parseClientRequestType(List<String> message) throws IOException {
		printMessageToConsole("In parseClientRequestType method");
		String[] requestType = message.get(0).split(SPLIT_CRITERIA, 0);
		// TODO refine message
		printMessageToConsole(String.format("Parsed request type (first time) '%s", requestType[0]));
		if (requestType[0].contains("HELO")) {
			String temp = requestType[0];
			String[] splitString = temp.split(" ", 0);
			requestType[0] = splitString[0];
		}
		printMessageToConsole(String.format("Parsed request type '%s", requestType[0]));

		return requestType[0];
	}

	public static void addClientRecordToServer(ClientNode clientNode) {
		if (!getAllConnectedClients().contains(clientNode)) {
			getAllConnectedClients().add(clientNode);
		}
	}

	private static void removeClientRecordFromServer(ClientNode clientNode, Chatroom requestedChatroom)
			throws IOException {
		// Note this involves removing from chatroom too
		for (Chatroom chatroom : getActiveChatRooms()) {
			if (chatroom == requestedChatroom) {
				chatroom.getSetOfConnectedClients().remove(clientNode);
				break;
			}
		}
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
		String output = String.format("%s>> Error processing request (client %s): %s", getCurrentDateTime(),
				clientNode.getName(), errorResponse);
		System.out.println(output);
	}

	public static void outputServiceErrorMessageToConsole(String errorMessage) {
		String output = String.format("%s>> Service error: %s", getCurrentDateTime(), errorMessage);
		System.out.println(output);
	}

	public static ServerSocket getServerSocket() {
		return serverSocket;
	}

	public static void printMessageToConsole(String message) {
		System.out.println(String.format("%s>> %s", getCurrentDateTime(), message));

	}

}
