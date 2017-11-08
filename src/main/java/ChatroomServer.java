package main.java;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
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

	public static ThreadPoolExecutor threadPoolExecutor;
	public static AtomicInteger nextClientId;
	public static AtomicInteger nextChatroomId;
	private static ServerSocket serverSocket;
	private static boolean running;
	private static ConcurrentSkipListSet<Chatroom> activeChatRooms;
	private static ConcurrentSkipListSet<ClientNode> connectedClients;
	static int serverPort;
	static String serverIP;

	/*
	 * Server port is passed as arg[0]
	 */
	public static void main(String[] args) {
		try {
			initialiseServer(args[0]);
			while (running) {
				printServerMessageToConsole("before loop");
				handleIncomingConnection();
				printServerMessageToConsole("after loop");
			}
		} catch (Exception e) {
			outputServiceErrorMessageToConsole(e.getMessage());
		} finally {
			shutdown();
		}
	}

	public static void initialiseServer(String portSpecified) throws Exception {
		threadPoolExecutor = new ThreadPoolExecutor();
		serverPort = Integer.parseInt(portSpecified);
		serverSocket = new ServerSocket(serverPort);
		serverIP = InetAddress.getLocalHost().getHostAddress().toString();
		initialiseServerManagementVariables();
		printServerMessageToConsole(String.format("Server started on port %s...", portSpecified));
	}

	private static String getCurrentDateTime() {
		LocalDateTime now = new LocalDateTime();
		return now.toString();
	}

	private static void initialiseServerManagementVariables() {
		connectedClients = new ConcurrentSkipListSet<ClientNode>();
		activeChatRooms = new ConcurrentSkipListSet<Chatroom>();
		running = true;
		nextClientId = new AtomicInteger(0);
		nextChatroomId = new AtomicInteger(0);
	}

	private static void handleIncomingConnection() throws Exception {
		Socket clientSocket = serverSocket.accept();
		clientSocket.setKeepAlive(true);
		clientSocket.setTcpNoDelay(true);
		System.out.println(String.format("%s>> Connection received from %s...", getCurrentDateTime(),
				clientSocket.getInetAddress().toString()));

		List<String> message = getFullMessageFromClient(clientSocket);
		if (message != null) {
			ClientRequest clientRequest = requestedAction(message);
			ClientNode clientNode = extractClientInfo(clientSocket, clientRequest, message);
			threadPoolExecutor.submitTask(clientNode, clientRequest, message);
		} else {
			outputServiceErrorMessageToConsole("Supplied request body was empty: cannot process request");
		}
	}

	public static List<String> getFullMessageFromClient(Socket clientSocket) throws IOException {
		BufferedInputStream inputStream = new BufferedInputStream(clientSocket.getInputStream());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		int result = inputStream.read();
		while ((result != -1) && (inputStream.available() > 0)) {
			outputStream.write((byte) result);
			result = inputStream.read();
		}
		// Assuming UTF-8 encoding
		String inFromClient = outputStream.toString("UTF-8");
		List<String> lines = getRequestStringAsArrayList(inFromClient);

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

	public static ClientNode extractClientInfo(Socket clientSocket, ClientRequest requestType, List<String> message)
			throws IOException {
		switch (requestType) {
		case JOIN_CHATROOM:
			return new ClientNode(clientSocket, message.get(3).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					message.get(0).split(JOIN_CHATROOM_IDENTIFIER, 0)[1], nextClientId.getAndIncrement());
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
			printServerMessageToConsole("Helo client node created");
			return new ClientNode(clientSocket, null, null, UNDEFINED_JOIN_ID);
		case KILL_SERVICE:
			return new ClientNode(null, null, null, UNDEFINED_JOIN_ID);
		default:
			printServerMessageToConsole("Null clientnode created: no match with expected request types");
			return null;
		}
	}

	public static synchronized void shutdown() {
		try {
			printServerMessageToConsole("Server shutting down...");
			threadPoolExecutor.shutdown();
			for (ClientNode node : getAllConnectedClients()) {
				node.getConnection().close();
			}
			getActiveChatRooms().clear();
			serverSocket.close();
			printServerMessageToConsole("Server shut down successfully. Goodbye.");
		} catch (Exception e) {
			outputServiceErrorMessageToConsole(
					String.format("Error occurred when trying to shut down the server: %s", e.getStackTrace()));
		}
	}

	static synchronized void recordClientChangeWithServer(ClientRequest requestedAction, ClientNode clientNode)
			throws Exception {
		if (clientNode != null) {
			printServerMessageToConsole("In recordClientChangeWithServer method - client node isn't null");
			if (requestedAction.equals(ClientRequest.JOIN_CHATROOM) && !getAllConnectedClients().contains(clientNode)
					&& (retrieveRequestedChatroomIfExists(clientNode.getChatroomId()) != null)) {
				addClientRecordToServer(clientNode);
				printServerMessageToConsole("Successfully added new client node to server");
				return;
			} else if (requestedAction.equals(ClientRequest.DISCONNECT)
					&& getAllConnectedClients().contains(clientNode)) {
				removeClientRecordFromServer(clientNode, retrieveRequestedChatroomIfExists(clientNode.getChatroomId()));
				printServerMessageToConsole("Successfully removed client node from server");
				return;
			}
		} else {
			printServerMessageToConsole(
					"Finished executing recordClientChangeWithServer method - client node was null");
		}
		// If we have left the chatroom, we want to keep the record that we were
		// in that chatroom (for repeated LEAVE requests)
	}

	public static ClientRequest requestedAction(List<String> message) throws IOException {
		String requestType = parseClientRequestType(message);
		try {
			ClientRequest clientRequest = ClientRequest.valueOf(requestType);
			printServerMessageToConsole("The parsed request type is " + clientRequest.getValue());
			return clientRequest;
		} catch (Exception e) {
			outputServiceErrorMessageToConsole("Error occurred trying to fetch the request type");
			return null;
		}
	}

	private static String parseClientRequestType(List<String> message) throws IOException {
		printServerMessageToConsole("In parseClientRequestType method");
		String[] requestType = message.get(0).split(SPLIT_CRITERIA, 0);
		if (requestType[0].contains("HELO")) {
			String temp = requestType[0];
			String[] splitString = temp.split(" ", 0);
			requestType[0] = splitString[0];
		}
		printServerMessageToConsole(String.format("Parsed request type '%s", requestType[0]));

		return requestType[0];
	}

	public static void addClientRecordToServer(ClientNode clientNode) {
		if (!getAllConnectedClients().contains(clientNode)) {
			getAllConnectedClients().add(clientNode);
			printServerMessageToConsole("Added client record to server");
		} else {
			printServerMessageToConsole("client record not added to server");
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

	public static void setRunning(boolean value) {
		running = value;
	}

	public static void outputRequestErrorMessageToConsole(String errorResponse, ClientNode clientNode) {
		String output = String.format("%s>> SERVER: Error processing request (client %s): %s", getCurrentDateTime(),
				clientNode.getName(), errorResponse);
		System.out.println(output);
	}

	public static void outputServiceErrorMessageToConsole(String errorMessage) {
		String output = String.format("%s>> SERVER: Service error: %s", getCurrentDateTime(), errorMessage);
		System.out.println(output);
	}

	public static ServerSocket getServerSocket() {
		return serverSocket;
	}

	public static void printServerMessageToConsole(String message) {
		System.out.println(String.format("%s>> SERVER: %s", getCurrentDateTime(), message));

	}

}
