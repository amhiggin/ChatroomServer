package main.java;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.LocalDateTime;

public class ChatroomServer {

	public static ThreadPoolExecutor threadPoolExecutor;
	public static AtomicInteger nextClientId;
	public static AtomicInteger nextChatroomId;
	private static ServerSocket serverSocket;
	private static volatile boolean running;
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
				handleIncomingConnection();
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
		printServerMessageToConsole(
				String.format("Connection received from %s...", clientSocket.getInetAddress().toString()));
		threadPoolExecutor.submitTask(clientSocket);
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
