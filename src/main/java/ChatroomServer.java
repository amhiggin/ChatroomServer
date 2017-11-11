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
	private static List<Socket> connectedClients;
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
		connectedClients = new ArrayList<Socket>();
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
		clientSocket.setTcpNoDelay(true);
		printServerMessageToConsole(
				String.format("Connection received from %s...", clientSocket.getInetAddress().toString()));
		return clientSocket;
	}

	private static void runClientThread(Socket clientSocket) {
		ClientThread thread = new ClientThread(clientSocket);
		thread.start();
	}

	public static synchronized void shutdown() {
		try {
			printServerMessageToConsole("Server shutting down...");
			for (Socket socket : getAllConnectedClients()) {
				socket.close();
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

	static synchronized void recordClientChangeWithServer(ClientRequest requestedAction, Socket clientSocket,
			ClientRequestNode clientNode) throws Exception {
		if (clientNode != null) {
			printServerMessageToConsole("In recordClientChangeWithServer method - client node isn't null");

			if (requestedAction.equals(ClientRequest.JOIN_CHATROOM) && !getAllConnectedClients().contains(clientSocket)
					&& (retrieveRequestedChatroomIfExists(clientNode.getChatroomRequested()) != null)) {
				addClientRecordToServer(clientSocket);
				printServerMessageToConsole("Successfully added new client record to server");
				return;
			} else if (requestedAction.equals(ClientRequest.DISCONNECT)
					&& getAllConnectedClients().contains(clientSocket)) {
				removeClientRecordFromServer(clientSocket,
						retrieveRequestedChatroomIfExists(clientNode.getChatroomRequested()));
				printServerMessageToConsole("Successfully removed client record from server");
				return;
			}
		} else {
			printServerMessageToConsole(
					"Finished executing recordClientChangeWithServer method - client node was null");
		}
		// If we have left the chatroom, we want to keep the record that we were
		// in that chatroom (for repeated LEAVE requests)
	}

	public static void addClientRecordToServer(Socket clientSocket) {
		for (Socket socket : connectedClients) {
			if (clientSocket == socket) {
				printServerMessageToConsole("client socket not added to server records: already existed");
				return;
			}
		}
		getAllConnectedClients().add(clientSocket);
		printServerMessageToConsole("Added client socket to server");
	}

	private static void removeClientRecordFromServer(Socket clientSocket, Chatroom requestedChatroom)
			throws IOException {
		// Note this involves removing from chatroom too
		for (Chatroom chatroom : getActiveChatRooms()) {
			if (chatroom == requestedChatroom) {
				chatroom.getListOfConnectedClients().remove(clientSocket);
				break;
			}
		}
		connectedClients.remove(clientSocket);
		clientSocket.close();
		return;
	}

	public static List<Chatroom> getActiveChatRooms() {
		return activeChatRooms;
	}

	public static Chatroom retrieveRequestedChatroomIfExists(String requestedChatroomToJoin) {
		for (Chatroom chatroom : activeChatRooms) {
			if ((chatroom.getChatroomId() == requestedChatroomToJoin)) {
				return chatroom;
			}
		}
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

	public static synchronized List<Socket> getAllConnectedClients() {
		return connectedClients;
	}

	public static void setRunning(boolean value) {
		running = value;
	}

	public static void outputRequestErrorMessageToConsole(String errorResponse, ClientRequestNode clientNode) {
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
