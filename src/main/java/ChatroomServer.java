package main.java;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.LocalDateTime;

public class ChatroomServer {

	public static AtomicInteger nextClientId;
	public static AtomicInteger nextChatroomId;
	private static ServerSocket serverSocket;
	private static volatile boolean running;
	private static List<Chatroom> activeChatRooms;
	private static List<ClientConnectionObject> connectedClients;
	static int serverPort;
	static String serverIP;

	/*
	 * Server port is passed as arg[0]
	 */
	public static void main(String[] args) {
		try {
			initialiseServer(args[0]);
			while (true) {
				if (running == false) {
					shutdown();
				}
				handleIncomingConnection();
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
		serverIP = InetAddress.getLocalHost().getHostAddress().toString();
		initialiseServerManagementVariables();
		printServerMessageToConsole(String.format("Server started on port %s...", portSpecified));
	}

	private static String getCurrentDateTime() {
		LocalDateTime now = new LocalDateTime();
		return now.toString();
	}

	private static void initialiseServerManagementVariables() {
		connectedClients = new ArrayList<ClientConnectionObject>();
		activeChatRooms = new ArrayList<Chatroom>();
		running = true;
		nextClientId = new AtomicInteger(0);
		nextChatroomId = new AtomicInteger(0);
	}

	private static void handleIncomingConnection() throws Exception {
		Socket clientSocket = acceptAndMaintainSocketConnection();
		runClientThread(clientSocket);
	}

	private static Socket acceptAndMaintainSocketConnection() throws IOException, SocketException {
		Socket clientSocket = serverSocket.accept();
		clientSocket.setKeepAlive(true);
		// clientSocket.setTcpNoDelay(true);
		printServerMessageToConsole(
				String.format("**********\nConnection received from %s...", clientSocket.getInetAddress().toString()));
		return clientSocket;
	}

	private static void runClientThread(Socket clientSocket) {
		ClientThread thread = new ClientThread(clientSocket);
		thread.start();
	}

	public static synchronized void shutdown() {
		try {
			printServerMessageToConsole("Server shutting down...");
			for (ClientConnectionObject clientConnection : getAllConnectedClients()) {
				clientConnection.getSocketInputStream().close();
				clientConnection.getSocketOutputStream().close();
				clientConnection.getSocket().close();
			}
			getActiveChatRooms().clear();
			getAllConnectedClients().clear();
			serverSocket.close();
			printServerMessageToConsole("Server shut down successfully. Goodbye.");
		} catch (Exception e) {
			outputServiceErrorMessageToConsole(
					String.format("Error occurred when trying to shut down the server: %s", e.getStackTrace()));
		}
	}

	static void addClientRecordToServer(ClientConnectionObject clientConnectionObject, RequestNode clientNode) {
		// JOIN
		if (clientNode.getRequestType().equals(ClientRequest.JOIN_CHATROOM)
				&& !getAllConnectedClients().contains(clientConnectionObject)
				&& (retrieveRequestedChatroomByRoomIdIfExists(clientNode.getChatroomRequested()) != null)) {
			for (ClientConnectionObject clientConnection : connectedClients) {
				if (clientConnection == clientConnectionObject) {
					printServerMessageToConsole("client socket not added to server records: already existed");
					return;
				}
			}
			getAllConnectedClients().add(clientConnectionObject);
			printServerMessageToConsole("Added client connection record to server");
		}
	}

	static synchronized void removeClientRecordFromServerUponDisconnect(ClientConnectionObject clientConnectionObject,
			RequestNode clientNode) throws Exception {
		String clientLeftChatroomMessage;
		String chatMessage;
		if (getAllConnectedClients().contains(clientConnectionObject)) {

			for (Chatroom chatroom : getActiveChatRooms()) {
				if (chatroom.getListOfConnectedClients().contains(clientConnectionObject)) {
					chatroom.removeClientRecord(clientConnectionObject, clientNode);
					printServerMessageToConsole(String.format("Removed client %s from chatroom %s",
							clientNode.getName(), chatroom.getChatroomId()));
					clientLeftChatroomMessage = String.format("%s has left this chatroom", clientNode.getName());
					chatMessage = String.format(ServerResponse.CHAT.getValue(), chatroom.getChatroomRef(),
							clientNode.getName(), clientLeftChatroomMessage);
					chatroom.broadcastMessageInChatroom(chatMessage);
					printServerMessageToConsole(
							String.format("Sent message in chatroom%s: '%s'", chatroom.getChatroomId(), chatMessage));
				}
			}
			printServerMessageToConsole(String.format("removed client record from all chatrooms"));
			getAllConnectedClients().remove(clientConnectionObject);
		}
	}

	public static List<Chatroom> getActiveChatRooms() {
		return activeChatRooms;
	}

	public static synchronized Chatroom retrieveRequestedChatroomByRoomIdIfExists(String requestedChatroomToJoin) {
		printServerMessageToConsole(
				String.format("Checking with server whether chatroom %s exists...", requestedChatroomToJoin));
		for (Chatroom chatroom : activeChatRooms) {
			if ((chatroom.getChatroomId().contains(requestedChatroomToJoin))) {
				printServerMessageToConsole(
						String.format("Chatroom %s found in server records", requestedChatroomToJoin));
				return chatroom;
			}
		}
		printServerMessageToConsole(
				String.format("Couldn't find chatroom %s in server records", requestedChatroomToJoin));
		return null;
	}

	public static Chatroom retrieveRequestedChatroomByRoomRefIfExists(String requestedChatroomToLeave) {
		for (Chatroom chatroom : activeChatRooms) {
			if (chatroom.getChatroomRef() == Integer.parseInt(requestedChatroomToLeave)) {
				return chatroom;
			}
		}
		return null;
	}

	public static int getServerPort() {
		return serverPort;
	}

	public static synchronized List<ClientConnectionObject> getAllConnectedClients() {
		return connectedClients;
	}

	public static void setRunning(boolean value) {
		running = value;
	}

	public static void outputRequestErrorMessageToConsole(String errorResponse, RequestNode clientNode) {
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
