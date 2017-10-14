package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatroomServer {

	private static ServerSocket serverSocket;
	private static AtomicBoolean terminateServer;
	private static ConcurrentSkipListMap<Chatroom, ConcurrentSkipListSet<ClientNode>> activeChatRooms;

	/*
	 * Server port is passed as arg[0]
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
		serverSocket = new ServerSocket(Integer.parseInt(args[0]));
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
		Request clientRequest = requestedAction(clientSocket);
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

	private static ClientNode extractClientInfo(Socket clientSocket) throws IOException {
		BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		// FIXME (potentially) Assume that the request is in the first line
		String clientSentence = inFromClient.readLine();

		// Check that the below works
		String[] clientName = (clientSentence.split("CLIENT_NAME:", 0))[0].split("\n", 0);
		String[] chatroomId = (clientSentence.split("CHAT:", 0))[0].split("\n", 0);
		return new ClientNode(clientSocket, clientName[0], 0);
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

	static synchronized void recordClientChangeWithServer(Request requestedAction, ClientNode clientNode)
			throws IOException {
		if (clientNode != null) {
			if (requestedAction.equals(Request.JoinChatroom) && !getActiveChatRooms().values().contains(clientNode)) {
				addClientRecordToServer(clientNode);
			} else if (requestedAction.equals(Request.LeaveChatroom)
					&& getActiveChatRooms().values().contains(clientNode)) {
				removeClientRecordFromServer(clientNode,
						doesChatroomAlreadyExistByReference(clientNode.getChatroomId()));
			}
		}
	}

	public static Request requestedAction(Socket clientSocket) throws IOException {
		String requestType = parseClientRequest(clientSocket);
		try {
			return Request.valueOf(requestType);
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

	public static Chatroom doesChatroomAlreadyExistByReference(int requestedChatroomToJoin) {
		for (Entry<Chatroom, ConcurrentSkipListSet<ClientNode>> entry : activeChatRooms.entrySet()) {
			if (entry.getKey().getChatroomId() == requestedChatroomToJoin) {
				return entry.getKey();
			}
		}
		return null;
	}

}
